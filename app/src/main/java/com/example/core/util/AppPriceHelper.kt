package com.example.core.util

import android.content.Context
import com.example.core.database.AppDatabase

object AppPriceHelper {
    suspend fun getEffectiveHourlyPrice(context: Context): Double {
        return try {
            val db = AppDatabase.getInstance(context)
            val valStr = db.appSettingDao().getSettingValue("default_price_per_hour")
            valStr?.toDoubleOrNull() ?: 5000.0
        } catch (_: Exception) {
            5000.0
        }
    }
}
