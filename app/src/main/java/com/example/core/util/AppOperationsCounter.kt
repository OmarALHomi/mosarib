package com.example.core.util

import android.content.Context
import com.example.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Centrally computes the exact, real-time total operations count directly from Room database.
 * Total operations = Sessions + Vouchers + Well Owner Purchases.
 */
object AppOperationsCounter {
    suspend fun getExactTotalOperations(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val sessions = db.waterSessionDao().getSessionsCount().first()
            val vouchers = db.voucherDao().getVouchersCount().first()
            val purchases = db.wellOwnerPurchaseDao().getPurchasesCount().first()
            sessions + vouchers + purchases
        } catch (_: Exception) {
            0
        }
    }
}
