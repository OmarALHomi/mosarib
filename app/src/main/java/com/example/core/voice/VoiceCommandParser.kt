package com.example.core.voice

/**
 * Pure parser converting spoken Arabic command phrases into structured [VoiceCommand].
 * Follows the hard rules:
 * - Never saves by voice (only fills forms)
 * - The user never says the total (ignores words like "المجموع" / "الإجمالي")
 * - Distinguishes SESSION, RECEIPT, EXPENSE, PURCHASE, DISCOUNT
 */
object VoiceCommandParser {

    private val EXPENSE_CATEGORIES = listOf(
        "ديزل", "شراء ديزل", "صيانة", "اصلاح", "إصلاح",
        "زيت", "تغيير زيت", "كهرباء", "فاتورة كهرباء",
        "عمال", "أجور عمال", "اجور عمال", "أجرة", "اجرة",
        "قطع غيار", "مواصير", "مواسير", "ليات",
        "فلتر", "فلاتر", "نقل", "توصيل", "ضيافة", "ضيافه",
        "بطارية", "بطاريه", "طاقة شمسية", "طاقه شمسيه"
    )

    private val PAID_TRIGGER_REGEX = Regex(
        "(?:\\b|(?<=\\s)|(?<=^))(?:و\\s*)?(?:دفعه|دفعنا|دفعت|دافع|مدفوع|سلمه|سلمت|دفع|سلم)(?:\\s*[:\\-]?\\s+|\\s+|$)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses an Arabic raw spoken string into a [VoiceCommand].
     */
    fun parse(rawText: String): VoiceCommand {
        val cleanedDigits = SpokenNumberParser.cleanNumberSeparators(rawText)
        val trimmed = cleanedDigits.trim()
        if (trimmed.isBlank()) {
            return VoiceCommand(type = null, rawText = rawText)
        }

        val norm = SpokenNumberParser.normalizeToken(trimmed)

        // 1. Identify command type (priority to prefix matching)
        val type = when {
            norm.startsWith("سقي") || norm.startsWith("سقيه") || norm.startsWith("سقايه") || norm.startsWith("سقيت") -> VoiceCommandType.SESSION
            norm.startsWith("قبض") || norm.startsWith("قبضت") || norm.startsWith("استلمت") || norm.startsWith("سدد") || norm.startsWith("سداد") -> VoiceCommandType.RECEIPT
            norm.startsWith("شراء") || norm.startsWith("اشتريت") || norm.startsWith("شريت") -> VoiceCommandType.PURCHASE
            norm.startsWith("خصم") || norm.startsWith("سامحت") -> VoiceCommandType.DISCOUNT
            norm.startsWith("صرف") || norm.startsWith("صرفت") || norm.startsWith("دفعت ل") -> VoiceCommandType.EXPENSE

            Regex("\\b(سقي|سقيه|سقايه|سقيت)\\b").containsMatchIn(norm) -> VoiceCommandType.SESSION
            Regex("\\b(قبض|قبضت|استلمت|سدد|سداد)\\b").containsMatchIn(norm) -> VoiceCommandType.RECEIPT
            Regex("\\b(شراء|اشتريت|شريت)\\b").containsMatchIn(norm) -> VoiceCommandType.PURCHASE
            Regex("\\b(خصم|سامحت)\\b").containsMatchIn(norm) -> VoiceCommandType.DISCOUNT
            Regex("\\b(صرف|صرفت)\\b").containsMatchIn(norm) || Regex("\\bدفعت\\s+لـ?\\b").containsMatchIn(norm) -> VoiceCommandType.EXPENSE

            else -> null
        }

        if (type == null) {
            return VoiceCommand(type = null, unparsed = listOf(rawText), rawText = rawText)
        }

        // Strip the command trigger prefix
        var workingText = when (type) {
            VoiceCommandType.SESSION -> trimmed.replaceFirst(Regex("^(سقي|سقية|سقاية|سقيت)\\s*", RegexOption.IGNORE_CASE), "")
            VoiceCommandType.RECEIPT -> trimmed.replaceFirst(Regex("^(قبض|قبضت|استلمت|سدد|سداد)\\s*", RegexOption.IGNORE_CASE), "")
            VoiceCommandType.PURCHASE -> trimmed.replaceFirst(Regex("^(شراء|اشتريت|شريت)\\s*", RegexOption.IGNORE_CASE), "")
            VoiceCommandType.DISCOUNT -> trimmed.replaceFirst(Regex("^(خصم|سامحت)\\s*", RegexOption.IGNORE_CASE), "")
            VoiceCommandType.EXPENSE -> trimmed.replaceFirst(Regex("^(صرف|صرفت|دفعت\\s+لـ?)\\s*", RegexOption.IGNORE_CASE), "")
        }.trim()

        // 2. Extract Notes (ملاحظة / البيان)
        var notes: String? = null
        val notesMatch = Regex("(ملاحظة|ملاحظه|البيان|بيان)\\s*[:\\-]?\\s*(.*)$", RegexOption.IGNORE_CASE).find(workingText)
        if (notesMatch != null) {
            notes = notesMatch.groupValues[2].trim().ifBlank { null }
            workingText = workingText.substring(0, notesMatch.range.first).trim()
        }

        // 3. Extract Paid amount for non-session/purchase commands, or if explicitly present
        var paid: Double? = null
        if (type != VoiceCommandType.SESSION && type != VoiceCommandType.PURCHASE) {
            val normWork = SpokenNumberParser.normalizeToken(workingText)
            val paidMatch = PAID_TRIGGER_REGEX.find(normWork)
            if (paidMatch != null) {
                val paidCandidate = workingText.substring(paidMatch.range.last + 1).trim()
                val cleanPaidStr = paidCandidate.split(Regex("\\b(المجموع|الإجمالي|اجمالي|مجموع)\\b"))[0].trim()
                paid = SpokenNumberParser.parseSpokenNumber(cleanPaidStr)
                workingText = workingText.substring(0, paidMatch.range.first).trim()
            }
        }

        // 4. Strip total-like words from remaining text
        // "The user never says the total. If a total-like word appears, ignore that number."
        workingText = workingText.replace(
            Regex("\\b(المجموع|الإجمالي|اجمالي|مجموع)(?:\\s+هو)?(?:\\s+بـ?)?\\s+([\\p{L}\\d]+(?:\\s+[\\p{L}\\d]+){0,4}?)(?=\\s+(?:و\\s*)?(?:دفع|دفعه|سلم|سلمه|مدفوع|دافع)|$)", RegexOption.IGNORE_CASE),
            " "
        ).trim()

        var durationMinutes: Int? = null
        var ratePerHour: Double? = null
        var amount: Double? = null
        var category: String? = null
        var customerQuery: String? = null
        val unparsedList = mutableListOf<String>()

        when (type) {
            VoiceCommandType.SESSION, VoiceCommandType.PURCHASE -> {
                val durSpan = SpokenDurationParser.findDurationSpan(workingText)
                if (durSpan != null) {
                    val durText = workingText.substring(durSpan)
                    durationMinutes = SpokenDurationParser.parseSpokenDuration(durText)

                    val beforeDur = workingText.substring(0, durSpan.first).trim()
                    val afterDur = workingText.substring(durSpan.last + 1).trim()

                    customerQuery = cleanCustomerJoiners(beforeDur)

                    if (afterDur.isNotBlank()) {
                        // Split rate and paid parts before parsing rate: the text after "من" ends at the paid word.
                        val normAfter = SpokenNumberParser.normalizeToken(afterDur)
                        val paidMatch = PAID_TRIGGER_REGEX.find(normAfter)
                        val (ratePart, paidPart) = if (paidMatch != null) {
                            val r = afterDur.substring(0, paidMatch.range.first).trim()
                            val p = afterDur.substring(paidMatch.range.last + 1).trim()
                            Pair(r, p)
                        } else {
                            Pair(afterDur, null)
                        }

                        if (ratePart.isNotBlank()) {
                            var cleanRateStr = ratePart
                                .replace(Regex("^(من|بسعر|سعر\\s+الساعه|سعر\\s+الساعة|الساعه\\s+بـ?|الساعة\\s+بـ?)\\s*"), "")
                                .trim()
                            cleanRateStr = cleanRateStr.replace(
                                Regex("\\b(المجموع|الإجمالي|اجمالي|مجموع)(?:\\s+هو)?(?:\\s+بـ?)?\\s+([\\p{L}\\d]+(?:\\s+[\\p{L}\\d]+){0,4}?)(?=\\s+(?:و\\s*)?(?:دفع|دفعه|سلم|سلمه|مدفوع|دافع)|$)", RegexOption.IGNORE_CASE),
                                " "
                            ).trim()
                            ratePerHour = SpokenNumberParser.parseSpokenNumber(cleanRateStr)
                            if (ratePerHour == null && cleanRateStr.isNotBlank()) {
                                unparsedList.add(cleanRateStr)
                            }
                        }

                        if (!paidPart.isNullOrBlank()) {
                            val cleanPaid = paidPart.split(Regex("\\b(المجموع|الإجمالي|اجمالي|مجموع)\\b"))[0].trim()
                            paid = SpokenNumberParser.parseSpokenNumber(cleanPaid)
                            if (paid == null && cleanPaid.isNotBlank()) {
                                unparsedList.add(cleanPaid)
                            }
                        }
                    }
                } else {
                    val normWork = SpokenNumberParser.normalizeToken(workingText)
                    val paidMatch = PAID_TRIGGER_REGEX.find(normWork)
                    val (beforePaid, paidPart) = if (paidMatch != null) {
                        Pair(workingText.substring(0, paidMatch.range.first).trim(), workingText.substring(paidMatch.range.last + 1).trim())
                    } else {
                        Pair(workingText, null)
                    }

                    val rateMatch = Regex("(من|بسعر|سعر\\s+الساعه|سعر\\s+الساعة|الساعه\\s+بـ?|الساعة\\s+بـ?)\\s+([\\p{L}\\d\\s,]+)$").find(beforePaid)
                    if (rateMatch != null) {
                        val custPart = beforePaid.substring(0, rateMatch.range.first).trim()
                        val ratePart = rateMatch.groupValues[2].trim()
                        customerQuery = cleanCustomerJoiners(custPart)
                        ratePerHour = SpokenNumberParser.parseSpokenNumber(ratePart)
                    } else {
                        customerQuery = cleanCustomerJoiners(beforePaid)
                    }

                    if (!paidPart.isNullOrBlank()) {
                        val cleanPaid = paidPart.split(Regex("\\b(المجموع|الإجمالي|اجمالي|مجموع)\\b"))[0].trim()
                        paid = SpokenNumberParser.parseSpokenNumber(cleanPaid)
                    }
                }
            }

            VoiceCommandType.RECEIPT, VoiceCommandType.DISCOUNT -> {
                val words = workingText.split(Regex("\\s+")).filter { it.isNotBlank() }
                var bestSplitIndex = -1
                var parsedAmt: Double? = null

                // Search from right to left to find longest valid number at the end
                for (k in words.indices) {
                    val potentialAmtStr = words.subList(k, words.size).joinToString(" ")
                    val parsed = SpokenNumberParser.parseSpokenNumber(potentialAmtStr)
                    if (parsed != null) {
                        bestSplitIndex = k
                        parsedAmt = parsed
                        break
                    }
                }

                if (bestSplitIndex > 0 && parsedAmt != null) {
                    val custStr = words.subList(0, bestSplitIndex).joinToString(" ")
                    customerQuery = cleanCustomerJoiners(custStr)
                    amount = parsedAmt
                } else {
                    val num = SpokenNumberParser.parseSpokenNumber(workingText)
                    if (num != null) {
                        amount = num
                    } else {
                        customerQuery = cleanCustomerJoiners(workingText)
                    }
                }
            }

            VoiceCommandType.EXPENSE -> {
                for (cat in EXPENSE_CATEGORIES) {
                    val p = Regex("\\b$cat\\b", RegexOption.IGNORE_CASE)
                    if (p.containsMatchIn(workingText)) {
                        category = cat
                        workingText = workingText.replace(p, " ").replace(Regex("\\s+"), " ").trim()
                        break
                    }
                }

                val parsedNum = SpokenNumberParser.parseSpokenNumber(workingText)
                if (parsedNum != null) {
                    amount = parsedNum
                } else {
                    val words = workingText.split(Regex("\\s+")).filter { it.isNotBlank() }
                    for (k in words.indices) {
                        val sub = words.subList(k, words.size).joinToString(" ")
                        val n = SpokenNumberParser.parseSpokenNumber(sub)
                        if (n != null) {
                            amount = n
                            val leftover = words.subList(0, k).joinToString(" ").trim()
                            if (leftover.isNotBlank()) {
                                val cleanLeftover = leftover.replace(Regex("^(شراء|لـ|ل|حق|شريت)\\s+"), "").trim()
                                if (cleanLeftover.isNotBlank()) {
                                    notes = notes?.let { "$it $cleanLeftover" } ?: cleanLeftover
                                }
                            }
                            break
                        }
                    }
                }

                if (category == null && workingText.isNotBlank() && amount == null) {
                    notes = workingText
                }
            }
        }

        return VoiceCommand(
            type = type,
            customerQuery = customerQuery?.takeIf { it.isNotBlank() },
            durationMinutes = durationMinutes,
            ratePerHour = ratePerHour,
            amount = amount,
            paid = paid,
            category = category,
            notes = notes,
            unparsed = unparsedList,
            rawText = rawText
        )
    }

    private fun cleanCustomerJoiners(text: String): String {
        return text
            .replace(Regex("^(من|لـ|عند|حق)\\s*|^ل(?=[\\p{L}])"), "")
            .replace(Regex("^(الأخ|الاخ|العم|الحاج|الشيخ)\\s+"), "")
            .trim()
            .removeSuffix("،")
            .removeSuffix(",")
            .trim()
    }
}
