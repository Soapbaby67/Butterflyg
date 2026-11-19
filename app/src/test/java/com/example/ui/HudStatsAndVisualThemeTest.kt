package com.example.ui

import com.example.R
import com.example.data.model.Campaign
import com.example.data.model.EmotionCategory
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCategory
import com.example.data.model.NpcRelation
import com.example.data.model.ScenarioGenre
import com.example.data.model.UniversePresets
import com.example.data.repository.AiModelChoice
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.NpcTrustGuardrail
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle
import com.example.data.repository.StreamingNarrativeExtractor
import com.example.data.repository.UserAppSettings
import com.example.ui.components.HudStatsParser
import com.example.ui.components.ItemVisualKind
import com.example.ui.components.resolveItemVisualSpec
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.NpcArchetype
import com.example.ui.theme.NpcPortraitSpec
import com.example.ui.theme.UniverseArchetype
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.buildChroniclesTypography
import com.example.ui.theme.fontMetricsFor
import com.example.ui.theme.readingFontFamilyFor
import com.example.ui.theme.resolveDynamicGradientSpec
import com.example.ui.theme.resolveNpcPortraitSpec
import com.example.ui.theme.resolveSceneBackdropRes
import com.example.ui.theme.resolveUniverseArchetype
import com.example.ui.theme.resolveUniverseVisualPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun prefersExplicitNumericVitalsAndStructuredInventoryOverRegexFallback() {
        val campaign = Campaign(
            id = "structured_1",
            title = "Неоновый след",
            universe = "Cyberpunk 2077 (Найт-Сити)",
            hudLocation = "Рынок Кабуки",
            hudTime = "Ночь, неон",
            hudStatusAndInventory = "Здоровье: в норме",
            healthPercent = 64L,
            currencyAmount = 450L,
            currencyLabel = "эвродолларов",
            skills = HeroSkills(might = 4L, stealth = 8L, insight = 7L, lore = 6L),
            inventoryItems = listOf(
                InventoryItem(
                    id = "deck_1",
                    name = "Кибердека Arasaka",
                    category = ItemCategory.GEAR.key,
                    quantity = 1L,
                    isEquipped = true
                ),
                InventoryItem(
                    id = "stim_1",
                    name = "Стимулятор",
                    category = ItemCategory.CONSUMABLE.key,
                    quantity = 2L,
                    isEquipped = false
                )
            )
        )

        val stats = HudStatsParser.parse(campaign)
        assertEquals(64, stats.healthPercent)
        assertEquals("450 эвродолларов", stats.currencyLabel)
        assertEquals(2, stats.structuredInventory.size)
        assertTrue(stats.structuredInventory.first().isEquipped)
        assertEquals(8L, stats.skills.stealth)
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
        val fantasyPalette = resolveUniverseVisualPalette("Ведьмак", isDark = true)
        val cyberPalette = resolveUniverseVisualPalette("Cyberpunk 2077", isDark = true)
        val cozyPalette = resolveUniverseVisualPalette("Современный город, романтика", isDark = false)

        val smuggler = resolveNpcPortraitSpec(
            name = "Контрабандист",
            role = "Торговый агент в доках",
            universe = "Ведьмак",
            explicitArchetype = "",
            palette = fantasyPalette
        )
        assertTrue(smuggler is NpcPortraitSpec.PhotoPortrait)
        assertEquals(NpcArchetype.ROGUE, (smuggler as NpcPortraitSpec.PhotoPortrait).archetype)

        val sorceress = resolveNpcPortraitSpec(
            name = "Трисс Меригольд",
            role = "Чародейка",
            universe = "Ведьмак",
            explicitArchetype = "",
            palette = fantasyPalette
        )
        assertTrue(sorceress is NpcPortraitSpec.PhotoPortrait)
        assertEquals(R.drawable.img_npc_fantasy_mage_f, (sorceress as NpcPortraitSpec.PhotoPortrait).resId)

        val merchant = resolveNpcPortraitSpec(
            name = "Берндт",
            role = "Трактирщик",
            universe = "Ведьмак",
            explicitArchetype = "merchant",
            palette = fantasyPalette
        )
        assertTrue(merchant is NpcPortraitSpec.PhotoPortrait)
        assertEquals(R.drawable.img_npc_fantasy_merchant_m, (merchant as NpcPortraitSpec.PhotoPortrait).resId)

        val femaleHacker = resolveNpcPortraitSpec(
            name = "Кира",
            role = "Нетраннер",
            universe = "Cyberpunk 2077",
            explicitArchetype = "cyber",
            palette = cyberPalette
        )
        assertTrue(femaleHacker is NpcPortraitSpec.PhotoPortrait)
        assertEquals(R.drawable.img_npc_cyber_hacker_f, (femaleHacker as NpcPortraitSpec.PhotoPortrait).resId)

        val barista = resolveNpcPortraitSpec(
            name = "Елена",
            role = "Бариста в кофейне",
            universe = "Современный город, романтика",
            explicitArchetype = "",
            palette = cozyPalette
        )
        assertTrue(barista is NpcPortraitSpec.PhotoPortrait)
        assertEquals(R.drawable.img_npc_modern_woman, (barista as NpcPortraitSpec.PhotoPortrait).resId)

        val unnamedCommoner = resolveNpcPortraitSpec(
            name = "Прохожий",
            role = "Житель квартала",
            universe = "Ведьмак",
            explicitArchetype = "commoner",
            palette = fantasyPalette
        )
        assertTrue(unnamedCommoner is NpcPortraitSpec.SilhouetteEmblem)
        assertEquals(NpcArchetype.COMMONER, (unnamedCommoner as NpcPortraitSpec.SilhouetteEmblem).archetype)
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
        assertTrue(defaults.applyFontToWholeUi)
        assertFalse(defaults.focusReadingMode)
        assertFalse(AiModelChoice.entries.any { it.modelId.contains("argon", ignoreCase = true) })
        assertEquals(13, ReadingFontStyle.entries.size)

        // Every font style resolves a valid family, metrics tuning, and updates whole-UI typography when enabled
        ReadingFontStyle.entries.forEach { style ->
            val family = readingFontFamilyFor(style)
            val metrics = fontMetricsFor(style)
            assertTrue(metrics.bodyMultiplier > 0.5f)
            assertTrue(metrics.lineHeightBoost >= 1.0f)

            val wholeUiTypo = buildChroniclesTypography(
                fontStyle = style,
                fontScale = ReadingFontScale.NORMAL,
                applyToWholeUi = true
            )
            assertEquals(family, wholeUiTypo.bodyLarge.fontFamily)
            assertEquals(family, wholeUiTypo.titleMedium.fontFamily)
            assertEquals(family, wholeUiTypo.labelLarge.fontFamily)

            val storyOnlyTypo = buildChroniclesTypography(
                fontStyle = style,
                fontScale = ReadingFontScale.NORMAL,
                applyToWholeUi = false
            )
            assertEquals(family, storyOnlyTypo.bodyLarge.fontFamily)
            assertEquals(InterFontFamily, storyOnlyTypo.bodyMedium.fontFamily)
            assertEquals(InterFontFamily, storyOnlyTypo.labelLarge.fontFamily)
        }
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

    @Test
    fun dynamicGradientAdaptsToLocationUniverseWeatherAndEmotion() {
        val defaults = UserAppSettings()
        assertEquals(PaperThemeMode.DYNAMIC, defaults.themeMode)
        assertEquals(12, PaperThemeMode.entries.size)

        // All 12 themes resolve valid non-crashing gradient specs respecting light/dark surface flags
        PaperThemeMode.entries.forEach { mode ->
            val spec = resolveDynamicGradientSpec(
                themeMode = mode,
                isSystemDark = false,
                context = DynamicThemeContext(location = AppVisualLocation.GAME)
            )
            if (mode != PaperThemeMode.SYSTEM && mode != PaperThemeMode.DYNAMIC) {
                assertEquals(!mode.isLightPalette, spec.isDarkSurface)
            }
        }

        val shelfSpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.DYNAMIC,
            isSystemDark = false,
            context = DynamicThemeContext(location = AppVisualLocation.SHELF)
        )
        val cozyJoySpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.DYNAMIC,
            isSystemDark = false,
            context = DynamicThemeContext(
                location = AppVisualLocation.GAME,
                universe = "Кофейня на углу, романтика",
                weatherMood = WeatherMood.INDOOR_CANDLE,
                activeEmotion = EmotionCategory.JOY
            )
        )
        val cyberRainFearSpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.DYNAMIC,
            isSystemDark = false,
            context = DynamicThemeContext(
                location = AppVisualLocation.GAME,
                universe = "Cyberpunk 2077 (Найт-Сити)",
                weatherMood = WeatherMood.RAIN,
                activeEmotion = EmotionCategory.FEAR
            )
        )
        val lightPaperSpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.LIGHT,
            isSystemDark = true,
            context = DynamicThemeContext(location = AppVisualLocation.GAME)
        )
        val cubicSpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.CUBIC_BIOME,
            isSystemDark = false,
            context = DynamicThemeContext(location = AppVisualLocation.GAME)
        )
        val oledSpec = resolveDynamicGradientSpec(
            themeMode = PaperThemeMode.OLED_OBSIDIAN,
            isSystemDark = false,
            context = DynamicThemeContext(location = AppVisualLocation.GAME)
        )

        assertTrue(shelfSpec.isDarkSurface)
        assertFalse(lightPaperSpec.isDarkSurface)
        assertTrue(cubicSpec.isDarkSurface)
        assertTrue(oledSpec.isDarkSurface)
        assertNotEquals(cubicSpec.topColor, oledSpec.topColor)
        assertNotEquals(shelfSpec.topColor, cozyJoySpec.topColor)
        assertNotEquals(cozyJoySpec.middleColor, cyberRainFearSpec.middleColor)
        assertNotEquals(cozyJoySpec.glowColor, cyberRainFearSpec.glowColor)
    }

    @Test
    fun itemVisualResolverAssignsDistinctPicturesAcrossCategories() {
        val coffee = resolveItemVisualSpec("Термос с латте")
        val blade = resolveItemVisualSpec("Серебряный стилет")
        val potion = resolveItemVisualSpec("Эликсир Ласточка")
        val key = resolveItemVisualSpec("Ключ от архива")
        val chip = resolveItemVisualSpec("Биометрический пропуск")
        val bouquet = resolveItemVisualSpec("Букет белых роз")

        assertEquals(ItemVisualKind.COFFEE_DRINK, coffee.kind)
        assertEquals(ItemVisualKind.BLADE_WEAPON, blade.kind)
        assertEquals(ItemVisualKind.POTION_ELIXIR, potion.kind)
        assertEquals(ItemVisualKind.KEY_ACCESS, key.kind)
        assertEquals(ItemVisualKind.PASS_CHIP, chip.kind)
        assertEquals(ItemVisualKind.FLOWER_GIFT, bouquet.kind)

        assertNotEquals(coffee.bgEnd, potion.bgEnd)
        assertNotEquals(blade.accent, key.accent)
    }

    @Test
    fun aiRepositoryUnwrapsGenericUnknownException() {
        val repo = GameMasterAiRepository()
        val wrapped = IllegalStateException(
            "Something unexpected happened.",
            RuntimeException("Unexpected JSON token at offset 412")
        )
        assertEquals(
            "Unexpected JSON token at offset 412",
            repo.extractDetailedErrorMessage(wrapped)
        )
    }
}
