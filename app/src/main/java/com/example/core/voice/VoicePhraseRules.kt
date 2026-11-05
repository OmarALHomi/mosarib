package com.example.core.voice

import java.util.Calendar

/**
 * Data-driven vocabulary and phrase rules for spoken command parsing.
 * All word lists, markers, joiners, trailing words and expense categories live here.
 * Adding a new phrase or keyword is a one-line change.
 */
object VoicePhraseRules {

    // 1. Type words identifying the command intent
    val TYPE_WORDS: Map<VoiceCommandType, List<String>> = mapOf(
        VoiceCommandType.SESSION to listOf(
            "سقي", "سقيه", "سقايه", "سقيت", "سقينا", "ري", "جلسه", "دوره"
        ),
        VoiceCommandType.RECEIPT to listOf(
            "قبض", "قبضت", "قبضنا", "استلمت", "استلمنا", "سدد", "سداد",
            "وصل", "واصل", "وصلني", "سجل", "قيد", "اضف لحساب", "ايداع",
            "دفع لي", "جاب", "جابه"
        ),
        VoiceCommandType.EXPENSE to listOf(
            "صرف", "صرفت", "صرفنا", "اعطيت", "عطيت", "سلفه", "سلفت",
            "دفعت ل", "سلمت ل"
        ),
        VoiceCommandType.PURCHASE to listOf(
            "شراء", "اشتريت", "شريت", "اشترينا", "من صاحب البئر",
            "صاحب البئر", "صاحب بئر", "من البئر", "بئر"
        ),
        VoiceCommandType.DISCOUNT to listOf(
            "خصم", "سامحت", "سامحته", "تنازل", "اسقط"
        )
    )

    // 2. Recognized expense categories
    val EXPENSE_CATEGORIES = listOf(
        "ديزل", "شراء ديزل", "صيانة", "صيانه", "اصلاح", "إصلاح",
        "زيت", "تغيير زيت", "كهرباء", "فاتورة كهرباء", "فاتوره كهرباء",
        "عمال", "أجور عمال", "اجور عمال", "أجرة", "اجرة",
        "قطع غيار", "مواصير", "مواسير", "ليات",
        "فلتر", "فلاتر", "نقل", "توصيل", "نقل وتوصيل", "ضيافة", "ضيافه",
        "بطارية", "بطاريه", "طاقة شمسية", "طاقه شمسيه"
    )

    // 3. Rate markers preceding the price per hour in sessions
    val RATE_MARKERS = listOf(
        "سعر الساعه", "سعر الساعة", "الساعه بـ", "الساعة بـ", "الساعه ب", "الساعة ب",
        "بسعر", "من"
    )

    // 4. Paid markers preceding an amount paid
    val PAID_MARKERS = listOf(
        "دفع منها", "وصل منه", "نقد منها", "اعطانا", "سلمنا", "سلمها", "سلمه",
        "مسلم", "وسلم", "دفعه", "دفعة", "ودفع", "ودفعه", "مدفوع", "واصل",
        "وادي", "اعطى", "عطى", "سلمت", "دفعت", "دفعنا", "دافع", "عربون",
        "مقدم", "كاش", "نقد", "أدى", "ادى", "سلم", "دفع", "وصل", "وسلمه"
    )

    // 5. Explicit markers meaning paid in full
    val PAID_FULL_WORDS = listOf(
        "كامل المبلغ", "دفع الكل", "ما عليه شي", "مسلم كامل", "مدفوع كامل",
        "كامل", "كله", "خالص", "مخلص", "نقدا", "نقد", "كاش"
    )

    // 6. Explicit debt markers (paid = empty / 0, no question asked)
    val EXPLICIT_DEBT_WORDS = listOf(
        "على الحساب", "قيده عليه", "بدون تسليم", "ما سلم", "ما دفع", "ولا ريال",
        "دينه", "دين", "عليه", "آجل", "اجل"
    )

    // 7. Direction words before customer names
    val CUSTOMER_JOINERS = listOf(
        "على حساب", "لحساب", "للأخ", "للاخ", "حساب", "حق", "عند", "الى", "إلى",
        "مع", "من", "لـ", "ل"
    )

    // 8. Honorific prefixes before customer names (أبو is preserved in the name)
    val HONORIFICS = listOf(
        "الأخ", "الاخ", "العم", "الحاج", "الشيخ"
    )

    // 9. Words that join name parts together
    val NAME_JOINERS = listOf(
        "بن", "ابن", "بنت"
    )

    // 10. Trailing filler words to drop from end of phrases
    val TRAILING_WORDS = listOf(
        "عندي", "اليوم", "الحين", "الآن", "الان", "تمام", "يا", "بس", "لي"
    )

    // 11. Words starting a general expense description / notes
    val DESCRIPTION_STARTERS = listOf(
        "حق", "ثمن", "قيمه", "قيمة", "اجره", "اجرة", "مقابل", "عشان", "بدل", "فاتوره", "فاتورة",
        "شراء", "شريت"
    )

    // 12. Joiners between start time and end time
    val TIME_RANGE_JOINERS = listOf(
        "الى", "إلى", "لين", "حتى", "لعند", "لما", "لا"
    )

    // 13. Day-part keywords indicating AM
    val DAY_PARTS_AM = listOf(
        "بالصبح", "الصبح", "الصباح", "صباح", "صباحا", "صباحاً", "الفجر", "بالفجر", "فجرا", "فجراً",
        "الضحى", "الضحي", "ضحى", "اخر الليل", "أخر الليل"
    )

    // 14. Day-part keywords indicating PM
    val DAY_PARTS_PM = listOf(
        "بالليل", "الليل", "في الليل", "ليلا", "ليلاً", "مساء", "مساءً", "مساءا",
        "الظهريه", "الظهرية", "الظهر", "بالظهر", "ظهرا", "ظهراً",
        "بعد العصر", "العصر", "بالعصر", "عصرا", "عصراً",
        "المغرب", "بالمغرب", "مغربا", "العشاء", "بالعشاء", "عشاء"
    )

    // 15. Relative day names
    val DAY_WORDS = listOf(
        "اول من امس", "اول امس", "قبل امس", "البارحه", "البارح", "امس", "اليوم"
    )

    // 16. Weekday mapping to Calendar constants
    val WEEKDAYS = mapOf(
        "السبت" to Calendar.SATURDAY,
        "الاحد" to Calendar.SUNDAY,
        "الإحد" to Calendar.SUNDAY,
        "الاثنين" to Calendar.MONDAY,
        "الإثنين" to Calendar.MONDAY,
        "الثلاثاء" to Calendar.TUESDAY,
        "الاربعاء" to Calendar.WEDNESDAY,
        "الإربعاء" to Calendar.WEDNESDAY,
        "الخميس" to Calendar.THURSDAY,
        "الجمعه" to Calendar.FRIDAY,
        "الجمعة" to Calendar.FRIDAY
    )

    // 17. In-phrase correction triggers
    val CORRECTION_TRIGGERS = listOf(
        "لا قصدي", "لا لا", "قصدي", "اقصد", "أقصد", "عفوا", "عفواً", "بل", "اقول", "أقول"
    )

    /**
     * Checks if [word] fuzzy matches [target] (4+ letters and Levenshtein edit distance <= 1).
     * Never fuzzy matches short words (< 4 letters) or names.
     */
    fun matchesFuzzy(word: String, target: String): Boolean {
        if (word == target) return true
        if (word.length < 4 || target.length < 4) return false
        if (Math.abs(word.length - target.length) > 1) return false
        return levenshteinDistance(word, target) <= 1
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
