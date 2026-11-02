package com.example.core.speech

import com.example.features.sessions.WaterSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpeechTextBuilderTest {

    @Test
    fun testFormatDayMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2)
        }
        val result = SpeechTextBuilder.formatDayMonth(cal.time)
        assertEquals("الثاني من نوفمبر", result)

        cal.set(Calendar.DAY_OF_MONTH, 1)
        assertEquals("الأول من نوفمبر", SpeechTextBuilder.formatDayMonth(cal.time))

        cal.set(Calendar.DAY_OF_MONTH, 15)
        assertEquals("الخامس عشر من نوفمبر", SpeechTextBuilder.formatDayMonth(cal.time))

        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        assertEquals("الحادي والثلاثون من يناير", SpeechTextBuilder.formatDayMonth(cal.time))
    }

    @Test
    fun testFormatDurationWords() {
        assertEquals("ساعتان وثلاثون دقيقة", SpeechTextBuilder.formatDurationWords(150))
        assertEquals("ساعة واحدة وثلاثون دقيقة", SpeechTextBuilder.formatDurationWords(90))
        assertEquals("ساعة واحدة", SpeechTextBuilder.formatDurationWords(60))
        assertEquals("ساعتان", SpeechTextBuilder.formatDurationWords(120))
        assertEquals("ثلاث ساعات", SpeechTextBuilder.formatDurationWords(180))
        assertEquals("خمس وأربعون دقيقة", SpeechTextBuilder.formatDurationWords(45))
        assertEquals("دقيقة واحدة", SpeechTextBuilder.formatDurationWords(1))
        assertEquals("دقيقتان", SpeechTextBuilder.formatDurationWords(2))
        assertEquals("عشر دقائق", SpeechTextBuilder.formatDurationWords(10))
        assertEquals("خمس عشرة دقيقة", SpeechTextBuilder.formatDurationWords(15))
        assertEquals("صفر دقيقة", SpeechTextBuilder.formatDurationWords(0))
    }

    @Test
    fun testSpeechNumberWordsRequiredAmounts() {
        assertEquals("سبع مِئة وخمسون ريال", SpeechTextBuilder.amountToSpeechWords(750.0))
        assertEquals("ثلاثة آلاف وخمس مِئة ريال", SpeechTextBuilder.amountToSpeechWords(3500.0))
        assertEquals("أربعة عشر ألف ريال", SpeechTextBuilder.amountToSpeechWords(14000.0))
        assertEquals("أربعة عشر ألف وخمس مِئة ريال", SpeechTextBuilder.amountToSpeechWords(14500.0))
        assertEquals("مِئة وخمسة وعشرون ألف ريال", SpeechTextBuilder.amountToSpeechWords(125000.0))
        assertEquals("مليونين ريال", SpeechTextBuilder.amountToSpeechWords(2000000.0))
    }

    @Test
    fun testAlfanNeverAppearsInSpeechText() {
        // Assert "ألفاً" never appears across various thousands
        val amounts = listOf(1000.0, 5000.0, 11000.0, 14000.0, 20000.0, 99000.0, 100000.0)
        for (amt in amounts) {
            val words = SpeechTextBuilder.amountToSpeechWords(amt)
            assertFalse("Words for $amt must not contain ألفاً", words.contains("ألفاً"))
            val forSpeechText = SpeechPronunciation.forSpeech(words)
            assertFalse("forSpeech output for $amt must not contain ألفاً", forSpeechText.contains("ألفاً"))
        }

        // Test explicit replacement of any residual tanween
        assertEquals("أربعةْ عشرْ أَلْفْ رِيَالْ", SpeechPronunciation.forSpeech("أربعة عشر ألفاً ريال"))
    }

    @Test
    fun testOwnerReportedPronunciations() {
        // Owner, 2026-11-03: no case endings or tanween, tens in the spoken form
        assertEquals("سَبْعِينْ أَلْفْ رِيَالْ", SpeechPronunciation.forSpeech("سبعون ألفاً ريالاً"))
        assertEquals("وَسِتِّينْ", SpeechPronunciation.forSpeech("وستون"))
        assertEquals("أَلْفَيْنْ رِيَالْ", SpeechPronunciation.forSpeech("ألفان ريال"))
        assertEquals("مِئَتَيْنْ", SpeechPronunciation.forSpeech("مائتان"))
        assertEquals("قَبْضْ", SpeechPronunciation.forSpeech("قبض"))
        assertEquals("سَاعَاتْ", SpeechPronunciation.forSpeech("ساعات"))
        assertEquals("مُسَدَّدْ", SpeechPronunciation.forSpeech("مسدد"))
        // Words outside the dictionary get the pause ending too; marked endings stay
        assertEquals("أحمدْ", SpeechPronunciation.forSpeech("أحمد"))
        assertEquals("عَلَيْكَ", SpeechPronunciation.forSpeech("عليك"))
    }

    @Test
    fun testWellOwnerBalanceUsesCardWording() {
        // Owner, 2026-11-03: read "له" / "عليه" like the card, not "عليك" / "لك"
        assertEquals(
            "رصيد صالح: له ألف ريال.",
            SpeechTextBuilder.buildBalanceSentence("صالح", 0.0, isWellOwner = true, payableBalance = 1000.0)
        )
        assertEquals(
            "رصيد صالح: عليه ألف ريال.",
            SpeechTextBuilder.buildBalanceSentence("صالح", 0.0, isWellOwner = true, receivableBalance = 1000.0)
        )
    }

    @Test
    fun testDictionaryWordsAndPrefixesPronunciation() {
        // Base words
        // عليك / لك / عليكم
        assertEquals("عَلَيْكَ", SpeechPronunciation.forSpeech("عليك"))
        assertEquals("وَعَلَيْكَ", SpeechPronunciation.forSpeech("وعليك"))
        assertEquals("فَعَلَيْكَ", SpeechPronunciation.forSpeech("فعليك"))

        assertEquals("لَكَ", SpeechPronunciation.forSpeech("لك"))
        assertEquals("وَلَكَ", SpeechPronunciation.forSpeech("ولك"))
        assertEquals("فَلَكَ", SpeechPronunciation.forSpeech("فلك"))

        assertEquals("عَلَيْكُمْ", SpeechPronunciation.forSpeech("عليكم"))
        assertEquals("وَعَلَيْكُمْ", SpeechPronunciation.forSpeech("وعليكم"))
        assertEquals("فَعَلَيْكُمْ", SpeechPronunciation.forSpeech("فعليكم"))

        assertEquals("سَقْيْ", SpeechPronunciation.forSpeech("سقي"))
        assertEquals("سَقْيَةْ", SpeechPronunciation.forSpeech("سقية"))
        assertEquals("سَنَدْ", SpeechPronunciation.forSpeech("سند"))
        assertEquals("قَبْضْ", SpeechPronunciation.forSpeech("قبض"))
        assertEquals("صَرْفْ", SpeechPronunciation.forSpeech("صرف"))
        assertEquals("خَصْمْ", SpeechPronunciation.forSpeech("خصم"))
        assertEquals("مُسَدَّدْ", SpeechPronunciation.forSpeech("مسدد"))
        assertEquals("عَلَيْهْ", SpeechPronunciation.forSpeech("عليه"))
        assertEquals("لَهُ", SpeechPronunciation.forSpeech("له"))
        assertEquals("البَاقِيْ", SpeechPronunciation.forSpeech("الباقي"))
        assertEquals("المَدْفُوعْ", SpeechPronunciation.forSpeech("المدفوع"))
        assertEquals("شِرَاءْ", SpeechPronunciation.forSpeech("شراء"))
        assertEquals("بِئْرْ", SpeechPronunciation.forSpeech("بئر"))
        assertEquals("كَشْفْ", SpeechPronunciation.forSpeech("كشف"))
        assertEquals("حِسَابْ", SpeechPronunciation.forSpeech("حساب"))
        assertEquals("الرَّصِيدْ", SpeechPronunciation.forSpeech("الرصيد"))
        assertEquals("مِضَخَّةْ", SpeechPronunciation.forSpeech("مضخة"))
        assertEquals("هَدْرْ", SpeechPronunciation.forSpeech("هدر"))
        assertEquals("دَوْرَةْ", SpeechPronunciation.forSpeech("دورة"))
        assertEquals("المُسَرِّبْ", SpeechPronunciation.forSpeech("المسرب"))
        assertEquals("تَقْرِيبًا", SpeechPronunciation.forSpeech("تقريباً"))
        assertEquals("سَاعَةْ", SpeechPronunciation.forSpeech("ساعة"))
        assertEquals("سَاعَتَانْ", SpeechPronunciation.forSpeech("ساعتان"))
        assertEquals("دَقِيقَةْ", SpeechPronunciation.forSpeech("دقيقة"))

        // With prefixes: و / ب / ل / ال / ف
        assertEquals("وَسَقْيْ", SpeechPronunciation.forSpeech("وسقي"))
        assertEquals("بِسَقْيْ", SpeechPronunciation.forSpeech("بسقي"))
        assertEquals("لِسَقْيْ", SpeechPronunciation.forSpeech("لسقي"))
        assertEquals("فَسَقْيْ", SpeechPronunciation.forSpeech("فسقي"))

        assertEquals("وَالرَّصِيدْ", SpeechPronunciation.forSpeech("والرصيد"))
        assertEquals("بِالرَّصِيدْ", SpeechPronunciation.forSpeech("بالرصيد"))
        assertEquals("لِلرَّصِيدْ", SpeechPronunciation.forSpeech("للرصيد"))
        assertEquals("فَالرَّصِيدْ", SpeechPronunciation.forSpeech("فالرصيد"))

        assertEquals("وَالبَاقِيْ", SpeechPronunciation.forSpeech("والباقي"))
        assertEquals("بِالبَاقِيْ", SpeechPronunciation.forSpeech("بالباقي"))
        assertEquals("لِلبَاقِيْ", SpeechPronunciation.forSpeech("للباقي"))
        assertEquals("فَالبَاقِيْ", SpeechPronunciation.forSpeech("فالباقي"))
    }

    @Test
    fun testBuildSessionSentenceFullyPaid() {
        val session = WaterSession(
            customerId = 1L,
            durationMinutes = 150,
            totalAmount = 5000.0,
            amountPaid = 5000.0,
            remainingDebt = 0.0
        )
        val text = SpeechTextBuilder.buildSessionSentence(session, "أحمد", "ريال")
        assertTrue(text.contains("سقي لـأحمد"))
        assertTrue(text.contains("ساعتان وثلاثون دقيقة"))
        assertTrue(text.contains("المبلغ خمسة آلاف ريال"))
        assertTrue(text.contains("مسدد بالكامل."))
        assertFalse(text.contains("المدفوع"))
        assertFalse(text.contains("الباقي"))
        assertFalse(text.contains("فقط"))
        assertFalse(text.contains("لا غير"))
    }

    @Test
    fun testBuildSessionSentencePartialPaid() {
        val session = WaterSession(
            customerId = 1L,
            durationMinutes = 60,
            totalAmount = 5000.0,
            amountPaid = 2000.0,
            remainingDebt = 3000.0
        )
        val text = SpeechTextBuilder.buildSessionSentence(session, "علي", "ريال")
        assertTrue(text.contains("سقي لـعلي"))
        assertTrue(text.contains("ساعة واحدة"))
        assertTrue(text.contains("المبلغ خمسة آلاف ريال"))
        assertTrue(text.contains("المدفوع ألفان ريال"))
        assertTrue(text.contains("الباقي ثلاثة آلاف ريال"))
        assertFalse(text.contains("مسدد بالكامل"))
    }

    @Test
    fun testBuildReceiptSentence() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2)
        }
        val text = SpeechTextBuilder.buildReceiptSentence("أحمد", 10000.0, cal.time, "ريال")
        assertEquals("سند قبض من أحمد، عشرة آلاف ريال، بتاريخ الثاني من نوفمبر.", text)
    }

    @Test
    fun testBuildExpenseSentence() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2)
        }
        val text = SpeechTextBuilder.buildExpenseSentence("صيانة ديزل", 15000.0, cal.time, "ريال")
        assertEquals("سند صرف لـصيانة ديزل، خمسة عشر ألف ريال، بتاريخ الثاني من نوفمبر.", text)
    }

    @Test
    fun testBuildDiscountSentence() {
        val text = SpeechTextBuilder.buildDiscountSentence("سعيد", 2500.0, "ريال")
        assertEquals("خصم لـسعيد، ألفان وخمس مِئة ريال.", text)
    }

    @Test
    fun testBuildPurchaseSentence() {
        val text = SpeechTextBuilder.buildPurchaseSentence("صالح", 150, 20000.0, "ريال")
        assertEquals("شراء ساعات من صالح، ساعتان وثلاثون دقيقة، عشرون ألف ريال.", text)
    }

    @Test
    fun testBuildBalanceSentence() {
        val debt = SpeechTextBuilder.buildBalanceSentence("أحمد", 5000.0, "ريال")
        assertEquals("رصيد أحمد: عليه خمسة آلاف ريال.", debt)

        val credit = SpeechTextBuilder.buildBalanceSentence("أحمد", -5000.0, "ريال")
        assertEquals("رصيد أحمد: له خمسة آلاف ريال.", credit)

        val settled = SpeechTextBuilder.buildBalanceSentence("أحمد", 0.0, "ريال")
        assertEquals("رصيد أحمد: الحساب مسدد.", settled)
    }

    @Test
    fun testTenOperationStatementWordCountUnder110Words() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2)
        }
        val dateMillis = cal.timeInMillis

        // 10 realistic operations
        val op1 = SpeechTextBuilder.buildStatementSessionLine(
            WaterSession(customerId = 1, durationMinutes = 120, totalAmount = 14000.0, amountPaid = 2000.0, remainingDebt = 12000.0),
            dateMillis
        )
        val op2 = SpeechTextBuilder.buildStatementReceiptLine(10000.0, dateMillis)
        val op3 = SpeechTextBuilder.buildStatementSessionLine(
            WaterSession(customerId = 1, durationMinutes = 60, totalAmount = 3000.0, amountPaid = 3000.0, remainingDebt = 0.0),
            dateMillis
        )
        val op4 = SpeechTextBuilder.buildStatementExpenseLine(3000.0, dateMillis)
        val op5 = SpeechTextBuilder.buildStatementPurchaseLine(120, 6000.0, dateMillis)
        val op6 = SpeechTextBuilder.buildStatementSessionLine(
            WaterSession(customerId = 1, durationMinutes = 150, totalAmount = 7500.0, amountPaid = 7500.0, remainingDebt = 0.0),
            dateMillis
        )
        val op7 = SpeechTextBuilder.buildStatementReceiptLine(12000.0, dateMillis)
        val op8 = SpeechTextBuilder.buildStatementDiscountLine(1500.0, dateMillis)
        val op9 = SpeechTextBuilder.buildStatementSessionLine(
            WaterSession(customerId = 1, durationMinutes = 90, totalAmount = 4500.0, amountPaid = 0.0, remainingDebt = 4500.0),
            dateMillis
        )
        val op10 = SpeechTextBuilder.buildStatementReceiptLine(5000.0, dateMillis)

        val operations = listOf(op1, op2, op3, op4, op5, op6, op7, op8, op9, op10)

        val balanceSentence = SpeechTextBuilder.buildBalanceSentence(
            customerName = "محمد علي الأحمدي",
            balance = 14500.0,
            currency = "ريال"
        )

        val fullStatement = SpeechTextBuilder.buildCustomerVoiceStatement(
            customerName = "محمد علي الأحمدي",
            distributorName = "مشروع مياه الريان",
            date = cal.time,
            balanceSentence = balanceSentence,
            operationSentences = operations
        )

        val words = fullStatement.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // Statement verification
        assertTrue("Statement must start with compact header", fullStatement.startsWith("كشف حساب محمد علي الأحمدي من مشروع مياه الريان بتاريخ 2 نوفمبر."))
        assertTrue("Statement must include balance sentence", fullStatement.contains(balanceSentence))
        assertTrue("Statement must end with انتهى الكشف.", fullStatement.endsWith("انتهى الكشف."))
        assertFalse("Statement operations should not repeat customer name", op1.contains("محمد علي الأحمدي"))

        // Requirement from Item 2: stays under ~110 words
        assertTrue("Statement word count ($wordCount words) must stay under 110 words", wordCount <= 110)
    }

    @Test
    fun testVoiceStatementCaptionOwed() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3)
        }
        val caption = SpeechTextBuilder.buildVoiceStatementCaption(
            customerName = "محمد أحمد",
            balance = 13163.0,
            isWellOwner = false,
            operationCount = 10,
            date = cal.time,
            distributorName = "عمر الحومي",
            distributorPhone = "773712030",
            currencySymbol = "ر.ي"
        )

        assertTrue(caption.contains("🔊 كشف حساب صوتي: محمد أحمد"))
        assertTrue(caption.contains("الرصيد: عليه 13,163 ر.ي (ثلاثة عشر ألفاً ومائة وثلاثة وستون ريالاً)"))
        assertTrue(caption.contains("آخر 10 عمليات · بتاريخ 03/11/2026"))
        assertTrue(caption.contains("من: عمر الحومي 773712030"))
        assertTrue(caption.contains("تطبيق المسرّب | م. عمر الحومي"))
        assertTrue(caption.contains("mosarib.tubbasoft.com"))
        assertTrue(caption.contains("wa.me/967773712030"))
    }

    @Test
    fun testVoiceStatementCaptionCreditWithoutPhone() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3)
        }
        val caption = SpeechTextBuilder.buildVoiceStatementCaption(
            customerName = "سالم علي",
            balance = -5000.0,
            isWellOwner = false,
            operationCount = 5,
            date = cal.time,
            distributorName = "مشروع المياه",
            distributorPhone = "",
            currencySymbol = "ر.ي"
        )

        assertTrue(caption.contains("🔊 كشف حساب صوتي: سالم علي"))
        assertTrue(caption.contains("الرصيد: له 5,000 ر.ي (خمسة آلاف ريالاً)"))
        assertTrue(caption.contains("آخر 5 عمليات · بتاريخ 03/11/2026"))
        assertTrue(caption.contains("من: مشروع المياه\n") || caption.contains("من: مشروع المياه\r\n"))
        assertFalse(caption.contains("من: مشروع المياه "))
        assertTrue(caption.contains("mosarib.tubbasoft.com"))
    }

    @Test
    fun testVoiceStatementCaptionSettled() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3)
        }
        val caption = SpeechTextBuilder.buildVoiceStatementCaption(
            customerName = "ياسر فهد",
            balance = 0.0,
            isWellOwner = false,
            operationCount = 3,
            date = cal.time,
            distributorName = "عمر الحومي",
            distributorPhone = "773712030",
            currencySymbol = "ر.ي"
        )

        assertTrue(caption.contains("🔊 كشف حساب صوتي: ياسر فهد"))
        assertTrue(caption.contains("الرصيد: مسدد"))
        assertFalse(caption.contains("عليه"))
        assertFalse(caption.contains("له"))
        assertTrue(caption.contains("آخر 3 عمليات · بتاريخ 03/11/2026"))
        assertTrue(caption.contains("mosarib.tubbasoft.com"))
    }
}
