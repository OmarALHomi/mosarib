package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.core.license.LicenseManager

/**
 * Result of querying speech recognition support on Android 13+ (API 33).
 */
data class ArabicSupportResult(
    val installedOfflineLocale: String?,
    val bestOnlineLocale: String,
    val canDownloadOffline: Boolean,
    val installedLanguages: List<String> = emptyList(),
    val supportedLanguages: List<String> = emptyList(),
    val onlineLanguages: List<String> = emptyList(),
    val pendingLanguages: List<String> = emptyList()
)

/**
 * Helper utilities for Android speech recognition.
 * Manages recognizer intent configuration, locale checking, online/offline fallback,
 * model download triggers, and candidate resolution.
 */
object VoiceRecognitionHelper {

    private const val PREFS_NAME = "mosarib_voice_prefs"
    private const val KEY_SEEN_ONLINE_NOTICE = "has_seen_online_notice"

    private var cachedSupportResult: ArabicSupportResult? = null
    private var isSupportChecked: Boolean = false

    /**
     * Checks if speech recognition service is available on the device.
     */
    fun isSpeechRecognitionAvailable(context: Context): Boolean {
        val pm = context.packageManager
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val list = pm.queryIntentActivities(intent, 0)
        return list.isNotEmpty()
    }

    fun isRecognitionAvailable(context: Context): Boolean = isSpeechRecognitionAvailable(context)

    /**
     * Has the user seen the one-time notice about online recognition?
     */
    fun hasSeenOnlineNotice(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SEEN_ONLINE_NOTICE, false)
    }

    /**
     * Marks the one-time online notice as seen.
     */
    fun markSeenOnlineNotice(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_SEEN_ONLINE_NOTICE, true).apply()
    }

    /**
     * Checks whether an offline Arabic package is installed on the device (API 33+).
     * Chooses the best offline Arabic locale in order: ar-YE, ar-SA, ar-EG, ar.
     * Logs exact recognitionSupport lists and determines whether Arabic is downloadable.
     */
    fun checkArabicSupport(
        context: Context,
        onResult: (supportResult: ArabicSupportResult) -> Unit
    ) {
        val fallback = ArabicSupportResult(
            installedOfflineLocale = null,
            bestOnlineLocale = "ar-YE",
            canDownloadOffline = false
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(fallback)
            return
        }

        if (isSupportChecked && cachedSupportResult != null) {
            onResult(cachedSupportResult!!)
            return
        }

        val preferredLocales = listOf("ar-YE", "ar-SA", "ar-EG", "ar")
        var hasCalledBack = false

        val checkIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        val handler = Handler(Looper.getMainLooper())
        var recognizerRef: SpeechRecognizer? = null
        val timeoutRunnable = Runnable {
            if (!hasCalledBack) {
                hasCalledBack = true
                try { recognizerRef?.destroy() } catch (_: Exception) {}
                recognizerRef = null
                onResult(cachedSupportResult ?: fallback)
            }
        }
        handler.postDelayed(timeoutRunnable, 600)

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizerRef = recognizer
            if (recognizer == null) {
                handler.removeCallbacks(timeoutRunnable)
                if (!hasCalledBack) {
                    hasCalledBack = true
                    onResult(fallback)
                }
                return
            }

            val executor = ContextCompat.getMainExecutor(context)
            recognizer.checkRecognitionSupport(
                checkIntent,
                executor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        handler.removeCallbacks(timeoutRunnable)
                        if (hasCalledBack) return
                        hasCalledBack = true

                        Log.i("VoiceSupport", "installedOnDeviceLanguages: ${recognitionSupport.installedOnDeviceLanguages}")
                        Log.i("VoiceSupport", "supportedOnDeviceLanguages: ${recognitionSupport.supportedOnDeviceLanguages}")
                        Log.i("VoiceSupport", "onlineLanguages: ${recognitionSupport.onlineLanguages}")
                        Log.i("VoiceSupport", "pendingOnDeviceLanguages: ${recognitionSupport.pendingOnDeviceLanguages}")

                        val installed = recognitionSupport.installedOnDeviceLanguages.map {
                            it.replace('_', '-').lowercase()
                        }
                        val supported = recognitionSupport.supportedOnDeviceLanguages.map {
                            it.replace('_', '-').lowercase()
                        }
                        val online = recognitionSupport.onlineLanguages.map {
                            it.replace('_', '-').lowercase()
                        }

                        val foundOffline = preferredLocales.firstOrNull { pref ->
                            val pNorm = pref.lowercase()
                            installed.any { it == pNorm || it.startsWith(pNorm) }
                        }
                        val canDownload = preferredLocales.any { pref ->
                            val pNorm = pref.lowercase()
                            supported.any { it == pNorm || it.startsWith(pNorm) }
                        }
                        val foundOnline = preferredLocales.firstOrNull { pref ->
                            val pNorm = pref.lowercase()
                            online.any { it == pNorm || it.startsWith(pNorm) }
                        } ?: "ar-YE"

                        val res = ArabicSupportResult(
                            installedOfflineLocale = foundOffline,
                            bestOnlineLocale = foundOnline,
                            canDownloadOffline = canDownload,
                            installedLanguages = recognitionSupport.installedOnDeviceLanguages,
                            supportedLanguages = recognitionSupport.supportedOnDeviceLanguages,
                            onlineLanguages = recognitionSupport.onlineLanguages,
                            pendingLanguages = recognitionSupport.pendingOnDeviceLanguages
                        )

                        cachedSupportResult = res
                        isSupportChecked = true

                        try {
                            recognizer.destroy()
                        } catch (_: Exception) {}

                        onResult(res)
                    }

                    override fun onError(errorCode: Int) {
                        handler.removeCallbacks(timeoutRunnable)
                        if (hasCalledBack) return
                        hasCalledBack = true

                        Log.w("VoiceSupport", "checkRecognitionSupport onError code=$errorCode")
                        cachedSupportResult = fallback
                        isSupportChecked = true

                        try {
                            recognizer.destroy()
                        } catch (_: Exception) {}

                        onResult(fallback)
                    }
                }
            )
        } catch (e: Exception) {
            Log.w("VoiceSupport", "checkRecognitionSupport exception: ${e.message}")
            handler.removeCallbacks(timeoutRunnable)
            try { recognizerRef?.destroy() } catch (_: Exception) {}
            recognizerRef = null
            if (!hasCalledBack) {
                hasCalledBack = true
                onResult(fallback)
            }
        }
    }

    /**
     * Triggers background download of the Arabic recognition model on API 33+,
     * or opens system voice typing settings.
     */
    fun triggerLanguagePackDownload(context: Context) {
        isSupportChecked = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                if (recognizer != null) {
                    val downloadIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                    }
                    recognizer.triggerModelDownload(downloadIntent)
                    recognizer.destroy()
                    Toast.makeText(context, "جارٍ طلب تنزيل حزمة التعرف على العربية...", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (_: Exception) {}
        }
        openSpeechSettings(context)
    }

    /**
     * Opens system speech or Google voice typing settings.
     */
    fun openSpeechSettings(context: Context) {
        val intents = listOf(
            Intent("com.google.android.voicesearch.SPEECH_SETTINGS"),
            Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates standard speech recognizer intent configured with language,
     * offline preference, and up to 5 result candidates.
     */
    fun createSpeechIntent(
        prompt: String = "تحدث الآن...",
        language: String = "ar-YE",
        preferOffline: Boolean = true
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar")
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }

    fun buildSpeechIntent(
        context: Context? = null,
        prompt: String = "تحدث الآن...",
        language: String = "ar-YE",
        preferOffline: Boolean = true
    ): Intent = createSpeechIntent(prompt, language, preferOffline)

    /**
     * Main entry point to launch speech recognition.
     * Checks availability, queries offline pack support on API 33+, manages online/offline branches,
     * and shows proper MosaribDialogs without crashing or showing Google's red error screen.
     */
    fun launchRecognition(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        onShowDialog: (VoiceDialogType) -> Unit,
        prompt: String = "تحدث الآن..."
    ) {
        if (!isRecognitionAvailable(context)) {
            onShowDialog(VoiceDialogType.MISSING_ENGINE)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkArabicSupport(context) { supportResult ->
                if (supportResult.installedOfflineLocale != null) {
                    val intent = createSpeechIntent(
                        prompt = prompt,
                        language = supportResult.installedOfflineLocale,
                        preferOffline = true
                    )
                    try {
                        launcher.launch(intent)
                    } catch (_: Exception) {
                        onShowDialog(VoiceDialogType.MISSING_ENGINE)
                    }
                } else {
                    val isOnline = LicenseManager.isInternetAvailable(context)
                    if (isOnline) {
                        val intent = createSpeechIntent(
                            prompt = prompt,
                            language = supportResult.bestOnlineLocale,
                            preferOffline = false
                        )
                        try {
                            launcher.launch(intent)
                            if (!hasSeenOnlineNotice(context)) {
                                markSeenOnlineNotice(context)
                                if (supportResult.canDownloadOffline) {
                                    onShowDialog(VoiceDialogType.ONLINE_NOTICE)
                                } else {
                                    onShowDialog(VoiceDialogType.ONLINE_ONLY_NOTICE)
                                }
                            }
                        } catch (_: Exception) {
                            onShowDialog(VoiceDialogType.MISSING_ENGINE)
                        }
                    } else {
                        if (supportResult.canDownloadOffline) {
                            onShowDialog(VoiceDialogType.OFFLINE_PACK_MISSING)
                        } else {
                            onShowDialog(VoiceDialogType.OFFLINE_NOT_SUPPORTED)
                        }
                    }
                }
            }
        } else {
            // API < 33: try offline first
            val intent = createSpeechIntent(
                prompt = prompt,
                language = "ar-YE",
                preferOffline = true
            )
            try {
                launcher.launch(intent)
            } catch (_: Exception) {
                onShowDialog(VoiceDialogType.MISSING_ENGINE)
            }
        }
    }

    /**
     * Maps Android SpeechRecognizer error codes to user-friendly Arabic messages.
     */
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت، يرجى إعادة المحاولة"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطأ في الاتصال بالإنترنت"
            SpeechRecognizer.ERROR_AUDIO -> "تعذر تسجيل الصوت، تأكد من سلامة الميكروفون"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت المهلة دون سماع كلام"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "محرك الصوت مشغول حالياً"
            SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "خطأ في خادم التعرف على الصوت"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يتطلب إذن الميكروفون"
            12 /* agsa_transcription_LANGUAGE_PACK_ERROR */ -> "حزمة اللغة غير مثبتة"
            else -> "تعذر التعرف على الصوت"
        }
    }

    /**
     * Safe debug logger that does not throw in unit tests where android.util.Log is unmocked.
     */
    fun logDebug(tag: String, message: String) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(tag, message)
            }
        } catch (_: Throwable) {
            // Ignored in unit test environments
        }
    }

    /**
     * Evaluates speech recognition candidates in order and returns the first one that parses.
     * Logs candidate list in debug builds.
     */
    fun parseFirstValidCommand(
        candidates: List<String>?,
        knownNames: List<String> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): VoiceCommand? {
        if (candidates.isNullOrEmpty()) return null
        logDebug("VoiceInput", "candidates: $candidates")
        for (cand in candidates) {
            // A parser bug must never crash the app: skip the candidate and fall back to the dialogs.
            val cmd = try {
                VoiceCommandParser.parse(cand, knownNames, now)
            } catch (e: Exception) {
                logDebug("VoiceInput", "parse failed for '$cand': ${e.message}")
                continue
            }
            if (cmd.type != null) {
                return cmd
            }
        }
        val first = candidates.first()
        return try {
            VoiceCommandParser.parse(first, knownNames, now)
        } catch (e: Exception) {
            VoiceCommand(type = null, rawText = first)
        }
    }
}
