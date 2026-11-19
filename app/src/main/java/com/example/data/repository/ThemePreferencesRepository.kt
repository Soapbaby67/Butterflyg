package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PaperThemeMode(val key: String, val labelRu: String) {
    DYNAMIC("dynamic", "Динамический градиент"),
    LIGHT("light", "Светлая бумага"),
    DARK("dark", "Тёмная бумага"),
    NEON("neon", "Ночной неон"),
    SYSTEM("system", "Системная");

    companion object {
        fun fromKey(key: String?): PaperThemeMode =
            entries.firstOrNull { it.key == key } ?: DYNAMIC
    }
}

enum class AiModelChoice(
    val modelId: String,
    val titleRu: String,
    val subtitleRu: String
) {
    FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        titleRu = "Gemini 3.5 Flash-Lite",
        subtitleRu = "По умолчанию • быстрый отклик и высокие лимиты"
    ),
    FLASH_38(
        modelId = "gemini-3.8-flash",
        titleRu = "Gemini 3.8 Flash",
        subtitleRu = "Более детальный стиль • лимит 5 запросов/мин"
    );

    companion object {
        fun fromKey(key: String?): AiModelChoice =
            entries.firstOrNull { it.modelId == key || it.name == key } ?: FLASH_LITE
    }
}

enum class NarrativePace(
    val key: String,
    val labelRu: String,
    val subtitleRu: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val promptDirectiveRu: String
) {
    DYNAMIC(
        key = "dynamic",
        labelRu = "Динамичный",
        subtitleRu = "2 лаконичных абзаца • упор на действие и живые реплики",
        temperature = 0.88f,
        maxOutputTokens = 6144,
        promptDirectiveRu = "ТЕМП ПОВЕСТВОВАНИЯ — ДИНАМИЧНЫЙ: пиши ровно 2 плотных, энергичных абзаца с упором на действие, диалоги и быстрый ритм сцены без длинных описаний."
    ),
    BOOKISH(
        key = "bookish",
        labelRu = "Книжный",
        subtitleRu = "3–4 художественных абзаца • баланс атмосферы и диалогов",
        temperature = 0.85f,
        maxOutputTokens = 8192,
        promptDirectiveRu = "ТЕМП ПОВЕСТВОВАНИЯ — КНИЖНЫЙ: пиши 3–4 выразительных литературных абзаца, сочетая атмосферу, детали мира и живые реплики NPC."
    ),
    DEEP(
        key = "deep",
        labelRu = "Глубокий",
        subtitleRu = "4–5 насыщенных абзацев • детали, подтекст и интриги",
        temperature = 0.72f,
        maxOutputTokens = 10240,
        promptDirectiveRu = "ТЕМП ПОВЕСТВОВАНИЯ — ГЛУБОКИЙ: пиши 4–5 детальных, психологически точных абзацев с богатыми сенсорными деталями, подтекстом, уликами и тонкими нюансами поведения NPC."
    );

    companion object {
        fun fromKey(key: String?): NarrativePace =
            entries.firstOrNull { it.key == key } ?: BOOKISH
    }
}

enum class ReadingFontStyle(
    val key: String,
    val labelRu: String,
    val subtitleRu: String
) {
    LITERATA("literata", "Книжный", "Классическая антиква"),
    PLAYFAIR("playfair", "Роман", "Выразительный журнальный"),
    NUNITO("nunito", "Уютный", "Мягкий для долгого чтения"),
    INTER("inter", "Чистый", "Современный минимализм"),
    JETBRAINS_MONO("jetbrains_mono", "Терминал", "Моноширинный Sci-Fi");

    companion object {
        fun fromKey(key: String?): ReadingFontStyle =
            entries.firstOrNull { it.key == key } ?: LITERATA
    }
}

enum class ReadingFontScale(
    val key: String,
    val labelRu: String,
    val multiplier: Float
) {
    SMALL("small", "Малый", 0.88f),
    NORMAL("normal", "Обычный", 1.0f),
    LARGE("large", "Крупный", 1.15f),
    XLARGE("xlarge", "XL", 1.30f);

    companion object {
        fun fromKey(key: String?): ReadingFontScale =
            entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

data class UserAppSettings(
    val themeMode: PaperThemeMode = PaperThemeMode.DYNAMIC,
    val aiModel: AiModelChoice = AiModelChoice.FLASH_LITE,
    val narrativePace: NarrativePace = NarrativePace.BOOKISH,
    val fontStyle: ReadingFontStyle = ReadingFontStyle.LITERATA,
    val fontScale: ReadingFontScale = ReadingFontScale.NORMAL,
    val liveStreamingEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val ostVolume: Float = 0.65f,
    val sfxVolume: Float = 0.80f,
    val autoReadTtsEnabled: Boolean = false
)

internal val Context.themeDataStore by preferencesDataStore(name = "chronicles_preferences")

class ThemePreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("paper_theme_mode")
    private val aiModelKey = stringPreferencesKey("ai_model_choice")
    private val narrativePaceKey = stringPreferencesKey("ai_narrative_pace")
    private val fontStyleKey = stringPreferencesKey("reading_font_style")
    private val fontScaleKey = stringPreferencesKey("reading_font_scale")
    private val liveStreamingKey = booleanPreferencesKey("live_streaming_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val soundEnabledKey = booleanPreferencesKey("sound_enabled")
    private val ostVolumeKey = floatPreferencesKey("sound_ost_volume")
    private val sfxVolumeKey = floatPreferencesKey("sound_sfx_volume")
    private val autoReadTtsKey = booleanPreferencesKey("tts_auto_read_enabled")

    private fun draftKeyForCampaign(campaignId: String) =
        stringPreferencesKey("draft_action_${campaignId.take(64)}")

    val settings: Flow<UserAppSettings> = context.themeDataStore.data.map { prefs ->
        UserAppSettings(
            themeMode = PaperThemeMode.fromKey(prefs[themeModeKey]),
            aiModel = AiModelChoice.fromKey(prefs[aiModelKey]),
            narrativePace = NarrativePace.fromKey(prefs[narrativePaceKey]),
            fontStyle = ReadingFontStyle.fromKey(prefs[fontStyleKey]),
            fontScale = ReadingFontScale.fromKey(prefs[fontScaleKey]),
            liveStreamingEnabled = prefs[liveStreamingKey] ?: true,
            hapticsEnabled = prefs[hapticsKey] ?: true,
            soundEnabled = prefs[soundEnabledKey] ?: true,
            ostVolume = (prefs[ostVolumeKey] ?: 0.65f).coerceIn(0f, 1f),
            sfxVolume = (prefs[sfxVolumeKey] ?: 0.80f).coerceIn(0f, 1f),
            autoReadTtsEnabled = prefs[autoReadTtsKey] ?: false
        )
    }

    val themeMode: Flow<PaperThemeMode> = settings.map { it.themeMode }

    fun observeDraftAction(campaignId: String): Flow<String> {
        val key = draftKeyForCampaign(campaignId)
        return context.themeDataStore.data.map { prefs -> prefs[key].orEmpty() }
    }

    suspend fun saveDraftAction(campaignId: String, text: String) {
        val key = draftKeyForCampaign(campaignId)
        context.themeDataStore.edit { prefs ->
            if (text.isBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = text.take(1200)
            }
        }
    }

    suspend fun clearDraftAction(campaignId: String) {
        val key = draftKeyForCampaign(campaignId)
        context.themeDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun setThemeMode(mode: PaperThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.key
        }
    }

    suspend fun setAiModel(model: AiModelChoice) {
        context.themeDataStore.edit { prefs ->
            prefs[aiModelKey] = model.modelId
        }
    }

    suspend fun setNarrativePace(pace: NarrativePace) {
        context.themeDataStore.edit { prefs ->
            prefs[narrativePaceKey] = pace.key
        }
    }

    suspend fun setFontStyle(style: ReadingFontStyle) {
        context.themeDataStore.edit { prefs ->
            prefs[fontStyleKey] = style.key
        }
    }

    suspend fun setFontScale(scale: ReadingFontScale) {
        context.themeDataStore.edit { prefs ->
            prefs[fontScaleKey] = scale.key
        }
    }

    suspend fun setLiveStreamingEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[liveStreamingKey] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[hapticsKey] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[soundEnabledKey] = enabled
        }
    }

    suspend fun setOstVolume(volume: Float) {
        context.themeDataStore.edit { prefs ->
            prefs[ostVolumeKey] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.themeDataStore.edit { prefs ->
            prefs[sfxVolumeKey] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun setAutoReadTtsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[autoReadTtsKey] = enabled
        }
    }
}
