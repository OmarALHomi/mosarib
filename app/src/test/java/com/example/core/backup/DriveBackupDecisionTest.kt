package com.example.core.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DriveBackupDecisionTest {

    private val oneHour = TimeUnit.HOURS.toMillis(1)
    private val oneDay = TimeUnit.DAYS.toMillis(1)
    private val threeDays = TimeUnit.DAYS.toMillis(3)
    private val sevenDays = TimeUnit.DAYS.toMillis(7)

    @Test
    fun shouldRunBackup_noGoogleAccount_returnsFalse() {
        val now = 1000000000L
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = 0L,
            currentDbModifiedMillis = 500L,
            lastBackupDbModifiedMillis = 0L,
            nowMillis = now,
            hasGoogleAccount = false,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_forceWithoutGoogleAccount_returnsFalse() {
        val now = 1000000000L
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = 0L,
            currentDbModifiedMillis = 500L,
            lastBackupDbModifiedMillis = 0L,
            nowMillis = now,
            hasGoogleAccount = false,
            force = true
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_forceWithGoogleAccount_returnsTrueEvenIfUnchanged() {
        val now = 1000000000L
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = now - oneHour,
            currentDbModifiedMillis = 500L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = true
        )
        assertTrue(result)
    }

    @Test
    fun shouldRunBackup_frequencyOff_returnsFalse() {
        val now = 1000000000L
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.OFF,
            lastBackupTimeMillis = now - sevenDays,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_firstTimeBackup_returnsTrue() {
        val now = 1000000000L
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = 0L,
            currentDbModifiedMillis = 500L,
            lastBackupDbModifiedMillis = 0L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertTrue(result)
    }

    @Test
    fun shouldRunBackup_dbNotModifiedSinceLastBackup_returnsFalse() {
        val now = 1000000000L
        val lastBackupTime = now - (oneDay * 5) // 5 days ago, but db hasn't changed
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 500L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_dailyFrequency_notDueBefore24Hours() {
        val now = 1000000000L
        val lastBackupTime = now - TimeUnit.HOURS.toMillis(23)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_dailyFrequency_dueAfter24Hours() {
        val now = 1000000000L
        val lastBackupTime = now - TimeUnit.HOURS.toMillis(25)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.DAILY,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertTrue(result)
    }

    @Test
    fun shouldRunBackup_threeDaysFrequency_notDueBefore72Hours() {
        val now = 1000000000L
        val lastBackupTime = now - TimeUnit.DAYS.toMillis(2)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.THREE_DAYS,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_threeDaysFrequency_dueAfter72Hours() {
        val now = 1000000000L
        val lastBackupTime = now - TimeUnit.HOURS.toMillis(73)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.THREE_DAYS,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertTrue(result)
    }

    @Test
    fun shouldRunBackup_weeklyFrequency_notDueBefore7Days() {
        val now = 1000000000L
        val lastBackupTime = now - TimeUnit.DAYS.toMillis(6)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.WEEKLY,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldRunBackup_weeklyFrequency_dueAfter7Days() {
        val now = 1000000000L
        val lastBackupTime = now - (sevenDays + oneHour)
        val result = DriveBackupDecision.shouldRunBackup(
            frequency = BackupFrequency.WEEKLY,
            lastBackupTimeMillis = lastBackupTime,
            currentDbModifiedMillis = 1000L,
            lastBackupDbModifiedMillis = 500L,
            nowMillis = now,
            hasGoogleAccount = true,
            force = false
        )
        assertTrue(result)
    }
}
