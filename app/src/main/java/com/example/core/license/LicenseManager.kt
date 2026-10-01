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
    private const val KEY_EXPIRES_AT = "subscription_expires_at"
    private const val KEY_SUBSCRIPTION_PLAN = "subscription_plan"

    // Secret salts (never expose raw algorithms)
    private const val DEVICE_CODE_SALT = "msrb_device_token_salt_v1"
    private const val SECRET_ACTIVATION_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app"

    enum class SubscriptionPlan(val durationDays: Int, val titleArabic: String, val codePrefix: String) {
        MONTHLY(30, "اشتراك شهري (30 يوماً)", "M"),
        YEARLY(365, "اشتراك سنوي (365 يوماً)", "Y")
    }

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
     * Mathematical generator to produce the activation key corresponding to a device code and plan.
     * Monthly: ACTV-M-XXXX-XXXX
     * Yearly: ACTV-Y-XXXX-XXXX
     */
    fun generateActivationKey(deviceCode: String, plan: SubscriptionPlan = SubscriptionPlan.MONTHLY): String {
        val cleanCode = deviceCode.replace("-", "").trim().uppercase()
        val hash = sha256(cleanCode + SECRET_ACTIVATION_SALT + plan.codePrefix)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "ACTV-${plan.codePrefix}-$part1-$part2"
    }

    /**
     * Verifies the activation key entered by the user.
     * Activates for either Monthly (30 days) or Yearly (365 days).
     * If already active, extends the expiry date safely.
     * Returns the activated SubscriptionPlan if successful, or null if invalid.
     */
    fun verifyAndActivate(context: Context, enteredKey: String): SubscriptionPlan? {
        val cleanEntered = enteredKey.replace("-", "").replace(" ", "").trim().uppercase()
        val currentDeviceCode = getDeviceCode(context)

        for (plan in SubscriptionPlan.entries) {
            val expectedKey = generateActivationKey(currentDeviceCode, plan).replace("-", "").uppercase()
            val expectedKeyNoPrefix = expectedKey.removePrefix("ACTV")
            if (cleanEntered == expectedKey || cleanEntered == expectedKeyNoPrefix) {
                val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                val currentExpiry = sp.getLong(KEY_EXPIRES_AT, 0L)
                val baseTime = maxOf(System.currentTimeMillis(), currentExpiry)
                val newExpiresAt = baseTime + (plan.durationDays * 24L * 3600L * 1000L)

                val signature = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + plan.name + newExpiresAt)

                sp.edit()
                    .putBoolean(KEY_IS_ACTIVATED, true)
                    .putString(KEY_SUBSCRIPTION_PLAN, plan.name)
                    .putLong(KEY_EXPIRES_AT, newExpiresAt)
                    .putString(KEY_ACTIVATION_SIGNATURE, signature)
                    .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
                    .apply()
                return plan
            }
        }
        return null
    }

    /**
     * Returns true if this device has an ACTIVE, non-expired subscription.
     * Validates cryptographic signature against device hardware and expiration time.
     */
    fun isActivated(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val isActivated = sp.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivated) return false

        val storedSig = sp.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false
        val storedPlan = sp.getString(KEY_SUBSCRIPTION_PLAN, null) ?: return false
        val storedExpiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)

        // Check expiration
        if (System.currentTimeMillis() > storedExpiresAt) {
            return false
        }

        val currentDeviceCode = getDeviceCode(context)
        val expectedSig = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + storedPlan + storedExpiresAt)
        return storedSig == expectedSig
    }

    /**
     * Returns the currently active subscription plan, or null if expired/unlicensed.
     */
    fun getActivePlan(context: Context): SubscriptionPlan? {
        if (!isActivated(context)) return null
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val planName = sp.getString(KEY_SUBSCRIPTION_PLAN, null) ?: return null
        return runCatching { SubscriptionPlan.valueOf(planName) }.getOrNull()
    }

    /**
     * Returns remaining days in the active subscription.
     * Returns 0 if expired or not activated.
     */
    fun getRemainingDays(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val storedExpiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)
        val remainingMs = storedExpiresAt - System.currentTimeMillis()
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 86399999L) / (24L * 3600L * 1000L)).toInt()
    }

    /**
     * Checks whether an operation (session or voucher) can be performed.
     * True if active subscription or if current total operations is below [FREE_OPERATIONS_LIMIT].
     */
    fun canPerformOperation(context: Context, currentOperationsCount: Int): Boolean {
        return isActivated(context) || currentOperationsCount < FREE_OPERATIONS_LIMIT
    }

    /**
     * Returns remaining free operations (0 if reached or unlimited if active subscription).
     */
    fun getRemainingOperations(context: Context, currentOperationsCount: Int): Int {
        if (isActivated(context)) return Int.MAX_VALUE
        return (FREE_OPERATIONS_LIMIT - currentOperationsCount).coerceAtLeast(0)
    }

    /**
     * Formats WhatsApp message URL for sending the device code to the developer.
     */
    fun getWhatsAppActivationUrl(deviceCode: String, requestedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY): String {
        val planText = if (requestedPlan == SubscriptionPlan.MONTHLY) "اشتراك شهري (30 يوماً)" else "اشتراك سنوي (365 يوماً)"
        val msg = """
السلام عليكم يا باشمهندس عمر،
أود تفعيل تطبيق المُسَرِّب للآبار والري ($planText).
كود جهازي هو:
$deviceCode
        """.trimIndent()
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }
}
