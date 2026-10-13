package com.example.core.license

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages remote policies, dynamic free operations limit, and per-device overrides configured
 * via the admin dashboard (Firebase Realtime Database nodes: /app_config and /device_overrides).
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    private const val FIREBASE_DB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app"
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
     * Checks if local backup/restore is allowed for this device.
     * Always true for subscribed users; restricted if admin locked it globally or per-device.
     */
    fun isBackupAllowed(context: Context): Boolean {
        if (LicenseManager.isActivated(context)) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)) return false
        return sp.getBoolean(KEY_IS_BACKUP_ALLOWED_FOR_TRIAL, true)
    }

    /**
     * Checks if Google Drive cloud backup is allowed for this device.
     */
    fun isCloudBackupAllowed(context: Context): Boolean {
        if (LicenseManager.isActivated(context)) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DEVICE_BACKUP_LOCKED, false)) return false
        return sp.getBoolean(KEY_IS_CLOUD_BACKUP_ALLOWED, true)
    }

    /**
     * Checks if Excel export is allowed for this device.
     */
    fun isExcelExportAllowed(context: Context): Boolean {
        if (LicenseManager.isActivated(context)) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
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
     * Syncs remote policies and device overrides from Firebase Realtime Database.
     */
    suspend fun syncRemotePolicies(context: Context) = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) return@withContext

        val deviceCode = LicenseManager.getDeviceCode(context)
        val cleanDeviceKey = deviceCode.replace("-", "").trim()
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        try {
            // 1. Fetch Global App Config (/app_config.json)
            val cfgUrl = URL("$FIREBASE_DB_URL/app_config.json")
            val cfgConn = (cfgUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            if (cfgConn.responseCode == 200) {
                val text = cfgConn.inputStream.bufferedReader().readText().trim()
                if (text != "null" && text.isNotBlank()) {
                    val obj = JSONObject(text)
                    val freeLimit = obj.optInt("freeOperationsLimit", LicenseManager.FREE_OPERATIONS_LIMIT)
                    val bkAllowed = obj.optBoolean("isBackupAllowedForTrial", true)
                    val driveAllowed = obj.optBoolean("isCloudBackupAllowed", true)
                    val excelAllowed = obj.optBoolean("isExcelExportAllowedForTrial", true)
                    val note = obj.optString("globalAnnouncementText", "")

                    sp.edit()
                        .putInt(KEY_FREE_OPS_LIMIT, freeLimit)
                        .putBoolean(KEY_IS_BACKUP_ALLOWED_FOR_TRIAL, bkAllowed)
                        .putBoolean(KEY_IS_CLOUD_BACKUP_ALLOWED, driveAllowed)
                        .putBoolean(KEY_IS_EXCEL_EXPORT_ALLOWED, excelAllowed)
                        .putString(KEY_GLOBAL_ANNOUNCEMENT, note)
                        .apply()
                }
            }

            // 2. Fetch Device Specific Overrides (/device_overrides/{cleanDeviceKey}.json)
            val devUrl = URL("$FIREBASE_DB_URL/device_overrides/$cleanDeviceKey.json")
            val devConn = (devUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            if (devConn.responseCode == 200) {
                val text = devConn.inputStream.bufferedReader().readText().trim()
                if (text != "null" && text.isNotBlank()) {
                    val obj = JSONObject(text)
                    val isBkLocked = obj.optBoolean("isBackupLocked", false)
                    val customLimit = obj.optInt("customOperationsLimit", 0)
                    val warningMsg = obj.optString("specialWarningMessage", "")

                    sp.edit()
                        .putBoolean(KEY_DEVICE_BACKUP_LOCKED, isBkLocked)
                        .putInt(KEY_DEVICE_CUSTOM_LIMIT, customLimit)
                        .putString(KEY_DEVICE_SPECIAL_WARNING, warningMsg)
                        .apply()
                } else {
                    sp.edit()
                        .putBoolean(KEY_DEVICE_BACKUP_LOCKED, false)
                        .putInt(KEY_DEVICE_CUSTOM_LIMIT, 0)
                        .putString(KEY_DEVICE_SPECIAL_WARNING, "")
                        .apply()
                }
            }

            sp.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "syncRemotePolicies failed: ${e.message}")
        }
    }
}
