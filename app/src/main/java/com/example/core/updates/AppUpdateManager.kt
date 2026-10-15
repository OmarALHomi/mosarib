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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

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
            // 1. Primary: Cloud Firestore REST API
            val fsDoc = com.example.core.license.FirestoreRestHelper.getDocument("app_broadcast", "current")
            if (fsDoc != null) {
                val id = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "id", "")
                val minVersion = com.example.core.license.FirestoreRestHelper.getInt(fsDoc, "minRequiredVersionCode", 1)
                val isForce = com.example.core.license.FirestoreRestHelper.getBoolean(fsDoc, "isForceUpdate", false) || (BuildConfig.VERSION_CODE < minVersion)

                return@withContext AppBroadcast(
                    id = id,
                    title = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "title", "تنبيه جديد"),
                    message = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "message", ""),
                    type = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "type", if (isForce) "UPDATE" else "INFO"),
                    actionUrl = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "actionUrl", ""),
                    actionText = com.example.core.license.FirestoreRestHelper.getString(fsDoc, "actionText", if (isForce) "تحديث التطبيق الآن" else "عرض التفاصيل"),
                    minRequiredVersionCode = minVersion,
                    isForceUpdate = isForce,
                    publishedAt = com.example.core.license.FirestoreRestHelper.getLong(fsDoc, "publishedAt", 0L)
                )
            }

            // 2. Fallback: Realtime Database
            val url = URL("$FIREBASE_DB_URL/app_broadcast.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
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

    private const val KEY_LAST_NOTIFIED_BROADCAST_ID = "last_notified_broadcast_id"

    fun isBroadcastSeen(context: Context, broadcastId: String): Boolean {
        if (broadcastId.isBlank()) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_LAST_SEEN_BROADCAST_ID, "") == broadcastId
    }

    fun markBroadcastSeen(context: Context, broadcastId: String) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LAST_SEEN_BROADCAST_ID, broadcastId).apply()
    }

    fun isBroadcastNotified(context: Context, broadcastId: String): Boolean {
        if (broadcastId.isBlank()) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_LAST_NOTIFIED_BROADCAST_ID, "") == broadcastId
    }

    fun markBroadcastNotified(context: Context, broadcastId: String) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LAST_NOTIFIED_BROADCAST_ID, broadcastId).apply()
    }

    fun postSystemNotification(context: Context, broadcast: AppBroadcast) {
        val channelId = "mosarib_broadcast_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "إعلانات وتحديثات مُسَرِّب"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "قناة إعلانات التحديثات والتنبيهات العامة"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = if (broadcast.actionUrl.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(broadcast.actionUrl))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            broadcast.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(broadcast.title)
            .setContentText(broadcast.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(broadcast.message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(broadcast.id.hashCode(), notification)
        markBroadcastNotified(context, broadcast.id)
    }
}
