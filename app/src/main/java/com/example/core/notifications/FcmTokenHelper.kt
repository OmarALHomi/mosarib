package com.example.core.notifications

import android.content.Context
import android.util.Log
import com.example.core.license.LicenseManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FcmTokenHelper {
    private const val TAG = "FcmTokenHelper"

    fun syncCurrentToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result ?: return@addOnCompleteListener
            Log.d(TAG, "Current FCM Token: $token")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val deviceCode = LicenseManager.getDeviceCode(context)
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
                    Log.e(TAG, "Failed to upload token: ${e.message}")
                }
            }
        }
    }
}
