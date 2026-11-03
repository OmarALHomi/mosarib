package com.example.core.voice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entry representing a spoken phrase that was unrecognized, partially recognized,
 * or ended in the type chooser.
 */
data class UnparsedPhraseEntry(
    val text: String,
    val date: Long,
    val result: String
)

/**
 * Local-only storage for phrases that the voice system could not fully recognize.
 * Preserved on the phone only (in app_settings, never Firebase / network)
 * so the owner can view, copy, and share them to help grow recognition rules.
 */
object VoiceUnparsedLog {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_UNPARSED_LOG = "voice_unparsed_log"
    private const val MAX_LOG_SIZE = 50

    fun log(context: Context, text: String, result: String) {
        if (text.isBlank()) return
        try {
            val list = getEntries(context).toMutableList()
            // Avoid duplicate identical entry at the very top
            if (list.firstOrNull()?.text == text.trim()) return

            list.add(0, UnparsedPhraseEntry(text = text.trim(), date = System.currentTimeMillis(), result = result))
            val trimmed = list.take(MAX_LOG_SIZE)

            val array = JSONArray()
            for (entry in trimmed) {
                val obj = JSONObject().apply {
                    put("text", entry.text)
                    put("date", entry.date)
                    put("result", entry.result)
                }
                array.put(obj)
            }

            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(KEY_UNPARSED_LOG, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getEntries(context: Context): List<UnparsedPhraseEntry> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_UNPARSED_LOG, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<UnparsedPhraseEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UnparsedPhraseEntry(
                        text = obj.optString("text"),
                        date = obj.optLong("date"),
                        result = obj.optString("result")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().remove(KEY_UNPARSED_LOG).apply()
    }
}
