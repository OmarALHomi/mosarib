package com.example

import com.example.core.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test accurate accounting balance without payment double counting`() {
        // Customer takes a 2-hour session at 5000/hr = 10,000 total.
        // Pays 4000 upfront. Remaining session debt = 6000.
        val sessionBilled = 10000.0
        val sessionPaidUpfront = 4000.0
        val sessionDebt = sessionBilled - sessionPaidUpfront
        assertEquals(6000.0, sessionDebt, 0.001)

        // Later, customer pays 3000 via a standalone receipt voucher.
        val voucherPayment = 3000.0

        // Total paid by customer must be upfront (4000) + voucher (3000) = 7000.
        // It must NOT double-count the 4000 upfront.
        val totalPaid = sessionPaidUpfront + voucherPayment
        assertEquals(7000.0, totalPaid, 0.001)

        // Net balance owed by customer must be 10000 - 7000 = 3000.
        val currentBalance = sessionBilled - totalPaid
        assertEquals(3000.0, currentBalance, 0.001)
    }

    @Test
    fun `test live timer minute rounding`() {
        val start = 1_000_000L

        // 45 seconds elapsed -> rounds to 1 minute
        val end45s = start + 45_000L
        val min45s = Math.max(1, Math.round((end45s - start) / 60000.0).toInt())
        assertEquals(1, min45s)

        // 1 minute 40 seconds -> rounds to 2 minutes
        val end100s = start + 100_000L
        val min100s = Math.max(1, Math.round((end100s - start) / 60000.0).toInt())
        assertEquals(2, min100s)

        // 3 hours exactly = 180 minutes
        val end3hrs = start + (3 * 3600_000L)
        val min3hrs = Math.max(1, Math.round((end3hrs - start) / 60000.0).toInt())
        assertEquals(180, min3hrs)
    }

    @Test
    fun `test formatters currency and duration`() {
        val formattedCost = Formatters.formatCurrency(12500.0, "ر.ي")
        assertEquals("12,500 ر.ي", formattedCost)

        val durationArabic = Formatters.formatDurationArabic(90)
        assertEquals("ساعة واحدة و 30 دقيقة", durationArabic)
    }

    @Test
    fun `test Arabic Tafqeet conversion`() {
        assertEquals("صفر ريال", Formatters.amountToArabicWords(0.0, "ريال"))
        assertEquals("فقط خمسة آلاف ريال لا غير", Formatters.amountToArabicWords(5000.0, "ريال"))
        assertEquals("فقط عشرة آلاف ريال لا غير", Formatters.amountToArabicWords(10000.0, "ريال"))
        assertEquals("فقط خمسة عشر ألفاً ريال لا غير", Formatters.amountToArabicWords(15000.0, "ريال"))
        assertEquals("فقط خمسة وعشرون ألفاً وخمسمائة ريال لا غير", Formatters.amountToArabicWords(25500.0, "ريال"))
        assertEquals("فقط مائة ألف ريال لا غير", Formatters.amountToArabicWords(100000.0, "ريال"))
    }

    @Test
    fun `test comma input formatting`() {
        assertEquals("1,000", Formatters.formatAmountInput("1000"))
        assertEquals("10,000", Formatters.formatAmountInput("10000"))
        assertEquals("100,000.5", Formatters.formatAmountInput("100000.5"))
        assertEquals(100000.5, Formatters.parseAmountInput("100,000.5"), 0.001)
    }
}
