package com.baynana.domain.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * تنسيق المبالغ للعرض. مصدر واحد للتنسيق حتى لا تظهر الأرقام بصيغتين في شاشة وملف PDF.
 * الأرقام لاتينية بفاصلة آلاف (الأوضح للجميع على الجوال)، ورمز العملة عربي.
 */
object MoneyFormat {

    private val enSymbols = DecimalFormatSymbols(Locale.US)

    fun format(money: Money, withCurrency: Boolean = true): String {
        val number = numberFormatter(money.currency.minorUnits).format(money.toPlainString().toBigDecimalOrNull())
        return if (withCurrency) "$number ${money.currency.symbol}" else number
    }

    /** المبلغ بأرقام لاتينية بلا فواصل: مناسب للمشاركة والاستيراد والحساب. */
    fun toPlainString(money: Money): String {
        if (money.currency.minorUnits == 0) return money.minor.toString()
        val major = money.minor / money.currency.minorPerUnit
        val fraction = money.minor % money.currency.minorPerUnit
        return "$major.${fraction.toString().padStart(money.currency.minorUnits, '0')}"
    }

    /** المبلغ بأرقام عربية-هندية مع فواصل آلاف عربية، لمن يفضّل ذلك في التقارير. */
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

    private fun numberFormatter(minorUnits: Int): DecimalFormat {
        val pattern = if (minorUnits == 0) "#,##0" else "#,##0." + "0".repeat(minorUnits)
        return DecimalFormat(pattern, enSymbols)
    }
}
