package com.example.core.license

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.core.network.FirebaseRest
import com.example.core.widget.MosaribQuickWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Effective remote policy resolved with precedence:
 * device override > versionPolicies[versionCode] > global /app_config > defaults.
 */
data class EffectiveRemotePolicy(
    val freeOperationsLimit: Int = LicenseManager.FREE_OPERATIONS_LIMIT,
    val globalFreeOperationsLimit: Int = LicenseManager.FREE_OPERATIONS_LIMIT,
    val versionFreeOperationsLimit: Int = 0,
    val deviceCustomLimit: Int = 0,
    val isBackupAllowedForTrial: Boolean = true,
    val isCloudBackupAllowed: Boolean = true,
    val isExcelExportAllowedForTrial: Boolean = true,
    val isDeviceBackupLocked: Boolean = false,
    val allowVoiceInput: Boolean = true,
    val allowVoiceReadout: Boolean = true,
    val allowPdfExport: Boolean = true,
    val allowHomeWidget: Boolean = true,
    val globalAnnouncementText: String = "",
    val specialWarningMessage: String = ""
)

/**
 * Manages remote policies, dynamic free operations limit, per-version feature flags, and per-device overrides configured
 * via the admin dashboard (Firebase Realtime Database nodes: /app_config and /device_overrides).
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    private const val PREFS_FILE = "mosarib_remote_config_prefs"
    const val FEATURE_DISABLED_MESSAGE = "هذه الميزة مغلقة مؤقتاً لهذه النسخة من قِبل الإدارة"

    private const val KEY_FREE_OPS_LIMIT = "remote_free_ops_limit"
    private const val KEY_VERSION_FREE_OPS_LIMIT = "version_policy_free_ops_limit"
    private const val KEY_IS_BACKUP_ALLOWED_FOR_TRIAL = "is_backup_allowed_for_trial"
    private const val KEY_IS_CLOUD_BACKUP_ALLOWED = "is_cloud_backup_allowed"
    private const val KEY_IS_EXCEL_EXPORT_ALLOWED = "is_excel_export_allowed_for_trial"
    private const val KEY_ALLOW_VOICE_INPUT = "version_policy_allow_voice_input"
    private const val KEY_ALLOW_VOICE_READOUT = "version_policy_allow_voice_readout"
    private const val KEY_ALLOW_PDF_EXPORT = "version_policy_allow_pdf_export"
    private const val KEY_ALLOW_HOME_WIDGET = "version_policy_allow_home_widget"
    private const val KEY_GLOBAL_ANNOUNCEMENT = "global_announcement_text"
    private const val KEY_DEVICE_BACKUP_LOCKED = "device_override_backup_locked"
    private const val KEY_DEVICE_CUSTOM_LIMIT = "device_override_custom_limit"
    private const val KEY_DEVICE_SPECIAL_WARNING = "device_override_special_warning"
    private const val KEY_LAST_SYNC_TIME = "remote_config_last_sync_time"

    /**
     * Resolves the effective free operations limit for this device:
     * Checks device-specific custom limit first, then per-version policy limit,
     * then server app_config limit, then local fallback (200).
     */
    fun getFreeOperationsLimit(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val customLimit = sp.getInt(KEY_DEVICE_CUSTOM_LIMIT, 0)
        if (customLimit > 0) return customLimit
        val versionLimit = sp.getInt(KEY_VERSION_FREE_OPS_LIMIT, 0)
        if (versionLimit > 0) return versionLimit
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

    fun isVoiceInputAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ALLOW_VOICE_INPUT, true)
    }

    fun isVoiceReadoutAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ALLOW_VOICE_READOUT, true)
    }

    fun isPdfExportAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ALLOW_PDF_EXPORT, true)
    }

    fun isHomeWidgetAllowed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ALLOW_HOME_WIDGET, true)
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

    private fun optBoolLayered(key: String, default: Boolean, vararg layers: JSONObject?): Boolean {
        for (layer in layers) {
            if (layer != null && layer.has(key) && !layer.isNull(key)) {
                return layer.optBoolean(key, default)
            }
        }
        return default
    }

    private fun cleanString(obj: JSONObject?, key: String): String {
        val raw = obj?.optString(key, "").orEmpty()
        return if (raw == "null") "" else raw
    }

    /**
     * Pure policy resolver with precedence:
     * [deviceOverrideObj] > [versionObj] > [globalObj] > defaults.
     */
    fun resolveEffectivePolicy(
        globalObj: JSONObject?,
        versionObj: JSONObject?,
        deviceOverrideObj: JSONObject? = null
    ): EffectiveRemotePolicy {
        val globalLimit = globalObj?.optInt("freeOperationsLimit", LicenseManager.FREE_OPERATIONS_LIMIT)
            ?.takeIf { it > 0 } ?: LicenseManager.FREE_OPERATIONS_LIMIT
        val versionLimit = versionObj?.let { readCustomLimit(it) }?.takeIf { it > 0 } ?: 0
        val deviceLimit = deviceOverrideObj?.let { readCustomLimit(it) }?.takeIf { it > 0 } ?: 0
        val effectiveLimit = when {
            deviceLimit > 0 -> deviceLimit
            versionLimit > 0 -> versionLimit
            else -> globalLimit
        }
        val isBackupLocked = deviceOverrideObj?.let {
            it.optBoolean("isBackupLocked", it.optBoolean("backupLocked", false))
        } ?: false

        return EffectiveRemotePolicy(
            freeOperationsLimit = effectiveLimit,
            globalFreeOperationsLimit = globalLimit,
            versionFreeOperationsLimit = versionLimit,
            deviceCustomLimit = deviceLimit,
            isBackupAllowedForTrial = optBoolLayered("isBackupAllowedForTrial", true, versionObj, globalObj),
            isCloudBackupAllowed = optBoolLayered("isCloudBackupAllowed", true, versionObj, globalObj),
            isExcelExportAllowedForTrial = optBoolLayered("isExcelExportAllowedForTrial", true, versionObj, globalObj),
            isDeviceBackupLocked = isBackupLocked,
            allowVoiceInput = optBoolLayered("allowVoiceInput", true, deviceOverrideObj, versionObj, globalObj),
            allowVoiceReadout = optBoolLayered("allowVoiceReadout", true, deviceOverrideObj, versionObj, globalObj),
            allowPdfExport = optBoolLayered("allowPdfExport", true, deviceOverrideObj, versionObj, globalObj),
            allowHomeWidget = optBoolLayered("allowHomeWidget", true, deviceOverrideObj, versionObj, globalObj),
            globalAnnouncementText = cleanString(globalObj, "globalAnnouncementText"),
            specialWarningMessage = cleanString(deviceOverrideObj, "specialWarningMessage")
        )
    }

    /** Convenience helper that looks up versionPolicies/{versionCode} from [globalObj]. */
    fun resolveEffectivePolicyForVersion(
        globalObj: JSONObject?,
        versionCode: Int,
        deviceOverrideObj: JSONObject? = null
    ): EffectiveRemotePolicy {
        val versionObj = globalObj?.optJSONObject("versionPolicies")?.optJSONObject(versionCode.toString())
        return resolveEffectivePolicy(globalObj, versionObj, deviceOverrideObj)
    }

    /**
     * Syncs remote policies and device overrides from Firebase Realtime Database.
     */
    suspend fun syncRemotePolicies(context: Context) = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) return@withContext

        val cleanDeviceKey = LicenseManager.getDeviceCode(context).replace("-", "").trim()
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        try {
            // 1. Global app config + per-version policies (/app_config) in ONE HTTP call
            val appConfigResp = FirebaseRest.get("app_config.json", timeoutMs = 4000)
            if (appConfigResp.isSuccess) {
                val globalObj = appConfigResp.jsonObject()
                val resolved = resolveEffectivePolicyForVersion(globalObj, BuildConfig.VERSION_CODE)
                sp.edit().apply {
                    putInt(KEY_FREE_OPS_LIMIT, resolved.globalFreeOperationsLimit)
                    if (resolved.versionFreeOperationsLimit > 0) {
                        putInt(KEY_VERSION_FREE_OPS_LIMIT, resolved.versionFreeOperationsLimit)
                    } else {
                        remove(KEY_VERSION_FREE_OPS_LIMIT)
                    }
                    putBoolean(KEY_IS_BACKUP_ALLOWED_FOR_TRIAL, resolved.isBackupAllowedForTrial)
                    putBoolean(KEY_IS_CLOUD_BACKUP_ALLOWED, resolved.isCloudBackupAllowed)
                    putBoolean(KEY_IS_EXCEL_EXPORT_ALLOWED, resolved.isExcelExportAllowedForTrial)
                    putBoolean(KEY_ALLOW_VOICE_INPUT, resolved.allowVoiceInput)
                    putBoolean(KEY_ALLOW_VOICE_READOUT, resolved.allowVoiceReadout)
                    putBoolean(KEY_ALLOW_PDF_EXPORT, resolved.allowPdfExport)
                    putBoolean(KEY_ALLOW_HOME_WIDGET, resolved.allowHomeWidget)
                    putString(KEY_GLOBAL_ANNOUNCEMENT, resolved.globalAnnouncementText)
                }.apply()
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
            runCatching { MosaribQuickWidgetProvider.requestWidgetRefresh(context) }
        } catch (e: Exception) {
            Log.w(TAG, "syncRemotePolicies failed: ${e.message}")
        }
    }
}
