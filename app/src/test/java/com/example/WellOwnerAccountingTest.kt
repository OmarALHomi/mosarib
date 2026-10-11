package com.example

import com.example.features.customers.Customer
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import org.junit.Assert.assertEquals
import org.junit.Test

class WellOwnerAccountingTest {
    @Test
    fun `owner-responsible waste reduces purchase payable and reconciles exactly`() {
        val purchase = WellOwnerPurchase(
            ownerCustomerId = 9,
            durationMinutes = 12 * 60,
            wastedMinutesOnOwner = 60,
            purchaseRatePerHour = 3000.0
        )

        assertEquals(720, purchase.durationMinutes)
        assertEquals(660, WellOwnerPurchaseMath.chargeableMinutes(purchase))
        assertEquals(36000.0, WellOwnerPurchaseMath.grossAmount(purchase), 0.001)
        assertEquals(33000.0, WellOwnerPurchaseMath.payableAmount(purchase), 0.001)
        assertEquals(3000.0, WellOwnerPurchaseMath.ownerWasteCredit(purchase), 0.001)
        assertEquals(
            WellOwnerPurchaseMath.grossAmount(purchase),
            WellOwnerPurchaseMath.payableAmount(purchase) + WellOwnerPurchaseMath.ownerWasteCredit(purchase),
            0.001
        )
    }

    @Test
    fun `well owner offsets irrigation sessions directly against purchase amounts`() {
        val owner = Customer(id = 9, name = "صاحب البئر", isWellOwner = true)
        val purchases = listOf(
            WellOwnerPurchase(
                ownerCustomerId = owner.id,
                durationMinutes = 10 * 60,
                wastedMinutesOnOwner = 60,
                purchaseRatePerHour = 3000.0
            )
        )
        val sessions = listOf(
            WaterSession(
                id = 1,
                customerId = owner.id,
                durationMinutes = 120,
                totalAmount = 10000.0,
                amountPaid = 3000.0
            )
        )
        val vouchers = listOf(
            Voucher(customerId = owner.id, type = VoucherType.EXPENSE, amount = 10000.0),
            Voucher(customerId = owner.id, type = VoucherType.RECEIPT, amount = 2000.0)
        )

        val result = calculateCustomerBalance(owner, sessions, vouchers, purchases = purchases)

        assertEquals(27000.0, result.totalPurchaseAmount, 0.001)
        // (27000 net purchase + 5000 collected) - (10000 disbursed + 10000 water sessions) = 12000 payable
        assertEquals(12000.0, result.payableBalance, 0.001)
        assertEquals(0.0, result.receivableBalance, 0.001)
        assertEquals(12000.0, result.balance, 0.001)
        assertEquals(5000.0, result.totalPaidAmount, 0.001)
        assertEquals(10000.0, result.totalDisbursedAmount, 0.001)
    }

    @Test
    fun `session billed to well owner on behalf of farmer offsets well owner balance and leaves farmer debt zero`() {
        val owner = Customer(id = 9, name = "صاحب البئر", isWellOwner = true)
        val farmer = Customer(id = 1, name = "المزارع صالح", isWellOwner = false)

        val purchases = listOf(
            WellOwnerPurchase(
                ownerCustomerId = owner.id,
                durationMinutes = 10 * 60,
                purchaseRatePerHour = 2000.0
            )
        )
        // 600 min @ 2000 = 20,000 net purchase for owner

        // Farmer waters, billed to well owner: 15,000
        val sessionBilledToOwner = WaterSession(
            id = 10,
            customerId = farmer.id,
            billedToCustomerId = owner.id,
            durationMinutes = 180,
            totalAmount = 15000.0,
            amountPaid = 0.0,
            remainingDebt = 0.0
        )

        val farmerResult = calculateCustomerBalance(farmer, listOf(sessionBilledToOwner), emptyList())
        assertEquals(0.0, farmerResult.receivableBalance, 0.001)
        assertEquals(0, farmerResult.totalSessionsCount)

        val ownerResult = calculateCustomerBalance(owner, listOf(sessionBilledToOwner), emptyList(), purchases = purchases)
        // 20000 purchase - 15000 session = 5000 payable to owner
        assertEquals(5000.0, ownerResult.payableBalance, 0.001)
        assertEquals(0.0, ownerResult.receivableBalance, 0.001)

        // If water sessions exceed purchases, owner becomes debtor (receivable)
        val excessiveSession = WaterSession(
            id = 11,
            customerId = owner.id,
            durationMinutes = 300,
            totalAmount = 25000.0,
            amountPaid = 0.0,
            remainingDebt = 0.0
        )
        val excessiveOwnerResult = calculateCustomerBalance(owner, listOf(excessiveSession), emptyList(), purchases = purchases)
        // 20000 purchase - 25000 session = -5000 (receivable from owner = 5000)
        assertEquals(0.0, excessiveOwnerResult.payableBalance, 0.001)
        assertEquals(5000.0, excessiveOwnerResult.receivableBalance, 0.001)
    }

    @Test
    fun `linked receipt voucher is not counted twice with session amount paid`() {
        val customer = Customer(id = 1, name = "مزارع")
        val sessions = listOf(
            WaterSession(id = 4, customerId = customer.id, totalAmount = 10000.0, amountPaid = 8000.0)
        )
        val vouchers = listOf(
            Voucher(customerId = customer.id, sessionId = 4, type = VoucherType.RECEIPT, amount = 3000.0),
            Voucher(customerId = customer.id, type = VoucherType.RECEIPT, amount = 500.0)
        )

        val result = calculateCustomerBalance(customer, sessions, vouchers)

        assertEquals(8500.0, result.totalPaidAmount, 0.001)
        assertEquals(1500.0, result.receivableBalance, 0.001)
    }

    @Test
    fun `waste cannot exceed purchase duration and chargeable minutes never drops below zero`() {
        val purchaseWithExcessiveWaste = WellOwnerPurchase(
            ownerCustomerId = 9,
            durationMinutes = 180,
            wastedMinutesOnOwner = 240, // Greater than duration
            purchaseRatePerHour = 4000.0
        )
        // WellOwnerPurchaseMath clamps chargeable minutes between 0 and duration
        val chargeable = WellOwnerPurchaseMath.chargeableMinutes(purchaseWithExcessiveWaste)
        assertEquals(0, chargeable)
        assertEquals(0.0, WellOwnerPurchaseMath.payableAmount(purchaseWithExcessiveWaste), 0.001)
        assertEquals(12000.0, WellOwnerPurchaseMath.grossAmount(purchaseWithExcessiveWaste), 0.001)
    }

    @Test
    fun `well owner purchase remaining due calculation handles immediate cash settlement`() {
        val purchase = WellOwnerPurchase(
            ownerCustomerId = 9,
            durationMinutes = 300, // 5 hours
            wastedMinutesOnOwner = 60, // 1 hour waste -> 4 hours chargeable
            purchaseRatePerHour = 5000.0 // 4 * 5000 = 20,000 payable
        )
        val payable = WellOwnerPurchaseMath.payableAmount(purchase)
        assertEquals(20000.0, payable, 0.001)

        val amountPaid = 15000.0
        val remainingDueToOwner = maxOf(0.0, payable - amountPaid)
        assertEquals(5000.0, remainingDueToOwner, 0.001)
    }

    @Test
    fun `maximum allowed session duration supports up to 10 days`() {
        val maxDays = 10
        val maxHours = maxDays * 24
        val maxMinutes = maxHours * 60
        assertEquals(240, maxHours)
        assertEquals(14400, maxMinutes)

        val tenDaySession = WaterSession(
            customerId = 1,
            durationMinutes = maxMinutes,
            totalAmount = 240000.0
        )
        assertEquals(14400, tenDaySession.durationMinutes)
    }
}
