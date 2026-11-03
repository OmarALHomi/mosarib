package com.example.core.voice

import java.util.Calendar

/**
 * Pure parser converting spoken Arabic command phrases into structured [VoiceCommand].
 * Follows the rules:
 * - Slot-based parsing independent of word order (pieces found anywhere, what remains is the name/notes)
 * - Never saves by voice (only pre-fills forms)
 * - The user never says the total (ignores total-like words)
 * - Distinguishes SESSION, RECEIPT, EXPENSE, PURCHASE, DISCOUNT
 */
object VoiceCommandParser {

    /**
     * Parses an Arabic raw spoken string into a [VoiceCommand].
     *
     * @param rawText Spoken phrase from speech recognizer
     * @param knownNames List of known customer names and aliases for separating name from notes
     * @param now Current timestamp in milliseconds (defaults to System.currentTimeMillis())
     */
    fun parse(
        rawText: String,
        knownNames: List<String> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): VoiceCommand {
        val trimmedRaw = rawText.trim()
        if (trimmedRaw.isBlank()) {
            return VoiceCommand(type = null, rawText = rawText)
        }

        // Section 7.1: Normalization
        val cleaned = SpokenNumberParser.cleanNumberSeparators(trimmedRaw)
        var norm = SpokenNumberParser.normalizeToken(cleaned)
        norm = SpokenNumberParser.splitGluedConjunction(norm)
        norm = norm.replace(Regex("[,،.]"), " ")
        norm = norm.replace(Regex("\\s+"), " ").trim()

        // Section 7.7: Two operations in one phrase
        val (firstOpText, secondOpBanner) = checkTwoOperations(norm)
        val warningBanner = secondOpBanner

        // Section 7.6: In-phrase corrections ("لا قصدي", "لا لا", "قصدي"...)
        for (trigger in VoicePhraseRules.CORRECTION_TRIGGERS) {
            val normTrigger = SpokenNumberParser.normalizeToken(trigger)
            val regex = Regex("(?:^|\\s)$normTrigger(?:\\s|$)")
            val match = regex.find(firstOpText)
            if (match != null && match.range.first > 0) {
                val before = firstOpText.substring(0, match.range.first).trim()
                val after = firstOpText.substring(match.range.last + 1).trim()
                if (before.isNotBlank() && after.isNotBlank()) {
                    val pBefore = parseSingleOperation(before, before, knownNames, now)
                    val pAfter = parseSingleOperation(after, after, knownNames, now)

                    val hasDur = pAfter.durationMinutes != null || pBefore.durationMinutes != null
                    val mergedType = pAfter.type ?: pBefore.type ?: (if (hasDur) VoiceCommandType.SESSION else null)
                    val mergedRate = pAfter.ratePerHour ?: (if (hasDur && pAfter.amount != null) pAfter.amount else pBefore.ratePerHour)
                    val mergedAmount = if (hasDur) null else (pAfter.amount ?: pBefore.amount)

                    return VoiceCommand(
                        type = mergedType,
                        customerQuery = pAfter.customerQuery ?: pBefore.customerQuery,
                        durationMinutes = pAfter.durationMinutes ?: pBefore.durationMinutes,
                        ratePerHour = mergedRate,
                        amount = mergedAmount,
                        paid = pAfter.paid ?: pBefore.paid,
                        paidFull = pAfter.paidFull || pBefore.paidFull,
                        category = pAfter.category ?: pBefore.category,
                        notes = pAfter.notes ?: pBefore.notes,
                        rawText = rawText,
                        startMillis = pAfter.startMillis ?: pBefore.startMillis,
                        endMillis = pAfter.endMillis ?: pBefore.endMillis,
                        warningBanner = listOfNotNull(warningBanner, pAfter.warningBanner, pBefore.warningBanner).joinToString(" · ").ifBlank { null }
                    )
                }
            }
        }

        // Parse single operation slots
        val parsed = parseSingleOperation(firstOpText, trimmedRaw, knownNames, now)

        // Combine warning banners if any
        val combinedBanner = when {
            warningBanner != null && parsed.warningBanner != null -> "$warningBanner · ${parsed.warningBanner}"
            warningBanner != null -> warningBanner
            else -> parsed.warningBanner
        }

        return parsed.copy(
            rawText = rawText,
            warningBanner = combinedBanner
        )
    }

    /**
     * Checks if phrase contains two operations (Section 7.7).
     * If so, returns the first operation text and a warning banner.
     */
    private fun checkTwoOperations(text: String): Pair<String, String?> {
        val pattern = Regex(
            "\\s+(?:و|ثم)\\s*(?:(قبض|قبضت|استلمت|سدد|سداد|صرف|صرفت|دفعت|اعطيت|سقي|سقيه|سقايه|شراء|اشتريت|خصم)\\b|(\\d+|[\\p{L}]+)\\s+(?:الف|الاف|آلاف|تلاف)\\s+(?:ديزل|صيانة|صيانه|زيت|كهرباء))"
        )
        val match = pattern.find(text)
        if (match != null && match.range.first > 0) {
            val firstPart = text.substring(0, match.range.first).trim()
            val banner = "قلت عمليتين، سُجّلت الأولى، أعد قول الثانية"
            return Pair(firstPart, banner)
        }
        return Pair(text, null)
    }

    /**
     * Core slot-based parser for a single spoken Arabic operation.
     */
    private fun parseSingleOperation(
        text: String,
        rawSource: String,
        knownNames: List<String>,
        now: Long
    ): VoiceCommand {
        var working = text
        var notes: String? = null

        // 1. Explicit Notes (ملاحظة / البيان) - priority extraction
        val notesMatch = Regex("(?:\\b|(?<=\\s))(ملاحظة|ملاحظه|البيان|بيان)\\s*[:\\-]?\\s*(.*)$", RegexOption.IGNORE_CASE).find(working)
        if (notesMatch != null) {
            val rawNoteVal = notesMatch.groupValues[2].trim().ifBlank { null }
            notes = rawNoteVal?.let { findInRawSource(it, rawSource) ?: it }
            working = working.substring(0, notesMatch.range.first).trim()
        }

        // 2. Prefix command type matching (highest priority for verbs at the start)
        var explicitType: VoiceCommandType? = when {
            working.startsWith("سقي ") || working.startsWith("سقيه ") || working.startsWith("سقايه ") || working.startsWith("سقيت ") || working.startsWith("ري ") -> VoiceCommandType.SESSION
            working.startsWith("قبض ") || working.startsWith("قبضت ") || working.startsWith("استلمت ") || working.startsWith("سدد ") || working.startsWith("سداد ") || working.startsWith("وصل ") || working.startsWith("وصلني ") -> VoiceCommandType.RECEIPT
            working.startsWith("صرف ") || working.startsWith("صرفت ") || working.startsWith("دفعت ل") -> VoiceCommandType.EXPENSE
            working.startsWith("شراء ") || working.startsWith("اشتريت ") || working.startsWith("شريت ") -> VoiceCommandType.PURCHASE
            working.startsWith("خصم ") || working.startsWith("سامحت ") -> VoiceCommandType.DISCOUNT
            else -> null
        }

        if (explicitType != null) {
            // Strip the leading command verb
            working = working.replaceFirst(Regex("^(سقي|سقيه|سقايه|سقيت|ري|قبض|قبضت|استلمت|سدد|سداد|وصل|وصلني|صرف|صرفت|دفعت\\s+لـ?|شراء|اشتريت|شريت|خصم|سامحت)\\s+"), "").trim()
        }

        // 3. Time range check (Section 6 B & B2)
        val timeRangeResult = parseTimeRange(working, now)
        var startMillis: Long? = null
        var endMillis: Long? = null
        var rangeDurationMinutes: Int? = null
        var bannerWarning: String? = null

        if (timeRangeResult != null) {
            startMillis = timeRangeResult.startMillis
            endMillis = timeRangeResult.endMillis
            rangeDurationMinutes = timeRangeResult.durationMinutes
            bannerWarning = timeRangeResult.bannerWarning
            working = timeRangeResult.remainingText
        }

        // 4. Spoken duration (Section 7.4)
        var durationMinutes: Int? = rangeDurationMinutes
        val durSpan = SpokenDurationParser.findDurationSpan(working)
        if (durSpan != null) {
            val durText = working.substring(durSpan)
            val parsedDur = SpokenDurationParser.parseSpokenDuration(durText)
            if (parsedDur != null) {
                if (rangeDurationMinutes != null) {
                    val diff = Math.abs(parsedDur - rangeDurationMinutes)
                    if (diff > 15) {
                        val nStr = if (parsedDur % 60 == 0) "${parsedDur / 60}" else "$parsedDur دقيقة"
                        val mStr = if (rangeDurationMinutes % 60 == 0) "${rangeDurationMinutes / 60}" else "$rangeDurationMinutes دقيقة"
                        bannerWarning = "قلت $nStr ساعات، والمدة بين الوقتين $mStr، راجعها"
                    }
                } else {
                    durationMinutes = parsedDur
                }
                working = (working.substring(0, durSpan.first) + " " + working.substring(durSpan.last + 1)).trim()
            }
        }

        // 5. Strip total-like words (user never says the total)
        working = working.replace(
            Regex("\\b(المجموع|الاجمالي|اجمالي|مجموع)(?:\\s+هو)?(?:\\s+بـ?)?\\s+([\\p{L}\\d]+(?:\\s+[\\p{L}\\d]+){0,4}?)(?=\\s+(?:و\\s*)?(?:دفع|دفعه|سلم|سلمه|مدفوع|دافع)|$)", RegexOption.IGNORE_CASE),
            " "
        ).trim()

        // 6. Extract paidFull
        var isPaidFull = false
        for (pf in VoicePhraseRules.PAID_FULL_WORDS) {
            val normPf = SpokenNumberParser.normalizeToken(pf)
            val p = Regex("\\b$normPf\\b")
            if (p.containsMatchIn(working)) {
                isPaidFull = true
                working = working.replace(p, " ").trim()
                break
            }
        }

        // 7. Extract amounts, rate and paid
        val (rate, amountRaw, paidRaw, cleanAfterAmounts) = extractAmounts(
            text = working,
            hasDuration = (durationMinutes != null),
            isSessionType = (explicitType == VoiceCommandType.SESSION || explicitType == VoiceCommandType.PURCHASE)
        )
        working = cleanAfterAmounts

        val (amount, paid) = if (amountRaw == null && paidRaw != null && (explicitType == VoiceCommandType.RECEIPT || explicitType == VoiceCommandType.EXPENSE || (durationMinutes == null && explicitType != VoiceCommandType.SESSION))) {
            Pair(paidRaw, null)
        } else {
            Pair(amountRaw, paidRaw)
        }

        // 7b. Explicit debt markers (only when no paid amount was explicitly spoken)
        var isExplicitDebt = false
        if (paid == null) {
            for (ed in VoicePhraseRules.EXPLICIT_DEBT_WORDS) {
                val normEd = SpokenNumberParser.normalizeToken(ed)
                if (normEd == "عليه" && (working.contains("باقي عليه") || working.contains("و باقي عليه") || working.contains("وباقي عليه"))) {
                    continue
                }
                val p = Regex("\\b$normEd\\b")
                if (p.containsMatchIn(working)) {
                    isExplicitDebt = true
                    working = working.replace(p, " ").trim()
                    break
                }
            }
        }

        // 8. Extract expense category
        var category: String? = null
        for (cat in VoicePhraseRules.EXPENSE_CATEGORIES) {
            val normCat = SpokenNumberParser.normalizeToken(cat)
            val p = Regex("(?:^|\\s)(?:ال)?$normCat(?:\\s|$)")
            if (p.containsMatchIn(working)) {
                category = cat
                break
            }
        }

        // 9. Non-prefix explicit type search (if not already found at the start)
        if (explicitType == null) {
            for ((cmdType, words) in VoicePhraseRules.TYPE_WORDS) {
                for (w in words) {
                    val normW = SpokenNumberParser.normalizeToken(w)
                    val p = Regex("\\b$normW\\b")
                    if (p.containsMatchIn(working)) {
                        explicitType = cmdType
                        if (w !in listOf("سلفة", "سلفه", "سلفت", "حق", "حساب")) {
                            working = working.replace(p, " ").trim()
                        }
                        break
                    }
                }
                if (explicitType != null) break
            }
        }

        // 10. Extract customer query and notes
        val (customerQuery, extraNotes) = separateCustomerAndNotes(
            text = working,
            rawSource = rawSource,
            knownNames = knownNames,
            hasCategory = (category != null),
            isExpense = (explicitType == VoiceCommandType.EXPENSE)
        )

        if (extraNotes != null && extraNotes != category && extraNotes != "ال$category") {
            notes = if (notes != null) "$extraNotes $notes" else extraNotes
        }

        // 11. Decide final operation type
        val finalType = determineOperationType(
            explicitType = explicitType,
            hasDuration = (durationMinutes != null),
            hasTimeRange = (startMillis != null && endMillis != null),
            originalText = text,
            customer = customerQuery,
            amount = amount,
            rate = rate,
            category = category
        )

        val finalPaid = when {
            paid != null -> paid
            isExplicitDebt -> null
            isPaidFull -> null
            else -> paid
        }

        return VoiceCommand(
            type = finalType,
            customerQuery = customerQuery,
            durationMinutes = durationMinutes,
            ratePerHour = rate,
            amount = amount,
            paid = finalPaid,
            category = category,
            notes = notes?.takeIf { it.isNotBlank() },
            unparsed = emptyList(),
            rawText = rawSource,
            paidFull = isPaidFull,
            startMillis = startMillis,
            endMillis = endMillis,
            warningBanner = bannerWarning
        )
    }

    /**
     * Extracts amounts, rates, and paid values with their position and markers.
     */
    private data class AmountsExtraction(
        val rate: Double?,
        val amount: Double?,
        val paid: Double?,
        val remainingText: String
    )

    private fun extractAmounts(text: String, hasDuration: Boolean, isSessionType: Boolean): AmountsExtraction {
        var working = text
        var rate: Double? = null
        var paid: Double? = null
        var amount: Double? = null

        // 1. Look for paid markers with numbers: سلم / مسلم / دفع / وادي 20 الف
        val paidRes = extractAmountAfterMarker(working, VoicePhraseRules.PAID_MARKERS)
        if (paidRes != null) {
            paid = paidRes.first
            working = paidRes.second
        }

        // 2. Look for rate markers with numbers: من / بسعر / سعر الساعه 10 الف
        if (hasDuration || isSessionType) {
            val rateRes = extractAmountAfterMarker(working, VoicePhraseRules.RATE_MARKERS)
            if (rateRes != null) {
                rate = rateRes.first
                working = rateRes.second
            }
        }

        // 3. Find remaining numbers in the text
        val tokens = working.split(" ").filter { it.isNotBlank() }
        val foundNumbers = mutableListOf<Pair<Double, IntRange>>()

        var idx = 0
        while (idx < tokens.size) {
            var found = false
            for (len in minOf(5, tokens.size - idx) downTo 1) {
                if (idx + len < tokens.size && tokens[idx + len] in listOf("الف", "الاف", "آلاف", "تلاف", "الفين", "مليون", "ملايين")) {
                    continue
                }
                val candidateTokens = tokens.subList(idx, idx + len)
                if (candidateTokens.count { it in listOf("الف", "الاف", "آلاف", "تلاف", "الفين", "مليون", "ملايين") } > 1) {
                    continue
                }
                val candidate = candidateTokens.joinToString(" ")
                val num = SpokenNumberParser.parseSpokenNumber(candidate)
                if (num != null) {
                    foundNumbers.add(Pair(num, IntRange(idx, idx + len - 1)))
                    idx += len
                    found = true
                    break
                }
            }
            if (!found) idx++
        }

        if (foundNumbers.isNotEmpty()) {
            if (hasDuration || isSessionType) {
                if (rate == null && paid == null) {
                    if (foundNumbers.isNotEmpty()) rate = foundNumbers[0].first
                    if (foundNumbers.size > 1) paid = foundNumbers[1].first
                } else if (rate == null) {
                    rate = foundNumbers[0].first
                } else if (paid == null) {
                    paid = foundNumbers[0].first
                }
            } else {
                if (amount == null && foundNumbers.isNotEmpty()) {
                    amount = foundNumbers[0].first
                }
            }

            val matchedWordIndices = foundNumbers.flatMap { it.second }.toSet()
            working = tokens.filterIndexed { i, _ -> i !in matchedWordIndices }.joinToString(" ")
        }

        // Clean currency words and dangling joiners/markers left behind
        working = working.replace(Regex("\\b(ريال|ريالات|الف|الاف)\\b"), " ")
        working = working.replace(Regex("\\s+(من|و|مع|و\\s+ادي|ادي|و\\s+سلم|و\\s+دفع)\\s*$"), " ")
        working = working.replace(Regex("\\s+من\\s*$"), " ")
        working = working.replace(Regex("\\s+و\\s*$"), " ")
        working = working.replace(Regex("^و\\s+"), " ")
        working = working.replace(Regex("\\s+"), " ").trim()

        return AmountsExtraction(rate = rate, amount = amount, paid = paid, remainingText = working)
    }

    private fun extractAmountAfterMarker(
        text: String,
        markers: List<String>
    ): Pair<Double, String>? {
        val sortedMarkers = markers.sortedByDescending { it.length }
        for (marker in sortedMarkers) {
            val normMarker = SpokenNumberParser.normalizeToken(marker)
            val baseMarker = if (normMarker.startsWith("و") && normMarker.length > 2) normMarker.removePrefix("و") else normMarker
            val pattern = Regex("(?:^|\\s)(?:و\\s*)?$baseMarker\\s+")
            for (match in pattern.findAll(text)) {
                val afterText = text.substring(match.range.last + 1)
                val tokens = afterText.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (tokens.isEmpty()) continue

                for (len in minOf(4, tokens.size) downTo 1) {
                    if (len < tokens.size && tokens[len] in listOf("الف", "الاف", "آلاف", "تلاف", "الفين", "مليون", "ملايين")) {
                        continue
                    }
                    val candidateTokens = tokens.subList(0, len)
                    if (candidateTokens.count { it in listOf("الف", "الاف", "آلاف", "تلاف", "الفين", "مليون", "ملايين") } > 1) {
                        continue
                    }
                    val candidateStr = candidateTokens.joinToString(" ")
                    val num = SpokenNumberParser.parseSpokenNumber(candidateStr)
                    if (num != null) {
                        val candRegex = Regex("^" + candidateTokens.joinToString("\\s+") { Regex.escape(it) } + "(?:\\s+(?:ريال|ريالات|ريالاً))?")
                        val candMatch = candRegex.find(afterText.trimStart())
                        val consumedLength = if (candMatch != null) {
                            afterText.indexOf(candMatch.value) + candMatch.value.length
                        } else {
                            candidateTokens.joinToString(" ").length
                        }
                        val remaining = (text.substring(0, match.range.first) + " " + afterText.substring(consumedLength))
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        return Pair(num, remaining)
                    }
                }
            }
        }
        return null
    }

    /**
     * Separates the customer name from the notes / description based on joiners, known names, and starters.
     * Preserves original letters from rawSource for customerQuery and notes.
     */
    private fun separateCustomerAndNotes(
        text: String,
        rawSource: String,
        knownNames: List<String>,
        hasCategory: Boolean,
        isExpense: Boolean = false
    ): Pair<String?, String?> {
        var working = text.trim()

        // Section 6 E: General expense without customer
        val firstWord = working.split(" ").firstOrNull().orEmpty()
        val startsWithDescription = VoicePhraseRules.DESCRIPTION_STARTERS.contains(firstWord)
        if (startsWithDescription && !working.startsWith("من ") && !working.startsWith("مع ")) {
            val rawNotes = findInRawSource(working, rawSource) ?: working
            return Pair(null, rawNotes)
        }

        val hasCustomerJoiner = Regex("^(من|مع|عند|حق|لحساب|حساب|على\\s+حساب|للأخ|للاخ|ل(?=[\\p{L}]))\\s*").containsMatchIn(working)

        // Clean customer joiners: من، مع، لـ، لحساب، حق، عند...
        working = working.replace(Regex("^(من|مع|عند|حق|لحساب|حساب|على\\s+حساب|للأخ|للاخ)\\s+"), "")
        working = working.replace(Regex("^ل(?=[\\p{L}])"), "")

        // Clean honorifics: الأخ، الاخ، العم، الحاج، الشيخ
        for (hon in VoicePhraseRules.HONORIFICS) {
            val normHon = SpokenNumberParser.normalizeToken(hon)
            if (working.startsWith("$normHon ")) {
                working = working.removePrefix("$normHon ").trim()
                break
            }
        }

        // Drop trailing words from the end
        for (tw in VoicePhraseRules.TRAILING_WORDS) {
            val normTw = SpokenNumberParser.normalizeToken(tw)
            if (working.endsWith(" $normTw")) {
                working = working.removeSuffix(" $normTw").trim()
            }
        }

        // Strip trailing punctuation / markers
        working = working.replace(Regex("\\s+(من|و)$"), "").trim()

        if (working.isBlank()) {
            return Pair(null, null)
        }

        val words = working.split(" ").filter { it.isNotBlank() }

        // Match known customer names
        var matchedCustName: String? = null
        var leftoverNotes: String? = null

        if (knownNames.isNotEmpty()) {
            for (len in minOf(5, words.size) downTo 1) {
                val candidateName = words.take(len).joinToString(" ")
                val normCand = SpokenNumberParser.normalizeToken(candidateName)
                val match = knownNames.firstOrNull { kn ->
                    val normKn = SpokenNumberParser.normalizeToken(kn)
                    normKn == normCand || CustomerMatcher.normalizeForMatching(kn) == CustomerMatcher.normalizeForMatching(candidateName)
                }
                if (match != null) {
                    matchedCustName = match
                    val noteCandidate = words.drop(len).joinToString(" ").trim().ifBlank { null }
                    leftoverNotes = noteCandidate?.let { findInRawSource(it, rawSource) ?: it }
                    break
                }
            }
        }

        if (matchedCustName == null) {
            val firstWord = words.firstOrNull().orEmpty()
            val startsWithDesc = VoicePhraseRules.DESCRIPTION_STARTERS.contains(firstWord)
            if ((hasCategory || startsWithDesc) && !hasCustomerJoiner) {
                val cleanExpenseNotes = working.replace(Regex("^(شراء|شريت)\\s+"), "").trim()
                val candidateNotes = cleanExpenseNotes.ifBlank { working }
                val rawNotes = findInRawSource(candidateNotes, rawSource) ?: candidateNotes
                return Pair(null, rawNotes)
            }

            var nameEndIndex = words.size
            for (i in words.indices) {
                val w = words[i]
                if (i > 0 && VoicePhraseRules.DESCRIPTION_STARTERS.contains(w)) {
                    nameEndIndex = i
                    break
                }
                if (i > 0 && (w == "باقي" || w == "سلفه" || w == "سلفة" || w == "ملاحظه" || w == "بيان")) {
                    nameEndIndex = i
                    break
                }
            }

            val rawCustCandidate = words.take(nameEndIndex).joinToString(" ").trim()
            val candidateNotes = words.drop(nameEndIndex).joinToString(" ").trim().ifBlank { null }
            leftoverNotes = candidateNotes?.let { findInRawSource(it, rawSource) ?: it }

            // Extract matching substring from rawSource to preserve original letters (e.g. أحمد vs احمد)
            matchedCustName = findInRawSource(rawCustCandidate, rawSource) ?: rawCustCandidate
        }

        return Pair(matchedCustName.takeIf { it.isNotBlank() }, leftoverNotes)
    }

    /**
     * Finds the exact original substring in rawSource that corresponds to normalized candidate.
     * Preserves hamzas (أ / إ / آ), taa marbuta (ة) from original speech input.
     */
    private fun findInRawSource(normCandidate: String, rawSource: String): String? {
        if (normCandidate.isBlank()) return null
        val normTarget = SpokenNumberParser.normalizeToken(normCandidate)
        val rawWords = rawSource.split(Regex("[\\s,،]+")).filter { it.isNotBlank() }

        for (len in minOf(15, rawWords.size) downTo 1) {
            for (start in 0..(rawWords.size - len)) {
                val slice = rawWords.subList(start, start + len).joinToString(" ")
                val cleanSlice = slice
                    .replace(Regex("^(من|مع|عند|حق|لحساب|حساب|سقي|قبض|صرف|شراء|خصم)\\s+"), "")
                    .replace(Regex("^ل(?=[\\p{L}])"), "")
                    .replace(Regex("^(الأخ|الاخ|العم|الحاج|الشيخ)\\s+"), "")
                    .trim()
                if (SpokenNumberParser.normalizeToken(cleanSlice) == normTarget) {
                    return cleanSlice
                }
                if (SpokenNumberParser.normalizeToken(slice) == normTarget) {
                    return slice
                }
            }
        }
        return normCandidate
    }

    /**
     * Decides the command operation type based on Section 2 & 6 priorities.
     */
    private fun determineOperationType(
        explicitType: VoiceCommandType?,
        hasDuration: Boolean,
        hasTimeRange: Boolean,
        originalText: String,
        customer: String?,
        amount: Double?,
        rate: Double?,
        category: String?
    ): VoiceCommandType? {
        val norm = SpokenNumberParser.normalizeToken(originalText)

        if (explicitType != null) {
            return explicitType
        }

        if (hasDuration || hasTimeRange) {
            return if (norm.contains("شراء") || norm.contains("قيد شراء") || norm.contains("سجل شراء")) {
                VoiceCommandType.PURCHASE
            } else {
                VoiceCommandType.SESSION
            }
        }

        // Pattern C: amount + "مع {name}" (no duration) -> EXPENSE
        if (customer != null && amount != null && (norm.contains(" مع ") || norm.startsWith("مع "))) {
            return VoiceCommandType.EXPENSE
        }

        // Pattern D: amount + "من {name}" (no duration) -> RECEIPT
        if (customer != null && amount != null && (norm.contains(" من ") || norm.startsWith("من "))) {
            return VoiceCommandType.RECEIPT
        }

        // Receipt keywords with amount + customer
        if (customer != null && amount != null && (
            norm.contains("واصل") || norm.contains("واصل منه") || norm.startsWith("قبض ") || norm.startsWith("استلمت ") || norm.startsWith("سدد ")
        )) {
            return VoiceCommandType.RECEIPT
        }

        // Expense words
        if (norm.contains("صرف") || norm.contains("صرفت") || norm.contains("دفعت ل")) {
            return VoiceCommandType.EXPENSE
        }

        // Expense category with amount
        if (category != null && amount != null) {
            return VoiceCommandType.EXPENSE
        }

        // General expense description starters without customer
        if (customer == null && amount != null) {
            for (ds in VoicePhraseRules.DESCRIPTION_STARTERS) {
                if (norm.contains(ds)) return VoiceCommandType.EXPENSE
            }
        }

        return null
    }

    /**
     * Result of parsing time range (Section 6 B & B2).
     */
    private data class TimeRangeResult(
        val startMillis: Long,
        val endMillis: Long,
        val durationMinutes: Int,
        val bannerWarning: String?,
        val remainingText: String
    )

    private fun parseTimeRange(text: String, now: Long): TimeRangeResult? {
        val norm = text

        val dayWordPattern = "قبل امس|اول من امس|اول امس|اليوم|امس|البارحه|البارح|يوم\\s+\\d+|\\d{1,2}[/-]\\d{1,2}|يوم\\s+[\\p{L}]+"
        val dayPartPattern = "بالصبح|الصبح|صباحا|صباحاً|بالليل|الليل|مساء|مساءً|الظهر|الظهريه|العصر|بعد العصر|المغرب|العشاء|الفجر|الضحى|الضحي"
        val timePattern = "نص الليل|نصف الليل|\\d{1,2}:\\d{2}|\\d{1,2}(?:\\s+(?:و\\s*(?:نص|نصف|ربع|ثلث|تلت|عشر(?:\\s*دقايق)?|20)|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت)))?"
        val joinerPattern = "الى|إلى|الي|لا|لاا|لااا|لين|حتى|حتي|لما|لعند"

        val rangeRegex = Regex(
            "(?:^|\\s)من\\s+(?:($dayWordPattern)\\s+)?(?:($dayPartPattern)\\s+)?(?:الساعه\\s+)?($timePattern)(?:\\s+($dayPartPattern))?\\s+($joinerPattern)\\s+(?:($dayWordPattern)\\s+)?(?:($dayPartPattern)\\s+)?(?:الساعه\\s+)?($timePattern)(?:\\s+($dayPartPattern))?(?=\\s+(?:و\\s*)?(?:من|بسعر|سعر|مسلم|سلم|دفع|كامل|حق|\\d|$)|$)"
        )

        val match = rangeRegex.find(norm) ?: return null

        val startDayStr = match.groupValues[1].trim().ifBlank { null }
        val startDayPartPre = match.groupValues[2].trim().ifBlank { null }
        val startTimeStr = match.groupValues[3].trim().ifBlank { null } ?: return null
        val startDayPartPost = match.groupValues[4].trim().ifBlank { null }
        val startDayPart = startDayPartPre ?: startDayPartPost

        val endDayStr = match.groupValues[6].trim().ifBlank { null }
        val endDayPartPre = match.groupValues[7].trim().ifBlank { null }
        val endTimeStr = match.groupValues[8].trim().ifBlank { null } ?: return null
        val endDayPartPost = match.groupValues[9].trim().ifBlank { null }
        val endDayPart = endDayPartPre ?: endDayPartPost

        val startCal = resolveDateTime(
            dayStr = startDayStr,
            timeStr = startTimeStr,
            dayPart = startDayPart,
            isStart = true,
            referenceTime = now,
            startReference = null
        ) ?: return null

        val endCal = resolveDateTime(
            dayStr = endDayStr,
            timeStr = endTimeStr,
            dayPart = endDayPart,
            isStart = false,
            referenceTime = now,
            startReference = startCal.timeInMillis
        ) ?: return null

        val sMillis = startCal.timeInMillis
        var eMillis = endCal.timeInMillis

        if (eMillis <= sMillis) {
            eMillis += 24 * 3600 * 1000L
        }

        val durMin = ((eMillis - sMillis) / 60000L).toInt()
        val remaining = (text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)).replace(Regex("\\s+"), " ").trim()

        return TimeRangeResult(
            startMillis = sMillis,
            endMillis = eMillis,
            durationMinutes = durMin,
            bannerWarning = null,
            remainingText = remaining
        )
    }

    private fun resolveDateTime(
        dayStr: String?,
        timeStr: String,
        dayPart: String?,
        isStart: Boolean,
        referenceTime: Long,
        startReference: Long?
    ): Calendar? {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceTime }

        val (hour, minute) = parseClockTime(timeStr, dayPart) ?: return null

        if (dayStr != null) {
            val dNorm = SpokenNumberParser.normalizeToken(dayStr)
            when {
                dNorm in listOf("اليوم") -> { /* today */ }
                dNorm in listOf("امس", "البارحه", "البارح") -> cal.add(Calendar.DAY_OF_YEAR, -1)
                dNorm in listOf("قبل امس", "اول امس", "اول من امس") -> cal.add(Calendar.DAY_OF_YEAR, -2)
                dNorm.startsWith("يوم ") && dNorm.substring(4).all { it.isDigit() } -> {
                    val targetDay = dNorm.substring(4).toInt()
                    cal.set(Calendar.DAY_OF_MONTH, targetDay)
                    if (cal.timeInMillis > referenceTime) cal.add(Calendar.MONTH, -1)
                }
                dNorm.contains("/") || dNorm.contains("-") -> {
                    val parts = dNorm.split(Regex("[/-]"))
                    if (parts.size >= 2) {
                        val d = parts[0].toIntOrNull() ?: 1
                        val m = (parts[1].toIntOrNull() ?: 1) - 1
                        cal.set(Calendar.MONTH, m)
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        if (cal.timeInMillis > referenceTime) cal.add(Calendar.YEAR, -1)
                    }
                }
                VoicePhraseRules.WEEKDAYS.containsKey(dNorm.removePrefix("يوم ")) -> {
                    val targetDow = VoicePhraseRules.WEEKDAYS[dNorm.removePrefix("يوم ")]!!
                    while (cal.get(Calendar.DAY_OF_WEEK) != targetDow) {
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                }
            }
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        if (isStart) {
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (dayPart == null && hour in 1..11) {
                // Pick the most recent past time for t1 relative to referenceTime
                val pmCal = (cal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, hour + 12) }
                if (pmCal.timeInMillis <= referenceTime) {
                    cal.set(Calendar.HOUR_OF_DAY, hour + 12)
                } else if (cal.timeInMillis > referenceTime) {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
            } else if (cal.timeInMillis > referenceTime) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            return cal
        } else {
            val startMs = startReference ?: referenceTime
            cal.timeInMillis = startMs
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (cal.timeInMillis <= startMs) {
                if (dayPart == null && hour in 1..11) {
                    cal.set(Calendar.HOUR_OF_DAY, hour + 12)
                    if (cal.timeInMillis <= startMs) {
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                } else {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return cal
        }
    }

    private fun parseClockTime(timeStr: String, dayPart: String?): Pair<Int, Int>? {
        val norm = SpokenNumberParser.normalizeToken(timeStr).trim()

        if (norm in listOf("نص الليل", "نصف الليل")) {
            return Pair(0, 0)
        }

        val dig = Regex("(\\d{1,2}):(\\d{2})").find(norm)
        if (dig != null) {
            val h = dig.groupValues[1].toInt()
            val m = dig.groupValues[2].toInt()
            return Pair(h, m)
        }

        var baseHour = 0
        var extraMin = 0

        val mHour = Regex("^(\\d+|واحد|واحده|اثنين|اثنان|اتنين|ثنتين|ثلاث|ثلاثه|تلات|تلاته|اربع|اربعه|خمس|خمسه|ست|سته|سبع|سبعه|ثمان|ثمانيه|تمان|تمانيه|تسع|تسعه|عشر|عشره|حداعش|طناعش)").find(norm)
        if (mHour != null) {
            val hToken = mHour.groupValues[1]
            baseHour = hToken.toIntOrNull() ?: when (hToken) {
                "واحد", "واحده" -> 1
                "اثنين", "اثنان", "اتنين", "ثنتين" -> 2
                "ثلاث", "ثلاثه", "تلات", "تلاته" -> 3
                "اربع", "اربعه" -> 4
                "خمس", "خمسه" -> 5
                "ست", "سته" -> 6
                "سبع", "سبعه" -> 7
                "ثمان", "ثمانيه", "تمان", "تمانيه" -> 8
                "تسع", "تسعه" -> 9
                "عشر", "عشره" -> 10
                "حداعش" -> 11
                "طناعش" -> 12
                else -> 0
            }
        }

        if (baseHour == 0) return null

        if (norm.contains("الا ربع") || norm.contains("إلا ربع")) {
            baseHour -= 1
            extraMin = 45
        } else if (norm.contains("الا ثلث") || norm.contains("إلا تلت")) {
            baseHour -= 1
            extraMin = 40
        } else if (norm.contains("ونص") || norm.contains("ونصف") || norm.contains("و نص") || norm.contains("و نصف")) {
            extraMin = 30
        } else if (norm.contains("وربع") || norm.contains("و ربع")) {
            extraMin = 15
        } else if (norm.contains("وثلث") || norm.contains("وتلت") || norm.contains("و ثلث") || norm.contains("و تلت")) {
            extraMin = 20
        } else if (norm.contains("وعشر") || norm.contains("و عشر")) {
            extraMin = 10
        } else if (norm.contains("و20") || norm.contains("و 20")) {
            extraMin = 20
        }

        if (dayPart != null) {
            val normDp = SpokenNumberParser.normalizeToken(dayPart)
            val normPmList = VoicePhraseRules.DAY_PARTS_PM.map { SpokenNumberParser.normalizeToken(it) }
            val normAmList = VoicePhraseRules.DAY_PARTS_AM.map { SpokenNumberParser.normalizeToken(it) }
            val isPM = normPmList.contains(normDp)
            val isAM = normAmList.contains(normDp)

            // Rule: بالليل / الليل with hour 1–4 means 01:00–04:00 (after midnight), not PM.
            // "نص الليل" stays 00:00. Hours 6–12 with بالليل stay PM.
            val isNightAfterMidnight = (normDp in listOf("بالليل", "الليل")) && (baseHour in 1..4)

            if (isNightAfterMidnight) {
                // Keep baseHour as 1..4 (01:00 - 04:00 AM after midnight)
            } else if (isPM && baseHour < 12) {
                baseHour += 12
            } else if (isAM && baseHour == 12) {
                baseHour = 0
            }
        }

        return Pair(baseHour, extraMin)
    }
}
