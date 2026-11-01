package com.example.core.network

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * The single REST client for the project's Firebase Realtime Database. Every read and write the
 * app makes goes through here, so timeouts, headers and error handling live in one place.
 *
 * Calls are blocking: invoke them from a background dispatcher.
 */
object FirebaseRest {
    private const val TAG = "FirebaseRest"
    const val DB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app"

    /**
     * Result of one request. [code] is the HTTP status, or [NETWORK_ERROR] when the request never
     * got a response. [body] is the response text (error text for failed requests), if any.
     */
    data class Response(val code: Int, val body: String?) {
        val isSuccess: Boolean get() = code in 200..299
        val isNetworkError: Boolean get() = code == NETWORK_ERROR

        /** The body as a JSON object, or null for a failed request, an empty node or non-objects. */
        fun jsonObject(): JSONObject? {
            if (!isSuccess) return null
            val text = body?.trim()
            if (text.isNullOrEmpty() || text == "null") return null
            return runCatching { JSONObject(text) }.getOrNull()
        }
    }

    const val NETWORK_ERROR = -1

    /** [path] is relative to the database root, e.g. "licenses/ABC.json". */
    fun get(path: String, timeoutMs: Int = 8000): Response = request(path, "GET", null, timeoutMs)

    /** Replaces the whole node at [path] with [json]. */
    fun put(path: String, json: String, timeoutMs: Int = 8000): Response =
        request(path, "PUT", json, timeoutMs)

    /** Writes only the children present in [json], keeping the rest of the node. */
    fun patch(path: String, json: String, timeoutMs: Int = 8000): Response =
        request(path, "PATCH", json, timeoutMs)

    private fun request(path: String, method: String, json: String?, timeoutMs: Int): Response {
        return try {
            val conn = (URL("$DB_URL/${path.trimStart('/')}").openConnection() as HttpURLConnection).apply {
                // HttpURLConnection has no PATCH; Firebase REST honours the override header.
                requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/json")
                if (json != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
            }
            try {
                if (json != null) {
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(json) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (code !in 200..299) Log.w(TAG, "$method $path failed: HTTP $code $body")
                Response(code, body)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "$method $path failed: ${e.message}")
            Response(NETWORK_ERROR, e.message)
        }
    }
}
