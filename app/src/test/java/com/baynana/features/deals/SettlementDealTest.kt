package com.baynana.features.deals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementDealTest {

    @Test
    fun dealCompletion_whenRemainingZero_returnsTrue() {
        val deal = SettlementDeal(
            id = "deal-1",
            dealNumber = "SLH-1001",
            cropTitle = "مزرعة رمان",
            cropType = "رمان",
            sellerName = "أحمد",
            buyerName = "محمد",
            totalAmount = 1000000.0,
            advancePayment = 1000000.0,
            remainingAmount = 0.0,
            status = "COMPLETED"
        )

        assertTrue(deal.isCompleted)
    }

    @Test
    fun dealCompletion_whenRemainingPositive_returnsFalse() {
        val deal = SettlementDeal(
            id = "deal-2",
            dealNumber = "SLH-1002",
            cropTitle = "مزرعة قات شامي",
            cropType = "قات",
            sellerName = "صالح",
            buyerName = "علي",
            totalAmount = 2000000.0,
            advancePayment = 500000.0,
            remainingAmount = 1500000.0,
            status = "ACTIVE"
        )

        assertFalse(deal.isCompleted)
    }

    @Test
    fun contractText_containsAllPartiesAndAmounts() {
        val deal = SettlementDeal(
            id = "deal-3",
            dealNumber = "SLH-5555",
            cropTitle = "عنب رازقي",
            cropType = "عنب",
            location = "بني حشيش",
            sellerName = "الحاج يحيى",
            sellerPhone = "771111111",
            buyerName = "أبو طارق",
            buyerPhone = "772222222",
            dallalName = "الشيخ ناصر",
            dallalPhone = "773333333",
            totalAmount = 3000000.0,
            advancePayment = 1000000.0,
            dallalCommission = 150000.0,
            remainingAmount = 2000000.0,
            status = "ACTIVE",
            termsNotes = "تسليم الثمرة بعد أسبوعين"
        )

        val contract = SettlementDocHelper.generateContractText(deal)

        assertTrue(contract.contains("SLH-5555"))
        assertTrue(contract.contains("الحاج يحيى"))
        assertTrue(contract.contains("أبو طارق"))
        assertTrue(contract.contains("الشيخ ناصر"))
        assertTrue(contract.contains("3,000,000"))
        assertTrue(contract.contains("1,000,000"))
        assertTrue(contract.contains("2,000,000"))
        assertTrue(contract.contains("150,000"))
        assertTrue(contract.contains("بني حشيش"))
        assertTrue(contract.contains("تسليم الثمرة بعد أسبوعين"))
    }
}
