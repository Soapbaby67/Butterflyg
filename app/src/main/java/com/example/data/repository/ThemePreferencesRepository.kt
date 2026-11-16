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
    val fontStyle: ReadingFontStyle = ReadingFontStyle.LITERATA,
    val fontScale: ReadingFontScale = ReadingFontScale.NORMAL,
    val liveStreamingEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val ostVolume: Float = 0.65f,
    val sfxVolume: Float = 0.80f
)

internal val Context.themeDataStore by preferencesDataStore(name = "chronicles_preferences")

class ThemePreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("paper_theme_mode")
    private val aiModelKey = stringPreferencesKey("ai_model_choice")
    private val fontStyleKey = stringPreferencesKey("reading_font_style")
    private val fontScaleKey = stringPreferencesKey("reading_font_scale")
    private val liveStreamingKey = booleanPreferencesKey("live_streaming_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val soundEnabledKey = booleanPreferencesKey("sound_enabled")
    private val ostVolumeKey = floatPreferencesKey("sound_ost_volume")
    private val sfxVolumeKey = floatPreferencesKey("sound_sfx_volume")

    val settings: Flow<UserAppSettings> = context.themeDataStore.data.map { prefs ->
        UserAppSettings(
            themeMode = PaperThemeMode.fromKey(prefs[themeModeKey]),
            aiModel = AiModelChoice.fromKey(prefs[aiModelKey]),
            fontStyle = ReadingFontStyle.fromKey(prefs[fontStyleKey]),
            fontScale = ReadingFontScale.fromKey(prefs[fontScaleKey]),
            liveStreamingEnabled = prefs[liveStreamingKey] ?: true,
            hapticsEnabled = prefs[hapticsKey] ?: true,
            soundEnabled = prefs[soundEnabledKey] ?: true,
            ostVolume = (prefs[ostVolumeKey] ?: 0.65f).coerceIn(0f, 1f),
            sfxVolume = (prefs[sfxVolumeKey] ?: 0.80f).coerceIn(0f, 1f)
        )
    }

    val themeMode: Flow<PaperThemeMode> = settings.map { it.themeMode }

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
}
