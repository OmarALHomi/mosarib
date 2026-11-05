package com.example.core.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small image loader for notification and announcement pictures (no image library in the app).
 * https only, 5 s timeout, at most 2 MB, scaled down to [maxSide] pixels. Returns null on any failure,
 * so callers fall back to text.
 */
object RemoteImageLoader {
    private const val MAX_BYTES = 2 * 1024 * 1024

    fun isAllowedUrl(url: String?): Boolean = !url.isNullOrBlank() && url.trim().startsWith("https://")

    suspend fun load(url: String, maxSide: Int = 1024, timeoutMs: Int = 5000): Bitmap? = withContext(Dispatchers.IO) {
        if (!isAllowedUrl(url)) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url.trim()).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val bytes = conn.inputStream.use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buffer.write(chunk, 0, n)
                    if (buffer.size() > MAX_BYTES) return@withContext null
                }
                buffer.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
