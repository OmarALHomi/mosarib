package com.example.core.util

import android.content.Context

object PeriodicBackupReminderManager {
    private const val PREFS_FILE = "mosarib_license_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_successful_backup_timestamp"
    private const val KEY_REMINDER_DISMISSED_UNTIL = "backup_reminder_dismissed_until"
    private const val REMINDER_INTERVAL_DAYS = 7L // أسبوعي افتراضياً

    fun recordBackupPerformed(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()
    }

    fun dismissReminderTemporarily(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        // Remind again after 3 days
        val snoozeUntil = System.currentTimeMillis() + (3L * 24 * 3600 * 1000L)
        sp.edit().putLong(KEY_REMINDER_DISMISSED_UNTIL, snoozeUntil).apply()
    }

    fun shouldShowBackupReminder(context: Context, totalOperationsCount: Int): Boolean {
        // Only remind users who have active data (> 5 operations)
        if (totalOperationsCount < 5) return false

        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val snoozeUntil = sp.getLong(KEY_REMINDER_DISMISSED_UNTIL, 0L)
        if (now < snoozeUntil) return false

        val lastBackupTime = sp.getLong(KEY_LAST_BACKUP_TIME, 0L)
        if (lastBackupTime == 0L) {
            // Never backed up, check if operations exist
            return totalOperationsCount >= 10
        }

        val elapsedMs = now - lastBackupTime
        val elapsedDays = elapsedMs / (24L * 3600L * 1000L)
        return elapsedDays >= REMINDER_INTERVAL_DAYS
    }
}
