package com.baynana.domain.license

/**
 * التصريح الموقّع (ح١٣) — الصيغة والقواعد، بلا Android وبلا شبكة.
 *
 * **المشكلة التي يحلّها:** كان الترخيص يتولّد داخل التطبيق نفسه من سرّ مكتوب في الكود
 * (`SECRET_ACTIVATION_SALT`)، فأي من يقرأ الكود يستطيع توليد مفاتيح تفعيل بلا حساب، وكان إدخال
 * الرمز نفسه مرتين **يمدّد المدة** لأنه لا أحد يسجّل أن الرمز استُردّ (LIC-01 في مراجعة المخاطر).
 *
 * **الحل:** التصريح وحدة بيانات يوقّعها المالك بمفتاحه الخاص (ECDSA P-256)، والتطبيق يتحقق منها
 * بالمفتاح العام وحده — فلا يستطيع أحد توليد تصريح ولو قرأ الكود كله. ولكل تصريح
 * `licenseId` فريد، فيُسجّل عند أول استرداد ويمنع أي تمديد ثانٍ.
 *
 * **الصيغة:** `BNNA1.<payload بترميز base64url>.<التوقيع بترميز base64url>`
 * ونصّ الحمولة حقول مفصولة بـ`|` بالترتيب:
 * `الإصدار|licenseId|كود الجهاز|المهنة|الخطة|عدد الأيام|تاريخ الإصدار|تاريخ الانتهاء`
 *
 * التوقيع على **نصّ الحمولة كما هو** (لا على الحمولة المفكوكة)، فلا يغيّر ترميزٌ أو مسافة معنًى.
 */
object LicenseToken {

    /** علامة التصاريح الموقّعة. رأيتها في نصّ الرمز تعني: هذه ليست مفاتيح النسخة القديمة. */
    const val PREFIX = "BNNA1"

    const val PAYLOAD_VERSION = "1"

    private const val FIELD_SEPARATOR = "|"
    private const val TOKEN_SEPARATOR = "."

    /** عدد حقول الحمولة — ثابت في الإصدار 1، وأي اختلاف يعني صيغة لا نفهمها. */
    private const val FIELD_COUNT = 8

    private const val MAX_DURATION_DAYS = 3650
    private const val MAX_LICENSE_ID_LENGTH = 64
    private const val MAX_DEVICE_CODE_LENGTH = 40
    private const val MAX_TERM_LENGTH = 16

    /**
     * استحقاق موقّع. كل حقول الزمن أرقام صحيحة بالمللي ثانية (بلا `java.time`، والقاعدة نفسها في
     * كل المشروع: الزمن `Long` بالمللي ثانية كما يقرأه النظام).
     */
    data class Grant(
        val licenseId: String,
        val deviceCode: String,
        val role: String,
        val plan: String,
        val durationDays: Int,
        val issuedAt: Long,
        val expiresAt: Long
    )

    /** نتيجة قراءة المدخل: ليست تصريحًا، أو تصريحًا مشوّهًا، أو تصريحًا مكتمل الأجزاء. */
    sealed interface Parsed {
        /** مدخل ليس تصريحًا موقّعًا أصلًا — قد يكون رمزًا قديمًا، فلا نحكم على صيغته. */
        data object NotAToken : Parsed

        /** بدأ بالعلامة `BNNA1` لكنه ناقص أو مُعدَّل: هذا رفض لا ترميم. */
        data class Malformed(val detailArabic: String) : Parsed

        /** الجزآن حاضران: نصّ الحمولة (هو ما يُوقَّع) وتوقيعه. */
        data class Signed(
            val payloadText: String,
            val payloadBytes: ByteArray,
            val signatureBytes: ByteArray
        ) : Parsed
    }

    /** كود الجهاز يُقارَن بعد تجريد الفواصل وحالة الأحرف، فلا يفرق `msrb-8f42-9d1b` عن الصيغة المطبوعة. */
    fun normalizeDeviceCode(code: String): String =
        code.filter { it.isLetterOrDigit() }.uppercase()

    /** حمولة التصريح كما تُوقَّع بالحرف: لا مسافات ولا ترتيب آخر. */
    fun payloadText(grant: Grant): String = listOf(
        PAYLOAD_VERSION,
        grant.licenseId,
        normalizeDeviceCode(grant.deviceCode),
        grant.role.uppercase(),
        grant.plan.uppercase(),
        grant.durationDays.toString(),
        grant.issuedAt.toString(),
        grant.expiresAt.toString()
    ).joinToString(FIELD_SEPARATOR)

    /** يبني الرمز النهائي من الحمولة وتوقيعها. */
    fun encode(payloadText: String, signatureBytes: ByteArray): String =
        PREFIX + TOKEN_SEPARATOR +
            Base64Codec.encodeUrl(payloadText.toByteArray(Charsets.UTF_8)) + TOKEN_SEPARATOR +
            Base64Codec.encodeUrl(signatureBytes)

    /** هل يبدو المدخل تصريحًا موقّعًا (بعلامته)؟ يُستعمل للتفريق قبل مسار المفاتيح القديمة. */
    fun looksLikeToken(entered: String): Boolean =
        entered.trim().startsWith(PREFIX + TOKEN_SEPARATOR, ignoreCase = true)

    /**
     * يقرأ الرمز إلى أجزائه بلا تحقق توقيع. التحقق من التوقيع مسؤولية [LicenseAuthority]،
     * والتفريق مقصود: القراءة والتحقق مسؤوليتان، والخلط بينهما هو ما يجعل الأخطاء غامضة.
     */
    fun parse(entered: String): Parsed {
        val trimmed = entered.trim()
        if (!looksLikeToken(trimmed)) return Parsed.NotAToken

        val parts = trimmed.split(TOKEN_SEPARATOR)
        if (parts.size != 3) {
            return Parsed.Malformed("الرمز يجب أن يتكون من ثلاثة أجزاء: العلامة والحمولة والتوقيع.")
        }
        if (!parts[0].equals(PREFIX, ignoreCase = true)) {
            return Parsed.Malformed("علامة الرمز غير معروفة.")
        }
        if (parts[1].isEmpty() || parts[2].isEmpty()) {
            return Parsed.Malformed("أحد جزأي الرمز فارغ.")
        }
        val payloadBytes = Base64Codec.decode(parts[1])
            ?: return Parsed.Malformed("حمولة الرمز ليست ترميزًا صحيحًا.")
        val signatureBytes = Base64Codec.decode(parts[2])
            ?: return Parsed.Malformed("توقيع الرمز ليس ترميزًا صحيحًا.")
        if (signatureBytes.size < 8) {
            return Parsed.Malformed("طول التوقيع لا يصلح.")
        }
        val payloadText = payloadBytes.toString(Charsets.UTF_8)
        if (!payloadText.contains(FIELD_SEPARATOR)) {
            return Parsed.Malformed("حمولة الرمز لا تشبه تصريحًا.")
        }
        return Parsed.Signed(payloadText, payloadBytes, signatureBytes)
    }

    /** يقرأ حقول الحمولة ويتحقق من معانيها، أو `null` إن كان فيها ما يستحيل قبوله. */
    fun decodeGrant(payloadText: String): Grant? {
        val fields = payloadText.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null
        if (fields.any { it.isEmpty() }) return null
        if (fields[0] != PAYLOAD_VERSION) return null

        val licenseId = fields[1]
        if (licenseId.length > MAX_LICENSE_ID_LENGTH || !isIdShaped(licenseId)) return null

        val deviceCode = normalizeDeviceCode(fields[2])
        if (deviceCode.isEmpty() || deviceCode.length > MAX_DEVICE_CODE_LENGTH) return null

        val role = fields[3]
        val plan = fields[4]
        if (!isTermShaped(role) || !isTermShaped(plan)) return null

        val durationDays = fields[5].toIntOrNull() ?: return null
        if (durationDays < 1 || durationDays > MAX_DURATION_DAYS) return null

        val issuedAt = fields[6].toLongOrNull() ?: return null
        val expiresAt = fields[7].toLongOrNull() ?: return null
        if (issuedAt <= 0L || expiresAt <= issuedAt) return null

        return Grant(
            licenseId = licenseId,
            deviceCode = deviceCode,
            role = role,
            plan = plan,
            durationDays = durationDays,
            issuedAt = issuedAt,
            expiresAt = expiresAt
        )
    }

    /** مفتاح الاسترداد للتصريح الموقّع: بهذا المفتاح يُسجَّل أنه استُردّ، وبه يُرفض ثانيةً. */
    fun redemptionKey(grant: Grant): String = "signed:" + grant.licenseId

    /**
     * مفتاح الاسترداد للمفاتيح القديمة (التي وُلّدت بالسرّ المكتوب): نستخدم بصمة الرمز نفسها،
     * فلا نحتاج تخزين الرمز ولا نُظهره في أي شاشة.
     */
    fun legacyRedemptionKey(keyFingerprint: String): String = "legacy:" + keyFingerprint

    private fun isIdShaped(value: String): Boolean =
        value.length >= 8 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun isTermShaped(value: String): Boolean =
        value.length in 2..MAX_TERM_LENGTH && value.all { it in 'A'..'Z' }
}
