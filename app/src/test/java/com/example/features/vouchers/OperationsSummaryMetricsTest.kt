package com.example.features.vouchers

import com.example.features.customers.Customer
import com.example.features.customers.CustomerWithBalance
import com.example.features.reports.ReportPeriod
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationsSummaryMetricsTest {

    private val c1 = Customer(id = 1L, name = "صالح", customGroup = "الشرق", isWellOwner = false)
    private val c2 = Customer(id = 2L, name = "محمد", customGroup = "الغرب", isWellOwner = false)
    private val c3 = Customer(id = 3L, name = "أحمد", customGroup = "الشرق", isWellOwner = true)
    private val c4 = Customer(id = 4L, name = "علي", customGroup = "", isWellOwner = false)

    private val cwb1 = CustomerWithBalance(customer = c1, receivableBalance = 100_000.0)
    private val cwb2 = CustomerWithBalance(customer = c2, receivableBalance = 50_000.0)
    private val cwb3 = CustomerWithBalance(customer = c3, receivableBalance = 70_000.0)
    private val cwb4 = CustomerWithBalance(customer = c4, receivableBalance = 20_000.0)
    private val allCustomersWithBalance = listOf(cwb1, cwb2, cwb3, cwb4)

    @Test
    fun calculateFilteredReceivableTotal_allCustomers() {
        val filter = OperationsFilterState()
        val total = calculateFilteredReceivableTotal(allCustomersWithBalance, filter)
        assertEquals(240_000.0, total, 0.001)
    }

    @Test
    fun calculateFilteredReceivableTotal_filterByFarmer() {
        val filter = OperationsFilterState(accountType = AccountTypeFilter.FARMER)
        val total = calculateFilteredReceivableTotal(allCustomersWithBalance, filter)
        // c1 (100k) + c2 (50k) + c4 (20k) = 170k (excludes c3 well owner)
        assertEquals(170_000.0, total, 0.001)
    }

    @Test
    fun calculateFilteredReceivableTotal_filterByWellOwner() {
        val filter = OperationsFilterState(accountType = AccountTypeFilter.WELL_OWNER)
        val total = calculateFilteredReceivableTotal(allCustomersWithBalance, filter)
        // only c3 (70k)
        assertEquals(70_000.0, total, 0.001)
    }

    @Test
    fun calculateFilteredReceivableTotal_filterByGroup() {
        val filterEast = OperationsFilterState(group = "الشرق")
        val totalEast = calculateFilteredReceivableTotal(allCustomersWithBalance, filterEast)
        // c1 (100k) + c3 (70k) = 170k
        assertEquals(170_000.0, totalEast, 0.001)

        val filterNoGroup = OperationsFilterState(group = "")
        val totalNoGroup = calculateFilteredReceivableTotal(allCustomersWithBalance, filterNoGroup)
        // c4 (20k)
        assertEquals(20_000.0, totalNoGroup, 0.001)
    }

    @Test
    fun calculateFilteredReceivableTotal_filterBySingleCustomer() {
        val filterCustomer2 = OperationsFilterState(customerId = 2L)
        val totalCustomer2 = calculateFilteredReceivableTotal(allCustomersWithBalance, filterCustomer2)
        assertEquals(50_000.0, totalCustomer2, 0.001)
    }

    @Test
    fun calculateFilteredReceivableTotal_ignoresPeriodFilter() {
        val filterWithPeriod = OperationsFilterState(
            period = ReportPeriod.THIS_MONTH,
            customRange = Pair(1000L, 2000L)
        )
        val total = calculateFilteredReceivableTotal(allCustomersWithBalance, filterWithPeriod)
        // Period does not affect current receivable balances
        assertEquals(240_000.0, total, 0.001)
    }

    @Test
    fun soldWaterMinutes_subtractsWasteCorrectly() {
        val session1 = WaterSession(id = 1L, customerId = 1L, durationMinutes = 100, wastedMinutes = 15)
        val session2 = WaterSession(id = 2L, customerId = 2L, durationMinutes = 60, wastedMinutes = 0)
        val session3 = WaterSession(id = 3L, customerId = 3L, durationMinutes = 30, wastedMinutes = 40) // waste > duration

        val sessions = listOf(session1, session2, session3)
        val soldMinutes = sessions.sumOf { s ->
            (s.durationMinutes - s.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
        }

        // session1: 85 min, session2: 60 min, session3: 0 min -> total = 145 min
        assertEquals(145, soldMinutes)
    }
}
