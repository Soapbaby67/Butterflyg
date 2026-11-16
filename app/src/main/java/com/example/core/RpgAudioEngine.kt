package com.example.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.EmotionCategory
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.UniverseArchetype
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.resolveUniverseArchetype
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SfxEvent {
    UI_TAP,
    SWIPE_WHOOSH,
    SEND_ACTION,
    AI_REVEAL,
    EMOTION_SHIFT,
    ITEM_USE,
    BUTTERFLY_SHIFT,
    ROULETTE_TICK,
    OUTCOME_TRIUMPH,
    OUTCOME_MIXED,
    OUTCOME_SETBACK,
    OUTCOME_TWIST
}

val LocalRpgAudioEngine = staticCompositionLocalOf<RpgAudioEngine?> { null }

private data class ActiveTone(
    var freq: Double,
    var endFreq: Double,
    var phase: Double,
    var elapsedSec: Double,
    val durationSec: Double,
    val attackSec: Double,
    val gain: Double,
    val waveform: Int // 0 = warm sine+2nd, 1 = soft triangle/wood, 2 = airy noise-swept chime
)

private data class MusicalScaleProfile(
    val titleRu: String,
    val rootHz: Double,
    val chordSemitones: List<List<Int>>,
    val melodyOffsets: List<Int>,
    val beatSeconds: Double,
    val padWarmth: Double
)

fun describeActiveSoundscape(context: DynamicThemeContext): String {
    val musicLabel = when (context.location) {
        AppVisualLocation.AUTH -> "Увертюра Хроник"
        AppVisualLocation.SHELF -> "Библиотечный ноктюрн"
        AppVisualLocation.WIZARD -> "Плетение судьбы"
        AppVisualLocation.PRESENTATION -> "Тематический амбиент"
        AppVisualLocation.GAME -> when (resolveUniverseArchetype(context.universe)) {
            UniverseArchetype.COZY_ROMANCE -> "Тёплое эл. пианино"
            UniverseArchetype.CYBERPUNK_SCIFI -> "Аналоговый неон-синт"
            UniverseArchetype.SPACE_SCIFI -> "Космический дрон"
            UniverseArchetype.WITCHER_FANTASY -> "Лютень и струны"
            UniverseArchetype.SHINOBI_ANIME -> "Кото и флейта"
            UniverseArchetype.TITAN_SURVIVAL -> "Тревожный оркестр"
            UniverseArchetype.ARCANE_MYSTERY -> "Мистическая челеста"
            UniverseArchetype.GENERIC_CHRONICLE -> "Книжный саундтрек"
        }
    }
    val weatherLabel = if (context.location == AppVisualLocation.GAME) {
        context.weatherMood.russianLabel
    } else {
        "Тихий зал"
    }
    val emotionLabel = if (context.location == AppVisualLocation.GAME &&
        context.activeEmotion != EmotionCategory.NEUTRAL
    ) {
        " • ${context.activeEmotion.russianDefault}"
    } else {
        ""
    }
    return "$musicLabel • $weatherLabel$emotionLabel"
}

/**
 * Zero-dependency, offline procedural sound engine rendering three synchronized layers:
 * 1. Dynamic OST (chords + melody adapting to screen, universe, and NPC emotion)
 * 2. Environmental Ambient (rain droplets, distant thunder, hearth crackle, neon hum, wind)
 * 3. Instant UI, Scene, Emotion & Fate Roulette SFX
 */
class RpgAudioEngine {
    @Volatile
    private var soundEnabled: Boolean = true

    @Volatile
    private var ostVolume: Float = 0.65f

    @Volatile
    private var sfxVolume: Float = 0.80f

    @Volatile
    private var inForeground: Boolean = true

    @Volatile
    private var themeContext: DynamicThemeContext = DynamicThemeContext()

    @Volatile
    private var released: Boolean = false

    private val lock = Any()
    private var workerThread: Thread? = null

    private val activeSfxTones = ConcurrentLinkedQueue<ActiveTone>()
    private val arpeggioQueue = mutableListOf<ActiveTone>()

    fun updateSettings(enabled: Boolean, ostVol: Float, sfxVol: Float) {
        soundEnabled = enabled
        ostVolume = ostVol.coerceIn(0f, 1f)
        sfxVolume = sfxVol.coerceIn(0f, 1f)
        syncPlaybackThread()
    }

    fun updateContext(context: DynamicThemeContext) {
        themeContext = context
    }

    fun setAppForeground(foreground: Boolean) {
        inForeground = foreground
        syncPlaybackThread()
    }

    fun playSfx(event: SfxEvent) {
        if (!soundEnabled || !inForeground || sfxVolume <= 0.01f || released) return
        syncPlaybackThread()
        val nowPitchJitter = Random.nextDouble(0.97, 1.03)
        when (event) {
            SfxEvent.UI_TAP -> {
                activeSfxTones.add(
                    ActiveTone(520.0 * nowPitchJitter, 340.0, 0.0, 0.0, 0.045, 0.003, 0.16, 1)
                )
            }
            SfxEvent.SWIPE_WHOOSH -> {
                activeSfxTones.add(
                    ActiveTone(240.0, 480.0, 0.0, 0.0, 0.14, 0.02, 0.11, 2)
                )
            }
            SfxEvent.SEND_ACTION -> {
                activeSfxTones.add(
                    ActiveTone(330.0, 495.0, 0.0, 0.0, 0.14, 0.008, 0.17, 0)
                )
                activeSfxTones.add(
                    ActiveTone(660.0, 660.0, 0.0, 0.0, 0.18, 0.015, 0.11, 0)
                )
            }
            SfxEvent.AI_REVEAL -> {
                listOf(523.25, 659.25, 783.99).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(
                            freq = hz,
                            endFreq = hz,
                            phase = 0.0,
                            elapsedSec = -idx * 0.065,
                            durationSec = 0.42,
                            attackSec = 0.015,
                            gain = 0.13,
                            waveform = 0
                        )
                    )
                }
            }
            SfxEvent.EMOTION_SHIFT -> {
                val baseHz = when (themeContext.activeEmotion) {
                    EmotionCategory.ANGER, EmotionCategory.FEAR -> 293.66
                    EmotionCategory.EMBARRASSMENT, EmotionCategory.JOY -> 587.33
                    EmotionCategory.SADNESS -> 392.00
                    else -> 440.00
                }
                activeSfxTones.add(ActiveTone(baseHz, baseHz * 1.25, 0.0, 0.0, 0.24, 0.02, 0.12, 0))
            }
            SfxEvent.ITEM_USE -> {
                activeSfxTones.add(ActiveTone(880.0, 1174.66, 0.0, 0.0, 0.16, 0.005, 0.14, 1))
            }
            SfxEvent.BUTTERFLY_SHIFT -> {
                listOf(440.0, 554.37, 659.25, 880.0, 1108.73).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(
                            freq = hz,
                            endFreq = hz * 1.01,
                            phase = 0.0,
                            elapsedSec = -idx * 0.05,
                            durationSec = 0.60,
                            attackSec = 0.02,
                            gain = 0.11,
                            waveform = 0
                        )
                    )
                }
            }
            SfxEvent.ROULETTE_TICK -> {
                activeSfxTones.add(
                    ActiveTone(760.0 * nowPitchJitter, 420.0, 0.0, 0.0, 0.036, 0.002, 0.20, 1)
                )
            }
            SfxEvent.OUTCOME_TRIUMPH -> {
                listOf(523.25, 659.25, 783.99, 1046.50).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(hz, hz, 0.0, -idx * 0.06, 0.55, 0.012, 0.16, 0)
                    )
                }
            }
            SfxEvent.OUTCOME_MIXED -> {
                listOf(440.0, 587.33, 659.25).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(hz, hz, 0.0, -idx * 0.07, 0.45, 0.015, 0.14, 0)
                    )
                }
            }
            SfxEvent.OUTCOME_SETBACK -> {
                listOf(329.63, 293.66, 220.00).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(hz, hz * 0.96, 0.0, -idx * 0.08, 0.50, 0.02, 0.16, 1)
                    )
                }
            }
            SfxEvent.OUTCOME_TWIST -> {
                listOf(415.30, 523.25, 659.25, 987.77).forEachIndexed { idx, hz ->
                    activeSfxTones.add(
                        ActiveTone(hz, hz * 1.02, 0.0, -idx * 0.055, 0.58, 0.015, 0.14, 0)
                    )
                }
            }
        }
    }

    fun release() {
        released = true
        stopThread()
    }

    private fun shouldRunAudio(): Boolean =
        !released && soundEnabled && inForeground && (ostVolume > 0.01f || sfxVolume > 0.01f)

    private fun syncPlaybackThread() {
        synchronized(lock) {
            if (shouldRunAudio()) {
                if (workerThread == null || workerThread?.isAlive == false) {
                    val thread = Thread(::runAudioLoop, "ChroniclesAudioSynth").apply {
                        isDaemon = true
                    }
                    workerThread = thread
                    thread.start()
                }
            } else {
                stopThreadLocked()
            }
        }
    }

    private fun stopThread() {
        synchronized(lock) {
            stopThreadLocked()
        }
    }

    private fun stopThreadLocked() {
        val current = workerThread
        workerThread = null
        current?.interrupt()
    }

    private fun runAudioLoop() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }

        val minBufBytes = runCatching {
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        }.getOrDefault(-1)

        if (minBufBytes <= 0) return

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufBytes, FRAMES_PER_CHUNK * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        try {
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                return
            }
            track.play()

            val buffer = ShortArray(FRAMES_PER_CHUNK)
            val padPhases = DoubleArray(4)
            var subPhase = 0.0
            var lfoPhase = 0.0
            var noiseLp = 0.0
            var noiseBp = 0.0
            var thunderEnv = 0.0
            var crackleEnv = 0.0
            var dropletEnv = 0.0
            var dropletPhase = 0.0
            var dropletFreq = 1200.0
            var stepTimerSec = 0.0
            var stepIndex = 0
            var smoothedOstGain = 0.0
            var smoothedSfxGain = 0.0
            val dt = 1.0 / SAMPLE_RATE

            while (!Thread.currentThread().isInterrupted && shouldRunAudio()) {
                val ctx = themeContext
                val profile = musicalProfileFor(ctx)
                val targetOstGain = ostVolume.toDouble().coerceIn(0.0, 1.0)
                val targetSfxGain = sfxVolume.toDouble().coerceIn(0.0, 1.0)
                val ambientGain = if (ctx.location == AppVisualLocation.GAME) {
                    targetSfxGain * 0.42
                } else {
                    targetOstGain * 0.14
                }

                for (i in buffer.indices) {
                    smoothedOstGain += (targetOstGain - smoothedOstGain) * 0.0025
                    smoothedSfxGain += (targetSfxGain - smoothedSfxGain) * 0.01

                    stepTimerSec += dt
                    if (stepTimerSec >= profile.beatSeconds) {
                        stepTimerSec -= profile.beatSeconds
                        stepIndex++
                        val chord = profile.chordSemitones[(stepIndex / 4) % profile.chordSemitones.size]
                        if (stepIndex % 4 == 0 || Random.nextDouble() < 0.72) {
                            val semitone = if (stepIndex % 2 == 0) {
                                chord[stepIndex % chord.size]
                            } else {
                                profile.melodyOffsets[Random.nextInt(profile.melodyOffsets.size)]
                            }
                            val noteHz = profile.rootHz * semitoneRatio(semitone + 12)
                            arpeggioQueue.add(
                                ActiveTone(
                                    freq = noteHz,
                                    endFreq = noteHz,
                                    phase = 0.0,
                                    elapsedSec = 0.0,
                                    durationSec = profile.beatSeconds * 2.3,
                                    attackSec = 0.025,
                                    gain = 0.095,
                                    waveform = 0
                                )
                            )
                            if (arpeggioQueue.size > 10) arpeggioQueue.removeAt(0)
                        }
                    }

                    // 1. OST Pad & Sub Layer
                    val chord = profile.chordSemitones[(stepIndex / 4) % profile.chordSemitones.size]
                    lfoPhase = (lfoPhase + dt * 0.22) % 1.0
                    val lfoBreath = 0.78 + 0.22 * sin(2.0 * PI * lfoPhase)
                    var padMix = 0.0
                    for (v in 0 until minOf(4, chord.size)) {
                        val freq = profile.rootHz * semitoneRatio(chord[v])
                        padPhases[v] = (padPhases[v] + dt * freq) % 1.0
                        val fundamental = sin(2.0 * PI * padPhases[v])
                        val second = sin(4.0 * PI * padPhases[v]) * profile.padWarmth
                        padMix += (fundamental + second) * 0.22
                    }
                    val subFreq = (profile.rootHz * semitoneRatio(chord.first())) * 0.5
                    subPhase = (subPhase + dt * subFreq) % 1.0
                    padMix += sin(2.0 * PI * subPhase) * 0.14
                    padMix *= lfoBreath

                    // OST Arpeggio notes
                    var arpMix = 0.0
                    val arpIter = arpeggioQueue.iterator()
                    while (arpIter.hasNext()) {
                        val t = arpIter.next()
                        t.elapsedSec += dt
                        if (t.elapsedSec >= t.durationSec) {
                            arpIter.remove()
                        } else {
                            arpMix += sampleTone(t, dt)
                        }
                    }
                    val ostSample = (padMix * 0.55 + arpMix * 0.85) * smoothedOstGain

                    // 2. Environmental Weather & Scene Ambient Layer
                    val white = Random.nextDouble(-1.0, 1.0)
                    noiseLp += (white - noiseLp) * 0.08
                    noiseBp += (white - noiseLp - noiseBp) * 0.18
                    var envSample = 0.0
                    if (ctx.location == AppVisualLocation.GAME) {
                        when (ctx.weatherMood) {
                            WeatherMood.RAIN -> {
                                envSample += noiseLp * 0.22 + noiseBp * 0.16
                                if (dropletEnv <= 0.001 && Random.nextDouble() < 0.00045) {
                                    dropletEnv = Random.nextDouble(0.06, 0.14)
                                    dropletFreq = Random.nextDouble(950.0, 2100.0)
                                    dropletPhase = 0.0
                                }
                            }
                            WeatherMood.STORM -> {
                                envSample += noiseLp * 0.30 + noiseBp * 0.22
                                if (thunderEnv <= 0.01 && Random.nextDouble() < 0.00004) {
                                    thunderEnv = Random.nextDouble(0.28, 0.52)
                                }
                            }
                            WeatherMood.INDOOR_CANDLE -> {
                                envSample += noiseLp * 0.06
                                if (crackleEnv <= 0.001 && Random.nextDouble() < 0.00075) {
                                    crackleEnv = Random.nextDouble(0.10, 0.28)
                                }
                            }
                            WeatherMood.NEON_NIGHT -> {
                                envSample += noiseLp * 0.10 + sin(2.0 * PI * ((stepTimerSec * 55.0) % 1.0)) * 0.03
                            }
                            WeatherMood.FOG, WeatherMood.NIGHT, WeatherMood.SNOW -> {
                                envSample += noiseLp * (0.14 + 0.08 * sin(2.0 * PI * lfoPhase))
                            }
                            WeatherMood.DAWN, WeatherMood.DAY -> {
                                envSample += noiseLp * 0.07
                            }
                        }
                        if (dropletEnv > 0.001) {
                            dropletPhase = (dropletPhase + dt * dropletFreq) % 1.0
                            envSample += sin(2.0 * PI * dropletPhase) * dropletEnv
                            dropletEnv *= 0.996
                        }
                        if (thunderEnv > 0.001) {
                            envSample += noiseLp * thunderEnv * 0.95
                            thunderEnv *= 0.99975
                        }
                        if (crackleEnv > 0.001) {
                            envSample += white * crackleEnv * 0.45
                            crackleEnv *= 0.988
                        }
                    } else {
                        envSample += noiseLp * 0.04
                    }

                    // 3. One-shot SFX Queue
                    var sfxMix = 0.0
                    val sfxIter = activeSfxTones.iterator()
                    while (sfxIter.hasNext()) {
                        val t = sfxIter.next()
                        t.elapsedSec += dt
                        if (t.elapsedSec < 0.0) continue
                        if (t.elapsedSec >= t.durationSec) {
                            sfxIter.remove()
                        } else {
                            sfxMix += sampleTone(t, dt)
                        }
                    }

                    val combined = ostSample + (envSample * ambientGain) + (sfxMix * smoothedSfxGain)
                    // Soft limiter to prevent digital clipping
                    val clamped = (combined / (1.0 + kotlin.math.abs(combined) * 0.35))
                        .coerceIn(-0.98, 0.98)
                    buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                }

                val written = track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                if (written < 0) break
            }
        } finally {
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.release() }
        }
    }

    private fun sampleTone(tone: ActiveTone, dt: Double): Double {
        val tNorm = (tone.elapsedSec / tone.durationSec).coerceIn(0.0, 1.0)
        val env = if (tone.elapsedSec < tone.attackSec) {
            tone.elapsedSec / tone.attackSec
        } else {
            exp(-4.2 * ((tone.elapsedSec - tone.attackSec) / (tone.durationSec - tone.attackSec).coerceAtLeast(0.01)))
        }
        val instFreq = tone.freq + (tone.endFreq - tone.freq) * tNorm
        tone.phase = (tone.phase + dt * instFreq) % 1.0
        val raw = when (tone.waveform) {
            1 -> {
                val tri = 1.0 - 4.0 * kotlin.math.abs(tone.phase - 0.5)
                tri * 0.7 + sin(2.0 * PI * tone.phase) * 0.3
            }
            2 -> {
                sin(2.0 * PI * tone.phase) * 0.45 + Random.nextDouble(-0.35, 0.35)
            }
            else -> {
                sin(2.0 * PI * tone.phase) * 0.80 + sin(4.0 * PI * tone.phase) * 0.20
            }
        }
        return raw * env * tone.gain
    }

    private fun musicalProfileFor(ctx: DynamicThemeContext): MusicalScaleProfile {
        val tenseEmotion = ctx.activeEmotion in setOf(
            EmotionCategory.ANGER,
            EmotionCategory.FEAR,
            EmotionCategory.SUSPICION
        )
        val warmEmotion = ctx.activeEmotion in setOf(
            EmotionCategory.JOY,
            EmotionCategory.EMBARRASSMENT,
            EmotionCategory.CALM
        )

        if (ctx.location != AppVisualLocation.GAME) {
            return when (ctx.location) {
                AppVisualLocation.WIZARD -> MusicalScaleProfile(
                    titleRu = "Плетение судьбы",
                    rootHz = 146.83, // D3
                    chordSemitones = listOf(listOf(0, 3, 7, 10), listOf(5, 8, 12, 15), listOf(3, 7, 10, 14)),
                    melodyOffsets = listOf(0, 3, 5, 7, 10, 12),
                    beatSeconds = 0.72,
                    padWarmth = 0.20
                )
                else -> MusicalScaleProfile(
                    titleRu = "Книжный ноктюрн",
                    rootHz = 174.61, // F3
                    chordSemitones = listOf(listOf(0, 4, 7, 11), listOf(5, 9, 12, 16), listOf(2, 5, 9, 12)),
                    melodyOffsets = listOf(0, 2, 4, 7, 9, 12),
                    beatSeconds = 0.84,
                    padWarmth = 0.16
                )
            }
        }

        val arch = resolveUniverseArchetype(ctx.universe)
        val base = when (arch) {
            UniverseArchetype.COZY_ROMANCE -> MusicalScaleProfile(
                titleRu = "Тёплое эл. пианино",
                rootHz = 174.61, // F3
                chordSemitones = listOf(listOf(0, 4, 7, 11), listOf(5, 9, 12, 16), listOf(2, 5, 9, 12), listOf(7, 11, 14, 17)),
                melodyOffsets = listOf(0, 4, 7, 9, 11, 12, 14),
                beatSeconds = 0.85,
                padWarmth = 0.15
            )
            UniverseArchetype.CYBERPUNK_SCIFI -> MusicalScaleProfile(
                titleRu = "Аналоговый неон-синт",
                rootHz = 130.81, // C3
                chordSemitones = listOf(listOf(0, 3, 7, 10), listOf(5, 8, 12, 15), listOf(8, 12, 15, 19), listOf(7, 10, 14, 17)),
                melodyOffsets = listOf(0, 3, 5, 7, 10, 12, 15),
                beatSeconds = 0.62,
                padWarmth = 0.34
            )
            UniverseArchetype.SPACE_SCIFI -> MusicalScaleProfile(
                titleRu = "Космический дрон",
                rootHz = 110.00, // A2
                chordSemitones = listOf(listOf(0, 7, 12, 16), listOf(5, 12, 17, 21), listOf(3, 10, 15, 19)),
                melodyOffsets = listOf(0, 4, 7, 11, 12, 19),
                beatSeconds = 0.96,
                padWarmth = 0.14
            )
            UniverseArchetype.WITCHER_FANTASY -> MusicalScaleProfile(
                titleRu = "Лютень и струны",
                rootHz = 146.83, // D3
                chordSemitones = listOf(listOf(0, 3, 7, 12), listOf(-2, 2, 5, 10), listOf(5, 9, 12, 17)),
                melodyOffsets = listOf(0, 2, 3, 5, 7, 9, 10, 12),
                beatSeconds = 0.74,
                padWarmth = 0.26
            )
            UniverseArchetype.SHINOBI_ANIME -> MusicalScaleProfile(
                titleRu = "Кото и флейта",
                rootHz = 164.81, // E3
                chordSemitones = listOf(listOf(0, 5, 7, 12), listOf(1, 5, 8, 13), listOf(5, 8, 12, 17)),
                melodyOffsets = listOf(0, 1, 5, 7, 8, 12, 13),
                beatSeconds = 0.68,
                padWarmth = 0.22
            )
            UniverseArchetype.TITAN_SURVIVAL -> MusicalScaleProfile(
                titleRu = "Тревожный оркестр",
                rootHz = 123.47, // B2
                chordSemitones = listOf(listOf(0, 3, 7, 12), listOf(5, 8, 12, 15), listOf(1, 5, 8, 12)),
                melodyOffsets = listOf(0, 3, 6, 7, 10, 12),
                beatSeconds = 0.65,
                padWarmth = 0.30
            )
            UniverseArchetype.ARCANE_MYSTERY -> MusicalScaleProfile(
                titleRu = "Мистическая челеста",
                rootHz = 164.81, // E3
                chordSemitones = listOf(listOf(0, 3, 7, 11), listOf(5, 9, 12, 16), listOf(7, 11, 14, 18)),
                melodyOffsets = listOf(0, 2, 3, 7, 8, 11, 12),
                beatSeconds = 0.80,
                padWarmth = 0.18
            )
            UniverseArchetype.GENERIC_CHRONICLE -> MusicalScaleProfile(
                titleRu = "Книжный саундтрек",
                rootHz = 146.83, // D3
                chordSemitones = listOf(listOf(0, 4, 7, 11), listOf(5, 9, 12, 16), listOf(3, 7, 10, 14)),
                melodyOffsets = listOf(0, 2, 4, 7, 9, 12),
                beatSeconds = 0.82,
                padWarmth = 0.18
            )
        }

        return when {
            tenseEmotion -> base.copy(
                beatSeconds = (base.beatSeconds * 0.82).coerceAtLeast(0.48),
                padWarmth = (base.padWarmth + 0.12).coerceAtMost(0.45),
                chordSemitones = listOf(listOf(0, 3, 6, 10), listOf(5, 8, 12, 15), listOf(1, 5, 8, 12))
            )
            warmEmotion -> base.copy(
                beatSeconds = (base.beatSeconds * 1.06).coerceAtMost(1.05),
                padWarmth = (base.padWarmth * 0.85)
            )
            else -> base
        }
    }

    private fun semitoneRatio(semitones: Int): Double =
        Math.pow(2.0, semitones / 12.0)

    private companion object {
        const val SAMPLE_RATE = 22050
        const val FRAMES_PER_CHUNK = 1024
    }
}
