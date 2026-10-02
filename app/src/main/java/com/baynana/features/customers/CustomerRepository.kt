package com.baynana.features.customers

import com.baynana.features.sessions.WaterSession
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CustomerWithBalance(
    val customer: Customer,
    val totalSessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val totalBilledAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val totalDisbursedAmount: Double = 0.0,
    val balance: Double = 0.0 // > 0 => Customer owes money (مدين), < 0 => Customer has surplus credit (دائن)
)

internal fun calculateCustomerBalance(
    customer: Customer,
    sessions: List<WaterSession>,
    vouchers: List<Voucher>
): CustomerWithBalance {
    // دورات السقي الخاصة بالعميل (سواء المسجلة له مباشرة، أو المسجلة على حسابه كمستفيد)
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
    private val voucherFlow: Flow<List<Voucher>>
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    val customersWithBalance: Flow<List<CustomerWithBalance>> =
        combine(customerDao.getAllCustomers(), sessionFlow, voucherFlow) { customers, sessions, vouchers ->
            customers.map { customer -> calculateCustomerBalance(customer, sessions, vouchers) }
        }

    suspend fun insertCustomer(customer: Customer): Long {
        val finalCustomer = if (customer.linkCode.isBlank()) {
            customer.copy(linkCode = com.baynana.core.util.LinkCodeGenerator.generate())
        } else {
            customer
        }
        return customerDao.insertCustomer(finalCustomer)
    }

    suspend fun updateCustomer(customer: Customer) {
        val existing = customerDao.getCustomerByIdDirect(customer.id)
        val linkCode = customer.linkCode.ifBlank {
            existing?.linkCode?.takeIf { it.isNotBlank() }
                ?: com.baynana.core.util.LinkCodeGenerator.generate()
        }
        customerDao.updateCustomer(
            customer.copy(linkCode = linkCode, createdAt = existing?.createdAt ?: customer.createdAt)
        )
    }

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun archiveCustomer(id: Long) = customerDao.archiveCustomer(id)
}
