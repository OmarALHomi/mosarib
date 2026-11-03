package com.example.core.speech

import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Pure builder for spoken Arabic sentences (TTS) and voice statements.
 * Free of UI dependencies, fully unit testable.
 * Uses speech-only pause forms (no tanween, "ألف" not "ألفاً", split "مِئة").
 */
object SpeechTextBuilder {

    val MONTH_NAMES = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val DAY_ORDINALS = arrayOf(
        "", // 0 unused
        "الأول", "الثاني", "الثالث", "الرابع", "الخامس",
        "السادس", "السابع", "الثامن", "التاسع", "العاشر",
        "الحادي عشر", "الثاني عشر", "الثالث عشر", "الرابع عشر", "الخامس عشر",
        "السادس عشر", "السابع عشر", "الثامن عشر", "التاسع عشر", "العشرون",
        "الحادي والعشرون", "الثاني والعشرون", "الثالث والعشرون", "الرابع والعشرون", "الخامس والعشرون",
        "السادس والعشرون", "السابع والعشرون", "الثامن والعشرون", "التاسع والعشرون", "الثلاثون",
        "الحادي والثلاثون"
    )

    private val ONES_TEENS = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة",
        "ستة", "سبعة", "ثمانية", "تسعة", "عشرة",
        "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val TENS = arrayOf(
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون",
        "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val HUNDREDS = arrayOf(
        "", "مِئَة", "مِئتان", "ثلاث مِئَة", "أربع مِئَة", "خمس مِئَة",
        "ست مِئَة", "سبع مِئَة", "ثمان مِئَة", "تسع مِئَة"
    )

    /**
     * Converts an amount to spoken Arabic words in pause form (no tanween, no case endings).
     * e.g. 14,500 -> "أربعة عشر ألف وخمس مِئة ريال"
     *      750    -> "سبع مِئة وخمسون ريال"
     *      3,500  -> "ثلاثة آلاف وخمس مِئة ريال"
     *      14,000 -> "أربعة عشر ألف ريال"
     *      125,000 -> "مِئة وخمسة وعشرون ألف ريال"
     *      2,000,000 -> "مليونين ريال"
     */
    fun amountToSpeechWords(amount: Double, currency: String = "ريال"): String {
        val absVal = abs(amount)
        val intPart = absVal.toLong()
        val decPart = round((absVal - intPart) * 100).toInt()

        val cleanCurrency = if (currency == "ر.ي") "ريال" else currency

        if (intPart == 0L && decPart == 0) return if (cleanCurrency.isNotBlank()) "صفر $cleanCurrency" else "صفر"

        val groups = mutableListOf<Int>()
        var temp = intPart
        while (temp > 0) {
            groups.add((temp % 1000).toInt())
            temp /= 1000
        }

        val resultParts = mutableListOf<String>()

        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0) continue

            val groupText = when (i) {
                0 -> convertThreeDigits(g)
                1 -> when (g) {
                    1 -> "ألف"
                    2 -> "ألفان"
                    in 3..10 -> "${ONES_TEENS[g]} آلاف"
                    else -> "${convertThreeDigits(g)} ألف"
                }
                2 -> when (g) {
                    1 -> "مليون"
                    2 -> "مليونين"
                    in 3..10 -> "${ONES_TEENS[g]} ملايين"
                    else -> "${convertThreeDigits(g)} مليون"
                }
                3 -> when (g) {
                    1 -> "مليار"
                    2 -> "ملياران"
                    in 3..10 -> "${ONES_TEENS[g]} مليارات"
                    else -> "${convertThreeDigits(g)} مليار"
                }
                else -> "${convertThreeDigits(g)} وحدة"
            }
            if (groupText.isNotBlank()) {
                resultParts.add(groupText)
            }
        }

        val intText = resultParts.joinToString(" و")
        val prefix = if (amount < -0.001) "سالب " else ""

        return if (decPart > 0) {
            val decText = convertThreeDigits(decPart)
            if (cleanCurrency.isNotBlank()) "$prefix$intText $cleanCurrency و$decText هللة" else "$prefix$intText و$decText هللة"
        } else {
            if (cleanCurrency.isNotBlank()) "$prefix$intText $cleanCurrency" else "$prefix$intText"
        }.trim()
    }

    /**
     * Approximate wording for spoken numbers in voice statement lines.
     * When amount is round, omits "تقريباً" to keep lines short.
     */
    fun amountToSpeechWordsApprox(amount: Double, includeCurrency: Boolean = true): String {
        val absVal = abs(amount)
        val rounded = round(absVal / 100.0) * 100.0
        val curr = if (includeCurrency) "ريال" else ""
        val baseWords = amountToSpeechWords(rounded, curr).trim()
        return if (abs(absVal - rounded) < 0.001) {
            baseWords
        } else {
            "$baseWords تقريباً"
        }
    }

    private fun convertThreeDigits(n: Int): String {
        if (n <= 0) return ""
        val h = n / 100
        val rem = n % 100
        val parts = mutableListOf<String>()

        if (h in 1..9) {
            parts.add(HUNDREDS[h])
        }

        if (rem in 1..19) {
            parts.add(ONES_TEENS[rem])
        } else if (rem >= 20) {
            val u = rem % 10
            val t = rem / 10
            if (u > 0) {
                parts.add("${ONES_TEENS[u]} و${TENS[t]}")
            } else {
                parts.add(TENS[t])
            }
        }

        return parts.joinToString(" و")
    }

    /**
     * Converts a Date to formal Arabic spoken date: e.g. "الثاني من نوفمبر"
     */
    fun formatDayMonth(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) // 0-based

        val dayStr = if (day in 1..31) DAY_ORDINALS[day] else "$day"
        val monthStr = if (month in 0..11) MONTH_NAMES[month] else ""
        return "$dayStr من $monthStr"
    }

    fun formatDayMonth(epochMillis: Long): String = formatDayMonth(Date(epochMillis))

    /**
     * Short day and month for voice statement lines: e.g. "2 نوفمبر"
     */
    fun formatDayMonthShort(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        val monthStr = if (month in 0..11) MONTH_NAMES[month] else ""
        return "$day $monthStr"
    }

    /**
     * Converts duration in minutes into pure spoken Arabic words:
     */
    fun formatDurationWords(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "صفر دقيقة"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val hoursStr = when (hours) {
            0 -> ""
            1 -> "ساعة واحدة"
            2 -> "ساعتان"
            in 3..10 -> "${digitToWordsMasculine(hours)} ساعات"
            else -> "${numberToWordsGeneral(hours)} ساعة"
        }

        val minutesStr = when (minutes) {
            0 -> ""
            1 -> "دقيقة واحدة"
            2 -> "دقيقتان"
            in 3..10 -> "${digitToWordsMasculine(minutes)} دقائق"
            else -> "${numberToWordsFeminine(minutes)} دقيقة"
        }

        return when {
            hours > 0 && minutes > 0 -> "$hoursStr و$minutesStr"
            hours > 0 -> hoursStr
            else -> minutesStr
        }
    }

    /**
     * Concise duration for voice statement lines: e.g. "ساعة", "ساعتان", "ساعتان ونصف"
     */
    fun formatDurationWordsShort(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "صفر دقيقة"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 1 && minutes == 0 -> "ساعة"
            hours == 1 && minutes == 30 -> "ساعة ونصف"
            hours == 2 && minutes == 0 -> "ساعتان"
            hours == 2 && minutes == 30 -> "ساعتان ونصف"
            hours in 3..10 && minutes == 0 -> "${digitToWordsMasculine(hours)} ساعات"
            hours > 0 && minutes == 0 -> "$hours ساعة"
            hours > 0 && minutes > 0 -> "${digitToWordsMasculine(hours)} ساعات و$minutes دقيقة"
            else -> "$minutes دقيقة"
        }
    }

    private fun digitToWordsMasculine(n: Int): String = when (n) {
        3 -> "ثلاث"
        4 -> "أربع"
        5 -> "خمس"
        6 -> "ست"
        7 -> "سبع"
        8 -> "ثماني"
        9 -> "تسع"
        10 -> "عشر"
        else -> "$n"
    }

    private fun numberToWordsFeminine(n: Int): String {
        if (n in 1..10) return when (n) {
            1 -> "واحدة"
            2 -> "اثنتان"
            3 -> "ثلاث"
            4 -> "أربع"
            5 -> "خمس"
            6 -> "ست"
            7 -> "سبع"
            8 -> "ثمان"
            9 -> "تسع"
            10 -> "عشر"
            else -> "$n"
        }
        if (n in 11..19) return when (n) {
            11 -> "إحدى عشرة"
            12 -> "اثنتا عشرة"
            13 -> "ثلاث عشرة"
            14 -> "أربع عشرة"
            15 -> "خمس عشرة"
            16 -> "ست عشرة"
            17 -> "سبع عشرة"
            18 -> "ثماني عشرة"
            19 -> "تسع عشرة"
            else -> "$n"
        }
        val tens = n / 10
        val units = n % 10
        val tensStr = when (tens) {
            2 -> "عشرون"
            3 -> "ثلاثون"
            4 -> "أربعون"
            5 -> "خمسون"
            6 -> "ستون"
            7 -> "سبعون"
            8 -> "ثمانون"
            9 -> "تسعون"
            else -> ""
        }
        if (units == 0) return tensStr
        val unitsStr = when (units) {
            1 -> "إحدى"
            2 -> "اثنتان"
            3 -> "ثلاث"
            4 -> "أربع"
            5 -> "خمس"
            6 -> "ست"
            7 -> "سبع"
            8 -> "ثمان"
            9 -> "تسع"
            else -> "$units"
        }
        return "$unitsStr و$tensStr"
    }

    private fun numberToWordsGeneral(n: Int): String {
        val tens = n / 10
        val units = n % 10
        val tensStr = when (tens) {
            2 -> "عشرون"
            3 -> "ثلاثون"
            4 -> "أربعون"
            5 -> "خمسون"
            6 -> "ستون"
            7 -> "سبعون"
            8 -> "ثمانون"
            9 -> "تسعون"
            else -> ""
        }
        if (units == 0) return tensStr
        val unitsStr = when (units) {
            1 -> "إحدى"
            2 -> "اثنتان"
            3 -> "ثلاث"
            4 -> "أربع"
            5 -> "خمس"
            6 -> "ست"
            7 -> "سبع"
            8 -> "ثمان"
            9 -> "تسع"
            else -> "$units"
        }
        return "$unitsStr و$tensStr"
    }

    /**
     * B3. Session sentence:
     */
    fun buildSessionSentence(
        session: WaterSession,
        customerName: String,
        currency: String = "ريال"
    ): String {
        val name = customerName.ifBlank { "عميل غير محدد" }
        val durationWords = formatDurationWords(session.durationMinutes)
        val amountWords = amountToSpeechWords(session.totalAmount, currency)
        val rate = when {
            session.pricePerHour > 0.0 -> session.pricePerHour
            session.durationMinutes > 0 -> (session.totalAmount * 60.0) / session.durationMinutes
            else -> 0.0
        }
        val rateWords = if (rate > 0.0) amountToSpeechWords(rate, "") else ""
        val ratePart = if (rateWords.isNotBlank()) " من $rateWords" else ""

        return if (session.remainingDebt <= 0.001) {
            "سقي لـ$name، $durationWords$ratePart، المبلغ $amountWords، مسدد بالكامل."
        } else {
            val paidWords = amountToSpeechWords(session.amountPaid, currency)
            val debtWords = amountToSpeechWords(session.remainingDebt, currency)
            "سقي لـ$name، $durationWords$ratePart، المبلغ $amountWords، المدفوع $paidWords، الباقي $debtWords."
        }
    }

    /**
     * B3. Receipt sentence:
     */
    fun buildReceiptSentence(
        customerName: String,
        amount: Double,
        date: Long,
        currency: String = "ريال"
    ): String = buildReceiptSentence(customerName, amount, Date(date), currency)

    fun buildReceiptSentence(
        customerName: String,
        amount: Double,
        date: Date,
        currency: String = "ريال"
    ): String {
        val name = customerName.ifBlank { "عميل غير محدد" }
        val words = amountToSpeechWords(amount, currency)
        val dateWords = formatDayMonth(date)
        return "سند قبض من $name، $words، بتاريخ $dateWords."
    }

    /**
     * B3. Expense sentence:
     */
    fun buildExpenseSentence(
        nameOrCategory: String,
        amount: Double,
        date: Long,
        currency: String = "ريال"
    ): String = buildExpenseSentence(nameOrCategory, amount, Date(date), currency)

    fun buildExpenseSentence(
        nameOrCategory: String,
        amount: Double,
        date: Date,
        currency: String = "ريال"
    ): String {
        val label = nameOrCategory.ifBlank { "مصروف عام" }
        val words = amountToSpeechWords(amount, currency)
        val dateWords = formatDayMonth(date)
        return "سند صرف لـ$label، $words، بتاريخ $dateWords."
    }

    /**
     * B3. Discount sentence:
     */
    fun buildDiscountSentence(
        customerName: String,
        amount: Double,
        currency: String = "ريال"
    ): String {
        val name = customerName.ifBlank { "عميل غير محدد" }
        val words = amountToSpeechWords(amount, currency)
        return "خصم لـ$name، $words."
    }

    fun buildDiscountSentence(
        customerName: String,
        amount: Double,
        date: Date? = null,
        currency: String = "ريال"
    ): String = buildDiscountSentence(customerName, amount, currency)

    fun buildDiscountSentence(
        customerName: String,
        amount: Double,
        date: Long? = null,
        currency: String = "ريال"
    ): String = buildDiscountSentence(customerName, amount, currency)

    /**
     * B3. Purchase sentence:
     */
    fun buildPurchaseSentence(
        ownerName: String,
        durationMinutes: Int,
        amount: Double,
        currency: String = "ريال",
        ratePerHour: Double = 0.0
    ): String {
        val name = ownerName.ifBlank { "صاحب البئر" }
        val durationWords = formatDurationWords(durationMinutes)
        val words = amountToSpeechWords(amount, currency)
        val rate = when {
            ratePerHour > 0.0 -> ratePerHour
            durationMinutes > 0 -> (amount * 60.0) / durationMinutes
            else -> 0.0
        }
        val rateWords = if (rate > 0.0) amountToSpeechWords(rate, "") else ""
        val ratePart = if (rateWords.isNotBlank()) " من $rateWords" else ""
        return "شراء ساعات من $name، $durationWords$ratePart، $words."
    }

    /**
     * B3. Balance sentence:
     */
    fun buildBalanceSentence(
        customerName: String,
        balance: Double,
        currency: String = "ريال",
        isWellOwner: Boolean = false,
        payableBalance: Double = 0.0,
        receivableBalance: Double = 0.0
    ): String {
        val name = customerName.ifBlank { "العميل" }
        if (isWellOwner) {
            return when {
                payableBalance > 0.001 -> {
                    val words = amountToSpeechWords(payableBalance, currency)
                    // Same wording as the card: the owner is owed ("له"), not "عليك"
                    "رصيد $name: له $words."
                }
                receivableBalance > 0.001 -> {
                    val words = amountToSpeechWords(receivableBalance, currency)
                    "رصيد $name: عليه $words."
                }
                else -> "رصيد $name: الحساب مسدد."
            }
        }
        return when {
            balance > 0.001 -> {
                val words = amountToSpeechWords(balance, currency)
                "رصيد $name: عليه $words."
            }
            balance < -0.001 -> {
                val words = amountToSpeechWords(abs(balance), currency)
                "رصيد $name: له $words."
            }
            else -> "رصيد $name: الحساب مسدد."
        }
    }

    // --- Compact lines for Customer Voice Statement (Item 2) ---

    fun buildStatementSessionLine(session: WaterSession, dateMillis: Long): String {
        val dayStr = formatDayMonthShort(dateMillis)
        val dur = formatDurationWordsShort(session.durationMinutes)
        val rate = when {
            session.pricePerHour > 0.0 -> session.pricePerHour
            session.durationMinutes > 0 -> (session.totalAmount * 60.0) / session.durationMinutes
            else -> 0.0
        }
        val rateWords = if (rate > 0.0) amountToSpeechWordsApprox(rate, includeCurrency = false) else ""
        val ratePart = if (rateWords.isNotBlank()) " من $rateWords" else ""
        return if (session.remainingDebt <= 0.001) {
            "$dayStr: سقي $dur$ratePart، مسدد بالكامل."
        } else {
            val debt = amountToSpeechWordsApprox(session.remainingDebt, includeCurrency = false)
            "$dayStr: سقي $dur$ratePart، الباقي $debt."
        }
    }

    fun buildStatementReceiptLine(amount: Double, dateMillis: Long): String {
        val dayStr = formatDayMonthShort(dateMillis)
        val amt = amountToSpeechWordsApprox(amount, includeCurrency = false)
        return "$dayStr: قبض $amt."
    }

    fun buildStatementExpenseLine(amount: Double, dateMillis: Long): String {
        val dayStr = formatDayMonthShort(dateMillis)
        val amt = amountToSpeechWordsApprox(amount, includeCurrency = false)
        return "$dayStr: صرف $amt."
    }

    fun buildStatementDiscountLine(amount: Double, dateMillis: Long): String {
        val dayStr = formatDayMonthShort(dateMillis)
        val amt = amountToSpeechWordsApprox(amount, includeCurrency = false)
        return "$dayStr: خصم $amt."
    }

    fun buildStatementPurchaseLine(durationMinutes: Int, amount: Double, dateMillis: Long): String {
        val dayStr = formatDayMonthShort(dateMillis)
        val dur = formatDurationWordsShort(durationMinutes)
        val amt = amountToSpeechWordsApprox(amount, includeCurrency = false)
        return "$dayStr: شراء ساعات $dur، $amt."
    }

    /**
     * C2. Customer voice statement (Item 2: target under 90s, <= 400KB, <= 110 words):
     * - "كشف حساب {name} من {distributor} بتاريخ {day month}."
     * - The current balance in exact words.
     * - Last 10 operations, newest first, one compact sentence each without repeating customer name.
     * - "انتهى الكشف."
     */
    fun buildCustomerVoiceStatement(
        customerName: String,
        distributorName: String,
        date: Date,
        balanceSentence: String,
        operationSentences: List<String>
    ): String {
        val name = customerName.ifBlank { "العميل" }
        val dist = distributorName.ifBlank { "المسرّب" }
        val todayWords = formatDayMonthShort(date.time)

        val header = "كشف حساب $name من $dist بتاريخ $todayWords."
        val recentOps = operationSentences.take(10)

        val opsPart = if (recentOps.isEmpty()) {
            "لا توجد حركات مسجلة."
        } else {
            recentOps.joinToString(" ")
        }

        return "$header $balanceSentence $opsPart انتهى الكشف."
    }

    /**
     * Builds the written text caption shared with the audio statement file (e.g. for WhatsApp).
     * Follows the standardized format with exact written words and shared footer.
     */
    fun buildVoiceStatementCaption(
        customerName: String,
        balance: Double,
        isWellOwner: Boolean = false,
        payableBalance: Double = 0.0,
        receivableBalance: Double = 0.0,
        operationCount: Int,
        date: Date = Date(),
        distributorName: String = "",
        distributorPhone: String = "",
        currencySymbol: String = "ر.ي"
    ): String {
        val (direction, absAmount) = if (isWellOwner) {
            val net = payableBalance - receivableBalance
            when {
                net > 0.001 -> "له" to net
                net < -0.001 -> "عليه" to -net
                else -> "مسدد" to 0.0
            }
        } else {
            when {
                balance > 0.001 -> "عليه" to balance
                balance < -0.001 -> "له" to -balance
                else -> "مسدد" to 0.0
            }
        }

        val balanceLine = if (direction == "مسدد") {
            "الرصيد: مسدد"
        } else {
            val formattedAmt = Formatters.formatCurrency(absAmount, currencySymbol)
            val currencyForWords = if (currencySymbol == "ر.ي" || currencySymbol == "ريال" || currencySymbol.isBlank()) "ريالاً" else currencySymbol
            val rawWords = Formatters.amountToArabicWords(absAmount, currencyForWords)
            val cleanWords = rawWords
                .removePrefix("سالب فقط ")
                .removePrefix("فقط ")
                .removeSuffix(" لا غير")
                .removeSuffix(" لاغير")
                .trim()
            "الرصيد: $direction $formattedAmt ($cleanWords)"
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val dateStr = sdf.format(date)
        val opsLine = "آخر $operationCount عمليات · بتاريخ $dateStr"

        val distClean = distributorName.ifBlank { "المسرّب" }
        val phoneSuffix = if (distributorPhone.isNotBlank()) " $distributorPhone" else ""
        val distLine = "من: $distClean$phoneSuffix"

        val body = """
🔊 كشف حساب صوتي: $customerName
$balanceLine
$opsLine
$distLine
        """.trimIndent()

        return FileSharingHelper.attachMessageFooter(body)
    }
}
