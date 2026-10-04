package com.baynana.domain.license

/**
 * فاحص التصاريح: يقرّر — بلا Android وبلا قاعدة ولا شبكة — هل يُقبل هذا الرمز؟
 *
 * الترتيب مقصود، لأن الرسالة التي يقرأها المزارع يجب أن تصف حالته الحقيقية لا أول فحص فشل:
 * 1. الصيغة (هل هو تصريح أصلًا؟)
 * 2. وجود المفتاح العام (هل تستطيع هذه النسخة التحقق؟)
 * 3. التوقيع (هل صدر عنّا؟) — قبل أي حديث عن الجهاز أو الاسترداد، وإلا أوحينا أن الرمز صحيح.
 * 4. الجهاز (لجهاز من صدر؟)
 * 5. الاسترداد (هل استُردّ سابقًا؟) — وهذا هو مانع الـreplay.
 * 6. المدة (هل انتهت قبل أن يستردّها؟)
 */
interface LicenseSignatureVerifier {
    /** يتحقق أن [signatureBytes] توقيع صحيح لـ[payloadBytes] بالمفتاح العام المعروف. */
    fun verify(payloadBytes: ByteArray, signatureBytes: ByteArray): Boolean
}

/** أسباب الرفض، وكل سبب يحمل رسالته العربية الظاهرة. */
enum class LicenseRejection(val messageArabic: String) {
    MALFORMED("صيغة الرمز غير مكتملة أو معدّلة."),
    NO_PUBLIC_KEY("هذه النسخة لا تحمل مفتاح التصاريح العام، فلا تستطيع التحقق من الرمز الموقّع."),
    BAD_SIGNATURE("توقيع الرمز لا يطابق محتواه: لم يصدر هذا التصريح عن بيننا."),
    WRONG_DEVICE("هذا التصريح صدر لجهاز آخر. اطلب تصريحًا لكود جهازك."),
    ALREADY_REDEEMED("هذا التصريح استُردّ سابقًا ولا يمدّد مرة ثانية."),
    PERIOD_ENDED("انتهت مدة هذا التصريح قبل استرداده. اطلب تصريحًا جديدًا."),
    UNKNOWN_TERM("التصريح لا يوافق مهنة أو خطة تعرفها هذه النسخة.")
}

/** نتيجة الفحص. */
sealed interface LicenseCheck {
    data class Granted(val grant: LicenseToken.Grant) : LicenseCheck

    data class Rejected(val reason: LicenseRejection) : LicenseCheck
}

object LicenseAuthority {

    /** أقصى تقدّم مقبول لساعة الجهاز على تاريخ إصدار التصريح (تسامح ساعات، لا أيام). */
    const val CLOCK_TOLERANCE_MS = 6L * 60L * 60L * 1000L

    /**
     * @param entered ما كتبه المستخدم أو لصقه.
     * @param deviceCode كود هذا الجهاز (بأي صيغة، يُطبَّع داخليًا).
     * @param nowMillis الآن.
     * @param verifier فاحص التوقيع، أو `null` إن كانت النسخة بلا مفتاح عام.
     * @param redeemedKeys مفاتيح ما استُردّ سابقًا (من [LicenseToken.redemptionKey]).
     * @param knownRoles المهن التي تعرفها هذه النسخة، لرفض تصريح لمهنة أُلغيت أو أُضيفت لاحقًا.
     */
    fun check(
        entered: String,
        deviceCode: String,
        nowMillis: Long,
        verifier: LicenseSignatureVerifier?,
        redeemedKeys: Set<String>,
        knownRoles: Set<String> = emptySet()
    ): LicenseCheck {
        val parsed = LicenseToken.parse(entered)
        return when (parsed) {
            is LicenseToken.Parsed.NotAToken -> LicenseCheck.Rejected(LicenseRejection.MALFORMED)
            is LicenseToken.Parsed.Malformed -> LicenseCheck.Rejected(LicenseRejection.MALFORMED)
            is LicenseToken.Parsed.Signed -> checkSigned(parsed, deviceCode, nowMillis, verifier, redeemedKeys, knownRoles)
        }
    }

    /**
     * الفحص لتصريح ثم التأكد من تطابق الحقول، مكتوب كسلسلة صريحة: كل خطوة إمّا تُرجع رفضًا
     * برسالته أو تُمرّر الاستحقاق. لا `!!` ولا استنتاج ضمني.
     */
    private fun checkSigned(
        signed: LicenseToken.Parsed.Signed,
        deviceCode: String,
        nowMillis: Long,
        verifier: LicenseSignatureVerifier?,
        redeemedKeys: Set<String>,
        knownRoles: Set<String>
    ): LicenseCheck {
        val checker = verifier ?: return LicenseCheck.Rejected(LicenseRejection.NO_PUBLIC_KEY)

        val signatureOk = checker.verify(signed.payloadBytes, signed.signatureBytes)
        if (!signatureOk) return LicenseCheck.Rejected(LicenseRejection.BAD_SIGNATURE)

        // الحمولة تُتحقق *بعد* التوقيع: لا معنى لرسالة «حقل ناقص» على نصّ لم يصدر عنّا أصلًا.
        val grant = LicenseToken.decodeGrant(signed.payloadText)
            ?: return LicenseCheck.Rejected(LicenseRejection.MALFORMED)

        if (knownRoles.isNotEmpty() && grant.role !in knownRoles) {
            return LicenseCheck.Rejected(LicenseRejection.UNKNOWN_TERM)
        }

        val expectedDevice = LicenseToken.normalizeDeviceCode(deviceCode)
        if (grant.deviceCode != expectedDevice) {
            return LicenseCheck.Rejected(LicenseRejection.WRONG_DEVICE)
        }

        if (LicenseToken.redemptionKey(grant) in redeemedKeys) {
            return LicenseCheck.Rejected(LicenseRejection.ALREADY_REDEEMED)
        }

        if (grant.expiresAt <= nowMillis) {
            return LicenseCheck.Rejected(LicenseRejection.PERIOD_ENDED)
        }

        // ساعة الجهاز لا تمنح ولا تسلب مدة: `expiresAt` موقّع في الحمولة، فالجهاز لا يحسب المدة
        // بنفسه. ولهذا لا نرفض تصريحًا لأن ساعة المستخدم متقدّمة أو متأخّرة — نُسجّل الفرق فقط
        // في سجل الأحداث ([CLOCK_TOLERANCE_MS] حدّ يُستعمل للتنبيه في التدقيق، لا للرفض).
        return LicenseCheck.Granted(grant)
    }
}
