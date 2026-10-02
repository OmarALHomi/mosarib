package com.baynana.core.license

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testDeviceCodeGeneration() {
        val code1 = LicenseManager.generateDeviceCodeFromId("android_test_id_1")
        val code2 = LicenseManager.generateDeviceCodeFromId("android_test_id_2")

        assertTrue(code1.startsWith("MSRB-"))
        assertTrue(code2.startsWith("MSRB-"))
        assertNotEquals(code1, code2)
    }

    @Test
    fun testActivationKeyGenerationForAllRoles() {
        val testDeviceCode = "MSRB-8F42-9D1B"

        val musribMonthly = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.MUSRIB, LicenseManager.SubscriptionPlan.MONTHLY)
        val musribYearly = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.MUSRIB, LicenseManager.SubscriptionPlan.YEARLY)
        val dallalMonthly = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.DALLAL, LicenseManager.SubscriptionPlan.MONTHLY)
        val buyerMonthly = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.BUYER, LicenseManager.SubscriptionPlan.MONTHLY)
        val farmerLifetime = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.FARMER, LicenseManager.SubscriptionPlan.LIFETIME)

        assertTrue(musribMonthly.startsWith("ACTV-M-"))
        assertTrue(musribYearly.startsWith("ACTV-Y-"))
        assertTrue(dallalMonthly.startsWith("DLLV-M-"))
        assertTrue(buyerMonthly.startsWith("BJRV-M-"))
        assertTrue(farmerLifetime.startsWith("FRMV-L-"))

        // Ensure keys are strictly distinct
        val allKeys = setOf(musribMonthly, musribYearly, dallalMonthly, buyerMonthly, farmerLifetime)
        assertEquals(5, allKeys.size)
    }

    @Test
    fun testResolveKeyStandardRoles() {
        val testDeviceCode = "MSRB-8F42-9D1B"

        val dallalKey = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.DALLAL, LicenseManager.SubscriptionPlan.YEARLY)
        val resolved = LicenseManager.resolveKey(testDeviceCode, dallalKey)
        assertNotNull(resolved)
        assertEquals(LicenseManager.LicenseRole.DALLAL, resolved!!.first)
        assertEquals(LicenseManager.SubscriptionPlan.YEARLY, resolved.second)

        val farmerKey = LicenseManager.generateActivationKey(testDeviceCode, LicenseManager.LicenseRole.FARMER, LicenseManager.SubscriptionPlan.LIFETIME)
        val resolvedFarmer = LicenseManager.resolveKey(testDeviceCode, farmerKey)
        assertNotNull(resolvedFarmer)
        assertEquals(LicenseManager.LicenseRole.FARMER, resolvedFarmer!!.first)
        assertEquals(LicenseManager.SubscriptionPlan.LIFETIME, resolvedFarmer.second)
    }

    @Test
    fun testResolveKeyLegacyBackwardCompatibility() {
        val testDeviceCode = "MSRB-8F42-9D1B"
        val clean = testDeviceCode.replace("-", "")

        // Legacy format 1: ACTV-M-XXXX-XXXX
        val legacy1Hash = LicenseManager.sha256(clean + "mosarib_secure_license_secret_key_alhomi_2026_water_app" + "M")
        val legacyKey1 = "ACTV-M-${legacy1Hash.substring(0, 4)}-${legacy1Hash.substring(4, 8)}"
        val res1 = LicenseManager.resolveKey(testDeviceCode, legacyKey1)
        assertNotNull("Legacy format 1 must resolve", res1)
        assertEquals(LicenseManager.LicenseRole.MUSRIB, res1!!.first)
        assertEquals(LicenseManager.SubscriptionPlan.MONTHLY, res1.second)

        // Legacy format 2: ACTV-XXXX-XXXX (no plan prefix)
        val legacy2Hash = LicenseManager.sha256(clean + "mosarib_secure_license_secret_key_alhomi_2026_water_app")
        val legacyKey2 = "ACTV-${legacy2Hash.substring(0, 4)}-${legacy2Hash.substring(4, 8)}"
        val res2 = LicenseManager.resolveKey(testDeviceCode, legacyKey2)
        assertNotNull("Legacy format 2 must resolve", res2)
        assertEquals(LicenseManager.LicenseRole.MUSRIB, res2!!.first)
        assertEquals(LicenseManager.SubscriptionPlan.MONTHLY, res2.second)
    }

    @Test
    fun testInvalidKeyOrDeviceCodeReturnsNull() {
        val deviceA = "MSRB-AAAA-1111"
        val deviceB = "MSRB-BBBB-2222"

        val keyForA = LicenseManager.generateActivationKey(deviceA, LicenseManager.LicenseRole.MUSRIB, LicenseManager.SubscriptionPlan.MONTHLY)
        val resolvedForB = LicenseManager.resolveKey(deviceB, keyForA)
        assertNull("Key generated for Device A must not be valid for Device B", resolvedForB)

        val bogusKey = "ACTV-M-ZZZZ-9999"
        assertNull(LicenseManager.resolveKey(deviceA, bogusKey))
    }

    @Test
    fun testVerifyAndActivateMultipleRoles() {
        val deviceCode = LicenseManager.getDeviceCode(context)

        // 1. Activate Musrib (ACTV)
        val musribKey = LicenseManager.generateActivationKey(deviceCode, LicenseManager.LicenseRole.MUSRIB, LicenseManager.SubscriptionPlan.MONTHLY)
        val musribResult = LicenseManager.verifyAndActivate(context, musribKey)
        assertNotNull(musribResult)
        assertEquals(LicenseManager.LicenseRole.MUSRIB, musribResult!!.role)
        assertTrue(LicenseManager.isRoleActivated(context, LicenseManager.LicenseRole.MUSRIB))
        assertTrue(LicenseManager.isActivated(context)) // Legacy check

        // Dallal is NOT activated yet
        assertFalse(LicenseManager.isRoleActivated(context, LicenseManager.LicenseRole.DALLAL))

        // 2. Activate Dallal (DLLV) independently
        val dallalKey = LicenseManager.generateActivationKey(deviceCode, LicenseManager.LicenseRole.DALLAL, LicenseManager.SubscriptionPlan.YEARLY)
        val dallalResult = LicenseManager.verifyAndActivate(context, dallalKey)
        assertNotNull(dallalResult)
        assertEquals(LicenseManager.LicenseRole.DALLAL, dallalResult!!.role)

        // Both roles are now active simultaneously
        assertTrue(LicenseManager.isRoleActivated(context, LicenseManager.LicenseRole.MUSRIB))
        assertTrue(LicenseManager.isRoleActivated(context, LicenseManager.LicenseRole.DALLAL))
        assertEquals(LicenseManager.SubscriptionPlan.MONTHLY, LicenseManager.getRoleActivePlan(context, LicenseManager.LicenseRole.MUSRIB))
        assertEquals(LicenseManager.SubscriptionPlan.YEARLY, LicenseManager.getRoleActivePlan(context, LicenseManager.LicenseRole.DALLAL))
    }

    @Test
    fun testFreeLaunchPeriodAndDynamicLimits() {
        // Without activation and default limits:
        assertFalse(LicenseManager.isFreeLaunchPeriodActive(context))
        assertTrue(LicenseManager.canPerformOperation(context, 199))
        assertFalse(LicenseManager.canPerformOperation(context, 200))

        // Activate free launch period dynamically:
        LicenseManager.updateSystemConfigLimits(context, isFreeLaunchPeriod = true)
        assertTrue(LicenseManager.isFreeLaunchPeriodActive(context))
        // Now Musrib can perform up to 2000 operations
        assertTrue(LicenseManager.canPerformOperation(context, 500))
        assertTrue(LicenseManager.canPerformOperation(context, 1999))
    }
}
