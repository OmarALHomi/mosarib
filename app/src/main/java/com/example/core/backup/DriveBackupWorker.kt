package com.example.core.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.core.database.AppDatabase
import com.example.core.util.BackupManager
import com.example.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class BackupFrequency(val days: Int, val label: String) {
    DAILY(1, "يومياً"),
    THREE_DAYS(3, "كل 3 أيام"),
    WEEKLY(7, "أسبوعياً"),
    OFF(0, "إيقاف")
}

/**
 * Pure decision logic for whether a Google Drive backup is due / should execute.
 * Has no Android framework dependencies to allow complete pure unit testing.
 */
object DriveBackupDecision {

    fun shouldRunBackup(
        frequency: BackupFrequency,
        lastBackupTimeMillis: Long,
        currentDbModifiedMillis: Long,
        lastBackupDbModifiedMillis: Long,
        nowMillis: Long,
        hasGoogleAccount: Boolean,
        force: Boolean = false
    ): Boolean {
        if (!hasGoogleAccount) return false
        if (force) return true
        if (frequency == BackupFrequency.OFF) return false

        // Skip when nothing changed in DB since last successful Drive backup
        if (lastBackupTimeMillis > 0L && currentDbModifiedMillis > 0L && currentDbModifiedMillis <= lastBackupDbModifiedMillis) {
            return false
        }

        val periodMillis = frequency.days * 24L * 60L * 60L * 1000L
        val elapsed = nowMillis - lastBackupTimeMillis
        return lastBackupTimeMillis <= 0L || elapsed >= periodMillis
    }
}

/**
 * Persists Google Drive automatic backup preferences and state.
 */
object DriveBackupPreferences {
    private const val PREFS_FILE = "mosarib_drive_backup_prefs"
    private const val KEY_FREQUENCY = "backup_frequency"
    private const val KEY_WIFI_ONLY = "backup_wifi_only"
    private const val KEY_LAST_BACKUP_TIME = "last_drive_backup_time"
    private const val KEY_LAST_BACKUP_DB_MODIFIED = "last_drive_backup_db_modified"
    private const val KEY_LAST_ERROR = "last_drive_error"
    private const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"
    private const val KEY_FIRST_FAILURE_TIME = "first_failure_time"
    private const val KEY_LAST_LIST_ERROR = "last_drive_list_error"

    fun getFrequency(context: Context): BackupFrequency {
        val name = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_FREQUENCY, BackupFrequency.DAILY.name)
        return try {
            BackupFrequency.valueOf(name ?: BackupFrequency.DAILY.name)
        } catch (_: Exception) {
            BackupFrequency.DAILY
        }
    }

    fun setFrequency(context: Context, frequency: BackupFrequency) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FREQUENCY, frequency.name)
            .apply()
    }

    fun isWifiOnly(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_WIFI_ONLY, false)
    }

    fun setWifiOnly(context: Context, wifiOnly: Boolean) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WIFI_ONLY, wifiOnly)
            .apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun getLastBackupDbModified(context: Context): Long {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_DB_MODIFIED, 0L)
    }

    fun getLastError(context: Context): String? {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ERROR, null)
    }

    fun getListError(context: Context): String? {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LAST_LIST_ERROR, null)
    }

    fun recordListError(context: Context, error: String) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_LIST_ERROR, error)
            .apply()
    }

    fun clearListError(context: Context) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_LIST_ERROR)
            .apply()
    }

    fun getConsecutiveFailures(context: Context): Int {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getInt(KEY_CONSECUTIVE_FAILURES, 0)
    }

    fun getFirstFailureTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_FIRST_FAILURE_TIME, 0L)
    }

    fun recordSuccess(context: Context, dbModifiedTime: Long) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putLong(KEY_LAST_BACKUP_DB_MODIFIED, dbModifiedTime)
            .remove(KEY_LAST_ERROR)
            .putInt(KEY_CONSECUTIVE_FAILURES, 0)
            .putLong(KEY_FIRST_FAILURE_TIME, 0L)
            .apply()
    }

    fun recordFailure(context: Context, errorMessage: String) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val currentFailures = prefs.getInt(KEY_CONSECUTIVE_FAILURES, 0) + 1
        val firstFailure = if (currentFailures == 1) now else prefs.getLong(KEY_FIRST_FAILURE_TIME, now)
        prefs.edit()
            .putString(KEY_LAST_ERROR, errorMessage)
            .putInt(KEY_CONSECUTIVE_FAILURES, currentFailures)
            .putLong(KEY_FIRST_FAILURE_TIME, firstFailure)
            .apply()
    }
}

/**
 * Schedules periodic and immediate Google Drive backup tasks with WorkManager.
 */
object DriveBackupScheduler {
    const val DATABASE_NAME = "water_distributor_db"
    const val PERIODIC_WORK_NAME = "mosarib_drive_backup"
    const val IMMEDIATE_WORK_NAME = "mosarib_drive_backup_immediate"

    fun schedulePeriodicWork(context: Context) {
        val frequency = DriveBackupPreferences.getFrequency(context)
        if (frequency == BackupFrequency.OFF) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val wifiOnly = DriveBackupPreferences.isWifiOnly(context)
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(
            frequency.days.toLong(), TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun enqueueImmediateBackup(context: Context) {
        val wifiOnly = DriveBackupPreferences.isWifiOnly(context)
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<DriveBackupWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .setInputData(workDataOf("is_manual" to true))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun checkStartupDriveBackup(context: Context) {
        schedulePeriodicWork(context)

        val account = GoogleDriveBackupHelper.getSignedInAccount(context) ?: return
        val frequency = DriveBackupPreferences.getFrequency(context)
        if (frequency == BackupFrequency.OFF) return

        val lastBackupTime = DriveBackupPreferences.getLastBackupTime(context)
        val lastDbModified = DriveBackupPreferences.getLastBackupDbModified(context)
        val currentDbModified = getDatabaseModifiedTime(context)
        val now = System.currentTimeMillis()

        if (DriveBackupDecision.shouldRunBackup(
                frequency = frequency,
                lastBackupTimeMillis = lastBackupTime,
                currentDbModifiedMillis = currentDbModified,
                lastBackupDbModifiedMillis = lastDbModified,
                nowMillis = now,
                hasGoogleAccount = true,
                force = false
            )
        ) {
            enqueueImmediateBackup(context)
        }
    }

    fun getDatabaseModifiedTime(context: Context): Long {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val walFile = File(dbFile.path + "-wal")
            maxOf(
                if (dbFile.exists()) dbFile.lastModified() else 0L,
                if (walFile.exists()) walFile.lastModified() else 0L
            )
        } catch (_: Exception) {
            0L
        }
    }
}

/**
 * Background worker that performs local backup generation and Google Drive upload.
 */
class DriveBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val isManual = inputData.getBoolean("is_manual", false)

        val account = GoogleDriveBackupHelper.getSignedInAccount(context)
        if (account == null) {
            if (isManual) {
                DriveBackupPreferences.recordFailure(context, "الحساب غير مربوط")
            }
            return@withContext Result.success()
        }

        val frequency = DriveBackupPreferences.getFrequency(context)
        if (!isManual && frequency == BackupFrequency.OFF) {
            return@withContext Result.success()
        }

        val currentDbModified = DriveBackupScheduler.getDatabaseModifiedTime(context)
        val lastDbModified = DriveBackupPreferences.getLastBackupDbModified(context)
        val lastBackupTime = DriveBackupPreferences.getLastBackupTime(context)

        // Skip when nothing changed since last successful backup (periodic only)
        if (!isManual && lastBackupTime > 0L && currentDbModified > 0L && currentDbModified <= lastDbModified) {
            return@withContext Result.success()
        }

        var tempBackupFile: File? = null
        try {
            val db = AppDatabase.getDatabase(context)
            tempBackupFile = BackupManager.createBackupJson(context, db)

            // Also maintain local daily rolling backup
            val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val autoDir = File(context.filesDir, "auto_backups").apply { if (!exists()) mkdirs() }
            val dailyFile = File(autoDir, "mosarib_auto_daily_$todayDateStr.back")
            if (!dailyFile.exists()) {
                tempBackupFile.copyTo(dailyFile, overwrite = true)
            }

            val fileToUpload = if (dailyFile.exists()) dailyFile else tempBackupFile
            val uploadResult = GoogleDriveBackupHelper.uploadBackupToAppData(context, account, fileToUpload)

            if (uploadResult.isSuccess) {
                DriveBackupPreferences.recordSuccess(context, currentDbModified)
                pruneDriveBackups(context, account)
                Result.success()
            } else {
                val error = uploadResult.exceptionOrNull()
                handleFailure(context, error)
            }
        } catch (e: Exception) {
            handleFailure(context, e)
        } finally {
            try {
                if (tempBackupFile != null && tempBackupFile.exists()) {
                    tempBackupFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun pruneDriveBackups(context: Context, account: GoogleSignInAccount) {
        try {
            val list = GoogleDriveBackupHelper.listAppDataBackups(context, account).getOrNull() ?: return
            val appBackups = list.filter { it.name.startsWith("mosarib_") && it.name.endsWith(".back") }
            if (appBackups.size > 10) {
                appBackups.drop(10).forEach { file ->
                    GoogleDriveBackupHelper.deleteAppDataBackup(context, account, file.id)
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleFailure(context: Context, throwable: Throwable?): Result {
        val readableError = if (throwable != null) {
            toPlainArabicError(throwable)
        } else {
            "حدث خطأ غير متوقع أثناء النسخ"
        }
        DriveBackupPreferences.recordFailure(context, readableError)

        val failures = DriveBackupPreferences.getConsecutiveFailures(context)
        if (failures >= 3) {
            val firstFailure = DriveBackupPreferences.getFirstFailureTime(context)
            val daysDiff = maxOf(
                1,
                ((System.currentTimeMillis() - firstFailure) / (24L * 3600L * 1000L)).toInt()
            )
            showFailureNotification(context, daysDiff)
        }

        return Result.retry()
    }

    private fun toPlainArabicError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            e is java.net.UnknownHostException || msg.contains("UnknownHost", ignoreCase = true) || msg.contains("No address", ignoreCase = true) || msg.contains("Network is unreachable", ignoreCase = true) ->
                "لا يوجد إنترنت"
            msg.contains("حساب Google غير متوفر") || msg.contains("DriveUserRecoverable") || msg.contains("401") || msg.contains("صلاحيات", ignoreCase = true) ->
                "الحساب غير مربوط"
            msg.contains("storage", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("مساحة", ignoreCase = true) ->
                "مساحة Drive ممتلئة"
            msg.contains("timeout", ignoreCase = true) || msg.contains("مهلة", ignoreCase = true) ->
                "انتهت مهلة الاتصال بالإنترنت"
            else ->
                GoogleDriveBackupHelper.getReadableErrorMessage(e).take(60)
        }
    }

    private fun showFailureNotification(context: Context, days: Int) {
        val channelId = "mosarib_backup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "النسخ الاحتياطي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات حالة النسخ الاحتياطي التلقائي إلى Google Drive"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1035,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = String.format(Locale.US, "تعذّر النسخ إلى Google Drive منذ %d أيام", days)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("النسخ الاحتياطي إلى Google Drive")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1035, notification)
    }
}
