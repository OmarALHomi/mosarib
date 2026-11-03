package com.example.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VoicePhraseTableTest {

    private val fixedNow: Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 3, 15, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val knownNames = listOf(
        "احمد علي",
        "يوسف قاسم",
        "يوسف قاسم سعيد",
        "حارث سعيد بن علي",
        "حارث سعيد"
    )

    // =========================================================================
    // Section 4: Phrase Table Tests (22 core phrases)
    // =========================================================================

    @Test
    fun testSection4Phrases() {
        // 1. 50,000 ريال مع يوسف قاسم سعيد -> EXPENSE, name: يوسف قاسم سعيد, amount: 50000
        val p1 = VoiceCommandParser.parse("50,000 ريال مع يوسف قاسم سعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p1.type)
        assertEquals("يوسف قاسم سعيد", p1.customerQuery)
        assertEquals(50000.0, p1.amount!!, 0.01)

        // 2. 50 الف مع يونس قاسم -> EXPENSE, name: يونس قاسم, amount: 50000
        val p2 = VoiceCommandParser.parse("50 الف مع يونس قاسم", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p2.type)
        assertEquals("يونس قاسم", p2.customerQuery)
        assertEquals(50000.0, p2.amount!!, 0.01)

        // 3. 20 الف ريال من حارث سعيد -> RECEIPT, name: حارث سعيد, amount: 20000
        val p3 = VoiceCommandParser.parse("20 الف ريال من حارث سعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p3.type)
        assertEquals("حارث سعيد", p3.customerQuery)
        assertEquals(20000.0, p3.amount!!, 0.01)

        // 4. 20 الف ريال من احمد عبده لي -> RECEIPT, name: احمد عبده, amount: 20000
        val p4 = VoiceCommandParser.parse("20 الف ريال من احمد عبده لي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p4.type)
        assertEquals("احمد عبده", p4.customerQuery)
        assertEquals(20000.0, p4.amount!!, 0.01)

        // 5. وصل 20 الف لحارث سعيد -> RECEIPT, name: حارث سعيد, amount: 20000
        val p5 = VoiceCommandParser.parse("وصل 20 الف لحارث سعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p5.type)
        assertEquals("حارث سعيد", p5.customerQuery)
        assertEquals(20000.0, p5.amount!!, 0.01)

        // 6. سجل 20 الف لحساب حارث سعيد -> RECEIPT, name: حارث سعيد, amount: 20000
        val p6 = VoiceCommandParser.parse("سجل 20 الف لحساب حارث سعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p6.type)
        assertEquals("حارث سعيد", p6.customerQuery)
        assertEquals(20000.0, p6.amount!!, 0.01)

        // 7. حارث سعيد واصل منه 20 الف -> RECEIPT, name: حارث سعيد, amount: 20000
        val p7 = VoiceCommandParser.parse("حارث سعيد واصل منه 20 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p7.type)
        assertEquals("حارث سعيد", p7.customerQuery)
        assertEquals(20000.0, p7.amount!!, 0.01)

        // 8. ثلاث ساعات ونص مع احمد علي من 10 الف -> SESSION, name: احمد علي, min: 210, rate: 10000
        val p8 = VoiceCommandParser.parse("ثلاث ساعات ونص مع احمد علي من 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p8.type)
        assertEquals("احمد علي", p8.customerQuery)
        assertEquals(210, p8.durationMinutes)
        assertEquals(10000.0, p8.ratePerHour!!, 0.01)

        // 9. ثلاث ساعات ونص مع احمد علي من 10,000 ومسلم 20,000 -> SESSION, paid: 20000
        val p9 = VoiceCommandParser.parse("ثلاث ساعات ونص مع احمد علي من 10,000 ومسلم 20,000", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p9.type)
        assertEquals("احمد علي", p9.customerQuery)
        assertEquals(210, p9.durationMinutes)
        assertEquals(10000.0, p9.ratePerHour!!, 0.01)
        assertEquals(20000.0, p9.paid!!, 0.01)

        // 10. ثلاث وربع مع احمد سعيد من 10 الف وادي 20 الف -> SESSION, min: 195, rate: 10000, paid: 20000
        val p10 = VoiceCommandParser.parse("ثلاث وربع مع احمد سعيد من 10 الف وادي 20 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p10.type)
        assertEquals("احمد سعيد", p10.customerQuery)
        assertEquals(195, p10.durationMinutes)
        assertEquals(10000.0, p10.ratePerHour!!, 0.01)
        assertEquals(20000.0, p10.paid!!, 0.01)

        // 11. احمد علي ساعتين من 8 الف -> SESSION, name: احمد علي, min: 120, rate: 8000
        val p11 = VoiceCommandParser.parse("احمد علي ساعتين من 8 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p11.type)
        assertEquals("احمد علي", p11.customerQuery)
        assertEquals(120, p11.durationMinutes)
        assertEquals(8000.0, p11.ratePerHour!!, 0.01)

        // 12. ساعتين ونص احمد علي 10 الف 5 الف -> SESSION, min: 150, rate: 10000, paid: 5000
        val p12 = VoiceCommandParser.parse("ساعتين ونص احمد علي 10 الف 5 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p12.type)
        assertEquals("احمد علي", p12.customerQuery)
        assertEquals(150, p12.durationMinutes)
        assertEquals(10000.0, p12.ratePerHour!!, 0.01)
        assertEquals(5000.0, p12.paid!!, 0.01)

        // 13. سقي محمد صالح ساعه ونص بسعر 12 الف -> SESSION, name: محمد صالح, min: 90, rate: 12000
        val p13 = VoiceCommandParser.parse("سقي محمد صالح ساعه ونص بسعر 12 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p13.type)
        assertEquals("محمد صالح", p13.customerQuery)
        assertEquals(90, p13.durationMinutes)
        assertEquals(12000.0, p13.ratePerHour!!, 0.01)

        // 14. قبض من محمد صالح خمسين الف -> RECEIPT, name: محمد صالح, amount: 50000
        val p14 = VoiceCommandParser.parse("قبض من محمد صالح خمسين الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p14.type)
        assertEquals("محمد صالح", p14.customerQuery)
        assertEquals(50000.0, p14.amount!!, 0.01)

        // 15. خمسين الف من محمد صالح -> RECEIPT, name: محمد صالح, amount: 50000
        val p15 = VoiceCommandParser.parse("خمسين الف من محمد صالح", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p15.type)
        assertEquals("محمد صالح", p15.customerQuery)
        assertEquals(50000.0, p15.amount!!, 0.01)

        // 16. صرف 30 الف ديزل -> EXPENSE, amount: 30000, category: ديزل
        val p16 = VoiceCommandParser.parse("صرف 30 الف ديزل", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p16.type)
        assertEquals(30000.0, p16.amount!!, 0.01)
        assertEquals("ديزل", p16.category)

        // 17. 15 الف صيانه -> EXPENSE, amount: 15000, category: صيانة
        val p17 = VoiceCommandParser.parse("15 الف صيانه", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p17.type)
        assertEquals(15000.0, p17.amount!!, 0.01)
        assertTrue(p17.category == "صيانة" || p17.category == "صيانه")

        // 18. اعطيت يوسف 10 الف -> EXPENSE, name: يوسف, amount: 10000
        val p18 = VoiceCommandParser.parse("اعطيت يوسف 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p18.type)
        assertEquals("يوسف", p18.customerQuery)
        assertEquals(10000.0, p18.amount!!, 0.01)

        // 19. خصم 5 الف لاحمد علي -> DISCOUNT, name: احمد علي, amount: 5000
        val p19 = VoiceCommandParser.parse("خصم 5 الف لاحمد علي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.DISCOUNT, p19.type)
        assertEquals("احمد علي", p19.customerQuery)
        assertEquals(5000.0, p19.amount!!, 0.01)

        // 20. اشتريت ثلاث ساعات من صاحب البئر سالم من 7 الف -> PURCHASE, name: سالم, min: 180, rate: 7000
        val p20 = VoiceCommandParser.parse("اشتريت ثلاث ساعات من صاحب البئر سالم من 7 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.PURCHASE, p20.type)
        assertTrue(p20.customerQuery?.contains("سالم") == true)
        assertEquals(180, p20.durationMinutes)
        assertEquals(7000.0, p20.ratePerHour!!, 0.01)

        // 21. مية الف من ابو محمد -> RECEIPT, name: ابو محمد, amount: 100000
        val p21 = VoiceCommandParser.parse("مية الف من ابو محمد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p21.type)
        assertEquals("ابو محمد", p21.customerQuery)
        assertEquals(100000.0, p21.amount!!, 0.01)

        // 22. ألفين ريال مع علي -> EXPENSE, name: علي, amount: 2000
        val p22 = VoiceCommandParser.parse("ألفين ريال مع علي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p22.type)
        assertEquals("علي", p22.customerQuery)
        assertEquals(2000.0, p22.amount!!, 0.01)
    }

    // =========================================================================
    // Section 6: Most Common Patterns (11 phrases)
    // =========================================================================

    @Test
    fun testSection6CommonPatterns() {
        // 1. مع احمد علي ثلاث ساعات من 10 الف ريال ومسلم 20 الف حق الشهر الماضي
        val p1 = VoiceCommandParser.parse("مع احمد علي ثلاث ساعات من 10 الف ريال ومسلم 20 الف حق الشهر الماضي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p1.type)
        assertEquals("احمد علي", p1.customerQuery)
        assertEquals(180, p1.durationMinutes)
        assertEquals(10000.0, p1.ratePerHour!!, 0.01)
        assertEquals(20000.0, p1.paid!!, 0.01)
        assertTrue(p1.notes?.contains("حق الشهر الماضي") == true)

        // 2. مع احمد علي ساعتين ونص من 8000
        val p2 = VoiceCommandParser.parse("مع احمد علي ساعتين ونص من 8000", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p2.type)
        assertEquals("احمد علي", p2.customerQuery)
        assertEquals(150, p2.durationMinutes)
        assertEquals(8000.0, p2.ratePerHour!!, 0.01)
        assertTrue(p2.paid == null || p2.paid == 0.0)

        // 3. مع احمد علي ساعتين من 10 الف ومسلم كامل
        val p3 = VoiceCommandParser.parse("مع احمد علي ساعتين من 10 الف ومسلم كامل", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p3.type)
        assertEquals("احمد علي", p3.customerQuery)
        assertEquals(120, p3.durationMinutes)
        assertEquals(10000.0, p3.ratePerHour!!, 0.01)
        assertTrue(p3.paidFull)

        // 4. مع احمد علي من الساعه 12 لا الساعه 2 من 10 الف
        val p4 = VoiceCommandParser.parse("مع احمد علي من الساعه 12 لا الساعه 2 من 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p4.type)
        assertEquals("احمد علي", p4.customerQuery)
        assertEquals(120, p4.durationMinutes)
        assertEquals(10000.0, p4.ratePerHour!!, 0.01)
        assertNotNull(p4.startMillis)
        assertNotNull(p4.endMillis)

        // 5. مع احمد علي من الساعه 8 بالصبح الى الساعه 11 ونص مسلم كامل
        val p5 = VoiceCommandParser.parse("مع احمد علي من الساعه 8 بالصبح الى الساعه 11 ونص مسلم كامل", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p5.type)
        assertEquals("احمد علي", p5.customerQuery)
        assertEquals(210, p5.durationMinutes)
        assertTrue(p5.paidFull)
        assertNotNull(p5.startMillis)
        assertNotNull(p5.endMillis)

        // 6. مع احمد علي من الساعه 9 بالليل لا الساعه 1
        val p6 = VoiceCommandParser.parse("مع احمد علي من الساعه 9 بالليل لا الساعه 1", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p6.type)
        assertEquals("احمد علي", p6.customerQuery)
        assertEquals(240, p6.durationMinutes)
        assertNotNull(p6.startMillis)
        assertNotNull(p6.endMillis)

        // 7. مع احمد علي من 1 لا 2 ونص من 10 الف ومسلم 5 الف باقي امس
        val p7 = VoiceCommandParser.parse("مع احمد علي من 1 لا 2 ونص من 10 الف ومسلم 5 الف باقي امس", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, p7.type)
        assertEquals("احمد علي", p7.customerQuery)
        assertEquals(90, p7.durationMinutes)
        assertEquals(10000.0, p7.ratePerHour!!, 0.01)
        assertEquals(5000.0, p7.paid!!, 0.01)
        assertTrue(p7.notes?.contains("باقي امس") == true)

        // 8. مع يوسف قاسم 50 الف سلفه للعيد
        val p8 = VoiceCommandParser.parse("مع يوسف قاسم 50 الف سلفه للعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p8.type)
        assertEquals("يوسف قاسم", p8.customerQuery)
        assertEquals(50000.0, p8.amount!!, 0.01)
        assertTrue(p8.notes?.contains("سلفه للعيد") == true)

        // 9. 50 الف ريال مع يوسف قاسم سعيد حق العمال
        val p9 = VoiceCommandParser.parse("50 الف ريال مع يوسف قاسم سعيد حق العمال", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p9.type)
        assertEquals("يوسف قاسم سعيد", p9.customerQuery)
        assertEquals(50000.0, p9.amount!!, 0.01)
        assertTrue(p9.notes?.contains("حق العمال") == true)

        // 10. 30 الف من حارث سعيد بن علي باقي حساب الشهر
        val p10 = VoiceCommandParser.parse("30 الف من حارث سعيد بن علي باقي حساب الشهر", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, p10.type)
        assertEquals("حارث سعيد بن علي", p10.customerQuery)
        assertEquals(30000.0, p10.amount!!, 0.01)
        assertTrue(p10.notes?.contains("باقي حساب الشهر") == true)

        // 11. 20 الف ريال حق الديزل من محطة صالح بن علي
        val p11 = VoiceCommandParser.parse("20 الف ريال حق الديزل من محطة صالح بن علي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, p11.type)
        assertNull(p11.customerQuery)
        assertEquals(20000.0, p11.amount!!, 0.01)
        assertEquals("ديزل", p11.category)
        assertTrue(p11.notes?.contains("حق الديزل من محطة صالح بن علي") == true)
    }

    // =========================================================================
    // Section 6: Day and Date Tests (6 phrases)
    // =========================================================================

    @Test
    fun testSection6DayAndDateTests() {
        // 1. مع احمد علي من امس الساعه 8 بالليل لا الساعه 6 بالصبح من 10 الف -> 600 min
        val d1 = VoiceCommandParser.parse("مع احمد علي من امس الساعه 8 بالليل لا الساعه 6 بالصبح من 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d1.type)
        assertEquals("احمد علي", d1.customerQuery)
        assertEquals(600, d1.durationMinutes)
        assertEquals(10000.0, d1.ratePerHour!!, 0.01)

        // 2. مع احمد علي 14 ساعه من قبل امس الساعه 6 بالصبح الى الساعه 8 بالليل ومسلم 50 الف -> 840 min
        val d2 = VoiceCommandParser.parse("مع احمد علي 14 ساعه من قبل امس الساعه 6 بالصبح الى الساعه 8 بالليل ومسلم 50 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d2.type)
        assertEquals("احمد علي", d2.customerQuery)
        assertEquals(840, d2.durationMinutes)
        assertEquals(50000.0, d2.paid!!, 0.01)

        // 3. مع احمد علي من يوم الخميس الساعه 10 بالليل الى يوم الجمعه الساعه 4 العصر من 10 الف -> 1080 min
        val d3 = VoiceCommandParser.parse("مع احمد علي من يوم الخميس الساعه 10 بالليل الى يوم الجمعه الساعه 4 العصر من 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d3.type)
        assertEquals("احمد علي", d3.customerQuery)
        assertEquals(1080, d3.durationMinutes)
        assertEquals(10000.0, d3.ratePerHour!!, 0.01)

        // 4. مع احمد علي من يوم 25 الساعه 7 بالصبح لااا يوم 26 الساعه 7 بالصبح -> 1440 min
        val d4 = VoiceCommandParser.parse("مع احمد علي من يوم 25 الساعه 7 بالصبح لااا يوم 26 الساعه 7 بالصبح", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d4.type)
        assertEquals("احمد علي", d4.customerQuery)
        assertEquals(1440, d4.durationMinutes)

        // 5. مع احمد علي من 25/10 الساعه 7 الصبح لا الساعه 11 الصبح -> 240 min
        val d5 = VoiceCommandParser.parse("مع احمد علي من 25/10 الساعه 7 الصبح لا الساعه 11 الصبح", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d5.type)
        assertEquals("احمد علي", d5.customerQuery)
        assertEquals(240, d5.durationMinutes)

        // 6. مع احمد علي 10 ساعات من امس الساعه 8 بالصبح لا الساعه 10 بالليل -> 840 min + warning banner
        val d6 = VoiceCommandParser.parse("مع احمد علي 10 ساعات من امس الساعه 8 بالصبح لا الساعه 10 بالليل", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, d6.type)
        assertEquals("احمد علي", d6.customerQuery)
        assertEquals(840, d6.durationMinutes)
        assertNotNull(d6.warningBanner)
        assertTrue(d6.warningBanner!!.contains("10") && d6.warningBanner!!.contains("14"))
    }

    // =========================================================================
    // Section 7.11: More Table Rows (13 phrases)
    // =========================================================================

    @Test
    fun testSection711Phrases() {
        // 1. مع احمد علي تلات ساعات من عشر تلاف وسلم خمستاشر الف
        val r1 = VoiceCommandParser.parse("مع احمد علي تلات ساعات من عشر تلاف وسلم خمستاشر الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r1.type)
        assertEquals("احمد علي", r1.customerQuery)
        assertEquals(180, r1.durationMinutes)
        assertEquals(10000.0, r1.ratePerHour!!, 0.01)
        assertEquals(15000.0, r1.paid!!, 0.01)

        // 2. مع احمد علي ساعتين الا ربع من 10 الف دين
        val r2 = VoiceCommandParser.parse("مع احمد علي ساعتين الا ربع من 10 الف دين", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r2.type)
        assertEquals("احمد علي", r2.customerQuery)
        assertEquals(105, r2.durationMinutes)
        assertEquals(10000.0, r2.ratePerHour!!, 0.01)
        assertTrue(r2.paid == null || r2.paid == 0.0)

        // 3. مع احمد علي ساعه وتلت من 9 الاف نقدا
        val r3 = VoiceCommandParser.parse("مع احمد علي ساعه وتلت من 9 الاف نقدا", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r3.type)
        assertEquals("احمد علي", r3.customerQuery)
        assertEquals(80, r3.durationMinutes)
        assertEquals(9000.0, r3.ratePerHour!!, 0.01)
        assertTrue(r3.paidFull)

        // 4. مع احمد علي من 10 الف لا قصدي 12 الف ساعتين
        val r4 = VoiceCommandParser.parse("مع احمد علي من 10 الف لا قصدي 12 الف ساعتين", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r4.type)
        assertEquals("احمد علي", r4.customerQuery)
        assertEquals(120, r4.durationMinutes)
        assertEquals(12000.0, r4.ratePerHour!!, 0.01)

        // 5. مع احمد علي لا قصدي يوسف قاسم ساعتين من 10 الف
        val r5 = VoiceCommandParser.parse("مع احمد علي لا قصدي يوسف قاسم ساعتين من 10 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r5.type)
        assertEquals("يوسف قاسم", r5.customerQuery)
        assertEquals(120, r5.durationMinutes)
        assertEquals(10000.0, r5.ratePerHour!!, 0.01)

        // 6. مع احمد علي من الساعه 2 الا ربع لين الساعه 5 العصر
        val r6 = VoiceCommandParser.parse("مع احمد علي من الساعه 2 الا ربع لين الساعه 5 العصر", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r6.type)
        assertEquals("احمد علي", r6.customerQuery)
        assertEquals(195, r6.durationMinutes)

        // 7. مع احمد علي من نص الليل لا الفجر الساعه 4
        val r7 = VoiceCommandParser.parse("مع احمد علي من نص الليل لا الفجر الساعه 4", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r7.type)
        assertEquals("احمد علي", r7.customerQuery)
        assertEquals(240, r7.durationMinutes)

        // 8. خمسه آلاف ونص من حارث سعيد
        val r8 = VoiceCommandParser.parse("خمسه آلاف ونص من حارث سعيد", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, r8.type)
        assertEquals("حارث سعيد", r8.customerQuery)
        assertEquals(5500.0, r8.amount!!, 0.01)

        // 9. ربع مليون من حارث سعيد بن علي
        val r9 = VoiceCommandParser.parse("ربع مليون من حارث سعيد بن علي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, r9.type)
        assertEquals("حارث سعيد بن علي", r9.customerQuery)
        assertEquals(250000.0, r9.amount!!, 0.01)

        // 10. ٢٠٠٠٠ من احمد علي
        val r10 = VoiceCommandParser.parse("٢٠٠٠٠ من احمد علي", knownNames, fixedNow)
        assertEquals(VoiceCommandType.RECEIPT, r10.type)
        assertEquals("احمد علي", r10.customerQuery)
        assertEquals(20000.0, r10.amount!!, 0.01)

        // 11. مع يوسف قاسم عشرين الف وخمسميه حق قطع غيار
        val r11 = VoiceCommandParser.parse("مع يوسف قاسم عشرين الف وخمسميه حق قطع غيار", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, r11.type)
        assertEquals("يوسف قاسم", r11.customerQuery)
        assertEquals(20500.0, r11.amount!!, 0.01)
        assertTrue(r11.notes?.contains("قطع غيار") == true)

        // 12. 10 الاف ديزل و5 الاف زيت
        val r12 = VoiceCommandParser.parse("10 الاف ديزل و5 الاف زيت", knownNames, fixedNow)
        assertEquals(VoiceCommandType.EXPENSE, r12.type)
        assertEquals(10000.0, r12.amount!!, 0.01)
        assertEquals("ديزل", r12.category)
        assertNotNull(r12.warningBanner)
        assertTrue(r12.warningBanner!!.contains("عمليتين"))

        // 13. سقي احمد علي ساعتين من 10 الف وقبض من حارث سعيد 5 الف
        val r13 = VoiceCommandParser.parse("سقي احمد علي ساعتين من 10 الف وقبض من حارث سعيد 5 الف", knownNames, fixedNow)
        assertEquals(VoiceCommandType.SESSION, r13.type)
        assertEquals("احمد علي", r13.customerQuery)
        assertEquals(120, r13.durationMinutes)
        assertEquals(10000.0, r13.ratePerHour!!, 0.01)
        assertNotNull(r13.warningBanner)
        assertTrue(r13.warningBanner!!.contains("عمليتين"))
    }

    // =========================================================================
    // Section 7.10: Generated Tests (200+ variations across A, B, B2, C, D, E)
    // =========================================================================

    @Test
    fun testGeneratedPatternVariants() {
        var patternACount = 0
        var patternBCount = 0
        var patternB2Count = 0
        var patternCCount = 0
        var patternDCount = 0
        var patternECount = 0

        // 1. Pattern A: SESSION by hours
        val durationsA = listOf(
            Pair("ساعتين", 120),
            Pair("ساعتين ونص", 150),
            Pair("ثلاث ساعات", 180),
            Pair("ساعه ونص", 90)
        )
        val ratesA = listOf("10 الف", "10000", "عشر تلاف")
        val paidMarkersA = listOf("ومسلم", "وسلم", "ودفع")
        val currenciesA = listOf("", "ريال", "ريال يمني")

        for ((durStr, durMin) in durationsA) {
            for (rateStr in ratesA) {
                for (curr in currenciesA) {
                    for (marker in paidMarkersA) {
                        val phrase = "مع احمد علي $durStr من $rateStr $curr $marker 5000"
                        val cmd = VoiceCommandParser.parse(phrase, knownNames, fixedNow)
                        assertEquals(VoiceCommandType.SESSION, cmd.type)
                        assertEquals("احمد علي", cmd.customerQuery)
                        assertEquals(durMin, cmd.durationMinutes)
                        assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
                        assertEquals(5000.0, cmd.paid!!, 0.01)
                        patternACount++
                    }
                }
            }
        }

        // 2. Pattern B: SESSION time range
        val joinersB = listOf("الى", "لا", "لين", "حتى")
        val hourPrefixesB = listOf("الساعه", "")
        for (j in joinersB) {
            for (hp in hourPrefixesB) {
                for (rateStr in listOf("من 10 الف", "بسعر 10000", "من عشره الف")) {
                    val hpPrefix = if (hp.isNotBlank()) "$hp " else ""
                    val phrase = "مع احمد علي من ${hpPrefix}12 $j ${hpPrefix}2 $rateStr"
                    val cmd = VoiceCommandParser.parse(phrase, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.SESSION, cmd.type)
                    assertEquals("احمد علي", cmd.customerQuery)
                    assertEquals(120, cmd.durationMinutes)
                    assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
                    patternBCount++
                }
            }
        }

        // 3. Pattern B2: Long irrigation with day words
        val dayStartsB2 = listOf("امس", "البارحه", "البارح")
        val dayEndJoinersB2 = listOf("لا", "الى", "لين")
        for (day in dayStartsB2) {
            for (j in dayEndJoinersB2) {
                for (rateStr in listOf("من 10 الف", "من 10000")) {
                    val phrase = "مع احمد علي من $day الساعه 8 بالليل $j الساعه 6 بالصبح $rateStr"
                    val cmd = VoiceCommandParser.parse(phrase, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.SESSION, cmd.type)
                    assertEquals("احمد علي", cmd.customerQuery)
                    assertEquals(600, cmd.durationMinutes)
                    assertEquals(10000.0, cmd.ratePerHour!!, 0.01)
                    patternB2Count++
                }
            }
        }

        // 4. Pattern C: Payment voucher (EXPENSE) to a person
        val amountsC = listOf(
            Pair("50 الف", 50000.0),
            Pair("50000", 50000.0),
            Pair("خمسين الف", 50000.0),
            Pair("عشرين الف وخمسميه", 20500.0)
        )
        val namesC = listOf("يوسف قاسم", "يوسف قاسم سعيد")
        for ((amtStr, amtVal) in amountsC) {
            for (name in namesC) {
                for (curr in listOf("", "ريال")) {
                    // "مع {name} {amount}"
                    val p1 = "مع $name $amtStr $curr حق العمال"
                    val c1 = VoiceCommandParser.parse(p1, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.EXPENSE, c1.type)
                    assertEquals(name, c1.customerQuery)
                    assertEquals(amtVal, c1.amount!!, 0.01)
                    patternCCount++

                    // "{amount} مع {name}"
                    val p2 = "$amtStr $curr مع $name حق العمال"
                    val c2 = VoiceCommandParser.parse(p2, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.EXPENSE, c2.type)
                    assertEquals(name, c2.customerQuery)
                    assertEquals(amtVal, c2.amount!!, 0.01)
                    patternCCount++
                }
            }
        }

        // 5. Pattern D: Receipt voucher (RECEIPT)
        val amountsD = listOf(
            Pair("20 الف", 20000.0),
            Pair("20000", 20000.0),
            Pair("عشرين الف", 20000.0),
            Pair("خمسه آلاف ونص", 5500.0)
        )
        val receiptVerbsD = listOf("قبضت", "استلمت", "وصل", "سدد", "")
        for ((amtStr, amtVal) in amountsD) {
            for (verb in receiptVerbsD) {
                for (curr in listOf("", "ريال")) {
                    val prefix = if (verb.isNotBlank()) "$verb " else ""
                    val phrase = "$prefix$amtStr $curr من حارث سعيد"
                    val cmd = VoiceCommandParser.parse(phrase, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.RECEIPT, cmd.type)
                    assertEquals("حارث سعيد", cmd.customerQuery)
                    assertEquals(amtVal, cmd.amount!!, 0.01)
                    patternDCount++
                }
            }
        }

        // 6. Pattern E: General expense (no person)
        val expenseAmtsE = listOf(
            Pair("30 الف", 30000.0),
            Pair("30000", 30000.0),
            Pair("ثلاثين الف", 30000.0)
        )
        val categoriesE = listOf("ديزل", "صيانه", "زيت", "كهرباء")
        for ((amtStr, amtVal) in expenseAmtsE) {
            for (cat in categoriesE) {
                for (verb in listOf("صرف ", "")) {
                    val phrase = "$verb$amtStr ريال $cat"
                    val cmd = VoiceCommandParser.parse(phrase, knownNames, fixedNow)
                    assertEquals(VoiceCommandType.EXPENSE, cmd.type)
                    assertNull(cmd.customerQuery)
                    assertEquals(amtVal, cmd.amount!!, 0.01)
                    patternECount++
                }
            }
        }

        val totalGenerated = patternACount + patternBCount + patternB2Count + patternCCount + patternDCount + patternECount
        println("Generated test count: Total=$totalGenerated (A=$patternACount, B=$patternBCount, B2=$patternB2Count, C=$patternCCount, D=$patternDCount, E=$patternECount)")
        assertTrue("Generated tests must be at least 200, found $totalGenerated", totalGenerated >= 200)
    }

    // =========================================================================
    // Review Round 1 Bug Fixes (Claude 2026-11-04)
    // =========================================================================

    @Test
    fun testReviewRound1BugFixes() {
        // 1. "بالليل" after midnight (10 بالليل -> 2 بالليل = 240 min)
        val b1 = VoiceCommandParser.parse(
            "مع احمد علي من امس الساعه 10 بالليل لا الساعه 2 بالليل من 10 الف",
            knownNames,
            fixedNow
        )
        assertEquals(VoiceCommandType.SESSION, b1.type)
        assertEquals("احمد علي", b1.customerQuery)
        assertEquals(240, b1.durationMinutes)
        assertEquals(10000.0, b1.ratePerHour!!, 0.01)
        assertNull(b1.paid)

        // 2. "بالليل" after midnight same night (11 بالليل -> 3 بالليل = 240 min)
        val b2 = VoiceCommandParser.parse(
            "مع احمد علي من الساعه 11 بالليل لا الساعه 3 بالليل",
            knownNames,
            fixedNow
        )
        assertEquals(VoiceCommandType.SESSION, b2.type)
        assertEquals("احمد علي", b2.customerQuery)
        assertEquals(240, b2.durationMinutes)

        // 3. Paid amount followed by notes starting with "وقال"
        val b3 = VoiceCommandParser.parse(
            "مع يوسف قاسم ساعتين من 10 الف وسلم 5 الف وقال الباقي بكره",
            knownNames,
            fixedNow
        )
        assertEquals(VoiceCommandType.SESSION, b3.type)
        assertEquals("يوسف قاسم", b3.customerQuery)
        assertEquals(120, b3.durationMinutes)
        assertEquals(10000.0, b3.ratePerHour!!, 0.01)
        assertEquals(5000.0, b3.paid!!, 0.01)
        assertTrue(b3.notes?.contains("الباقي بكره") == true)

        // 4. Paid amount followed by notes starting with "وباقي"
        val b4 = VoiceCommandParser.parse(
            "مع احمد علي ساعتين من 10 الف ودفع 5 الف وباقي عليه",
            knownNames,
            fixedNow
        )
        assertEquals(VoiceCommandType.SESSION, b4.type)
        assertEquals("احمد علي", b4.customerQuery)
        assertEquals(120, b4.durationMinutes)
        assertEquals(10000.0, b4.ratePerHour!!, 0.01)
        assertEquals(5000.0, b4.paid!!, 0.01)
        assertTrue(b4.notes?.contains("باقي عليه") == true)
    }
}
