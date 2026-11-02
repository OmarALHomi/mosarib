package com.example

import com.example.features.customers.CustomerFormSnapshot
import com.example.features.sessions.SessionFormSnapshot
import com.example.features.vouchers.VoucherFormSnapshot
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.PurchaseFormSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormSnapshotTest {

    // --- 1. VoucherFormSnapshot Tests ---

    @Test
    fun `voucher snapshot unchanged gives false for hasUnsavedChanges`() {
        val initial = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "5000",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "دفعة حساب",
            selectedSessionIds = setOf(1L, 2L)
        )
        val current = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "5000",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "دفعة حساب",
            selectedSessionIds = setOf(1L, 2L)
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    @Test
    fun `voucher snapshot changed amount gives true for hasUnsavedChanges`() {
        val initial = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "5000",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "",
            selectedSessionIds = emptySet()
        )
        val current = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "6000",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "",
            selectedSessionIds = emptySet()
        )
        val hasUnsavedChanges = current != initial
        assertTrue(hasUnsavedChanges)
    }

    @Test
    fun `voucher snapshot formatted amount 5,000 vs 5000 gives false for hasUnsavedChanges`() {
        val initial = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "5000.0",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "ملاحظة  ",
            selectedSessionIds = emptySet()
        )
        val current = VoucherFormSnapshot.from(
            type = VoucherType.RECEIPT,
            customerId = 10L,
            beneficiaryId = null,
            amountStr = "5,000",
            discountStr = "",
            paymentMethod = "نقداً",
            notes = "  ملاحظة",
            selectedSessionIds = emptySet()
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    // --- 2. SessionFormSnapshot Tests ---

    @Test
    fun `session snapshot unchanged gives false for hasUnsavedChanges`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد أحمد",
            billedToCustomerId = null,
            grossMinutes = 120,
            amountPaidStr = "4000",
            discountAmountStr = "500",
            wastedMinutes = 15,
            wastedReason = "عطل دينمو",
            notes = "سقي مزرعة الغيل",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد أحمد",
            billedToCustomerId = null,
            grossMinutes = 120,
            amountPaidStr = "4000",
            discountAmountStr = "500",
            wastedMinutes = 15,
            wastedReason = "عطل دينمو",
            notes = "سقي مزرعة الغيل",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    @Test
    fun `session snapshot changed gross minutes gives true for hasUnsavedChanges`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 90,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val hasUnsavedChanges = current != initial
        assertTrue(hasUnsavedChanges)
    }

    @Test
    fun `session snapshot formatted amounts 4,500 vs 4500 gives false for hasUnsavedChanges`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "  محمد أحمد  ",
            billedToCustomerId = null,
            grossMinutes = 120,
            amountPaidStr = "4500.0",
            discountAmountStr = "500",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "عام",
            pricePerHourStr = "2,000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد أحمد",
            billedToCustomerId = null,
            grossMinutes = 120,
            amountPaidStr = "4,500",
            discountAmountStr = "500.0",
            wastedMinutes = 0,
            wastedReason = "",
            notes = " عام ",
            pricePerHourStr = "2000.0",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    @Test
    fun `session snapshot changed pricePerHour gives true for hasUnsavedChanges`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2500",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        assertTrue(current != initial)
    }

    @Test
    fun `session snapshot changed startTimeMinute gives true for hasUnsavedChanges even if duration unchanged`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L, // time A
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L + 3600000L, // time A + 1 hour
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        assertTrue(current != initial)
    }

    @Test
    fun `session snapshot changed quick customer fields gives true for hasUnsavedChanges`() {
        val base = SessionFormSnapshot.from(
            customerId = 0L,
            customerInputText = "",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val withName = base.copy(quickName = "علي")
        assertTrue(withName != base)

        val withPhone = base.copy(quickPhone = "777123456")
        assertTrue(withPhone != base)

        val withOwner = base.copy(quickIsWellOwner = true)
        assertTrue(withOwner != base)
    }

    @Test
    fun `session snapshot preselected customer ignores customerInputText changes`() {
        // When customerId > 0, customerInputText filling in later does NOT trigger hasUnsavedChanges
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "", // initial frame before name resolution
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد أحمد", // resolved in subsequent frame
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        assertFalse(current != initial)
    }

    @Test
    fun `session snapshot preselected customer triggers change when customerId changes`() {
        val initial = SessionFormSnapshot.from(
            customerId = 5L,
            customerInputText = "محمد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val changedCustomer = SessionFormSnapshot.from(
            customerId = 8L,
            customerInputText = "سعيد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        assertTrue(changedCustomer != initial)
    }

    @Test
    fun `session snapshot unselected customer triggers change when customerInputText changes`() {
        val initial = SessionFormSnapshot.from(
            customerId = 0L,
            customerInputText = "",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        val current = SessionFormSnapshot.from(
            customerId = 0L,
            customerInputText = "زبون جديد",
            billedToCustomerId = null,
            grossMinutes = 60,
            amountPaidStr = "",
            discountAmountStr = "",
            wastedMinutes = 0,
            wastedReason = "",
            notes = "",
            pricePerHourStr = "2000",
            startTimeMillis = 1700000000000L,
            quickName = "",
            quickPhone = "",
            quickIsWellOwner = false
        )
        assertTrue(current != initial)
    }

    // --- 3. PurchaseFormSnapshot Tests ---

    @Test
    fun `purchase snapshot unchanged gives false for hasUnsavedChanges`() {
        val initial = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 30,
            rateStr = "3000",
            amountPaidStr = "2000",
            notes = "شراء ساعات ري"
        )
        val current = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 30,
            rateStr = "3000",
            amountPaidStr = "2000",
            notes = "شراء ساعات ري"
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    @Test
    fun `purchase snapshot changed rate gives true for hasUnsavedChanges`() {
        val initial = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 0,
            rateStr = "3000",
            amountPaidStr = "",
            notes = ""
        )
        val current = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 0,
            rateStr = "3500",
            amountPaidStr = "",
            notes = ""
        )
        val hasUnsavedChanges = current != initial
        assertTrue(hasUnsavedChanges)
    }

    @Test
    fun `purchase snapshot formatted rate 3,000 vs 3000 gives false for hasUnsavedChanges`() {
        val initial = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 0,
            rateStr = "3000.0",
            amountPaidStr = "1,500",
            notes = "  ملاحظة  "
        )
        val current = PurchaseFormSnapshot.from(
            ownerId = 8L,
            durationMinutes = 180,
            wastedMinutes = 0,
            rateStr = "3,000",
            amountPaidStr = "1500.0",
            notes = "ملاحظة"
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    // --- 4. CustomerFormSnapshot Tests ---

    @Test
    fun `customer snapshot unchanged gives false for hasUnsavedChanges`() {
        val initial = CustomerFormSnapshot.from(
            name = "صالح اليافعي",
            phone = "777123456",
            farmName = "مزرعة الجبل",
            location = "صنعاء",
            notes = "زبون دائم",
            customGroup = "مجموعة أ",
            isWellOwner = false,
            isBeneficiary = false,
            customPriceStr = "4000"
        )
        val current = CustomerFormSnapshot.from(
            name = "صالح اليافعي",
            phone = "777123456",
            farmName = "مزرعة الجبل",
            location = "صنعاء",
            notes = "زبون دائم",
            customGroup = "مجموعة أ",
            isWellOwner = false,
            isBeneficiary = false,
            customPriceStr = "4000"
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }

    @Test
    fun `customer snapshot changed phone gives true for hasUnsavedChanges`() {
        val initial = CustomerFormSnapshot.from(
            name = "صالح",
            phone = "777123456",
            farmName = "",
            location = "",
            notes = "",
            customGroup = "",
            isWellOwner = false,
            isBeneficiary = false,
            customPriceStr = ""
        )
        val current = CustomerFormSnapshot.from(
            name = "صالح",
            phone = "777654321",
            farmName = "",
            location = "",
            notes = "",
            customGroup = "",
            isWellOwner = false,
            isBeneficiary = false,
            customPriceStr = ""
        )
        val hasUnsavedChanges = current != initial
        assertTrue(hasUnsavedChanges)
    }

    @Test
    fun `customer snapshot formatted price 4,000 vs 4000 and trimmed strings gives false for hasUnsavedChanges`() {
        val initial = CustomerFormSnapshot.from(
            name = "  صالح اليافعي  ",
            phone = " 777123456 ",
            farmName = "مزرعة الجبل",
            location = " صنعاء ",
            notes = " ملاحظات ",
            customGroup = " مجموعة أ ",
            isWellOwner = true,
            isBeneficiary = false,
            customPriceStr = "4,000"
        )
        val current = CustomerFormSnapshot.from(
            name = "صالح اليافعي",
            phone = "777123456",
            farmName = "  مزرعة الجبل  ",
            location = "صنعاء",
            notes = "ملاحظات",
            customGroup = "مجموعة أ",
            isWellOwner = true,
            isBeneficiary = false,
            customPriceStr = "4000.0"
        )
        val hasUnsavedChanges = current != initial
        assertFalse(hasUnsavedChanges)
    }
}
