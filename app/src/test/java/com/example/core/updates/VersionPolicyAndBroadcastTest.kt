package com.example.core.updates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VersionPolicyAndBroadcastTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mosarib_remote_config_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun isUpdateBroadcast_onlyTrueForUpdateTypeOrForceFlag() {
        assertFalse(AppUpdateManager.isUpdateBroadcast("INFO", isForceUpdate = false))
        assertFalse(AppUpdateManager.isUpdateBroadcast("WARNING", isForceUpdate = false))
        assertFalse(AppUpdateManager.isUpdateBroadcast("MAINTENANCE", isForceUpdate = false))
        assertTrue(AppUpdateManager.isUpdateBroadcast("UPDATE", isForceUpdate = false))
        assertTrue(AppUpdateManager.isUpdateBroadcast(" update ", isForceUpdate = false))
        assertTrue(AppUpdateManager.isUpdateBroadcast("INFO", isForceUpdate = true))
        assertTrue(AppUpdateManager.isUpdateBroadcast("WARNING", isForceUpdate = true))
    }

    @Test
    fun nonUpdateBroadcast_ignoresStaleMinRequiredVersionCode() {
        val infoMin = AppUpdateManager.resolveEffectiveMinVersion("INFO", isForceUpdate = false, rawMinVersion = 99)
        val warnMin = AppUpdateManager.resolveEffectiveMinVersion("WARNING", isForceUpdate = false, rawMinVersion = 15)
        assertEquals(1, infoMin)
        assertEquals(1, warnMin)
        assertFalse(AppUpdateManager.shouldForceUpdate(currentCode = 8, minCode = infoMin, isForceUpdateFlag = false))

        // Update broadcasts and forced broadcasts preserve minRequiredVersionCode
        assertEquals(15, AppUpdateManager.resolveEffectiveMinVersion("UPDATE", isForceUpdate = false, rawMinVersion = 15))
        assertEquals(15, AppUpdateManager.resolveEffectiveMinVersion("INFO", isForceUpdate = true, rawMinVersion = 15))
        assertTrue(AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = 5, isForceUpdateFlag = false))
    }

    @Test
    fun resolveEffectivePolicy_appliesPrecedenceDeviceOverVersionOverGlobal() {
        val globalJson = JSONObject(
            """
            {
              "freeOperationsLimit": 200,
              "isBackupAllowedForTrial": true,
              "isCloudBackupAllowed": true,
              "isExcelExportAllowedForTrial": true,
              "versionPolicies": {
                "8": {
                  "freeOperationsLimit": 50,
                  "isBackupAllowedForTrial": false,
                  "allowVoiceInput": false,
                  "allowVoiceReadout": true,
                  "allowPdfExport": false,
                  "allowHomeWidget": false
                }
              }
            }
            """.trimIndent()
        )

        // Version 8 gets per-version limit (50) and disabled voice input / pdf / widget
        val v8Policy = RemoteConfigManager.resolveEffectivePolicyForVersion(globalJson, versionCode = 8)
        assertEquals(50, v8Policy.freeOperationsLimit)
        assertEquals(200, v8Policy.globalFreeOperationsLimit)
        assertEquals(50, v8Policy.versionFreeOperationsLimit)
        assertFalse(v8Policy.isBackupAllowedForTrial)
        assertTrue(v8Policy.isCloudBackupAllowed)
        assertFalse(v8Policy.allowVoiceInput)
        assertTrue(v8Policy.allowVoiceReadout)
        assertFalse(v8Policy.allowPdfExport)
        assertFalse(v8Policy.allowHomeWidget)

        // Version 9 (not in versionPolicies) falls back to global 200 and defaults true
        val v9Policy = RemoteConfigManager.resolveEffectivePolicyForVersion(globalJson, versionCode = 9)
        assertEquals(200, v9Policy.freeOperationsLimit)
        assertEquals(0, v9Policy.versionFreeOperationsLimit)
        assertTrue(v9Policy.isBackupAllowedForTrial)
        assertTrue(v9Policy.allowVoiceInput)
        assertTrue(v9Policy.allowPdfExport)
        assertTrue(v9Policy.allowHomeWidget)

        // Device override (customOperationsLimit = 100) overrides version 8's 50, while preserving version feature flags
        val deviceOverride = JSONObject("""{"customOperationsLimit": 100, "isBackupLocked": true}""")
        val deviceResolved = RemoteConfigManager.resolveEffectivePolicyForVersion(
            globalJson,
            versionCode = 8,
            deviceOverrideObj = deviceOverride
        )
        assertEquals(100, deviceResolved.freeOperationsLimit)
        assertEquals(100, deviceResolved.deviceCustomLimit)
        assertTrue(deviceResolved.isDeviceBackupLocked)
        assertFalse(deviceResolved.allowVoiceInput)
        assertFalse(deviceResolved.allowPdfExport)
    }

    @Test
    fun resolveEffectivePolicy_handlesDeletionFallbackCleanly() {
        val globalOnly = JSONObject("""{"freeOperationsLimit": 200}""")
        val deletedVersionPolicy = RemoteConfigManager.resolveEffectivePolicy(
            globalObj = globalOnly,
            versionObj = null,
            deviceOverrideObj = null
        )
        assertEquals(200, deletedVersionPolicy.freeOperationsLimit)
        assertEquals(0, deletedVersionPolicy.versionFreeOperationsLimit)
        assertTrue(deletedVersionPolicy.allowVoiceInput)
        assertTrue(deletedVersionPolicy.allowVoiceReadout)
        assertTrue(deletedVersionPolicy.allowPdfExport)
        assertTrue(deletedVersionPolicy.allowHomeWidget)

        // Null global falls back to LicenseManager default
        val allNull = RemoteConfigManager.resolveEffectivePolicy(null, null, null)
        assertEquals(LicenseManager.FREE_OPERATIONS_LIMIT, allNull.freeOperationsLimit)
        assertTrue(allNull.allowVoiceInput)
    }

    @Test
    fun remoteConfigPrefs_precedenceAndFeatureGetters() {
        // Default values
        assertEquals(LicenseManager.FREE_OPERATIONS_LIMIT, RemoteConfigManager.getFreeOperationsLimit(context))
        assertTrue(RemoteConfigManager.isVoiceInputAllowed(context))
        assertTrue(RemoteConfigManager.isVoiceReadoutAllowed(context))
        assertTrue(RemoteConfigManager.isPdfExportAllowed(context))
        assertTrue(RemoteConfigManager.isHomeWidgetAllowed(context))

        val sp = context.getSharedPreferences("mosarib_remote_config_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putInt("remote_free_ops_limit", 200)
            .putInt("version_policy_free_ops_limit", 50)
            .putBoolean("version_policy_allow_voice_input", false)
            .putBoolean("version_policy_allow_pdf_export", false)
            .commit()

        assertEquals(50, RemoteConfigManager.getFreeOperationsLimit(context))
        assertFalse(RemoteConfigManager.isVoiceInputAllowed(context))
        assertTrue(RemoteConfigManager.isVoiceReadoutAllowed(context))
        assertFalse(RemoteConfigManager.isPdfExportAllowed(context))

        // Device custom limit takes precedence over version limit
        RemoteConfigManager.setDeviceCustomLimit(context, 100)
        assertEquals(100, RemoteConfigManager.getFreeOperationsLimit(context))

        // Removing version policy limit falls back to global 200 once device override is 0
        RemoteConfigManager.setDeviceCustomLimit(context, 0)
        sp.edit().remove("version_policy_free_ops_limit").commit()
        assertEquals(200, RemoteConfigManager.getFreeOperationsLimit(context))
    }
}
