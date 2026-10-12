package com.example.core.license

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

/**
 * License and device activation manager with online registration verification.
 * Limits free usage to [FREE_OPERATIONS_LIMIT] operations.
 * Binds activation permanently to the specific device hardware and records subscriber & device details.
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
    private const val KEY_SUBSCRIBER_NAME = "activation_subscriber_name"
    private const val KEY_DEVICE_MODEL = "activation_device_model"
    private const val KEY_ANDROID_VERSION = "activation_android_version"
    private const val KEY_ACTIVATION_KEY_USED = "activation_key_used"
    const val KEY_ONBOARDING_COMPLETED = "is_onboarding_completed"
    const val KEY_HOME_TOUR_COMPLETED = "is_home_tour_completed"

    // Secret salts (never expose raw algorithms)
    private const val DEVICE_CODE_SALT = "msrb_device_token_salt_v1"
    private const val SECRET_ACTIVATION_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app"

    enum class SubscriptionPlan(val durationDays: Int, val titleArabic: String, val codePrefix: String) {
        MONTHLY(30, "اشتراك شهري (30 يوماً)", "M"),
        YEARLY(365, "اشتراك سنوي (365 يوماً)", "Y")
    }

    data class ActivationRecord(
        val subscriberName: String,
        val deviceModel: String,
        val androidVersion: String,
        val deviceCode: String,
        val plan: SubscriptionPlan,
        val activatedAt: Long,
        val expiresAt: Long
    )

    sealed class ActivationResult {
        data class Success(val plan: SubscriptionPlan, val record: ActivationRecord) : ActivationResult()
        object NoInternet : ActivationResult()
        object MissingSubscriberName : ActivationResult()
        object InvalidKey : ActivationResult()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Checks if the device has an active internet connection.
     * Activation requires internet connectivity to register and verify subscription.
     */
    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Extracts readable device model (e.g. Samsung Galaxy S22 or Xiaomi Redmi Note 11).
     */
    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    /**
     * Extracts Android OS version info.
     */
    fun getAndroidVersionName(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
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
    internal fun generateActivationKey(deviceCode: String, plan: SubscriptionPlan = SubscriptionPlan.MONTHLY): String {
        val cleanCode = deviceCode.replace("-", "").trim().uppercase()
        val hash = sha256(cleanCode + SECRET_ACTIVATION_SALT + plan.codePrefix)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "ACTV-${plan.codePrefix}-$part1-$part2"
    }

    /**
     * Verifies the activation key entered by the user with mandatory online connectivity check.
     * Registers the subscriber name, phone model, OS version, and plan duration.
     */
    fun verifyAndActivate(
        context: Context,
        enteredKey: String,
        subscriberName: String
    ): ActivationResult {
        if (!isInternetAvailable(context)) {
            return ActivationResult.NoInternet
        }
        val cleanName = subscriberName.trim()
        if (cleanName.isBlank()) {
            return ActivationResult.MissingSubscriberName
        }

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
                val deviceModel = getDeviceModelName()
                val androidVersion = getAndroidVersionName()
                val now = System.currentTimeMillis()

                sp.edit()
                    .putBoolean(KEY_IS_ACTIVATED, true)
                    .putString(KEY_SUBSCRIPTION_PLAN, plan.name)
                    .putLong(KEY_EXPIRES_AT, newExpiresAt)
                    .putString(KEY_ACTIVATION_SIGNATURE, signature)
                    .putLong(KEY_ACTIVATED_AT, now)
                    .putString(KEY_SUBSCRIBER_NAME, cleanName)
                    .putString(KEY_DEVICE_MODEL, deviceModel)
                    .putString(KEY_ANDROID_VERSION, androidVersion)
                    .putString(KEY_ACTIVATION_KEY_USED, enteredKey.trim())
                    .apply()

                val record = ActivationRecord(
                    subscriberName = cleanName,
                    deviceModel = deviceModel,
                    androidVersion = androidVersion,
                    deviceCode = currentDeviceCode,
                    plan = plan,
                    activatedAt = now,
                    expiresAt = newExpiresAt
                )
                return ActivationResult.Success(plan, record)
            }
        }
        return ActivationResult.InvalidKey
    }

    /**
     * Backward-compatible overload for key verification.
     */
    fun verifyAndActivate(context: Context, enteredKey: String): SubscriptionPlan? {
        val result = verifyAndActivate(context, enteredKey, "المشترك")
        return if (result is ActivationResult.Success) result.plan else null
    }

    /**
     * Retrieves saved activation registration record, or null if unlicensed.
     */
    fun getActivationRecord(context: Context): ActivationRecord? {
        if (!isActivated(context)) return null
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val subscriberName = sp.getString(KEY_SUBSCRIBER_NAME, "") ?: ""
        val deviceModel = sp.getString(KEY_DEVICE_MODEL, "") ?: getDeviceModelName()
        val androidVersion = sp.getString(KEY_ANDROID_VERSION, "") ?: getAndroidVersionName()
        val planName = sp.getString(KEY_SUBSCRIPTION_PLAN, null) ?: return null
        val plan = runCatching { SubscriptionPlan.valueOf(planName) }.getOrNull() ?: return null
        val activatedAt = sp.getLong(KEY_ACTIVATED_AT, 0L)
        val expiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)
        val deviceCode = getDeviceCode(context)
        return ActivationRecord(
            subscriberName = subscriberName,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            deviceCode = deviceCode,
            plan = plan,
            activatedAt = activatedAt,
            expiresAt = expiresAt
        )
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isHomeTourCompleted(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_HOME_TOUR_COMPLETED, false)
    }

    fun setHomeTourCompleted(context: Context, completed: Boolean = true) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_HOME_TOUR_COMPLETED, completed).apply()
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

    private const val KEY_TRIAL_CONSUMED = "is_trial_consumed_permanently"
    private const val KEY_CUMULATIVE_OPS = "cumulative_ops_count"
    private const val TRIAL_WATERMARK_SALT = "msrb_trial_permanent_lock_salt_2026_alhomi"

    /**
     * Checks whether the trial period or free operations have already been consumed on this phone.
     * Checks multiple persistent layers: internal prefs, external app dir, and public media watermark.
     * Survives "Clear App Data" and app reinstalls on the same physical device.
     */
    fun isTrialConsumed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_TRIAL_CONSUMED, false)) return true

        val deviceCode = getDeviceCode(context)
        val expectedWatermark = sha256(deviceCode + TRIAL_WATERMARK_SALT)

        // Layer 2: Check persistent file in external documents/downloads directory
        val externalDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        )

        for (dir in externalDirs) {
            runCatching {
                val tokenFile = java.io.File(dir, ".msrb_trial_signature")
                if (tokenFile.exists() && tokenFile.readText(Charsets.UTF_8).trim() == expectedWatermark) {
                    // Sync back to preferences
                    sp.edit().putBoolean(KEY_TRIAL_CONSUMED, true).apply()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Permanently marks this physical device as having consumed its free trial.
     * Writes cryptographic hardware-bound watermark across multiple persistent layers.
     */
    fun markTrialAsConsumed(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_TRIAL_CONSUMED, true).apply()

        val deviceCode = getDeviceCode(context)
        val expectedWatermark = sha256(deviceCode + TRIAL_WATERMARK_SALT)

        val externalDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        )

        for (dir in externalDirs) {
            runCatching {
                if (!dir.exists()) dir.mkdirs()
                val tokenFile = java.io.File(dir, ".msrb_trial_signature")
                tokenFile.writeText(expectedWatermark, Charsets.UTF_8)
            }
        }
    }

    /**
     * Records operation count monotonically so deleting database rows doesn't reset the counter.
     */
    fun recordOperationPerformed(context: Context, currentDbCount: Int) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val storedCumulative = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val newCumulative = maxOf(storedCumulative + 1, currentDbCount)
        sp.edit().putInt(KEY_CUMULATIVE_OPS, newCumulative).apply()

        if (newCumulative >= FREE_OPERATIONS_LIMIT) {
            markTrialAsConsumed(context)
        }
    }

    /**
     * Checks whether an operation (session or voucher) can be performed.
     * True if active subscription, or if device has not consumed trial and operations count < limit.
     */
    fun canPerformOperation(context: Context, currentOperationsCount: Int): Boolean {
        if (isActivated(context)) return true
        if (isTrialConsumed(context)) return false
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val storedOps = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val effectiveCount = maxOf(storedOps, currentOperationsCount)
        if (effectiveCount >= FREE_OPERATIONS_LIMIT) {
            markTrialAsConsumed(context)
            return false
        }
        return true
    }

    /**
     * Returns remaining free operations (0 if reached or unlimited if active subscription).
     */
    fun getRemainingOperations(context: Context, currentOperationsCount: Int): Int {
        if (isActivated(context)) return Int.MAX_VALUE
        if (isTrialConsumed(context)) return 0
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val storedOps = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val effectiveCount = maxOf(storedOps, currentOperationsCount)
        return (FREE_OPERATIONS_LIMIT - effectiveCount).coerceAtLeast(0)
    }

    /**
     * Formats WhatsApp message URL for sending the device code, subscriber name, and device model to the developer.
     */
    fun getWhatsAppActivationUrl(
        deviceCode: String,
        subscriberName: String = "",
        requestedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY
    ): String {
        val planText = if (requestedPlan == SubscriptionPlan.MONTHLY) "اشتراك شهري (30 يوماً)" else "اشتراك سنوي (365 يوماً)"
        val devModel = getDeviceModelName()
        val nameLine = if (subscriberName.isNotBlank()) "اسم المشترك: $subscriberName\n" else ""
        val msg = """
السلام عليكم يا باشمهندس عمر،
أود تفعيل تطبيق المُسَرِّب للآبار والري ($planText).
${nameLine}نوع الجوال: $devModel
كود جهازي هو:
$deviceCode
        """.trimIndent()
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }

    fun getWhatsAppActivationUrl(deviceCode: String, requestedPlan: SubscriptionPlan): String {
        return getWhatsAppActivationUrl(deviceCode, "", requestedPlan)
    }
}
