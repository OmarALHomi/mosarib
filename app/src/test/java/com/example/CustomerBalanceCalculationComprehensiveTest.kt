package com.example

import com.example.features.customers.Customer
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Comprehensive unit test suite for [calculateCustomerBalance].
 * Validates financial invariants, prevents double counting on session-linked vouchers,
 * and verifies edge cases for farmers, well owners, and hybrid accounts.
 */
class CustomerBalanceCalculationComprehensiveTest {

    @Test
    fun `farmer with fully paid session has zero receivable balance`() {
        val farmer = Customer(id = 1, name = "مزارع أحمد", isWellOwner = false)
        val sessions = listOf(
            WaterSession(
                id = 101,
                customerId = farmer.id,
                durationMinutes = 120,
                totalAmount = 5000.0,
                amountPaid = 5000.0,
                remainingDebt = 0.0
            )
        )

        val result = calculateCustomerBalance(farmer, sessions, emptyList())

        assertEquals(5000.0, result.totalBilledAmount, 0.001)
        assertEquals(5000.0, result.totalPaidAmount, 0.001)
        assertEquals(0.0, result.receivableBalance, 0.001)
        assertEquals(0.0, result.payableBalance, 0.001)
        assertEquals(0.0, result.balance, 0.001)
        assertEquals(1, result.totalSessionsCount)
    }

    @Test
    fun `farmer with partial payment has exact remaining debt as receivable balance`() {
        val farmer = Customer(id = 2, name = "مزارع سعيد", isWellOwner = false)
        val sessions = listOf(
            WaterSession(
                id = 102,
                customerId = farmer.id,
                durationMinutes = 180,
                totalAmount = 9000.0,
                amountPaid = 4000.0,
                remainingDebt = 5000.0
            )
        )

        val result = calculateCustomerBalance(farmer, sessions, emptyList())

        assertEquals(9000.0, result.totalBilledAmount, 0.001)
        assertEquals(4000.0, result.totalPaidAmount, 0.001)
        assertEquals(5000.0, result.receivableBalance, 0.001)
        assertEquals(5000.0, result.balance, 0.001)
    }

    @Test
    fun `session-linked receipt does not double count against session amountPaid`() {
        val farmer = Customer(id = 3, name = "مزارع ناصر", isWellOwner = false)
        val session = WaterSession(
            id = 103,
            customerId = farmer.id,
            durationMinutes = 60,
            totalAmount = 3000.0,
            amountPaid = 3000.0 // Payment already recorded inside session
        )
        // Receipt linked directly to session #103
        val linkedReceipt = Voucher(
            id = 501,
            customerId = farmer.id,
            sessionId = session.id,
            type = VoucherType.RECEIPT,
            amount = 3000.0
        )

        val result = calculateCustomerBalance(farmer, listOf(session), listOf(linkedReceipt))

        // Must still be 3000 total paid, NOT 6000!
        assertEquals(3000.0, result.totalPaidAmount, 0.001)
        assertEquals(0.0, result.receivableBalance, 0.001)
    }

    @Test
    fun `standalone receipt reduces debt independently`() {
        val farmer = Customer(id = 4, name = "مزارع خالد", isWellOwner = false)
        val session = WaterSession(
            id = 104,
            customerId = farmer.id,
            durationMinutes = 120,
            totalAmount = 6000.0,
            amountPaid = 0.0
        )
        // Standalone cash receipt later
        val standaloneReceipt = Voucher(
            id = 502,
            customerId = farmer.id,
            sessionId = null,
            type = VoucherType.RECEIPT,
            amount = 2500.0
        )

        val result = calculateCustomerBalance(farmer, listOf(session), listOf(standaloneReceipt))

        assertEquals(6000.0, result.totalBilledAmount, 0.001)
        assertEquals(2500.0, result.totalPaidAmount, 0.001)
        assertEquals(3500.0, result.receivableBalance, 0.001)
    }

    @Test
    fun `standalone discount voucher reduces customer debt`() {
        val farmer = Customer(id = 5, name = "مزارع عمر", isWellOwner = false)
        val session = WaterSession(
            id = 105,
            customerId = farmer.id,
            durationMinutes = 60,
            totalAmount = 4000.0,
            amountPaid = 1000.0
        )
        val discountVoucher = Voucher(
            id = 503,
            customerId = farmer.id,
            sessionId = null,
            type = VoucherType.DISCOUNT,
            amount = 500.0
        )

        val result = calculateCustomerBalance(farmer, listOf(session), listOf(discountVoucher))

        // Debt = 4000 total - 1000 paid - 500 discount = 2500
        assertEquals(2500.0, result.receivableBalance, 0.001)
    }

    @Test
    fun `farmer distributor waste minutes are accurately tracked and deducted from sold minutes`() {
        val farmer = Customer(id = 6, name = "مزارع بلال", isWellOwner = false)
        val session = WaterSession(
            id = 106,
            customerId = farmer.id,
            durationMinutes = 240, // 4 hours
            wastedMinutes = 30,    // 30 min distributor canal waste
            totalAmount = 7000.0,
            amountPaid = 7000.0
        )

        val result = calculateCustomerBalance(farmer, listOf(session), emptyList())

        assertEquals(240, result.totalMinutes)
        assertEquals(210, result.totalSoldMinutes)
        assertEquals(30, result.totalDistributorWasteMinutes)
    }

    @Test
    fun `disbursement to customer adds to receivable balance as cash advance`() {
        val farmer = Customer(id = 7, name = "مزارع راشد", isWellOwner = false)
        val session = WaterSession(
            id = 107,
            customerId = farmer.id,
            durationMinutes = 60,
            totalAmount = 2000.0,
            amountPaid = 2000.0
        )
        val advanceVoucher = Voucher(
            id = 504,
            customerId = farmer.id,
            type = VoucherType.EXPENSE,
            amount = 1500.0
        )

        val result = calculateCustomerBalance(farmer, listOf(session), listOf(advanceVoucher))

        // 2000 session + 1500 advance - 2000 paid = 1500 receivable
        assertEquals(1500.0, result.receivableBalance, 0.001)
        assertEquals(1500.0, result.totalDisbursedAmount, 0.001)
    }

    @Test
    fun `well owner with net purchase payable has positive payable balance`() {
        val owner = Customer(id = 8, name = "صاحب بئر الوادي", isWellOwner = true)
        val purchases = listOf(
            WellOwnerPurchase(
                id = 1,
                ownerCustomerId = owner.id,
                durationMinutes = 600, // 10 hours
                wastedMinutesOnOwner = 60, // 1 hour owner waste
                purchaseRatePerHour = 4000.0
            )
        )
        // Chargeable = 9 hours = 36,000 net purchase

        // Distributor pays 16,000 cash to owner
        val disbursement = Voucher(
            id = 505,
            customerId = owner.id,
            type = VoucherType.EXPENSE,
            amount = 16000.0
        )

        val result = calculateCustomerBalance(owner, emptyList(), listOf(disbursement), purchases = purchases)

        assertEquals(36000.0, result.totalPurchaseAmount, 0.001)
        assertEquals(4000.0, result.totalOwnerWasteCredit, 0.001)
        assertEquals(16000.0, result.totalDisbursedAmount, 0.001)
        // 36000 payable - 16000 paid out = 20,000 distributor owes owner
        assertEquals(20000.0, result.payableBalance, 0.001)
        assertEquals(0.0, result.receivableBalance, 0.001)
        assertEquals(20000.0, result.balance, 0.001)
    }

    @Test
    fun `well owner irrigation sessions exceeding purchases invert into receivable debt`() {
        val owner = Customer(id = 9, name = "صاحب بئر السلام", isWellOwner = true)
        val purchases = listOf(
            WellOwnerPurchase(
                id = 2,
                ownerCustomerId = owner.id,
                durationMinutes = 180, // 3 hours
                purchaseRatePerHour = 3000.0
            )
        )
        // Net purchase = 9,000

        // Owner watered their own farm worth 15,000 with 0 cash paid
        val session = WaterSession(
            id = 108,
            customerId = owner.id,
            durationMinutes = 300,
            totalAmount = 15000.0,
            amountPaid = 0.0
        )

        val result = calculateCustomerBalance(owner, listOf(session), emptyList(), purchases = purchases)

        // netOwnerPayable = (9000 purchase) - (15000 water sales) = -6000
        // Result: payable = 0, receivable = 6000 (Owner owes distributor 6000)
        assertEquals(0.0, result.payableBalance, 0.001)
        assertEquals(6000.0, result.receivableBalance, 0.001)
    }
}
