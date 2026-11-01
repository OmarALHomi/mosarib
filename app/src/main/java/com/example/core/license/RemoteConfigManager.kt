package com.example.core.license

import android.content.Context
import android.util.Log
import com.example.core.network.FirebaseRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Manages remote policies, dynamic free operations limit, and per-device overrides configured
 * via the admin dashboard (Firebase Realtime Database nodes: /app_config and /device_overrides).
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    private const val PREFS_FILE = "mosarib_remote_config_prefs"

    private const val KEY_FREE_OPS_LIMIT = "remote_free_ops_limit"
    private const val KEY_IS_BACKUP_ALLOWED_FOR_TRIAL = "is_backup_allowed_for_trial"
    private const val KEY_IS_CLOUD_BACKUP_ALLOWED = "is_cloud_backup_allowed"
    private const val KEY_IS_EXCEL_EXPORT_ALLOWED = "is_excel_export_allowed_for_trial"
    private const val KEY_GLOBAL_ANNOUNCEMENT = "global_announcement_text"
    private const val KEY_DEVICE_BACKUP_LOCKED = "device_override_backup_locked"
    private const val KEY_DEVICE_CUSTOM_LIMIT = "device_override_custom_limit"
    private const val KEY_DEVICE_SPECIAL_WARNING = "device_override_special_warning"
    private const val KEY_LAST_SYNC_TIME = "remote_config_last_sync_time"

    /**
     * Resolves the effective free operations limit for this device:
     * Checks device-specific custom limit first, then server app_config limit, then local fallback (200).
     */
    fun getFreeOperationsLimit(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val customLimit = sp.getInt(KEY_DEVICE_CUSTOM_LIMIT, 0)
        if (customLimit > 0) return customLimit
        return sp.getInt(KEY_FREE_OPS_LIMIT, LicenseManager.FREE_OPERATIONS_LIMIT)
    }

    /**
     * Returns explicit custom operations limit set specifically for this device (0 if not set).
     */
    fun getCustomDeviceLimit(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_DEVICE_CUSTOM_LIMIT, 0)
    }

    /**
     * Returns true if backup was specifically locked for this device by admin.
     */
    fun isDeviceBackupLocked(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)
    }

    /**
     * Saves per-device backup lock status directly into preferences.
     */
    fun setDeviceBackupLocked(context: Context, locked: Boolean) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_DEVICE_BACKUP_LOCKED, locked).apply()
    }

    /**
     * Saves per-device custom limit directly into preferences.
     */
    fun setDeviceCustomLimit(context: Context, limit: Int) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_DEVICE_CUSTOM_LIMIT, limit).apply()
    }

    /**
     * Checks if local backup/restore is allowed for this device.
     * Always true for subscribed users unless explicitly locked for this device.
     */
    fun isBackupAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)) return false
        if (LicenseManager.isActivated(context)) return true
        return sp.getBoolean(KEY_IS_BACKUP_ALLOWED_FOR_TRIAL, true)
    }

    /**
     * Checks if Google Drive cloud backup is allowed for this device.
     */
    fun isCloudBackupAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)) return false
        if (LicenseManager.isActivated(context)) return true
        return sp.getBoolean(KEY_IS_CLOUD_BACKUP_ALLOWED, true)
    }

    /**
     * Checks if Excel export is allowed for this device.
     */
    fun isExcelExportAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)) return false
        if (LicenseManager.isActivated(context)) return true
        return sp.getBoolean(KEY_IS_EXCEL_EXPORT_ALLOWED, true)
    }

    /**
     * Gets custom warning message specifically targeted to this device by admin.
     */
    fun getSpecialWarningMessage(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val msg = sp.getString(KEY_DEVICE_SPECIAL_WARNING, null)
        return if (msg.isNullOrBlank()) null else msg
    }

    /**
     * Gets global announcement text broadcasted to trial users.
     */
    fun getGlobalAnnouncement(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val note = sp.getString(KEY_GLOBAL_ANNOUNCEMENT, null)
        return if (note.isNullOrBlank()) null else note
    }

    /**
     * The custom operations limit an admin set on a policy node. Older dashboard versions used
     * different field names, so all of them are accepted.
     */
    internal fun readCustomLimit(policy: JSONObject): Int =
        policy.optInt("customOperationsLimit", policy.optInt("freeOperationsLimit", policy.optInt("operationsLimit", policy.optInt("limit", 0))))

    /**
     * Syncs remote policies and device overrides from Firebase Realtime Database.
     */
    suspend fun syncRemotePolicies(context: Context) = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) return@withContext

        val cleanDeviceKey = LicenseManager.getDeviceCode(context).replace("-", "").trim()
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        try {
            // 1. Global app config (/app_config)
            FirebaseRest.get("app_config.json", timeoutMs = 4000).jsonObject()?.let { obj ->
                sp.edit()
                    .putInt(KEY_FREE_OPS_LIMIT, obj.optInt("freeOperationsLimit", LicenseManager.FREE_OPERATIONS_LIMIT))
                    .putBoolean(KEY_IS_BACKUP_ALLOWED_FOR_TRIAL, obj.optBoolean("isBackupAllowedForTrial", true))
                    .putBoolean(KEY_IS_CLOUD_BACKUP_ALLOWED, obj.optBoolean("isCloudBackupAllowed", true))
                    .putBoolean(KEY_IS_EXCEL_EXPORT_ALLOWED, obj.optBoolean("isExcelExportAllowedForTrial", true))
                    .putString(KEY_GLOBAL_ANNOUNCEMENT, obj.optString("globalAnnouncementText", ""))
                    .apply()
            }

            // 2. Device specific overrides (/device_overrides/{key})
            FirebaseRest.get("device_overrides/$cleanDeviceKey.json", timeoutMs = 4000).jsonObject()?.let { obj ->
                sp.edit()
                    .putBoolean(KEY_DEVICE_BACKUP_LOCKED, obj.optBoolean("isBackupLocked", obj.optBoolean("backupLocked", false)))
                    .putInt(KEY_DEVICE_CUSTOM_LIMIT, readCustomLimit(obj))
                    .putString(KEY_DEVICE_SPECIAL_WARNING, obj.optString("specialWarningMessage", ""))
                    .apply()
            }

            // 3. The same admin fields may also be set on the device's /all_users record
            FirebaseRest.get("all_users/$cleanDeviceKey.json", timeoutMs = 4000).jsonObject()?.let { obj ->
                val editor = sp.edit()
                if (obj.has("isBackupLocked") || obj.has("backupLocked")) {
                    editor.putBoolean(KEY_DEVICE_BACKUP_LOCKED, obj.optBoolean("isBackupLocked", obj.optBoolean("backupLocked", false)))
                }
                val customLimit = readCustomLimit(obj)
                if (customLimit > 0) editor.putInt(KEY_DEVICE_CUSTOM_LIMIT, customLimit)
                if (obj.has("specialWarningMessage")) {
                    editor.putString(KEY_DEVICE_SPECIAL_WARNING, obj.optString("specialWarningMessage", ""))
                }
                editor.apply()
            }

            sp.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "syncRemotePolicies failed: ${e.message}")
        }
    }
}
