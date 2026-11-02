package com.example.features.vouchers

import com.example.core.util.Formatters
import com.example.core.util.PeriodRange
import com.example.features.customers.Customer
import com.example.features.reports.ReportPeriod
import com.example.features.wellowners.WellOwnerPurchaseMath

enum class SessionPaymentFilter {
    ALL, PAID, PARTIAL, UNPAID
}

enum class AccountTypeFilter {
    ALL, FARMER, WELL_OWNER
}

enum class OperationsSort {
    NEWEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC,
    NAME_ASC,
    NAME_DESC
}

data class OperationsFilterState(
    val sort: OperationsSort = OperationsSort.NEWEST,
    val type: OperationsFilter = OperationsFilter.ALL,
    val accountType: AccountTypeFilter = AccountTypeFilter.ALL,
    val sessionPayment: SessionPaymentFilter = SessionPaymentFilter.ALL,
    val group: String? = null,                           // null = all; "" = customers with no group
    val period: ReportPeriod = ReportPeriod.ALL,         // reuse; CUSTOM uses the range below
    val customRange: Pair<Long, Long>? = null,           // inclusive start-of-day .. end-of-day
    val customerId: Long? = null,
    val paymentMethods: Set<String> = emptySet(),        // vouchers' paymentMethod values
    val pumpNames: Set<String> = emptySet()              // sessions' pumpName values
) {
    val activeCount: Int
        get() {
            var count = 0
            if (sort != OperationsSort.NEWEST) count++
            if (type != OperationsFilter.ALL) count++
            if (accountType != AccountTypeFilter.ALL) count++
            if (sessionPayment != SessionPaymentFilter.ALL) count++
            if (group != null) count++
            if (period != ReportPeriod.ALL || customRange != null) count++
            if (customerId != null) count++
            if (paymentMethods.isNotEmpty()) count++
            if (pumpNames.isNotEmpty()) count++
            return count
        }
}

val UnifiedOperation.amount: Double
    get() = when (this) {
        is UnifiedOperation.SessionOp -> sessionWithCustomer.session.totalAmount
        is UnifiedOperation.VoucherOp -> voucherWithCustomer.voucher.amount
        is UnifiedOperation.PurchaseOp -> WellOwnerPurchaseMath.payableAmount(purchase)
    }

object OperationsFilterEngine {

    private fun getCustomerName(op: UnifiedOperation, customers: Map<Long, Customer>): String {
        return when (op) {
            is UnifiedOperation.SessionOp -> op.sessionWithCustomer.customer?.name ?: customers[op.sessionWithCustomer.session.customerId]?.name ?: ""
            is UnifiedOperation.VoucherOp -> op.voucherWithCustomer.customer?.name ?: op.voucherWithCustomer.voucher.customerId?.let { customers[it]?.name } ?: ""
            is UnifiedOperation.PurchaseOp -> op.owner?.name ?: customers[op.purchase.ownerCustomerId]?.name ?: ""
        }
    }

    fun apply(
        ops: List<UnifiedOperation>,
        state: OperationsFilterState,
        customers: Map<Long, Customer>,
        query: String,
        now: Long = System.currentTimeMillis()
    ): List<UnifiedOperation> {
        // 1. Filter by Type
        val byType = when (state.type) {
            OperationsFilter.ALL -> ops
            OperationsFilter.SESSIONS -> ops.filterIsInstance<UnifiedOperation.SessionOp>()
            OperationsFilter.RECEIPTS -> ops.filterIsInstance<UnifiedOperation.VoucherOp>()
                .filter { it.voucherWithCustomer.voucher.type == VoucherType.RECEIPT }
            OperationsFilter.EXPENSES -> ops.filterIsInstance<UnifiedOperation.VoucherOp>()
                .filter { it.voucherWithCustomer.voucher.type == VoucherType.EXPENSE }
            OperationsFilter.DISCOUNTS -> ops.filterIsInstance<UnifiedOperation.VoucherOp>()
                .filter { it.voucherWithCustomer.voucher.type == VoucherType.DISCOUNT }
            OperationsFilter.PURCHASES -> ops.filterIsInstance<UnifiedOperation.PurchaseOp>()
            OperationsFilter.DEFERRED -> ops.filter {
                (it is UnifiedOperation.SessionOp && it.sessionWithCustomer.session.remainingDebt > 0.01) ||
                (it is UnifiedOperation.VoucherOp && it.voucherWithCustomer.voucher.paymentMethod == "آجل")
            }
        }

        // 2. Filter by Account Type
        val byAccountType = when (state.accountType) {
            AccountTypeFilter.ALL -> byType
            AccountTypeFilter.FARMER -> byType.filter { op ->
                when (op) {
                    is UnifiedOperation.SessionOp -> {
                        val c = op.sessionWithCustomer.customer ?: customers[op.sessionWithCustomer.session.customerId]
                        c?.isWellOwner == false
                    }
                    is UnifiedOperation.VoucherOp -> {
                        val c = op.voucherWithCustomer.customer ?: op.voucherWithCustomer.voucher.customerId?.let { customers[it] }
                        c != null && !c.isWellOwner
                    }
                    is UnifiedOperation.PurchaseOp -> false
                }
            }
            AccountTypeFilter.WELL_OWNER -> byType.filter { op ->
                when (op) {
                    is UnifiedOperation.SessionOp -> {
                        val c = op.sessionWithCustomer.customer ?: customers[op.sessionWithCustomer.session.customerId]
                        c?.isWellOwner == true
                    }
                    is UnifiedOperation.VoucherOp -> {
                        val c = op.voucherWithCustomer.customer ?: op.voucherWithCustomer.voucher.customerId?.let { customers[it] }
                        c?.isWellOwner == true
                    }
                    is UnifiedOperation.PurchaseOp -> true
                }
            }
        }

        // 3. Filter by Period
        val (start, end) = PeriodRange.rangeFor(state.period, now, state.customRange)
        val byPeriod = if (start == null && end == null) {
            byAccountType
        } else {
            byAccountType.filter { op ->
                val t = op.timestamp
                (start == null || t >= start) && (end == null || t <= end)
            }
        }

        // 4. Filter by Customer Group
        val byGroup = if (state.group == null) {
            byPeriod
        } else {
            byPeriod.filter { op ->
                val custId = when (op) {
                    is UnifiedOperation.SessionOp -> op.sessionWithCustomer.customer?.id ?: op.sessionWithCustomer.session.customerId
                    is UnifiedOperation.VoucherOp -> op.voucherWithCustomer.customer?.id ?: op.voucherWithCustomer.voucher.customerId
                    is UnifiedOperation.PurchaseOp -> op.owner?.id ?: op.purchase.ownerCustomerId
                }
                val cust = custId?.let { customers[it] }
                if (state.group.isEmpty()) {
                    cust != null && cust.customGroup.isBlank()
                } else {
                    cust != null && cust.customGroup == state.group
                }
            }
        }

        // 5. Filter by Single Customer
        val byCustomer = if (state.customerId == null) {
            byGroup
        } else {
            byGroup.filter { op ->
                when (op) {
                    is UnifiedOperation.SessionOp -> (op.sessionWithCustomer.customer?.id ?: op.sessionWithCustomer.session.customerId) == state.customerId
                    is UnifiedOperation.VoucherOp -> (op.voucherWithCustomer.customer?.id ?: op.voucherWithCustomer.voucher.customerId) == state.customerId
                    is UnifiedOperation.PurchaseOp -> (op.owner?.id ?: op.purchase.ownerCustomerId) == state.customerId
                }
            }
        }

        // 6. Filter by Session Payment State
        val byPayment = if (state.sessionPayment == SessionPaymentFilter.ALL) {
            byCustomer
        } else {
            // Applies to sessions only; non-sessions are hidden when not ALL
            byCustomer.filterIsInstance<UnifiedOperation.SessionOp>().filter { op ->
                val s = op.sessionWithCustomer.session
                val debt = s.remainingDebt
                val paid = s.amountPaid
                when (state.sessionPayment) {
                    SessionPaymentFilter.PAID -> debt <= 0.01
                    SessionPaymentFilter.UNPAID -> paid <= 0.01 && debt > 0.01
                    SessionPaymentFilter.PARTIAL -> paid > 0.01 && debt > 0.01
                    SessionPaymentFilter.ALL -> true
                }
            }
        }

        // 7. Filter by Payment Methods and / or Pump Names
        val hasMethods = state.paymentMethods.isNotEmpty()
        val hasPumps = state.pumpNames.isNotEmpty()

        val byMethodsAndPumps = if (!hasMethods && !hasPumps) {
            byPayment
        } else {
            byPayment.filter { op ->
                when (op) {
                    is UnifiedOperation.VoucherOp -> hasMethods && op.voucherWithCustomer.voucher.paymentMethod in state.paymentMethods
                    is UnifiedOperation.SessionOp -> hasPumps && op.sessionWithCustomer.session.pumpName in state.pumpNames
                    is UnifiedOperation.PurchaseOp -> false
                }
            }
        }

        // 8. Search query filter
        val bySearch = if (query.isBlank()) {
            byMethodsAndPumps
        } else {
            val q = query.trim()
            byMethodsAndPumps.filter { op ->
                when (op) {
                    is UnifiedOperation.SessionOp -> {
                        val s = op.sessionWithCustomer.session
                        val c = op.sessionWithCustomer.customer ?: customers[s.customerId]
                        (c?.name?.contains(q, ignoreCase = true) == true) ||
                        (c?.farmName?.contains(q, ignoreCase = true) == true) ||
                        s.notes.contains(q, ignoreCase = true) ||
                        s.pumpName.contains(q, ignoreCase = true)
                    }
                    is UnifiedOperation.VoucherOp -> {
                        val v = op.voucherWithCustomer.voucher
                        val c = op.voucherWithCustomer.customer ?: v.customerId?.let { customers[it] }
                        v.voucherNumber.contains(q, ignoreCase = true) ||
                        v.category.contains(q, ignoreCase = true) ||
                        v.notes.contains(q, ignoreCase = true) ||
                        (c?.name?.contains(q, ignoreCase = true) == true)
                    }
                    is UnifiedOperation.PurchaseOp -> {
                        val p = op.purchase
                        val o = op.owner ?: customers[p.ownerCustomerId]
                        (o?.name?.contains(q, ignoreCase = true) == true) ||
                        p.notes.contains(q, ignoreCase = true)
                    }
                }
            }
        }

        // 9. Sorting
        return when (state.sort) {
            OperationsSort.NEWEST -> bySearch.sortedWith(
                compareByDescending<UnifiedOperation> { it.timestamp }.thenByDescending { it.id }
            )
            OperationsSort.OLDEST -> bySearch.sortedWith(
                compareBy<UnifiedOperation> { it.timestamp }.thenBy { it.id }
            )
            OperationsSort.AMOUNT_DESC -> bySearch.sortedWith(
                compareByDescending<UnifiedOperation> { it.amount }.thenByDescending { it.timestamp }
            )
            OperationsSort.AMOUNT_ASC -> bySearch.sortedWith(
                compareBy<UnifiedOperation> { it.amount }.thenBy { it.timestamp }
            )
            OperationsSort.NAME_ASC -> bySearch.sortedWith(
                compareBy<UnifiedOperation> { getCustomerName(it, customers) }.thenByDescending { it.timestamp }
            )
            OperationsSort.NAME_DESC -> bySearch.sortedWith(
                compareByDescending<UnifiedOperation> { getCustomerName(it, customers) }.thenByDescending { it.timestamp }
            )
        }
    }

    fun describe(state: OperationsFilterState, customerName: String? = null): String {
        val parts = mutableListOf<String>()

        // Period
        when (state.period) {
            ReportPeriod.ALL -> Unit
            ReportPeriod.TODAY -> parts.add("اليوم")
            ReportPeriod.YESTERDAY -> parts.add("أمس")
            ReportPeriod.THIS_WEEK -> parts.add("هذا الأسبوع")
            ReportPeriod.THIS_MONTH -> parts.add("هذا الشهر")
            ReportPeriod.LAST_MONTH -> parts.add("الشهر الماضي")
            ReportPeriod.CUSTOM -> {
                if (state.customRange != null) {
                    parts.add("من ${Formatters.formatDate(state.customRange.first)} إلى ${Formatters.formatDate(state.customRange.second)}")
                } else {
                    parts.add("فترة مخصصة")
                }
            }
        }

        // Group
        if (state.group != null) {
            if (state.group.isEmpty()) {
                parts.add("بدون مجموعة")
            } else {
                parts.add(state.group)
            }
        }

        // Customer
        if (!customerName.isNullOrBlank()) {
            parts.add(customerName)
        }

        // Account Type
        when (state.accountType) {
            AccountTypeFilter.ALL -> Unit
            AccountTypeFilter.FARMER -> parts.add("مزارع")
            AccountTypeFilter.WELL_OWNER -> parts.add("صاحب بئر")
        }

        // Session Payment
        when (state.sessionPayment) {
            SessionPaymentFilter.ALL -> Unit
            SessionPaymentFilter.PAID -> parts.add("مسدد")
            SessionPaymentFilter.PARTIAL -> parts.add("سداد جزئي")
            SessionPaymentFilter.UNPAID -> parts.add("غير مسدد")
        }

        // Payment methods
        if (state.paymentMethods.isNotEmpty()) {
            parts.add(state.paymentMethods.joinToString("، "))
        }

        // Pump names
        if (state.pumpNames.isNotEmpty()) {
            parts.add(state.pumpNames.joinToString("، "))
        }

        // Sort
        when (state.sort) {
            OperationsSort.NEWEST -> Unit
            OperationsSort.OLDEST -> parts.add("الأقدم")
            OperationsSort.AMOUNT_DESC -> parts.add("الأعلى مبلغاً")
            OperationsSort.AMOUNT_ASC -> parts.add("الأقل مبلغاً")
            OperationsSort.NAME_ASC -> parts.add("الاسم (أ-ي)")
            OperationsSort.NAME_DESC -> parts.add("الاسم (ي-أ)")
        }

        return if (parts.isEmpty()) "كل الوقت" else parts.joinToString(" · ")
    }
}
