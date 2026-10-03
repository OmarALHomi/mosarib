package com.baynana.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٢ (الخطة v6 §4.1 و§15) بعد البند المعتمد 2026-10-03:
 * كل تخزين وحساب بالوحدة الصغرى (فلس)، والإدخال بالريال، والعرض بالريال، والإرسال نصًّا.
 * هذه الاختبارات تعمل على JVM بلا محاكي.
 */
class MoneyTest {

    // --------------------------------------------------------- الوحدة الصغرى

    @Test
    fun `one riyal is one hundred fils in every currency`() {
        assertEquals(100L, Currency.YER_NEW.minorPerUnit)
        assertEquals(100L, Currency.YER_OLD.minorPerUnit)
        assertEquals(100L, Currency.SAR.minorPerUnit)
        assertEquals(100L, Currency.USD.minorPerUnit)
        assertEquals(1_500_000L, Money.ofMajor(15_000, Currency.YER_NEW).minor)
    }

    @Test
    fun `money is never stored as a fractional type`() {
        // حارس تصميمي: النوع المخزّن صحيح 64-بت، لا Double ولا Float.
        val fieldType = Money::class.java.getDeclaredField("minor").type
        assertEquals("حقل المال يجب أن يكون Long", Long::class.javaPrimitiveType, fieldType)
        assertFalse(fieldType == Double::class.javaPrimitiveType)
        assertFalse(fieldType == Float::class.javaPrimitiveType)
    }

    @Test
    fun `fils precision survives a division that would break doubles`() {
        // 10,000 ريال ÷ 3 = 3,333.33 لكل جزء: لا يمكن تمثيله بـDouble بلا خطأ تراكمي.
        val amount = Money.ofMajor(10_000, Currency.YER_NEW)
        val aThird = amount.times(1, 3)
        assertEquals(333_333L, aThird.minor)
        val total = aThird * 3
        assertEquals(999_999L, total.minor)
        // الفرق فلس واحد فقط بعد التقريب لكل جزء، ولا كسور عائمة في أي خطوة.
        assertEquals(Money.ofMajor(10_000, Currency.YER_NEW).minus(total).minor, 1L)
    }

    @Test
    fun `accumulated cents never drift as they do with floating point`() {
        // 0.10 + 0.20 = 0.30 بالضبط: الحالة الشهيرة التي تكشف Double.
        var sum = Money.zero(Currency.YER_NEW)
        repeat(3) { sum += Money.ofMinor(10, Currency.YER_NEW) } // 30 فلسًا
        assertEquals(30L, sum.minor)
        assertEquals(0L, (sum - Money.ofMinor(30, Currency.YER_NEW)).minor)
    }

    // ------------------------------------------------------------- البناء

    @Test
    fun `conversion from riyals to fils is exact`() {
        assertEquals(100L, Money.ofMajor(1, Currency.YER_NEW).minor)
        assertEquals(1_500_000L, Money.ofMajor(15_000, Currency.YER_NEW).minor)
        assertEquals(1_050L, Money.ofMajor(10, Currency.SAR).minor + 50L)
    }

    @Test
    fun `negative amounts are rejected at construction`() {
        assertThrows(MoneyException::class.java) { Money.ofMinor(-1, Currency.YER_NEW) }
        assertThrows(MoneyException::class.java) { Money.ofMajor(-5, Currency.SAR) }
    }

    @Test
    fun `amounts above the ceiling are rejected`() {
        assertThrows(MoneyException::class.java) { Money.ofMinor(Money.MAX_MINOR + 1, Currency.YER_NEW) }
    }

    @Test
    fun `the ceiling covers big settlements and their totals`() {
        // السقف 1e12 فلسًا = 10 مليارات ريال: يتجاوز أي صلح منفرد و100 صلح من أكبر الصلوح.
        val bigDeal = Money.ofMajor(50_000_000, Currency.YER_NEW)
        var total = Money.zero(Currency.YER_NEW)
        repeat(100) { total += bigDeal }
        assertEquals(500_000_000_000L, total.minor)
    }

    // ------------------------------------------------------------ العمليات

    @Test
    fun `addition and subtraction stay exact`() {
        val price = Money.ofMajor(10_000, Currency.YER_NEW)
        val paid = Money.ofMajor(3_000, Currency.YER_NEW)
        assertEquals(1_300_000L, (price + paid).minor)
        assertEquals(700_000L, (price - paid).minor)
        assertTrue(Money.zero(Currency.YER_NEW).isZero)
    }

    @Test
    fun `subtraction below zero is refused instead of silently flipping sign`() {
        val debt = Money.ofMajor(3_000, Currency.YER_NEW)
        val payment = Money.ofMajor(5_000, Currency.YER_NEW)
        val error = assertThrows(MoneyException::class.java) { debt - payment }
        assertTrue(error.message!!.contains("سالب"))
    }

    @Test
    fun `mixing two currencies is refused`() {
        val riyal = Money.ofMajor(5_000, Currency.YER_NEW)
        val dollar = Money.ofMajor(10, Currency.USD)
        assertThrows(MoneyException::class.java) { riyal + dollar }
        assertThrows(MoneyException::class.java) { riyal - dollar }
        assertThrows(MoneyException::class.java) { riyal.compareTo(dollar) }
    }

    @Test
    fun `overflow is reported not wrapped`() {
        val big = Money.ofMinor(Money.MAX_MINOR, Currency.YER_NEW)
        assertThrows(MoneyException::class.java) { big + Money.ofMinor(1, Currency.YER_NEW) }
        assertThrows(MoneyException::class.java) { big * 2 }
    }

    // -------------------------------------------------------------- التقريب

    @Test
    fun `hourly rate times minutes rounds half up in fils`() {
        val perHour = Money.ofMajor(5_000, Currency.YER_NEW)
        assertEquals(375_000L, perHour.times(45, 60).minor) // 3750 ريال
        assertEquals(58_333L, perHour.times(7, 60).minor)   // 583.33 ريال
        assertEquals(250_000L, perHour.times(30, 60).minor) // 2500 ريال
        assertEquals(4_167L, perHour.times(1, 120).minor)   // 41.67 ريال
    }

    @Test
    fun `rounding keeps the second decimal when it matters`() {
        val amount = Money.ofMinor(1_000, Currency.YER_NEW) // 10 ريال
        val third = amount.times(1, 3) // 3.3333 ريال → 333 فلسًا
        assertEquals(333L, third.minor)
        assertEquals("3.33 ر.ي", third.format())
    }

    @Test
    fun `zero and negative factors are handled explicitly`() {
        val amount = Money.ofMajor(100, Currency.YER_NEW)
        assertEquals(0L, amount.times(0, 60).minor)
        assertThrows(MoneyException::class.java) { amount.times(-1, 60) }
        assertThrows(MoneyException::class.java) { amount.times(1, 0) }
        assertThrows(MoneyException::class.java) { amount.times(-2) }
    }

    @Test
    fun `negate returns the opposite for reverse entries only`() {
        assertEquals(-100_000L, Money.ofMajor(1_000, Currency.YER_NEW).negate())
    }

    // -------------------------------------------------------------- القراءة

    @Test
    fun `input is in riyals and is stored in fils`() {
        assertEquals(1_500_000L, ok("15000", Currency.YER_NEW).minor)
        assertEquals(1_500_000L, ok("15,000", Currency.YER_NEW).minor)
        assertEquals(1_500_000L, ok("١٥٬٠٠٠", Currency.YER_NEW).minor)
        assertEquals(1_500_000L, ok("15 000", Currency.YER_NEW).minor)
        assertEquals(150L, ok("1.5", Currency.YER_NEW).minor) // ريال ونصف = 150 فلسًا
        assertEquals(1_005L, ok("10.05", Currency.YER_NEW).minor)
    }

    @Test
    fun `rejects more than two decimals and unparseable text with an arabic reason`() {
        listOf("", "  ", "abc", "10..5", "10-5", "-10", "١٠٪", "10.555").forEach { bad ->
            val result = Money.parse(bad, Currency.YER_NEW)
            assertTrue("يجب رفض: '$bad'", result is MoneyParse.Error)
            assertTrue("الرسالة عربية: '$bad'", (result as MoneyParse.Error).message.isNotBlank())
        }
    }

    @Test
    fun `rejects amounts beyond the ceiling`() {
        assertTrue(Money.parse("99999999999999999999", Currency.YER_NEW) is MoneyParse.Error)
    }

    // ------------------------------------------------- السلك: نص لا رقم

    @Test
    fun `wire format sends minor units as text`() {
        val money = Money.ofMajor(15_000, Currency.YER_NEW)
        val fields = MoneyWire.encode(money)
        assertEquals("1500000", fields[MoneyWire.KEY_AMOUNT_MINOR])
        assertEquals("YER_NEW", fields[MoneyWire.KEY_CURRENCY])
        // لا كسور عشرية في السلك إطلاقًا.
        assertFalse(fields[MoneyWire.KEY_AMOUNT_MINOR]!!.contains("."))
        assertEquals("1500000:YER_NEW", MoneyWire.encodeCompact(money))
    }

    @Test
    fun `wire decoding round trips without loss`() {
        val money = Money.ofMinor(1_234_567, Currency.YER_OLD)
        val decoded = MoneyWire.decode(money.toMinorString(), money.currency.code)
        assertTrue(decoded is MoneyParse.Ok)
        assertEquals(money, (decoded as MoneyParse.Ok).money)

        val compact = MoneyWire.decodeCompact(MoneyWire.encodeCompact(money))
        assertEquals(money, (compact as MoneyParse.Ok).money)
    }

    @Test
    fun `wire decoding refuses anything that is not an integer minor amount`() {
        assertTrue(MoneyWire.decode(null, "YER_NEW") is MoneyParse.Error)
        assertTrue(MoneyWire.decode("", "YER_NEW") is MoneyParse.Error)
        assertTrue(MoneyWire.decode("1500.00", "YER_NEW") is MoneyParse.Error) // كسر عشري مرفوض
        assertTrue(MoneyWire.decode("1,500", "YER_NEW") is MoneyParse.Error)
        assertTrue(MoneyWire.decode("-1500", "YER_NEW") is MoneyParse.Error)
        assertTrue(MoneyWire.decode("1500", null) is MoneyParse.Error)
        assertTrue(MoneyWire.decode("1500", "EUR") is MoneyParse.Error)
        assertTrue(MoneyWire.decode("99999999999999999999", "YER_NEW") is MoneyParse.Error)
    }

    @Test
    fun `legacy major amounts are converted once during migration with a recorded path`() {
        // صيغة قديمة كانت تخزّن الريال كنص: تُقرأ مرة واحدة في الترحيل لا في المسار العادي.
        val legacy = MoneyWire.decodeLegacyMajor("15000", "YER_NEW")
        assertEquals(1_500_000L, (legacy as MoneyParse.Ok).money.minor)
        assertTrue(MoneyWire.decodeLegacyMajor("15000", null) is MoneyParse.Error)
    }

    // -------------------------------------------------------------- التنسيق

    @Test
    fun `display shows riyals and hides a zero fraction`() {
        assertEquals("15,000 ر.ي", Money.ofMajor(15_000, Currency.YER_NEW).format())
        assertEquals("1,500,000 ر.ي قديم", Money.ofMajor(1_500_000, Currency.YER_OLD).format())
        assertEquals("12.50 ر.س", Money.ofMajor(12, Currency.SAR).plus(Money.ofMinor(50, Currency.SAR)).format())
        assertEquals("15,000.75 $", Money.ofMinor(1_500_075, Currency.USD).format())
        assertEquals("0 ر.ي", Money.zero(Currency.YER_NEW).format())
    }

    @Test
    fun `plain major string is stable for pdf and import`() {
        assertEquals("15000", Money.ofMajor(15_000, Currency.YER_NEW).toPlainMajorString())
        assertEquals("15000.50", Money.ofMinor(1_500_050, Currency.YER_NEW).toPlainMajorString())
        assertEquals("1500000", Money.ofMajor(15_000, Currency.YER_NEW).toMinorString())
    }

    @Test
    fun `formats arabic digits for reports when requested`() {
        val formatted = MoneyFormat.formatArabicDigits(Money.ofMajor(15_000, Currency.YER_NEW))
        assertTrue(formatted.startsWith("١٥"))
        assertTrue(formatted.contains("٬"))
    }

    @Test
    fun `currency codes are stable because they are stored`() {
        assertEquals("YER_OLD", Currency.YER_OLD.code)
        assertEquals("YER_NEW", Currency.YER_NEW.code)
        assertEquals("SAR", Currency.SAR.code)
        assertEquals("USD", Currency.USD.code)
        assertNotNull(Currency.fromCode("SAR"))
        assertEquals(null, Currency.fromCode("EUR"))
    }

    @Test
    fun `comparison is currency aware and exact`() {
        val small = Money.ofMajor(100, Currency.YER_NEW)
        val large = Money.ofMajor(200, Currency.YER_NEW)
        assertTrue(small < large)
        assertFalse(large < small)
        assertEquals(0, small.compareTo(Money.ofMajor(100, Currency.YER_NEW)))
    }

    private fun ok(input: String, currency: Currency): Money {
        val result = Money.parse(input, currency)
        assertTrue("توقع نجاح القراءة: '$input' → $result", result is MoneyParse.Ok)
        return (result as MoneyParse.Ok).money
    }
}
