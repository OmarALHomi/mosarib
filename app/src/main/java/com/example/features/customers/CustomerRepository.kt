package com.example.features.customers

import com.example.features.pumps.PumpSource
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * One contact may act as both a customer and a well-owner supplier. The two directions are kept
 * separate: [receivableBalance] is money owed to the distributor; [payableBalance] is money owed
 * by the distributor to this owner. [balance] remains as a compatibility/sort value for older UI.
 */
data class CustomerWithBalance(
    val customer: Customer,
    val totalSessionsCount: Int = 0,
    val totalPurchasesCount: Int = 0,
    val totalVouchersCount: Int = 0,
    val totalOperationsCount: Int = 0,
    /** Gross recorded runtime for the sessions billed to this account. */
    val totalMinutes: Int = 0,
    val totalBilledAmount: Double = 0.0,
    /** Cash actually received for sessions and standalone receipts; discounts are not cash. */
    val totalPaidAmount: Double = 0.0,
    val totalDisbursedAmount: Double = 0.0,
    /** Positive means owed to us for customer activity, or owed by us for owner purchases. */
    val balance: Double = 0.0,
    val receivableBalance: Double = 0.0,
    val payableBalance: Double = 0.0,
    val totalSoldMinutes: Int = 0,
    val totalDistributorWasteMinutes: Int = 0,
    val totalPurchasedMinutes: Int = 0,
    val totalChargeablePurchasedMinutes: Int = 0,
    val totalPurchaseGrossAmount: Double = 0.0,
    val totalPurchaseAmount: Double = 0.0,
    val totalOwnerWasteCredit: Double = 0.0
)

/** Shared, pure balance calculation so account lists, statements, and reports use the same rules. */
internal fun calculateCustomerBalance(
    customer: Customer,
    sessions: List<WaterSession>,
    vouchers: List<Voucher>,
    pumps: List<PumpSource> = emptyList(), // Kept for source compatibility with existing callers.
    purchases: List<WellOwnerPurchase> = emptyList()
): CustomerWithBalance {
    val customerSessions = sessions.filter {
        it.billedToCustomerId == customer.id ||
            (it.customerId == customer.id && it.billedToCustomerId == null)
    }
    val customerVouchers = vouchers.filter { it.customerId == customer.id }

    // amountPaid on a session already includes receipts linked to that session. Only independent
    // receipts and discounts are added separately, preventing a second count of linked settlements.
    val sessionCash = customerSessions.sumOf { it.amountPaid.coerceAtLeast(0.0) }
    val standaloneReceipts = customerVouchers
        .filter { it.type == VoucherType.RECEIPT && it.sessionId == null }
        .sumOf { it.amount.coerceAtLeast(0.0) }
    val standaloneDiscounts = customerVouchers
        .filter { it.type == VoucherType.DISCOUNT && it.sessionId == null }
        .sumOf { it.amount.coerceAtLeast(0.0) }
    val collectedCash = sessionCash + standaloneReceipts
    val totalSales = customerSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
    val totalDisbursed = customerVouchers
        .filter { it.type == VoucherType.EXPENSE }
        .sumOf { it.amount.coerceAtLeast(0.0) }

    // Advances/disbursements to a non-owner account add to its receivable. For owners, EXPENSE
    // vouchers represent money paid out to the supplier and reduce only the supplier payable.
    val customerSideDisbursements = if (customer.isWellOwner) 0.0 else totalDisbursed
    val receivable = totalSales + customerSideDisbursements - collectedCash - standaloneDiscounts

    val ownerPurchases = purchases.filter { it.ownerCustomerId == customer.id }
    val grossPurchaseAmount = ownerPurchases.sumOf(WellOwnerPurchaseMath::grossAmount)
    val ownerWasteCredit = ownerPurchases.sumOf(WellOwnerPurchaseMath::ownerWasteCredit)
    val netPurchaseAmount = ownerPurchases.sumOf(WellOwnerPurchaseMath::payableAmount)
    val chargeablePurchasedMinutes = ownerPurchases.sumOf(WellOwnerPurchaseMath::chargeableMinutes)
    val purchasedMinutes = ownerPurchases.sumOf { it.durationMinutes.coerceAtLeast(0) }
    val payable = if (customer.isWellOwner) (netPurchaseAmount - totalDisbursed) else 0.0

    return CustomerWithBalance(
        customer = customer,
        totalSessionsCount = customerSessions.size,
        totalPurchasesCount = ownerPurchases.size,
        totalVouchersCount = customerVouchers.size,
        totalOperationsCount = customerSessions.size + ownerPurchases.size + customerVouchers.size,
        totalMinutes = customerSessions.sumOf { it.durationMinutes.coerceAtLeast(0) },
        totalBilledAmount = totalSales,
        totalPaidAmount = collectedCash,
        totalDisbursedAmount = totalDisbursed,
        balance = if (customer.isWellOwner) payable else receivable,
        receivableBalance = receivable,
        payableBalance = payable,
        totalSoldMinutes = customerSessions.sumOf {
            (it.durationMinutes - it.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
        },
        totalDistributorWasteMinutes = customerSessions.sumOf {
            it.wastedMinutes.coerceAtMost(it.durationMinutes.coerceAtLeast(0)).coerceAtLeast(0)
        },
        totalPurchasedMinutes = purchasedMinutes,
        totalChargeablePurchasedMinutes = chargeablePurchasedMinutes,
        totalPurchaseGrossAmount = grossPurchaseAmount,
        totalPurchaseAmount = netPurchaseAmount,
        totalOwnerWasteCredit = ownerWasteCredit
    )
}

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val sessionFlow: Flow<List<WaterSession>>,
    private val voucherFlow: Flow<List<Voucher>>,
    private val pumpFlow: Flow<List<PumpSource>> = flowOf(emptyList()),
    private val ownerPurchaseFlow: Flow<List<WellOwnerPurchase>> = flowOf(emptyList())
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    val customersWithBalance: Flow<List<CustomerWithBalance>> =
        combine(customerDao.getAllCustomers(), sessionFlow, voucherFlow, pumpFlow, ownerPurchaseFlow) {
                customers, sessions, vouchers, _, purchases ->
            customers.map { customer ->
                calculateCustomerBalance(customer, sessions, vouchers, purchases = purchases)
            }
        }

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun archiveCustomer(id: Long) = customerDao.archiveCustomer(id)
}
