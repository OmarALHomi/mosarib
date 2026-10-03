package com.baynana.domain.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * تنسيق المبالغ للعرض. مصدر واحد للتنسيق حتى لا تظهر الأرقام بصيغتين في شاشة وملف PDF.
 *
 * قاعدة العرض المعتمدة: التخزين بالفلس، والعرض **بالريال**، ويُحذف الكسر إذا كان صفرًا.
 * فالمستخدم يرى «15,000 ر.ي» لا «15,000.00 ر.ي»، ويرى الكسر فقط عندما يوجد فعلًا («15,000.50»).
 * هذا يجعل الانتقال إلى الوحدة الصغرى غير مرئي لمن لا تظهر له كسور.
 */
object MoneyFormat {

    private val enSymbols = DecimalFormatSymbols(Locale.US)

    fun format(money: Money, withCurrency: Boolean = true): String {
        val text = displayNumber(money)
        return if (withCurrency) "$text ${money.currency.symbol}" else text
    }

    /** المبلغ بالريال بفواصل آلاف، وبلا كسر إن كان صفرًا، وبلا رمز عملة. */
    fun displayNumber(money: Money): String {
        val major = money.minor / money.currency.minorPerUnit
        val fraction = money.minor % money.currency.minorPerUnit
        // القالب يتغيّر بحسب وجود الكسر فعلًا: قالب ثابت بخانتين يُظهر «15,000.00» دائمًا،
        // والقاعدة المعتمدة أن المستخدم يرى ريالات صحيحة إن لم يكن هناك كسر حقيقي.
        val pattern = if (fraction == 0L || money.currency.minorUnits == 0) "#,##0" else "#,##0.00"
        val formatter = DecimalFormat(pattern, enSymbols)
        return if (fraction == 0L) {
            formatter.format(major)
        } else {
            formatter.format(major + fraction.toDouble() / money.currency.minorPerUnit)
        }
    }

    /**
     * الوحدة الصغرى نصًّا: هذا هو الشكل الذي يُخزَّن ويُرسل (انظر [MoneyWire]).
     * لا كسور عشرية ولا فواصل.
     */
    fun toMinorString(money: Money): String = money.minor.toString()

    /**
     * المبلغ بالريال كسلسلة عشرية بلا فواصل آلاف: يلزم للطباعة في PDF (RTL) وللاستيراد.
     * يحذف الكسر الصفري أيضًا.
     */
    fun toPlainMajorString(money: Money): String {
        val major = money.minor / money.currency.minorPerUnit
        val fraction = money.minor % money.currency.minorPerUnit
        if (fraction == 0L) return major.toString()
        return "$major.${fraction.toString().padStart(money.currency.minorUnits, '0')}"
    }

    /** المبالغ بأرقام عربية-هندية مع فواصل عربية، لمن يفضّل ذلك في التقارير. */
    fun formatArabicDigits(money: Money, withCurrency: Boolean = true): String {
        val plain = format(money, withCurrency = false)
        val arabic = plain.map { char ->
            when (char) {
                in '0'..'9' -> ('\u0660' + (char - '0'))
                ',' -> '\u066C'
                '.' -> '\u066B'
                else -> char
            }
        }.joinToString("")
        return if (withCurrency) "$arabic ${money.currency.symbol}" else arabic
    }
}
