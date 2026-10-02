package com.example.features.deals

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import java.util.Locale

/**
 * Represents a formal agricultural settlement deal between seller (farmer),
 * buyer (mojabri), and broker (dallal) in the Jerba ecosystem.
 */
@Entity(tableName = "settlement_deals")
data class SettlementDeal(
    @PrimaryKey
    val id: String,
    val dealNumber: String,
    val cropTitle: String,
    val cropType: String,
    val location: String = "",
    val sellerName: String,
    val sellerPhone: String = "",
    val buyerName: String,
    val buyerPhone: String = "",
    val dallalName: String = "",
    val dallalPhone: String = "",
    val totalAmount: Double,
    val advancePayment: Double,
    val dallalCommission: Double = 0.0,
    val commissionPaid: Double = 0.0,
    val remainingAmount: Double,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val dealDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val termsNotes: String = "",
    val syncStatus: String = "SYNCED"
) {
    val isCompleted: Boolean get() = status == "COMPLETED" || remainingAmount <= 0.0

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " ريال"
    }
}

/**
 * Represents an installment or commission payment against a settlement deal.
 */
@Entity(tableName = "deal_payments")
data class DealPayment(
    @PrimaryKey
    val id: String,
    val dealId: String,
    val amount: Double,
    val paidBy: String = "BUYER",
    val paymentType: String = "INSTALLMENT", // "INSTALLMENT", "COMMISSION"
    val notes: String = "",
    val paymentDate: Long = System.currentTimeMillis()
)

data class DealWithPayments(
    @Embedded val deal: SettlementDeal,
    @Relation(
        parentColumn = "id",
        entityColumn = "dealId"
    )
    val payments: List<DealPayment>
)

@Dao
interface SettlementDealDao {
    @Query("SELECT * FROM settlement_deals ORDER BY dealDate DESC")
    fun getAllDeals(): Flow<List<SettlementDeal>>

    @Query("SELECT * FROM settlement_deals WHERE id = :id")
    suspend fun getDealById(id: String): SettlementDeal?

    @Transaction
    @Query("SELECT * FROM settlement_deals WHERE id = :id")
    fun getDealWithPayments(id: String): Flow<DealWithPayments?>

    @Query("SELECT * FROM deal_payments WHERE dealId = :dealId ORDER BY paymentDate DESC")
    fun getPaymentsForDeal(dealId: String): Flow<List<DealPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: SettlementDeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeals(deals: List<SettlementDeal>)

    @Update
    suspend fun updateDeal(deal: SettlementDeal)

    @Query("UPDATE settlement_deals SET remainingAmount = :remaining, status = :status WHERE id = :id")
    suspend fun updateRemaining(id: String, remaining: Double, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DealPayment)

    @Query("DELETE FROM settlement_deals WHERE id = :id")
    suspend fun deleteDeal(id: String)
}
