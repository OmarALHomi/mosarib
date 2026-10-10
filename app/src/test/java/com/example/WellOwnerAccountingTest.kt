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
    fun `well owner keeps supplier payable and customer receivable separate`() {
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
        assertEquals(17000.0, result.payableBalance, 0.001)
        assertEquals(5000.0, result.receivableBalance, 0.001)
        // The list compatibility value is the supplier side; neither account offsets the other.
        assertEquals(17000.0, result.balance, 0.001)
        assertEquals(5000.0, result.totalPaidAmount, 0.001)
        assertEquals(10000.0, result.totalDisbursedAmount, 0.001)
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
}
