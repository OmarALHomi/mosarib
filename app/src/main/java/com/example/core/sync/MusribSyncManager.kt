package com.example.core.sync

import android.util.Log
import com.example.features.customers.Customer
import com.example.features.farmer.LinkedMusrib
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Handles real-time, offline-first synchronization between Musrib and Farmer.
 * Bridges both WaterSessions and Vouchers (FIFO payments/receipts) under a unified channel
 * to ensure mathematical consistency between both devices.
 */
object MusribSyncManager {

    private const val TAG = "MusribSyncManager"
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
                Log.w(TAG, "Firestore settings already applied or error: ${e.message}")
            }
        }
        db
    }

    /**
     * Unified model for entries displayed in the Farmer's ledger.
     */
    data class MusribCloudEntry(
        val entryId: String = "",
        val entryType: String = "", // "WATER_SESSION" | "VOUCHER"
        val date: String = "",
        val notes: String = "",
        val timestamp: Long = 0L,
        // Water Session details
        val hours: Int = 0,
        val minutes: Int = 0,
        val pricePerHour: Double = 0.0,
        val totalPrice: Double = 0.0,
        val amountPaid: Double = 0.0,
        val remainingDebt: Double = 0.0,
        // Voucher details
        val voucherType: String = "", // "RECEIPT" | "PAYMENT"
        val voucherAmount: Double = 0.0,
        val receiptNumber: String = ""
    )

    /**
     * Pushes or updates the master summary document for a linked customer under musrib_links/{linkCode}.
     */
    suspend fun syncCustomerSummary(
        customer: Customer,
        distributorName: String,
        distributorPhone: String,
        totalBilled: Double,
        totalPaid: Double,
        currentBalance: Double
    ) {
        val linkCode = customer.linkCode.trim().uppercase()
        if (linkCode.isBlank()) return

        val docData = hashMapOf(
            "linkCode" to linkCode,
            "musribName" to distributorName,
            "musribPhone" to distributorPhone,
            "customerName" to customer.name,
            "customerPhone" to customer.phone,
            "farmName" to customer.farmName,
            "currentBalance" to currentBalance,
            "totalDebit" to totalBilled,
            "totalPaid" to totalPaid,
            "lastUpdatedAt" to System.currentTimeMillis()
        )

        try {
            firestore.collection("musrib_links").document(linkCode)
                .set(docData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync customer summary for $linkCode: ${e.message}")
        }
    }

    /**
     * Pushes a WaterSession to musrib_entries/{linkCode}/entries/session_{id}.
     */
    suspend fun syncSession(linkCode: String, session: WaterSession) {
        val code = linkCode.trim().uppercase()
        if (code.isBlank()) return

        val entryId = "session_${session.id}"
        val h = session.durationMinutes / 60
        val m = session.durationMinutes % 60
        val entryData = hashMapOf(
            "entryId" to entryId,
            "entryType" to "WATER_SESSION",
            "date" to com.example.core.util.Formatters.formatDate(session.startTime),
            "hours" to h,
            "minutes" to m,
            "pricePerHour" to session.pricePerHour,
            "totalPrice" to session.totalAmount,
            "amountPaid" to session.amountPaid,
            "remainingDebt" to session.remainingDebt,
            "notes" to session.notes,
            "timestamp" to session.createdAt
        )

        try {
            firestore.collection("musrib_entries")
                .document(code)
                .collection("entries")
                .document(entryId)
                .set(entryData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync session $entryId for $code: ${e.message}")
        }
    }

    /**
     * Pushes a Voucher (Receipt or Payment) to musrib_entries/{linkCode}/entries/voucher_{id}.
     */
    suspend fun syncVoucher(linkCode: String, voucher: Voucher) {
        val code = linkCode.trim().uppercase()
        if (code.isBlank()) return

        val entryId = "voucher_${voucher.id}"
        val entryData = hashMapOf(
            "entryId" to entryId,
            "entryType" to "VOUCHER",
            "date" to com.example.core.util.Formatters.formatDate(voucher.date),
            "voucherType" to voucher.type.name,
            "voucherAmount" to voucher.amount,
            "receiptNumber" to voucher.voucherNumber,
            "notes" to voucher.notes,
            "timestamp" to voucher.createdAt
        )

        try {
            firestore.collection("musrib_entries")
                .document(code)
                .collection("entries")
                .document(entryId)
                .set(entryData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync voucher $entryId for $code: ${e.message}")
        }
    }

    /**
     * Deletes an entry from Firestore when deleted locally by Musrib.
     */
    suspend fun deleteEntry(linkCode: String, entryId: String) {
        val code = linkCode.trim().uppercase()
        if (code.isBlank()) return

        try {
            firestore.collection("musrib_entries")
                .document(code)
                .collection("entries")
                .document(entryId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete entry $entryId: ${e.message}")
        }
    }

    /**
     * For Farmer: Fetches the summary of a linkCode to add or refresh a linked Musrib account.
     */
    suspend fun fetchLinkedMusrib(linkCode: String): Result<LinkedMusrib> {
        val code = linkCode.trim().uppercase()
        return try {
            val doc = firestore.collection("musrib_links").document(code).get().await()
            if (!doc.exists()) {
                Result.failure(IllegalArgumentException("كود الربط غير صحيح أو لم يتم تفعيله بعد من المسرب"))
            } else {
                val data = doc.data ?: return Result.failure(IllegalStateException("بيانات غير مكتملة"))
                val linked = LinkedMusrib(
                    linkCode = code,
                    musribName = data["musribName"] as? String ?: "المسرب",
                    musribPhone = data["musribPhone"] as? String ?: "",
                    farmName = data["farmName"] as? String ?: "",
                    currentBalance = (data["currentBalance"] as? Number)?.toDouble() ?: 0.0,
                    totalDebit = (data["totalDebit"] as? Number)?.toDouble() ?: 0.0,
                    totalPaid = (data["totalPaid"] as? Number)?.toDouble() ?: 0.0,
                    lastSyncTimestamp = (data["lastUpdatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                Result.success(linked)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * For Farmer: Listens to the full chronological stream of sessions and vouchers for this linkCode.
     */
    fun listenToEntries(
        linkCode: String,
        onEntries: (List<MusribCloudEntry>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        val code = linkCode.trim().uppercase()
        return firestore.collection("musrib_entries")
            .document(code)
            .collection("entries")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val d = doc.data ?: return@mapNotNull null
                        MusribCloudEntry(
                            entryId = doc.id,
                            entryType = d["entryType"] as? String ?: "WATER_SESSION",
                            date = d["date"] as? String ?: "",
                            notes = d["notes"] as? String ?: "",
                            timestamp = (d["timestamp"] as? Number)?.toLong() ?: 0L,
                            hours = (d["hours"] as? Number)?.toInt() ?: 0,
                            minutes = (d["minutes"] as? Number)?.toInt() ?: 0,
                            pricePerHour = (d["pricePerHour"] as? Number)?.toDouble() ?: 0.0,
                            totalPrice = (d["totalPrice"] as? Number)?.toDouble() ?: 0.0,
                            amountPaid = (d["amountPaid"] as? Number)?.toDouble() ?: 0.0,
                            remainingDebt = (d["remainingDebt"] as? Number)?.toDouble() ?: 0.0,
                            voucherType = d["voucherType"] as? String ?: "",
                            voucherAmount = (d["voucherAmount"] as? Number)?.toDouble() ?: 0.0,
                            receiptNumber = d["receiptNumber"] as? String ?: ""
                        )
                    }
                    onEntries(list)
                }
            }
    }
}
