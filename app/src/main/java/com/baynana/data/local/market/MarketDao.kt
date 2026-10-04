package com.baynana.data.local.market

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {

    // ------------------------------------------------------------------ العروض

    /**
     * **`@Upsert` لا `@Insert(REPLACE)`** — وهذا ليس تفصيلًا: `INSERT OR REPLACE` في SQLite تُنفَّذ
     * حذفًا ثم إدراجًا، وبما أن `market_contacts` و`market_moderations` أبناء بـ`ON DELETE CASCADE`،
     * فإن كل تحديث للعرض كان يمحو رقم الدلال وصفّ المصادقة معه — عيب حقيقي كشفه اختبار الخصوصية في CI:
     * الرسالة كانت «رقم الدلال مصرّح بنشره فيظهر» والرقم غير موجود. و`@Upsert` تُنفَّذ
     * `ON CONFLICT DO UPDATE` فلا حذف ولا سلسلة حذف. ويحرس هذا حاجز ١٨ في `tools/ci_invariants.sh`.
     */
    @Upsert
    suspend fun upsertListing(row: MarketListingRow)

    @Upsert
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

    @Upsert
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

    @Upsert
    suspend fun upsertModeration(row: ModerationRow)

    @Query("SELECT * FROM market_moderations WHERE listingId = :listingId AND revision = :revision")
    suspend fun getModeration(listingId: String, revision: Int): ModerationRow?

    @Query("SELECT * FROM market_moderations WHERE listingId = :listingId ORDER BY revision DESC LIMIT 1")
    suspend fun getLatestModeration(listingId: String): ModerationRow?

    @Query("SELECT * FROM market_moderations WHERE decision = 'APPROVED' ORDER BY decidedAt DESC")
    suspend fun getApprovedModerations(): List<ModerationRow>
}
