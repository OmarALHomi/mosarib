package com.example.core.license

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
 * Universal REST client for Cloud Firestore (default database).
 * Fully independent of Google Play Services libraries and works directly via HTTPS REST API.
 */
object FirestoreRestHelper {
    private const val TAG = "FirestoreRestHelper"
    const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/mosarib/databases/(default)/documents"

    suspend fun getDocument(collection: String, documentId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val cleanDoc = documentId.trim().removePrefix("/").removeSuffix("/")
            val url = URL("$FIRESTORE_BASE_URL/$collection/$cleanDoc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (text.isNotBlank() && text != "null") {
                    val root = JSONObject(text)
                    return@withContext root.optJSONObject("fields")
                }
            } else if (conn.responseCode != 404) {
                Log.w(TAG, "GET $collection/$cleanDoc returned HTTP ${conn.responseCode}")
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching document $collection/$documentId: ${e.message}")
            null
        }
    }

    suspend fun patchDocument(collection: String, documentId: String, fields: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanDoc = documentId.trim().removePrefix("/").removeSuffix("/")
            val url = URL("$FIRESTORE_BASE_URL/$collection/$cleanDoc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val fieldsObj = JSONObject()
            for ((key, value) in fields) {
                if (value == null) continue
                val valObj = JSONObject()
                when (value) {
                    is Boolean -> valObj.put("booleanValue", value)
                    is Int -> valObj.put("integerValue", value.toString())
                    is Long -> valObj.put("integerValue", value.toString())
                    is Double -> valObj.put("doubleValue", value)
                    is Float -> valObj.put("doubleValue", value.toDouble())
                    else -> valObj.put("stringValue", value.toString())
                }
                fieldsObj.put(key, valObj)
            }

            val body = JSONObject().put("fields", fieldsObj).toString()
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Error patching document $collection/$documentId: ${e.message}")
            false
        }
    }

    fun getString(fields: JSONObject?, key: String, default: String = ""): String {
        return fields?.optJSONObject(key)?.optString("stringValue", default) ?: default
    }

    fun getInt(fields: JSONObject?, key: String, default: Int = 0): Int {
        val obj = fields?.optJSONObject(key) ?: return default
        val str = obj.optString("integerValue", "")
        return str.toIntOrNull() ?: obj.optInt("integerValue", default)
    }

    fun getLong(fields: JSONObject?, key: String, default: Long = 0L): Long {
        val obj = fields?.optJSONObject(key) ?: return default
        val str = obj.optString("integerValue", "")
        return str.toLongOrNull() ?: obj.optLong("integerValue", default)
    }

    fun getDouble(fields: JSONObject?, key: String, default: Double = 0.0): Double {
        val obj = fields?.optJSONObject(key) ?: return default
        if (obj.has("doubleValue")) return obj.optDouble("doubleValue", default)
        val str = obj.optString("integerValue", "")
        return str.toDoubleOrNull() ?: obj.optDouble("integerValue", default)
    }

    fun getBoolean(fields: JSONObject?, key: String, default: Boolean = false): Boolean {
        return fields?.optJSONObject(key)?.optBoolean("booleanValue", default) ?: default
    }
}
