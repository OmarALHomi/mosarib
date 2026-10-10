package com.example.features.wellowners

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WellOwnerPurchaseDao {
    @Query("SELECT COUNT(*) FROM well_owner_purchases")
    fun getPurchasesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM well_owner_purchases")
    suspend fun getPurchasesCountDirect(): Int

    @Query("SELECT * FROM well_owner_purchases ORDER BY date DESC, id DESC")
    fun getAllPurchases(): Flow<List<WellOwnerPurchase>>

    @Query("SELECT * FROM well_owner_purchases WHERE ownerCustomerId = :ownerCustomerId ORDER BY date DESC, id DESC")
    fun getPurchasesForOwner(ownerCustomerId: Long): Flow<List<WellOwnerPurchase>>

    @Query("SELECT * FROM well_owner_purchases WHERE date >= :fromTime AND date <= :toTime ORDER BY date DESC, id DESC")
    fun getPurchasesBetween(fromTime: Long, toTime: Long): Flow<List<WellOwnerPurchase>>

    @Query("SELECT * FROM well_owner_purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): WellOwnerPurchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: WellOwnerPurchase): Long

    @Update
    suspend fun updatePurchase(purchase: WellOwnerPurchase)

    @Delete
    suspend fun deletePurchase(purchase: WellOwnerPurchase)
}
