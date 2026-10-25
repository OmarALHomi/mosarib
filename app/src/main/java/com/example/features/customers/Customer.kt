package com.example.features.customers

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val farmName: String = "",
    val location: String = "",
    val notes: String = "",
    val customPricePerHour: Double? = null, // Optional special hourly price override for this customer
    val isBeneficiary: Boolean = false, // حساب مستفيد (يقبل الصرف، أو تسجيل سقي له أو لعملاء على حسابه)
    val isWellOwner: Boolean = false, // حساب صاحب بئر مستقل (تسجل له مستحقات الساعات وتصرف له دفعات)
    val customGroup: String = "", // قائمة أو مجموعة خاصة (مثل: مجموعة الشرق، آبار الوادي)
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
