package com.example

import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `customer balance combines session payment and receipt voucher`() {
        val customer = Customer(id = 1, name = "عميل")
        val sessions = listOf(
            WaterSession(customerId = customer.id, durationMinutes = 120, totalAmount = 10000.0, amountPaid = 4000.0)
        )
        val vouchers = listOf(
            Voucher(customerId = customer.id, type = VoucherType.RECEIPT, amount = 3000.0)
        )

        val result = calculateCustomerBalance(customer, sessions, vouchers)

        assertEquals(10000.0, result.totalBilledAmount, 0.001)
        assertEquals(7000.0, result.totalPaidAmount, 0.001)
        assertEquals(3000.0, result.balance, 0.001)
        assertEquals(120, result.totalMinutes)
    }

    @Test
    fun `customer balance applies discounts and disbursements and ignores general expenses and other customers`() {
        val customer = Customer(id = 1, name = "عميل")
        val sessions = listOf(
            WaterSession(customerId = customer.id, totalAmount = 10000.0, amountPaid = 4000.0)
        )
        val vouchers = listOf(
            Voucher(customerId = customer.id, type = VoucherType.DISCOUNT, amount = 1000.0),
            Voucher(customerId = customer.id, type = VoucherType.EXPENSE, amount = 2000.0),
            Voucher(customerId = null, type = VoucherType.EXPENSE, amount = 5000.0),
            Voucher(customerId = 2, type = VoucherType.RECEIPT, amount = 6000.0)
        )

        val result = calculateCustomerBalance(customer, sessions, vouchers)

        assertEquals(5000.0, result.totalPaidAmount, 0.001)
        assertEquals(2000.0, result.totalDisbursedAmount, 0.001)
        assertEquals(7000.0, result.balance, 0.001)
    }

    @Test
    fun `beneficiary customer balance includes sessions billed on his account`() {
        val beneficiary = Customer(id = 5, name = "مستفيد / شريك", isBeneficiary = true)
        val sessions = listOf(
            WaterSession(id = 1, customerId = 10, billedToCustomerId = beneficiary.id, totalAmount = 8000.0, amountPaid = 3000.0)
        )
        val vouchers = listOf(
            Voucher(customerId = beneficiary.id, type = VoucherType.EXPENSE, amount = 1500.0)
        )

        val result = calculateCustomerBalance(beneficiary, sessions, vouchers)

        assertEquals(8000.0, result.totalBilledAmount, 0.001)
        assertEquals(3000.0, result.totalPaidAmount, 0.001)
        assertEquals(1500.0, result.totalDisbursedAmount, 0.001)
        assertEquals(6500.0, result.balance, 0.001)
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

    @Test
    fun `test phone normalization handles Eastern Arabic digits`() {
        val rawArabic = "٠٥٠١٢٣٤٥٦٧"
        val normalized = com.example.core.util.FileSharingHelper.normalizePhone(rawArabic)
        assertEquals("967501234567", normalized)

        val withCode = "+967 ٧٧١ ٢٣٤ ٥٦٧"
        val normalizedCode = com.example.core.util.FileSharingHelper.normalizePhone(withCode)
        assertEquals("967771234567", normalizedCode)
    }
}
