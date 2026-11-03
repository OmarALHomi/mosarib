package com.example.core.speech

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.license.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeechAccessTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val sp = context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
    }

    @Test
    fun testVoiceStatementEntitlementTracksActivation() {
        // Free / unactivated state: voice statement locked
        assertFalse(LicenseManager.isActivated(context))
        assertFalse(SpeechAccess.canSendVoiceStatement(context))

        activateLocallyForTest(context)
        assertTrue(LicenseManager.isActivated(context))
        assertTrue(SpeechAccess.canSendVoiceStatement(context))
    }

    private fun activateLocallyForTest(context: Context) {
        val deviceCode = LicenseManager.getDeviceCode(context)
        val plan = "LIFETIME"
        val expiresAt = System.currentTimeMillis() + 365L * 24 * 3600 * 1000
        val salt = "mosarib_secure_license_secret_key_alhomi_2026_water_app"
        val input = "${deviceCode}${salt}${plan}${expiresAt}"
        val md = MessageDigest.getInstance("SHA-256")
        val sig = md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02X".format(it) }

        val sp = context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("is_app_activated", true)
            .putBoolean("is_license_revoked_locally", false)
            .putString("subscription_plan", plan)
            .putLong("subscription_expires_at", expiresAt)
            .putString("activation_signature", sig)
            .putLong("last_known_safe_timestamp", 0L)
            .commit()
    }
}
