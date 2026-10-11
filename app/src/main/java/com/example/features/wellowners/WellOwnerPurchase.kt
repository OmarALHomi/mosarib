package com.example.features.wellowners

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.util.Formatters
import com.example.features.customers.Customer

/**
 * A purchase of pumping hours from a well owner. Purchases are deliberately independent of
 * customer irrigation sessions: a purchase creates an owner payable, while a session creates a
 * customer receivable.
 */
@Entity(
    tableName = "well_owner_purchases",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["ownerCustomerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("ownerCustomerId"), Index("date")]
)
data class WellOwnerPurchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerCustomerId: Long,
    val date: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    /** Minutes wasted due to the well owner; these minutes reduce the amount payable to them. */
    val wastedMinutesOnOwner: Int = 0,
    /** Purchase-rate snapshot, so later price changes never rewrite historical accounts. */
    val purchaseRatePerHour: Double,
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Pure, shared purchase calculations used by account balances, reports, and the purchase form. */
object WellOwnerPurchaseMath {
    fun chargeableMinutes(purchase: WellOwnerPurchase): Int =
        (purchase.durationMinutes - purchase.wastedMinutesOnOwner.coerceIn(0, purchase.durationMinutes.coerceAtLeast(0)))
            .coerceAtLeast(0)

    fun grossAmount(purchase: WellOwnerPurchase): Double =
        Formatters.calculateWaterCost(purchase.durationMinutes.coerceAtLeast(0), purchase.purchaseRatePerHour.coerceAtLeast(0.0))

    fun payableAmount(purchase: WellOwnerPurchase): Double =
        Formatters.calculateWaterCost(chargeableMinutes(purchase), purchase.purchaseRatePerHour.coerceAtLeast(0.0))

    /** Difference-based credit guarantees gross amount = payable amount + owner-waste credit. */
    fun ownerWasteCredit(purchase: WellOwnerPurchase): Double =
        Formatters.roundMoney((grossAmount(purchase) - payableAmount(purchase)).coerceAtLeast(0.0))
}
