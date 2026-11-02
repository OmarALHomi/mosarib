package com.example.features.reports

import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.AccountTypeFilter
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReportsFilterTest {

    private val cust1 = Customer(id = 1L, name = "صالح", customGroup = "الشرق", isWellOwner = false)
    private val cust2 = Customer(id = 2L, name = "محمد", customGroup = "الشرق", isWellOwner = true)
    private val cust3 = Customer(id = 3L, name = "علي", customGroup = "الغرب", isWellOwner = false)
    private val cust4 = Customer(id = 4L, name = "حسن", customGroup = "", isWellOwner = false)
    private val allCustomers = listOf(cust1, cust2, cust3, cust4)

    // 1. Tests for Customer Filtering by Group and Account Type
    @Test
    fun testCustomerFilteringByGroupAndAccountType() {
        // Group = null, AccountType = ALL -> returns all
        val all = ReportsFilter.filterCustomers(allCustomers, group = null, accountType = AccountTypeFilter.ALL)
        assertEquals(4, all.size)

        // Specific group "الشرق"
        val sharq = ReportsFilter.filterCustomers(allCustomers, group = "الشرق", accountType = AccountTypeFilter.ALL)
        assertEquals(listOf(cust1, cust2), sharq)

        // Empty group "" (without group)
        val noGroup = ReportsFilter.filterCustomers(allCustomers, group = "", accountType = AccountTypeFilter.ALL)
        assertEquals(listOf(cust4), noGroup)

        // Farmers only
        val farmers = ReportsFilter.filterCustomers(allCustomers, group = null, accountType = AccountTypeFilter.FARMER)
        assertEquals(listOf(cust1, cust3, cust4), farmers)
        assertTrue(farmers.none { it.isWellOwner })

        // Well owners only
        val owners = ReportsFilter.filterCustomers(allCustomers, group = null, accountType = AccountTypeFilter.WELL_OWNER)
        assertEquals(listOf(cust2), owners)
        assertTrue(owners.all { it.isWellOwner })

        // Combined: Group "الشرق" + FARMER -> cust1 only
        val combined = ReportsFilter.filterCustomers(allCustomers, group = "الشرق", accountType = AccountTypeFilter.FARMER)
        assertEquals(listOf(cust1), combined)
    }

    // 2. Tests for Session and Voucher Filtering by Period and Allowed Customer IDs
    @Test
    fun testSessionAndVoucherFilteringByPeriodAndCustomer() {
        val s1 = WaterSession(id = 10L, customerId = 1L, startTime = 1000L, totalAmount = 500.0)
        val s2 = WaterSession(id = 11L, customerId = 2L, startTime = 2000L, totalAmount = 800.0)
        val s3 = WaterSession(id = 12L, customerId = 3L, startTime = 3000L, totalAmount = 1200.0)
        val sessions = listOf(s1, s2, s3)

        // Period filter only
        val byPeriod = ReportsFilter.filterSessions(sessions, Pair(1500L, 2500L))
        assertEquals(listOf(s2), byPeriod)

        // Period + customer filter (allowed customerIds: {1, 3})
        val byBoth = ReportsFilter.filterSessions(sessions, Pair(500L, 3500L), allowedCustomerIds = setOf(1L, 3L))
        assertEquals(listOf(s1, s3), byBoth)

        // Vouchers filter
        val v1 = Voucher(id = 20L, customerId = 1L, date = 1000L, amount = 300.0, type = VoucherType.RECEIPT)
        val v2 = Voucher(id = 21L, customerId = 2L, date = 2000L, amount = 400.0, type = VoucherType.RECEIPT)
        val vOperating = Voucher(id = 22L, customerId = null, date = 2500L, amount = 150.0, type = VoucherType.EXPENSE)
        val vouchers = listOf(v1, v2, vOperating)

        // No customer filter -> includes operating expense
        val allV = ReportsFilter.filterVouchers(vouchers, Pair(null, null), allowedCustomerIds = null)
        assertEquals(3, allV.size)

        // With customer filter -> operating expenses without customer are excluded
        val custV = ReportsFilter.filterVouchers(vouchers, Pair(null, null), allowedCustomerIds = setOf(1L))
        assertEquals(listOf(v1), custV)
    }

    // 3. Tests for Percentage Change Calculation
    @Test
    fun testPercentageChange() {
        // Previous value is 0.0 -> returns null ("—")
        assertNull(ReportsFilter.calculatePercentageChange(current = 100.0, previous = 0.0))
        assertNull(ReportsFilter.calculatePercentageChange(current = 0.0, previous = 0.0))

        // Normal increase: from 100 to 112 -> 12%
        assertEquals(12, ReportsFilter.calculatePercentageChange(current = 112.0, previous = 100.0))

        // Normal decrease: from 100 to 80 -> -20%
        assertEquals(-20, ReportsFilter.calculatePercentageChange(current = 80.0, previous = 100.0))

        // No change: from 50 to 50 -> 0%
        assertEquals(0, ReportsFilter.calculatePercentageChange(current = 50.0, previous = 50.0))

        // Large increase: from 200 to 500 -> 150%
        assertEquals(150, ReportsFilter.calculatePercentageChange(current = 500.0, previous = 200.0))
    }

    @Test
    fun testFormatComparisonPresentation() {
        // When previous is 0.0 -> "—"
        val compZero = ReportsFilter.formatComparison(current = 500.0, previous = 0.0)
        assertEquals("—", compZero.text)
        assertNull(compZero.isBetter)

        // When current is better (diff > 0)
        val compBetter = ReportsFilter.formatComparison(current = 112.0, previous = 100.0)
        assertEquals("↑ 12% عن الفترة السابقة", compBetter.text)
        assertEquals(true, compBetter.isBetter)

        // When current is worse (diff < 0)
        val compWorse = ReportsFilter.formatComparison(current = 80.0, previous = 100.0)
        assertEquals("↓ 20% عن الفترة السابقة", compWorse.text)
        assertEquals(false, compWorse.isBetter)

        // When unchanged
        val compSame = ReportsFilter.formatComparison(current = 100.0, previous = 100.0)
        assertEquals("0% عن الفترة السابقة", compSame.text)
        assertNull(compSame.isBetter)
    }

    @Test
    fun testFormatCompactNumberArabic() {
        assertEquals("2.3 مليون", ReportsFilter.formatCompactNumber(2_300_000.0))
        assertEquals("771 ألف", ReportsFilter.formatCompactNumber(771_000.0))
        assertEquals("50 ألف", ReportsFilter.formatCompactNumber(50_000.0))
        assertEquals("12.5 ألف", ReportsFilter.formatCompactNumber(12_500.0))
        assertEquals("500", ReportsFilter.formatCompactNumber(500.0))
        assertEquals("0", ReportsFilter.formatCompactNumber(0.0))
        assertEquals("-1.5 مليون", ReportsFilter.formatCompactNumber(-1_500_000.0))
    }

    // 4. Tests for Trend Bucket Building: Days vs Months & Empty Buckets Filled with 0
    @Test
    fun testTrendBucketsDaysForThisMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        val now = cal.timeInMillis

        // Empty sessions and vouchers -> should build 30 day buckets for November, each 0.0
        val buckets = TrendBucketBuilder.buildBuckets(
            period = ReportPeriod.THIS_MONTH,
            customRange = null,
            sessions = emptyList(),
            vouchers = emptyList(),
            nowMillis = now
        )

        // November has 30 days
        assertEquals(30, buckets.size)
        assertEquals("1", buckets.first().label)
        assertEquals("30", buckets.last().label)

        // All empty buckets filled with 0.0
        assertTrue(buckets.all { it.revenue == 0.0 && it.collected == 0.0 })
    }

    @Test
    fun testTrendBucketsMonthsForThisYearAndAll() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        val now = cal.timeInMillis

        // THIS_YEAR -> 12 month buckets
        val yearBuckets = TrendBucketBuilder.buildBuckets(
            period = ReportPeriod.THIS_YEAR,
            customRange = null,
            sessions = emptyList(),
            vouchers = emptyList(),
            nowMillis = now
        )
        assertEquals(12, yearBuckets.size)
        assertEquals("يناير", yearBuckets.first().label)
        assertEquals("ديسمبر", yearBuckets.last().label)

        // ALL -> 12 month buckets (last 12 months)
        val allBuckets = TrendBucketBuilder.buildBuckets(
            period = ReportPeriod.ALL,
            customRange = null,
            sessions = emptyList(),
            vouchers = emptyList(),
            nowMillis = now
        )
        assertEquals(12, allBuckets.size)
        // Last bucket should be current month ("نوفمبر")
        assertEquals("نوفمبر", allBuckets.last().label)
    }

    @Test
    fun testTrendBucketsCustomDaysVsMonths() {
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalShort = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        // 10 days <= 31 -> DAY buckets
        val dayBuckets = TrendBucketBuilder.buildBuckets(
            period = ReportPeriod.CUSTOM,
            customRange = Pair(startCal.timeInMillis, endCalShort.timeInMillis),
            sessions = emptyList(),
            vouchers = emptyList()
        )
        assertEquals(10, dayBuckets.size)
        assertEquals("1", dayBuckets.first().label)
        assertEquals("10", dayBuckets.last().label)

        // 60 days > 31 -> MONTH buckets
        val endCalLong = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val monthBuckets = TrendBucketBuilder.buildBuckets(
            period = ReportPeriod.CUSTOM,
            customRange = Pair(startCal.timeInMillis, endCalLong.timeInMillis),
            sessions = emptyList(),
            vouchers = emptyList()
        )
        // Nov and Dec = 2 months
        assertEquals(2, monthBuckets.size)
        assertEquals("نوفمبر", monthBuckets[0].label)
        assertEquals("ديسمبر", monthBuckets[1].label)
    }
}
