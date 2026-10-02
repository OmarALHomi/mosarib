package com.baynana.features.vouchers

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.baynana.features.customers.Customer

enum class VoucherType {
    RECEIPT,  // سند قبض (تحصيل من عميل)
    EXPENSE,  // سند صرف / مصروفات عامة أو للمستفيد
    DISCOUNT  // خصم / تسوية
}

@Entity(
    tableName = "vouchers",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("customerId"),
        Index("date")
    ]
)
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String = "",
    val type: VoucherType = VoucherType.RECEIPT,
    val customerId: Long? = null,
    val sessionId: Long? = null, // ربط السند مباشرة بجلسة سقي محددة
    val amount: Double = 0.0,
    val category: String = "عام", // سداد حساب, أو بيان المصروف
    val paymentMethod: String = "نقداً", // نقداً, حوالة مالية, شبكة / بنكي
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
