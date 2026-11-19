package com.example.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SceneNarratorTts(
    context: Context,
    private val audioEngine: RpgAudioEngine? = null
) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile
    private var initialized: Boolean = false
    @Volatile
    private var released: Boolean = false
    private var pendingSpeak: Pair<String, String>? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingTurnId = MutableStateFlow<String?>(null)
    val speakingTurnId: StateFlow<String?> = _speakingTurnId.asStateFlow()

    private val _isAvailable = MutableStateFlow(true)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (released) return@TextToSpeech
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val ruLocale = Locale("ru", "RU")
                        val langResult = engine.setLanguage(ruLocale)
                        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            engine.setLanguage(Locale.getDefault())
                        }
                        engine.setSpeechRate(0.98f)
                        engine.setPitch(0.98f)
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                                audioEngine?.setTtsDucking(true)
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                                _speakingTurnId.value = null
                                audioEngine?.setTtsDucking(false)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                _speakingTurnId.value = null
                                audioEngine?.setTtsDucking(false)
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                                _speakingTurnId.value = null
                                audioEngine?.setTtsDucking(false)
                            }

                            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                                if (!interrupted) {
                                    _isSpeaking.value = false
                                    _speakingTurnId.value = null
                                    audioEngine?.setTtsDucking(false)
                                }
                            }
                        })
                        initialized = true
                        pendingSpeak?.let { (turnId, text) ->
                            pendingSpeak = null
                            speak(turnId, text)
                        }
                    }
                } else {
                    _isAvailable.value = false
                }
            }
        }.onFailure {
            _isAvailable.value = false
        }
    }

    fun toggleSpeak(turnId: String, narrativeText: String) {
        if (_isSpeaking.value && _speakingTurnId.value == turnId) {
            stop()
        } else {
            speak(turnId, narrativeText)
        }
    }

    fun speak(turnId: String, narrativeText: String) {
        if (released) return
        val cleanText = sanitizeForSpeech(narrativeText)
        if (cleanText.isBlank()) return

        if (!initialized) {
            pendingSpeak = turnId to cleanText
            return
        }

        val engine = tts ?: return
        _speakingTurnId.value = turnId
        _isSpeaking.value = true
        audioEngine?.setTtsDucking(true)

        val chunks = splitIntoTtsChunks(cleanText)
        chunks.forEachIndexed { idx, chunk ->
            val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val uttId = if (idx == chunks.lastIndex) "turn_$turnId" else "turn_${turnId}_$idx"
            runCatching {
                engine.speak(chunk, queueMode, null, uttId)
            }.onFailure {
                _isSpeaking.value = false
                _speakingTurnId.value = null
                audioEngine?.setTtsDucking(false)
            }
        }
    }

    fun stop() {
        pendingSpeak = null
        runCatching { tts?.stop() }
        _isSpeaking.value = false
        _speakingTurnId.value = null
        audioEngine?.setTtsDucking(false)
    }

    fun shutdown() {
        released = true
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    companion object {
        private const val MAX_CHUNK_CHARS = 1800

        internal fun sanitizeForSpeech(raw: String): String {
            return raw
                .replace(Regex("""\[[^\]]*]"""), " ")
                .replace(Regex("""[*_`#]+"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        internal fun splitIntoTtsChunks(text: String): List<String> {
            if (text.length <= MAX_CHUNK_CHARS) return listOf(text)
            val result = mutableListOf<String>()
            var start = 0
            while (start < text.length) {
                val end = (start + MAX_CHUNK_CHARS).coerceAtMost(text.length)
                if (end == text.length) {
                    result.add(text.substring(start).trim())
                    break
                }
                val cut = text.lastIndexOf('.', end)
                    .takeIf { it > start + 200 }
                    ?: text.lastIndexOf(' ', end).takeIf { it > start + 100 }
                    ?: end
                result.add(text.substring(start, (cut + 1).coerceAtMost(text.length)).trim())
                start = (cut + 1).coerceAtMost(text.length)
            }
            return result.filter { it.isNotBlank() }
        }
    }
}
