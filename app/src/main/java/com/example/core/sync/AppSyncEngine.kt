package com.example.core.sync

import android.content.Context
import android.util.Log
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import com.example.core.license.UserDeviceTracker
import com.example.core.notifications.FcmTokenHelper
import com.example.core.updates.AppBroadcast
import com.example.core.updates.AppUpdateManager
import com.example.core.util.AutoBackupEngine
import com.example.core.util.PeriodicBackupReminderManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

const val RESYNC_MIN_INTERVAL_MS = 10 * 60 * 1000L

/** True when at least 10 minutes passed since the last licence sync. */
fun shouldResync(lastSyncAt: Long, now: Long): Boolean = now - lastSyncAt >= RESYNC_MIN_INTERVAL_MS

object AppSyncEngine {
    private const val TAG = "AppSyncEngine"

    @Volatile private var lastLicenseSyncAt = 0L

    /** Emits when a foreground re-sync activated or extended the subscription. */
    val licenseChanged = kotlinx.coroutines.flow.MutableSharedFlow<LicenseManager.SyncChange>(extraBufferCapacity = 1)

    /** Re-runs licence sync and pushes any new operations when the app returns to the foreground. */
    suspend fun resyncOnResume(context: Context) = withContext(Dispatchers.IO) {
        if (LicenseManager.isInternetAvailable(context)) {
            runCatching {
                val exactOps = com.example.core.util.AppOperationsCounter.getExactTotalOperations(context)
                UserDeviceTracker.onNetworkAvailable(context, exactOps)
            }
        }
        val now = System.currentTimeMillis()
        if (!shouldResync(lastLicenseSyncAt, now)) return@withContext
        lastLicenseSyncAt = now
        val change = runCatching { LicenseManager.syncRemoteSubscriptionStatus(context) }
            .getOrDefault(LicenseManager.SyncChange.NONE)
        if (change != LicenseManager.SyncChange.NONE) licenseChanged.emit(change)
    }

    /**
     * Executes consolidated background synchronization in a single well-managed coroutine pass.
     * Prevents network storms, coordinates state, and returns a single clean decision on what dialog/screen to display.
     */
    private suspend fun resolveEffectiveOperationsCount(context: Context, hintCount: Int): Int {
        if (hintCount > 0) return hintCount
        return runCatching {
            val db = com.example.core.database.AppDatabase.getDatabase(context)
            db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
        }.getOrDefault(0)
    }

    suspend fun performConsolidatedStartupSync(
        context: Context,
        operationsCount: Int
    ): StartupDecision = withContext(Dispatchers.IO) {
        val resolvedOpsCount = resolveEffectiveOperationsCount(context, operationsCount)

        // 0. Trigger daily silent rolling backup (keeps last 7 days)
        AutoBackupEngine.performDailyAutoBackup(context, com.example.core.database.AppDatabase.getDatabase(context), resolvedOpsCount)

        // 0b. Keep the periodic Drive backup scheduled and catch up when it is overdue (never blocks startup)
        try {
            com.example.core.backup.DriveBackupScheduler.checkStartupDriveBackup(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Drive backup scheduling failed: ${e.message}")
        }

        // Register smart connectivity listener so offline launches get registered as soon as internet appears
        registerNetworkListener(context, resolvedOpsCount)

        if (!LicenseManager.isInternetAvailable(context)) {
            // Offline fallback
            if (UserDeviceTracker.isDeviceBlockedLocally(context)) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.DEVICE_BLOCKED,
                    blockReason = UserDeviceTracker.getBlockReason(context)
                )
            }
            if (PeriodicBackupReminderManager.shouldShowBackupReminder(context, resolvedOpsCount)) {
                return@withContext StartupDecision(dialogToShow = StartupDialogType.BACKUP_REMINDER)
            }
            return@withContext StartupDecision(dialogToShow = StartupDialogType.NONE)
        }

        try {
            // 1. Sync device presence & Google Account & check if blocked
            val presenceResult = UserDeviceTracker.syncPresence(context, resolvedOpsCount)
            if (presenceResult.isBlocked) {
                return@withContext StartupDecision(
                    dialogToShow = StartupDialogType.DEVICE_BLOCKED,
                    blockReason = presenceResult.blockReason ?: "تم إيقاف التطبيق على هذا الجهاز"
                )
            }

            // 2. Check remote license revocation, auto-activation, and extension
            lastLicenseSyncAt = System.currentTimeMillis()
            LicenseManager.syncRemoteSubscriptionStatus(context)

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
            if (PeriodicBackupReminderManager.shouldShowBackupReminder(context, resolvedOpsCount)) {
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

    private var isListenerRegistered = false
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun registerNetworkListener(context: Context, initialOperationsCount: Int = 0) {
        if (isListenerRegistered) return
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                private var lastTrigger = 0L

                override fun onAvailable(network: Network) {
                    val now = System.currentTimeMillis()
                    if (now - lastTrigger < 3000L) return
                    lastTrigger = now

                    syncScope.launch {
                        try {
                            UserDeviceTracker.onNetworkAvailable(appContext, initialOperationsCount)
                        } catch (e: Exception) {
                            Log.w(TAG, "Network listener onAvailable sync error: ${e.message}")
                        }
                    }
                }
            })
            isListenerRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Network callback registration failed: ${e.message}")
        }
    }
}
