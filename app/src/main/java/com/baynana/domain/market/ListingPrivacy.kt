package com.baynana.domain.market

/**
 * خصوصية العرض (ح٩) — الطبقة التي تحوّل بيانات المزارع إلى **عرض عام لا يمكن أن يحتوي هاتفه**.
 *
 * المبدأ: الخصوصية ليست «تجاهل حقل عند العرض»، بل **بنية نوعية**: [MarketEngine.PublicListing]
 * لا يملك حقلًا لهاتف المزارع أصلًا، فلا يمكن أن يتسرّب من شاشة أو PDF أو مشاركة واتساب سهوًا.
 * وفوق ذلك: تنقيح الموقع إلى المحافظة وحدها، ومسح النصّ الحرّ من أي رقم يشبه هاتفًا، ومن أي ذكر
 * لمبالغ ديون (العرض العام بيان محصول، لا كشف حساب).
 */
object ListingPrivacy {

    /** مكان دقيق: للمزارع والدلال فقط. */
    data class PrivateLocation(
        val governorate: String = "",
        val district: String = "",
        val village: String = "",
        val landmark: String = ""
    )

    /** ما يراه الناس: المحافظة وحدها. لا مديرية ولا قرية ولا علامة تدلّ على البيت. */
    data class PublicLocation(val governorate: String)

    fun publicLocation(private: PrivateLocation): PublicLocation =
        PublicLocation(governorate = private.governorate.trim())

    /** نتيجة فحص نصّ حرّ: هل يحمل ما لا يجوز في عرض عام؟ */
    data class ScanResult(val safe: Boolean, val findings: List<String>) {
        val summary: String get() = if (safe) "لا مخالفة" else findings.joinToString("؛ ")
    }

    private val arabicIndicDigits = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    /** يحوّل الأرقام العربية-الهندية والفارسية إلى أرقام لاتينية، لأن التسرّب لا يفرّق بينهما. */
    fun normalizeDigits(text: String): String = buildString(text.length) {
        text.forEach { char ->
            val mapped = when {
                char in arabicIndicDigits -> arabicIndicDigits.getValue(char)
                char in '۰'..'۹' -> ('0' + (char - '۰'))
                else -> char
            }
            append(mapped)
        }
    }

    /**
     * يمشي على النصّ ويجمع كل «مقطع أرقام»: الأرقام وما بينها من فواصل يقبلها الناس في كتابة
     * الهاتف (مسافة، شرطة، نقطة، خط مائل، قوسان). المقطع الذي يجمع سبعة أرقام أو أكثر مشبوه،
     * سواء كُتب `777123456` أو `777 123 456` أو `+967-777-123-456`.
     *
     * ولا نلمس قطعًا يفصلها حرف: «20 كيس … 35000» مقطعان لا واحد، فلا يُخلط سعر بكمية.
     */
    private fun digitSpans(text: String): List<IntRange> {
        val separators = " -._/()"
        val spans = mutableListOf<IntRange>()
        var start = -1
        var lastDigit = -1
        var digits = 0

        fun close() {
            if (start >= 0 && digits >= 7) spans += start..lastDigit
            start = -1
            digits = 0
        }

        text.forEachIndexed { index, char ->
            val normalized = if (char in arabicIndicDigits) arabicIndicDigits.getValue(char) else char
            when {
                normalized in '0'..'9' -> {
                    if (start < 0) start = index
                    lastDigit = index
                    digits++
                }
                char in separators -> Unit // فاصل مسموح داخل المقطع
                else -> close()
            }
        }
        close()
        return spans
    }

    /** الأرقام التي تظهر في المقطع بلا فواصل، للتقرير وللمقارنة مع رقم معروف. */
    private fun digitsOf(text: String, span: IntRange): String =
        text.substring(span.first, span.last + 1).filter { it.isDigit() }.let(::normalizeDigits)

    private val debtWords = listOf(
        "دين", "دينه", "ديون", "مديون", "باقي عليه", "الباقي عليه",
        "سداد", "سدد", "قبض", "كشف حساب", "رصيده", "متبقي عليه"
    )

    private val moneyWords = listOf("ريال", "فلس", "دولار", "سعودي", "ر.ي", "ر.س")

    /**
     * يفحص نصًّا حرًّا قبل نشره. الرفض هنا أهون من رقم يظهر في مجموعة واتساب فيها مئات الناس.
     */
    fun scan(text: String): ScanResult {
        if (text.isBlank()) return ScanResult(safe = true, findings = emptyList())
        val findings = mutableListOf<String>()

        digitSpans(text).forEach { span ->
            findings += "رقم يشبه الهاتف في النصّ: ${digitsOf(text, span)} — اكتب العرض بلا أرقام تواصل، " +
                "والسعر في خانه لا في الوصف"
        }

        val normalized = normalizeDigits(text)
        debtWords.forEach { word ->
            if (normalized.contains(word)) findings += "ذكر دَين في عرض عام («$word»): العرض بيان محصول لا كشف حساب"
        }
        val hasMoney = moneyWords.any { normalized.contains(it) }
        if (hasMoney && debtWords.any { normalized.contains(it) }) {
            findings += "مبلغ مالي بجانب ذكر دَين في عرض عام"
        }

        return ScanResult(safe = findings.isEmpty(), findings = findings.distinct())
    }

    /** يخفي مقاطع الأرقام المشبوهة، بدل رفض العرض كله لأجل وصف مكتوب بعجلة. */
    fun sanitize(text: String): String {
        val spans = digitSpans(text).sortedByDescending { it.first }
        var result = text
        spans.forEach { span ->
            result = result.replaceRange(span.first, span.last + 1, "•••••••")
        }
        return result.trim()
    }

    /** الفحص المفروض على كل نصّ سيُنشر: العنوان والوصف والكمية. */
    fun scanAll(vararg texts: String): ScanResult {
        val findings = texts.filter { it.isNotBlank() }.flatMap { scan(it).findings }
        return ScanResult(safe = findings.isEmpty(), findings = findings.distinct())
    }
}
