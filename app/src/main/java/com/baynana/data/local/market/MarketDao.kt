package com.baynana.data.local.market

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {

    // ------------------------------------------------------------------ العروض

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListing(row: MarketListingRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(row: MarketContactRow)

    @Query("SELECT * FROM market_listings WHERE id = :id")
    suspend fun getListing(id: String): MarketListingRow?

    @Query("SELECT * FROM market_contacts WHERE listingId = :listingId")
    suspend fun getContact(listingId: String): MarketContactRow?

    /** ما يراه الناس: المعروض والمحجوز فقط، والأحدث نشرًا أولًا. */
    @Query(
        "SELECT * FROM market_listings WHERE status IN ('PUBLISHED','RESERVED') " +
            "ORDER BY COALESCE(publishedAt, updatedAt) DESC"
    )
    fun observePublicListings(): Flow<List<MarketListingRow>>

    /** نفس الاستعلام بلا تدفّق — للاختبارات وللتصدير. */
    @Query(
        "SELECT * FROM market_listings WHERE status IN ('PUBLISHED','RESERVED') " +
            "ORDER BY COALESCE(publishedAt, updatedAt) DESC"
    )
    suspend fun getPublicListings(): List<MarketListingRow>

    @Query("SELECT * FROM market_listings ORDER BY updatedAt DESC")
    fun observeAllListings(): Flow<List<MarketListingRow>>

    @Query("SELECT * FROM market_listings WHERE brokerMemberId = :memberId ORDER BY updatedAt DESC")
    fun observeListingsOfBroker(memberId: String): Flow<List<MarketListingRow>>

    @Query("SELECT COUNT(*) FROM market_listings WHERE status IN ('PUBLISHED','RESERVED')")
    suspend fun publicCount(): Int

    // ------------------------------------------------------------------ طلبات التسويق

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequest(row: MarketingRequestRow)

    @Query("SELECT * FROM market_requests WHERE id = :id")
    suspend fun getRequest(id: String): MarketingRequestRow?

    /** أحدث طلب بين مزارع ودلال — هو الذي يفتح باب النشر لهذا الطرفين. */
    @Query(
        "SELECT * FROM market_requests WHERE farmerMemberId = :farmerMemberId " +
            "AND brokerMemberId = :brokerMemberId ORDER BY requestedAt DESC LIMIT 1"
    )
    suspend fun getRequestBetween(farmerMemberId: String, brokerMemberId: String): MarketingRequestRow?

    @Query("SELECT * FROM market_requests WHERE farmerMemberId = :farmerMemberId ORDER BY requestedAt DESC")
    fun observeRequestsOfFarmer(farmerMemberId: String): Flow<List<MarketingRequestRow>>

    @Query("SELECT * FROM market_requests WHERE brokerMemberId = :brokerMemberId ORDER BY requestedAt DESC")
    suspend fun getRequestsOfBroker(brokerMemberId: String): List<MarketingRequestRow>

    // ------------------------------------------------------------------ المصادقة

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModeration(row: ModerationRow)

    @Query("SELECT * FROM market_moderations WHERE listingId = :listingId AND revision = :revision")
    suspend fun getModeration(listingId: String, revision: Int): ModerationRow?

    @Query("SELECT * FROM market_moderations WHERE listingId = :listingId ORDER BY revision DESC LIMIT 1")
    suspend fun getLatestModeration(listingId: String): ModerationRow?

    @Query("SELECT * FROM market_moderations WHERE decision = 'APPROVED' ORDER BY decidedAt DESC")
    suspend fun getApprovedModerations(): List<ModerationRow>
}
