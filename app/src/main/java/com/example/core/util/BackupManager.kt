package com.example.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * النسخ الاحتياطي والاسترجاع.
 *
 * ما تغيّر بعد مراجعة v4 (DATA-01..03):
 * - النسخة صارت تشمل الجداول العشرة، لا خمسة. الروابط والصلوح ودفعاتها وعروض السوق
 *   ومصروفات المزرعة لم تكن تُنسخ قبل الآن، فكانت تُفقد عند الاستعادة.
 * - الصفوف المؤرشفة والمصادر المعطّلة تُنسخ أيضًا؛ كانت [com.example.features.customers.CustomerDao.getAllCustomers]
 *   و[com.example.features.pumps.PumpSourceDao.getAllPumps] تُسقطها.
 * - الاسترجاع يمر بثلاث مراحل: معاينة ([previewRestoreFromFile] / [previewRestoreFromUri])،
 *   ثم نسخة أمان تلقائية للوضع الحالي، ثم كتابة داخل معاملة واحدة تُلغى كاملة عند أي خطأ.
 * - لا يُحذف شيء لا يذكره الملف، ولا يُقبل ملف من صيغة أحدث، ولا تُمزج نسخة ناقصة بصمت.
 */
object BackupManager {

    const val BACKUP_FILE_PREFIX = "jerba_backup_"
    const val SAFETY_COPY_PREFIX = "jerba_before_restore_"

    /** عدد نسخ الأمان التلقائية المحفوظة قبل حذف الأقدم. */
    private const val MAX_SAFETY_COPIES = 5

    private fun backupsDir(context: Context): File = File(context.filesDir, "backups").apply {
        if (!exists()) mkdirs()
    }

    suspend fun createBackupJson(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val source = BackupSnapshot.readAll(database)
        val root = BackupSnapshot.toJson(source)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupsDir(context), "$BACKUP_FILE_PREFIX$dateStr.back")
        FileOutputStream(backupFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }
        backupFile
    }

    data class BackupFileInfo(
        val file: File,
        val name: String,
        val formattedDate: String,
        val sizeText: String
    )

    fun getAvailableBackups(context: Context): List<BackupFileInfo> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))
        return backupDir.listFiles { _, name -> name.endsWith(".back") || name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val sizeKb = (file.length() / 1024.0)
                val sizeStr = if (sizeKb < 1.0) "${file.length()} بايت" else String.format(Locale.US, "%.1f ك.ب", sizeKb)
                BackupFileInfo(
                    file = file,
                    name = file.name,
                    formattedDate = sdf.format(Date(file.lastModified())),
                    sizeText = sizeStr
                )
            } ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean {
        return runCatching { if (file.exists()) file.delete() else false }.getOrDefault(false)
    }

    // ------------------------------------------------------------- المعاينة

    /**
     * معاينة ملف النسخة قبل أي كتابة: عدد الجداول، وما سيُستبدل، والأخطاء المانعة.
     * لا تُكتب أي بيانات هنا.
     */
    suspend fun previewRestoreFromFile(database: AppDatabase, file: File): Result<BackupSnapshot.RestorePlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(file.readText(Charsets.UTF_8))
                BackupSnapshot.plan(root, BackupSnapshot.readAll(database))
            }
        }

    suspend fun previewRestoreFromUri(context: Context, database: AppDatabase, uri: Uri): Result<BackupSnapshot.RestorePlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(readText(context, uri))
                BackupSnapshot.plan(root, BackupSnapshot.readAll(database))
            }
        }

    // ------------------------------------------------------------ الاسترجاع

    suspend fun restoreFromFile(context: Context, database: AppDatabase, file: File): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(file.readText(Charsets.UTF_8))
                val plan = BackupSnapshot.plan(root, BackupSnapshot.readAll(database))
                applyPlan(context, database, plan)
            }
        }

    suspend fun restoreFromJson(context: Context, database: AppDatabase, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(readText(context, uri))
                val plan = BackupSnapshot.plan(root, BackupSnapshot.readAll(database))
                applyPlan(context, database, plan)
            }
        }

    /** يُستخدم من اختبارات القاعدة ومن الاسترجاع المباشر بعد معاينة مقبولة. */
    suspend fun restoreFromPlan(context: Context, database: AppDatabase, plan: BackupSnapshot.RestorePlan): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching { applyPlan(context, database, plan) }
        }

    private suspend fun applyPlan(
        context: Context,
        database: AppDatabase,
        plan: BackupSnapshot.RestorePlan
    ): Int {
        if (!plan.canRestore) {
            throw IllegalArgumentException(plan.errors.joinToString(" "))
        }
        // نسخة أمان للوضع الحالي قبل أي كتابة، حتى يمكن الرجوع إن فشل شيء لاحقًا.
        createSafetyCopy(context, database)
        return database.withTransaction { BackupSnapshot.apply(database, plan) }
    }

    /**
     * نسخة أمان تلقائية للبيانات الحالية قبل الاسترجاع. تُحفظ في مجلد النسخ نفسه،
     * ولا تُحذف إلا إذا تجاوز عدد النسخ التلقائية [MAX_SAFETY_COPIES].
     */
    suspend fun createSafetyCopy(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val source = BackupSnapshot.readAll(database)
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(backupsDir(context), "$SAFETY_COPY_PREFIX$dateStr.back")
        FileOutputStream(file).use { fos ->
            fos.write(BackupSnapshot.toJson(source).toString(2).toByteArray(Charsets.UTF_8))
        }
        pruneSafetyCopies(context)
        file
    }

    private fun pruneSafetyCopies(context: Context) {
        val copies = backupsDir(context)
            .listFiles { _, name -> name.startsWith(SAFETY_COPY_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        copies.drop(MAX_SAFETY_COPIES).forEach { it.delete() }
    }

    private fun readText(context: Context, uri: Uri): String {
        val builder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    builder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return builder.toString()
    }

    // -------------------------------------------------------------- المشاركة

    fun shareBackupToDriveOrApps(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - جِربة | Jerba")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "حفظ في Google Drive أو مشاركة النسخة الاحتياطية").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Attempts to send the backup file directly to Google Drive app.
     * Falls back to general chooser if Google Drive is not installed.
     */
    fun shareBackupDirectToDrive(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val driveIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - جِربة | Jerba")
            `package` = "com.google.android.apps.docs"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        if (driveIntent.resolveActivity(pm) != null) {
            context.startActivity(driveIntent)
        } else {
            // Google Drive not installed or cannot resolve — fallback to system chooser
            shareBackupToDriveOrApps(context, backupFile)
        }
    }

    suspend fun saveBackupToPhoneDownloads(context: Context, database: AppDatabase): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = createBackupJson(context, database)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, backupFile.name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/JerbaBackups")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        backupFile.inputStream().use { it.copyTo(os) }
                    }
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val subDir = File(downloadsDir, "JerbaBackups")
                if (!subDir.exists()) subDir.mkdirs()
                val targetFile = File(subDir, backupFile.name)
                backupFile.copyTo(targetFile, overwrite = true)
            }
            backupFile
        }
    }
}
