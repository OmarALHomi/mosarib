package com.example.core.util

import android.content.Context
import com.example.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * محرك النسخ الاحتياطي التلقائي اليومي ونقاط الأمان الوقائية.
 * يضمن بقاء بيانات المستخدم بأمان تام بدون استهلاك مساحة التخزين عبر نظام التدوير لـ 7 أيام.
 */
object AutoBackupEngine {

    private const val PREFS_FILE = "mosarib_auto_backup_prefs"
    private const val KEY_LAST_DAILY_BACKUP_DATE = "last_daily_backup_date"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val AUTO_BACKUP_DIR_NAME = "auto_backups"
    private const val MAX_DAILY_BACKUPS = 7
    private const val MAX_SAFETY_CHECKPOINTS = 3

    fun isAutoBackupEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled)
            .apply()
    }

    data class AutoBackupItem(
        val file: File,
        val title: String,
        val formattedDate: String,
        val sizeText: String,
        val isSafetyCheckpoint: Boolean = false
    )

    private fun getAutoBackupDir(context: Context): File {
        val dir = File(context.filesDir, AUTO_BACKUP_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * تنفيذ النسخ اليومي التلقائي عند فتح التطبيق (مرة واحدة فقط في اليوم).
     */
    suspend fun performDailyAutoBackup(context: Context, database: AppDatabase, operationsCount: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isAutoBackupEnabled(context)) return@withContext false
            // لا نأخذ نسخة فارغة إذا لم تكن هناك بيانات كافية
            if (operationsCount < 3) return@withContext false

            val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val lastDate = sp.getString(KEY_LAST_DAILY_BACKUP_DATE, null)

            // إذا أُخذت نسخة اليوم بالفعل، نتخطى لحفظ موارد الهاتف
            if (todayDateStr == lastDate) return@withContext false

            val autoDir = getAutoBackupDir(context)
            val tempBackup = BackupManager.createBackupJson(context, database)

            val dailyFile = File(autoDir, "mosarib_auto_daily_$todayDateStr.back")
            if (dailyFile.exists()) dailyFile.delete()

            // نقل أو نسخ الملف إلى مجلد النسخ التلقائي
            tempBackup.copyTo(dailyFile, overwrite = true)
            tempBackup.delete()

            sp.edit().putString(KEY_LAST_DAILY_BACKUP_DATE, todayDateStr).apply()
            PeriodicBackupReminderManager.recordBackupPerformed(context)

            // تنظيف النسخ القديمة للاحتفاظ بآخر 7 أيام فقط
            pruneOldDailyBackups(context)

            // محاولة المزامنة الصامتة مع Google Drive إذا كان الحساب مربوطاً وتوفر الإنترنت
            syncToGoogleDriveIfPossible(context, dailyFile)

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * إنشاء نقطة أمان فورية قبل أي عملية حساسة (استعادة ملف جديد، تصفير الحسابات، إلخ).
     */
    suspend fun createSafetyCheckpoint(context: Context, database: AppDatabase, reason: String = "checkpoint"): File? = withContext(Dispatchers.IO) {
        try {
            val autoDir = getAutoBackupDir(context)
            val tempBackup = BackupManager.createBackupJson(context, database)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val checkpointFile = File(autoDir, "mosarib_safety_${reason}_$timeStamp.back")

            tempBackup.copyTo(checkpointFile, overwrite = true)
            tempBackup.delete()

            pruneSafetyCheckpoints(context)
            checkpointFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * جلب جميع النسخ التلقائية ونقاط الأمان المتاحة مع تسميات عربية ذكية.
     */
    fun getAvailableAutoBackups(context: Context): List<AutoBackupItem> {
        val dir = getAutoBackupDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".back") } ?: return emptyList()

        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(yesterdayCal.time)
        val arabicDateSdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val isSafety = file.name.contains("safety")
            val sizeKb = (file.length() / 1024.0)
            val sizeStr = if (sizeKb < 1.0) "${file.length()} بايت" else String.format(Locale.US, "%.1f ك.ب", sizeKb)

            val title = when {
                isSafety -> "نقطة أمان وقائية (قبل تعديل/استعادة)"
                file.name.contains(todayStr) -> "نسخة اليوم التلقائية"
                file.name.contains(yesterdayStr) -> "نسخة الأمس التلقائية"
                else -> {
                    // استخراج التاريخ من اسم الملف
                    val regex = Regex("""\d{8}""")
                    val match = regex.find(file.name)?.value
                    if (match != null) {
                        try {
                            val fileDate = SimpleDateFormat("yyyyMMdd", Locale.US).parse(match)
                            val diffDays = ((System.currentTimeMillis() - (fileDate?.time ?: 0L)) / (24L * 3600L * 1000L)).coerceAtLeast(0)
                            "نسخة قبل $diffDays أيام"
                        } catch (_: Exception) {
                            "نسخة احتياطية دورية"
                        }
                    } else {
                        "نسخة احتياطية دورية"
                    }
                }
            }

            AutoBackupItem(
                file = file,
                title = title,
                formattedDate = arabicDateSdf.format(Date(file.lastModified())),
                sizeText = sizeStr,
                isSafetyCheckpoint = isSafety
            )
        }
    }

    /**
     * الاحتفاظ بآخر 7 أيام فقط من النسخ اليومية وحذف الأقدم تلقائياً.
     */
    private fun pruneOldDailyBackups(context: Context) {
        val dir = getAutoBackupDir(context)
        val dailyFiles = dir.listFiles { _, name -> name.startsWith("mosarib_auto_daily_") && name.endsWith(".back") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (dailyFiles.size > MAX_DAILY_BACKUPS) {
            dailyFiles.drop(MAX_DAILY_BACKUPS).forEach { it.delete() }
        }
    }

    /**
     * الاحتفاظ بآخر 3 نقاط أمان فقط وحذف الأقدم.
     */
    private fun pruneSafetyCheckpoints(context: Context) {
        val dir = getAutoBackupDir(context)
        val checkpointFiles = dir.listFiles { _, name -> name.startsWith("mosarib_safety_") && name.endsWith(".back") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (checkpointFiles.size > MAX_SAFETY_CHECKPOINTS) {
            checkpointFiles.drop(MAX_SAFETY_CHECKPOINTS).forEach { it.delete() }
        }
    }

    /**
     * مزامنة صامتة مع Google Drive إذا كان هناك حساب مسجل وإنترنت متاح.
     */
    private suspend fun syncToGoogleDriveIfPossible(context: Context, backupFile: File) = withContext(Dispatchers.IO) {
        try {
            if (!com.example.core.license.LicenseManager.isInternetAvailable(context)) return@withContext
            val signedAccount = GoogleDriveBackupHelper.getSignedInAccount(context) ?: return@withContext
            GoogleDriveBackupHelper.uploadBackupToAppData(context, signedAccount, backupFile)
        } catch (_: Exception) {
            // صامت تماماً - لا يؤثر على سير عمل المستخدم
        }
    }
}
