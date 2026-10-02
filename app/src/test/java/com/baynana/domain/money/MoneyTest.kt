package com.baynana.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٢ (الخطة v6 §4.1 و§15): المال بالوحدات الصغرى، أربع عملات بلا تحويل، لا تجاوز صامت،
 * وسياسة تقريب واحدة معلنة. هذه الاختبارات تعمل على JVM بلا محاكي.
 */
class MoneyTest {

    // ------------------------------------------------------------- البناء

    @Test
    fun `major amount converts to minor units per currency`() {
        assertEquals(15000L, Money.ofMajor(15000, Currency.YER_NEW).minor)
        assertEquals(15000L, Money.ofMajor(15000, Currency.YER_OLD).minor)
        assertEquals(1000L, Money.ofMajor(10, Currency.SAR).minor)
        assertEquals(10.0, Money.ofMajor(10, Currency.SAR).minor / 100.0, 0.0)
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

    // ------------------------------------------------------------ العمليات

    @Test
    fun `addition and subtraction stay exact`() {
        val price = Money.ofMajor(10000, Currency.YER_NEW)
        val paid = Money.ofMajor(3000, Currency.YER_NEW)
        assertEquals(13000L, (price + paid).minor)
        assertEquals(7000L, (price - paid).minor)
        assertTrue(Money.zero(Currency.YER_NEW).isZero)
    }

    @Test
    fun `subtraction below zero is refused instead of silently flipping sign`() {
        val debt = Money.ofMajor(3000, Currency.YER_NEW)
        val payment = Money.ofMajor(5000, Currency.YER_NEW)
        val error = assertThrows(MoneyException::class.java) { debt - payment }
        assertTrue(error.message!!.contains("سالب"))
    }

    @Test
    fun `mixing two currencies is refused`() {
        val riyal = Money.ofMajor(5000, Currency.YER_NEW)
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
    fun `hourly rate times minutes rounds half up to the currency minor unit`() {
        val perHour = Money.ofMajor(5000, Currency.YER_NEW)
        // 45 دقيقة من سعر 5000 للساعة = 3750 بالضبط
        assertEquals(3750L, perHour.times(45, 60).minor)
        // 7 دقائق = 583.33 → 583
        assertEquals(583L, perHour.times(7, 60).minor)
        // 30 ثانية = 2500 بالضبط
        assertEquals(2500L, perHour.times(30, 60).minor)
        // نصف دقيقة من سعر 5000 للساعة = 41.666… وتقرّب لأعلى إلى 42
        assertEquals(42L, perHour.times(1, 120).minor)
    }

    @Test
    fun `rounding keeps cents for currencies that have them`() {
        val price = Money.ofMinor(1000, Currency.USD) // 10.00 دولار
        val third = price.times(1, 3) // 3.3333 → 3.33
        assertEquals(333L, third.minor)
        assertEquals("3.33 $", third.format())
    }

    @Test
    fun `a fractional factor cannot produce a fraction of a minor unit`() {
        val amount = Money.ofMajor(1, Currency.YER_NEW)
        val third = amount.times(1, 3) // 0.333 → 0
        assertEquals(0L, third.minor)
        assertTrue(third.isZero)
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
        val sale = Money.ofMajor(1000, Currency.YER_NEW)
        assertEquals(-1000L, sale.negate())
    }

    // -------------------------------------------------------------- القراءة

    @Test
    fun `parses latin and arabic digits with separators`() {
        assertEquals(15000L, ok("15000", Currency.YER_NEW).minor)
        assertEquals(15000L, ok("15,000", Currency.YER_NEW).minor)
        assertEquals(15000L, ok("١٥٬٠٠٠", Currency.YER_NEW).minor)
        assertEquals(15000L, ok("۱۵٬۰۰۰", Currency.YER_NEW).minor)
        assertEquals(15000L, ok(" 15 000 ", Currency.YER_NEW).minor)
        assertEquals(15L, ok("١٥", Currency.YER_NEW).minor)
    }

    @Test
    fun `parses fractions only in currencies that have them`() {
        assertEquals(1050L, ok("10.50", Currency.SAR).minor)
        assertEquals(1050L, ok("10٫50", Currency.SAR).minor)
        assertEquals(1005L, ok("10.05", Currency.USD).minor)
        assertEquals(1000L, ok("10", Currency.USD).minor)
        assertTrue(Money.parse("10.5", Currency.YER_NEW) is MoneyParse.Error)
        assertTrue(Money.parse("10.555", Currency.SAR) is MoneyParse.Error)
    }

    @Test
    fun `rejects what it cannot understand with an arabic reason`() {
        listOf("", "  ", "abc", "10..5", "10-5", "-10", "١٠٪").forEach { bad ->
            val result = Money.parse(bad, Currency.YER_NEW)
            assertTrue("يجب رفض: '$bad'", result is MoneyParse.Error)
            assertTrue("الرسالة عربية: '$bad'", (result as MoneyParse.Error).message.isNotBlank())
        }
    }

    @Test
    fun `rejects amounts beyond the ceiling`() {
        val result = Money.parse("99999999999999999999", Currency.YER_NEW)
        assertTrue(result is MoneyParse.Error)
    }

    @Test
    fun `parsed money round trips through plain text`() {
        val money = ok("12,345", Currency.SAR)
        assertEquals(12345L, money.minor)
        assertEquals("123.45", money.toPlainString())
        assertEquals(12345L, ok(money.toPlainString(), Currency.SAR).minor)
    }

    // -------------------------------------------------------------- التنسيق

    @Test
    fun `formats with grouping and currency symbol`() {
        assertEquals("15,000 ر.ي", Money.ofMajor(15000, Currency.YER_NEW).format())
        assertEquals("12.50 ر.س", Money.ofMinor(1250, Currency.SAR).format())
        assertEquals("1,000.00 $", Money.ofMinor(100000, Currency.USD).format())
    }

    @Test
    fun `formats arabic digits for reports when requested`() {
        val formatted = MoneyFormat.formatArabicDigits(Money.ofMajor(15000, Currency.YER_NEW))
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

    private fun ok(input: String, currency: Currency): Money {
        val result = Money.parse(input, currency)
        assertTrue("توقع نجاح القراءة: '$input' → $result", result is MoneyParse.Ok)
        return (result as MoneyParse.Ok).money
    }

    @Test
    fun `comparison is currency aware and exact`() {
        val small = Money.ofMajor(100, Currency.YER_NEW)
        val large = Money.ofMajor(200, Currency.YER_NEW)
        assertTrue(small < large)
        assertFalse(large < small)
        assertEquals(0, small.compareTo(Money.ofMajor(100, Currency.YER_NEW)))
    }
}
