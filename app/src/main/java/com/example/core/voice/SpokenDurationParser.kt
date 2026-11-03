package com.example.core.voice

/**
 * Pure parser converting spoken Arabic duration expressions to total minutes (Int).
 * Handles hours, dual ("ساعتين"), word and digit numbers ("3 ساعات"),
 * fractions ("ونص", "وربع", "الا ربع", "وثلث", "وتلت"), and explicit minute expressions.
 * Also handles bare hour + fraction ("ثلاث وربع", "ثلاث ونص") in duration contexts.
 */
object SpokenDurationParser {

    private val HOUR_WORDS_MAP = mapOf(
        "واحد" to 1, "واحده" to 1, "وحدة" to 1,
        "اثنين" to 2, "اثنان" to 2, "اتنين" to 2, "ثنتين" to 2,
        "ثلاثه" to 3, "تلاته" to 3, "ثلاث" to 3, "تلات" to 3,
        "اربعه" to 4, "اربع" to 4,
        "خمسه" to 5, "خمس" to 5,
        "سته" to 6, "ست" to 6,
        "سبعه" to 7, "سبع" to 7,
        "ثمانيه" to 8, "تمانيه" to 8, "ثمان" to 8, "تمان" to 8,
        "تسعه" to 9, "تسع" to 9,
        "عشره" to 10, "عشر" to 10,
        "حداعش" to 11, "احداعش" to 11,
        "طناعش" to 12, "اطناعش" to 12, "اثناعش" to 12
    )

    private val MINUTE_WORDS_MAP = mapOf(
        "خمس" to 5, "خمسه" to 5,
        "عشر" to 10, "عشره" to 10,
        "خمسطعش" to 15, "خمسة عشر" to 15,
        "عشرين" to 20, "عشرون" to 20,
        "ثلاثين" to 30, "ثلاثون" to 30,
        "خمس واربعين" to 45, "خمسه واربعين" to 45, "خمسة واربعون" to 45,
        "خمسين" to 50, "خمسون" to 50
    )

    /**
     * Parses a duration string in Arabic into minutes.
     * Returns null if no duration pattern was found.
     */
    fun parseSpokenDuration(text: String): Int? {
        var norm = SpokenNumberParser.normalizeToken(text).trim()
        if (norm.isBlank()) return null

        // 1. Digital time notation: e.g. "2:30", "02:30", "1:45"
        val digitalMatch = Regex("\\b(\\d{1,2}):(\\d{2})\\b").find(norm)
        if (digitalMatch != null) {
            val h = digitalMatch.groupValues[1].toIntOrNull() ?: 0
            val m = digitalMatch.groupValues[2].toIntOrNull() ?: 0
            return h * 60 + m
        }

        var hours = 0
        var hasHours = false

        // 2. Check dual and hour patterns FIRST before standalone fractions
        if (Regex("\\b(ساعتين|ساعتان)\\b").containsMatchIn(norm)) {
            hours = 2
            hasHours = true
        } else if (Regex("\\b(ساعه|ساعات)\\b").containsMatchIn(norm)) {
            val m = Regex("(\\d+|واحد|اثنين|اثنان|اتنين|ثلاثه|تلاته|ثلاث|تلات|اربعه|اربع|خمسه|خمس|سته|ست|سبعه|سبع|ثمانيه|تمانيه|ثمان|تمان|تسعه|تسع|عشره|عشر)\\s+(ساعات|ساعه)").find(norm)
            if (m != null) {
                val numStr = m.groupValues[1]
                val digitNum = numStr.toIntOrNull()
                hours = digitNum ?: HOUR_WORDS_MAP[numStr] ?: 1
                hasHours = true
            } else if (Regex("\\bساعه\\s+واحده\\b").containsMatchIn(norm)) {
                hours = 1
                hasHours = true
            } else if (Regex("\\bساعه\\b").containsMatchIn(norm) && !Regex("\\b(نص|نصف|ربع|ثلث|تلت)\\s+ساعه\\b").containsMatchIn(norm)) {
                hours = 1
                hasHours = true
            }
        } else {
            // Bare hour word followed by fraction (e.g. "ثلاث وربع", "ثلاث ونص", "ساعتين الا ربع")
            val bareHourMatch = Regex("^(واحد|اثنين|اثنان|اتنين|ثنتين|ثلاثه|تلاته|ثلاث|تلات|اربعه|اربع|خمسه|خمس|سته|ست|سبعه|سبع|ثمانيه|تمانيه|ثمان|تمان|تسعه|تسع|عشره|عشر)\\s+(و\\s*(?:نص|نصف|ربع|ثلث|تلت)|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت))").find(norm)
            if (bareHourMatch != null) {
                val hStr = bareHourMatch.groupValues[1]
                hours = HOUR_WORDS_MAP[hStr] ?: 0
                hasHours = true
            }
        }

        if (hasHours) {
            var extraMinutes = 0
            if (Regex("\\b(الا|إلا)\\s+ربع\\b").containsMatchIn(norm)) {
                extraMinutes -= 15
            } else if (Regex("\\b(الا|إلا)\\s+(ثلث|تلت)\\b").containsMatchIn(norm)) {
                extraMinutes -= 20
            } else if (Regex("\\bو\\s*(نص|نصف)").containsMatchIn(norm)) {
                // Accepts "ونص", "و نص", "ونصف", "و نصف", "ونص ساعه", "و نص ساعه"
                extraMinutes += 30
            } else if (Regex("\\bو\\s*ربع\\b").containsMatchIn(norm)) {
                extraMinutes += 15
            } else if (Regex("\\bو\\s*(ثلث|تلت)\\b").containsMatchIn(norm)) {
                extraMinutes += 20
            } else {
                // Check "و 30", "و30", "و 30 دقيقه", "و X دقيقة", "و X دقائق"
                val mMin = Regex("و\\s*(\\d+|عشر|عشره|خمس|خمسه|عشرين|ثلاثين|اربعين|خمسين)(?:\\s*(دقايق|دقائق|دقيقه))?").find(norm)
                if (mMin != null) {
                    val numS = mMin.groupValues[1]
                    val dVal = numS.toIntOrNull()
                    extraMinutes += dVal ?: MINUTE_WORDS_MAP[numS] ?: 0
                }
            }
            return maxOf(0, hours * 60 + extraMinutes)
        }

        // 3. Standalone fractions of an hour (only when no explicit hours were found)
        if (Regex("\\b(نص|نصف)\\s+ساعه\\b").containsMatchIn(norm)) return 30
        if (Regex("\\b(ربع)\\s+ساعه\\b").containsMatchIn(norm)) return 15
        if (Regex("\\b(ثلث|تلت)\\s+ساعه\\b").containsMatchIn(norm)) return 20
        if (Regex("\\bساعه\\s+(الا|إلا)\\s+ربع\\b").containsMatchIn(norm)) return 45
        if (Regex("\\bساعه\\s+(الا|إلا)\\s+(ثلث|تلت)\\b").containsMatchIn(norm)) return 40
        if (Regex("\\b(الا|إلا)\\s+ربع\\b").containsMatchIn(norm)) return 45

        // 4. Standalone minutes (e.g. "45 دقيقة", "عشر دقائق", "دقيقتين")
        val mOnly = Regex("(\\d+|عشر|عشره|خمس|خمسه|خمسطعش|عشرين|ثلاثين|خمس واربعين|خمسه واربعين|خمسين)\\s*(دقايق|دقائق|دقيقه)").find(norm)
        if (mOnly != null) {
            val numS = mOnly.groupValues[1]
            val dVal = numS.toIntOrNull()
            return dVal ?: MINUTE_WORDS_MAP[numS]
        }

        if (Regex("\\b(دقيقتين|دقيقتان)\\b").containsMatchIn(norm)) return 2
        if (Regex("\\b(دقيقه\\s+واحده|دقيقة\\s+واحدة|دقيقه|دقيقة)\\b").containsMatchIn(norm)) return 1

        return null
    }

    /**
     * Extracts the range (startIndex to endIndex) of the duration match in the original text,
     * allowing the command parser to isolate and remove it from remaining text.
     * Compound hour patterns (e.g. "ساعتين ونص ساعه") MUST be evaluated before standalone fractions ("نص ساعه").
     */
    fun findDurationSpan(text: String): IntRange? {
        val norm = SpokenNumberParser.normalizeToken(text)
        val patterns = listOf(
            Regex("\\b\\d{1,2}:\\d{2}\\b"),
            // Compound hours with fractions / minutes must come before standalone fractions
            Regex("(?U)\\b(ساعتين|ساعتان)(?:\\s+(?:و\\s*(?:نص|نصف|ربع|ثلث|تلت)(?:\\s*ساعه)?|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت)|و\\s*\\d{1,2}(?:\\s*(?:دقيقه|دقيقة|دقايق|دقائق))?))\\b"),
            Regex("(?U)(\\d+|واحد|اثنين|اثنان|اتنين|ثلاثه|تلاته|ثلاث|تلات|اربعه|اربع|خمسه|خمس|سته|ست|سبعه|سبع|ثمانيه|تمانيه|ثمان|تمان|تسعه|تسع|عشره|عشر)\\s+(ساعات|ساعه)(?:\\s+(?:و\\s*(?:نص|نصف|ربع|ثلث|تلت)(?:\\s*ساعه)?|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت)|و\\s*\\d{1,2}(?:\\s*(?:دقيقه|دقيقة|دقايق|دقائق))?))"),
            Regex("(?U)\\bساعه(?:\\s+(?:و\\s*(?:نص|نصف|ربع|ثلث|تلت)(?:\\s*ساعه)?|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت)|و\\s*\\d{1,2}(?:\\s*(?:دقيقه|دقيقة|دقايق|دقائق))?))\\b"),
            Regex("(?U)\\bساعه\\s+(الا|إلا)\\s+(ربع|ثلث|تلت)\\b"),
            Regex("(?U)\\bساعه\\s+واحده\\b"),
            Regex("(?U)\\b(ساعتين|ساعتان)\\b"),
            Regex("(?U)(\\d+|واحد|اثنين|اثنان|اتنين|ثلاثه|تلاته|ثلاث|تلات|اربعه|اربع|خمسه|خمس|سته|ست|سبعه|سبع|ثمانيه|تمانيه|ثمان|تمان|تسعه|تسع|عشره|عشر)\\s+(ساعات|ساعه)"),
            Regex("(?U)\\bساعه\\b"),
            // Bare hour + fraction followed by rate marker, paid marker, "مع", or digits (e.g. "ثلاث وربع مع احمد سعيد من 10 الف")
            Regex("(?U)\\b(واحد|اثنين|اثنان|اتنين|ثنتين|ثلاثه|تلاته|ثلاث|تلات|اربعه|اربع|خمسه|خمس|سته|ست|سبعه|سبع|ثمانيه|تمانيه|ثمان|تمان|تسعه|تسع|عشره|عشر)\\s+(?:و\\s*(?:نص|نصف|ربع|ثلث|تلت)|(?:الا|إلا)\\s*(?:ربع|ثلث|تلت))(?=\\s+(?:مع|من|بسعر|سعر|الساعه|سلم|مسلم|دفع|وادي|ادى|\\d)|$)"),
            // Standalone fractions of an hour
            Regex("(?U)\\b(نص|نصف)\\s+ساعه\\b"),
            Regex("(?U)\\b(ربع)\\s+ساعه\\b"),
            Regex("(?U)\\b(ثلث|تلت)\\s+ساعه\\b"),
            Regex("(?U)\\b(الا|إلا)\\s+ربع\\b"),
            // Standalone minutes
            Regex("(?U)(\\d+|عشر|عشره|خمس|خمسه|خمسطعش|عشرين|ثلاثين|خمس واربعين|خمسه واربعين|خمسين)\\s*(دقايق|دقائق|دقيقه)"),
            Regex("(?U)\\b(دقيقتين|دقيقتان)\\b"),
            Regex("(?U)\\b(دقيقه|دقيقة)\\b")
        )

        for (p in patterns) {
            val m = p.find(norm)
            if (m != null) {
                return m.range
            }
        }
        return null
    }
}
