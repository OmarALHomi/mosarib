package com.example.features.customers

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CustomerDeleteCounts(
    val sessions: Int = 0,
    val vouchers: Int = 0,
    val purchases: Int = 0
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersIncludingArchived(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR farmName LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllCustomersDirect(): List<Customer>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersIncludingArchivedDirect(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCustomer(id: Long)

    @Query("SELECT * FROM customers WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedCustomers(): Flow<List<Customer>>

    @Query("UPDATE customers SET isArchived = 0 WHERE id = :id")
    suspend fun restoreCustomer(id: Long)

    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM water_sessions WHERE customerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)) AS sessions,
            (SELECT COUNT(*) FROM vouchers WHERE customerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)) AS vouchers,
            (SELECT COUNT(*) FROM well_owner_purchases WHERE ownerCustomerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)) AS purchases
    """)
    suspend fun getCustomerDeleteCounts(customerId: Long): CustomerDeleteCounts

    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM water_sessions WHERE customerId IN (SELECT id FROM customers WHERE isArchived = 1)) AS sessions,
            (SELECT COUNT(*) FROM vouchers WHERE customerId IN (SELECT id FROM customers WHERE isArchived = 1)) AS vouchers,
            (SELECT COUNT(*) FROM well_owner_purchases WHERE ownerCustomerId IN (SELECT id FROM customers WHERE isArchived = 1)) AS purchases
    """)
    suspend fun getRecycleBinTotalCounts(): CustomerDeleteCounts

    @Query("DELETE FROM well_owner_purchases WHERE ownerCustomerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)")
    suspend fun deletePurchasesForArchivedCustomer(customerId: Long): Int

    @Query("DELETE FROM vouchers WHERE customerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)")
    suspend fun deleteVouchersForArchivedCustomer(customerId: Long): Int

    @Query("DELETE FROM water_sessions WHERE customerId IN (SELECT id FROM customers WHERE id = :customerId AND isArchived = 1)")
    suspend fun deleteSessionsForArchivedCustomer(customerId: Long): Int

    @Query("DELETE FROM customers WHERE id = :customerId AND isArchived = 1")
    suspend fun deleteArchivedCustomerOnly(customerId: Long): Int

    @Transaction
    suspend fun permanentlyDeleteCustomer(id: Long) {
        deletePurchasesForArchivedCustomer(id)
        deleteVouchersForArchivedCustomer(id)
        deleteSessionsForArchivedCustomer(id)
        deleteArchivedCustomerOnly(id)
    }

    @Query("DELETE FROM well_owner_purchases WHERE ownerCustomerId IN (SELECT id FROM customers WHERE isArchived = 1)")
    suspend fun deletePurchasesForArchivedCustomers(): Int

    @Query("DELETE FROM vouchers WHERE customerId IN (SELECT id FROM customers WHERE isArchived = 1)")
    suspend fun deleteVouchersForArchivedCustomers(): Int

    @Query("DELETE FROM water_sessions WHERE customerId IN (SELECT id FROM customers WHERE isArchived = 1)")
    suspend fun deleteSessionsForArchivedCustomers(): Int

    @Query("DELETE FROM customers WHERE isArchived = 1")
    suspend fun deleteArchivedCustomersOnly(): Int

    @Transaction
    suspend fun emptyRecycleBin() {
        deletePurchasesForArchivedCustomers()
        deleteVouchersForArchivedCustomers()
        deleteSessionsForArchivedCustomers()
        deleteArchivedCustomersOnly()
    }

    @Query("UPDATE customers SET customGroup = :newGroup WHERE customGroup = :oldGroup")
    suspend fun renameCustomGroup(oldGroup: String, newGroup: String)

    @Query("UPDATE customers SET customGroup = '' WHERE customGroup = :group")
    suspend fun clearCustomGroup(group: String)

    @Query("UPDATE customers SET customGroup = :group WHERE id IN (:customerIds)")
    suspend fun assignCustomGroup(customerIds: List<Long>, group: String)

    @Query("UPDATE customers SET customGroup = '' WHERE id IN (:customerIds) AND customGroup = :group")
    suspend fun removeCustomGroup(customerIds: List<Long>, group: String)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}
