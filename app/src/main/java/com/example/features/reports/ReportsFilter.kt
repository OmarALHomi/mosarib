package com.example.features.reports

import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.AccountTypeFilter
import com.example.features.vouchers.Voucher
import com.example.features.wellowners.WellOwnerPurchase
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Pure filtering and comparison logic for reports.
 */
object ReportsFilter {

    /**
     * Filters customers by group and account type.
     * [group] == null: all groups
     * [group] == "": customers with no group (customGroup is blank)
     * [group] != "": customers whose customGroup matches (case-insensitive, trimmed)
     */
    fun filterCustomers(
        customers: List<Customer>,
        group: String?,
        accountType: AccountTypeFilter
    ): List<Customer> {
        return customers.filter { customer ->
            val matchesAccount = when (accountType) {
                AccountTypeFilter.ALL -> true
                AccountTypeFilter.FARMER -> !customer.isWellOwner
                AccountTypeFilter.WELL_OWNER -> customer.isWellOwner
            }
            val matchesGroup = when {
                group == null -> true
                group.isEmpty() -> customer.customGroup.isBlank()
                else -> customer.customGroup.trim().equals(group.trim(), ignoreCase = true)
            }
            matchesAccount && matchesGroup
        }
    }

    /**
     * Filters sessions by date range and optionally by customer ID set.
     */
    fun filterSessions(
        sessions: List<WaterSession>,
        periodRange: Pair<Long?, Long?>,
        allowedCustomerIds: Set<Long>? = null
    ): List<WaterSession> {
        val (start, end) = periodRange
        return sessions.filter { session ->
            val inPeriod = (start == null || session.startTime >= start) && (end == null || session.startTime <= end)
            val inCustomer = if (allowedCustomerIds == null) {
                true
            } else {
                val custId = session.billedToCustomerId ?: session.customerId
                allowedCustomerIds.contains(custId)
            }
            inPeriod && inCustomer
        }
    }

    /**
     * Filters vouchers by date range and optionally by customer ID set.
     */
    fun filterVouchers(
        vouchers: List<Voucher>,
        periodRange: Pair<Long?, Long?>,
        allowedCustomerIds: Set<Long>? = null
    ): List<Voucher> {
        val (start, end) = periodRange
        return vouchers.filter { voucher ->
            val inPeriod = (start == null || voucher.date >= start) && (end == null || voucher.date <= end)
            val inCustomer = if (allowedCustomerIds == null) {
                true
            } else {
                // If customer filter is active, only include vouchers belonging to allowed customers
                voucher.customerId != null && allowedCustomerIds.contains(voucher.customerId)
            }
            inPeriod && inCustomer
        }
    }

    /**
     * Filters purchases by date range and optionally by customer ID set.
     */
    fun filterPurchases(
        purchases: List<WellOwnerPurchase>,
        periodRange: Pair<Long?, Long?>,
        allowedCustomerIds: Set<Long>? = null
    ): List<WellOwnerPurchase> {
        val (start, end) = periodRange
        return purchases.filter { purchase ->
            val inPeriod = (start == null || purchase.date >= start) && (end == null || purchase.date <= end)
            val inCustomer = allowedCustomerIds == null || allowedCustomerIds.contains(purchase.ownerCustomerId)
            inPeriod && inCustomer
        }
    }

    /**
     * Percentage change calculation:
     * Returns null if previousValue <= 0.0 (previous is 0).
     * Returns rounded integer percentage ((current - previous) / previous * 100).
     */
    fun calculatePercentageChange(current: Double, previous: Double): Int? {
        if (previous <= 0.0) return null
        val change = ((current - previous) / previous) * 100.0
        return round(change).toInt()
    }

    data class ComparisonResult(
        val text: String,
        val isBetter: Boolean?, // true: success, false: debt, null: neutral / dash
        val percent: Int?
    )

    /**
     * Comparison presentation against previous period:
     * "↑ 12% عن الفترة السابقة" (success when better, debt when worse; "—" when previous value is 0).
     */
    fun formatComparison(current: Double, previous: Double): ComparisonResult {
        val pct = calculatePercentageChange(current, previous)
        return when {
            pct == null -> ComparisonResult(text = "—", isBetter = null, percent = null)
            pct > 0 -> ComparisonResult(text = "↑ $pct% عن الفترة السابقة", isBetter = true, percent = pct)
            pct < 0 -> ComparisonResult(text = "↓ ${abs(pct)}% عن الفترة السابقة", isBetter = false, percent = pct)
            else -> ComparisonResult(text = "0% عن الفترة السابقة", isBetter = null, percent = 0)
        }
    }

    /**
     * Formats compact numbers in Arabic (e.g., "2.3 مليون", "771 ألف", "500").
     * Keeps Western digits 0-9.
     */
    fun formatCompactNumber(amount: Double): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> {
                val formatted = String.format(Locale.US, "%.1f", absVal / 1_000_000.0).removeSuffix(".0")
                "$sign$formatted مليون"
            }
            absVal >= 1_000 -> {
                val thousands = absVal / 1_000.0
                val formatted = if (thousands >= 100) {
                    String.format(Locale.US, "%.0f", thousands)
                } else {
                    String.format(Locale.US, "%.1f", thousands).removeSuffix(".0")
                }
                "$sign$formatted ألف"
            }
            else -> {
                String.format(Locale.US, "%s%.0f", sign, absVal)
            }
        }
    }
}
