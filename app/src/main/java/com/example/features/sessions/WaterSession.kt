package com.example.features.sessions

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.features.customers.Customer

@Entity(
    tableName = "water_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerId"),
        Index("startTime")
    ]
)
data class WaterSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val pumpName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val pricePerHour: Double = 0.0, // Historical snapshot price per hour
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingDebt: Double = 0.0,
    val notes: String = "",
    val isLive: Boolean = false,
    val billedToCustomerId: Long? = null, // إذا كان السقي مسجلاً ومحسوباً على حساب مستفيد آخر
    val createdAt: Long = System.currentTimeMillis()
)
