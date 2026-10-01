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
    fun `customer balance supports both receipt and disbursement vouchers`() {
        val customer = Customer(id = 5, name = "عميل")
        val sessions = listOf(
            WaterSession(id = 1, customerId = customer.id, totalAmount = 8000.0, amountPaid = 3000.0)
        )
        val vouchers = listOf(
            Voucher(customerId = customer.id, type = VoucherType.EXPENSE, amount = 1500.0)
        )

        val result = calculateCustomerBalance(customer, sessions, vouchers)

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
        assertEquals("100,000", Formatters.formatAmountInput("100000"))
        assertEquals(100000.0, Formatters.parseAmountInput("100,000"), 0.001)
        // Test rounding of decimals to nearest integer
        assertEquals(100001.0, Formatters.parseAmountInput("100,000.6"), 0.001)
        assertEquals(6417.0, Formatters.calculateWaterCost(35, 11000.0), 0.001)
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

    @Test
    fun `test LicenseManager monthly and yearly key generation format and reproducibility`() {
        val deviceCode = "MSRB-8F42-9D1B"
        val monthlyKey1 = com.example.core.license.LicenseManager.generateActivationKey(deviceCode, com.example.core.license.LicenseManager.SubscriptionPlan.MONTHLY)
        val monthlyKey2 = com.example.core.license.LicenseManager.generateActivationKey(deviceCode, com.example.core.license.LicenseManager.SubscriptionPlan.MONTHLY)
        val yearlyKey = com.example.core.license.LicenseManager.generateActivationKey(deviceCode, com.example.core.license.LicenseManager.SubscriptionPlan.YEARLY)

        assertEquals(monthlyKey1, monthlyKey2)
        org.junit.Assert.assertNotEquals(monthlyKey1, yearlyKey)
        org.junit.Assert.assertTrue(monthlyKey1.startsWith("ACTV-M-"))
        org.junit.Assert.assertTrue(yearlyKey.startsWith("ACTV-Y-"))
        assertEquals(16, monthlyKey1.length) // ACTV-M-XXXX-XXXX
        assertEquals(16, yearlyKey.length) // ACTV-Y-XXXX-XXXX
    }

    @Test
    fun `test LicenseManager free operations limit constant`() {
        assertEquals(200, com.example.core.license.LicenseManager.FREE_OPERATIONS_LIMIT)
    }

    @Test
    fun `test FIFO allocation covers oldest sessions first with partial remainder`() {
        val s1 = WaterSession(id = 1, customerId = 1L, startTime = 1000L, totalAmount = 10000.0, amountPaid = 2000.0, remainingDebt = 8000.0)
        val s2 = WaterSession(id = 2, customerId = 1L, startTime = 2000L, totalAmount = 15000.0, amountPaid = 0.0, remainingDebt = 15000.0)
        val s3 = WaterSession(id = 3, customerId = 1L, startTime = 3000L, totalAmount = 5000.0, amountPaid = 0.0, remainingDebt = 5000.0)

        // Customer pays 12,000
        val result = com.example.features.vouchers.calculateFifoAllocation(listOf(s3, s1, s2), 12000.0)

        assertEquals(0.0, result.surplus, 0.001)
        assertEquals(3, result.steps.size)

        // Session 1 (oldest: 1000L) gets full remainingDebt of 8000
        val step1 = result.steps.find { it.session.id == 1L }!!
        assertEquals(8000.0, step1.allocatedAmount, 0.001)
        assertEquals(10000.0, step1.newPaid, 0.001)
        assertEquals(0.0, step1.newDebt, 0.001)
        org.junit.Assert.assertTrue(step1.isFullyPaid)

        // Session 2 (middle: 2000L) gets remaining 4000
        val step2 = result.steps.find { it.session.id == 2L }!!
        assertEquals(4000.0, step2.allocatedAmount, 0.001)
        assertEquals(4000.0, step2.newPaid, 0.001)
        assertEquals(11000.0, step2.newDebt, 0.001)
        org.junit.Assert.assertFalse(step2.isFullyPaid)

        // Session 3 (newest: 3000L) gets 0
        val step3 = result.steps.find { it.session.id == 3L }!!
        assertEquals(0.0, step3.allocatedAmount, 0.001)
        assertEquals(0.0, step3.newPaid, 0.001)
        assertEquals(5000.0, step3.newDebt, 0.001)
        org.junit.Assert.assertFalse(step3.isFullyPaid)
    }

    @Test
    fun `test FIFO allocation with surplus overpayment`() {
        val s1 = WaterSession(id = 1, customerId = 1L, startTime = 1000L, totalAmount = 5000.0, amountPaid = 0.0, remainingDebt = 5000.0)
        val s2 = WaterSession(id = 2, customerId = 1L, startTime = 2000L, totalAmount = 3000.0, amountPaid = 0.0, remainingDebt = 3000.0)

        // Customer pays 10,000 (total debt is 8,000)
        val result = com.example.features.vouchers.calculateFifoAllocation(listOf(s1, s2), 10000.0)

        assertEquals(2000.0, result.surplus, 0.001)
        org.junit.Assert.assertTrue(result.steps.all { it.isFullyPaid })
    }
}
