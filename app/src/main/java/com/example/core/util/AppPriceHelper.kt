package com.example.core.util

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.core.network.FirebaseRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AppPriceHelper {
    private const val PREFS_FILE = "mosarib_price_sync_prefs"
    private const val KEY_LAST_SYNCED_PRICE = "last_synced_hourly_price"

    suspend fun getEffectiveHourlyPrice(context: Context): Double {
        return try {
            val db = AppDatabase.getDatabase(context)
            val valStr = db.appSettingDao().getSettingValue("default_price_per_hour")
            valStr?.toDoubleOrNull() ?: 5000.0
        } catch (_: Exception) {
            5000.0
        }
    }

    fun getLastSyncedPrice(context: Context): Double? {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return if (sp.contains(KEY_LAST_SYNCED_PRICE)) {
            sp.getFloat(KEY_LAST_SYNCED_PRICE, 0f).toDouble()
        } else {
            null
        }
    }

    fun markPriceAsSynced(context: Context, price: Double) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putFloat(KEY_LAST_SYNCED_PRICE, price.toFloat()).apply()
    }

    /**
     * Broadcasts the new hourly price to Firebase ONLY if it changed locally.
     * Uses PATCH directly on the user node or market rate node to write ONLY when needed,
     * saving Firebase reads/writes and keeping quota strictly protected.
     */
    suspend fun broadcastPriceIfChanged(context: Context, newPrice: Double): Boolean = withContext(Dispatchers.IO) {
        if (newPrice <= 0.0) return@withContext false

        // Only genuine, paid/activated distributors subscribed to the full version can broadcast hourly rates
        val isActivated = com.example.core.license.LicenseManager.isActivated(context)
        val isBlocked = com.example.core.license.UserDeviceTracker.isDeviceBlockedLocally(context)
        if (!isActivated || isBlocked) {
            return@withContext false
        }

        val lastSynced = getLastSyncedPrice(context)
        // If price is identical to last broadcasted value, do NOT send anything (0 Firebase cost)
        if (lastSynced != null && Math.abs(lastSynced - newPrice) < 0.01) {
            return@withContext false
        }

        try {
            val deviceCode = com.example.core.license.LicenseManager.getDeviceCode(context)
            val cleanDeviceKey = deviceCode.replace("-", "").trim()

            // 1. Send PATCH only with the price field to device node (Zero unnecessary reads)
            val patchPayload = JSONObject().apply {
                put("pricePerHour", newPrice)
                put("hourlyRate", newPrice)
                put("priceUpdatedAt", System.currentTimeMillis())
            }

            if (FirebaseRest.patch("all_users/$cleanDeviceKey.json", patchPayload.toString(), timeoutMs = 4000).isSuccess) {
                markPriceAsSynced(context, newPrice)
                return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}

