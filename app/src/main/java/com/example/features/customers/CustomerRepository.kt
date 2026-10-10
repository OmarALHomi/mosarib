package com.example.features.customers

import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import com.example.features.pumps.PumpSource
import kotlinx.coroutines.flow.flowOf

data class CustomerWithBalance(
    val customer: Customer,
    val totalSessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val totalBilledAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val totalDisbursedAmount: Double = 0.0,
    val balance: Double = 0.0 // > 0 => Customer owes money (مدين) للمزارع، أو المسرب مدين لصاحب البئر (عليك)
)

internal fun calculateCustomerBalance(
    customer: Customer,
    sessions: List<WaterSession>,
    vouchers: List<Voucher>,
    pumps: List<PumpSource> = emptyList()
): CustomerWithBalance {
    if (customer.isWellOwner) {
        // حساب صاحب البئر المستقل (دائن بساعات الماء المستهلكة من بئره، ومدين بالدفعات المصروفة له)
        val ownedPumpIds = pumps.filter { it.ownerCustomerId == customer.id }.map { it.id }.toSet()
        val wellSessions = sessions.filter {
            (it.pumpSourceId != null && ownedPumpIds.contains(it.pumpSourceId)) ||
            (pumps.any { p -> p.ownerCustomerId == customer.id && p.name == it.pumpName })
        }
        val wellMinutes = wellSessions.sumOf { Math.max(0, it.durationMinutes - it.wastedMinutes) }
        val totalOwedToOwner = wellSessions.sumOf { s ->
            val netMin = Math.max(0, s.durationMinutes - s.wastedMinutes)
            val costRate = if (s.costPricePerHour > 0) s.costPricePerHour else {
                pumps.find { it.id == s.pumpSourceId }?.costPricePerHour ?: 0.0
            }
            (netMin / 60.0) * costRate
        }
        val totalDisbursed = vouchers
            .filter { it.customerId == customer.id && it.type == VoucherType.EXPENSE }
            .sumOf { it.amount }

        // موجب = عليك لصاحب البئر
        val netBalance = totalOwedToOwner - totalDisbursed

        return CustomerWithBalance(
            customer = customer,
            totalSessionsCount = wellSessions.size,
            totalMinutes = wellMinutes,
            totalBilledAmount = totalOwedToOwner,
            totalPaidAmount = totalDisbursed,
            totalDisbursedAmount = totalDisbursed,
            balance = netBalance
        )
    }

    // دورات السقي الخاصة بالمزارع (سواء المسجلة له مباشرة، أو المسجلة على حسابه كمستفيد)
    val customerSessions = sessions.filter {
        (it.billedToCustomerId == customer.id) || (it.customerId == customer.id && it.billedToCustomerId == null)
    }
    val customerVouchers = vouchers.filter { it.customerId == customer.id }

    // السندات المستقلة (غير المرتبطة بجلسة سقي محددة لمنع احتساب السداد مرتين)
    val standaloneReceiptsAndDiscounts = customerVouchers
        .filter { (it.type == VoucherType.RECEIPT || it.type == VoucherType.DISCOUNT) && it.sessionId == null }
        .sumOf { it.amount }

    val totalPaid = customerSessions.sumOf { it.amountPaid } + standaloneReceiptsAndDiscounts
    val totalBilled = customerSessions.sumOf { it.totalAmount }

    // سندات الصرف المسلّمة للعميل / المستفيد (تزيد من مطلوباته أو تقلل رصيده الدائن)
    val totalDisbursed = customerVouchers
        .filter { it.type == VoucherType.EXPENSE }
        .sumOf { it.amount }

    // صافي الرصيد = (ما عليه من سقي + ما صُرف له من نقد) - ما دفعه
    val netBalance = (totalBilled + totalDisbursed) - totalPaid

    return CustomerWithBalance(
        customer = customer,
        totalSessionsCount = customerSessions.size,
        totalMinutes = customerSessions.sumOf { it.durationMinutes },
        totalBilledAmount = totalBilled,
        totalPaidAmount = totalPaid,
        totalDisbursedAmount = totalDisbursed,
        balance = netBalance
    )
}

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val sessionFlow: Flow<List<WaterSession>>,
    private val voucherFlow: Flow<List<Voucher>>,
    private val pumpFlow: Flow<List<PumpSource>> = flowOf(emptyList())
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    val customersWithBalance: Flow<List<CustomerWithBalance>> =
        combine(customerDao.getAllCustomers(), sessionFlow, voucherFlow, pumpFlow) { customers, sessions, vouchers, pumps ->
            customers.map { customer -> calculateCustomerBalance(customer, sessions, vouchers, pumps) }
        }

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun archiveCustomer(id: Long) = customerDao.archiveCustomer(id)
}
