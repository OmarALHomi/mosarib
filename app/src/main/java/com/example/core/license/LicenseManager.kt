package com.example.core.license

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale

/**
 * License and device activation manager with online registration verification.
 * Limits free usage to [FREE_OPERATIONS_LIMIT] operations.
 * Binds activation permanently to the specific device hardware and records subscriber & device details.
 * Prevents time tampering, duplicate device usage, and supports remote subscription revocation via Firebase.
 */
object LicenseManager {

    const val FREE_OPERATIONS_LIMIT = 50
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
    private const val KEY_LAST_KNOWN_TIMESTAMP = "last_known_safe_timestamp"
    private const val KEY_IS_REVOKED_LOCALLY = "is_license_revoked_locally"
    const val KEY_ONBOARDING_COMPLETED = "is_onboarding_completed"
    const val KEY_HOME_TOUR_COMPLETED = "is_home_tour_completed_v2"

    // Secret salts
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
        data class BoundToOtherDevice(val boundDevice: String?) : ActivationResult()
        object SubscriptionRevoked : ActivationResult()
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
     */
    internal fun generateActivationKey(deviceCode: String, plan: SubscriptionPlan = SubscriptionPlan.MONTHLY): String {
        val cleanCode = deviceCode.replace("-", "").trim().uppercase()
        val hash = sha256(cleanCode + SECRET_ACTIVATION_SALT + plan.codePrefix)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "ACTV-${plan.codePrefix}-$part1-$part2"
    }

    /**
     * Verifies the activation key entered by the user with online connectivity check and single device binding.
     */
    suspend fun verifyAndActivateOnline(
        context: Context,
        enteredKey: String,
        subscriberName: String
    ): ActivationResult {
        val cleanName = subscriberName.trim()
        if (cleanName.isBlank()) {
            return ActivationResult.MissingSubscriberName
        }

        val cleanEntered = enteredKey.replace("-", "").replace(" ", "").trim().uppercase()
        val currentDeviceCode = getDeviceCode(context)

        // Find matching plan
        var matchedPlan: SubscriptionPlan? = null
        for (plan in SubscriptionPlan.entries) {
            val expectedKey = generateActivationKey(currentDeviceCode, plan).replace("-", "").uppercase()
            val expectedKeyNoPrefix = expectedKey.removePrefix("ACTV")
            if (cleanEntered == expectedKey || cleanEntered == expectedKeyNoPrefix) {
                matchedPlan = plan
                break
            }
        }

        if (matchedPlan == null) {
            // Also check if entered key is formatted as ACTV-M-XXXX-XXXX or ACTV-Y-XXXX-XXXX
            matchedPlan = if (cleanEntered.startsWith("ACTVM") || cleanEntered.startsWith("M")) {
                SubscriptionPlan.MONTHLY
            } else if (cleanEntered.startsWith("ACTVY") || cleanEntered.startsWith("Y")) {
                SubscriptionPlan.YEARLY
            } else {
                SubscriptionPlan.MONTHLY
            }
        }

        val expectedForDevice = generateActivationKey(currentDeviceCode, matchedPlan).replace("-", "").uppercase()
        val expectedWithoutPrefix = expectedForDevice.removePrefix("ACTV")
        val isMathValid = cleanEntered == expectedForDevice || cleanEntered == expectedWithoutPrefix
        val hasInternet = isInternetAvailable(context)

        var serverExpiresAt: Long? = null

        if (hasInternet) {
            // Verify with Firebase Cloud backend
            val cloudCheck = FirebaseLicenseService.verifyAndBindKeyOnline(
                key = cleanEntered,
                currentDeviceCode = currentDeviceCode,
                subscriberName = cleanName,
                plan = matchedPlan,
                durationDays = matchedPlan.durationDays
            )

            if (cloudCheck.isRevoked) {
                revokeLicenseLocally(context)
                return ActivationResult.SubscriptionRevoked
            }

            if (cloudCheck.isBoundToOtherDevice) {
                return ActivationResult.BoundToOtherDevice(cloudCheck.boundDeviceCode)
            }

            if (!isMathValid && !cloudCheck.isValid) {
                return ActivationResult.InvalidKey
            }
            serverExpiresAt = cloudCheck.serverExpiresAt
        } else {
            // Offline fallback: Must be mathematically valid for this device
            if (!isMathValid) {
                return ActivationResult.NoInternet
            }
        }

        // Save activation locally
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val currentExpiry = sp.getLong(KEY_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()
        val baseTime = maxOf(now, currentExpiry)
        val newExpiresAt = serverExpiresAt ?: (baseTime + (matchedPlan.durationDays * 24L * 3600L * 1000L))

        val signature = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + matchedPlan.name + newExpiresAt)
        val deviceModel = getDeviceModelName()
        val androidVersion = getAndroidVersionName()

        sp.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putBoolean(KEY_IS_REVOKED_LOCALLY, false)
            .putString(KEY_SUBSCRIPTION_PLAN, matchedPlan.name)
            .putLong(KEY_EXPIRES_AT, newExpiresAt)
            .putString(KEY_ACTIVATION_SIGNATURE, signature)
            .putLong(KEY_ACTIVATED_AT, now)
            .putLong(KEY_LAST_KNOWN_TIMESTAMP, now)
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
            plan = matchedPlan,
            activatedAt = now,
            expiresAt = newExpiresAt
        )
        return ActivationResult.Success(matchedPlan, record)
    }

    /**
     * Synchronous fallback for legacy callers, calls verifyAndActivateOnline on IO scope.
     */
    fun verifyAndActivate(context: Context, enteredKey: String, subscriberName: String): ActivationResult {
        return kotlinx.coroutines.runBlocking {
            verifyAndActivateOnline(context, enteredKey, subscriberName)
        }
    }

    fun verifyAndActivate(context: Context, enteredKey: String): SubscriptionPlan? {
        val res = verifyAndActivate(context, enteredKey, "المشترك")
        return if (res is ActivationResult.Success) res.plan else null
    }

    /**
     * Revokes license locally and blocks further usage.
     */
    fun revokeLicenseLocally(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean(KEY_IS_ACTIVATED, false)
            .putBoolean(KEY_IS_REVOKED_LOCALLY, true)
            .putLong(KEY_EXPIRES_AT, 0L)
            .remove(KEY_ACTIVATION_SIGNATURE)
            .apply()
    }

    /**
     * Synchronizes subscription status with Firebase:
     * - Checks for remote revocation.
     * - Auto-activates or extends subscription if approved / renewed remotely via the admin dashboard.
     */
    suspend fun syncRemoteSubscriptionStatus(context: Context) {
        if (!isInternetAvailable(context)) return
        val deviceCode = getDeviceCode(context)
        val license = FirebaseLicenseService.getDeviceRemoteLicense(deviceCode)
        // Also keep local pricing & payment methods cache fresh in the background
        runCatching {
            FirebaseLicenseService.getSubscriptionPricing(context)
            FirebaseLicenseService.getPaymentMethods(context)
        }

        if (!license.isFound) return

        if (license.isRevoked) {
            revokeLicenseLocally(context)
            return
        }

        val now = System.currentTimeMillis()
        if (license.expiresAt > now) {
            val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val currentExpiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)
            val isLocallyActive = isActivated(context)

            // If not active locally, or admin extended the subscription expiration
            if (!isLocallyActive || license.expiresAt > currentExpiresAt) {
                val plan = runCatching { SubscriptionPlan.valueOf(license.plan) }.getOrDefault(SubscriptionPlan.MONTHLY)
                val signature = sha256(deviceCode + SECRET_ACTIVATION_SALT + plan.name + license.expiresAt)
                val deviceModel = getDeviceModelName()
                val androidVersion = getAndroidVersionName()

                sp.edit()
                    .putBoolean(KEY_IS_ACTIVATED, true)
                    .putBoolean(KEY_IS_REVOKED_LOCALLY, false)
                    .putString(KEY_SUBSCRIPTION_PLAN, plan.name)
                    .putLong(KEY_EXPIRES_AT, license.expiresAt)
                    .putString(KEY_ACTIVATION_SIGNATURE, signature)
                    .putLong(KEY_ACTIVATED_AT, now)
                    .putLong(KEY_LAST_KNOWN_TIMESTAMP, now)
                    .putString(KEY_SUBSCRIBER_NAME, license.subscriberName.ifBlank { "المشترك" })
                    .putString(KEY_DEVICE_MODEL, deviceModel)
                    .putString(KEY_ANDROID_VERSION, androidVersion)
                    .putString(KEY_ACTIVATION_KEY_USED, license.key)
                    .apply()
            }
        }
    }

    fun checkRemoteRevocationInBackground(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            syncRemoteSubscriptionStatus(context)
        }
    }

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
     * Also guards against backwards clock manipulation.
     */
    fun isActivated(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_IS_REVOKED_LOCALLY, false)) return false
        val isActivated = sp.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivated) return false

        val storedSig = sp.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false
        val storedPlan = sp.getString(KEY_SUBSCRIPTION_PLAN, null) ?: return false
        val storedExpiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)

        val now = System.currentTimeMillis()
        val lastKnownTime = sp.getLong(KEY_LAST_KNOWN_TIMESTAMP, 0L)

        // Anti-clock rollback: if current time is substantially in the past relative to last known time (> 2 hours)
        if (lastKnownTime > 0L && now < (lastKnownTime - 2 * 3600 * 1000L)) {
            // Clock was rolled backwards to cheat expiry!
            return false
        }

        // Monotonically advance last known time
        if (now > lastKnownTime) {
            sp.edit().putLong(KEY_LAST_KNOWN_TIMESTAMP, now).apply()
        }

        // Check expiration
        if (now > storedExpiresAt) {
            return false
        }

        val currentDeviceCode = getDeviceCode(context)
        val expectedSig = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + storedPlan + storedExpiresAt)
        return storedSig == expectedSig
    }

    fun getActivePlan(context: Context): SubscriptionPlan? {
        if (!isActivated(context)) return null
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val planName = sp.getString(KEY_SUBSCRIPTION_PLAN, null) ?: return null
        return runCatching { SubscriptionPlan.valueOf(planName) }.getOrNull()
    }

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

    fun isTrialConsumed(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (com.example.BuildConfig.DEBUG && sp.getBoolean("debug_unlimited_ops", false)) return false
        if (sp.getBoolean(KEY_TRIAL_CONSUMED, false)) return true

        val deviceCode = getDeviceCode(context)
        val expectedWatermark = sha256(deviceCode + TRIAL_WATERMARK_SALT)

        val externalDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        )

        for (dir in externalDirs) {
            runCatching {
                val tokenFile = java.io.File(dir, ".msrb_trial_signature")
                if (tokenFile.exists() && tokenFile.readText(Charsets.UTF_8).trim() == expectedWatermark) {
                    sp.edit().putBoolean(KEY_TRIAL_CONSUMED, true).apply()
                    return true
                }
            }
        }
        return false
    }

    fun markTrialAsConsumed(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (com.example.BuildConfig.DEBUG && sp.getBoolean("debug_unlimited_ops", false)) return
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

    fun getFreeOperationsLimit(context: Context): Int {
        return RemoteConfigManager.getFreeOperationsLimit(context)
    }

    /**
     * Checks if the trial limit has been exceeded, taking into account debug mode overrides.
     */
    fun isTrialLimitExceeded(context: Context, currentOperationsCount: Int): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (com.example.BuildConfig.DEBUG && sp.getBoolean("debug_unlimited_ops", false)) return false
        if (isActivated(context)) return false
        if (isTrialConsumed(context)) return true
        val storedOps = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val effectiveCount = maxOf(storedOps, currentOperationsCount)
        val effectiveLimit = getFreeOperationsLimit(context)
        return effectiveCount >= effectiveLimit
    }

    fun recordOperationPerformed(context: Context, currentDbCount: Int) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val storedCumulative = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val newCumulative = maxOf(storedCumulative + 1, currentDbCount)
        sp.edit().putInt(KEY_CUMULATIVE_OPS, newCumulative).apply()

        val effectiveLimit = getFreeOperationsLimit(context)
        if (newCumulative >= effectiveLimit) {
            markTrialAsConsumed(context)
        }
    }

    fun getCumulativeOperations(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CUMULATIVE_OPS, 0)
    }

    fun syncCumulativeOperations(context: Context, serverCount: Int) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val current = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        if (serverCount > current) {
            sp.edit().putInt(KEY_CUMULATIVE_OPS, serverCount).apply()
            val effectiveLimit = getFreeOperationsLimit(context)
            if (serverCount >= effectiveLimit) {
                markTrialAsConsumed(context)
            }
        }
    }

    /**
     * Synchronizes license usage and trial limits after restoring a backup.
     * @return true if the restored operations count reaches or exceeds the dynamic free operations limit,
     * meaning the trial is fully consumed and subscription is mandatory.
     */
    fun syncOperationsAfterRestore(context: Context, totalDbCount: Int): Boolean {
        if (isActivated(context)) return false

        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_CUMULATIVE_OPS, totalDbCount).apply()

        val effectiveLimit = getFreeOperationsLimit(context)
        return if (totalDbCount >= effectiveLimit) {
            markTrialAsConsumed(context)
            true
        } else {
            resetTrialConsumptionForRestore(context)
            false
        }
    }

    fun resetTrialConsumptionForRestore(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_TRIAL_CONSUMED, false).apply()

        val externalDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        )

        for (dir in externalDirs) {
            runCatching {
                val tokenFile = java.io.File(dir, ".msrb_trial_signature")
                if (tokenFile.exists()) {
                    tokenFile.delete()
                }
            }
        }
    }

    fun canPerformOperation(context: Context, currentOperationsCount: Int): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (com.example.BuildConfig.DEBUG && sp.getBoolean("debug_unlimited_ops", false)) return true
        if (isActivated(context)) return true
        if (isTrialConsumed(context)) return false
        val storedOps = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val effectiveCount = maxOf(storedOps, currentOperationsCount)
        val effectiveLimit = getFreeOperationsLimit(context)
        if (effectiveCount >= effectiveLimit) {
            markTrialAsConsumed(context)
            return false
        }
        return true
    }

    fun getLimitReachedToastMessage(context: Context): String {
        val limit = getFreeOperationsLimit(context)
        return "استنفدت العمليات التجريبية المجانية ($limit عملية). يرجى تفعيل النسخة الكاملة للتطبيق"
    }

    fun getRemainingOperations(context: Context, currentOperationsCount: Int): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (com.example.BuildConfig.DEBUG && sp.getBoolean("debug_unlimited_ops", false)) return 999999
        if (isActivated(context)) return Int.MAX_VALUE
        if (isTrialConsumed(context)) return 0
        val storedOps = sp.getInt(KEY_CUMULATIVE_OPS, 0)
        val effectiveCount = maxOf(storedOps, currentOperationsCount)
        val effectiveLimit = getFreeOperationsLimit(context)
        return (effectiveLimit - effectiveCount).coerceAtLeast(0)
    }

    fun getWhatsAppActivationUrl(
        deviceCode: String,
        subscriberName: String = "",
        requestedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
        receiptNumber: String = "",
        region: String = "",
        customPlanText: String? = null,
        paymentMethod: String = ""
    ): String {
        val planText = customPlanText ?: if (requestedPlan == SubscriptionPlan.MONTHLY) "اشتراك شهري 10$ (30 يوماً)" else "اشتراك سنوي 80$ (365 يوماً - وفر 40$)"
        val devModel = getDeviceModelName()
        val nameLine = if (subscriberName.isNotBlank()) "اسم المشترك: $subscriberName\n" else ""
        val regionLine = if (region.isNotBlank()) "المحافظة والمنطقة: $region\n" else ""
        val paymentLine = if (paymentMethod.isNotBlank()) "طريقة الدفع / المحفظة: $paymentMethod\n" else ""
        val receiptLine = if (receiptNumber.isNotBlank()) "رقم سند التحويل / الإيداع: $receiptNumber\n" else ""
        val msg = """
السلام عليكم يا باشمهندس عمر،
أود تفعيل تطبيق المُسَرِّب للآبار والري ($planText).
${nameLine}${regionLine}${paymentLine}${receiptLine}نوع الجوال: $devModel
كود جهازي هو:
$deviceCode
        """.trimIndent()
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }

    fun getWhatsAppActivationUrl(deviceCode: String, requestedPlan: SubscriptionPlan): String {
        return getWhatsAppActivationUrl(deviceCode, "", requestedPlan)
    }

    fun getWhatsAppRenewalUrl(
        deviceCode: String,
        subscriberName: String = "",
        requestedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
        isUpgrade: Boolean = false,
        receiptNumber: String = "",
        region: String = "",
        customPlanText: String? = null,
        paymentMethod: String = ""
    ): String {
        val devModel = getDeviceModelName()
        val nameLine = if (subscriberName.isNotBlank()) "اسم المشترك: $subscriberName\n" else ""
        val regionLine = if (region.isNotBlank()) "المحافظة والمنطقة: $region\n" else ""
        val paymentLine = if (paymentMethod.isNotBlank()) "طريقة الدفع / المحفظة: $paymentMethod\n" else ""
        val receiptLine = if (receiptNumber.isNotBlank()) "رقم سند التحويل / الإيداع: $receiptNumber\n" else ""
        val msg = if (isUpgrade) {
            val upgradeText = customPlanText ?: "اشتراك سنوي 80$ (365 يوماً)"
            """
السلام عليكم يا باشمهندس عمر،
أود ترقية اشتراكي في تطبيق المُسَرِّب من شهري إلى $upgradeText.
${nameLine}${regionLine}${paymentLine}${receiptLine}نوع الجوال: $devModel
كود جهازي هو:
$deviceCode
            """.trimIndent()
        } else {
            val planText = customPlanText ?: if (requestedPlan == SubscriptionPlan.MONTHLY) "اشتراك شهري 10$ (30 يوماً)" else "اشتراك سنوي 80$ (365 يوماً)"
            """
السلام عليكم يا باشمهندس عمر،
أود تجديد اشتراكي في تطبيق المُسَرِّب للآبار والري ($planText).
${nameLine}${regionLine}${paymentLine}${receiptLine}نوع الجوال: $devModel
كود جهازي هو:
$deviceCode
            """.trimIndent()
        }
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }
}
