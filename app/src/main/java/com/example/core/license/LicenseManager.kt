package com.example.core.license

import android.content.Context
import android.net.Uri
import android.provider.Settings
import java.security.MessageDigest

/**
 * Offline-first license and device activation manager.
 * Limits free usage to [FREE_OPERATIONS_LIMIT] operations.
 * Binds activation permanently to the specific device hardware.
 */
object LicenseManager {

    const val FREE_OPERATIONS_LIMIT = 200
    const val DEVELOPER_PHONE = "967773712030"
    const val DEVELOPER_NAME = "المهندس عمر الحومي"
    const val DEVELOPER_EMAIL = "mralhwmy@gmail.com"
    const val DEVELOPER_WEBSITE = "https://omar.tubbasoft.com"
    const val COMPANY_WEBSITE = "https://tubbasoft.com"

    private const val PREFS_FILE = "mosarib_license_prefs"
    private const val KEY_IS_ACTIVATED = "is_app_activated"
    private const val KEY_ACTIVATION_SIGNATURE = "activation_signature"
    private const val KEY_ACTIVATED_AT = "activated_at"

    // Secret salts (never expose raw algorithms)
    private const val DEVICE_CODE_SALT = "msrb_device_token_salt_v1"
    private const val SECRET_ACTIVATION_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app"

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Extracts a clean, unique device code for the current phone.
     * Example: MSRB-8F42-9D1B
     */
    fun getDeviceCode(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
        val hash = sha256(androidId + DEVICE_CODE_SALT)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "MSRB-$part1-$part2"
    }

    /**
     * Mathematical generator to produce the activation key corresponding to a device code.
     * Can be used by the developer or within the app to verify keys.
     * Example: ACTV-A39F-B71C
     */
    fun generateActivationKey(deviceCode: String): String {
        val cleanCode = deviceCode.replace("-", "").trim().uppercase()
        val hash = sha256(cleanCode + SECRET_ACTIVATION_SALT)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "ACTV-$part1-$part2"
    }

    /**
     * Verifies the activation key entered by the user.
     * If valid, permanently activates the app for this device.
     */
    fun verifyAndActivate(context: Context, enteredKey: String): Boolean {
        val cleanEntered = enteredKey.replace("-", "").replace(" ", "").trim().uppercase()
        val currentDeviceCode = getDeviceCode(context)
        val expectedKey = generateActivationKey(currentDeviceCode).replace("-", "").uppercase()

        if (cleanEntered == expectedKey || cleanEntered == expectedKey.removePrefix("ACTV")) {
            val signature = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + "ACTIVATED_OK")
            val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            sp.edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putString(KEY_ACTIVATION_SIGNATURE, signature)
                .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
                .apply()
            return true
        }
        return false
    }

    /**
     * Returns true if this device is permanently activated.
     * Validates cryptographic signature against current device hardware.
     */
    fun isActivated(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val isActivated = sp.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivated) return false

        val storedSig = sp.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false
        val currentDeviceCode = getDeviceCode(context)
        val expectedSig = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + "ACTIVATED_OK")
        return storedSig == expectedSig
    }

    /**
     * Checks whether an operation (session or voucher) can be performed.
     * True if activated or if current total operations is below [FREE_OPERATIONS_LIMIT].
     */
    fun canPerformOperation(context: Context, currentOperationsCount: Int): Boolean {
        return isActivated(context) || currentOperationsCount < FREE_OPERATIONS_LIMIT
    }

    /**
     * Returns remaining free operations (0 if reached or unlimited if activated).
     */
    fun getRemainingOperations(context: Context, currentOperationsCount: Int): Int {
        if (isActivated(context)) return Int.MAX_VALUE
        return (FREE_OPERATIONS_LIMIT - currentOperationsCount).coerceAtLeast(0)
    }

    /**
     * Formats WhatsApp message URL for sending the device code to the developer.
     */
    fun getWhatsAppActivationUrl(deviceCode: String): String {
        val msg = """
السلام عليكم يا باشمهندس عمر،
أود تفعيل تطبيق المُسَرِّب للآبار والري.
كود جهازي هو:
$deviceCode
        """.trimIndent()
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }
}
