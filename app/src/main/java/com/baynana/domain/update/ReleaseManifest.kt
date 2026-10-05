package com.baynana.domain.update

import com.baynana.domain.license.Base64Codec

/**
 * ملفّ الإصدار الموقّع (ح٢٠) — الصيغة والقواعد، بلا Android وبلا شبكة.
 *
 * **المشكلة التي يحلّها:** التطبيق يُوزَّع على أجهزة متفرّقة، ثم تُكتشف فيه علّة تُفسد الحساب.
 * فتًلا قناة تحديث: المستخدم يبقى على نسخته إلى الأبد، أو — وهذا أسوأ — يُطلب منه تنزيل ملفّ من
 * رابط يظهر في واتساب، فلا أحد يضمن أنه هو الملفّ ولا أنه لم يُستبدل في الطريق.
 *
 * **الحل:** ملفّ نصّي واحد منشور على شبكة، يحمل آخر إصدار ورابطه وبصمته، **موقّعًا بمفتاح المالك
 * الخاص**. والتطبيق يتحقق بالمفتاح العام وحده، فلا يستطيع أحد — ولا من يخترق الاستضافة نفسها —
 * أن يعرض على الأجهزة تحديثًا مزيّفًا.
 *
 * **الصيغة:** `BNR1.<الحمولة بترميز base64url>.<التوقيع بترميز base64url>`، والحمولة حقول نصّية
 * مفصولة بـ`|` بالترتيب: `الإصدار|رقم الإصدار|اسم الإصدار|أقدم مدعوم|رابط الملفّ|بصمة sha256|تاريخ
 * النشر|رسالة عربية`. والتوقيع على **نصّ الحمولة as-is** (كما في التصاريح) فلا يغيّر ترميزٌ معنًى.
 *
 * **ملفّ غير موقّع = ملفّ مرفوض.** لا نقبل JSON عارٍ ولو كان صحيحًا، لأن قبوله يجعل القناة كلها
 * بلا حماية: يكفي أن يُخترق المستضيف ليصل للأجهزة «تحديث» لم يوقّعه المالك.
 *
 * **قواعد التحقق ليست تزيّدًا:** كل شرط هنا يمنع عطبًا يمكن أن يحدث فعلًا:
 * 1. **https فقط** — على http يستطيع الوسيط استبدال الملفّ (والتوقيع يحمي المحتوى، لكن الرابط
 *    نفسه هو ما ينزّل منه الملفّ).
 * 2. **بصمة sha256 مكتملة (٦٤ حرفًا hex)** — بلا بصمة، التوقيع يحمي بيانات الملفّ لا محتوى الـAPK.
 * 3. **أقدم مدعوم لا يسبق الأحدث** — وإلا لَقيل لكل الأجهزة «أنت غير مدعوم».
 * 4. **رقم إصدار صحيح موجب، واسمه بصيغة `رقم.رقم[.رقم]`** — الأرقام هي ما يُقارن، فلو فسدت فسد
 *    قرار «هل أحدّث؟».
 * 5. **رسالة عربية غير فارغة** وبلا `|` ولا أسطر جديدة — الرسالة تُقرأ للإنسان، والمستخدم يستحق
 *    أن يعرف **ما تغيّر** قبل أن يضغط زرًّا يغيّر تطبيقه.
 */
object ReleaseManifest {

    /** علامة ملفّ الإصدار. رأيتها تعني: هذا ملفّ موقّع من «بيننا»، لا مجرّد نصّ. */
    const val PREFIX = "BNR1"

    const val PAYLOAD_VERSION = "1"

    private const val FIELD_SEPARATOR = "|"
    private const val TOKEN_SEPARATOR = "."
    private const val FIELD_COUNT = 8

    /** أطول نسخة نصّية لاسم إصدار معقولة (`1.2.3`). */
    private const val MAX_VERSION_NAME_LENGTH = 20
    private const val MAX_URL_LENGTH = 300
    private const val MAX_MESSAGE_LENGTH = 500
    private const val SHA256_HEX_LENGTH = 64

    private val VERSION_NAME_PATTERN = Regex("^[0-9]+\\.[0-9]+(\\.[0-9]+)?$")
    private val SHA256_PATTERN = Regex("^[0-9a-f]{$SHA256_HEX_LENGTH}$")

    /**
     * إصدار منشور. كل الحقول نصّية في الملفّ، وتُقرأ هنا إلى ما تحتاجه الشيفرة:
     * المقارنة برقم الإصدار، والتنزيل بالرابط والبصمة، والرسالة كما هي للإنسان.
     */
    data class Release(
        val versionCode: Int,
        val versionName: String,
        val minSupportedVersionCode: Int,
        val apkUrl: String,
        val apkSha256: String,
        val publishedAt: Long,
        val messageArabic: String
    ) {
        /** هل هذا الإصدار أحدث من نسخة الجهاز؟ */
        fun isNewerThan(currentVersionCode: Int): Boolean = versionCode > currentVersionCode

        /** هل نسخة الجهاز أقدم من الحد المدعوم (فتوقف عن العمل الصحيح)؟ */
        fun isUnsupported(currentVersionCode: Int): Boolean = currentVersionCode < minSupportedVersionCode
    }

    /** نتيجة قراءة ملفّ الإصدار. الفصل بين الأنواع مقصود: سبب الرفض يجب أن يُقال بدقّة. */
    sealed interface Readout {
        /** ليس ملفّ إصدار أصلًا (مثلًا صفحة خطأ HTML، أو نصّ فارغ). */
        data class NotARelease(val reasonArabic: String) : Readout

        /** بدأ بالعلامة `BNR1` لكنه ناقص أو مشوّه: رفض، لا ترميم. */
        data class Malformed(val reasonArabic: String) : Readout

        /** التوقيع غائب أو مرفوض — ولو بدت الحقول سليمة. */
        data class SignatureRejected(val reasonArabic: String) : Readout

        /** ملفّ سليم موقّع من المالك. */
        data class Verified(val release: Release) : Readout
    }

    /**
     * الحمولة بالنصّ كما تُوقَّع: الحقول بالترتيب، والرسالة آخرها (فلا يلزم أن يخلو رابطٌ من `|`).
     */
    fun buildPayload(release: Release): String = listOf(
        PAYLOAD_VERSION,
        release.versionCode.toString(),
        release.versionName,
        release.minSupportedVersionCode.toString(),
        release.apkUrl,
        release.apkSha256.lowercase(),
        release.publishedAt.toString().ifEmpty { "0" },
        release.messageArabic
    ).joinToString(FIELD_SEPARATOR)

    /** يبني الرمز الكامل من حمولة موقّعة: الشكل الذي يُنشر ملفًّا. */
    fun buildToken(payload: String, signatureBase64Url: String): String =
        "$PREFIX$TOKEN_SEPARATOR${Base64Codec.encodeUrl(payload.toByteArray(Charsets.UTF_8))}" +
            "$TOKEN_SEPARATOR$signatureBase64Url"

    /** الحمولة النصّية للرمز، أو `null` إن لم يكن رمزًا بالصيغة. */
    fun payloadOf(token: String): String? {
        val parts = token.trim().split(TOKEN_SEPARATOR)
        if (parts.size != 3 || parts[0] != PREFIX) return null
        val bytes = Base64Codec.decode(parts[1]) ?: return null
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * يقرأ ملفّ الإصدار ويتحقق من توقيعه ثم من كل حقل.
     *
     * [verify] يُمرَّر من طبقة التحقق (المفتاح العام في التطبيق، أو مفتاح اختبار في الاختبارات).
     * ولو أردنا قبول ملفّ بلا توقيع لكان المعنى: لا حماية للقناة أصلًا.
     */
    fun read(token: String, verify: (payloadBytes: ByteArray, signatureBytes: ByteArray) -> Boolean): Readout {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return Readout.NotARelease("ملفّ الإصدار فارغ")
        if (!trimmed.startsWith(PREFIX)) {
            return Readout.NotARelease("هذا ليس ملفّ إصدار من «بيننا» (لا يحمل العلامة $PREFIX)")
        }

        val parts = trimmed.split(TOKEN_SEPARATOR)
        if (parts.size != 3) return Readout.Malformed("ملفّ الإصدار ناقص الأجزاء")
        val payloadBytes = Base64Codec.decode(parts[1])
            ?: return Readout.Malformed("حمولة ملفّ الإصدار ليست base64url صحيحًا")
        val signatureBytes = Base64Codec.decode(parts[2])
            ?: return Readout.Malformed("توقيع ملفّ الإصدار ليس base64url صحيحًا")

        if (!runCatching { verify(payloadBytes, signatureBytes) }.getOrDefault(false)) {
            return Readout.SignatureRejected("توقيع ملفّ الإصدار مرفوض: الملفّ ليس من المالك أو تغيّر بعد التوقيع")
        }

        return parsePayload(String(payloadBytes, Charsets.UTF_8))
    }

    /** يقرأ الحمولة المفكوكة بعد نجاح التوقيع، ويتحقق من كل حقل برسالته. */
    fun parsePayload(payload: String): Readout {
        val fields = payload.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) {
            return Readout.Malformed("حمولة ملفّ الإصدار فيها ${fields.size} حقلًا والمتوقع $FIELD_COUNT")
        }
        if (fields[0] != PAYLOAD_VERSION) {
            return Readout.Malformed("نسخة حمولة غير معروفة: ${fields[0].take(8)} — حدّث التطبيق يدويًا")
        }

        val versionCode = fields[1].toIntOrNull()
            ?: return Readout.Malformed("رقم الإصدار ليس عددًا: ${fields[1].take(12)}")
        if (versionCode <= 0) return Readout.Malformed("رقم الإصدار يجب أن يكون موجبًا")

        val versionName = fields[2].trim()
        if (versionName.isEmpty() || versionName.length > MAX_VERSION_NAME_LENGTH ||
            !VERSION_NAME_PATTERN.matches(versionName)
        ) {
            return Readout.Malformed("اسم الإصدار غير مقبول: «${versionName.take(MAX_VERSION_NAME_LENGTH)}»")
        }

        val minSupported = fields[3].toIntOrNull()
            ?: return Readout.Malformed("أقدم إصدار مدعوم ليس عددًا: ${fields[3].take(12)}")
        if (minSupported < 1 || minSupported > versionCode) {
            return Readout.Malformed("أقدم إصدار مدعوم ($minSupported) غير منطقي مع الإصدار ($versionCode)")
        }

        val url = fields[4].trim()
        if (url.length > MAX_URL_LENGTH || !url.startsWith("https://")) {
            // روابط http مرفوضة صراحةً: الوسيط يستطيع أن يستبدل الملفّ لو كان الرابط غير مشفّر،
            // والتوقيع يحمي بيانات الملفّ لا الملفّ الذي ينزل من هذا الرابط.
            return Readout.Malformed("رابط الملفّ يجب أن يبدأ بـhttps:// وبطول معقول")
        }

        val sha256 = fields[5].trim().lowercase()
        if (!SHA256_PATTERN.matches(sha256)) {
            return Readout.Malformed("بصمة الملفّ ليست sha256 كاملًا (٦٤ حرفًا hex)")
        }

        val publishedAt = fields[6].toLongOrNull()
            ?: return Readout.Malformed("تاريخ النشر ليس عددًا (مللي ثانية)")
        if (publishedAt <= 0) return Readout.Malformed("تاريخ النشر غير معقول")

        val message = fields[7].trim()
        if (message.isEmpty()) {
            return Readout.Malformed("ملفّ بلا رسالة: المستخدم يستحق أن يعرف ما تغيّر")
        }
        if (message.length > MAX_MESSAGE_LENGTH) {
            return Readout.Malformed("رسالة الملفّ أطول من $MAX_MESSAGE_LENGTH حرفًا")
        }
        if (message.contains('\n') || message.contains('\r')) {
            return Readout.Malformed("رسالة الملفّ تحتوي سطرًا جديدًا — والملفّ سطر واحد")
        }

        return Readout.Verified(
            Release(
                versionCode = versionCode,
                versionName = versionName,
                minSupportedVersionCode = minSupported,
                apkUrl = url,
                apkSha256 = sha256,
                publishedAt = publishedAt,
                messageArabic = message
            )
        )
    }
}
