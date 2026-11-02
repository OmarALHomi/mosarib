package com.example

import com.example.features.customers.Customer
import com.example.features.customers.CustomerLedgerItem
import com.example.features.customers.LedgerRunningBalance
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerRunningBalanceTest {

    @Test
    fun `normal customer with sessions receipts expenses and discounts reconciles with CustomerRepository`() {
        val customer = Customer(id = 10, name = "مزارع علي", isWellOwner = false)

        val sessions = listOf(
            WaterSession(
                id = 1,
                customerId = customer.id,
                startTime = 1000L,
                durationMinutes = 120,
                totalAmount = 6000.0,
                amountPaid = 0.0,
                remainingDebt = 6000.0
            ),
            WaterSession(
                id = 2,
                customerId = customer.id,
                startTime = 3000L,
                durationMinutes = 60,
                totalAmount = 3000.0,
                amountPaid = 0.0,
                remainingDebt = 3000.0
            )
        )

        val vouchers = listOf(
            Voucher(
                id = 101,
                customerId = customer.id,
                date = 2000L,
                type = VoucherType.RECEIPT,
                amount = 2000.0
            ),
            Voucher(
                id = 102,
                customerId = customer.id,
                date = 4000L,
                type = VoucherType.EXPENSE,
                amount = 500.0
            ),
            Voucher(
                id = 103,
                customerId = customer.id,
                date = 5000L,
                type = VoucherType.DISCOUNT,
                amount = 300.0
            )
        )

        val repoResult = calculateCustomerBalance(customer, sessions, vouchers)
        val ledgerResult = LedgerRunningBalance.compute(customer, sessions, vouchers)

        // Repository calculation:
        // totalSales (6000 + 3000 = 9000) + disbursed (500) - receipts (2000) - discounts (300) = 7200.0
        assertEquals(7200.0, repoResult.receivableBalance, 0.001)
        assertEquals(7200.0, repoResult.balance, 0.001)
        assertEquals(repoResult.balance, ledgerResult.finalBalance, 0.001)

        // Verify progression:
        // t=1000: session 1 (+6000) -> 6000
        // t=2000: receipt 101 (-2000) -> 4000
        // t=3000: session 2 (+3000) -> 7000
        // t=4000: expense 102 (+500) -> 7500
        // t=5000: discount 103 (-300) -> 7200
        val items = LedgerRunningBalance.buildItems(customer, sessions, vouchers)
        val s1 = items.first { it is CustomerLedgerItem.SessionItem && it.session.id == 1L }
        val r1 = items.first { it is CustomerLedgerItem.ReceiptItem && it.voucher.id == 101L }
        val s2 = items.first { it is CustomerLedgerItem.SessionItem && it.session.id == 2L }
        val e1 = items.first { it is CustomerLedgerItem.DisbursementItem && it.voucher.id == 102L }
        val d1 = items.first { it is CustomerLedgerItem.DiscountItem && it.voucher.id == 103L }

        assertEquals(6000.0, ledgerResult.balancesByItem[s1] ?: 0.0, 0.001)
        assertEquals(4000.0, ledgerResult.balancesByItem[r1] ?: 0.0, 0.001)
        assertEquals(7000.0, ledgerResult.balancesByItem[s2] ?: 0.0, 0.001)
        assertEquals(7500.0, ledgerResult.balancesByItem[e1] ?: 0.0, 0.001)
        assertEquals(7200.0, ledgerResult.balancesByItem[d1] ?: 0.0, 0.001)
    }

    @Test
    fun `well owner with purchases and payments reconciles with CustomerRepository`() {
        val owner = Customer(id = 20, name = "صاحب بئر ناصر", isWellOwner = true)

        val purchases = listOf(
            WellOwnerPurchase(
                id = 1,
                ownerCustomerId = owner.id,
                date = 1000L,
                durationMinutes = 10 * 60,
                purchaseRatePerHour = 3000.0,
                wastedMinutesOnOwner = 60
            ) // 9 hours chargeable @ 3000 = 27,000 payable
        )

        val vouchers = listOf(
            Voucher(
                id = 201,
                customerId = owner.id,
                date = 2000L,
                type = VoucherType.EXPENSE,
                amount = 10000.0
            ), // Paid to well owner (-10,000)
            Voucher(
                id = 202,
                customerId = owner.id,
                date = 3000L,
                type = VoucherType.RECEIPT,
                amount = 2000.0
            ) // Collected from well owner (+2,000)
        )

        val repoResult = calculateCustomerBalance(owner, emptyList(), vouchers, purchases = purchases)
        val ledgerResult = LedgerRunningBalance.compute(owner, emptyList(), vouchers, purchases)

        // Repository calculation:
        // netPurchaseAmount (27,000) + collectedCash (2,000) - totalDisbursed (10,000) = 19,000 payable
        assertEquals(19000.0, repoResult.payableBalance, 0.001)
        assertEquals(19000.0, repoResult.balance, 0.001)
        assertEquals(repoResult.balance, ledgerResult.finalBalance, 0.001)

        val items = LedgerRunningBalance.buildItems(owner, emptyList(), vouchers, purchases)
        val p1 = items.first { it is CustomerLedgerItem.PurchaseItem && it.purchase.id == 1L }
        val e1 = items.first { it is CustomerLedgerItem.DisbursementItem && it.voucher.id == 201L }
        val r1 = items.first { it is CustomerLedgerItem.ReceiptItem && it.voucher.id == 202L }

        assertEquals(27000.0, ledgerResult.balancesByItem[p1] ?: 0.0, 0.001)
        assertEquals(17000.0, ledgerResult.balancesByItem[e1] ?: 0.0, 0.001)
        assertEquals(19000.0, ledgerResult.balancesByItem[r1] ?: 0.0, 0.001)
    }

    @Test
    fun `session-linked receipt does not double count in ledger running balance`() {
        val customer = Customer(id = 30, name = "مزارع سعيد", isWellOwner = false)

        val session = WaterSession(
            id = 5,
            customerId = customer.id,
            startTime = 1000L,
            durationMinutes = 60,
            totalAmount = 4000.0,
            amountPaid = 4000.0,
            remainingDebt = 0.0
        )
        val linkedReceipt = Voucher(
            id = 301,
            customerId = customer.id,
            sessionId = session.id,
            date = 1050L,
            type = VoucherType.RECEIPT,
            amount = 4000.0
        )

        val repoResult = calculateCustomerBalance(customer, listOf(session), listOf(linkedReceipt))
        val ledgerResult = LedgerRunningBalance.compute(customer, listOf(session), listOf(linkedReceipt))

        assertEquals(0.0, repoResult.receivableBalance, 0.001)
        assertEquals(0.0, repoResult.balance, 0.001)
        assertEquals(repoResult.balance, ledgerResult.finalBalance, 0.001)
    }

    @Test
    fun `formatRunningBalance outputs proper extraNote strings for customer and well owner`() {
        // Normal customer in debt
        val noteDebt = LedgerRunningBalance.formatRunningBalance(5000.0, isWellOwner = false, currencySymbol = "ر.ي")
        assertTrue(noteDebt.contains("الرصيد بعد العملية:"))
        assertTrue(noteDebt.contains("5,000"))
        assertTrue(noteDebt.contains("(عليه)"))

        // Normal customer in credit
        val noteCredit = LedgerRunningBalance.formatRunningBalance(-1500.0, isWellOwner = false, currencySymbol = "ر.ي")
        assertTrue(noteCredit.contains("1,500"))
        assertTrue(noteCredit.contains("(له)"))

        // Normal customer settled
        val noteSettled = LedgerRunningBalance.formatRunningBalance(0.0, isWellOwner = false, currencySymbol = "ر.ي")
        assertTrue(noteSettled.contains("مسدد"))

        // Well owner payable
        val noteOwnerPayable = LedgerRunningBalance.formatRunningBalance(12000.0, isWellOwner = true, currencySymbol = "ر.ي")
        assertTrue(noteOwnerPayable.contains("12,000"))
        assertTrue(noteOwnerPayable.contains("(له)"))

        // Well owner receivable
        val noteOwnerReceivable = LedgerRunningBalance.formatRunningBalance(-3000.0, isWellOwner = true, currencySymbol = "ر.ي")
        assertTrue(noteOwnerReceivable.contains("3,000"))
        assertTrue(noteOwnerReceivable.contains("(عليه)"))
    }
}
