package com.baynana.core.sync

import android.util.Log
import com.baynana.features.deals.DealPayment
import com.baynana.features.deals.SettlementDeal
import com.baynana.features.deals.SettlementDealDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles real-time offline-first synchronization for agricultural settlement deals
 * and installments under Firestore collection "deals".
 */
object DealsSyncManager {

    private const val TAG = "DealsSyncManager"
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        if (!isInitialized) {
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
                isInitialized = true
            } catch (e: Exception) {
                Log.w(TAG, "Firestore settings error: ${e.message}")
            }
        }
        db
    }

    private suspend fun ensureAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (e: Exception) {
                Log.w(TAG, "Anonymous auth error: ${e.message}")
            }
        }
    }

    /**
     * Real-time listener for deals to update local Room cache.
     */
    fun startListeningToDeals(
        dao: SettlementDealDao,
        coroutineScope: CoroutineScope
    ): ListenerRegistration {
        return firestore.collection("deals")
            .orderBy("deal_date", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to deals: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val deals = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val dealNum = doc.getString("deal_number") ?: "SLH"
                                val cropTitle = doc.getString("crop_title") ?: ""
                                val cropType = doc.getString("crop_type") ?: ""
                                val location = doc.getString("location") ?: ""
                                val sellerName = doc.getString("seller_name") ?: ""
                                val sellerPhone = doc.getString("seller_phone") ?: ""
                                val buyerName = doc.getString("buyer_name") ?: ""
                                val buyerPhone = doc.getString("buyer_phone") ?: ""
                                val dallalName = doc.getString("dallal_name") ?: ""
                                val dallalPhone = doc.getString("dallal_phone") ?: ""
                                val total = doc.getDouble("total_amount") ?: 0.0
                                val advance = doc.getDouble("advance_payment") ?: 0.0
                                val commission = doc.getDouble("dallal_commission") ?: 0.0
                                val commissionPaid = doc.getDouble("commission_paid") ?: 0.0
                                val remaining = doc.getDouble("remaining_amount") ?: (total - advance)
                                val status = doc.getString("status") ?: "ACTIVE"
                                val dealDate = doc.getLong("deal_date") ?: System.currentTimeMillis()
                                val dueDate = doc.getLong("due_date")
                                val notes = doc.getString("terms_notes") ?: ""

                                SettlementDeal(
                                    id = id,
                                    dealNumber = dealNum,
                                    cropTitle = cropTitle,
                                    cropType = cropType,
                                    location = location,
                                    sellerName = sellerName,
                                    sellerPhone = sellerPhone,
                                    buyerName = buyerName,
                                    buyerPhone = buyerPhone,
                                    dallalName = dallalName,
                                    dallalPhone = dallalPhone,
                                    totalAmount = total,
                                    advancePayment = advance,
                                    dallalCommission = commission,
                                    commissionPaid = commissionPaid,
                                    remainingAmount = remaining,
                                    status = status,
                                    dealDate = dealDate,
                                    dueDate = dueDate,
                                    termsNotes = notes,
                                    syncStatus = "SYNCED"
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing deal: ${e.message}")
                                null
                            }
                        }

                        if (deals.isNotEmpty()) {
                            dao.insertDeals(deals)
                        }
                    }
                }
            }
    }

    /**
     * Publishes a new deal to Firestore and stores it locally.
     */
    suspend fun publishDeal(
        deal: SettlementDeal,
        dao: SettlementDealDao
    ): Result<Unit> {
        return try {
            ensureAuth()
            val docData = hashMapOf(
                "deal_number" to deal.dealNumber,
                "crop_title" to deal.cropTitle,
                "crop_type" to deal.cropType,
                "location" to deal.location,
                "seller_name" to deal.sellerName,
                "seller_phone" to deal.sellerPhone,
                "buyer_name" to deal.buyerName,
                "buyer_phone" to deal.buyerPhone,
                "dallal_name" to deal.dallalName,
                "dallal_phone" to deal.dallalPhone,
                "total_amount" to deal.totalAmount,
                "advance_payment" to deal.advancePayment,
                "dallal_commission" to deal.dallalCommission,
                "commission_paid" to deal.commissionPaid,
                "remaining_amount" to deal.remainingAmount,
                "status" to deal.status,
                "deal_date" to deal.dealDate,
                "due_date" to deal.dueDate,
                "terms_notes" to deal.termsNotes
            )

            firestore.collection("deals")
                .document(deal.id)
                .set(docData, SetOptions.merge())
                .await()

            dao.insertDeal(deal.copy(syncStatus = "SYNCED"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish deal: ${e.message}")
            dao.insertDeal(deal.copy(syncStatus = "PENDING"))
            Result.failure(e)
        }
    }

    /**
     * Records an installment payment in Firestore subcollection and updates remaining amount.
     */
    suspend fun recordPayment(
        dealId: String,
        payment: DealPayment,
        newRemaining: Double,
        dao: SettlementDealDao
    ): Result<Unit> {
        return try {
            ensureAuth()
            val status = if (newRemaining <= 0.0) "COMPLETED" else "ACTIVE"

            val paymentData = hashMapOf(
                "id" to payment.id,
                "deal_id" to dealId,
                "amount" to payment.amount,
                "paid_by" to payment.paidBy,
                "payment_type" to payment.paymentType,
                "notes" to payment.notes,
                "payment_date" to payment.paymentDate
            )

            // 1. Add payment doc
            firestore.collection("deals")
                .document(dealId)
                .collection("payments")
                .document(payment.id)
                .set(paymentData)
                .await()

            // 2. Update remaining amount & status in deal doc
            firestore.collection("deals")
                .document(dealId)
                .update(
                    mapOf(
                        "remaining_amount" to newRemaining,
                        "status" to status
                    )
                ).await()

            // 3. Local database update
            dao.insertPayment(payment)
            dao.updateRemaining(dealId, newRemaining, status)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record payment: ${e.message}")
            val status = if (newRemaining <= 0.0) "COMPLETED" else "ACTIVE"
            dao.insertPayment(payment)
            dao.updateRemaining(dealId, newRemaining, status)
            Result.failure(e)
        }
    }
}
