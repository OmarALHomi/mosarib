package com.example.core.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val symbols = java.text.DecimalFormatSymbols(Locale.US)
    private val numberFormat = DecimalFormat("#,##0.##", symbols)
    private val integerFormat = DecimalFormat("#,##0", symbols)

    fun formatNumber(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            integerFormat.format(amount)
        } else {
            numberFormat.format(amount)
        }
    }

    fun formatCurrency(amount: Double, currency: String = "ر.ي"): String {
        return "${formatNumber(amount)} $currency"
    }

    /**
     * Formats user input with thousands separators (commas) as they type
     * e.g. "12000" -> "12,000", "12000.5" -> "12,000.5"
     */
    fun formatAmountInput(raw: String): String {
        val clean = raw.replace(",", "").replace("،", "").trim()
        if (clean.isEmpty()) return ""
        if (clean == ".") return "0."
        val parts = clean.split(".")
        val integerPart = parts[0].toLongOrNull() ?: return clean
        val formattedInt = integerFormat.format(integerPart)
        return if (parts.size > 1) {
            "$formattedInt.${parts[1]}"
        } else {
            formattedInt
        }
    }

    /**
     * Cleans formatted input with commas back to Double
     */
    fun parseAmountInput(text: String): Double {
        val clean = text.replace(",", "").replace("،", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private val ONES = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة",
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val TENS = arrayOf(
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val HUNDREDS = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    private fun convertThreeDigits(number: Int): String {
        var n = number
        val parts = mutableListOf<String>()

        val h = n / 100
        if (h > 0) {
            parts.add(HUNDREDS[h])
            n %= 100
        }

        if (n in 1..19) {
            parts.add(ONES[n])
        } else if (n >= 20) {
            val u = n % 10
            val t = n / 10
            if (u > 0) {
                parts.add("${ONES[u]} و${TENS[t]}")
            } else {
                parts.add(TENS[t])
            }
        }

        return parts.joinToString(" و")
    }

    /**
     * Converts a numeric amount to formal spoken Arabic words (Tafqeet)
     * e.g. 5000 -> "فقط خمسة آلاف ريال لا غير"
     */
    fun amountToArabicWords(amount: Double, currency: String = "ريال"): String {
        if (amount == 0.0) return "صفر $currency"
        val isNegative = amount < 0
        val positiveAmount = Math.abs(amount)
        val intPart = positiveAmount.toLong()
        val decPart = Math.round((positiveAmount - intPart) * 100).toInt()

        if (intPart == 0L && decPart == 0) return "صفر $currency"

        val groups = mutableListOf<Int>()
        var temp = intPart
        while (temp > 0) {
            groups.add((temp % 1000).toInt())
            temp /= 1000
        }

        val groupNames = arrayOf(
            "", // units
            "ألف", // thousands
            "مليون", // millions
            "مليار" // billions
        )

        val resultParts = mutableListOf<String>()

        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0) continue

            val text3 = convertThreeDigits(g)
            val gText = when (i) {
                0 -> text3
                1 -> when (g) {
                    1 -> "ألف"
                    2 -> "ألفان"
                    in 3..10 -> "$text3 آلاف"
                    in 11..99 -> "$text3 ألفاً"
                    else -> "$text3 ألف"
                }
                2 -> when (g) {
                    1 -> "مليون"
                    2 -> "مليونان"
                    in 3..10 -> "$text3 ملايين"
                    in 11..99 -> "$text3 مليونا"
                    else -> "$text3 مليون"
                }
                3 -> when (g) {
                    1 -> "مليار"
                    2 -> "ملياران"
                    in 3..10 -> "$text3 مليارات"
                    in 11..99 -> "$text3 ملياراً"
                    else -> "$text3 مليار"
                }
                else -> "$text3 ${groupNames.getOrElse(i) { "" }}"
            }
            resultParts.add(gText)
        }

        val intText = resultParts.joinToString(" و")
        val cleanCurrency = if (currency.contains("ر") || currency.contains("ريال")) currency else "$currency ريال"
        val prefix = if (isNegative) "سالب فقط " else "فقط "

        return if (decPart > 0) {
            val decText = convertThreeDigits(decPart)
            "$prefix$intText $cleanCurrency و$decText فلس لا غير"
        } else {
            "$prefix$intText $cleanCurrency لا غير"
        }
    }

    /**
     * Formats duration into a natural Arabic string
     * e.g. "3 ساعات و 45 دقيقة", "ساعة واحدة", "45 دقيقة"
     */
    fun formatDurationArabic(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0 دقيقة"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val hoursStr = when (hours) {
            0 -> ""
            1 -> "ساعة واحدة"
            2 -> "ساعتان"
            in 3..10 -> "$hours ساعات"
            else -> "$hours ساعة"
        }

        val minutesStr = when (minutes) {
            0 -> ""
            1 -> "دقيقة واحدة"
            2 -> "دقيقتان"
            in 3..10 -> "$minutes دقائق"
            else -> "$minutes دقيقة"
        }

        return when {
            hours > 0 && minutes > 0 -> "$hoursStr و $minutesStr"
            hours > 0 -> hoursStr
            else -> minutesStr
        }
    }

    fun formatDurationShort(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d س", hours, minutes)
    }

    fun formatDurationClock(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateFull(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Calculates cost from duration in minutes and price per hour
     */
    fun calculateWaterCost(durationMinutes: Int, pricePerHour: Double): Double {
        return (durationMinutes.toDouble() / 60.0) * pricePerHour
    }
}
