package com.example.features.wellowners

import kotlinx.coroutines.flow.Flow

class WellOwnerPurchaseRepository(private val dao: WellOwnerPurchaseDao) {
    val allPurchases: Flow<List<WellOwnerPurchase>> = dao.getAllPurchases()

    fun getPurchasesForOwner(ownerCustomerId: Long): Flow<List<WellOwnerPurchase>> =
        dao.getPurchasesForOwner(ownerCustomerId)

    fun getPurchasesBetween(fromTime: Long, toTime: Long): Flow<List<WellOwnerPurchase>> =
        dao.getPurchasesBetween(fromTime, toTime)

    suspend fun insert(purchase: WellOwnerPurchase): Long = dao.insertPurchase(purchase)
    suspend fun update(purchase: WellOwnerPurchase) = dao.updatePurchase(purchase)
    suspend fun delete(purchase: WellOwnerPurchase) = dao.deletePurchase(purchase)
}
