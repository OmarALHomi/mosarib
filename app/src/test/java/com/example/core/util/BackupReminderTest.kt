package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReminderTest {
    private val day = 24L * 60 * 60 * 1000

    @Test fun recentBackupHidesBanner() = assertNull(BackupReminder.daysToShow(10 * day, 16 * day, true))

    @Test fun oldBackupShowsDays() = assertEquals(8, BackupReminder.daysToShow(10 * day, 18 * day, true))

    @Test fun noBackupWithDataShowsNone() = assertEquals(BackupReminder.NO_BACKUP, BackupReminder.daysToShow(0, 5 * day, true))

    @Test fun noBackupNoDataHides() = assertNull(BackupReminder.daysToShow(0, 5 * day, false))

    @Test fun hiddenForThreeDays() {
        assertTrue(BackupReminder.isHidden(BackupReminder.hideUntil(0), 2 * day))
        assertTrue(!BackupReminder.isHidden(BackupReminder.hideUntil(0), 4 * day))
    }
}
