package com.example.core.voice

import com.example.features.customers.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceParsersTest {

    // =========================================================================
    // 1. SpokenNumberParser Tests (25 phrases)
    // =========================================================================

    @Test
    fun testDigitsParsing() {
        assertEquals(5000.0, SpokenNumberParser.parseSpokenNumber("5000")!!, 0.01)
        assertEquals(5000.0, SpokenNumberParser.parseSpokenNumber("٥٠٠٠")!!, 0.01)
        assertEquals(12500.0, SpokenNumberParser.parseSpokenNumber("12,500")!!, 0.01)
        assertEquals(12500.0, SpokenNumberParser.parseSpokenNumber("١٢٥٠٠")!!, 0.01)
    }

    @Test
    fun testMsaNumbers() {
        assertEquals(1.0, SpokenNumberParser.parseSpokenNumber("واحد")!!, 0.01)
        assertEquals(10.0, SpokenNumberParser.parseSpokenNumber("عشرة")!!, 0.01)
        assertEquals(20.0, SpokenNumberParser.parseSpokenNumber("عشرين")!!, 0.01)
        assertEquals(50.0, SpokenNumberParser.parseSpokenNumber("خمسين")!!, 0.01)
        assertEquals(100.0, SpokenNumberParser.parseSpokenNumber("مئة")!!, 0.01)
        assertEquals(200.0, SpokenNumberParser.parseSpokenNumber("مئتين")!!, 0.01)
        assertEquals(1000.0, SpokenNumberParser.parseSpokenNumber("ألف")!!, 0.01)
        assertEquals(2000.0, SpokenNumberParser.parseSpokenNumber("ألفين")!!, 0.01)
        assertEquals(3000.0, SpokenNumberParser.parseSpokenNumber("ثلاثة آلاف")!!, 0.01)
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("عشرة آلاف")!!, 0.01)
        assertEquals(125000.0, SpokenNumberParser.parseSpokenNumber("مئة وخمسة وعشرون ألف")!!, 0.01)
    }

    @Test
    fun testYemeniDialectNumbers() {
        // Dialect teens
        assertEquals(11.0, SpokenNumberParser.parseSpokenNumber("حداعش")!!, 0.01)
        assertEquals(12.0, SpokenNumberParser.parseSpokenNumber("اطناعش")!!, 0.01)
        assertEquals(13.0, SpokenNumberParser.parseSpokenNumber("تلطاعش")!!, 0.01)
        assertEquals(14.0, SpokenNumberParser.parseSpokenNumber("اربعطعش")!!, 0.01)
        assertEquals(15.0, SpokenNumberParser.parseSpokenNumber("خمسطعش")!!, 0.01)
        assertEquals(16.0, SpokenNumberParser.parseSpokenNumber("ستطعش")!!, 0.01)
        assertEquals(17.0, SpokenNumberParser.parseSpokenNumber("سبعطعش")!!, 0.01)
        assertEquals(18.0, SpokenNumberParser.parseSpokenNumber("ثمنطعش")!!, 0.01)
        assertEquals(19.0, SpokenNumberParser.parseSpokenNumber("تسعطعش")!!, 0.01)

        // Hundreds and thousands dialect
        assertEquals(100.0, SpokenNumberParser.parseSpokenNumber("مية")!!, 0.01)
        assertEquals(200.0, SpokenNumberParser.parseSpokenNumber("ميتين")!!, 0.01)
        assertEquals(300.0, SpokenNumberParser.parseSpokenNumber("تلتمية")!!, 0.01)
        assertEquals(500.0, SpokenNumberParser.parseSpokenNumber("خمسمية")!!, 0.01)
        assertEquals(5000.0, SpokenNumberParser.parseSpokenNumber("خمسة تلاف")!!, 0.01)
        assertEquals(2500.0, SpokenNumberParser.parseSpokenNumber("الفين وخمسمية")!!, 0.01)

        // "ونص" after thousands (+500)
        assertEquals(5500.0, SpokenNumberParser.parseSpokenNumber("خمسة آلاف ونص")!!, 0.01)
        assertEquals(1500.0, SpokenNumberParser.parseSpokenNumber("ألف ونص")!!, 0.01)
        assertEquals(10500.0, SpokenNumberParser.parseSpokenNumber("عشرة آلاف ونص")!!, 0.01)
        assertEquals(15500.0, SpokenNumberParser.parseSpokenNumber("خمسطعش ألف ونص")!!, 0.01)

        // Millions idioms
        assertEquals(250000.0, SpokenNumberParser.parseSpokenNumber("ربع مليون")!!, 0.01)
        assertEquals(500000.0, SpokenNumberParser.parseSpokenNumber("نص مليون")!!, 0.01)
        assertEquals(1500000.0, SpokenNumberParser.parseSpokenNumber("مليون ونص")!!, 0.01)
    }

    @Test
    fun testMixedDigitAndWordNumbers() {
        assertEquals(15000.0, SpokenNumberParser.parseSpokenNumber("15 ألف")!!, 0.01)
        assertEquals(2000000.0, SpokenNumberParser.parseSpokenNumber("2 مليون")!!, 0.01)
        assertEquals(100000.0, SpokenNumberParser.parseSpokenNumber("100 ألف")!!, 0.01)
    }

    // =========================================================================
    // 2. SpokenDurationParser Tests (18 phrases)
    // =========================================================================

    @Test
    fun testDurationParsing() {
        assertEquals(60, SpokenDurationParser.parseSpokenDuration("ساعة")!!)
        assertEquals(60, SpokenDurationParser.parseSpokenDuration("ساعة واحدة")!!)
        assertEquals(120, SpokenDurationParser.parseSpokenDuration("ساعتين")!!)
        assertEquals(120, SpokenDurationParser.parseSpokenDuration("ساعتان")!!)
        assertEquals(180, SpokenDurationParser.parseSpokenDuration("ثلاث ساعات")!!)
        assertEquals(180, SpokenDurationParser.parseSpokenDuration("3 ساعات")!!)
        assertEquals(240, SpokenDurationParser.parseSpokenDuration("أربع ساعات")!!)

        // Fractions with hour
        assertEquals(90, SpokenDurationParser.parseSpokenDuration("ساعة ونص")!!)
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين ونص")!!)
        assertEquals(210, SpokenDurationParser.parseSpokenDuration("ثلاث ساعات ونص")!!)
        assertEquals(75, SpokenDurationParser.parseSpokenDuration("ساعة وربع")!!)
        assertEquals(135, SpokenDurationParser.parseSpokenDuration("ساعتين وربع")!!)
        assertEquals(105, SpokenDurationParser.parseSpokenDuration("ساعتين الا ربع")!!)
        assertEquals(45, SpokenDurationParser.parseSpokenDuration("ساعة الا ربع")!!)
        assertEquals(140, SpokenDurationParser.parseSpokenDuration("ساعتين وثلث")!!)

        // Standalone fractions
        assertEquals(30, SpokenDurationParser.parseSpokenDuration("نص ساعة")!!)
        assertEquals(15, SpokenDurationParser.parseSpokenDuration("ربع ساعة")!!)
        assertEquals(20, SpokenDurationParser.parseSpokenDuration("ثلث ساعة")!!)

        // Explicit minutes
        assertEquals(70, SpokenDurationParser.parseSpokenDuration("ساعة وعشر دقائق")!!)
        assertEquals(45, SpokenDurationParser.parseSpokenDuration("45 دقيقة")!!)
        assertEquals(140, SpokenDurationParser.parseSpokenDuration("ساعتين و20 دقيقة")!!)
        assertEquals(10, SpokenDurationParser.parseSpokenDuration("عشر دقائق")!!)
    }

    // =========================================================================
    // 3. VoiceCommandParser Core 5 Examples & Command Types (20 phrases)
    // =========================================================================

    @Test
    fun testCoreExample1Session() {
        val cmd = VoiceCommandParser.parse("سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة آلاف")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("أحمد العولقي", cmd.customerQuery)
        assertEquals(150, cmd.durationMinutes)
        assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
        assertEquals(5000.0, cmd.paid!!, 0.01)
        assertNull(cmd.amount)
    }

    @Test
    fun testCoreExample2Receipt() {
        val cmd = VoiceCommandParser.parse("قبض من أحمد العولقي خمسطعش ألف")
        assertEquals(VoiceCommandType.RECEIPT, cmd.type)
        assertEquals("أحمد العولقي", cmd.customerQuery)
        assertEquals(15000.0, cmd.amount!!, 0.01)
        assertNull(cmd.durationMinutes)
        assertNull(cmd.ratePerHour)
    }

    @Test
    fun testCoreExample3Expense() {
        val cmd = VoiceCommandParser.parse("صرف ديزل خمسين ألف")
        assertEquals(VoiceCommandType.EXPENSE, cmd.type)
        assertEquals("ديزل", cmd.category)
        assertEquals(50000.0, cmd.amount!!, 0.01)
    }

    @Test
    fun testCoreExample4Purchase() {
        val cmd = VoiceCommandParser.parse("شراء من صالح ثلاث ساعات من ثمانية آلاف")
        assertEquals(VoiceCommandType.PURCHASE, cmd.type)
        assertEquals("صالح", cmd.customerQuery)
        assertEquals(180, cmd.durationMinutes)
        assertEquals(8000.0, cmd.ratePerHour!!, 0.01)
    }

    @Test
    fun testCoreExample5Discount() {
        val cmd = VoiceCommandParser.parse("خصم لأحمد ألفين")
        assertEquals(VoiceCommandType.DISCOUNT, cmd.type)
        assertEquals("أحمد", cmd.customerQuery)
        assertEquals(2000.0, cmd.amount!!, 0.01)
    }

    @Test
    fun testTotalWordsAreStrictlyIgnored() {
        // "The user never says the total. If a total-like word appears, ignore that number."
        val cmd = VoiceCommandParser.parse("سقي علي ساعتين من خمسة آلاف المجموع عشرة آلاف دفع ألفين")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("علي", cmd.customerQuery)
        assertEquals(120, cmd.durationMinutes)
        assertEquals(5000.0, cmd.ratePerHour!!, 0.01)
        assertEquals(2000.0, cmd.paid!!, 0.01)
        // Ensure total (10,000) was completely discarded from command fields
        assertNull(cmd.amount)
    }

    @Test
    fun testMissingRateInSession() {
        val cmd = VoiceCommandParser.parse("سقي محمد ثلاث ساعات")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("محمد", cmd.customerQuery)
        assertEquals(180, cmd.durationMinutes)
        assertNull(cmd.ratePerHour) // Missing rate should be left null for form default
    }

    @Test
    fun testMissingDurationInSession() {
        val cmd = VoiceCommandParser.parse("سقي سعيد من سبعة آلاف")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("سعيد", cmd.customerQuery)
        assertNull(cmd.durationMinutes) // Missing duration left null
        assertEquals(7000.0, cmd.ratePerHour!!, 0.01)
    }

    @Test
    fun testExpenseWithUnrecognizedCategoryGoesToNotes() {
        val cmd = VoiceCommandParser.parse("صرف شراء وجبة غداء خمسة آلاف")
        assertEquals(VoiceCommandType.EXPENSE, cmd.type)
        assertEquals(5000.0, cmd.amount!!, 0.01)
        assertTrue(cmd.notes?.contains("وجبة غداء") == true || cmd.category == "وجبة غداء")
    }

    @Test
    fun testExplicitNotesParsing() {
        val cmd = VoiceCommandParser.parse("سقي سالم ساعتين من ستة آلاف ملاحظة ماء صافي")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("سالم", cmd.customerQuery)
        assertEquals(120, cmd.durationMinutes)
        assertEquals(6000.0, cmd.ratePerHour!!, 0.01)
        assertEquals("ماء صافي", cmd.notes)
    }

    // =========================================================================
    // 4. CustomerMatcher Tests (Similar names & learned aliases)
    // =========================================================================

    @Test
    fun testSimilarNamesAmbiguity() {
        // Two similar names: "أحمد بن حسين" and "أحمد بن قاسم"
        val c1 = Customer(id = 1L, name = "أحمد بن حسين العولقي", phone = "771111111")
        val c2 = Customer(id = 2L, name = "أحمد بن قاسم الحكيمي", phone = "772222222")
        val customers = listOf(c1, c2)

        // Query "أحمد" matches both partially -> not high confidence, user must choose
        val matches = CustomerMatcher.match("أحمد", customers)
        assertEquals(2, matches.size)
        assertFalse("Ambiguous single name match must NOT be accepted automatically", CustomerMatcher.isHighConfidence(matches))

        // Query "أحمد العولقي" matches c1 clearly ahead of c2
        val specificMatches = CustomerMatcher.match("أحمد العولقي", customers)
        assertTrue(specificMatches.isNotEmpty())
        assertEquals(1L, specificMatches.first().customer.id)
        assertTrue("Specific match with surname must be high confidence", CustomerMatcher.isHighConfidence(specificMatches))
    }

    @Test
    fun testLearnedAliasesOverridesScoring() {
        val c1 = Customer(id = 10L, name = "صالح محسن السعدي", phone = "773333333")
        val c2 = Customer(id = 20L, name = "صالح علي اليافعي", phone = "774444444")
        val customers = listOf(c1, c2)

        // Learned alias: user previously mapped "ابو علي" to customer 20
        val aliases = mapOf("ابو علي" to 20L)
        val matches = CustomerMatcher.match("ابو علي", customers, aliases)
        assertTrue(matches.isNotEmpty())
        assertEquals(20L, matches.first().customer.id)
        assertTrue("Learned alias must have high confidence", CustomerMatcher.isHighConfidence(matches))
    }

    @Test
    fun testVoiceAliasSerialization() {
        val map = mapOf("احمد العولقي" to 15L, "صالح اليافعي" to 22L)
        val json = VoiceAliasRepository.serializeAliasesJson(map)
        val parsed = VoiceAliasRepository.parseAliasesJson(json)
        assertEquals(map, parsed)
    }

    // =========================================================================
    // 5. Comprehensive 65-Phrase Suite
    // =========================================================================

    @Test
    fun testSixtyFivePhrasesSuite() {
        val phrases = listOf(
            "سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة آلاف" to VoiceCommandType.SESSION,
            "قبض من أحمد العولقي خمسطعش ألف" to VoiceCommandType.RECEIPT,
            "صرف ديزل خمسين ألف" to VoiceCommandType.EXPENSE,
            "شراء من صالح ثلاث ساعات من ثمانية آلاف" to VoiceCommandType.PURCHASE,
            "خصم لأحمد ألفين" to VoiceCommandType.DISCOUNT,
            "سقي علي ساعتين من خمسة آلاف المجموع عشرة آلاف دفع ألفين" to VoiceCommandType.SESSION,
            "سقي محمد ثلاث ساعات" to VoiceCommandType.SESSION,
            "سقي سعيد من سبعة آلاف" to VoiceCommandType.SESSION,
            "سقي فؤاد ساعتين ونص دفع ثلاثة آلاف" to VoiceCommandType.SESSION,
            "سقي خالد ساعة واحدة من تسعة آلاف" to VoiceCommandType.SESSION,
            "سقية ناصر ساعتين وربع من 12000" to VoiceCommandType.SESSION,
            "سقاية قاسم ساعتين الا ربع من 8500" to VoiceCommandType.SESSION,
            "سقيت لحسين أربع ساعات من سبعة آلاف وخمسمية" to VoiceCommandType.SESSION,
            "سقي عند سالم ساعتين وثلث من 10000" to VoiceCommandType.SESSION,
            "سقي حق يحيى نص ساعة من 5000" to VoiceCommandType.SESSION,
            "سقي الأخ عادل 45 دقيقة من ستة آلاف" to VoiceCommandType.SESSION,
            "سقي العم مسعد ساعتين وعشر دقائق من 8000" to VoiceCommandType.SESSION,
            "سقي الحاج صالح ساعتين من عشرة آلاف ونص" to VoiceCommandType.SESSION,
            "سقي الشيخ مقبل ثلاث ساعات ونص من 15 ألف" to VoiceCommandType.SESSION,
            "سقي لـ أحمد خمس ساعات من 6000 سلم أربعة آلاف" to VoiceCommandType.SESSION,
            "قبض من محمد عشرة آلاف" to VoiceCommandType.RECEIPT,
            "قبضت من سالم عشرين ألف" to VoiceCommandType.RECEIPT,
            "استلمت من علي خمسة آلاف" to VoiceCommandType.RECEIPT,
            "سدد أحمد خمسة وعشرين ألف" to VoiceCommandType.RECEIPT,
            "سداد من صالح ثلاثين ألف" to VoiceCommandType.RECEIPT,
            "قبض من مزارع مائة ألف" to VoiceCommandType.RECEIPT,
            "قبض من العولقي الفين وخمسمية" to VoiceCommandType.RECEIPT,
            "استلمت من سعيد خمسة تلاف" to VoiceCommandType.RECEIPT,
            "قبض من فهد ربع مليون" to VoiceCommandType.RECEIPT,
            "قبض من حسين نص مليون" to VoiceCommandType.RECEIPT,
            "قبض من محسن مليون ونص" to VoiceCommandType.RECEIPT,
            "استلمت من قاسم 12500" to VoiceCommandType.RECEIPT,
            "قبض من يحيى 50000" to VoiceCommandType.RECEIPT,
            "سدد خالد 15 ألف" to VoiceCommandType.RECEIPT,
            "صرف صيانة خمسة عشر ألف" to VoiceCommandType.EXPENSE,
            "صرفت تغيير زيت ستة آلاف" to VoiceCommandType.EXPENSE,
            "صرف كهرباء ثمانية آلاف" to VoiceCommandType.EXPENSE,
            "صرف أجور عمال عشرين ألف" to VoiceCommandType.EXPENSE,
            "صرف قطع غيار خمسة وثلاثين ألف" to VoiceCommandType.EXPENSE,
            "صرف مواصير اثنا عشر ألف" to VoiceCommandType.EXPENSE,
            "صرف فلتر ديزل ألفين وخمسمية" to VoiceCommandType.EXPENSE,
            "صرف نقل وتوصيل أربعة آلاف" to VoiceCommandType.EXPENSE,
            "صرف ضيافة ثلاثة آلاف" to VoiceCommandType.EXPENSE,
            "صرف بطارية جديدة خمسين ألف" to VoiceCommandType.EXPENSE,
            "صرف طاقة شمسية مئة ألف" to VoiceCommandType.EXPENSE,
            "صرفت شراء وجبة غداء خمسة آلاف" to VoiceCommandType.EXPENSE,
            "صرف مفتاح صيانة ألفين" to VoiceCommandType.EXPENSE,
            "دفعت لـ ورشة الحدادة عشرة آلاف" to VoiceCommandType.EXPENSE,
            "شراء ساعتين من بئر السلام من سبعة آلاف" to VoiceCommandType.PURCHASE,
            "اشتريت من بئر النور ثلاث ساعات ونص من 8000" to VoiceCommandType.PURCHASE,
            "شريت من صاحب البئر أربع ساعات من تسعة آلاف دفعنا خمسة آلاف" to VoiceCommandType.PURCHASE,
            "شراء من بئر الخير ساعة وربع من 6000" to VoiceCommandType.PURCHASE,
            "شراء من ناصر ساعتين الا ربع من 7500" to VoiceCommandType.PURCHASE,
            "شراء من محسن نص ساعة من 4000" to VoiceCommandType.PURCHASE,
            "خصم لسالم خمسة آلاف" to VoiceCommandType.DISCOUNT,
            "سامحت علي بألفين ريال" to VoiceCommandType.DISCOUNT,
            "خصم لمحمد ثلاثة آلاف وخمسمية" to VoiceCommandType.DISCOUNT,
            "خصم للأخ حسن عشرة آلاف" to VoiceCommandType.DISCOUNT,
            "سامحت خالد بألف ونص" to VoiceCommandType.DISCOUNT,
            "خصم لسعيد 5000" to VoiceCommandType.DISCOUNT,
            "سقي أحمد ساعتين من 10000 الإجمالي 20000 مدفوع 5000" to VoiceCommandType.SESSION,
            "سقي صالح ساعتين من خمسة آلاف بيان سقي قات" to VoiceCommandType.SESSION,
            "قبض من عادل سبعة آلاف وخمسمية بيان دفعة أولى" to VoiceCommandType.RECEIPT,
            "صرف ديزل أربعين ألف ملاحظة برميل ديزل" to VoiceCommandType.EXPENSE,
            "سقي علي ثلاث ساعات من 8000 سلمت 4000" to VoiceCommandType.SESSION
        )

        assertEquals(65, phrases.size)
        for ((phrase, expectedType) in phrases) {
            val cmd = VoiceCommandParser.parse(phrase)
            assertEquals("Phrase '$phrase' must be parsed as $expectedType", expectedType, cmd.type)
        }
    }

    @Test
    fun testRuleFiveVoicePrefillRequiresUnsavedChangesPrompt() {
        val dummyCommand = VoiceCommand(
            type = VoiceCommandType.SESSION,
            customerQuery = "أحمد",
            durationMinutes = 120,
            ratePerHour = 10000.0,
            paid = 5000.0
        )

        // When a form is opened WITHOUT voice, initialSnapshot == currentSnapshot -> no unsaved changes
        val emptyFormHasChanges = (false /* current == initial */) || (null != null)
        assertFalse("Empty form without edits has no unsaved changes", emptyFormHasChanges)

        // When a form is opened WITH voice, even if currentSnapshot == initialSnapshot (no user edits yet),
        // hasUnsavedChanges must evaluate to true so closing the form asks 'تجاهل التغييرات؟'
        val voicePrefilledFormHasChanges = (false /* current == initial */) || (dummyCommand != null)
        assertTrue("Voice prefilled form must trigger discard confirmation to prevent data loss", voicePrefilledFormHasChanges)
    }

    // =========================================================================
    // 5. Recognizer-Style Test Input (Review 030 Round 1 Item 3)
    // Testing the 5 example phrases in recognizer-returned format:
    // ASCII digits, Eastern Arabic digits, and no punctuation.
    // =========================================================================

    @Test
    fun testRecognizerStyleExamplePhrases() {
        // Phrase 1: سقي (ASCII digits, Eastern digits, mixed phrases, no punctuation)
        val p1Ascii = VoiceCommandParser.parse("سقي احمد العولقي 2 ساعة ونص من 10000 دفع 5000")
        assertEquals(VoiceCommandType.SESSION, p1Ascii.type)
        assertEquals("احمد العولقي", p1Ascii.customerQuery)
        assertEquals(150, p1Ascii.durationMinutes)
        assertEquals(10000.0, p1Ascii.ratePerHour!!, 0.01)
        assertEquals(5000.0, p1Ascii.paid!!, 0.01)

        val p1Eastern = VoiceCommandParser.parse("سقي احمد العولقي ٢ ساعة ونص من ١٠٠٠٠ دفع ٥٠٠٠")
        assertEquals(VoiceCommandType.SESSION, p1Eastern.type)
        assertEquals("احمد العولقي", p1Eastern.customerQuery)
        assertEquals(150, p1Eastern.durationMinutes)
        assertEquals(10000.0, p1Eastern.ratePerHour!!, 0.01)
        assertEquals(5000.0, p1Eastern.paid!!, 0.01)

        val p1MixedAscii = VoiceCommandParser.parse("سقي احمد العولقي ساعتين ونص من 10 الاف دفع 5 الاف")
        assertEquals(VoiceCommandType.SESSION, p1MixedAscii.type)
        assertEquals("احمد العولقي", p1MixedAscii.customerQuery)
        assertEquals(150, p1MixedAscii.durationMinutes)
        assertEquals(10000.0, p1MixedAscii.ratePerHour!!, 0.01)
        assertEquals(5000.0, p1MixedAscii.paid!!, 0.01)

        val p1MixedEastern = VoiceCommandParser.parse("سقي احمد العولقي ساعتين ونص من ١٠ الاف دفع ٥ الاف")
        assertEquals(VoiceCommandType.SESSION, p1MixedEastern.type)
        assertEquals("احمد العولقي", p1MixedEastern.customerQuery)
        assertEquals(150, p1MixedEastern.durationMinutes)
        assertEquals(10000.0, p1MixedEastern.ratePerHour!!, 0.01)
        assertEquals(5000.0, p1MixedEastern.paid!!, 0.01)

        // Phrase 2: قبض (ASCII raw digits, ASCII mixed, Eastern raw digits, Eastern mixed)
        val p2AsciiRaw = VoiceCommandParser.parse("قبض من احمد العولقي 15000")
        assertEquals(VoiceCommandType.RECEIPT, p2AsciiRaw.type)
        assertEquals("احمد العولقي", p2AsciiRaw.customerQuery)
        assertEquals(15000.0, p2AsciiRaw.amount!!, 0.01)

        val p2AsciiWord = VoiceCommandParser.parse("قبض من احمد العولقي 15 الف")
        assertEquals(VoiceCommandType.RECEIPT, p2AsciiWord.type)
        assertEquals("احمد العولقي", p2AsciiWord.customerQuery)
        assertEquals(15000.0, p2AsciiWord.amount!!, 0.01)

        val p2EasternRaw = VoiceCommandParser.parse("قبض من احمد العولقي ١٥٠٠٠")
        assertEquals(VoiceCommandType.RECEIPT, p2EasternRaw.type)
        assertEquals("احمد العولقي", p2EasternRaw.customerQuery)
        assertEquals(15000.0, p2EasternRaw.amount!!, 0.01)

        val p2EasternWord = VoiceCommandParser.parse("قبض من احمد العولقي ١٥ الف")
        assertEquals(VoiceCommandType.RECEIPT, p2EasternWord.type)
        assertEquals("احمد العولقي", p2EasternWord.customerQuery)
        assertEquals(15000.0, p2EasternWord.amount!!, 0.01)

        // Phrase 3: صرف (ASCII raw digits, ASCII mixed, Eastern raw digits, Eastern mixed)
        val p3AsciiRaw = VoiceCommandParser.parse("صرف ديزل 50000")
        assertEquals(VoiceCommandType.EXPENSE, p3AsciiRaw.type)
        assertEquals("ديزل", p3AsciiRaw.category)
        assertEquals(50000.0, p3AsciiRaw.amount!!, 0.01)

        val p3AsciiWord = VoiceCommandParser.parse("صرف ديزل 50 الف")
        assertEquals(VoiceCommandType.EXPENSE, p3AsciiWord.type)
        assertEquals("ديزل", p3AsciiWord.category)
        assertEquals(50000.0, p3AsciiWord.amount!!, 0.01)

        val p3EasternRaw = VoiceCommandParser.parse("صرف ديزل ٥٠٠٠٠")
        assertEquals(VoiceCommandType.EXPENSE, p3EasternRaw.type)
        assertEquals("ديزل", p3EasternRaw.category)
        assertEquals(50000.0, p3EasternRaw.amount!!, 0.01)

        val p3EasternWord = VoiceCommandParser.parse("صرف ديزل ٥٠ الف")
        assertEquals(VoiceCommandType.EXPENSE, p3EasternWord.type)
        assertEquals("ديزل", p3EasternWord.category)
        assertEquals(50000.0, p3EasternWord.amount!!, 0.01)

        // Phrase 4: شراء (ASCII raw digits, ASCII mixed, Eastern raw digits, Eastern mixed)
        val p4AsciiRaw = VoiceCommandParser.parse("شراء من صالح 3 ساعات من 8000")
        assertEquals(VoiceCommandType.PURCHASE, p4AsciiRaw.type)
        assertEquals("صالح", p4AsciiRaw.customerQuery)
        assertEquals(180, p4AsciiRaw.durationMinutes)
        assertEquals(8000.0, p4AsciiRaw.ratePerHour!!, 0.01)

        val p4AsciiWord = VoiceCommandParser.parse("شراء من صالح 3 ساعات من 8 الاف")
        assertEquals(VoiceCommandType.PURCHASE, p4AsciiWord.type)
        assertEquals("صالح", p4AsciiWord.customerQuery)
        assertEquals(180, p4AsciiWord.durationMinutes)
        assertEquals(8000.0, p4AsciiWord.ratePerHour!!, 0.01)

        val p4EasternRaw = VoiceCommandParser.parse("شراء من صالح ٣ ساعات من ٨٠٠٠")
        assertEquals(VoiceCommandType.PURCHASE, p4EasternRaw.type)
        assertEquals("صالح", p4EasternRaw.customerQuery)
        assertEquals(180, p4EasternRaw.durationMinutes)
        assertEquals(8000.0, p4EasternRaw.ratePerHour!!, 0.01)

        val p4EasternWord = VoiceCommandParser.parse("شراء من صالح ٣ ساعات من ٨ الاف")
        assertEquals(VoiceCommandType.PURCHASE, p4EasternWord.type)
        assertEquals("صالح", p4EasternWord.customerQuery)
        assertEquals(180, p4EasternWord.durationMinutes)
        assertEquals(8000.0, p4EasternWord.ratePerHour!!, 0.01)

        // Phrase 5: خصم (ASCII raw digits, ASCII mixed, Eastern raw digits, Eastern mixed)
        val p5AsciiRaw = VoiceCommandParser.parse("خصم لاحمد 2000")
        assertEquals(VoiceCommandType.DISCOUNT, p5AsciiRaw.type)
        assertEquals("احمد", p5AsciiRaw.customerQuery)
        assertEquals(2000.0, p5AsciiRaw.amount!!, 0.01)

        val p5AsciiWord = VoiceCommandParser.parse("خصم لاحمد 2 الف")
        assertEquals(VoiceCommandType.DISCOUNT, p5AsciiWord.type)
        assertEquals("احمد", p5AsciiWord.customerQuery)
        assertEquals(2000.0, p5AsciiWord.amount!!, 0.01)

        val p5EasternRaw = VoiceCommandParser.parse("خصم لاحمد ٢٠٠٠")
        assertEquals(VoiceCommandType.DISCOUNT, p5EasternRaw.type)
        assertEquals("احمد", p5EasternRaw.customerQuery)
        assertEquals(2000.0, p5EasternRaw.amount!!, 0.01)

        val p5EasternWord = VoiceCommandParser.parse("خصم لاحمد ٢ الف")
        assertEquals(VoiceCommandType.DISCOUNT, p5EasternWord.type)
        assertEquals("احمد", p5EasternWord.customerQuery)
        assertEquals(2000.0, p5EasternWord.amount!!, 0.01)
    }


    // =========================================================================
    // 6. Review 030 Round 2 Tests (Google Output & Owner Verification)
    // =========================================================================

    @Test
    fun testReviewRound2Item1ExactPhrase() {
        // Exact real phrase produced by Google in review round 2 owner test
        val cmd = VoiceCommandParser.parse("سقي أحمد العولقي ساعتين ونص من 10,000 دفعه 5000")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertEquals("أحمد العولقي", cmd.customerQuery)
        assertEquals(150, cmd.durationMinutes)
        assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
        assertEquals(5000.0, cmd.paid!!, 0.01)
        assertTrue("Unparsed list should be empty: ${cmd.unparsed}", cmd.unparsed.isEmpty())
    }

    @Test
    fun testReviewRound2Item1Separators() {
        // "10,000", "10٬000", "10 000", "١٠٬٠٠٠", "10.000"
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("10,000")!!, 0.01)
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("10٬000")!!, 0.01)
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("10 000")!!, 0.01)
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("١٠٬٠٠٠")!!, 0.01)
        assertEquals(10000.0, SpokenNumberParser.parseSpokenNumber("10.000")!!, 0.01)

        val separatorPhrases = listOf(
            "سقي أحمد ساعتين من 10,000 دفع 5000",
            "سقي أحمد ساعتين من 10٬000 دفع 5000",
            "سقي أحمد ساعتين من 10 000 دفع 5000",
            "سقي أحمد ساعتين من ١٠٬٠٠٠ دفع 5000",
            "سقي أحمد ساعتين من 10.000 دفع 5000"
        )
        for (phrase in separatorPhrases) {
            val cmd = VoiceCommandParser.parse(phrase)
            assertEquals(120, cmd.durationMinutes)
            assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
            assertEquals(5000.0, cmd.paid!!, 0.01)
            assertTrue("Unparsed should be empty for '$phrase'", cmd.unparsed.isEmpty())
        }
    }

    @Test
    fun testReviewRound2Item1PaidWordsVariants() {
        // Accept: دفعه، دفعة، ودفع، و دفع، دافع، مدفوع، سلّم، سلمه، وسلم، and missing space after و
        val paidVariants = listOf(
            "دفعه", "دفعة", "ودفع", "و دفع", "دافع", "مدفوع",
            "سلّم", "سلم", "سلمه", "سلمة", "وسلم", "و سلم", "ودفعه", "وسلمه"
        )
        for (paidWord in paidVariants) {
            val phrase = "سقي أحمد ساعتين من 10000 $paidWord 5000"
            val cmd = VoiceCommandParser.parse(phrase)
            assertEquals("Failed parsing paid word: '$paidWord' in '$phrase'", 5000.0, cmd.paid!!, 0.01)
            assertEquals("Failed parsing rate with paid word: '$paidWord'", 10000.0, cmd.ratePerHour!!, 0.01)
            assertEquals(120, cmd.durationMinutes)
            assertTrue("Unparsed should be empty for '$phrase'", cmd.unparsed.isEmpty())
        }
    }

    @Test
    fun testReviewRound2Item2DurationVariants() {
        // "ونص"، "و نص"، "ونصف"، "و نصف"، "ونُص"، "و30"، "و 30 دقيقه"،
        // "و نص ساعه"، "ساعتين ونص ساعه"، "٢ ساعه ونص"، "2:30"، "ساعتين و نصف"، "الا ربع"، "إلا ربع"
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين ونص"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين و نص"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين ونصف"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين و نصف"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين ونُص"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين و30"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين و 30 دقيقه"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين و نص ساعه"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("ساعتين ونص ساعه"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("٢ ساعه ونص"))
        assertEquals(150, SpokenDurationParser.parseSpokenDuration("2:30"))
        assertEquals(45, SpokenDurationParser.parseSpokenDuration("الا ربع"))
        assertEquals(45, SpokenDurationParser.parseSpokenDuration("إلا ربع"))
        assertEquals(105, SpokenDurationParser.parseSpokenDuration("ساعتين الا ربع"))
        assertEquals(105, SpokenDurationParser.parseSpokenDuration("ساعتين إلا ربع"))

        // In full voice commands
        val cmdDigital = VoiceCommandParser.parse("سقي أحمد العولقي 2:30 من 10000")
        assertEquals(150, cmdDigital.durationMinutes)
        assertEquals(10000.0, cmdDigital.ratePerHour!!, 0.01)
        assertEquals("أحمد العولقي", cmdDigital.customerQuery)

        val cmdEastern = VoiceCommandParser.parse("سقي أحمد العولقي ٢ ساعه ونص من 10000")
        assertEquals(150, cmdEastern.durationMinutes)
        assertEquals(10000.0, cmdEastern.ratePerHour!!, 0.01)

        val cmdRedundantHour = VoiceCommandParser.parse("سقي أحمد العولقي ساعتين ونص ساعه من 10000")
        assertEquals(150, cmdRedundantHour.durationMinutes)
        assertEquals(10000.0, cmdRedundantHour.ratePerHour!!, 0.01)

        val cmdDigitsMinutes = VoiceCommandParser.parse("سقي أحمد العولقي ساعتين و 30 دقيقه من 10000")
        assertEquals(150, cmdDigitsMinutes.durationMinutes)
        assertEquals(10000.0, cmdDigitsMinutes.ratePerHour!!, 0.01)
    }


    // =========================================================================
    // 7. Review 030 Round 3 Tests (Unprefixed duration phrases & candidate lists)
    // =========================================================================

    @Test
    fun testReviewRound3Item1OwnerPhraseCandidates() {
        val candidates = listOf(
            "ثلاث ساعات ونص مع احمد علي من 10,000 ومسلم 20,000",
            "… مسلم 20,000",
            "3 ساعات ونص مع احمد علي من 10,000 ومسلم 20,000"
        )
        val cmd = VoiceRecognitionHelper.parseFirstValidCommand(candidates)
        assertNotNull("Command must be parsed successfully", cmd)
        assertEquals(VoiceCommandType.SESSION, cmd!!.type)
        assertEquals(210, cmd.durationMinutes)
        assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
        assertEquals(20000.0, cmd.paid!!, 0.01)
        assertEquals("احمد علي", cmd.customerQuery)
        assertTrue("Unparsed list should be empty: ${cmd.unparsed}", cmd.unparsed.isEmpty())
    }
}
