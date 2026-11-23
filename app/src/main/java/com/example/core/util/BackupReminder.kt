package com.example.core.util

object BackupReminder {
    const val NO_BACKUP = -1
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Days since the last backup (>= 7), [NO_BACKUP] when none exists but there is data, null = no banner. */
    fun daysToShow(lastBackupMs: Long, nowMs: Long, hasOperations: Boolean): Int? {
        if (lastBackupMs <= 0L) return if (hasOperations) NO_BACKUP else null
        val days = ((nowMs - lastBackupMs) / DAY_MS).toInt()
        return if (days >= 7) days else null
    }

    fun isHidden(hiddenUntilMs: Long, nowMs: Long): Boolean = nowMs < hiddenUntilMs

    fun hideUntil(nowMs: Long): Long = nowMs + 3 * DAY_MS
}
