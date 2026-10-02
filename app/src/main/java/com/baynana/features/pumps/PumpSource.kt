package com.baynana.features.pumps

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pump_sources")
data class PumpSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val locationOrWellNumber: String = "",
    val defaultPricePerHour: Double = 5000.0,
    val powerType: String = "ديزل", // ديزل, كهرباء, طاقة شمسية
    val notes: String = "",
    val isPrimary: Boolean = false,
    val isActive: Boolean = true
)
