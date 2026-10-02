package com.baynana.features.market

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Represents an agricultural crop/fruit listing in the Baynana harvest market.
 * Designed to empower Dallals (brokers) while protecting their commissions by
 * masking the farmer's contact information by default.
 */
@Entity(tableName = "crop_listings")
data class CropListing(
    @PrimaryKey
    val id: String,
    val title: String,
    val cropType: String, // "قات", "عنب", "رمان", "بن", "حبوب", "خضار", "فواكه", "أخرى"
    val description: String = "",
    val district: String = "", // العزلة / المديرية
    val village: String = "",  // القرية / المحل
    val priceEstimate: Double = 0.0,
    val priceUnit: String = "شروة كاملة", // "شروة كاملة", "بالكيلو", "بالحبة", "بالقدح", "بالسلة", "على السوم"
    val dallalName: String = "",
    val dallalPhone: String = "",
    val dallalId: String = "",
    val farmerName: String = "",
    val farmerPhone: String = "",
    val hideFarmerPhone: Boolean = true,
    val status: String = "AVAILABLE", // "AVAILABLE", "IN_NEGOTIATION", "SOLD"
    val createdAt: Long = System.currentTimeMillis(),
    val isFeatured: Boolean = false,
    val syncStatus: String = "SYNCED" // "PENDING", "SYNCED"
) {
    val isSold: Boolean get() = status == "SOLD"
    val isAvailable: Boolean get() = status == "AVAILABLE"
    val isInNegotiation: Boolean get() = status == "IN_NEGOTIATION"

    fun getDisplayPrice(): String {
        return if (priceEstimate <= 0.0) {
            "على السوم والمفاوضة"
        } else {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(priceEstimate)
            "$formatted ريال ($priceUnit)"
        }
    }
}

@Dao
interface CropListingDao {
    @Query("SELECT * FROM crop_listings ORDER BY isFeatured DESC, createdAt DESC")
    fun getAllListings(): Flow<List<CropListing>>

    /** كل العروض، للنسخ الاحتياطي (بما فيها المباع والمحذوف محليًا). */
    @Query("SELECT * FROM crop_listings ORDER BY createdAt ASC")
    suspend fun getAllListingsForBackup(): List<CropListing>

    @Query("SELECT * FROM crop_listings WHERE cropType = :type ORDER BY isFeatured DESC, createdAt DESC")
    fun getListingsByType(type: String): Flow<List<CropListing>>

    @Query("SELECT * FROM crop_listings WHERE id = :id")
    suspend fun getListingById(id: String): CropListing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: CropListing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<CropListing>)

    @Update
    suspend fun updateListing(listing: CropListing)

    @Query("UPDATE crop_listings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM crop_listings WHERE id = :id")
    suspend fun deleteListing(id: String)
}
