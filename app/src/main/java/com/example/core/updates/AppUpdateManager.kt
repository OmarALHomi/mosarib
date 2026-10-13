package com.example.core.updates

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.core.license.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppBroadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // INFO, WARNING, UPDATE
    val actionUrl: String = "",
    val actionText: String = "فتح الرابط",
    val minRequiredVersionCode: Int = 1,
    val isForceUpdate: Boolean = false,
    val publishedAt: Long = 0L
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val FIREBASE_DB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val PREFS_FILE = "mosarib_license_prefs"
    private const val KEY_LAST_SEEN_BROADCAST_ID = "last_seen_broadcast_id"

    suspend fun checkBroadcastAndUpdates(context: Context): AppBroadcast? = withContext(Dispatchers.IO) {
        if (!LicenseManager.isInternetAvailable(context)) return@withContext null

        try {
            val url = URL("$FIREBASE_DB_URL/app_broadcast.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val res = reader.readText().trim()
                reader.close()

                if (res != "null" && res.isNotBlank()) {
                    val obj = JSONObject(res)
                    val id = obj.optString("id", "")
                    val minVersion = obj.optInt("minRequiredVersionCode", 1)
                    val isForceUpdate = obj.optBoolean("isForceUpdate", false) || (BuildConfig.VERSION_CODE < minVersion)

                    return@withContext AppBroadcast(
                        id = id,
                        title = obj.optString("title", "تنبيه جديد"),
                        message = obj.optString("message", ""),
                        type = obj.optString("type", if (isForceUpdate) "UPDATE" else "INFO"),
                        actionUrl = obj.optString("actionUrl", ""),
                        actionText = obj.optString("actionText", if (isForceUpdate) "تحديث التطبيق الآن" else "عرض التفاصيل"),
                        minRequiredVersionCode = minVersion,
                        isForceUpdate = isForceUpdate,
                        publishedAt = obj.optLong("publishedAt", 0L)
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch updates/broadcasts: ${e.message}")
            null
        }
    }

    fun isBroadcastSeen(context: Context, broadcastId: String): Boolean {
        if (broadcastId.isBlank()) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_LAST_SEEN_BROADCAST_ID, "") == broadcastId
    }

    fun markBroadcastSeen(context: Context, broadcastId: String) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LAST_SEEN_BROADCAST_ID, broadcastId).apply()
    }
}
