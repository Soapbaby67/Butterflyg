package com.example.ui

import com.example.core.ChronicleExporter
import com.example.core.describeActiveSoundscape
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.RiskOutcome
import com.example.data.model.RiskOutcomeTier
import com.example.data.model.RiskyChoiceResolver
import com.example.data.model.TurnChoice
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.UserAppSettings
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.WeatherMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioAndRiskyChoicesTest {

    @Test
    fun audioSettingsHaveSaneDefaults() {
        val defaults = UserAppSettings()
        assertTrue(defaults.soundEnabled)
        assertEquals(0.65f, defaults.ostVolume, 0.001f)
        assertEquals(0.80f, defaults.sfxVolume, 0.001f)
    }

    @Test
    fun soundscapeLabelAdaptsToUniverseWeatherAndEmotion() {
        val cyberStorm = describeActiveSoundscape(
            DynamicThemeContext(
                location = AppVisualLocation.GAME,
                universe = "Cyberpunk 2077 (Найт-Сити)",
                weatherMood = WeatherMood.STORM,
                activeEmotion = EmotionCategory.FEAR
            )
        )
        assertTrue(cyberStorm.contains("неон", ignoreCase = true))
        assertTrue(cyberStorm.contains("Гроза", ignoreCase = true))
        assertTrue(cyberStorm.contains("Тревога", ignoreCase = true))

        val shelfLabel = describeActiveSoundscape(
            DynamicThemeContext(location = AppVisualLocation.SHELF)
        )
        assertTrue(shelfLabel.contains("ноктюрн", ignoreCase = true))
    }

    @Test
    fun riskyChoiceResolverDetectsRiskSynthesizesOutcomesAndRollsWeighted() {
        val safeChoice = TurnChoice(
            id = "1",
            approach = "Дипломатический",
            text = "Вежливо спросить дорогу к библиотеке."
        )
        assertFalse(RiskyChoiceResolver.isChoiceRisky(safeChoice))

        val stealthChoice = TurnChoice(
            id = "2",
            approach = "Скрытный",
            text = "Проскользнуть мимо стражи в архив."
        )
        assertTrue(RiskyChoiceResolver.isChoiceRisky(stealthChoice))

        val outcomes = RiskyChoiceResolver.resolveOutcomes(stealthChoice, activeNpcName = "Ирука")
        assertEquals(4, outcomes.size)
        assertTrue(outcomes.any { it.description.contains("Ирука") })

        val (idx, picked) = RiskyChoiceResolver.pickWeightedOutcome(outcomes, Random(42))
        assertTrue(idx in outcomes.indices)
        assertEquals(outcomes[idx], picked)

        val prompt = RiskyChoiceResolver.formatOutcomePrompt(stealthChoice.text, picked)
        assertTrue(prompt.contains(stealthChoice.text))
        assertTrue(prompt.contains(picked.label))
    }

    @Test
    fun aiParserExtractsRiskOutcomesAndActiveQuests() {
        val json = """
            {
              "narrative": "Дождь усиливается над крышами.",
              "hudLocation": "Крыша архива",
              "hudTime": "Полночь, ливень",
              "sceneWeather": "rain",
              "healthPercent": 90,
              "healthStatus": "Сосредоточен",
              "currencyLabel": "120 рё",
              "hudStatusAndInventory": "Здоровье: Сосредоточен (90%)",
              "hudRelationsAndCanon": "Канон: сдвиг",
              "canonDeviationPercent": 18,
              "activeNpc": {
                "hasActiveNpc": false,
                "name": "",
                "emotionLabel": "Спокойствие",
                "emotionCategory": "calm",
                "intensity": 1,
                "note": ""
              },
              "inventory": ["Кунай"],
              "npcs": [],
              "hasButterflyEvent": false,
              "butterflyEventTitle": "",
              "butterflyEventConsequence": "",
              "butterflyEventImpact": "minor",
              "storySummary": "Герой пробирается к архиву.",
              "activeQuests": ["Добыть свиток печатей", "Не попасться АНБУ"],
              "choices": [
                {
                  "id": "1",
                  "approach": "Скрытный",
                  "text": "Взломать окно на чердаке",
                  "isRisky": true,
                  "riskOutcomes": [
                    {
                      "id": "win",
                      "label": "Тихое проникновение",
                      "description": "Замок поддаётся без единого скрипа.",
                      "tier": "triumph",
                      "weight": 30
                    },
                    {
                      "id": "fail",
                      "label": "Хруст стекла",
                      "description": "Тревога поднята по всему двору.",
                      "tier": "setback",
                      "weight": 70
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val repo = GameMasterAiRepository()
        val outcome = repo.parseTurnOutcome(json, turnNumber = 2L)

        assertEquals(listOf("Добыть свиток печатей", "Не попасться АНБУ"), outcome.activeQuests)
        assertEquals(1, outcome.choices.size)
        val choice = outcome.choices.first()
        assertTrue(choice.isRisky)
        assertEquals(2, choice.riskOutcomes.size)
        assertEquals(RiskOutcomeTier.TRIUMPH.key, choice.riskOutcomes[0].tier)
        assertEquals(30L, choice.riskOutcomes[0].weight)
    }

    @Test
    fun chronicleExporterFormatsStoryAndButterflyEvents() {
        val campaign = Campaign(
            id = "c1",
            title = "Огни Новиграда",
            universe = "Ведьмак",
            characterSummary = "Веслав из Оксенфурта",
            canonDeviationPercent = 25L,
            activeQuests = listOf("Найти письмо чародея"),
            butterflyEvents = listOf(
                ButterflyEvent(
                    turnNumber = 2L,
                    title = "Спасён торговец",
                    consequence = "Стража начала искать чужака в доках.",
                    impactLevel = "major"
                )
            )
        )
        val turns = listOf(
            CampaignTurn(turnNumber = 1L, narrative = "Первая глава под проливным дождём."),
            CampaignTurn(turnNumber = 2L, playerAction = "Помочь купцу", narrative = "Вы отбили его у бандитов.")
        )

        val formatted = ChronicleExporter.formatChronicleText(campaign, turns)
        assertTrue(formatted.contains("Огни Новиграда"))
        assertTrue(formatted.contains("Найти письмо чародея"))
        assertTrue(formatted.contains("Глава 1"))
        assertTrue(formatted.contains("Выбор игрока: Помочь купцу"))
        assertTrue(formatted.contains("Спасён торговец"))
    }
}
