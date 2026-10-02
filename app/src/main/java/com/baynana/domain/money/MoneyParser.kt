package com.baynana.domain.money

/**
 * قراءة المبالغ كما يكتبها الناس في اليمن: أرقام عربية أو لاتينية، وفواصل آلاف عربية أو
 * لاتينية، ومسافات. لا تخمين: ما لا يُفهم يُرفض برسالة صريحة، ولا يُقرَّب صامتًا.
 */
object MoneyParser {

    private const val ARABIC_INDIC_ZERO = '\u0660' // ٠
    private const val ARABIC_INDIC_NINE = '\u0669' // ٩
    private const val EXTENDED_ARABIC_ZERO = '\u06F0' // ۰
    private const val EXTENDED_ARABIC_NINE = '\u06F9' // ۹
    private const val ARABIC_DECIMAL = '\u066B' // ٫
    private const val ARABIC_THOUSANDS = '\u066C' // ٬

    fun parse(input: String, currency: Currency): MoneyParse {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return MoneyParse.Error("اكتب المبلغ")

        val normalized = StringBuilder(trimmed.length)
        for (char in trimmed) {
            when {
                char in ARABIC_INDIC_ZERO..ARABIC_INDIC_NINE ->
                    normalized.append('0' + (char - ARABIC_INDIC_ZERO))
                char in EXTENDED_ARABIC_ZERO..EXTENDED_ARABIC_NINE ->
                    normalized.append('0' + (char - EXTENDED_ARABIC_ZERO))
                char == ARABIC_DECIMAL -> normalized.append('.')
                char == '.' -> normalized.append('.')
                char == ARABIC_THOUSANDS || char == ',' || char == ' ' || char == '\u00A0' || char == '\u200F' || char == '\u200E' ->
                    normalized.append('_') // فاصل يُهمل
                else -> normalized.append(char)
            }
        }

        val cleaned = normalized.toString().replace("_", "")
        if (cleaned.isEmpty()) return MoneyParse.Error("اكتب المبلغ")

        if (cleaned.startsWith("-") || cleaned.startsWith("−")) {
            return MoneyParse.Error("لا يُقبل مبلغ سالب")
        }
        if (cleaned.count { it == '.' } > 1) return MoneyParse.Error("صيغة المبلغ غير صحيحة")
        if (cleaned.any { it != '.' && !it.isDigit() }) return MoneyParse.Error("صيغة المبلغ غير صحيحة")

        val parts = cleaned.split('.')
        val majorText = parts[0].ifEmpty { "0" }
        val fractionText = parts.getOrNull(1) ?: ""

        if (fractionText.isNotEmpty() && currency.minorUnits == 0) {
            return MoneyParse.Error("${currency.arabicName} لا يقبل كسورًا؛ اكتب مبلغًا صحيحًا")
        }
        if (fractionText.length > currency.minorUnits) {
            return MoneyParse.Error("أكثر من ${currency.minorUnits} خانة عشرية مسموح به في ${currency.arabicName}")
        }

        val major = majorText.toLongOrNull() ?: return MoneyParse.Error("المبلغ أكبر من الحد المسموح")
        val fraction = if (fractionText.isEmpty()) 0L else {
            val padded = fractionText.padEnd(currency.minorUnits, '0')
            padded.toLongOrNull() ?: return MoneyParse.Error("صيغة المبلغ غير صحيحة")
        }

        return try {
            val minor = Math.addExact(Math.multiplyExact(major, currency.minorPerUnit), fraction)
            MoneyParse.Ok(Money.ofMinor(minor, currency))
        } catch (_: ArithmeticException) {
            MoneyParse.Error("المبلغ أكبر من الحد المسموح")
        } catch (error: MoneyException) {
            MoneyParse.Error(error.message ?: "مبلغ غير صالح")
        }
    }
}
