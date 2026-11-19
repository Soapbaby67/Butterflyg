package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PaperThemeMode(
    val key: String,
    val labelRu: String,
    val subtitleRu: String,
    val isLightPalette: Boolean = false
) {
    DYNAMIC("dynamic", "Динамический градиент", "Подстраивается под сцену и мир"),
    LIGHT("light", "Светлая бумага", "Классический книжный лист", isLightPalette = true),
    DARK("dark", "Тёмная бумага", "Тёплый вечерний пергамент"),
    NEON("neon", "Ночной неон", "Киберпанк и неоновый циан"),
    EMERALD_FOREST("emerald_forest", "Изумрудный лес", "Хвойный сумрак и золотистый мох"),
    CRIMSON_VELVET("crimson_velvet", "Алый бархат", "Готический обсидиан и гранат"),
    SEPIA_PARCHMENT("sepia_parchment", "Сепия пергамента", "Винтажная тёплая бумага", isLightPalette = true),
    SAKURA_MIST("sakura_mist", "Сакура и туман", "Рисовый шёлк и нежные лепестки", isLightPalette = true),
    DEEP_COSMOS("deep_cosmos", "Глубокий космос", "Звёздный ультрамарин и лунный свет"),
    CUBIC_BIOME("cubic_biome", "Кубический мир", "Изумрудная листва и факельный янтарь"),
    OLED_OBSIDIAN("oled_obsidian", "OLED-Обсидиан", "Абсолютно чёрный фон для ночи"),
    SYSTEM("system", "Системная", "Как в настройках устройства");

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
    val subtitleRu: String,
    val samplePhraseRu: String = "Взмах крыла меняет ход судьбы"
) {
    LITERATA("literata", "Книжный", "Классическая антиква", "Страницы древнего тома шелестят"),
    PLAYFAIR("playfair", "Роман", "Выразительный журнальный", "Вечерний свет ложится на витраж"),
    NUNITO("nunito", "Уютный", "Мягкий для долгого чтения", "Тёплый чай и тихий голос дождя"),
    INTER("inter", "Чистый", "Современный минимализм", "Ясный ритм и точность каждой детали"),
    MINECRAFT_PIXEL("minecraft_pixel", "Майнкрафт", "Культовый 8-бит пиксель", "НОВЫЙ БИОМ • КРАФТ И ПРИКЛЮЧЕНИЕ"),
    ARCADE_16("arcade_16", "Аркада 16-бит", "Чёткий пиксель для RPG", "Уровень повышен: +1 к Скрытности"),
    AUTHOR_DIARY("author_diary", "Авторский дневник", "Живой рукописный почерк", "Запись от руки на полях хроники…"),
    QUILL_SCRIPT("quill_script", "Пергаментное перо", "Каллиграфический авторский", "Письмо, запечатанное алым сургучом"),
    ANCIENT_CHRONICLE("ancient_chronicle", "Древняя летопись", "Славянско-фэнтезийный стиль", "Сказание о вратах и забытых клятвах"),
    CYBER_DISPLAY("cyber_display", "Кибер-дисплей", "Футуристичный техно-шрифт", "СЕКТОР 09 // ПРОТОКОЛ БАБОЧКИ"),
    INDIE_GRUNGE("indie_grunge", "Авторский гранж", "Характерный инди-шрифт", "Город не прощает лишних вопросов"),
    NOIR_TYPEWRITER("noir_typewriter", "Нуар-машинка", "Печатная машинка детектива", "ДЕЛО №47: улика на старом причале"),
    JETBRAINS_MONO("jetbrains_mono", "Терминал", "Моноширинный Sci-Fi", "system.log :: deviation = 42%");

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
    val applyFontToWholeUi: Boolean = true,
    val focusReadingMode: Boolean = false,
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
    private val applyFontToWholeUiKey = booleanPreferencesKey("apply_font_to_whole_ui")
    private val focusReadingModeKey = booleanPreferencesKey("focus_reading_mode")
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
            applyFontToWholeUi = prefs[applyFontToWholeUiKey] ?: true,
            focusReadingMode = prefs[focusReadingModeKey] ?: false,
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

    suspend fun setApplyFontToWholeUi(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[applyFontToWholeUiKey] = enabled
        }
    }

    suspend fun setFocusReadingMode(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[focusReadingModeKey] = enabled
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
