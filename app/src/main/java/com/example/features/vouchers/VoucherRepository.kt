package com.example.features.vouchers

import com.example.features.customers.Customer
import com.example.features.customers.CustomerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class VoucherWithCustomer(
    val voucher: Voucher,
    val customer: Customer?
)

class VoucherRepository(
    private val voucherDao: VoucherDao,
    private val customerDao: CustomerDao
) {
    val allVouchers: Flow<List<Voucher>> = voucherDao.getAllVouchers()

    val vouchersWithCustomer: Flow<List<VoucherWithCustomer>> =
        combine(voucherDao.getAllVouchers(), customerDao.getAllCustomers()) { vouchers, customers ->
            val customerMap = customers.associateBy { it.id }
            vouchers.map { voucher ->
                VoucherWithCustomer(
                    voucher = voucher,
                    customer = voucher.customerId?.let { customerMap[it] }
                )
            }
        }

    fun getVouchersForCustomer(customerId: Long): Flow<List<Voucher>> =
        voucherDao.getVouchersForCustomer(customerId)

    fun getVouchersForSession(sessionId: Long): Flow<List<Voucher>> =
        voucherDao.getVouchersForSession(sessionId)

    fun getVouchersByType(type: VoucherType): Flow<List<Voucher>> =
        voucherDao.getVouchersByType(type)

    fun getVouchersBetween(fromTime: Long, toTime: Long): Flow<List<Voucher>> =
        voucherDao.getVouchersBetween(fromTime, toTime)

    suspend fun getVoucherById(id: Long): Voucher? = voucherDao.getVoucherById(id)

    suspend fun insertVoucher(voucher: Voucher): Long = voucherDao.insertVoucher(voucher)

    suspend fun updateVoucher(voucher: Voucher) = voucherDao.updateVoucher(voucher)

    suspend fun deleteVoucher(voucher: Voucher) = voucherDao.deleteVoucher(voucher)

    suspend fun deleteVoucherById(id: Long) = voucherDao.deleteVoucherById(id)
}
