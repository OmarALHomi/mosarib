package com.example.core.util

import com.example.features.customers.Customer
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
}
