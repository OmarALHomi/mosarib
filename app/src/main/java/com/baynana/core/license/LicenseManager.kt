package com.baynana.core.license

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.license.LicenseRepository
import com.baynana.domain.license.LicenseRejection
import com.baynana.domain.license.LicenseToken
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

/**
 * الترخيص والتفعيل في «بيننا» — بلا إنترنت وبلا حساب.
 *
 * **مساران، ولا ثالث:**
 * 1. **تصريح موقّع (ح١٣، وهو المعتمد):** `BNNA1.<payload>.<توقيع>` يوقّعه المالك بمفتاحه الخاص
 *    ECDSA P-256، ويتحقق منه التطبيق بالمفتاح العام في الأصول. لا يمكن توليده من داخل التطبيق،
 *    ولا استخدامه مرتين: يُسجَّل في جدول `licenses` بمفتاحه الفريد فيُرفض التكرار (منع replay).
 * 2. **مفتاح قديم (للتوافق فقط):** مولَّد من سرّ مكتوب في الكود، فيمكن توليده من يعرف الكود،
 *    ولذلك هو أضعف أمنيًا بطبيعته ويُوسَم `LEGACY` في السجل. نُبقيه لأن أجهزة كثيرة تعمل به،
 *    لكنه أُغلق العيب الأخطر فيه: إدخال الرمز نفسه مرتين لم يعد يمدّد شيئًا.
 *
 * **قاعدة المال**: الترخيص استحقاق لا رصيد؛ لا يمرّ من هنا أي مبلغ ولا عملة، ولا يُحسب الكسر.
 *
 * Legacy notes kept for backward compatibility with earlier Mosarib single-role keys.
 * Supports 4 distinct roles:
 * - [LicenseRole.MUSRIB]: Prefix ACTV (Water irrigation provider)
 * - [LicenseRole.DALLAL]: Prefix DLLV (Broker / Marketer)
 * - [LicenseRole.BUYER]: Prefix BJRV (Wholesale crop buyer / Mojabri)
 * - [LicenseRole.FARMER]: Prefix FRMV (Verified farm owner)
 *
 * Full backward-compatibility with earlier Mosarib single-role keys.
 */
object LicenseManager {

    const val FREE_OPERATIONS_LIMIT = 200
    const val DEVELOPER_PHONE = "967773712030"
    const val DEVELOPER_NAME = "المهندس عمر الحومي"
    const val DEVELOPER_EMAIL = "mralhwmy@gmail.com"
    const val DEVELOPER_WEBSITE = "https://omar.tubbasoft.com"
    const val COMPANY_WEBSITE = "https://tubbasoft.com"

    private const val PREFS_FILE = "mosarib_license_prefs"

    // Legacy keys (preserved for 100% backward compatibility)
    private const val KEY_LEGACY_IS_ACTIVATED = "is_app_activated"
    private const val KEY_LEGACY_ACTIVATION_SIGNATURE = "activation_signature"
    private const val KEY_LEGACY_ACTIVATED_AT = "activated_at"
    private const val KEY_LEGACY_EXPIRES_AT = "subscription_expires_at"
    private const val KEY_LEGACY_SUBSCRIPTION_PLAN = "subscription_plan"

    // Dynamic config keys (populated from Firestore system_config/limits cache)
    private const val KEY_CONFIG_FREE_LAUNCH_PERIOD = "config_free_launch_period"
    private const val KEY_CONFIG_LIMIT_MUSRIB = "config_limit_musrib"
    private const val KEY_CONFIG_LIMIT_DALLAL_LISTINGS = "config_limit_dallal_listings"
    private const val KEY_CONFIG_LIMIT_DALLAL_DEALS = "config_limit_dallal_deals"

    // Cryptographic salts
    /** وسم يُكتب في سجل الترخيص مع كل مفتاح من المسار القديم، فلا يُخلط بالنوع المعتمد. */
    const val LEGACY_KEY_NOTE = "مفتاح النسخة القديمة (السرّ المتناظر) — يُستبدل بتصريح موقّع."

    private const val DEVICE_CODE_SALT = "msrb_device_token_salt_v1"
    private const val SECRET_ACTIVATION_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app"

    /**
     * Supported user roles with distinctive activation key prefixes.
     */
    enum class LicenseRole(val prefix: String, val titleArabic: String) {
        MUSRIB("ACTV", "المُسَرِّب (خدمة الري)"),
        DALLAL("DLLV", "الدلال (الوساطة والتسويق)"),
        BUYER("BJRV", "المشتري / المجبري"),
        FARMER("FRMV", "المزارع (توثيق الحساب)")
    }

    /**
     * Subscription plans with respective durations and code tokens.
     */
    enum class SubscriptionPlan(val durationDays: Int, val titleArabic: String, val codePrefix: String) {
        MONTHLY(30, "اشتراك شهري (30 يوماً)", "M"),
        YEARLY(365, "اشتراك سنوي (365 يوماً)", "Y"),
        LIFETIME(3650, "توثيق دائم (مدى الحياة)", "L")
    }

    /**
     * Holds details of a successful activation event.
     */
    data class ActivationResult(
        val role: LicenseRole,
        val plan: SubscriptionPlan,
        val expiresAt: Long,
        /**
         * العنوان كما يُعرض. التصاريح الموقّعة قد تحمل مدة مخصّصة (٩٠ يومًا مثلًا) لا يوافقها اسم
         * خطة في هذه النسخة، فنعرض المدة الموقّعة نفسها بدل أن نكذب باسم خطة.
         */
        val labelArabic: String = ""
    ) {
        val titleArabic: String
            get() = labelArabic.ifEmpty { "${role.titleArabic} — ${plan.titleArabic}" }
    }

    /** نتيجة محاولة تفعيل: قُبلت (باستحقاق معروف) أو رُفضت (بسبب عربي ظاهر). */
    sealed interface ActivationOutcome {
        data class Activated(
            val result: ActivationResult,
            /** نهاية الاستحقاق قبل هذه العملية (0 إن لم يكن هناك استحقاق). */
            val expiresAtBefore: Long,
            /** `SIGNED` أو `LEGACY` — يُعلن نوع الاستحقاق ولا يُخفى. */
            val kind: String
        ) : ActivationOutcome

        data class Rejected(
            val reason: LicenseRejection,
            val messageArabic: String
        ) : ActivationOutcome
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Returns a stable, hardware-bound device code.
     * Example: MSRB-8F42-9D1B
     */
    fun getDeviceCode(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
        return generateDeviceCodeFromId(androidId)
    }

    /**
     * Pure function to generate a device code from any raw identifier (useful for tests).
     */
    fun generateDeviceCodeFromId(rawId: String): String {
        val hash = sha256(rawId + DEVICE_CODE_SALT)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "MSRB-$part1-$part2"
    }

    /**
     * **موروث — لا يُستعمل للمفاتيح الجديدة**: يولّد مفتاحًا من سرّ مكتوب في الكود، أي أن من يقرأ
     * الكود يستطيع توليده. بقي للتوافق مع أجهزة تعمل به، واللوحة الإدارية تُظهره بوضوح تحت
     * «المفاتيح القديمة» أما الجديد فهو [LicenseToken] الموقّع.
     *
     * Mathematical generator producing the activation key for a given device, role, and plan.
     * Example: ACTV-M-8F42-9D1B, DLLV-Y-8F42-9D1B, FRMV-L-8F42-9D1B
     */
    fun generateActivationKey(
        deviceCode: String,
        role: LicenseRole = LicenseRole.MUSRIB,
        plan: SubscriptionPlan = SubscriptionPlan.MONTHLY
    ): String {
        val cleanCode = deviceCode.replace("-", "").trim().uppercase()
        val hash = sha256(cleanCode + SECRET_ACTIVATION_SALT + role.prefix + plan.codePrefix)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        return "${role.prefix}-${plan.codePrefix}-$part1-$part2"
    }

    /**
     * Legacy generator overload for MUSRIB role compatibility.
     */
    fun generateActivationKey(
        deviceCode: String,
        plan: SubscriptionPlan
    ): String = generateActivationKey(deviceCode, LicenseRole.MUSRIB, plan)

    /**
     * Pure function to resolve an entered key against a known device code.
     * Checks all current roles and plans, plus legacy fallback formats.
     * Returns Pair(LicenseRole, SubscriptionPlan) if valid, or null.
     */
    fun resolveKey(deviceCode: String, enteredKey: String): Pair<LicenseRole, SubscriptionPlan>? {
        val cleanEntered = enteredKey.replace("-", "").replace(" ", "").trim().uppercase()
        val cleanDeviceCode = deviceCode.replace("-", "").trim().uppercase()

        // 1. Check all standard role + plan combinations
        for (role in LicenseRole.entries) {
            for (plan in SubscriptionPlan.entries) {
                val expectedFull = generateActivationKey(deviceCode, role, plan).replace("-", "").uppercase()
                val expectedNoPrefix = expectedFull.removePrefix(role.prefix)
                if (cleanEntered == expectedFull || cleanEntered == expectedNoPrefix) {
                    return Pair(role, plan)
                }
            }
        }

        // 2. Legacy check 1: Old LicenseManager format (ACTV + plan.codePrefix without role prefix in hash)
        for (plan in listOf(SubscriptionPlan.MONTHLY, SubscriptionPlan.YEARLY)) {
            val legacyHash = sha256(cleanDeviceCode + SECRET_ACTIVATION_SALT + plan.codePrefix)
            val p1 = legacyHash.substring(0, 4)
            val p2 = legacyHash.substring(4, 8)
            val legacyKey = "ACTV${plan.codePrefix}$p1$p2"
            val legacyNoPrefix = "${plan.codePrefix}$p1$p2"
            if (cleanEntered == legacyKey || cleanEntered == legacyNoPrefix) {
                return Pair(LicenseRole.MUSRIB, plan)
            }
        }

        // 3. Legacy check 2: Original key_generator.html format (ACTV-XXXX-XXXX with no plan prefix)
        val basicHash = sha256(cleanDeviceCode + SECRET_ACTIVATION_SALT)
        val bp1 = basicHash.substring(0, 4)
        val bp2 = basicHash.substring(4, 8)
        val basicKey = "ACTV$bp1$bp2"
        if (cleanEntered == basicKey || cleanEntered == "$bp1$bp2") {
            return Pair(LicenseRole.MUSRIB, SubscriptionPlan.MONTHLY)
        }

        return null
    }

    /**
     * يسترد رمزًا (تصريحًا موقّعًا أو مفتاحًا قديمًا) ويفعّل المهنة.
     *
     * كل الكتابة تمرّ من [LicenseRepository]: هو الذي يسجّل الاستحقاق بمفتاحه الفريد ويمنع
     * الاسترداد الثاني، وهو الذي يكتب أثره في `license_events` في المعاملة نفسها.
     *
     * @param repository حقنة اختبارية صريحة؛ الإنتاج يمرّر `null` فيُستعمل دفتر التطبيق.
     */
    suspend fun redeem(
        context: Context,
        enteredKey: String,
        repository: LicenseRepository? = null
    ): ActivationOutcome {
        val trimmed = enteredKey.trim()
        if (trimmed.isEmpty()) {
            return ActivationOutcome.Rejected(
                LicenseRejection.MALFORMED,
                "اكتب رمز التفعيل أولًا."
            )
        }

        val deviceCode = getDeviceCode(context)
        val repo = repository ?: LicenseRepository(
            db = AppDatabase.getDatabase(context),
            knownRoles = LicenseRole.entries.map { it.name }.toSet()
        )

        // ---- المسار المعتمد: تصريح موقّع
        if (LicenseToken.looksLikeToken(trimmed)) {
            return when (val outcome = repo.redeemSigned(trimmed, deviceCode, LicenseSignatures.verifier(context))) {
                is LicenseRepository.Redemption.Rejected ->
                    ActivationOutcome.Rejected(outcome.reason, outcome.reason.messageArabic)

                is LicenseRepository.Redemption.Granted -> {
                    val role = runCatching { LicenseRole.valueOf(outcome.role) }.getOrNull()
                        ?: return ActivationOutcome.Rejected(
                            LicenseRejection.UNKNOWN_TERM,
                            LicenseRejection.UNKNOWN_TERM.messageArabic
                        )
                    val result = applyGrant(
                        context = context,
                        role = role,
                        planName = outcome.plan,
                        durationDays = outcome.durationDays,
                        expiresAt = outcome.expiresAt
                    )
                    ActivationOutcome.Activated(result, outcome.expiresAtBefore, outcome.kind)
                }
            }
        }

        // ---- المسار الموروث: مفتاح من السرّ المكتوب في الكود (يُوسَم LEGACY في السجل)
        val resolved = resolveKey(deviceCode, trimmed)
            ?: return ActivationOutcome.Rejected(
                LicenseRejection.MALFORMED,
                "هذا الرمز غير صالح لهذا الجهاز. تأكد من نسخه كاملًا، أو اطلب تصريحًا جديدًا."
            )
        val (role, plan) = resolved
        val outcome = repo.claimLegacy(
            rawKey = trimmed,
            deviceCode = deviceCode,
            role = role.name,
            plan = plan.name,
            durationDays = plan.durationDays,
            note = LEGACY_KEY_NOTE
        )
        return when (outcome) {
            is LicenseRepository.Redemption.Rejected ->
                ActivationOutcome.Rejected(outcome.reason, outcome.reason.messageArabic)

            is LicenseRepository.Redemption.Granted -> {
                val result = applyGrant(
                    context = context,
                    role = role,
                    planName = plan.name,
                    durationDays = plan.durationDays,
                    expiresAt = outcome.expiresAt
                )
                ActivationOutcome.Activated(result, outcome.expiresAtBefore, outcome.kind)
            }
        }
    }

    /**
     * المسار المتزامن القديم (يعود بـ`null` عند أي رفض، بلا سبب).
     * بقي للتوافق مع الاختبارات والنداءات القديمة؛ الواجهة الجديدة تستدعي [redeem] لتعرض السبب.
     */
    fun verifyAndActivate(context: Context, enteredKey: String): ActivationResult? =
        when (val outcome = runBlocking { redeem(context, enteredKey) }) {
            is ActivationOutcome.Activated -> outcome.result
            is ActivationOutcome.Rejected -> null
        }

    /**
     * يكتب الاستحقاق في التخزين السريع (SharedPreferences) بعد أن ثبّته المستودع في الدفتر.
     *
     * العلاقة بينهما صريحة: **الدفتر هو الأصل** (صفّ في `licenses` لا يُكرَّر، فلا تمديد مرتين)،
     * وSharedPreferences مجرّد مرآة سريعة يقرأها باقي التطبيق بلا `suspend` — ثلاثون موضعًا تسأل
     * «هل المهنة مفعّلة؟» في مسار الواجهة، ولا يصحّ أن يقف كل واحد منها على قاعدة البيانات.
     */
    private fun applyGrant(
        context: Context,
        role: LicenseRole,
        planName: String,
        durationDays: Int,
        expiresAt: Long
    ): ActivationResult {
        val plan = SubscriptionPlan.entries.firstOrNull { it.name == planName }
        val resolvedPlan = plan ?: fallbackPlanFor(durationDays)
        val label = if (plan != null) {
            "${role.titleArabic} — ${plan.titleArabic}"
        } else {
            "${role.titleArabic} — تصريح موقّع لمدة $durationDays يومًا"
        }

        val deviceCode = getDeviceCode(context)
        val activatedAt = System.currentTimeMillis()
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val roleSig = sha256(deviceCode + SECRET_ACTIVATION_SALT + role.name + resolvedPlan.name + expiresAt)

        val editor = sp.edit()
            .putBoolean("role_activated_${role.name}", true)
            .putString("role_plan_${role.name}", resolvedPlan.name)
            .putLong("role_expires_at_${role.name}", expiresAt)
            .putString("role_signature_${role.name}", roleSig)
            .putLong("role_activated_at_${role.name}", activatedAt)

        // If activating MUSRIB, also mirror to legacy keys for 100% backward compatibility
        if (role == LicenseRole.MUSRIB) {
            val legacySig = sha256(deviceCode + SECRET_ACTIVATION_SALT + resolvedPlan.name + expiresAt)
            editor
                .putBoolean(KEY_LEGACY_IS_ACTIVATED, true)
                .putString(KEY_LEGACY_SUBSCRIPTION_PLAN, resolvedPlan.name)
                .putLong(KEY_LEGACY_EXPIRES_AT, expiresAt)
                .putString(KEY_LEGACY_ACTIVATION_SIGNATURE, legacySig)
                .putLong(KEY_LEGACY_ACTIVATED_AT, activatedAt)
        }

        editor.apply()
        return ActivationResult(role, resolvedPlan, expiresAt, label)
    }

    /**
     * خطة تُعرض للمدة فقط عندما يحمل التصريح مدة مخصّصة لا يوافقها اسم خطة في هذه النسخة.
     * التصنيف تقريبي للعرض، أما **المدة المعروضة فموقّعة** فلا يأتي التقريب على حقّ المستخدم.
     */
    private fun fallbackPlanFor(durationDays: Int): SubscriptionPlan = when {
        durationDays >= SubscriptionPlan.LIFETIME.durationDays -> SubscriptionPlan.LIFETIME
        durationDays >= SubscriptionPlan.YEARLY.durationDays -> SubscriptionPlan.YEARLY
        else -> SubscriptionPlan.MONTHLY
    }

    /**
     * Checks if a specific role is active on this device.
     */
    fun isRoleActivated(context: Context, role: LicenseRole): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val isRoleActive = sp.getBoolean("role_activated_${role.name}", false)

        if (isRoleActive) {
            val storedSig = sp.getString("role_signature_${role.name}", null) ?: return false
            val storedPlan = sp.getString("role_plan_${role.name}", null) ?: return false
            val storedExpiresAt = sp.getLong("role_expires_at_${role.name}", 0L)

            if (System.currentTimeMillis() > storedExpiresAt) return false

            val currentDeviceCode = getDeviceCode(context)
            val expectedSig = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + role.name + storedPlan + storedExpiresAt)
            if (storedSig == expectedSig) return true
        }

        // Fallback for MUSRIB: check legacy preferences
        if (role == LicenseRole.MUSRIB) {
            val legacyActive = sp.getBoolean(KEY_LEGACY_IS_ACTIVATED, false)
            if (!legacyActive) return false

            val storedSig = sp.getString(KEY_LEGACY_ACTIVATION_SIGNATURE, null) ?: return false
            val storedPlan = sp.getString(KEY_LEGACY_SUBSCRIPTION_PLAN, null) ?: return false
            val storedExpiresAt = sp.getLong(KEY_LEGACY_EXPIRES_AT, 0L)

            if (System.currentTimeMillis() > storedExpiresAt) return false

            val currentDeviceCode = getDeviceCode(context)
            val expectedSig = sha256(currentDeviceCode + SECRET_ACTIVATION_SALT + storedPlan + storedExpiresAt)
            return storedSig == expectedSig
        }

        return false
    }

    /**
     * Legacy method: checks whether MUSRIB irrigation service is activated.
     */
    fun isActivated(context: Context): Boolean = isRoleActivated(context, LicenseRole.MUSRIB)

    /**
     * Returns the active plan for a specific role, or null if expired/unlicensed.
     */
    fun getRoleActivePlan(context: Context, role: LicenseRole): SubscriptionPlan? {
        if (!isRoleActivated(context, role)) return null
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val planName = sp.getString("role_plan_${role.name}", null)
            ?: (if (role == LicenseRole.MUSRIB) sp.getString(KEY_LEGACY_SUBSCRIPTION_PLAN, null) else null)
            ?: return null
        return runCatching { SubscriptionPlan.valueOf(planName) }.getOrNull()
    }

    /**
     * Legacy method: returns active plan for MUSRIB role.
     */
    fun getActivePlan(context: Context): SubscriptionPlan? = getRoleActivePlan(context, LicenseRole.MUSRIB)

    /**
     * Returns expiration epoch ms for a role (0 if none).
     */
    fun getRoleExpiresAt(context: Context, role: LicenseRole): Long {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val roleExpiry = sp.getLong("role_expires_at_${role.name}", 0L)
        if (roleExpiry > 0L) return roleExpiry
        if (role == LicenseRole.MUSRIB) {
            return sp.getLong(KEY_LEGACY_EXPIRES_AT, 0L)
        }
        return 0L
    }

    /**
     * Returns remaining days for a specific role.
     */
    fun getRoleRemainingDays(context: Context, role: LicenseRole): Int {
        val expiresAt = getRoleExpiresAt(context, role)
        val remainingMs = expiresAt - System.currentTimeMillis()
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 86399999L) / (24L * 3600L * 1000L)).toInt()
    }

    /**
     * Legacy method: remaining days for MUSRIB.
     */
    fun getRemainingDays(context: Context): Int = getRoleRemainingDays(context, LicenseRole.MUSRIB)

    /**
     * Checks whether a free launch period is active for the isolation/community.
     */
    fun isFreeLaunchPeriodActive(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_CONFIG_FREE_LAUNCH_PERIOD, false)
    }

    /**
     * Configures launch limits dynamically (cached locally from Firestore system_config/limits).
     */
    fun updateSystemConfigLimits(
        context: Context,
        isFreeLaunchPeriod: Boolean,
        musribLimit: Int = FREE_OPERATIONS_LIMIT,
        dallalListingsLimit: Int = 10,
        dallalDealsLimit: Int = 5
    ) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CONFIG_FREE_LAUNCH_PERIOD, isFreeLaunchPeriod)
            .putInt(KEY_CONFIG_LIMIT_MUSRIB, musribLimit)
            .putInt(KEY_CONFIG_LIMIT_DALLAL_LISTINGS, dallalListingsLimit)
            .putInt(KEY_CONFIG_LIMIT_DALLAL_DEALS, dallalDealsLimit)
            .apply()
    }

    /**
     * Returns the effective operation limit for Musrib.
     */
    fun getEffectiveMusribLimit(context: Context): Int {
        if (isFreeLaunchPeriodActive(context)) return 2000
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CONFIG_LIMIT_MUSRIB, FREE_OPERATIONS_LIMIT)
    }

    /**
     * Determines whether an operation (water session or voucher) can be performed.
     */
    fun canPerformOperation(context: Context, currentOperationsCount: Int): Boolean {
        if (isRoleActivated(context, LicenseRole.MUSRIB)) return true
        return currentOperationsCount < getEffectiveMusribLimit(context)
    }

    /**
     * Remaining free operations for Musrib.
     */
    fun getRemainingOperations(context: Context, currentOperationsCount: Int): Int {
        if (isRoleActivated(context, LicenseRole.MUSRIB)) return Int.MAX_VALUE
        return (getEffectiveMusribLimit(context) - currentOperationsCount).coerceAtLeast(0)
    }

    /**
     * Checks if Dallal can post a new crop listing.
     */
    fun canDallalPostListing(context: Context, currentListingsCount: Int): Boolean {
        if (isRoleActivated(context, LicenseRole.DALLAL)) return true
        if (isFreeLaunchPeriodActive(context)) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val limit = sp.getInt(KEY_CONFIG_LIMIT_DALLAL_LISTINGS, 10)
        return currentListingsCount < limit
    }

    /**
     * Checks if Dallal can create a new deal/settlement.
     */
    fun canDallalCreateDeal(context: Context, currentDealsCount: Int): Boolean {
        if (isRoleActivated(context, LicenseRole.DALLAL)) return true
        if (isFreeLaunchPeriodActive(context)) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val limit = sp.getInt(KEY_CONFIG_LIMIT_DALLAL_DEALS, 5)
        return currentDealsCount < limit
    }

    /**
     * Checks if Farmer has verified status (for advanced analytics and profit calculations).
     */
    fun isFarmerVerified(context: Context): Boolean = isRoleActivated(context, LicenseRole.FARMER)

    /**
     * Formats WhatsApp message URL for sending the device code to the developer.
     */
    fun getWhatsAppActivationUrl(
        deviceCode: String,
        requestedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
        role: LicenseRole = LicenseRole.MUSRIB
    ): String {
        val msg = """
السلام عليكم يا باشمهندس عمر،
أود تفعيل تطبيق بيننا (${role.titleArabic} — ${requestedPlan.titleArabic}).
كود جهازي هو:
$deviceCode
        """.trimIndent()
        return "https://wa.me/$DEVELOPER_PHONE?text=" + Uri.encode(msg)
    }
}
