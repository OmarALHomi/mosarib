package com.baynana.data.local.settlement

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * قراءة وكتابة الصلوح وأقساطها وسعايتها. لا حذف هنا: الصلح يُفسخ بحالة معلنة (والقيد يُعكس في
 * الدفتر)، لأن الحذف يمحو بيانًا شارك فيه طرف آخر.
 */
@Dao
interface DealDao {

    // ------------------------------------------------------------------ الصلوح

    @Upsert
    suspend fun upsertDeal(deal: DealRecord)

    @Query("SELECT * FROM deals WHERE id = :id")
    suspend fun getDeal(id: String): DealRecord?

    @Query("SELECT * FROM deals ORDER BY dealAt DESC")
    fun observeDeals(): Flow<List<DealRecord>>

    @Query("SELECT * FROM deals ORDER BY dealAt ASC")
    suspend fun getAllDealsForBackup(): List<DealRecord>

    /** الصلوح التي تحجز عرضًا: قائمة أو مكتملة. المسودة والمفسوخ لا يحجزان. */
    @Query("SELECT * FROM deals WHERE listingId = :listingId AND status IN ('PENDING', 'ACTIVE', 'COMPLETED')")
    suspend fun getBlockingDealsForListing(listingId: String): List<DealRecord>

    @Query("SELECT * FROM deals WHERE listingId IS NOT NULL AND status IN ('PENDING', 'ACTIVE', 'COMPLETED')")
    suspend fun getBlockingDeals(): List<DealRecord>

    @Query("SELECT COUNT(*) FROM deals WHERE listingId = :listingId AND status = 'COMPLETED'")
    suspend fun countCompletedDealsForListing(listingId: String): Int

    @Query("UPDATE deals SET status = :status, updatedAt = :updatedAt, closedAt = :closedAt WHERE id = :id")
    suspend fun setStatus(id: String, status: String, updatedAt: Long, closedAt: Long?)

    @Query(
        "UPDATE deals SET commissionPayer = :payer, commissionRateBasisPoints = :rateBasisPoints, " +
            "commissionTotalMinor = :totalMinor, commissionSellerMinor = :sellerMinor, " +
            "commissionBuyerMinor = :buyerMinor, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateCommission(
        id: String,
        payer: String,
        rateBasisPoints: Int,
        totalMinor: Long,
        sellerMinor: Long,
        buyerMinor: Long,
        updatedAt: Long
    )

    // ------------------------------------------------------------------ الأقساط

    @Upsert
    suspend fun upsertInstallments(installments: List<DealInstallment>)

    @Upsert
    suspend fun upsertInstallment(installment: DealInstallment)

    @Query("SELECT * FROM deal_installments WHERE dealId = :dealId ORDER BY seq ASC")
    suspend fun getInstallments(dealId: String): List<DealInstallment>

    @Query("SELECT * FROM deal_installments ORDER BY dealId ASC, seq ASC")
    suspend fun getAllInstallmentsForBackup(): List<DealInstallment>

    @Query("UPDATE deal_installments SET paidMinor = :paidMinor, status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInstallment(id: String, paidMinor: Long, status: String, updatedAt: Long)

    @Query("SELECT * FROM deal_installments WHERE dealId = :dealId AND status IN ('SCHEDULED', 'PARTIAL') ORDER BY dueAt ASC, seq ASC")
    suspend fun getOpenInstallments(dealId: String): List<DealInstallment>

    // ------------------------------------------------------------------ السعاية

    @Upsert
    suspend fun upsertCommission(commission: DealCommission)

    @Query("SELECT * FROM deal_commissions WHERE dealId = :dealId ORDER BY createdAt ASC")
    suspend fun getCommissions(dealId: String): List<DealCommission>

    @Query("SELECT * FROM deal_commissions ORDER BY dealId ASC")
    suspend fun getAllCommissionsForBackup(): List<DealCommission>

    @Query("SELECT * FROM deal_commissions WHERE entryId = :entryId")
    suspend fun getCommissionByEntry(entryId: String): DealCommission?

    @Query("UPDATE deal_commissions SET status = :status, updatedAt = :updatedAt WHERE dealId = :dealId")
    suspend fun setCommissionStatus(dealId: String, status: String, updatedAt: Long)
}
