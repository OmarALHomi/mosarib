package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.license.LicenseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MosaribFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "مُسَرِّب"
        val body = remoteMessage.notification?.body ?: data["message"] ?: data["body"] ?: ""
        val actionUrl = data["actionUrl"] ?: ""
        val type = data["type"] ?: "GENERAL"

        showNotification(title, body, actionUrl, type)
    }

    private fun showNotification(title: String, body: String, actionUrl: String, type: String) {
        val channelId = "mosarib_notifications_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "إشعارات وتنبيهات مُسَرِّب"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "قناة إشعارات التحديثات، التنبيهات والأخبار الميدانية"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = if (actionUrl.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceCode = LicenseManager.getDeviceCode(applicationContext)
                val cleanKey = deviceCode.replace("-", "").trim()
                val url = URL("https://mosarib-default-rtdb.firebaseio.com/fcm_tokens/$cleanKey.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val payload = "{\"deviceCode\":\"$deviceCode\",\"fcmToken\":\"$token\",\"updatedAt\":${System.currentTimeMillis()}}"
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                conn.responseCode
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload FCM token: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "MosaribFCM"
    }
}
