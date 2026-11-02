package com.example.features.vouchers

import com.example.features.customers.Customer
import com.example.features.reports.ReportPeriod
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.wellowners.WellOwnerPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class OperationsFilterEngineTest {

    private val c1 = Customer(id = 1L, name = "صالح بن علي", customGroup = "مجموعة الشرق", phone = "777111222")
    private val c2 = Customer(id = 2L, name = "محمد ناصر", customGroup = "", phone = "777333444")
    private val c3 = Customer(id = 3L, name = "أحمد اليافعي", customGroup = "مجموعة الغرب", phone = "777555666", isWellOwner = true)
    private val customersMap = mapOf(1L to c1, 2L to c2, 3L to c3)

    // Base calendar: Nov 15, 2026
    private val baseCal = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
    }
    private val now = baseCal.timeInMillis

    private val sessionNov = UnifiedOperation.SessionOp(
        WaterSessionWithCustomer(
            session = WaterSession(
                id = 101L,
                customerId = 1L,
                startTime = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 10, 8, 0, 0) }.timeInMillis,
                totalAmount = 5000.0,
                amountPaid = 5000.0,
                remainingDebt = 0.0,
                pumpName = "مضخة 1",
                notes = "سقي قصب"
            ),
            customer = c1
        )
    )

    private val sessionOct = UnifiedOperation.SessionOp(
        WaterSessionWithCustomer(
            session = WaterSession(
                id = 102L,
                customerId = 2L,
                startTime = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 9, 0, 0) }.timeInMillis,
                totalAmount = 8000.0,
                amountPaid = 2000.0,
                remainingDebt = 6000.0,
                pumpName = "مضخة 2",
                notes = "سقي ذرة"
            ),
            customer = c2
        )
    )

    private val sessionUnpaid = UnifiedOperation.SessionOp(
        WaterSessionWithCustomer(
            session = WaterSession(
                id = 103L,
                customerId = 1L,
                startTime = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12, 10, 0, 0) }.timeInMillis,
                totalAmount = 3000.0,
                amountPaid = 0.0,
                remainingDebt = 3000.0,
                pumpName = "مضخة 1",
                notes = "سقي برسيم"
            ),
            customer = c1
        )
    )

    private val voucherReceipt = UnifiedOperation.VoucherOp(
        VoucherWithCustomer(
            voucher = Voucher(
                id = 201L,
                voucherNumber = "REC-101",
                type = VoucherType.RECEIPT,
                customerId = 1L,
                amount = 4000.0,
                paymentMethod = "نقداً",
                date = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 11, 14, 0, 0) }.timeInMillis,
                notes = "قبض كاش"
            ),
            customer = c1
        )
    )

    private val voucherExpense = UnifiedOperation.VoucherOp(
        VoucherWithCustomer(
            voucher = Voucher(
                id = 202L,
                voucherNumber = "EXP-102",
                type = VoucherType.EXPENSE,
                customerId = null,
                amount = 1500.0,
                paymentMethod = "شيك",
                category = "صيانة",
                date = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 14, 16, 0, 0) }.timeInMillis,
                notes = "تبديل زيت"
            ),
            customer = null
        )
    )

    private val voucherDiscount = UnifiedOperation.VoucherOp(
        VoucherWithCustomer(
            voucher = Voucher(
                id = 203L,
                voucherNumber = "DISC-103",
                type = VoucherType.DISCOUNT,
                customerId = 1L,
                amount = 500.0,
                paymentMethod = "خصم",
                date = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 14, 18, 0, 0) }.timeInMillis,
                notes = "خصم خاص"
            ),
            customer = c1
        )
    )

    private val purchaseOp = UnifiedOperation.PurchaseOp(
        purchase = WellOwnerPurchase(
            id = 301L,
            ownerCustomerId = 3L,
            date = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 13, 11, 0, 0) }.timeInMillis,
            startTime = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 13, 11, 0, 0) }.timeInMillis,
            durationMinutes = 120,
            purchaseRatePerHour = 4000.0,
            notes = "شراء ساعتين"
        ),
        owner = c3
    )

    private val allOps = listOf(sessionNov, sessionOct, sessionUnpaid, voucherReceipt, voucherExpense, voucherDiscount, purchaseOp)

    @Test
    fun testPeriodThisMonth() {
        val state = OperationsFilterState(period = ReportPeriod.THIS_MONTH)
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        // October session should be excluded
        assertTrue(filtered.contains(sessionNov))
        assertTrue(filtered.contains(sessionUnpaid))
        assertTrue(filtered.contains(voucherReceipt))
        assertTrue(filtered.contains(voucherExpense))
        assertTrue(filtered.contains(voucherDiscount))
        assertTrue(filtered.contains(purchaseOp))
        assertTrue(!filtered.contains(sessionOct))
    }

    @Test
    fun testCustomRangeInclusive() {
        val startCal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1, 0, 0, 0) }
        val endCal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 31, 23, 59, 59) }
        val state = OperationsFilterState(
            period = ReportPeriod.CUSTOM,
            customRange = Pair(startCal.timeInMillis, endCal.timeInMillis)
        )
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(1, filtered.size)
        assertEquals(sessionOct, filtered[0])
    }

    @Test
    fun testGroupFilterIncludingNoGroup() {
        // Group "مجموعة الشرق"
        val stateEast = OperationsFilterState(group = "مجموعة الشرق")
        val filteredEast = OperationsFilterEngine.apply(allOps, stateEast, customersMap, "", now)
        assertTrue(filteredEast.all { op ->
            when (op) {
                is UnifiedOperation.SessionOp -> op.sessionWithCustomer.customer?.customGroup == "مجموعة الشرق"
                is UnifiedOperation.VoucherOp -> op.voucherWithCustomer.customer?.customGroup == "مجموعة الشرق"
                is UnifiedOperation.PurchaseOp -> op.owner?.customGroup == "مجموعة الشرق"
            }
        })
        assertEquals(4, filteredEast.size) // sessionNov, sessionUnpaid, voucherReceipt, voucherDiscount

        // Group "" (بدون مجموعة)
        val stateNoGroup = OperationsFilterState(group = "")
        val filteredNoGroup = OperationsFilterEngine.apply(allOps, stateNoGroup, customersMap, "", now)
        assertEquals(1, filteredNoGroup.size)
        assertEquals(sessionOct, filteredNoGroup[0])
    }

    @Test
    fun testSingleCustomerFilter() {
        val state = OperationsFilterState(customerId = 1L)
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(4, filtered.size)
        assertTrue(filtered.contains(sessionNov))
        assertTrue(filtered.contains(sessionUnpaid))
        assertTrue(filtered.contains(voucherReceipt))
        assertTrue(filtered.contains(voucherDiscount))
    }

    @Test
    fun testSessionPaymentStates() {
        // PAID
        val statePaid = OperationsFilterState(sessionPayment = SessionPaymentFilter.PAID)
        val filteredPaid = OperationsFilterEngine.apply(allOps, statePaid, customersMap, "", now)
        assertEquals(1, filteredPaid.size)
        assertEquals(sessionNov, filteredPaid[0])

        // UNPAID
        val stateUnpaid = OperationsFilterState(sessionPayment = SessionPaymentFilter.UNPAID)
        val filteredUnpaid = OperationsFilterEngine.apply(allOps, stateUnpaid, customersMap, "", now)
        assertEquals(1, filteredUnpaid.size)
        assertEquals(sessionUnpaid, filteredUnpaid[0])

        // PARTIAL
        val statePartial = OperationsFilterState(sessionPayment = SessionPaymentFilter.PARTIAL)
        val filteredPartial = OperationsFilterEngine.apply(allOps, statePartial, customersMap, "", now)
        assertEquals(1, filteredPartial.size)
        assertEquals(sessionOct, filteredPartial[0])
    }

    @Test
    fun testPaymentMethodsFilter() {
        val state = OperationsFilterState(paymentMethods = setOf("شيك"))
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(1, filtered.size)
        assertEquals(voucherExpense, filtered[0])
    }

    @Test
    fun testPumpNamesFilter() {
        val state = OperationsFilterState(pumpNames = setOf("مضخة 2"))
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(1, filtered.size)
        assertEquals(sessionOct, filtered[0])
    }

    @Test
    fun testPaymentMethodsAndPumpNamesBothSet() {
        // Both set: vouchers matching method OR sessions matching pump; purchases hidden
        val state = OperationsFilterState(
            paymentMethods = setOf("شيك"),
            pumpNames = setOf("مضخة 2")
        )
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(2, filtered.size)
        assertTrue(filtered.contains(voucherExpense))
        assertTrue(filtered.contains(sessionOct))
        assertFalse(filtered.contains(purchaseOp))
        assertFalse(filtered.contains(sessionNov))
        assertFalse(filtered.contains(voucherReceipt))
    }

    @Test
    fun testSortOrders() {
        // Amount desc
        val stateAmount = OperationsFilterState(sort = OperationsSort.AMOUNT_DESC)
        val filteredAmount = OperationsFilterEngine.apply(allOps, stateAmount, customersMap, "", now)
        val amounts = filteredAmount.map { it.amount }
        assertEquals(amounts.sortedDescending(), amounts)

        // Oldest
        val stateOldest = OperationsFilterState(sort = OperationsSort.OLDEST)
        val filteredOldest = OperationsFilterEngine.apply(allOps, stateOldest, customersMap, "", now)
        val times = filteredOldest.map { it.timestamp }
        assertEquals(times.sorted(), times)
    }

    @Test
    fun testSearchCombinedWithFilter() {
        val state = OperationsFilterState(period = ReportPeriod.THIS_MONTH)
        // Search "قصب" (in notes of sessionNov)
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "قصب", now)
        assertEquals(1, filtered.size)
        assertEquals(sessionNov, filtered[0])
    }

    @Test
    fun testActiveCountAndDescribe() {
        val stateDefault = OperationsFilterState()
        assertEquals(0, stateDefault.activeCount)
        assertEquals("كل الوقت", OperationsFilterEngine.describe(stateDefault))

        val stateActive = OperationsFilterState(
            period = ReportPeriod.THIS_MONTH,
            group = "مجموعة الشرق",
            sort = OperationsSort.AMOUNT_DESC
        )
        assertEquals(3, stateActive.activeCount)
        val desc = OperationsFilterEngine.describe(stateActive)
        assertTrue(desc.contains("هذا الشهر"))
        assertTrue(desc.contains("مجموعة الشرق"))
        assertTrue(desc.contains("الأعلى مبلغاً"))
    }

    @Test
    fun testAccountTypeFilter() {
        // FARMER: customer is not well owner; purchases excluded
        val stateFarmer = OperationsFilterState(accountType = AccountTypeFilter.FARMER)
        val filteredFarmer = OperationsFilterEngine.apply(allOps, stateFarmer, customersMap, "", now)
        assertTrue(filteredFarmer.contains(sessionNov))
        assertTrue(filteredFarmer.contains(sessionOct))
        assertTrue(filteredFarmer.contains(sessionUnpaid))
        assertTrue(filteredFarmer.contains(voucherReceipt))
        assertTrue(filteredFarmer.contains(voucherDiscount))
        assertFalse(filteredFarmer.contains(purchaseOp))

        // WELL_OWNER: customer is well owner or purchase
        val stateWellOwner = OperationsFilterState(accountType = AccountTypeFilter.WELL_OWNER)
        val filteredWellOwner = OperationsFilterEngine.apply(allOps, stateWellOwner, customersMap, "", now)
        assertEquals(1, filteredWellOwner.size)
        assertEquals(purchaseOp, filteredWellOwner[0])
    }

    @Test
    fun testDiscountsFilter() {
        val state = OperationsFilterState(type = OperationsFilter.DISCOUNTS)
        val filtered = OperationsFilterEngine.apply(allOps, state, customersMap, "", now)
        assertEquals(1, filtered.size)
        assertEquals(voucherDiscount, filtered[0])
    }

    @Test
    fun testAdditionalSortOrders() {
        // AMOUNT_ASC
        val stateAmountAsc = OperationsFilterState(sort = OperationsSort.AMOUNT_ASC)
        val filteredAmountAsc = OperationsFilterEngine.apply(allOps, stateAmountAsc, customersMap, "", now)
        val amounts = filteredAmountAsc.map { it.amount }
        assertEquals(amounts.sorted(), amounts)

        // NAME_ASC
        val stateNameAsc = OperationsFilterState(sort = OperationsSort.NAME_ASC)
        val filteredNameAsc = OperationsFilterEngine.apply(allOps, stateNameAsc, customersMap, "", now)
        assertEquals(allOps.size, filteredNameAsc.size)

        // NAME_DESC
        val stateNameDesc = OperationsFilterState(sort = OperationsSort.NAME_DESC)
        val filteredNameDesc = OperationsFilterEngine.apply(allOps, stateNameDesc, customersMap, "", now)
        assertEquals(allOps.size, filteredNameDesc.size)
    }
}
