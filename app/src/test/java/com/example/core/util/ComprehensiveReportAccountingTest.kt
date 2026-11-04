package com.example.core.util

import com.example.core.util.ComprehensiveReportMath.OwnerBalanceType
import com.example.features.customers.Customer
import com.example.features.customers.CustomerWithBalance
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveReportAccountingTest {

    @Test
    fun testComprehensiveReportAccountingSeparation() {
        // 1. Regular farmer
        val farmer = Customer(
            id = 1L,
            name = "مزارع صالح",
            farmName = "مزرعة الوفاء",
            phone = "777000001",
            isWellOwner = false
        )
        val farmerSessions = listOf(
            WaterSession(
                id = 101L,
                customerId = farmer.id,
                durationMinutes = 120,
                totalAmount = 50000.0,
                amountPaid = 20000.0
            )
        )
        val farmerBalance = calculateCustomerBalance(
            customer = farmer,
            sessions = farmerSessions,
            vouchers = emptyList()
        )

        val farmerRow = ComprehensiveCustomerRow(
            customerName = farmer.name,
            phone = farmer.phone,
            farmName = farmer.farmName.ifEmpty { farmer.location },
            sessionsCount = farmerBalance.totalSessionsCount,
            waterMinutes = farmerBalance.totalSoldMinutes,
            billed = farmerBalance.totalBilledAmount,
            paid = farmerBalance.totalPaidAmount,
            balance = farmerBalance.balance,
            isWellOwner = farmer.isWellOwner,
            purchasedMinutes = farmerBalance.totalPurchasedMinutes,
            chargeablePurchasedMinutes = farmerBalance.totalChargeablePurchasedMinutes,
            ownerWasteMinutes = (farmerBalance.totalPurchasedMinutes - farmerBalance.totalChargeablePurchasedMinutes).coerceAtLeast(0),
            ownerWasteCredit = farmerBalance.totalOwnerWasteCredit,
            purchaseAmount = farmerBalance.totalPurchaseAmount,
            ownerPayments = farmerBalance.totalDisbursedAmount,
            payableBalance = farmerBalance.payableBalance,
            receivableBalance = farmerBalance.receivableBalance
        )

        // 2. Well owner with sales 78,250, collected 46,853, and purchases that give a net payable
        val owner = Customer(
            id = 2L,
            name = "عمر بن سالم الأهدل",
            farmName = "بئر الأهدل",
            phone = "777000002",
            isWellOwner = true
        )
        val ownerSessions = listOf(
            WaterSession(
                id = 201L,
                customerId = owner.id,
                durationMinutes = 180,
                totalAmount = 78250.0,
                amountPaid = 46853.0
            )
        )
        // Purchases and disbursements: netPurchaseAmount = 181,397, disbursed = 47,147
        // netOwnerPayable = (181,397 + 46,853) - (47,147 + 78,250) = 228,250 - 125,397 = 102,853 payable
        val ownerPurchases = listOf(
            WellOwnerPurchase(
                id = 301L,
                ownerCustomerId = owner.id,
                durationMinutes = 60,
                wastedMinutesOnOwner = 0,
                purchaseRatePerHour = 181397.0
            )
        )
        val ownerVouchers = listOf(
            Voucher(
                id = 401L,
                customerId = owner.id,
                type = VoucherType.EXPENSE,
                amount = 47147.0
            )
        )
        val ownerBalance = calculateCustomerBalance(
            customer = owner,
            sessions = ownerSessions,
            vouchers = ownerVouchers,
            purchases = ownerPurchases
        )

        val ownerRow = ComprehensiveCustomerRow(
            customerName = owner.name,
            phone = owner.phone,
            farmName = owner.farmName.ifEmpty { owner.location },
            sessionsCount = ownerBalance.totalSessionsCount,
            waterMinutes = ownerBalance.totalSoldMinutes,
            billed = ownerBalance.totalBilledAmount,
            paid = ownerBalance.totalPaidAmount,
            balance = ownerBalance.balance,
            isWellOwner = owner.isWellOwner,
            purchasedMinutes = ownerBalance.totalPurchasedMinutes,
            chargeablePurchasedMinutes = ownerBalance.totalChargeablePurchasedMinutes,
            ownerWasteMinutes = (ownerBalance.totalPurchasedMinutes - ownerBalance.totalChargeablePurchasedMinutes).coerceAtLeast(0),
            ownerWasteCredit = ownerBalance.totalOwnerWasteCredit,
            purchaseAmount = ownerBalance.totalPurchaseAmount,
            ownerPayments = ownerBalance.totalDisbursedAmount,
            payableBalance = ownerBalance.payableBalance,
            receivableBalance = ownerBalance.receivableBalance
        )

        val customerRows = listOf(farmerRow, ownerRow)

        // 3. Assertions
        // Page 1 Balance Cell choice:
        val (farmerCellText, farmerIsDebt) = ComprehensiveReportMath.page1BalanceCell(farmerRow)
        assertEquals("Farmer cell must equal his receivable (30,000)", Formatters.formatNumber(30000.0), farmerCellText)
        assertTrue("Farmer with unpaid balance must be marked as debt (isDebt = true)", farmerIsDebt)
        assertEquals("Farmer receivable balance must be exactly 30,000", 30000.0, farmerRow.receivableBalance, 0.01)

        val (ownerCellText, ownerIsDebt) = ComprehensiveReportMath.page1BalanceCell(ownerRow)
        assertTrue("Owner cell must start with 'له '", ownerCellText.startsWith("له "))
        assertEquals("Owner cell must show 'له 102,853'", "له ${Formatters.formatNumber(102853.0)}", ownerCellText)
        assertFalse("Owner cell with payableBalance > 0 must not be debt (isDebt = false, green)", ownerIsDebt)
        assertEquals("Owner payable balance must be 102,853", 102853.0, ownerRow.payableBalance, 0.01)
        assertEquals("Owner receivable balance must be 0 (already netted into payable)", 0.0, ownerRow.receivableBalance, 0.01)

        // Page 1 Grand Total row & "ديون العملاء" card: sum of receivableBalance across all customers
        val totalCustomerDebt = customerRows.sumOf { it.receivableBalance }
        assertEquals("Debt total must exclude the owner: 30,000 + 0 = 30,000", 30000.0, totalCustomerDebt, 0.01)

        // "مستحقات أصحاب الآبار" card
        val totalOwnerPayables = customerRows.filter { it.isWellOwner }.sumOf { it.payableBalance }
        assertEquals("Owner payables must be 102,853", 102853.0, totalOwnerPayables, 0.01)
    }

    @Test
    fun testOwnerNetBalanceCell() {
        // 1. Owner with net payable (payableBalance > 0) -> "له X", PAYABLE
        val payableOwner = Customer(id = 11L, name = "مالك دائن", isWellOwner = true)
        val payablePurchases = listOf(
            WellOwnerPurchase(id = 101L, ownerCustomerId = payableOwner.id, durationMinutes = 60, purchaseRatePerHour = 100000.0)
        )
        val payableSessions = listOf(
            WaterSession(id = 201L, customerId = payableOwner.id, durationMinutes = 60, totalAmount = 30000.0, amountPaid = 0.0)
        )
        val payableBalance = calculateCustomerBalance(
            customer = payableOwner,
            sessions = payableSessions,
            vouchers = emptyList(),
            purchases = payablePurchases
        )
        val payableRow = toRow(payableOwner, payableBalance)
        val payableCell = ComprehensiveReportMath.ownerNetBalanceCell(payableRow)
        assertEquals(OwnerBalanceType.PAYABLE, payableCell.type)
        assertEquals("له ${Formatters.formatNumber(70000.0)}", payableCell.text)

        // 2. Owner with net receivable (receivableBalance > 0, sales > purchases) -> "عليه X", RECEIVABLE
        val receivableOwner = Customer(id = 12L, name = "مالك مدين", isWellOwner = true)
        val receivablePurchases = listOf(
            WellOwnerPurchase(id = 102L, ownerCustomerId = receivableOwner.id, durationMinutes = 60, purchaseRatePerHour = 30000.0)
        )
        val receivableSessions = listOf(
            WaterSession(id = 202L, customerId = receivableOwner.id, durationMinutes = 60, totalAmount = 80000.0, amountPaid = 0.0)
        )
        val receivableBalance = calculateCustomerBalance(
            customer = receivableOwner,
            sessions = receivableSessions,
            vouchers = emptyList(),
            purchases = receivablePurchases
        )
        val receivableRow = toRow(receivableOwner, receivableBalance)
        val receivableCell = ComprehensiveReportMath.ownerNetBalanceCell(receivableRow)
        assertEquals(OwnerBalanceType.RECEIVABLE, receivableCell.type)
        assertEquals("عليه ${Formatters.formatNumber(50000.0)}", receivableCell.text)

        // 3. Settled owner (balance is 0) -> "مسدد", SETTLED
        val settledOwner = Customer(id = 13L, name = "مالك مسدد", isWellOwner = true)
        val settledPurchases = listOf(
            WellOwnerPurchase(id = 103L, ownerCustomerId = settledOwner.id, durationMinutes = 60, purchaseRatePerHour = 50000.0)
        )
        val settledSessions = listOf(
            WaterSession(id = 203L, customerId = settledOwner.id, durationMinutes = 60, totalAmount = 50000.0, amountPaid = 0.0)
        )
        val settledBalance = calculateCustomerBalance(
            customer = settledOwner,
            sessions = settledSessions,
            vouchers = emptyList(),
            purchases = settledPurchases
        )
        val settledRow = toRow(settledOwner, settledBalance)
        val settledCell = ComprehensiveReportMath.ownerNetBalanceCell(settledRow)
        assertEquals(OwnerBalanceType.SETTLED, settledCell.type)
        assertEquals("مسدد", settledCell.text)
    }

    private fun toRow(customer: Customer, balance: CustomerWithBalance): ComprehensiveCustomerRow {
        return ComprehensiveCustomerRow(
            customerName = customer.name,
            phone = customer.phone,
            farmName = customer.farmName.ifEmpty { customer.location },
            sessionsCount = balance.totalSessionsCount,
            waterMinutes = balance.totalSoldMinutes,
            billed = balance.totalBilledAmount,
            paid = balance.totalPaidAmount,
            balance = balance.balance,
            isWellOwner = customer.isWellOwner,
            purchasedMinutes = balance.totalPurchasedMinutes,
            chargeablePurchasedMinutes = balance.totalChargeablePurchasedMinutes,
            ownerWasteMinutes = (balance.totalPurchasedMinutes - balance.totalChargeablePurchasedMinutes).coerceAtLeast(0),
            ownerWasteCredit = balance.totalOwnerWasteCredit,
            purchaseAmount = balance.totalPurchaseAmount,
            ownerPayments = balance.totalDisbursedAmount,
            payableBalance = balance.payableBalance,
            receivableBalance = balance.receivableBalance
        )
    }
}
