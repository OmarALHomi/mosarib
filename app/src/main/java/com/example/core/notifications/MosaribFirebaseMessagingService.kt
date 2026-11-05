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

class MosaribFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        FcmTokenHelper.uploadToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "مُسَرِّب"
        val body = remoteMessage.notification?.body ?: data["message"] ?: data["body"] ?: ""
        val actionUrl = (data["actionUrl"] ?: "").trim()
        val type = data["type"] ?: "GENERAL"
        val imageUrl = (data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString() ?: "").trim()
        val openInApp = data["openInApp"] == "true"

        // Runs on FCM's worker thread, so a short blocking image download is fine (5 s timeout)
        val picture = if (RemoteImageLoader.isAllowedUrl(imageUrl)) {
            kotlinx.coroutines.runBlocking { RemoteImageLoader.load(imageUrl) }
        } else null

        showNotification(title, body, actionUrl, type, picture, openInApp)
    }

    private fun showNotification(
        title: String,
        body: String,
        actionUrl: String,
        type: String,
        picture: android.graphics.Bitmap? = null,
        openInApp: Boolean = false
    ) {
        val channelId = "mosarib_notifications_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "إشعارات وتنبيهات مُسَرِّب"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "قناة إشعارات التحديثات، التنبيهات والأخبار الميدانية"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val requestCode = System.currentTimeMillis().toInt()
        val webLink = actionUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        // https link -> browser (unless the admin chose "open in app"); mosarib:// -> that screen; else the app
        val intent = if (webLink != null && !openInApp) {
            Intent(Intent.ACTION_VIEW, Uri.parse(webLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            DeepLinks.appIntent(this, actionUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                if (picture != null) {
                    NotificationCompat.BigPictureStyle().bigPicture(picture).setSummaryText(body)
                } else {
                    NotificationCompat.BigTextStyle().bigText(body)
                }
            )
            .apply { if (picture != null) setLargeIcon(picture) }
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        if (webLink != null) {
            val linkIntent = PendingIntent.getActivity(
                this,
                requestCode + 1,
                Intent(Intent.ACTION_VIEW, Uri.parse(webLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(0, "فتح الرابط", linkIntent)
        }

        notificationManager.notify(requestCode, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MosaribFCM"
    }
}
