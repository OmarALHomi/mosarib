package com.example.features.customers

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    /**
     * كل الصفوف بلا استثناء، للنسخ الاحتياطي فقط.
     * [getAllCustomers] يُسقط المؤرشف، والنسخة الاحتياطية يجب ألا تُسقط شيئًا.
     */
    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersForBackup(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR farmName LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    // A restore/update must not DELETE its parent row and cascade into financial history.
    @Upsert
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCustomer(id: Long)
}
