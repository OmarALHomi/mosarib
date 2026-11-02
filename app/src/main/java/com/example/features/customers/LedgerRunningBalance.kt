package com.example.features.customers

import com.example.core.util.Formatters
import com.example.core.util.MoneyPrecision
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath

data class LedgerRunningBalanceResult(
    val balancesByItem: Map<CustomerLedgerItem, Double>,
    val finalBalance: Double
)

object LedgerRunningBalance {

    /**
     * Computes the running balance after each operation, walking oldest -> newest
     * with the same accounting signs used by [CustomerRepository].
     *
     * - For a normal customer:
     *   balance = totalSales + disbursements - collectedCash - discounts
     *   Where:
     *     Session adds (totalAmount - unvoucheredPaid)
     *     Disbursement (سند صرف) adds voucher.amount
     *     Receipt (سند قبض) subtracts voucher.amount
     *     Discount (خصم) subtracts voucher.amount
     *
     * - For a well owner:
     *   netOwnerPayable = (netPurchases + collectedCash) - (disbursements + ownerSessions - discounts)
     *   Where:
     *     Purchase adds payableAmount
     *     Receipt adds voucher.amount
     *     Disbursement subtracts voucher.amount
     *     Session subtracts (totalAmount - unvoucheredPaid)
     *     Discount adds voucher.amount
     */
    fun compute(
        items: List<CustomerLedgerItem>,
        isWellOwner: Boolean
    ): LedgerRunningBalanceResult {
        if (items.isEmpty()) {
            return LedgerRunningBalanceResult(emptyMap(), 0.0)
        }

        // Sort chronologically: oldest first.
        // For identical timestamps, billings (purchases/sessions) precede settlements/payments.
        val sorted = items.sortedWith(
            compareBy<CustomerLedgerItem> { it.timestamp }
                .thenBy { item ->
                    when (item) {
                        is CustomerLedgerItem.PurchaseItem -> 0
                        is CustomerLedgerItem.SessionItem -> 1
                        is CustomerLedgerItem.DisbursementItem -> 2
                        is CustomerLedgerItem.ReceiptItem -> 3
                        is CustomerLedgerItem.DiscountItem -> 4
                    }
                }
        )

        val balances = mutableMapOf<CustomerLedgerItem, Double>()
        var running = 0.0

        for (item in sorted) {
            if (isWellOwner) {
                when (item) {
                    is CustomerLedgerItem.PurchaseItem -> {
                        running += WellOwnerPurchaseMath.payableAmount(item.purchase)
                    }
                    is CustomerLedgerItem.ReceiptItem -> {
                        running += item.voucher.amount
                    }
                    is CustomerLedgerItem.DisbursementItem -> {
                        running -= item.voucher.amount
                    }
                    is CustomerLedgerItem.SessionItem -> {
                        val unvoucheredPaid = (item.session.amountPaid - item.linkedVouchers.sumOf { it.amount }).coerceAtLeast(0.0)
                        running -= (item.session.totalAmount - unvoucheredPaid)
                    }
                    is CustomerLedgerItem.DiscountItem -> {
                        running += item.voucher.amount
                    }
                }
            } else {
                when (item) {
                    is CustomerLedgerItem.SessionItem -> {
                        val unvoucheredPaid = (item.session.amountPaid - item.linkedVouchers.sumOf { it.amount }).coerceAtLeast(0.0)
                        running += (item.session.totalAmount - unvoucheredPaid)
                    }
                    is CustomerLedgerItem.DisbursementItem -> {
                        running += item.voucher.amount
                    }
                    is CustomerLedgerItem.ReceiptItem -> {
                        running -= item.voucher.amount
                    }
                    is CustomerLedgerItem.DiscountItem -> {
                        running -= item.voucher.amount
                    }
                    is CustomerLedgerItem.PurchaseItem -> {
                        // Well purchases do not apply to normal customers
                    }
                }
            }
            balances[item] = MoneyPrecision.roundToCurrency(running)
        }

        val roundedFinal = MoneyPrecision.roundToCurrency(running)
        return LedgerRunningBalanceResult(
            balancesByItem = balances,
            finalBalance = roundedFinal
        )
    }

    /**
     * Overload accepting customer, sessions, vouchers, and purchases directly.
     * Builds the unified items list and computes running balance.
     */
    fun compute(
        customer: Customer,
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        purchases: List<WellOwnerPurchase> = emptyList()
    ): LedgerRunningBalanceResult {
        val items = buildItems(customer, sessions, vouchers, purchases)
        return compute(items, isWellOwner = customer.isWellOwner)
    }

    /**
     * Builds the list of [CustomerLedgerItem] corresponding to this customer.
     */
    fun buildItems(
        customer: Customer,
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        purchases: List<WellOwnerPurchase> = emptyList(),
        allCustomers: List<Customer> = emptyList()
    ): List<CustomerLedgerItem> {
        val customerSessions = sessions.filter {
            it.billedToCustomerId == customer.id ||
                (it.customerId == customer.id && it.billedToCustomerId == null)
        }
        val customerVouchers = vouchers.filter { it.customerId == customer.id }
        val ownerPurchases = if (customer.isWellOwner) {
            purchases.filter { it.ownerCustomerId == customer.id }
        } else emptyList()

        val sessionItems = customerSessions.map { s ->
            val origCust = if (s.customerId != customer.id) allCustomers.find { it.id == s.customerId } else null
            val sponsorOwner = if (s.billedToCustomerId != null && s.billedToCustomerId != customer.id) allCustomers.find { it.id == s.billedToCustomerId } else null
            val linked = customerVouchers.filter { it.sessionId == s.id }
            CustomerLedgerItem.SessionItem(s, origCust, sponsorOwner, linked)
        }
        val purchaseItems = ownerPurchases.map { CustomerLedgerItem.PurchaseItem(it) }
        val receiptItems = customerVouchers.filter { it.type == VoucherType.RECEIPT }.map { CustomerLedgerItem.ReceiptItem(it) }
        val disbursementItems = customerVouchers.filter { it.type == VoucherType.EXPENSE }.map { CustomerLedgerItem.DisbursementItem(it) }
        val discountItems = customerVouchers.filter { it.type == VoucherType.DISCOUNT }.map { CustomerLedgerItem.DiscountItem(it) }

        return (sessionItems + purchaseItems + receiptItems + disbursementItems + discountItems)
            .sortedByDescending { it.timestamp }
    }

    /**
     * Formats the running balance note to be displayed in [extraNote]:
     * "الرصيد بعد العملية: X"
     */
    fun formatRunningBalance(
        balance: Double,
        isWellOwner: Boolean,
        currencySymbol: String
    ): String {
        val absBal = kotlin.math.abs(balance)
        val valueText = if (absBal <= 0.001) {
            "مسدد"
        } else {
            val formatted = Formatters.formatCurrency(absBal, currencySymbol)
            val suffix = if (isWellOwner) {
                if (balance > 0.001) " (له)" else " (عليه)"
            } else {
                if (balance > 0.001) " (عليه)" else " (له)"
            }
            "$formatted$suffix"
        }
        return "الرصيد بعد العملية: $valueText"
    }
}
