package com.example.ui

import com.example.R
import com.example.data.model.Campaign
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
}
