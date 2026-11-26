package com.example.core.speech

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class SpeechStatus {
    Ready,
    MissingVoiceData,
    LanguageNotSupported,
    NoEngine,
    DisabledByPolicy
}

data class SpeechError(
    val utteranceId: String? = null,
    val errorCode: Int? = null,
    val message: String = "تعذّر تشغيل الصوت"
)

/**
 * App-scoped singleton wrapper around Android's [TextToSpeech].
 * Lazy-initialized on first use, supports spoken playback and file synthesis.
 * Discovers and binds strictly to an installed OFFLINE Arabic voice (prioritizing Google TTS).
 */
object MosaribSpeaker {

    private const val PREFS_NAME = "mosarib_speech_prefs"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_CHOSEN_ENGINE = "chosen_tts_engine"
    private const val KEY_CHOSEN_VOICE = "chosen_tts_voice"
    const val DEFAULT_SPEECH_RATE = 0.9f
    private const val KEY_CONNECTED_READING = "connected_reading"
    const val DEFAULT_PREVIEW_TEXT = "مَرْحَبًا، هَذَا صَوْت تَطْبِيق المُسَرِّب. رَصِيد أحمد عَلَيْه خمسة آلاف وخمس مِئَة رِيَال."

    private var _connectedReading: Boolean = true

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initMutex = Mutex()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeUtteranceId = MutableStateFlow<String?>(null)
    val activeUtteranceId: StateFlow<String?> = _activeUtteranceId.asStateFlow()

    private val _speechRate = MutableStateFlow(DEFAULT_SPEECH_RATE)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _lastStatus = MutableStateFlow<SpeechStatus?>(null)
    val lastStatus: StateFlow<SpeechStatus?> = _lastStatus.asStateFlow()

    private val _currentEngineLabel = MutableStateFlow<String?>("Google")
    val currentEngineLabel: StateFlow<String?> = _currentEngineLabel.asStateFlow()
    val currentEngineDisplayName: String get() = _currentEngineLabel.value ?: "Google"

    private val _errors = MutableSharedFlow<SpeechError>(extraBufferCapacity = 8)
    val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()

    private val fileUtteranceCallbacks = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    fun initSettings(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rate = sp.getFloat(KEY_SPEECH_RATE, DEFAULT_SPEECH_RATE).coerceIn(0.7f, 1.3f)
        _speechRate.value = rate
        _connectedReading = sp.getBoolean(KEY_CONNECTED_READING, true)
        val savedEngine = sp.getString(KEY_CHOSEN_ENGINE, null)
        if (savedEngine != null) {
            _currentEngineLabel.value = formatEngineName(savedEngine)
        }
    }

    fun isConnectedReadingEnabled(context: Context? = null): Boolean {
        if (context != null) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _connectedReading = sp.getBoolean(KEY_CONNECTED_READING, true)
        }
        return _connectedReading
    }

    fun setConnectedReadingEnabled(context: Context, enabled: Boolean) {
        _connectedReading = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONNECTED_READING, enabled)
            .apply()
    }

    fun getSpeechRate(context: Context): Float {
        initSettings(context)
        return speechRate.value
    }

    fun openTtsDownload(context: Context) = openInstallTtsData(context)
    fun openPlayStoreForGoogleTts(context: Context) = openGoogleTtsPlayStore(context)

    fun setSpeechRate(context: Context, rate: Float) {
        val clamped = rate.coerceIn(0.7f, 1.3f)
        _speechRate.value = clamped
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_SPEECH_RATE, clamped)
            .apply()
        tts?.setSpeechRate(clamped)
    }

    private fun formatEngineName(packageName: String?): String {
        if (packageName == null) return "Google"
        return when {
            packageName.contains("google", ignoreCase = true) -> "Google"
            packageName.contains("samsung", ignoreCase = true) -> "Samsung"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    fun getInstalledOfflineArabicVoices(context: Context): List<Voice> {
        val currentTts = tts ?: return emptyList()
        val allVoices = try { currentTts.voices } catch (_: Exception) { null } ?: return emptyList()
        val offline = allVoices.filter { v ->
            v.locale?.language == "ar" &&
            !v.isNetworkConnectionRequired &&
            (v.features == null || !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        }.sortedBy { it.name }

        return if (offline.size > 1) {
            offline.filterNot { it.name.equals("ar-language", ignoreCase = true) }
        } else {
            offline
        }
    }

    fun getChosenVoiceName(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_CHOSEN_VOICE, null) ?: tts?.voice?.name
    }

    fun setChosenVoice(context: Context, voiceName: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_CHOSEN_VOICE, voiceName).apply()
        val currentTts = tts ?: return
        val voiceObj = try { currentTts.voices?.firstOrNull { it.name == voiceName } } catch (_: Exception) { null }
        if (voiceObj != null) {
            try { currentTts.voice = voiceObj } catch (_: Exception) {}
        }
    }

    suspend fun previewVoice(context: Context, voice: Voice, sampleText: String = DEFAULT_PREVIEW_TEXT) {
        prepare(context)
        val currentTts = tts ?: return
        try {
            currentTts.voice = voice
        } catch (_: Exception) {}
        speak(sampleText)
    }

    /**
     * Initializes candidate engine and verifies that an OFFLINE Arabic voice is actually installed.
     * Prevents network-only voices (e.g. ar-xa-x-arz-server) from failing with error code -4.
     */
    private suspend fun testCandidateEngine(
        context: Context,
        enginePackage: String?,
        preferredVoiceName: String? = null
    ): Triple<TextToSpeech?, SpeechStatus, String?> {
        val initDeferred = CompletableDeferred<Int>()
        val candidate = try {
            if (enginePackage != null) {
                TextToSpeech(context.applicationContext, { status -> initDeferred.complete(status) }, enginePackage)
            } else {
                TextToSpeech(context.applicationContext) { status -> initDeferred.complete(status) }
            }
        } catch (_: Exception) {
            return Triple(null, SpeechStatus.NoEngine, null)
        }

        val result = try {
            withTimeoutOrNull(4000L) { initDeferred.await() }
        } catch (e: CancellationException) {
            try { candidate.shutdown() } catch (_: Exception) {}
            throw e
        } catch (_: Exception) {
            null
        }

        if (result != TextToSpeech.SUCCESS) {
            try { candidate.shutdown() } catch (_: Exception) {}
            return Triple(null, SpeechStatus.NoEngine, null)
        }

        val arabicLocale = Locale("ar")
        val langResult = try {
            candidate.isLanguageAvailable(arabicLocale)
        } catch (_: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return Triple(candidate, SpeechStatus.LanguageNotSupported, null)
        }

        val setLang = try { candidate.setLanguage(arabicLocale) } catch (_: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
        if (setLang == TextToSpeech.LANG_NOT_SUPPORTED) {
            return Triple(candidate, SpeechStatus.LanguageNotSupported, null)
        }

        // Verify tts.voices to guarantee an OFFLINE installed voice
        val allVoices = try { candidate.voices } catch (_: Exception) { null }
        if (!allVoices.isNullOrEmpty()) {
            val rawOfflineVoices = allVoices.filter { v ->
                v.locale?.language == "ar" &&
                !v.isNetworkConnectionRequired &&
                (v.features == null || !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
            }
            val installedOfflineArabicVoices = if (rawOfflineVoices.size > 1) {
                rawOfflineVoices.filterNot { it.name.equals("ar-language", ignoreCase = true) }
            } else {
                rawOfflineVoices
            }

            val chosenVoice = if (preferredVoiceName != null) {
                installedOfflineArabicVoices.firstOrNull { it.name == preferredVoiceName }
                    ?: installedOfflineArabicVoices.firstOrNull()
            } else {
                installedOfflineArabicVoices.firstOrNull()
            }

            if (chosenVoice != null) {
                try {
                    candidate.voice = chosenVoice
                } catch (_: Exception) {}
                return Triple(candidate, SpeechStatus.Ready, chosenVoice.name)
            } else {
                // Arabic is nominally supported but offline voice files are not installed
                return Triple(candidate, SpeechStatus.MissingVoiceData, null)
            }
        } else {
            // Engine does not implement Voices API (legacy/fallback)
            return if (langResult == TextToSpeech.LANG_MISSING_DATA || setLang == TextToSpeech.LANG_MISSING_DATA) {
                Triple(candidate, SpeechStatus.MissingVoiceData, null)
            } else {
                Triple(candidate, SpeechStatus.Ready, null)
            }
        }
    }

    private fun attachActiveEngine(activeTts: TextToSpeech, enginePackage: String?) {
        tts = activeTts
        isInitialized = true
        _currentEngineLabel.value = formatEngineName(enginePackage ?: activeTts.defaultEngine)

        activeTts.setSpeechRate(_speechRate.value)
        activeTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith("synth_")) {
                    _isSpeaking.value = true
                    _activeUtteranceId.value = utteranceId
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith("synth_")) {
                    if (_activeUtteranceId.value == utteranceId || _activeUtteranceId.value == null) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }
                }
                if (utteranceId != null) {
                    fileUtteranceCallbacks.remove(utteranceId)?.complete(Unit)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == null || !utteranceId.startsWith("synth_")) {
                    if (_activeUtteranceId.value == utteranceId || _activeUtteranceId.value == null) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }
                    _errors.tryEmit(SpeechError(utteranceId = utteranceId, message = "تعذّر تشغيل الصوت"))
                }
                if (utteranceId != null) {
                    fileUtteranceCallbacks.remove(utteranceId)?.completeExceptionally(
                        RuntimeException("TTS synthesis error for $utteranceId")
                    )
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == null || !utteranceId.startsWith("synth_")) {
                    if (_activeUtteranceId.value == utteranceId || _activeUtteranceId.value == null) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }
                    _errors.tryEmit(SpeechError(utteranceId = utteranceId, errorCode = errorCode, message = "تعذّر تشغيل الصوت (رمز الخطأ: $errorCode)"))
                }
                if (utteranceId != null) {
                    fileUtteranceCallbacks.remove(utteranceId)?.completeExceptionally(
                        RuntimeException("TTS synthesis error code $errorCode for $utteranceId")
                    )
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (utteranceId == null || !utteranceId.startsWith("synth_")) {
                    if (_activeUtteranceId.value == utteranceId || _activeUtteranceId.value == null) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }
                }
                if (utteranceId != null) {
                    fileUtteranceCallbacks.remove(utteranceId)?.cancel()
                }
            }
        })
    }

    /**
     * Lazily prepares the TTS engine, ensuring an offline voice is ready.
     * Searches installed engines (preferring Google TTS) if default engine lacks Arabic.
     */
    suspend fun prepare(context: Context): SpeechStatus = withContext(Dispatchers.Main) {
        if (!com.example.core.license.RemoteConfigManager.isVoiceReadoutAllowed(context)) {
            _lastStatus.value = SpeechStatus.DisabledByPolicy
            return@withContext SpeechStatus.DisabledByPolicy
        }
        initMutex.withLock {
            initSettings(context)
            cleanupOldVoiceFiles(context)

            if (tts != null && isInitialized && _lastStatus.value == SpeechStatus.Ready) {
                // Verify voice is still offline and valid
                val currentVoice = tts?.voice
                if (currentVoice == null || (!currentVoice.isNetworkConnectionRequired && (currentVoice.features == null || !currentVoice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)))) {
                    return@withLock SpeechStatus.Ready
                }
            }

            // Cleanup any existing instance before re-initializing
            if (tts != null) {
                try { tts?.shutdown() } catch (_: Exception) {}
                tts = null
                isInitialized = false
            }

            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedEngine = sp.getString(KEY_CHOSEN_ENGINE, null)
            val savedVoice = sp.getString(KEY_CHOSEN_VOICE, null)

            // 1. Try saved engine first if present
            if (savedEngine != null) {
                val (candSaved, statusSaved, voiceSaved) = testCandidateEngine(context, savedEngine, savedVoice)
                if (statusSaved == SpeechStatus.Ready && candSaved != null) {
                    if (voiceSaved != null) {
                        sp.edit().putString(KEY_CHOSEN_VOICE, voiceSaved).apply()
                    }
                    attachActiveEngine(candSaved, savedEngine)
                    _lastStatus.value = SpeechStatus.Ready
                    return@withLock SpeechStatus.Ready
                } else {
                    try { candSaved?.shutdown() } catch (_: Exception) {}
                }
            }

            // 2. Try default system engine
            val (candDefault, statusDefault, voiceDefault) = testCandidateEngine(context, null)
            val defaultEngineName = candDefault?.defaultEngine
            val availableEngines = candDefault?.engines ?: emptyList()

            if (statusDefault == SpeechStatus.Ready && candDefault != null) {
                sp.edit().putString(KEY_CHOSEN_ENGINE, defaultEngineName).apply()
                if (voiceDefault != null) {
                    sp.edit().putString(KEY_CHOSEN_VOICE, voiceDefault).apply()
                }
                attachActiveEngine(candDefault, defaultEngineName)
                _lastStatus.value = SpeechStatus.Ready
                return@withLock SpeechStatus.Ready
            }

            try { candDefault?.shutdown() } catch (_: Exception) {}

            // 3. Go through available engines, prioritizing com.google.android.tts first
            val sortedEngines = availableEngines
                .filter { it.name != defaultEngineName }
                .sortedWith(compareByDescending { it.name == "com.google.android.tts" })

            var bestStatus = statusDefault
            var bestEngineCandidate: TextToSpeech? = null
            var bestEngineName: String? = null
            var bestVoiceName: String? = null

            for (engine in sortedEngines) {
                val (cand, status, voiceName) = testCandidateEngine(context, engine.name)
                if (status == SpeechStatus.Ready && cand != null) {
                    bestEngineCandidate?.let { try { it.shutdown() } catch (_: Exception) {} }
                    sp.edit().putString(KEY_CHOSEN_ENGINE, engine.name).apply()
                    if (voiceName != null) {
                        sp.edit().putString(KEY_CHOSEN_VOICE, voiceName).apply()
                    }
                    attachActiveEngine(cand, engine.name)
                    _lastStatus.value = SpeechStatus.Ready
                    return@withLock SpeechStatus.Ready
                } else {
                    if (status == SpeechStatus.MissingVoiceData && bestStatus != SpeechStatus.MissingVoiceData) {
                        bestStatus = SpeechStatus.MissingVoiceData
                        bestEngineCandidate?.let { try { it.shutdown() } catch (_: Exception) {} }
                        bestEngineCandidate = cand
                        bestEngineName = engine.name
                        bestVoiceName = voiceName
                    } else {
                        try { cand?.shutdown() } catch (_: Exception) {}
                    }
                }
            }

            if (bestEngineCandidate != null) {
                sp.edit().putString(KEY_CHOSEN_ENGINE, bestEngineName).apply()
                if (bestVoiceName != null) {
                    sp.edit().putString(KEY_CHOSEN_VOICE, bestVoiceName).apply()
                }
                attachActiveEngine(bestEngineCandidate, bestEngineName)
            }

            _lastStatus.value = bestStatus
            bestStatus
        }
    }

    /**
     * Speaks the given Arabic text aloud. Automatically pre-processes with [SpeechPronunciation.forSpeech].
     * Publishes SpeechError on failure.
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()): Boolean {
        if (_lastStatus.value == SpeechStatus.DisabledByPolicy) {
            _errors.tryEmit(
                SpeechError(
                    utteranceId = utteranceId,
                    message = com.example.core.license.RemoteConfigManager.FEATURE_DISABLED_MESSAGE
                )
            )
            return false
        }
        val currentTts = tts
        if (currentTts == null || !isInitialized) {
            _errors.tryEmit(SpeechError(utteranceId = utteranceId, message = "محرك الصوت غير جاهز"))
            return false
        }
        val vocalized = SpeechPronunciation.forSpeech(text, connected = _connectedReading)
        return try {
            currentTts.setSpeechRate(_speechRate.value)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val res = currentTts.speak(vocalized, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (res != TextToSpeech.SUCCESS) {
                _isSpeaking.value = false
                _activeUtteranceId.value = null
                _errors.tryEmit(SpeechError(utteranceId = utteranceId, errorCode = res, message = "تعذّر بدء القراءة الصوتية"))
                false
            } else {
                _activeUtteranceId.value = utteranceId
                true
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
            _activeUtteranceId.value = null
            _errors.tryEmit(SpeechError(utteranceId = utteranceId, message = e.message ?: "تعذّر تشغيل الصوت"))
            false
        }
    }

    /**
     * Stops current playback immediately.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _activeUtteranceId.value = null
    }

    /**
     * Synthesizes the given text directly to a WAV file.
     * Automatically pre-processes with [SpeechPronunciation.forSpeech].
     */
    suspend fun synthesizeToFile(text: String, file: File): Result<File> = withContext(Dispatchers.IO) {
        val currentTts = tts ?: return@withContext Result.failure(IllegalStateException("TTS not initialized"))
        val utteranceId = "synth_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Unit>()
        fileUtteranceCallbacks[utteranceId] = deferred

        file.parentFile?.mkdirs()
        if (file.exists()) {
            file.delete()
        }

        val vocalized = SpeechPronunciation.forSpeech(text, connected = _connectedReading)

        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val queueResult = currentTts.synthesizeToFile(vocalized, params, file, utteranceId)
            if (queueResult != TextToSpeech.SUCCESS) {
                fileUtteranceCallbacks.remove(utteranceId)
                if (file.exists() && file.length() == 0L) file.delete()
                return@withContext Result.failure(RuntimeException("Failed to queue synthesizeToFile"))
            }

            deferred.await()
            if (file.exists() && file.length() > 0) {
                Result.success(file)
            } else {
                if (file.exists() && file.length() == 0L) file.delete()
                Result.failure(RuntimeException("Synthesized file is empty or missing"))
            }
        } catch (e: CancellationException) {
            fileUtteranceCallbacks.remove(utteranceId)
            if (file.exists() && file.length() == 0L) file.delete()
            throw e
        } catch (e: Exception) {
            fileUtteranceCallbacks.remove(utteranceId)
            if (file.exists() && file.length() == 0L) file.delete()
            Result.failure(e)
        }
    }

    /**
     * Launches the system intent to download Arabic voice data.
     * Targets Google TTS directly if installed to prevent engine chooser popup.
     */
    fun openInstallTtsData(context: Context) {
        val isGoogleInstalled = try {
            context.packageManager.getPackageInfo("com.google.android.tts", 0)
            true
        } catch (_: Exception) {
            false
        }

        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                if (isGoogleInstalled) {
                    setPackage("com.google.android.tts")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                try {
                    val settingsIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (_: Exception) {
                    try {
                        val generalIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(generalIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Opens Google Speech Services on Google Play Store.
     */
    fun openGoogleTtsPlayStore(context: Context) {
        try {
            val uri = Uri.parse("market://details?id=com.google.android.tts")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            val uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Cleans up voice statement files older than 24 hours.
     */
    fun cleanupOldVoiceFiles(context: Context) {
        try {
            val voiceDir = File(context.cacheDir, "voice")
            if (voiceDir.exists() && voiceDir.isDirectory) {
                val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                voiceDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < oneDayAgo || file.length() == 0L) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
