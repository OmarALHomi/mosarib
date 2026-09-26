package com.example.features.customers

import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CustomerWithBalance(
    val customer: Customer,
    val totalSessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val totalBilledAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val balance: Double = 0.0 // > 0 => Customer owes money (مدين), < 0 => Customer has surplus credit (دائن)
)

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
            customers.map { customer ->
                val custSessions = sessions.filter { it.customerId == customer.id }
                val custVouchers = vouchers.filter { it.customerId == customer.id }

                val totalMinutes = custSessions.sumOf { it.durationMinutes }
                val totalBilled = custSessions.sumOf { it.totalAmount }
                
                // Total paid = amount paid during sessions + receipt vouchers - discount adjustments
                val sessionPaid = custSessions.sumOf { it.amountPaid }
                val receiptVouchersPaid = custVouchers
                    .filter { it.type == VoucherType.RECEIPT }
                    .sumOf { it.amount }
                val discountVouchers = custVouchers
                    .filter { it.type == VoucherType.DISCOUNT }
                    .sumOf { it.amount }

                val totalPaid = sessionPaid + receiptVouchersPaid + discountVouchers
                val currentBalance = totalBilled - totalPaid

                CustomerWithBalance(
                    customer = customer,
                    totalSessionsCount = custSessions.size,
                    totalMinutes = totalMinutes,
                    totalBilledAmount = totalBilled,
                    totalPaidAmount = totalPaid,
                    balance = currentBalance
                )
            }
        }

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun archiveCustomer(id: Long) = customerDao.archiveCustomer(id)
}
