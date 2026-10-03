package com.baynana.data.local.settlement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.domain.settlement.CommissionPayer
import com.baynana.domain.settlement.CommissionStatus
import com.baynana.domain.settlement.DealStatus
import com.baynana.domain.settlement.InstallmentStatus

/**
 * جداول الصلح (ح٨). **الجدولان القديمان `settlement_deals`/`deal_payments` لا يُمَسّان**: مبالغهما
 * `Double` (مخالفة لـADR-04) ومقترنة بمنطق قديم، فنُبقيها للقراءة والنسخ الاحتياطي ونبني الجديد
 * على الوحدة الصغرى. من يريد نقل صلح قديم يمرّ من ترحيل الإرث (ح٧/ح٢٣) بجرد صريح.
 *
 * المبادئ:
 * - **المال بالوحدة الصغرى** (`Long` + رمز العملة)، بلا كسور عشرية (ADR-04).
 * - **الصفوف لا تُمحى**: `RESTRICT` على الغرفة والصلح، والفسخ حالة معلنة لا حذف.
 * - **الأقساط جدول مستقل** (`deal_installments`) لأن استحقاق القسط بيان له تاريخه وحالته،
 *   وليس مجرد سطر في نصّ.
 * - **السعاية بيان مستقل** (`deal_commissions`) مربوط بقيده في غرفة الدلال/الوسيط، فلا تختلط
 *   بدَين المحصول في الغرفة نفسها.
 */

/**
 * الصلح: بيانه وأطرافه وأرقامه.
 *
 * [roomId] هي غرفة الدفتر التي تحمل المال (دَين المحصول والعربون والأقساط)، و[listingId] هو العرض
 * في السوق — وهو حاجز منع البيع المزدوج.
 */
@Entity(
    tableName = "deals",
    foreignKeys = [
        ForeignKey(
            entity = LedgerRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("roomId"),
        Index("listingId"),
        Index("status"),
        Index("dealAt"),
        Index("currency")
    ]
)
data class DealRecord(
    @PrimaryKey val id: String,
    val roomId: String,
    val listingId: String? = null,
    val cropTitle: String,
    val place: String = "",
    val currency: String,
    val totalMinor: Long,
    val advanceMinor: Long,
    val sellerMemberId: String,
    val sellerName: String,
    val buyerMemberId: String,
    val buyerName: String,
    val brokerMemberId: String = "",
    val brokerName: String = "",
    val commissionTotalMinor: Long = 0L,
    val commissionSellerMinor: Long = 0L,
    val commissionBuyerMinor: Long = 0L,
    val commissionPayer: String = CommissionPayer.BUYER,
    val commissionRateBasisPoints: Int = 0,
    val status: String = DealStatus.PENDING,
    val dealAt: Long,
    val firstDueAt: Long? = null,
    val intervalDays: Int = 30,
    val terms: String = "",
    val createdByMemberId: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null
)

/** قسط واحد من جدول أقساط الصلح. [seq] ترتيبه ابتداءً من 1. */
@Entity(
    tableName = "deal_installments",
    foreignKeys = [
        ForeignKey(
            entity = DealRecord::class,
            parentColumns = ["id"],
            childColumns = ["dealId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("dealId"),
        Index("dueAt"),
        Index("status"),
        Index(value = ["dealId", "seq"], unique = true)
    ]
)
data class DealInstallment(
    @PrimaryKey val id: String,
    val dealId: String,
    @ColumnInfo(name = "seq") val seq: Int,
    val dueAt: Long,
    val amountMinor: Long,
    val paidMinor: Long = 0L,
    val status: String = InstallmentStatus.SCHEDULED,
    val createdAt: Long,
    val updatedAt: Long
) {
    val openMinor: Long get() = (amountMinor - paidMinor).coerceAtLeast(0L)
}

/**
 * سعاية الدلال: بيان مستقل في غرفته، ومربوط بقيده ([entryId]) للمراجعة والمطابقة.
 * [memberId] هو من له السعاية (الدلال)، و[entryId] هو القيد الذي يمثلها في دفتره.
 */
@Entity(
    tableName = "deal_commissions",
    foreignKeys = [
        ForeignKey(
            entity = DealRecord::class,
            parentColumns = ["id"],
            childColumns = ["dealId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LedgerRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("dealId"),
        Index("roomId"),
        Index("entryId"),
        Index("status")
    ]
)
data class DealCommission(
    @PrimaryKey val id: String,
    val dealId: String,
    val roomId: String,
    val memberId: String,
    val payerMemberId: String,
    val currency: String,
    val totalMinor: Long,
    val entryId: String? = null,
    val status: String = CommissionStatus.OPEN,
    val createdAt: Long,
    val updatedAt: Long
)
