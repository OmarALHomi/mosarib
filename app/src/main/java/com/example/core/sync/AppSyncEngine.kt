package com.example.core.sync

import android.content.Context
import android.util.Log
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import com.example.core.license.UserDeviceTracker
import com.example.core.notifications.FcmTokenHelper
import com.example.core.updates.AppBroadcast
import com.example.core.updates.AppUpdateManager
import com.example.core.util.PeriodicBackupReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Priority of dialogs/screens to show on app launch:
 * 1. FORCE_UPDATE (Absolute blocker)
 * 2. DEVICE_BLOCKED (Absolute blocker)
 * 3. BACKUP_REMINDER (Critical data safety)
 * 4. BROADCAST_ANNOUNCEMENT (Informational)
 */
enum class StartupDialogType {
    NONE,
    FORCE_UPDATE,
    DEVICE_BLOCKED,
    BACKUP_REMINDER,
    ANNOUNCEMENT
}

data class StartupDecision(
    val dialogToShow: StartupDialogType = StartupDialogType.NONE,
    val forceUpdateBroadcast: AppBroadcast? = null,
    val announcementBroadcast: AppBroadcast? = null,
    val blockReason: String = ""
)

object AppSyncEngine {
    private const val TAG = "AppSyncEngine"

    /**
     * Executes consolidated background synchronization in a single well-managed coroutine pass.
     * Prevents network storms, coordinates state, and returns a single clean decision on what dialog/screen to display.
     */
    suspend fun performConsolidatedStartupSync(
        context: Context,
        operationsCount: Int
    ): StartupDecision = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) {
            // Offline fallback
            if (UserDeviceTracker.isDeviceBlockedLocally(context)) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.DEVICE_BLOCKED,
                    blockReason = UserDeviceTracker.getBlockReason(context)
                )
            }
            if (PeriodicBackupReminderManager.shouldShowBackupReminder(context, operationsCount)) {
                return@withContext StartupDecision(dialogToShow = StartupDialogType.BACKUP_REMINDER)
            }
            return@withContext StartupDecision(dialogToShow = StartupDialogType.NONE)
        }

        try {
            // 1. Sync device presence & Google Account & check if blocked
            val presenceResult = UserDeviceTracker.syncPresence(context, operationsCount)
            if (presenceResult.isBlocked) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.DEVICE_BLOCKED,
                    blockReason = presenceResult.blockReason ?: "تم إيقاف التطبيق على هذا الجهاز"
                )
            }

            // 2. Check remote license revocation
            LicenseManager.checkRemoteRevocationInBackground(context)

            // 3. Fetch broadcasts, announcements, and force-update rules
            val broadcast = AppUpdateManager.checkBroadcastAndUpdates(context)
            if (broadcast != null && broadcast.isForceUpdate) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.FORCE_UPDATE,
                    forceUpdateBroadcast = broadcast
                )
            }

            // 4. Sync dynamic remote configuration policies (limits, feature toggles)
            RemoteConfigManager.syncRemotePolicies(context)

            // 5. Sync FCM Push Token quietly
            FcmTokenHelper.syncCurrentToken(context)

            // 6. Check priority of gentle dialogs (Only ONE dialog per session!)
            if (PeriodicBackupReminderManager.shouldShowBackupReminder(context, operationsCount)) {
                return@withContext StartupDecision(dialogToShow = StartupDialogType.BACKUP_REMINDER)
            }

            if (broadcast != null && !AppUpdateManager.isBroadcastSeen(context, broadcast.id)) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.ANNOUNCEMENT,
                    announcementBroadcast = broadcast
                )
            }

            StartupDecision(dialogToShow = StartupDialogType.NONE)
        } catch (e: Exception) {
            Log.e(TAG, "Startup sync error: ${e.message}")
            StartupDecision(dialogToShow = StartupDialogType.NONE)
        }
    }
}
