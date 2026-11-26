package com.example.core.updates

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.core.license.LicenseManager
import com.example.core.network.FirebaseRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppBroadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // INFO, WARNING, UPDATE
    val actionUrl: String = "",
    val actionText: String = "فتح الرابط",
    val minRequiredVersionCode: Int = 1,
    val isForceUpdate: Boolean = false,
    val publishedAt: Long = 0L,
    val requiredVersionName: String = "",
    val releaseNotes: List<String> = emptyList(),
    val imageUrl: String = "",
    // Optional audience: show the announcement only to builds in [min, max] (task 036)
    val targetMinVersionCode: Int? = null,
    val targetMaxVersionCode: Int? = null,
    // Builds that must update even when they are >= minRequiredVersionCode (e.g. a buggy build)
    val forceVersionCodes: List<Int> = emptyList()
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val PREFS_FILE = "mosarib_license_prefs"
    private const val KEY_LAST_SEEN_BROADCAST_ID = "last_seen_broadcast_id"
    private const val KEY_CACHED_MIN_VERSION_CODE = "cached_min_version_code"
    private const val KEY_CACHED_REQUIRED_VERSION_NAME = "cached_required_version_name"
    private const val KEY_CACHED_FORCE_ACTION_URL = "cached_force_action_url"
    private const val KEY_CACHED_FORCE_MESSAGE = "cached_force_message"
    private const val KEY_CACHED_FORCE_VERSION_CODES = "cached_force_version_codes"
    private const val KEY_DISMISSED_TODAY_PREFIX = "dismissed_broadcast_day_"
    private const val KEY_DISMISSED_EXPIRY_DAY = "dismissed_expiry_day"

    fun updateCachedForceUpdate(
        context: Context,
        minVersionCode: Int,
        requiredVersionName: String = "",
        actionUrl: String = "",
        message: String = "",
        forceVersionCodes: List<Int> = emptyList()
    ) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit()
            .putInt(KEY_CACHED_MIN_VERSION_CODE, minVersionCode)
            .putString(KEY_CACHED_FORCE_VERSION_CODES, forceVersionCodes.joinToString(","))
            .putString(KEY_CACHED_REQUIRED_VERSION_NAME, requiredVersionName)
            .putString(KEY_CACHED_FORCE_ACTION_URL, actionUrl)
            .putString(KEY_CACHED_FORCE_MESSAGE, message)
            .apply()
    }

    fun clearForceUpdateCache(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit()
            .remove(KEY_CACHED_MIN_VERSION_CODE)
            .remove(KEY_CACHED_REQUIRED_VERSION_NAME)
            .remove(KEY_CACHED_FORCE_ACTION_URL)
            .remove(KEY_CACHED_FORCE_MESSAGE)
            .remove(KEY_CACHED_FORCE_VERSION_CODES)
            .apply()
    }

    fun getCachedMinVersionCode(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CACHED_MIN_VERSION_CODE, 1)
    }

    fun getCachedForceVersionCodes(context: Context): List<Int> {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return parseVersionCodes(sp.getString(KEY_CACHED_FORCE_VERSION_CODES, "") ?: "")
    }

    /** "4, 5" or "4,5" -> [4, 5]; anything that is not a positive number is ignored. */
    fun parseVersionCodes(raw: String): List<Int> =
        raw.split(Regex("[,\\s]+")).mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct()

    /** Pure check: true only when broadcast is an update prompt or forced update. */
    fun isUpdateBroadcast(type: String, isForceUpdate: Boolean): Boolean =
        isForceUpdate || type.trim().equals("UPDATE", ignoreCase = true)

    /** Non-update announcements never inherit stale minRequiredVersionCode > 1. */
    internal fun resolveEffectiveMinVersion(
        type: String,
        isForceUpdate: Boolean,
        rawMinVersion: Int
    ): Int = if (isUpdateBroadcast(type, isForceUpdate)) rawMinVersion else 1

    /**
     * Pure decision function for force update.
     * Force when currentCode < minCode, or when currentCode is one of [forceVersionCodes].
     */
    fun shouldForceUpdate(
        currentCode: Int,
        minCode: Int,
        isForceUpdateFlag: Boolean = false,
        forceVersionCodes: List<Int> = emptyList()
    ): Boolean {
        return currentCode < minCode || currentCode in forceVersionCodes
    }

    /** Pure audience check for announcements: null bounds are open. */
    fun isInTargetRange(currentCode: Int, minCode: Int?, maxCode: Int?): Boolean {
        if (minCode != null && minCode > 0 && currentCode < minCode) return false
        if (maxCode != null && maxCode > 0 && currentCode > maxCode) return false
        return true
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    suspend fun checkBroadcastAndUpdates(context: Context): AppBroadcast? = withContext(Dispatchers.IO) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        if (!LicenseManager.isInternetAvailable(context)) {
            // Offline fallback: use cached minimum version code so an old version
            // cannot skip a force update by turning off the internet.
            val cachedMinVersion = getCachedMinVersionCode(context)
            if (shouldForceUpdate(BuildConfig.VERSION_CODE, cachedMinVersion, forceVersionCodes = getCachedForceVersionCodes(context))) {
                val cachedReqName = sp.getString(KEY_CACHED_REQUIRED_VERSION_NAME, "") ?: ""
                val cachedActionUrl = sp.getString(KEY_CACHED_FORCE_ACTION_URL, "") ?: ""
                val cachedMessage = sp.getString(KEY_CACHED_FORCE_MESSAGE, "") ?: ""
                return@withContext AppBroadcast(
                    id = "cached_force_update",
                    title = "تحديث إجباري مطلوب",
                    message = cachedMessage.ifBlank { "يجب تحديث التطبيق إلى الإصدار الأحدث لمتابعة الاستخدام." },
                    type = "UPDATE",
                    actionUrl = cachedActionUrl,
                    actionText = "تحميل التحديث",
                    minRequiredVersionCode = cachedMinVersion,
                    isForceUpdate = true,
                    requiredVersionName = cachedReqName
                )
            }
            return@withContext null
        }

        try {
            // Managed by the web dashboard (/app_broadcast)
            val resp = FirebaseRest.get("app_broadcast.json", timeoutMs = 4000)
            // A server error must not skip a cached force update: fall back like a network error
            if (!resp.isSuccess) throw java.io.IOException("HTTP ${resp.code}")
            if (resp.isSuccess) {
                val text = resp.body?.trim()
                val obj = resp.jsonObject()
                if (text.isNullOrEmpty() || text == "null" || obj == null || obj.length() == 0) {
                    // When /app_broadcast is empty or removed, clear the cache so offline users aren't locked
                    clearForceUpdateCache(context)
                    return@withContext null
                }

                val id = obj.optString("id", "")
                val rawType = obj.optString("type", "INFO").trim().uppercase()
                val isForceUpdateFlag = obj.optBoolean("isForceUpdate", false)
                val isUpdateType = isUpdateBroadcast(rawType, isForceUpdateFlag)
                val minVersion = resolveEffectiveMinVersion(
                    rawType,
                    isForceUpdateFlag,
                    obj.optInt("minRequiredVersionCode", 1)
                )
                val reqVersionName = obj.optString("requiredVersionName", obj.optString("versionName", ""))
                val actionUrl = obj.optString("actionUrl", "")
                val message = obj.optString("message", "")
                val forceCodes = mutableListOf<Int>()
                if (isUpdateType) {
                    obj.optJSONArray("forceVersionCodes")?.let { arr ->
                        for (i in 0 until arr.length()) arr.optInt(i, 0).takeIf { it > 0 }?.let { forceCodes.add(it) }
                    } ?: forceCodes.addAll(parseVersionCodes(obj.optString("forceVersionCodes", "")))
                }
                val imageUrl = obj.optString("imageUrl", "").takeIf { it.startsWith("https://") }.orEmpty()
                val targetMin = obj.optInt("targetMinVersionCode", 0).takeIf { it > 0 }
                val targetMax = obj.optInt("targetMaxVersionCode", 0).takeIf { it > 0 }

                // Write the effective minVersion (1 for non-update announcements so stale values never lock offline)
                updateCachedForceUpdate(context, minVersion, reqVersionName, actionUrl, message, forceCodes)

                // Parse optional release notes
                val notesList = mutableListOf<String>()
                val notesArray = obj.optJSONArray("releaseNotes")
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val item = notesArray.optString(i, "").trim()
                        if (item.isNotEmpty()) notesList.add(item)
                    }
                } else {
                    val notesRaw = obj.optString("releaseNotes", obj.optString("notes", "")).trim()
                    if (notesRaw.isNotEmpty()) {
                        notesList.addAll(notesRaw.split("\n").map { it.trim().removePrefix("•").removePrefix("-").trim() }.filter { it.isNotEmpty() })
                    }
                }

                val requiresForceUpdate = shouldForceUpdate(BuildConfig.VERSION_CODE, minVersion, isForceUpdateFlag, forceCodes)

                if (requiresForceUpdate) {
                    return@withContext AppBroadcast(
                        id = id,
                        title = obj.optString("title", "تحديث إجباري مطلوب"),
                        message = message,
                        type = "UPDATE",
                        actionUrl = actionUrl,
                        actionText = obj.optString("actionText", "تحميل التحديث").let {
                            if (it == "null" || it.isBlank()) "تحميل التحديث" else it
                        },
                        minRequiredVersionCode = minVersion,
                        isForceUpdate = true,
                        publishedAt = obj.optLong("publishedAt", 0L),
                        requiredVersionName = reqVersionName,
                        releaseNotes = notesList,
                        imageUrl = imageUrl,
                        forceVersionCodes = forceCodes
                    )
                }

                // An announcement aimed at other builds is not shown here
                if (!isInTargetRange(BuildConfig.VERSION_CODE, targetMin, targetMax)) {
                    return@withContext null
                }

                // When the flag is true and the app is already at or above the minimum,
                // show nothing for that broadcast (no announcement either).
                if (isForceUpdateFlag) {
                    return@withContext null
                }

                // Normal announcement / optional update
                return@withContext AppBroadcast(
                    id = id,
                    title = obj.optString("title", "تنبيه جديد"),
                    message = message,
                    type = obj.optString("type", "INFO"),
                    actionUrl = actionUrl,
                    actionText = obj.optString("actionText", "").let {
                        if (it == "null") "" else it.trim()
                    },
                    minRequiredVersionCode = minVersion,
                    isForceUpdate = false,
                    publishedAt = obj.optLong("publishedAt", 0L),
                    requiredVersionName = reqVersionName,
                    releaseNotes = notesList,
                    imageUrl = imageUrl,
                    targetMinVersionCode = targetMin,
                    targetMaxVersionCode = targetMax
                )
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch updates/broadcasts: ${e.message}")
            // Fallback to cached force update if network error occurs
            val cachedMin = getCachedMinVersionCode(context)
            if (shouldForceUpdate(BuildConfig.VERSION_CODE, cachedMin, forceVersionCodes = getCachedForceVersionCodes(context))) {
                val cachedReqName = sp.getString(KEY_CACHED_REQUIRED_VERSION_NAME, "") ?: ""
                val cachedActionUrl = sp.getString(KEY_CACHED_FORCE_ACTION_URL, "") ?: ""
                val cachedMessage = sp.getString(KEY_CACHED_FORCE_MESSAGE, "") ?: ""
                return@withContext AppBroadcast(
                    id = "cached_force_update",
                    title = "تحديث إجباري مطلوب",
                    message = cachedMessage.ifBlank { "يجب تحديث التطبيق إلى الإصدار الأحدث لمتابعة الاستخدام." },
                    type = "UPDATE",
                    actionUrl = cachedActionUrl,
                    actionText = "تحميل التحديث",
                    minRequiredVersionCode = cachedMin,
                    isForceUpdate = true,
                    requiredVersionName = cachedReqName
                )
            }
            null
        }
    }

    private const val KEY_LAST_NOTIFIED_BROADCAST_ID = "last_notified_broadcast_id"

    fun isBroadcastSeen(context: Context, broadcastId: String): Boolean {
        if (broadcastId.isBlank()) return true
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.getString(KEY_LAST_SEEN_BROADCAST_ID, "") == broadcastId) return true
        val dismissedToday = sp.getString(KEY_DISMISSED_TODAY_PREFIX + broadcastId, "")
        return dismissedToday == getTodayDateString()
    }

    fun markBroadcastSeen(context: Context, broadcastId: String) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LAST_SEEN_BROADCAST_ID, broadcastId).apply()
    }

    fun dismissBroadcastForToday(context: Context, broadcastId: String) {
        if (broadcastId.isBlank()) return
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_DISMISSED_TODAY_PREFIX + broadcastId, getTodayDateString()).apply()
    }

    fun isExpiryNoticeDismissedToday(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_DISMISSED_EXPIRY_DAY, "") == getTodayDateString()
    }

    fun dismissExpiryNoticeForToday(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_DISMISSED_EXPIRY_DAY, getTodayDateString()).apply()
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
