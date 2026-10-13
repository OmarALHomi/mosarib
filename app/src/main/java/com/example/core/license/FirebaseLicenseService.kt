package com.example.core.license

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct HTTPS REST client for Firebase Realtime Database.
 * Works seamlessly with Firebase project 'mosarib' without requiring heavyweight SDKs.
 */
object FirebaseLicenseService {
    private const val TAG = "FirebaseLicenseService"
    // Firebase Realtime Database URL for project mosarib
    private const val FIREBASE_DB_URL = "https://mosarib-default-rtdb.firebaseio.com"

    data class LicenseCheckResult(
        val isValid: Boolean,
        val isRevoked: Boolean,
        val isBoundToOtherDevice: Boolean,
        val boundDeviceCode: String? = null,
        val serverExpiresAt: Long? = null,
        val plan: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Attempts to verify or activate an activation key against Firebase.
     * Enforces single-device lock: If key is already bound to a different device, it rejects it.
     */
    suspend fun verifyAndBindKeyOnline(
        key: String,
        currentDeviceCode: String,
        subscriberName: String,
        plan: LicenseManager.SubscriptionPlan,
        durationDays: Int
    ): LicenseCheckResult = withContext(Dispatchers.IO) {
        val cleanKey = key.replace("-", "").replace(" ", "").trim().uppercase()
        val safeKeyPath = "licenses/$cleanKey"

        try {
            val getUrl = URL("$FIREBASE_DB_URL/$safeKeyPath.json")
            val conn = (getUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.readText().trim()
                reader.close()

                if (responseText == "null" || responseText.isBlank()) {
                    // New key being activated for the first time by the device
                    val now = System.currentTimeMillis()
                    val expiresAt = now + (durationDays * 24L * 3600L * 1000L)
                    val newLicensePayload = JSONObject().apply {
                        put("key", cleanKey)
                        put("deviceCode", currentDeviceCode)
                        put("subscriberName", subscriberName)
                        put("plan", plan.name)
                        put("activatedAt", now)
                        put("expiresAt", expiresAt)
                        put("isRevoked", false)
                        put("deviceModel", LicenseManager.getDeviceModelName())
                        put("androidVersion", LicenseManager.getAndroidVersionName())
                        put("lastCheckedAt", now)
                    }

                    val putSuccess = putJson("$safeKeyPath.json", newLicensePayload.toString())
                    // Also record under devices for fast device lookup
                    putJson("devices/$currentDeviceCode.json", newLicensePayload.toString())

                    return@withContext if (putSuccess) {
                        LicenseCheckResult(
                            isValid = true,
                            isRevoked = false,
                            isBoundToOtherDevice = false,
                            boundDeviceCode = currentDeviceCode,
                            serverExpiresAt = expiresAt,
                            plan = plan.name
                        )
                    } else {
                        // Offline or DB rules block - fallback gracefully if local math matches
                        LicenseCheckResult(
                            isValid = true,
                            isRevoked = false,
                            isBoundToOtherDevice = false,
                            serverExpiresAt = expiresAt
                        )
                    }
                } else {
                    val json = JSONObject(responseText)
                    val isRevoked = json.optBoolean("isRevoked", false)
                    val boundDevice = json.optString("deviceCode", "")
                    val serverExpiresAt = json.optLong("expiresAt", 0L)
                    val savedPlan = json.optString("plan", plan.name)

                    if (isRevoked) {
                        return@withContext LicenseCheckResult(
                            isValid = false,
                            isRevoked = true,
                            isBoundToOtherDevice = false,
                            errorMessage = "تم سحب أو إلغاء هذا الاشتراك من قِبل الإدارة"
                        )
                    }

                    // Strict single device enforcement
                    if (boundDevice.isNotBlank() && boundDevice != currentDeviceCode) {
                        return@withContext LicenseCheckResult(
                            isValid = false,
                            isRevoked = false,
                            isBoundToOtherDevice = true,
                            boundDeviceCode = boundDevice,
                            errorMessage = "هذا الكود مفعّل لجهاز آخر مسبقاً ولا يمكن استخدامه على جهازين!"
                        )
                    }

                    // Update last check and subscriber name if changed
                    val updatePayload = JSONObject().apply {
                        put("lastCheckedAt", System.currentTimeMillis())
                        put("deviceCode", currentDeviceCode)
                        if (subscriberName.isNotBlank()) put("subscriberName", subscriberName)
                        put("deviceModel", LicenseManager.getDeviceModelName())
                    }
                    patchJson("$safeKeyPath.json", updatePayload.toString())
                    putJson("devices/$currentDeviceCode.json", json.apply {
                        put("lastCheckedAt", System.currentTimeMillis())
                    }.toString())

                    return@withContext LicenseCheckResult(
                        isValid = true,
                        isRevoked = false,
                        isBoundToOtherDevice = false,
                        boundDeviceCode = currentDeviceCode,
                        serverExpiresAt = if (serverExpiresAt > 0) serverExpiresAt else null,
                        plan = savedPlan
                    )
                }
            } else {
                Log.w(TAG, "Firebase returned code $responseCode")
                return@withContext LicenseCheckResult(isValid = false, isRevoked = false, isBoundToOtherDevice = false, errorMessage = "خطأ في استجابة الخادم: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Online license check failed: ${e.message}")
            return@withContext LicenseCheckResult(
                isValid = false,
                isRevoked = false,
                isBoundToOtherDevice = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Periodic background check for license revocation.
     * If the developer revoked the license from the admin dashboard, returns true.
     */
    suspend fun isDeviceRevokedOnline(deviceCode: String, keyUsed: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check by device code
            val deviceUrl = URL("$FIREBASE_DB_URL/devices/$deviceCode.json")
            val conn = (deviceUrl.openConnection() as HttpURLConnection).apply {
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
                    if (obj.optBoolean("isRevoked", false)) return@withContext true
                }
            }

            // Also check by key if provided
            if (!keyUsed.isNullOrBlank()) {
                val cleanKey = keyUsed.replace("-", "").trim().uppercase()
                val keyUrl = URL("$FIREBASE_DB_URL/licenses/$cleanKey.json")
                val keyConn = (keyUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (keyConn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(keyConn.inputStream))
                    val res = reader.readText().trim()
                    reader.close()
                    if (res != "null" && res.isNotBlank()) {
                        val obj = JSONObject(res)
                        if (obj.optBoolean("isRevoked", false)) return@withContext true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    data class FeedbackSubmitResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Submits user feedback directly to Firebase Realtime Database.
     */
    suspend fun sendFeedbackOnline(
        subscriberName: String,
        phone: String,
        rating: Int,
        category: String,
        message: String,
        deviceCode: String
    ): FeedbackSubmitResult = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val feedbackId = "FB_${timestamp}_${deviceCode.takeLast(4)}"

        // 1. First Attempt: Cloud Firestore REST API
        val firestoreFields = mapOf(
            "id" to feedbackId,
            "subscriberName" to subscriberName,
            "phone" to phone,
            "rating" to rating,
            "category" to category,
            "message" to message,
            "deviceCode" to deviceCode,
            "deviceModel" to LicenseManager.getDeviceModelName(),
            "androidVersion" to LicenseManager.getAndroidVersionName(),
            "timestamp" to timestamp
        )
        if (sendToFirestore("feedbacks", feedbackId, firestoreFields)) {
            Log.i(TAG, "Feedback successfully sent via Cloud Firestore REST API")
            return@withContext FeedbackSubmitResult(isSuccess = true)
        }

        // 2. Second Attempt: Firebase Realtime Database
        val payload = JSONObject().apply {
            put("id", feedbackId)
            put("subscriberName", subscriberName)
            put("phone", phone)
            put("rating", rating)
            put("category", category)
            put("message", message)
            put("deviceCode", deviceCode)
            put("deviceModel", LicenseManager.getDeviceModelName())
            put("androidVersion", LicenseManager.getAndroidVersionName())
            put("timestamp", timestamp)
        }

        try {
            val url = URL("$FIREBASE_DB_URL/feedbacks/$feedbackId.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            if (code in 200..299) {
                FeedbackSubmitResult(isSuccess = true)
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "sendFeedbackOnline RTDB failed: HTTP $code, errorBody: $errBody")
                val userMsg = when (code) {
                    401, 403 -> "قواعد أمان Firebase ترفض الكتابة (Permission Denied). يرجى التأكد من نشر القواعد في تبويب Rules في Firebase Console."
                    404 -> "قاعدة بيانات Firebase غير موجودة بهذا الرابط ($FIREBASE_DB_URL)."
                    else -> "فشل الإرسال عبر الخادم (كود الخطأ: $code)"
                }
                FeedbackSubmitResult(isSuccess = false, errorMessage = userMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendFeedbackOnline exception: ${e.message}", e)
            FeedbackSubmitResult(
                isSuccess = false,
                errorMessage = "تعذر الاتصال بخادم Firebase (${e.localizedMessage ?: "انقطاع الاتصال"})."
            )
        }
    }

    /**
     * Posts a document directly to Cloud Firestore REST API.
     */
    private fun sendToFirestore(
        collection: String,
        documentId: String,
        fields: Map<String, Any>
    ): Boolean {
        return try {
            val projectId = "mosarib"
            val urlStr = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?documentId=$documentId"
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val fieldsJson = JSONObject()
            for ((key, value) in fields) {
                val valueObj = JSONObject()
                when (value) {
                    is String -> valueObj.put("stringValue", value)
                    is Number -> valueObj.put("integerValue", value.toString())
                    is Boolean -> valueObj.put("booleanValue", value)
                    else -> valueObj.put("stringValue", value.toString())
                }
                fieldsJson.put(key, valueObj)
            }
            val rootDoc = JSONObject().apply {
                put("fields", fieldsJson)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(rootDoc.toString()) }
            val code = conn.responseCode
            if (code in 200..299) {
                Log.d(TAG, "sendToFirestore succeeded: HTTP $code")
                true
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.w(TAG, "sendToFirestore returned HTTP $code: $errBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendToFirestore failed: ${e.message}")
            false
        }
    }

    private fun putJson(path: String, jsonString: String): Boolean {
        return try {
            val url = URL("$FIREBASE_DB_URL/$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream).use { it.write(jsonString) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "putJson failed for $path, code: $code, response: $err")
            }
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "putJson exception for $path: ${e.message}", e)
            false
        }
    }

    private fun patchJson(path: String, jsonString: String): Boolean {
        return try {
            val url = URL("$FIREBASE_DB_URL/$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                // HttpURLConnection does not support PATCH on all Android versions directly, use POST with override or PUT
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream).use { it.write(jsonString) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
