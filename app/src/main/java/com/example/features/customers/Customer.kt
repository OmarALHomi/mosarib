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
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
