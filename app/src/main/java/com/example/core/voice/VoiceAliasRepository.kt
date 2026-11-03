package com.example.core.voice

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.features.settings.AppSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages learned voice aliases (e.g. "أحمد العولقي" -> customerId 12).
 * Persisted as JSON in `app_settings` under key "voice_aliases" without Room schema changes.
 */
object VoiceAliasRepository {

    const val KEY_VOICE_ALIASES = "voice_aliases"

    /**
     * Parses a simple JSON string {"alias": id, ...} into a Map<String, Long>.
     * Custom pure implementation to work reliably in Android runtime and JVM unit tests.
     */
    fun parseAliasesJson(json: String?): Map<String, Long> {
        if (json.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, Long>()
        val trimmed = json.trim().removePrefix("{").removeSuffix("}").trim()
        if (trimmed.isBlank()) return emptyMap()

        // Match "key": value entries
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*(\\d+)")
        for (m in regex.findAll(trimmed)) {
            val key = m.groupValues[1]
            val value = m.groupValues[2].toLongOrNull()
            if (value != null) {
                result[key] = value
            }
        }
        return result
    }

    /**
     * Serializes a Map<String, Long> into a compact JSON string.
     */
    fun serializeAliasesJson(map: Map<String, Long>): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.joinToString(",") { (k, v) ->
            val escapedKey = k.replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$escapedKey\":$v"
        }
        return "{$entries}"
    }

    /**
     * Retrieves all saved voice aliases from `app_settings`.
     */
    suspend fun getAliases(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val json = db.appSettingDao().getSettingValue(KEY_VOICE_ALIASES)
        parseAliasesJson(json)
    }

    /**
     * Saves a learned alias mapping normalized spoken text to the selected customer ID.
     */
    suspend fun saveAlias(context: Context, spokenText: String, customerId: Long) = withContext(Dispatchers.IO) {
        val normKey = SpokenNumberParser.normalizeToken(spokenText).trim()
        if (normKey.isBlank() || customerId <= 0) return@withContext

        val db = AppDatabase.getDatabase(context)
        val currentJson = db.appSettingDao().getSettingValue(KEY_VOICE_ALIASES)
        val currentMap = parseAliasesJson(currentJson).toMutableMap()
        currentMap[normKey] = customerId

        val newJson = serializeAliasesJson(currentMap)
        db.appSettingDao().saveSetting(AppSetting(KEY_VOICE_ALIASES, newJson))
    }
}
