package com.example.core.voice

/**
 * Pure parser converting Arabic spoken numbers (MSA and Yemeni dialect) into [Double].
 * Handles standard Arabic words, Yemeni dialect numbers (حداعش، خمسمية، تلاف، ونص بعد الآلاف),
 * ASCII and Eastern Arabic digits, mixed phrases (15 ألف، 2 مليون), and ignores total-like words.
 */
object SpokenNumberParser {

    private const val EASTERN_DIGITS = "٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹"
    private const val ASCII_DIGITS = "01234567890123456789"

    fun toAsciiDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val idx = EASTERN_DIGITS.indexOf(ch)
            if (idx >= 0) {
                sb.append(ASCII_DIGITS[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Cleans number group separators in numeric digits:
     * - Thousands comma: "10,000" -> "10000"
     * - Arabic thousands separator: "10٬000" -> "10000"
     * - Thousands dot (when followed by exactly 3 digits): "10.000" -> "10000"
     * - Thousands space: "10 000" -> "10000"
     */
    fun cleanNumberSeparators(text: String): String {
        var s = toAsciiDigits(text)
        // Dot as thousands separator when followed by exactly 3 digits (e.g. 10.000, 1.000.000)
        s = s.replace(Regex("(?<=\\d)\\.(?=\\d{3}(?:\\D|$))"), "")
        // Arabic thousands separator (٬), comma (,), Arabic comma (،)
        s = s.replace(Regex("(?<=\\d)[,،٬](?=\\d{3}(?:\\D|$))"), "")
        s = s.replace(Regex("(?<=\\d)[,٬](?=\\d)"), "")
        // Space as thousands separator between digits
        s = s.replace(Regex("(?<=\\d)\\s+(?=\\d{3}(?:\\D|$))"), "")
        return s
    }

    /**
     * Normalizes text for number matching (strips diacritics, tatweel, standardizes alef, taa marbuta, yaa).
     */
    fun normalizeToken(text: String): String {
        var t = toAsciiDigits(text)
        t = t.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        t = t.replace("ـ", "")
        t = t.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        t = t.replace('ة', 'ه')
        t = t.replace('ى', 'ي')
        return t
    }

    private fun stripConjunctionWaw(token: String): String {
        if (token in listOf("واحد", "واحده", "وحدة", "وحدي")) return token
        return if (token.startsWith("و") && token.length > 2) token.drop(1) else token
    }

    private val UNIT_WORDS = mapOf(
        "صفر" to 0.0,
        "واحد" to 1.0, "واحده" to 1.0, "وحدة" to 1.0,
        "اثنين" to 2.0, "اثنان" to 2.0, "اتنين" to 2.0, "ثنتين" to 2.0, "اثنتين" to 2.0,
        "ثلاثه" to 3.0, "تلاته" to 3.0, "ثلاث" to 3.0, "تلات" to 3.0,
        "اربعه" to 4.0, "اربع" to 4.0,
        "خمسه" to 5.0, "خمس" to 5.0,
        "سته" to 6.0, "ست" to 6.0,
        "سبعه" to 7.0, "سبع" to 7.0,
        "ثمانيه" to 8.0, "تمانيه" to 8.0, "ثمان" to 8.0, "تمان" to 8.0,
        "تسعه" to 9.0, "تسع" to 9.0,
        "عشره" to 10.0, "عشر" to 10.0,
        // Teens (MSA and Yemeni dialect)
        "حداعش" to 11.0, "احداعش" to 11.0, "احدعشر" to 11.0, "احداعشر" to 11.0,
        "اطناعش" to 12.0, "طناعش" to 12.0, "اثناعشر" to 12.0, "اثنيعشر" to 12.0,
        "تلطاعش" to 13.0, "ثلاثطعش" to 13.0, "تلاتاشر" to 13.0, "ثلاثتاشر" to 13.0,
        "اربعطعش" to 14.0, "ارباطاعش" to 14.0, "اربعتاشر" to 14.0,
        "خمسطعش" to 15.0, "خموسطاعش" to 15.0, "خمستاشر" to 15.0,
        "ستطعش" to 16.0, "سطاعش" to 16.0, "ستاشر" to 16.0,
        "سبعطعش" to 17.0, "سبعتاشر" to 17.0,
        "ثمنطعش" to 18.0, "تمنطعش" to 18.0, "تمانتاشر" to 18.0,
        "تسعطعش" to 19.0, "تسعتاشر" to 19.0
    )

    private val TENS_WORDS = mapOf(
        "عشرين" to 20.0, "عشرون" to 20.0,
        "ثلاثين" to 30.0, "ثلاثون" to 30.0, "تلاتين" to 30.0,
        "اربعين" to 40.0, "اربعون" to 40.0,
        "خمسين" to 50.0, "خمسون" to 50.0,
        "ستين" to 60.0, "ستون" to 60.0,
        "سبعين" to 70.0, "سبعون" to 70.0,
        "ثمانين" to 80.0, "تمانين" to 80.0, "ثمانون" to 80.0,
        "تسعين" to 90.0, "تسعون" to 90.0
    )

    private val HUNDREDS_WORDS = mapOf(
        "ميه" to 100.0, "مئه" to 100.0, "مائه" to 100.0, "ميا" to 100.0,
        "ميتين" to 200.0, "مئتين" to 200.0, "مائتين" to 200.0, "مئتان" to 200.0, "مائتان" to 200.0,
        "تلتميه" to 300.0, "تلاتميه" to 300.0, "ثلاثميه" to 300.0, "ثلاثمئه" to 300.0, "ثلاثمائه" to 300.0,
        "ربعميه" to 400.0, "اربعميه" to 400.0, "اربعمئه" to 400.0, "اربعمائه" to 400.0,
        "خمسميه" to 500.0, "خمسمئه" to 500.0, "خمسمائه" to 500.0,
        "ستميه" to 600.0, "ستمئه" to 600.0, "ستمائه" to 600.0,
        "سبعميه" to 700.0, "سبعمئه" to 700.0, "سبعمائه" to 700.0,
        "تمنميه" to 800.0, "ثمانميه" to 800.0, "ثمانمئه" to 800.0, "ثمانمائه" to 800.0,
        "تسعميه" to 900.0, "تسعمئه" to 900.0, "تسعمائه" to 900.0
    )

    /**
     * Parses a spoken Arabic number string into a [Double] amount, or null if no number was recognized.
     */
    fun parseSpokenNumber(text: String): Double? {
        val cleaned = cleanNumberSeparators(text)
        var norm = normalizeToken(cleaned).trim()
        if (norm.isBlank()) return null

        // Strip non-decimal punctuation
        norm = norm.replace(Regex("[,،]"), " ")
        norm = norm.replace(Regex("(?<!\\d)\\.|\\.(?!\\d)"), " ")
        norm = norm.replace(Regex("\\b(ريال|ريالا|ريالات|ر\\.ي|ريالاً)\\b"), " ")
        norm = norm.replace(Regex("^(المجموع|الاجمالي|مجموع|اجمالي)\\s*"), " ")
        norm = norm.replace(Regex("\\s+"), " ").trim()
        if (norm.isBlank()) return null

        // Direct raw digits check (e.g. "5000", "5000.0")
        val cleanDigits = norm.replace(" ", "")
        cleanDigits.toDoubleOrNull()?.let { return it }

        // Common dialect idioms
        when (norm) {
            "ربع مليون", "ربع المليون" -> return 250000.0
            "نص مليون", "نصف مليون", "نص المليون", "نصف المليون" -> return 500000.0
            "ثلاثه ارباع مليون", "ثلاثه أرباع مليون", "ثلاثة ارباع مليون" -> return 750000.0
            "مليون ونص", "مليون ونصف" -> return 1500000.0
            "مليونين ونص", "مليونين ونصف" -> return 2500000.0
            "الف ونص", "ألف ونص", "الف ونصف", "ألف ونصف" -> return 1500.0
            "الفين ونص", "ألفين ونص", "الفين ونصف", "ألفين ونصف" -> return 2500.0
        }

        val rawTokens = norm.split(' ').filter { it.isNotBlank() }
        if (rawTokens.isEmpty()) return null

        // Pre-combine compound tokens: e.g. "احد" + "عشر" -> "احدعشر", "ثلاث" + "ميه" -> "ثلاثميه"
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < rawTokens.size) {
            val t = stripConjunctionWaw(rawTokens[i])
            if (t.isBlank()) {
                i++
                continue
            }
            if (i + 1 < rawTokens.size) {
                val tNext = stripConjunctionWaw(rawTokens[i + 1])
                if (t in listOf("احد", "اثنا", "اثني", "ثلاث", "ثلاثه", "تلات", "تلاته", "اربع", "اربعه", "خمس", "خمسه", "ست", "سته", "سبع", "سبعه", "ثمان", "ثمانيه", "تمان", "تمانيه", "تسع", "تسعه") &&
                    tNext in listOf("عشر", "عشره")
                ) {
                    val combo = t + tNext
                    if (UNIT_WORDS.containsKey(combo)) {
                        tokens.add(combo)
                        i += 2
                        continue
                    }
                }
                if (t in listOf("ثلاث", "تلات", "اربع", "خمس", "ست", "سبع", "ثمان", "تمان", "تسع") &&
                    tNext in listOf("ميه", "مئه", "مائه")
                ) {
                    val combo = t + tNext
                    if (HUNDREDS_WORDS.containsKey(combo)) {
                        tokens.add(combo)
                        i += 2
                        continue
                    }
                }
            }
            tokens.add(t)
            i++
        }

        var total = 0.0
        var currentGroup = 0.0
        var hasNumber = false

        var j = 0
        while (j < tokens.size) {
            val tok = tokens[j]

            // Check if token is raw digits
            val numVal = tok.toDoubleOrNull()
            if (numVal != null) {
                currentGroup += numVal
                hasNumber = true
                j++
                continue
            }

            // Millions
            if (tok in listOf("مليون", "ملايين", "مليونا", "مليوناً")) {
                val mult = if (currentGroup > 0.0) currentGroup else 1.0
                total += mult * 1000000.0
                currentGroup = 0.0
                hasNumber = true
                j++
                continue
            }
            if (tok in listOf("مليونين", "مليونان")) {
                total += 2000000.0
                currentGroup = 0.0
                hasNumber = true
                j++
                continue
            }

            // Thousands
            if (tok in listOf("الف", "الاف", "تلاف", "الفا", "ألف", "آلاف", "ألفاً")) {
                val mult = if (currentGroup > 0.0) currentGroup else 1.0
                total += mult * 1000.0
                currentGroup = 0.0
                hasNumber = true

                // Check Yemeni dialect "+ ونص" / "+ وربع" right after thousand (e.g. "خمسة آلاف ونص" -> +500)
                if (j + 1 < tokens.size && tokens[j + 1] in listOf("نص", "نصف")) {
                    total += 500.0
                    j += 2
                    continue
                }
                if (j + 1 < tokens.size && tokens[j + 1] in listOf("ربع")) {
                    total += 250.0
                    j += 2
                    continue
                }
                j++
                continue
            }
            if (tok in listOf("الفين", "الفان", "ألفين", "ألفان")) {
                total += 2000.0
                currentGroup = 0.0
                hasNumber = true
                if (j + 1 < tokens.size && tokens[j + 1] in listOf("نص", "نصف")) {
                    total += 500.0
                    j += 2
                    continue
                }
                if (j + 1 < tokens.size && tokens[j + 1] in listOf("ربع")) {
                    total += 250.0
                    j += 2
                    continue
                }
                j++
                continue
            }

            // Hundreds
            val hVal = HUNDREDS_WORDS[tok]
            if (hVal != null) {
                currentGroup += hVal
                hasNumber = true
                j++
                continue
            }

            // Tens
            val tVal = TENS_WORDS[tok]
            if (tVal != null) {
                currentGroup += tVal
                hasNumber = true
                j++
                continue
            }

            // Units / Teens
            val uVal = UNIT_WORDS[tok]
            if (uVal != null) {
                currentGroup += uVal
                hasNumber = true
                j++
                continue
            }

            // Standalone "نص" fraction
            if (tok in listOf("نص", "نصف") && (total > 0.0 || currentGroup > 0.0)) {
                if (total >= 1000000.0 && currentGroup == 0.0) {
                    total += 500000.0
                } else if (total >= 1000.0 && currentGroup == 0.0) {
                    total += 500.0
                } else {
                    currentGroup += 0.5
                }
                hasNumber = true
                j++
                continue
            }

            // If a token is not part of number vocabulary and not digits, return null
            // to ensure non-number strings (e.g. customer names) are not parsed as numbers
            return null
        }

        val result = total + currentGroup
        return if (hasNumber) result else null
    }
}
