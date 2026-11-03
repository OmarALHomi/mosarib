package com.example.core.speech

/**
 * Normalizes Arabic text specifically for Text-To-Speech (TTS) pronunciation.
 * Written text on UI cards and PDFs remains untouched; only text passed to
 * [MosaribSpeaker.speak] or [MosaribSpeaker.synthesizeToFile] goes through [forSpeech].
 */
object SpeechPronunciation {

    /**
     * Sample text containing all dictionary words and test amounts (750 / 3,500 / 14,000 / 125,000 / 2,000,000).
     * Used by the "تجربة النطق" button in Settings and verified by unit tests.
     */
    const val SAMPLE_PRONUNCIATION_TEXT =
        "تجربة نطق تطبيق المُسَرِّب: كَشْف حِسَاب، الرَّصِيد عَلَيْه مِئَة وخمسة وعشرون ألف ريال، ولَهُ سَبْع مِئَة وخمسون ريال. سَنَد قَبْض بمبلغ أربعة عشر ألف ريال، وسَنَد صَرْف، وخَصْم. سَقْي وسَقْيَة، شِرَاء سَاعَة وسَاعَتَان من بِئْر، دَوْرَة مِضَخَّة دون هَدْر مَدْفُوع ومُسَدَّد تَقْرِيبًا. المَدْفُوع ثلاثة آلاف وخمس مِئَة ريال والبَاقِي مليونين ريال خلال خمسين دَقِيقَة."

    // Base root dictionary mapping un-vocalized words to their properly vocalized forms.
    // Pairs: (Word, VocalizedForm, isSunLetter: Boolean)
    private val BASE_DICTIONARY = listOf(
        Triple("سقي", "سَقْي", true),
        Triple("سقية", "سَقْيَة", true),
        Triple("سند", "سَنَد", true),
        Triple("قبض", "قَبْض", false),
        Triple("صرف", "صَرْف", true),
        Triple("خصم", "خَصْم", false),
        Triple("مسدد", "مُسَدَّد", false),
        Triple("عليه", "عَلَيْه", false),
        Triple("له", "لَهُ", false),
        Triple("عليك", "عَلَيْكَ", false),
        Triple("لك", "لَكَ", false),
        Triple("عليكم", "عَلَيْكُم", false),
        Triple("باقي", "بَاقِي", false),
        Triple("الباقي", "البَاقِي", false),
        Triple("مدفوع", "مَدْفُوع", false),
        Triple("المدفوع", "المَدْفُوع", false),
        Triple("شراء", "شِرَاء", true),
        Triple("بئر", "بِئْر", false),
        Triple("كشف", "كَشْف", false),
        Triple("حساب", "حِسَاب", false),
        Triple("رصيد", "رَصِيد", true),
        Triple("الرصيد", "الرَّصِيد", true),
        Triple("مضخة", "مِضَخَّة", false),
        Triple("هدر", "هَدْر", false),
        Triple("دورة", "دَوْرَة", true),
        Triple("مسرب", "مُسَرِّب", false),
        Triple("المسرب", "المُسَرِّب", false),
        Triple("تقريبا", "تَقْرِيبًا", false),
        Triple("تقريباً", "تَقْرِيبًا", false),
        Triple("ساعة", "سَاعَة", true),
        Triple("ساعتان", "سَاعَتَان", true),
        Triple("ساعتين", "سَاعَتَيْن", true),
        Triple("ساعات", "سَاعَات", true),
        Triple("دقيقة", "دَقِيقَة", true),
        Triple("دقيقتان", "دَقِيقَتَان", true),
        Triple("دقيقتين", "دَقِيقَتَيْن", true),
        Triple("دقائق", "دَقَائِق", true),
        Triple("ريال", "رِيَال", true),
        Triple("ألف", "أَلْف", false),
        Triple("ألفان", "أَلْفَيْن", false),
        Triple("ألفين", "أَلْفَيْن", false),
        Triple("آلاف", "آلَاف", false),
        Triple("مليون", "مِلْيُون", false),
        Triple("مليونان", "مِلْيُونَيْن", false),
        Triple("مليونين", "مِلْيُونَيْن", false),
        Triple("ملايين", "مَلَايِين", false),
        // Tens in the spoken form ("سبعين ألف", not "سبعون ألف")
        Triple("عشرون", "عِشْرِين", false),
        Triple("ثلاثون", "ثَلَاثِين", false),
        Triple("أربعون", "أَرْبَعِين", false),
        Triple("خمسون", "خَمْسِين", false),
        Triple("ستون", "سِتِّين", false),
        Triple("سبعون", "سَبْعِين", false),
        Triple("ثمانون", "ثَمَانِين", false),
        Triple("تسعون", "تِسْعِين", false),
        // Task 029 additions: preview words, dictionary normalization, and مِئَة
        Triple("مئة", "مِئَة", false),
        Triple("مائة", "مِئَة", false),
        Triple("تجربة", "تَجْرِبَة", true),
        Triple("قراءة", "قِرَاءَة", false),
        Triple("القراءة", "القِرَاءَة", false),
        Triple("صوت", "صَوْت", true),
        Triple("الصوت", "الصَّوْت", true),
        Triple("عربي", "عَرَبِي", false),
        Triple("العربي", "العَرَبِي", false),
        Triple("تطبيق", "تَطْبِيق", true),
        Triple("التطبيق", "التَّطْبِيق", true),
        Triple("مرحبا", "مَرْحَبًا", false),
        Triple("مرحباً", "مَرْحَبًا", false),
        Triple("هذه", "هَذِهِ", false),
        Triple("هذا", "هَذَا", false)
    )

    // Lookup map populated with base words and all supported prefix variants:
    // Prefixes: و / ب / ل / ال / فـ (and compounds: وال / فال / بال / لل / ول / فل / بل)
    val DICTIONARY: Map<String, String> = buildMap {
        for ((base, vocalized, isSun) in BASE_DICTIONARY) {
            put(base, vocalized)

            if (base.startsWith("ال")) {
                val subBase = base.substring(2)
                val subVoc = vocalized.substring(2)

                put("و$base", "وَ$vocalized")
                put("ف$base", "فَ$vocalized")
                put("ب$base", "بِ$vocalized")
                put("لل$subBase", "لِل$subVoc")
                put("ولل$subBase", "وَلِل$subVoc")
            } else {
                put("و$base", "وَ$vocalized")
                put("ف$base", "فَ$vocalized")
                put("ب$base", "بِ$vocalized")
                put("ل$base", "لِ$vocalized")

                // With definite article 'ال'
                val alVoc = if (isSun) {
                    val firstChar = vocalized.firstOrNull() ?: ' '
                    val rest = vocalized.drop(1)
                    "ال$firstChar\u0651$rest" // Add Shaddah to sun letter
                } else {
                    "ال$vocalized"
                }
                val alBase = "ال$base"
                put(alBase, alVoc)

                put("و$alBase", "وَ$alVoc")
                put("ف$alBase", "فَ$alVoc")
                put("ب$alBase", "بِ$alVoc")
                val subVoc = alVoc.substring(2)
                put("لل$base", "لِل$subVoc")
                put("ولل$base", "وَلِل$subVoc")
            }
        }
    }

    private val WORD_REGEX = Regex("[\\p{L}\\p{M}]+")
    private val DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670]")
    private val PHRASE_DELIMITERS = setOf('،', ',', '.', ':', '؛', ';', '?', '؟', '!', '\n', '\r')

    /**
     * Strips Arabic diacritics and shaddah so words carrying marks (e.g. "المُسَرِّب", "مرحباً")
     * correctly match their dictionary root key ("المسرب", "مرحبا").
     */
    internal fun stripDiacritics(word: String): String = DIACRITICS_REGEX.replace(word, "")

    internal fun lookupWord(rawWord: String): String {
        val clean = stripDiacritics(rawWord)
        return DICTIONARY[clean] ?: DICTIONARY[rawWord] ?: rawWord
    }

    /**
     * Pre-processes Arabic text before sending it to the TTS engine:
     * 1. Normalizes number/currency words (no tanween pause forms: "ألف", split "مِئَة").
     * 2. Replaces currency abbreviations ("ر.ي", "ريالاً" -> "ريال").
     * 3. Drops formal padding ("فقط", "لا غير").
     * 4. Applies diacritics dictionary to application domain words.
     *
     * @param connected When `true`, pause ending (sukun / ة -> ه) is placed ONLY on the last word
     * of each phrase (before punctuation or end of text), allowing smooth natural speech flow.
     * When `false` (today's behavior), pause ending is added to every word.
     */
    fun forSpeech(text: String, connected: Boolean = false): String {
        if (text.isBlank()) return text

        var processed = text
            // Currency and tanween normalization
            .replace("ر.ي", "ريال")
            .replace("ريالاً", "ريال")
            .replace("ريالات", "ريال")
            // Whole word only: a plain "ألفا" replace also cut "ألفان" into "ألفن"
            .replace(Regex("ألفاً|ألفا(?![\\p{L}\\p{M}])"), "ألف")
            // Drop formal invoice padding
            .replace("فقط لا غير", "")
            .replace("فقط لاغير", "")
            .replace("لا غير", "")
            .replace("لاغير", "")
            .replace(Regex("\\bفقط\\b"), "")
            // Normalize hundreds spelling with fatha on the hamza ("مِئَة")
            .replace("ثلاثمائة", "ثلاث مِئَة")
            .replace("ثلاثمئة", "ثلاث مِئَة")
            .replace("أربعمائة", "أربع مِئَة")
            .replace("أربعمئة", "أربع مِئَة")
            .replace("خمسمائة", "خمس مِئَة")
            .replace("خمسمئة", "خمس مِئَة")
            .replace("ستمائة", "ست مِئَة")
            .replace("ستمئة", "ست مِئَة")
            .replace("سبعمائة", "سبع مِئَة")
            .replace("سبعمئة", "سبع مِئَة")
            .replace("ثمانمائة", "ثمان مِئَة")
            .replace("ثمانمئة", "ثمان مِئَة")
            .replace("تسعمائة", "تسع مِئَة")
            .replace("تسعمئة", "تسع مِئَة")
            .replace("مِئتان", "مِئَتَيْن")
            .replace("مائتان", "مِئَتَيْن")
            .replace("مائتين", "مِئَتَيْن")
            .replace("مئتان", "مِئَتَيْن")
            .replace("مئتين", "مِئَتَيْن")
            .replace("مائة", "مِئَة")
            .replace("مئة", "مِئَة")
            // Idafa: a number before ألف / آلاف / مليون keeps its ة as "ت" ("مِئَتْ ألف", "خمستْ آلاف")
            .replace(IDAFA_TAA_REGEX, "ت$SUKUN")

        val matches = WORD_REGEX.findAll(processed).toList()
        if (matches.isEmpty()) return processed

        val sb = StringBuilder()
        var lastPos = 0
        for (i in matches.indices) {
            val m = matches[i]
            if (m.range.first > lastPos) {
                sb.append(processed.substring(lastPos, m.range.first))
            }
            val word = m.value
            val nextStart = if (i + 1 < matches.size) matches[i + 1].range.first else processed.length
            val between = processed.substring(m.range.last + 1, nextStart)
            val isLastInPhrase = (i == matches.lastIndex) || between.any { it in PHRASE_DELIMITERS }

            val resolved = lookupWord(word)
            val finalWord = if (!connected || isLastInPhrase) {
                withPauseEnding(resolved)
            } else {
                resolved
            }
            sb.append(finalWord)
            lastPos = m.range.last + 1
        }
        if (lastPos < processed.length) {
            sb.append(processed.substring(lastPos))
        }

        // Clean redundant whitespace
        return sb.toString().replace(Regex("[ \\t]+"), " ").trim()
    }

    private const val SUKUN = 'ْ'
    // Endings that are already a vowel; a sukun on them changes nothing or sounds wrong.
    private val VOWEL_ENDINGS = setOf('ا', 'ى', 'آ')
    private val IDAFA_TAA_REGEX = Regex("ة(?=\\s+(?:ألف|آلاف|مليون|ملايين)(?![\\p{L}\\p{M}]))")

    /**
     * Waqf: puts a sukun on the last letter of an Arabic word that has no ending mark yet, so the
     * engine stops adding case endings and tanween ("قبضُ" → "قبضْ", "ريالٍ" → "ريالْ").
     * A final ة is read as "ه" ("دقيقة" → "دقيقه", "مِئَة وخمسون" → "مِئه وخمسين").
     * Words that already end with a mark ("عَلَيْكَ", "لَهُ", "مِئَتْ") keep it.
     */
    internal fun withPauseEnding(word: String): String {
        if (word.length < 2) return word
        val last = word.last()
        if (last == 'ة') return word.dropLast(1) + "ه$SUKUN"
        if (last !in 'ء'..'ي' || last in VOWEL_ENDINGS) return word
        return word + SUKUN
    }
}
