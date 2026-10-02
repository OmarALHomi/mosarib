package com.baynana.domain.money

/**
 * عملات «بيننا» الأربع. القرار المعتمد: لا تحويل تلقائي بينها ولا مقاصة، ورصيد كل عملة مستقل.
 *
 * [minorUnits] عدد الخانات العشرية المعروضة، و[minorPerUnit] عدد الوحدات الصغرى في الوحدة.
 * الريال اليمني (قديمًا وجديدًا) يُتعامل به بلا كسور عمليًا، فيُرفض أي كسر عند الإدخال
 * بدل تدويره صامتًا؛ أما الريال السعودي والدولار فكسراهما معتمدان (هللة/سنت).
 */
enum class Currency(
    val code: String,
    val arabicName: String,
    val symbol: String,
    val minorUnits: Int
) {
    YER_OLD("YER_OLD", "ريال يمني قديم", "ر.ي قديم", 0),
    YER_NEW("YER_NEW", "ريال يمني جديد", "ر.ي", 0),
    SAR("SAR", "ريال سعودي", "ر.س", 2),
    USD("USD", "دولار أمريكي", "$", 2);

    /** عدد الوحدات الصغرى في وحدة واحدة: 1 للريال اليمني، 100 للسعودي والدولار. */
    val minorPerUnit: Long = if (minorUnits == 0) 1L else pow10(minorUnits.toLong())

    companion object {
        private fun pow10(exponent: Long): Long {
            var result = 1L
            repeat(exponent.toInt()) { result *= 10L }
            return result
        }

        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code == code }
    }
}

/** خطأ حسابي في المال. الرسالة عربية لأنها تُعرض للمستخدم في مسار الإدخال. */
class MoneyException(message: String) : IllegalArgumentException(message)
