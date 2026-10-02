package com.baynana.core.sync

import android.util.Log
import com.baynana.features.market.CropListing
import com.baynana.features.market.CropListingDao
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
 * Manages real-time, offline-first synchronization of Baynana crop and fruit listings.
 * Connects the local Room database with Firestore collection "listings".
 */
object MarketSyncManager {

    private const val TAG = "MarketSyncManager"
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
                Log.w(TAG, "Firestore settings already applied: ${e.message}")
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
     * Listens to real-time market listings and keeps Room cache synchronized.
     */
    fun startListeningToListings(
        dao: CropListingDao,
        coroutineScope: CoroutineScope,
        onSyncStateChanged: (Boolean) -> Unit = {}
    ): ListenerRegistration {
        onSyncStateChanged(true)
        return firestore.collection("listings")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                onSyncStateChanged(false)
                if (error != null) {
                    Log.e(TAG, "Error listening to market listings: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val listings = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val title = doc.getString("title") ?: return@mapNotNull null
                                val cropType = doc.getString("crop_type") ?: "أخرى"
                                val description = doc.getString("description") ?: ""
                                val district = doc.getString("district") ?: ""
                                val village = doc.getString("village") ?: ""
                                val priceEstimate = doc.getDouble("price_estimate") ?: 0.0
                                val priceUnit = doc.getString("price_unit") ?: "شروة كاملة"
                                val dallalName = doc.getString("dallal_name") ?: ""
                                val dallalPhone = doc.getString("dallal_phone") ?: ""
                                val dallalUid = doc.getString("dallal_uid") ?: ""
                                val farmerName = doc.getString("farmer_name") ?: ""
                                val farmerPhone = doc.getString("farmer_phone") ?: ""
                                val hideFarmer = doc.getBoolean("hide_farmer") ?: true
                                val status = doc.getString("status") ?: "AVAILABLE"
                                val createdAt = doc.getLong("created_at") ?: System.currentTimeMillis()
                                val isFeatured = doc.getBoolean("is_featured") ?: false

                                CropListing(
                                    id = id,
                                    title = title,
                                    cropType = cropType,
                                    description = description,
                                    district = district,
                                    village = village,
                                    priceEstimate = priceEstimate,
                                    priceUnit = priceUnit,
                                    dallalName = dallalName,
                                    dallalPhone = dallalPhone,
                                    dallalId = dallalUid,
                                    farmerName = farmerName,
                                    farmerPhone = farmerPhone,
                                    hideFarmerPhone = hideFarmer,
                                    status = status,
                                    createdAt = createdAt,
                                    isFeatured = isFeatured,
                                    syncStatus = "SYNCED"
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse listing doc: ${e.message}")
                                null
                            }
                        }

                        if (listings.isNotEmpty()) {
                            dao.insertListings(listings)
                        }
                    }
                }
            }
    }

    /**
     * Publishes a new listing to Firestore and updates local Room cache.
     */
    suspend fun publishListing(
        listing: CropListing,
        dao: CropListingDao
    ): Result<Unit> {
        return try {
            ensureAuth()
            val auth = FirebaseAuth.getInstance()
            val dallalUid = auth.currentUser?.uid ?: ""

            val docData = hashMapOf(
                "id" to listing.id,
                "title" to listing.title,
                "crop_type" to listing.cropType,
                "description" to listing.description,
                "district" to listing.district,
                "village" to listing.village,
                "price_estimate" to listing.priceEstimate,
                "price_unit" to listing.priceUnit,
                "dallal_name" to listing.dallalName,
                "dallal_phone" to listing.dallalPhone,
                "dallal_uid" to dallalUid,
                "farmer_name" to listing.farmerName,
                "farmer_phone" to if (listing.hideFarmerPhone) "" else listing.farmerPhone,
                "hide_farmer" to listing.hideFarmerPhone,
                "status" to listing.status,
                "created_at" to listing.createdAt,
                "is_featured" to listing.isFeatured
            )

            // Write to Firestore
            firestore.collection("listings")
                .document(listing.id)
                .set(docData, SetOptions.merge())
                .await()

            // Update local Room database
            dao.insertListing(listing.copy(dallalId = dallalUid, syncStatus = "SYNCED"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish listing: ${e.message}")
            // Save locally as PENDING so it isn't lost
            dao.insertListing(listing.copy(syncStatus = "PENDING"))
            Result.failure(e)
        }
    }

    /**
     * Updates listing status (e.g. mark as SOLD).
     */
    suspend fun updateListingStatus(
        id: String,
        status: String,
        dao: CropListingDao
    ): Result<Unit> {
        return try {
            ensureAuth()
            firestore.collection("listings")
                .document(id)
                .update("status", status)
                .await()

            dao.updateStatus(id, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update listing status: ${e.message}")
            dao.updateStatus(id, status)
            Result.failure(e)
        }
    }
}
