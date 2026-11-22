package com.example.features.customers

import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import org.junit.Assert.assertEquals
import org.junit.Test

class LastActivitySortTest {
    private fun c(id: Long, name: String) = Customer(id = id, name = name)

    @Test
    fun ordersByLatestOperationThenNameForCustomersWithoutOperations() {
        val a = c(1, "أحمد"); val b = c(2, "بدر"); val d = c(3, "جميل"); val e = c(4, "حسن")
        val sessions = listOf(WaterSession(customerId = 1, startTime = 1_000L))
        val vouchers = listOf(Voucher(customerId = 2, date = 5_000L), Voucher(customerId = 1, date = 500L))
        val sorted = listOf(e, d, a, b)
            .map { calculateCustomerBalance(it, sessions, vouchers) }
            .sortedByLastActivity()
            .map { it.customer.name }
        assertEquals(listOf("بدر", "أحمد", "جميل", "حسن"), sorted)
    }
}
