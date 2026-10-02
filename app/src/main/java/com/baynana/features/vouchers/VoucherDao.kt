package com.baynana.features.vouchers

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT COUNT(*) FROM vouchers")
    fun getVouchersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vouchers")
    suspend fun getVouchersCountDirect(): Int

    @Query("SELECT * FROM vouchers ORDER BY date DESC, id DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE customerId = :customerId ORDER BY date DESC, id DESC")
    fun getVouchersForCustomer(customerId: Long): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE type = :type ORDER BY date DESC, id DESC")
    fun getVouchersByType(type: VoucherType): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE sessionId = :sessionId ORDER BY date ASC")
    fun getVouchersForSession(sessionId: Long): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE date >= :fromTime AND date <= :toTime ORDER BY date DESC, id DESC")
    fun getVouchersBetween(fromTime: Long, toTime: Long): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getVoucherById(id: Long): Voucher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    @Update
    suspend fun updateVoucher(voucher: Voucher)

    @Delete
    suspend fun deleteVoucher(voucher: Voucher)

    @Query("DELETE FROM vouchers WHERE id = :id")
    suspend fun deleteVoucherById(id: Long)
}
