package com.example.core.license

import android.content.Context
import android.util.Log
import com.example.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tracks every user/device installing or opening the app, whether subscribed or trial.
 * Syncs Google Account email, device ID, operation count, and checks if device or email is blocked.
 */
object UserDeviceTracker {
    private const val TAG = "UserDeviceTracker"
    private const val FIREBASE_DB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val PREFS_FILE = "mosarib_license_prefs"
    private const val KEY_IS_BLOCKED_LOCALLY = "is_device_blocked_by_admin"
    private const val KEY_BLOCK_REASON = "device_block_reason"
    private const val KEY_REGISTERED_NAME = "user_registered_name"
    private const val KEY_REGISTERED_PHONE = "user_registered_phone"

    data class DeviceStatusResult(
        val isBlocked: Boolean,
        val blockReason: String? = null
    )

    private const val KEY_IS_GOOGLE_ACCOUNT_AUTHED = "is_google_account_authenticated_for_app"

    fun setGoogleAccountAuthenticatedForApp(context: Context, isAuthed: Boolean) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_IS_GOOGLE_ACCOUNT_AUTHED, isAuthed).apply()
    }

    fun isGoogleAccountAuthenticatedForApp(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_IS_GOOGLE_ACCOUNT_AUTHED, false)
    }

    fun saveUserRegistrationInfo(context: Context, name: String, phone: String) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit()
            .putString(KEY_REGISTERED_NAME, name.trim())
            .putString(KEY_REGISTERED_PHONE, phone.trim())
            .apply()
    }

    fun getRegisteredName(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_REGISTERED_NAME, "") ?: ""
    }

    fun getRegisteredPhone(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_REGISTERED_PHONE, "") ?: ""
    }

    /**
     * Checks if this device or user has been flagged/blocked by admin.
     */
    fun isDeviceBlockedLocally(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_IS_BLOCKED_LOCALLY, false)
    }

    fun getBlockReason(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_BLOCK_REASON, "تم إيقاف التطبيق على هذا الجهاز من قِبل الإدارة لمخالفة شروط الاستخدام أو التحايل.") ?: ""
    }

    /**
     * Registers or updates this device presence in Firebase with Google Account info (if signed in),
     * operations count, activation status, and syncs block status.
     */
    suspend fun syncPresence(context: Context, operationsCount: Int): DeviceStatusResult = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) {
            return@withContext DeviceStatusResult(isDeviceBlockedLocally(context), getBlockReason(context))
        }

        // Concurrently refresh remote policies
        RemoteConfigManager.syncRemotePolicies(context)

        // Ensure exact database count is counted
        val exactCount = com.example.core.util.AppOperationsCounter.getExactTotalOperations(context)
        val finalOpsCount = maxOf(operationsCount, exactCount)

        val deviceCode = LicenseManager.getDeviceCode(context)
        val cleanDeviceKey = deviceCode.replace("-", "").trim()
        val isAuthed = isGoogleAccountAuthenticatedForApp(context)
        val googleAccount = if (isAuthed) GoogleSignIn.getLastSignedInAccount(context) else null
        val email = googleAccount?.email ?: ""
        val displayName = googleAccount?.displayName ?: ""
        val isActivated = LicenseManager.isActivated(context)
        val activePlan = LicenseManager.getActivePlan(context)?.name ?: (if (isActivated) "ACTIVATED" else "TRIAL")
        val now = System.currentTimeMillis()

        try {
            var isBlocked = false
            var blockReason = "تم حظر هذا الجهاز أو الحساب من قبل الإدارة"
            var remoteOpsCount = 0
            var remoteCumulativeOps = 0
            var remoteTrialConsumed = false

            // 1. Primary: Check Realtime Database (/all_users/{cleanDeviceKey}.json)
            try {
                val checkUrl = URL("$FIREBASE_DB_URL/all_users/$cleanDeviceKey.json")
                val conn = (checkUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseText = reader.readText().trim()
                    reader.close()
                    if (responseText != "null" && responseText.isNotBlank()) {
                        val existing = JSONObject(responseText)
                        if (existing.optBoolean("isBlocked", false)) {
                            isBlocked = true
                            blockReason = existing.optString("blockReason", blockReason)
                        }
                        if (existing.has("isBackupLocked") || existing.has("backupLocked")) {
                            val isBkLocked = existing.optBoolean("isBackupLocked", existing.optBoolean("backupLocked", false))
                            RemoteConfigManager.setDeviceBackupLocked(context, isBkLocked)
                        }
                        val customLim = existing.optInt("customOperationsLimit", existing.optInt("freeOperationsLimit", existing.optInt("operationsLimit", existing.optInt("limit", 0))))
                        if (customLim > 0) {
                            RemoteConfigManager.setDeviceCustomLimit(context, customLim)
                        }
                        remoteOpsCount = existing.optInt("operationsCount", 0)
                        remoteCumulativeOps = existing.optInt("cumulativeOpsCount", 0)
                        remoteTrialConsumed = existing.optBoolean("trialConsumed", false)
                    }
                }
            } catch (_: Exception) {}

            // 1.5. Also check Realtime Database (/device_overrides/{cleanDeviceKey}.json)
            try {
                val devUrl = URL("$FIREBASE_DB_URL/device_overrides/$cleanDeviceKey.json")
                val devConn = (devUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Accept", "application/json")
                }
                if (devConn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(devConn.inputStream))
                    val devText = reader.readText().trim()
                    reader.close()
                    if (devText != "null" && devText.isNotBlank()) {
                        val devObj = JSONObject(devText)
                        if (devObj.optBoolean("isBlocked", false)) {
                            isBlocked = true
                            blockReason = devObj.optString("blockReason", blockReason)
                        }
                        if (devObj.has("isBackupLocked") || devObj.has("backupLocked")) {
                            val isBkLocked = devObj.optBoolean("isBackupLocked", devObj.optBoolean("backupLocked", false))
                            RemoteConfigManager.setDeviceBackupLocked(context, isBkLocked)
                        }
                        val customLim = devObj.optInt("customOperationsLimit", devObj.optInt("freeOperationsLimit", devObj.optInt("operationsLimit", devObj.optInt("limit", 0))))
                        if (customLim > 0) {
                            RemoteConfigManager.setDeviceCustomLimit(context, customLim)
                        }
                    }
                }
            } catch (_: Exception) {}


            // Also check by Google Email if present
            if (!isBlocked && email.isNotBlank()) {
                val safeEmailKey = email.replace(".", "_").replace("@", "_at_")
                val emailCheckUrl = URL("$FIREBASE_DB_URL/blocked_emails/$safeEmailKey.json")
                val emailConn = (emailCheckUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (emailConn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(emailConn.inputStream))
                    val res = reader.readText().trim()
                    reader.close()
                    if (res != "null" && res.isNotBlank()) {
                        isBlocked = true
                        blockReason = "تم حظر حساب Google ($email) من قِبل الإدارة"
                    }
                }
            }

            // Save local block state
            val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            sp.edit()
                .putBoolean(KEY_IS_BLOCKED_LOCALLY, isBlocked)
                .putString(KEY_BLOCK_REASON, blockReason)
                .apply()

            if (isBlocked) {
                LicenseManager.revokeLicenseLocally(context)
                return@withContext DeviceStatusResult(isBlocked = true, blockReason = blockReason)
            }

            // Anti-cheat: restore consumed operations count from server if user cleared app data or reinstalled
            val effectiveLimit = LicenseManager.getFreeOperationsLimit(context)
            val effectiveRemoteOps = maxOf(remoteOpsCount, remoteCumulativeOps)
            if (remoteTrialConsumed || effectiveRemoteOps >= effectiveLimit) {
                LicenseManager.markTrialAsConsumed(context)
            }
            if (effectiveRemoteOps > 0) {
                LicenseManager.syncCumulativeOperations(context, effectiveRemoteOps)
            }

            val regName = getRegisteredName(context).ifBlank {
                LicenseManager.getActivationRecord(context)?.subscriberName ?: displayName
            }
            val regPhone = getRegisteredPhone(context)

            val highestOps = maxOf(finalOpsCount, remoteOpsCount)
            val cumulativeOps = maxOf(LicenseManager.getCumulativeOperations(context), effectiveRemoteOps, highestOps)
            val isTrialConsumed = LicenseManager.isTrialConsumed(context)

            // Only attach price if user is an activated genuine subscriber and not blocked
            val lastSyncedPrice = if (isActivated && !isBlocked) {
                com.example.core.util.AppPriceHelper.getLastSyncedPrice(context)
            } else null

            // 1. Upload / update user presence data
            val payload = JSONObject().apply {
                put("deviceCode", deviceCode)
                put("cleanDeviceKey", cleanDeviceKey)
                put("deviceModel", LicenseManager.getDeviceModelName())
                put("androidVersion", LicenseManager.getAndroidVersionName())
                put("googleEmail", email)
                put("googleDisplayName", displayName)
                put("subscriberName", regName)
                put("phone", regPhone)
                put("isActivated", isActivated)
                put("plan", activePlan)
                put("operationsCount", highestOps)
                put("cumulativeOpsCount", cumulativeOps)
                put("trialConsumed", isTrialConsumed)
                if (lastSyncedPrice != null) {
                    put("pricePerHour", lastSyncedPrice)
                    put("hourlyRate", lastSyncedPrice)
                }
                put("lastActiveAt", now)
            }

            // Realtime Database presence sync (Use POST+Override to preserve admin fields like isBlocked/blockReason)
            // HttpURLConnection does not support PATCH on all Android versions — use POST with override header
            try {
                val patchUrl = URL("$FIREBASE_DB_URL/all_users/$cleanDeviceKey.json")
                val patchConn = (patchUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
                OutputStreamWriter(patchConn.outputStream).use { it.write(payload.toString()) }
                val code = patchConn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "RTDB presence write failed: HTTP $code")
                }
            } catch (e: Exception) {
                Log.w(TAG, "RTDB presence write exception: ${e.message}")
            }
            DeviceStatusResult(isBlocked = false)
        } catch (e: Exception) {
            Log.e(TAG, "Presence sync failed: ${e.message}")
            DeviceStatusResult(isDeviceBlockedLocally(context), getBlockReason(context))
        }
    }
}
