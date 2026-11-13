package com.example.ui

import com.example.R
import com.example.data.model.Campaign
import com.example.data.model.NpcRelation
import com.example.data.model.ScenarioGenre
import com.example.data.model.UniversePresets
import com.example.data.repository.AiModelChoice
import com.example.data.repository.NpcTrustGuardrail
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle
import com.example.data.repository.StreamingNarrativeExtractor
import com.example.data.repository.UserAppSettings
import com.example.ui.components.HudStatsParser
import com.example.ui.theme.NpcArchetype
import com.example.ui.theme.NpcPortraitSpec
import com.example.ui.theme.UniverseArchetype
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.resolveNpcPortraitSpec
import com.example.ui.theme.resolveSceneBackdropRes
import com.example.ui.theme.resolveUniverseArchetype
import com.example.ui.theme.resolveUniverseVisualPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudStatsAndVisualThemeTest {

    @Test
    fun parsesLegacyBracketStyleCampaignCleanly() {
        val campaign = Campaign(
            id = "witcher_1",
            title = "Огни Новиграда",
            universe = "Ведьмак (Северные королевства)",
            hudLocation = "Доки Новиграда",
            hudTime = "День, пасмурно, проливной дождь",
            hudStatusAndInventory = "Здоровье: в норме | Инвентарь: Серебряный стилет, алхимический набор, 15 крон",
            hudRelationsAndCanon = "Отношения нейтральные, Канон не нарушен",
            inventory = listOf("Серебряный стилет", "алхимический набор", "15 крон")
        )

        val stats = HudStatsParser.parse(campaign)

        assertEquals(100, stats.healthPercent)
        assertTrue(stats.healthLabel.equals("В норме", ignoreCase = true))
        assertEquals("15 крон", stats.currencyLabel)
        assertEquals(listOf("Серебряный стилет", "Алхимический набор"), stats.inventoryItems)
        assertEquals("Отношения нейтральные", stats.relationsSummary)
        assertEquals(WeatherMood.RAIN, stats.weatherMood)
    }

    @Test
    fun resolvesUniversesAndSceneBackdropsCorrectly() {
        assertEquals(
            UniverseArchetype.CYBERPUNK_SCIFI,
            resolveUniverseArchetype("Cyberpunk 2077 (Найт-Сити)")
        )
        assertEquals(
            UniverseArchetype.SHINOBI_ANIME,
            resolveUniverseArchetype("Наруто (мир шиноби)")
        )
        assertEquals(
            UniverseArchetype.COZY_ROMANCE,
            resolveUniverseArchetype("Современный европейский город, романтическая драма повседневности")
        )
        assertEquals(
            UniverseArchetype.SPACE_SCIFI,
            resolveUniverseArchetype("Научная фантастика, орбитальная станция и дальний космос")
        )
        assertEquals(
            R.drawable.img_scene_neon_district,
            resolveSceneBackdropRes("Cyberpunk 2077", "Бар в Кабуки", WeatherMood.NEON_NIGHT)
        )
        assertEquals(
            R.drawable.img_scene_fantasy_harbor,
            resolveSceneBackdropRes("Ведьмак", "Доки Новиграда", WeatherMood.RAIN)
        )
        assertEquals(
            R.drawable.img_scene_ancient_sanctum,
            resolveSceneBackdropRes("Гарри Поттер", "Библиотека Хогвартса", WeatherMood.INDOOR_CANDLE)
        )
        assertEquals(
            R.drawable.img_scene_cozy_cafe,
            resolveSceneBackdropRes("Романтика повседневности", "Кофейня на углу", WeatherMood.RAIN)
        )
        assertEquals(
            R.drawable.img_scene_space_station,
            resolveSceneBackdropRes("Научная фантастика", "Смотровая палуба", WeatherMood.NIGHT)
        )
    }

    @Test
    fun resolvesDiverseNpcPortraitsAndSilhouettes() {
        val palette = resolveUniverseVisualPalette("Ведьмак", isDark = true)

        val smuggler = resolveNpcPortraitSpec(
            name = "Контрабандист",
            role = "Торговый агент в доках",
            universe = "Ведьмак",
            explicitArchetype = "",
            palette = palette
        )
        assertTrue(smuggler is NpcPortraitSpec.PhotoPortrait)
        assertEquals(NpcArchetype.ROGUE, (smuggler as NpcPortraitSpec.PhotoPortrait).archetype)

        val sorceress = resolveNpcPortraitSpec(
            name = "Трисс Меригольд",
            role = "Чародейка",
            universe = "Ведьмак",
            explicitArchetype = "",
            palette = palette
        )
        assertTrue(sorceress is NpcPortraitSpec.PhotoPortrait)
        assertEquals(NpcArchetype.NOBLE, (sorceress as NpcPortraitSpec.PhotoPortrait).archetype)

        val merchant = resolveNpcPortraitSpec(
            name = "Берндт",
            role = "Трактирщик",
            universe = "Ведьмак",
            explicitArchetype = "merchant",
            palette = palette
        )
        assertTrue(merchant is NpcPortraitSpec.SilhouetteEmblem)
        assertEquals(NpcArchetype.MERCHANT, (merchant as NpcPortraitSpec.SilhouetteEmblem).archetype)
    }

    @Test
    fun streamingNarrativeExtractorDecodesChunkedEscapedRussianJson() {
        val extractor = StreamingNarrativeExtractor()

        assertNull(extractor.append("""{"hudTime": "Вечер", "n"""))
        assertNull(extractor.append("""arrative": """"))

        val chunk1 = extractor.append("""Дождь за окном\n«Привет», — ти""")
        assertEquals("Дождь за окном\n«Привет», — ти", chunk1)

        val chunk2 = extractor.append("""хо \\ сказал \u041e\u043d.\"Идём?""")
        assertEquals("Дождь за окном\n«Привет», — тихо \\ сказал Он.\"Идём?", chunk2)

        val chunk3 = extractor.append("""\"", "hudLocation": "Кофейня"}""")
        assertEquals("Дождь за окном\n«Привет», — тихо \\ сказал Он.\"Идём?\"", chunk3)

        // Subsequent JSON fields should not modify extracted narrative
        assertNull(extractor.append(""", "storySummary": "Игнорируемый текст"}"""))
        assertEquals(
            "Дождь за окном\n«Привет», — тихо \\ сказал Он.\"Идём?\"",
            extractor.currentNarrative()
        )
    }

    @Test
    fun expandedScenariosCoverRomanceRoutineAndSciFiWithoutPaidOrArgonModels() {
        assertEquals(10, UniversePresets.templates.size)
        assertTrue(UniversePresets.templates.any { it.genre == ScenarioGenre.ROMANCE })
        assertTrue(UniversePresets.templates.any { it.genre == ScenarioGenre.ROUTINE })
        assertTrue(UniversePresets.templates.any { it.genre == ScenarioGenre.FANTASY })
        assertTrue(UniversePresets.templates.any { it.genre == ScenarioGenre.ANIME })
        assertTrue(UniversePresets.templates.any { it.genre == ScenarioGenre.MYSTERY_SCIFI })

        val defaults = UserAppSettings()
        assertEquals(AiModelChoice.FLASH_LITE, defaults.aiModel)
        assertEquals("gemini-3.5-flash-lite", defaults.aiModel.modelId)
        assertEquals(ReadingFontStyle.LITERATA, defaults.fontStyle)
        assertEquals(ReadingFontScale.NORMAL, defaults.fontScale)
        assertFalse(AiModelChoice.entries.any { it.modelId.contains("argon", ignoreCase = true) })
        assertEquals(5, ReadingFontStyle.entries.size)
    }

    @Test
    fun npcTrustGuardrailPreventsInstantRomanceAndPreservesBoundaries() {
        // 1. Stranger on first meeting cannot instantly jump to +85 "Влюблена"
        val firstMeetingAttempt = NpcRelation(
            name = "Елена",
            role = "Бариста",
            trustLevel = 85L,
            statusLabel = "Влюблена без памяти",
            lastEmotion = "Восторг",
            lastEmotionCategory = "joy",
            personality = "Ироничная и гордая",
            boundaries = "Не флиртует с незнакомцами и не терпит навязчивости"
        )
        val clampedFirst = NpcTrustGuardrail.apply(firstMeetingAttempt, previous = null)
        assertEquals(25L, clampedFirst.trustLevel)
        assertEquals("Осторожный интерес", clampedFirst.statusLabel)
        assertEquals("Ироничная и гордая", clampedFirst.personality)
        assertEquals("Не флиртует с незнакомцами и не терпит навязчивости", clampedFirst.boundaries)

        // 2. Subsequent turn cannot jump trust by more than +12 and preserves omitted personality/boundaries
        val secondTurnAttempt = NpcRelation(
            name = "Елена",
            role = "",
            trustLevel = 90L,
            statusLabel = "Твоя девушка",
            personality = "",
            boundaries = ""
        )
        val clampedSecond = NpcTrustGuardrail.apply(secondTurnAttempt, previous = clampedFirst)
        assertEquals(37L, clampedSecond.trustLevel)
        assertEquals("Осторожный интерес", clampedSecond.statusLabel)
        assertEquals("Бариста", clampedSecond.role)
        assertEquals("Ироничная и гордая", clampedSecond.personality)
        assertEquals("Не флиртует с незнакомцами и не терпит навязчивости", clampedSecond.boundaries)

        // 3. Disrespect or boundary violation drops trust noticeably
        val rudeTurn = NpcRelation(
            name = "Елена",
            role = "Бариста",
            trustLevel = -10L,
            statusLabel = "Раздражена"
        )
        val clampedRude = NpcTrustGuardrail.apply(rudeTurn, previous = clampedSecond)
        assertEquals(2L, clampedRude.trustLevel)
        assertEquals("Раздражена", clampedRude.statusLabel)
    }
}
