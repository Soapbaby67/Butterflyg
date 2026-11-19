package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ChronicleExporter
import com.example.core.RpgAudioEngine
import com.example.core.ScenarioCodeCodec
import com.example.core.SceneNarratorTts
import com.example.core.describeActiveSoundscape
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCategory
import com.example.data.model.PlayerGender
import com.example.data.model.QuestEntry
import com.example.data.model.QuestStatus
import com.example.data.model.RiskOutcomeTier
import com.example.data.model.RiskyChoiceResolver
import com.example.data.model.SessionSetupInput
import com.example.data.model.SkillType
import com.example.data.model.TurnChoice
import com.example.data.model.TurnStateSnapshot
import com.example.data.repository.CampaignExtendedStore
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.NarrativePace
import com.example.data.repository.UserAppSettings
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.WeatherMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun audioSettingsAndNarrativePaceHaveSaneDefaults() {
        val defaults = UserAppSettings()
        assertTrue(defaults.soundEnabled)
        assertEquals(0.65f, defaults.ostVolume, 0.001f)
        assertEquals(0.80f, defaults.sfxVolume, 0.001f)
        assertEquals(NarrativePace.BOOKISH, defaults.narrativePace)
        assertFalse(defaults.autoReadTtsEnabled)
    }

    @Test
    fun audioEngineTracksTtsDuckingStateCleanly() {
        val engine = RpgAudioEngine()
        assertEquals(1.0, engine.currentDuckingMultiplier(), 0.001)
        engine.setTtsDucking(true)
        assertEquals(0.25, engine.currentDuckingMultiplier(), 0.001)
        engine.setTtsDucking(false)
        assertEquals(1.0, engine.currentDuckingMultiplier(), 0.001)
        engine.release()
    }

    @Test
    fun ttsSanitizerAndChunkerCleanNarrativeSafely() {
        val cleaned = SceneNarratorTts.sanitizeForSpeech(
            "[Локация | Ночь] **Глава** *первая* — дождь   стихает."
        )
        assertEquals("Глава первая — дождь стихает.", cleaned)
        val longText = List(120) { "Герой идёт по тихой улице мимо старых фонарей." }.joinToString(" ")
        val chunks = SceneNarratorTts.splitIntoTtsChunks(longText)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.length <= 1850 })
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
    fun riskyChoiceResolverDetectsRiskSynthesizesOutcomesAndAppliesSkillAndItemModifiers() {
        val safeChoice = TurnChoice(
            id = "1",
            approach = "Дипломатический",
            text = "Вежливо спросить дорогу к библиотеке."
        )
        assertFalse(RiskyChoiceResolver.isChoiceRisky(safeChoice))

        val stealthChoice = TurnChoice(
            id = "2",
            approach = "Скрытный",
            text = "Взломать замок и проскользнуть мимо стражи в архив."
        )
        assertTrue(RiskyChoiceResolver.isChoiceRisky(stealthChoice))

        val baseEval = RiskyChoiceResolver.resolveOutcomesWithModifiers(
            choice = stealthChoice,
            activeNpcName = "Ирука",
            skills = HeroSkills(stealth = 4L),
            inventoryItems = emptyList()
        )
        val boostedEval = RiskyChoiceResolver.resolveOutcomesWithModifiers(
            choice = stealthChoice,
            activeNpcName = "Ирука",
            skills = HeroSkills(stealth = 9L),
            inventoryItems = listOf(
                InventoryItem(
                    id = "lockpick",
                    name = "Отмычка мастера",
                    category = ItemCategory.GEAR.key,
                    isEquipped = true
                )
            )
        )

        assertEquals(SkillType.STEALTH, boostedEval.modifier.relevantSkill)
        assertEquals("Отмычка мастера", boostedEval.modifier.matchedItemName)
        assertTrue(boostedEval.modifier.matchedItemEquipped)

        val baseSetback = baseEval.outcomes.first { it.tier == RiskOutcomeTier.SETBACK.key }.weight
        val boostedSetback = boostedEval.outcomes.first { it.tier == RiskOutcomeTier.SETBACK.key }.weight
        assertTrue("High skill + equipped item should reduce setback risk", boostedSetback < baseSetback)
        assertTrue("Setback weight should stay at least 10%", boostedSetback >= 10L)

        val (idx, picked) = RiskyChoiceResolver.pickWeightedOutcome(boostedEval.outcomes, Random(42))
        assertTrue(idx in boostedEval.outcomes.indices)

        val prompt = RiskyChoiceResolver.formatOutcomePrompt(
            actionText = stealthChoice.text,
            outcome = picked,
            modifier = boostedEval.modifier
        )
        assertTrue(prompt.contains(stealthChoice.text))
        assertTrue(prompt.contains(picked.label))
        assertTrue(prompt.contains("Скрытность"))
        assertTrue(prompt.contains("Отмычка мастера"))
    }

    @Test
    fun aiParserExtractsStructuredInventoryQuestsNpcMemoryAndChapterArchive() {
        val json = """
            {
              "narrative": "Дождь усиливается над крышами.",
              "hudLocation": "Крыша архива",
              "hudTime": "Полночь, ливень",
              "sceneWeather": "rain",
              "healthPercent": 88,
              "healthStatus": "Сосредоточен",
              "currencyAmount": 120,
              "currencyLabel": "рё",
              "hudStatusAndInventory": "Здоровье: Сосредоточен (88%)",
              "hudRelationsAndCanon": "Канон: сдвиг",
              "canonDeviationPercent": 18,
              "activeNpc": {
                "hasActiveNpc": true,
                "name": "Ирука",
                "emotionLabel": "Настороженность",
                "emotionCategory": "fear",
                "intensity": 3,
                "note": "Прислушивается к шагам на черепице."
              },
              "inventory": ["Кунай"],
              "inventoryItems": [
                {
                  "id": "kunai_steel",
                  "name": "Стальной кунай",
                  "category": "weapon",
                  "quantity": 3,
                  "isEquipped": true,
                  "description": "Сбалансированный клинок шиноби."
                }
              ],
              "npcs": [
                {
                  "name": "Ирука",
                  "role": "Наставник",
                  "trustLevel": 20,
                  "statusLabel": "Осторожное доверие",
                  "lastEmotion": "Настороженность",
                  "lastEmotionCategory": "fear",
                  "personality": "Заботливый, но строгий",
                  "boundaries": "Не прощает предательства деревни",
                  "archetype": "warrior",
                  "recentActions": ["Предупредил о патруле", "Отдал запасной фонарь"]
                }
              ],
              "hasButterflyEvent": false,
              "butterflyEventTitle": "",
              "butterflyEventConsequence": "",
              "butterflyEventImpact": "minor",
              "storySummary": "Герой пробирается к архиву.",
              "chapterArchiveSummary": "За первые десять ходов герой раскрыл слежку в квартале и заручился поддержкой Ируки.",
              "activeQuests": ["Добыть свиток печатей"],
              "quests": [
                { "id": "q1", "title": "Добыть свиток печатей", "status": "active", "updatedTurn": 10 },
                { "id": "q2", "title": "Уйти от патруля", "status": "completed", "updatedTurn": 9 }
              ],
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
        val outcome = repo.parseTurnOutcome(json, turnNumber = 10L)

        assertEquals(88L, outcome.healthPercent)
        assertEquals(120L, outcome.currencyAmount)
        assertEquals("рё", outcome.currencyLabel)
        assertEquals(1, outcome.inventoryItems.size)
        assertEquals(ItemCategory.WEAPON.key, outcome.inventoryItems.first().category)
        assertTrue(outcome.inventoryItems.first().isEquipped)
        assertEquals(3L, outcome.inventoryItems.first().quantity)
        assertEquals(2, outcome.quests.size)
        assertEquals(QuestStatus.COMPLETED, outcome.quests[1].resolvedStatus())
        assertEquals(2, outcome.npcs.first().recentActions.size)
        assertTrue(outcome.chapterArchiveSummary?.contains("первые десять ходов") == true)
        assertEquals(1, outcome.choices.size)
        assertTrue(outcome.choices.first().isRisky)
    }

    @Test
    fun nextTurnPromptUses7TurnWindowGenderLockAnd10TurnArchiveDirective() {
        val repo = GameMasterAiRepository()
        val campaign = Campaign(
            id = "c_mem",
            title = "Архив Конохи",
            universe = "Наруто",
            playerName = "Алина",
            playerGender = PlayerGender.FEMALE.key,
            skills = HeroSkills(might = 4L, stealth = 8L, insight = 6L, lore = 7L),
            turnCount = 9L,
            chapterSummaries = listOf("Глава 1: прибытие в деревню."),
            storySummary = "Текущая арка — проникновение в архив."
        )
        val turns = (1L..9L).map { idx ->
            CampaignTurn(
                id = "t$idx",
                turnNumber = idx,
                playerAction = "Действие $idx",
                narrative = "Сцена $idx разворачивается под дождём."
            )
        }

        val prompt = repo.buildNextTurnPrompt(
            campaign = campaign,
            recentTurns = turns,
            playerAction = "Открыть дверь архива",
            pinnedFacts = listOf("Обещала вернуть ключ к рассвету"),
            narrativePace = NarrativePace.DEEP,
            isReroll = true
        )

        // Sliding window of 7 turns: turn 3..9 included, turn 1 & 2 omitted
        assertFalse(prompt.contains("--- Ход 1 ---"))
        assertFalse(prompt.contains("--- Ход 2 ---"))
        assertTrue(prompt.contains("--- Ход 3 ---"))
        assertTrue(prompt.contains("--- Ход 9 ---"))
        // Gender + Name lock
        assertTrue(prompt.contains("Алина"))
        assertTrue(prompt.contains("Женский род"))
        // 10-turn archive trigger (turnCount = 9 -> nextTurn = 10)
        assertTrue(prompt.contains("chapterArchiveSummary"))
        // Deep pace & reroll directive
        assertTrue(prompt.contains("ГЛУБОКИЙ"))
        assertTrue(prompt.contains("РЕЖИМ ПЕРЕИГРОВКИ СЦЕНЫ"))
    }

    @Test
    fun scenarioCodeCodecRoundTripsAndRejectsInvalidInput() {
        val setup = SessionSetupInput(
            title = "Ночной Новиград",
            universe = "Ведьмак",
            characterSummary = "Свой персонаж (OC): алхимик-самоучка",
            startingPoint = "Портовый трактир под дождём",
            toneAndDifficulty = "Мрачный реализм",
            playerName = "Мира",
            playerGender = PlayerGender.FEMALE,
            skills = HeroSkills(might = 4L, stealth = 7L, insight = 6L, lore = 8L)
        )
        val code = ScenarioCodeCodec.encode(setup)
        assertTrue(code.startsWith("CBW1:"))

        val decoded = ScenarioCodeCodec.decode(code)
        assertNotNull(decoded)
        assertEquals(setup.title, decoded!!.title)
        assertEquals(setup.universe, decoded.universe)
        assertEquals(setup.playerName, decoded.playerName)
        assertEquals(PlayerGender.FEMALE, decoded.playerGender)
        assertEquals(8L, decoded.skills.lore)

        assertNull(ScenarioCodeCodec.decode("INVALID_CODE"))
        assertNull(ScenarioCodeCodec.decode("CBW1:not-valid-base64!!!"))
    }

    @Test
    fun chronicleExporterFormatsTextMarkdownAndHtml() {
        val campaign = Campaign(
            id = "c1",
            title = "Огни Новиграда",
            universe = "Ведьмак",
            playerName = "Веслав",
            playerGender = PlayerGender.MALE.key,
            characterSummary = "Веслав из Оксенфурта",
            canonDeviationPercent = 25L,
            chapterSummaries = listOf("Веслав прибыл в порт и раскрыл заговор контрабандистов."),
            quests = listOf(
                QuestEntry(id = "q1", title = "Найти письмо чародея", status = QuestStatus.ACTIVE.key, updatedAtTurn = 2L),
                QuestEntry(id = "q2", title = "Спасти купца", status = QuestStatus.COMPLETED.key, updatedAtTurn = 2L)
            ),
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

        val markdown = ChronicleExporter.formatChronicleMarkdown(campaign, turns)
        assertTrue(markdown.startsWith("# Огни Новиграда"))
        assertTrue(markdown.contains("## Архив глав"))
        assertTrue(markdown.contains("- [x] Спасти купца"))
        assertTrue(markdown.contains("### Глава 2"))

        val html = ChronicleExporter.formatChronicleHtml(campaign, turns)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("Оглавление"))
        assertTrue(html.contains("Глава 2"))
        assertTrue(html.contains("Спасён торговец"))
    }

    @Test
    fun legacyFirestorePayloadFilterStripsNewFieldsCleanly() {
        val fullCampaignUpdate: Map<String, Any> = mapOf(
            "turnCount" to 4L,
            "hudLocation" to "Аудитория",
            "hudTime" to "Утро",
            "hudStatusAndInventory" to "Здоровье: 90%",
            "hudRelationsAndCanon" to "Канон 5%",
            "canonDeviationPercent" to 5L,
            "healthPercent" to 90L,
            "currencyAmount" to 400L,
            "currencyLabel" to "руб.",
            "skills" to mapOf("might" to 5L, "stealth" to 5L, "insight" to 6L, "lore" to 7L),
            "inventory" to listOf("Смартфон"),
            "inventoryItems" to listOf(mapOf("name" to "Смартфон")),
            "activeQuests" to listOf("Сдать зачёт"),
            "quests" to listOf(mapOf("title" to "Сдать зачёт")),
            "chapterSummaries" to listOf("Глава 1"),
            "playerName" to "Алиса",
            "playerGender" to "female",
            "storySummary" to "Утренняя лекция."
        )

        val legacyWithQuests = CampaignRepository.filterLegacyCampaignPayload(
            fullCampaignUpdate,
            includeActiveQuests = true
        )
        assertTrue("turnCount" in legacyWithQuests)
        assertTrue("activeQuests" in legacyWithQuests)
        assertFalse("healthPercent" in legacyWithQuests)
        assertFalse("currencyAmount" in legacyWithQuests)
        assertFalse("skills" in legacyWithQuests)
        assertFalse("inventoryItems" in legacyWithQuests)
        assertFalse("quests" in legacyWithQuests)
        assertFalse("chapterSummaries" in legacyWithQuests)
        assertFalse("playerName" in legacyWithQuests)
        assertFalse("playerGender" in legacyWithQuests)

        val minimalLegacy = CampaignRepository.filterLegacyCampaignPayload(
            fullCampaignUpdate,
            includeActiveQuests = false
        )
        assertFalse("activeQuests" in minimalLegacy)
        assertTrue("storySummary" in minimalLegacy)

        val fullTurn: Map<String, Any> = mapOf(
            "userId" to "u1",
            "campaignId" to "c1",
            "turnNumber" to 4L,
            "playerAction" to "Задать вопрос",
            "narrative" to "Преподаватель кивает.",
            "hudLine" to "[Аудитория | Утро | Норма | Канон]",
            "activeNpc" to mapOf("hasActiveNpc" to false),
            "choices" to emptyList<Any>(),
            "stateSnapshot" to mapOf("healthPercent" to 90L)
        )
        val legacyTurn = CampaignRepository.filterLegacyTurnPayload(fullTurn)
        assertTrue("narrative" in legacyTurn)
        assertFalse("stateSnapshot" in legacyTurn)
    }

    @Test
    fun campaignExtendedStorePersistsAndMergesIntoLegacyDocuments() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CampaignExtendedStore(context)
        val cid = "camp_legacy_test"
        store.deleteCampaign(cid)

        val item = InventoryItem(
            id = "phone",
            name = "Смартфон",
            category = ItemCategory.GEAR.key,
            quantity = 1L,
            isEquipped = true,
            description = "Заряд 62%"
        )
        val quest = QuestEntry(
            id = "q_lec",
            title = "Понять тему лекции",
            status = QuestStatus.ACTIVE.key,
            updatedAtTurn = 3L
        )
        val snap = TurnStateSnapshot(
            hudLocation = "Аудитория 304",
            healthPercent = 82L,
            currencyAmount = 400L,
            currencyLabel = "руб.",
            inventoryItems = listOf(item),
            quests = listOf(quest),
            stateChangeSummary = "Записала конспект"
        )

        store.saveCampaignState(
            campaignId = cid,
            playerName = "Алиса",
            playerGender = PlayerGender.FEMALE.key,
            healthPercent = 82L,
            currencyAmount = 400L,
            currencyLabel = "руб.",
            skills = HeroSkills(might = 3L, stealth = 5L, insight = 8L, lore = 7L),
            inventoryItems = listOf(item),
            quests = listOf(quest),
            chapterSummaries = listOf("Ходы 1–10: первое утро в университете."),
            turnSnapshot = 3L to snap
        )

        // Legacy Firestore doc has none of the Stage 1-4 fields
        val legacyDoc = Campaign(
            id = cid,
            title = "Утро в кампусе",
            universe = "Повседневность",
            inventory = listOf("Смартфон")
        )
        val merged = store.mergeInto(legacyDoc)
        assertEquals("Алиса", merged.playerName)
        assertEquals(PlayerGender.FEMALE.key, merged.playerGender)
        assertEquals(82L, merged.healthPercent)
        assertEquals(400L, merged.currencyAmount)
        assertEquals("руб.", merged.currencyLabel)
        assertEquals(8L, merged.skills.insight)
        assertEquals(1, merged.inventoryItems.size)
        assertTrue(merged.inventoryItems.first().isEquipped)
        assertEquals(1, merged.quests.size)
        assertEquals(1, merged.chapterSummaries.size)

        val mergedTurns = store.mergeInto(
            cid,
            listOf(CampaignTurn(id = "t3", turnNumber = 3L, narrative = "Лекция идёт."))
        )
        assertNotNull(mergedTurns.first().stateSnapshot)
        assertEquals(82L, mergedTurns.first().stateSnapshot!!.healthPercent)

        store.deleteCampaign(cid)
        assertNull(store.read(cid))
    }
}

