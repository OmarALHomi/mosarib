package com.example.core.notifications

import android.content.Context
import android.util.Log
import com.example.core.license.LicenseManager
import com.example.core.network.FirebaseRest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object FcmTokenHelper {
    private const val TAG = "FcmTokenHelper"

    fun syncCurrentToken(context: Context) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all")
            FirebaseMessaging.getInstance().subscribeToTopic("mosarib_users")
        } catch (e: Exception) {
            Log.e(TAG, "Topic subscription error: ${e.message}")
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result ?: return@addOnCompleteListener
            Log.d(TAG, "Current FCM Token: $token")

            uploadToken(context, token)
        }
    }

    /** Stores this device's push token under /fcm_tokens so the dashboard can target it. */
    fun uploadToken(context: Context, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val deviceCode = LicenseManager.getDeviceCode(context)
            val payload = JSONObject().apply {
                put("deviceCode", deviceCode)
                put("fcmToken", token)
                put("updatedAt", System.currentTimeMillis())
            }
            FirebaseRest.put("fcm_tokens/${deviceCode.replace("-", "").trim()}.json", payload.toString(), timeoutMs = 6000)
        }
    }
}
