package com.example.ui

import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.ChapterBookmark
import com.example.data.model.ChapterFilterMode
import com.example.data.model.ChapterSearchEngine
import com.example.data.model.ChronicleAchievementEngine
import com.example.data.model.HeroSkills
import com.example.data.model.NpcRelation
import com.example.data.model.SkillProgressionEngine
import com.example.data.model.SkillType
import com.example.data.model.SkillXpState
import com.example.ui.components.ButterflyBranchGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChroniclesUpgradesTest {

    @Test
    fun skillProgressionClassifiesActionsAcrossAllFourSkills() {
        assertEquals(
            SkillType.STEALTH,
            SkillProgressionEngine.classifyActionSkill("Тихо прокрасться мимо стражи в тень")
        )
        assertEquals(
            SkillType.MIGHT,
            SkillProgressionEngine.classifyActionSkill("Выбить дверь плечом и атаковать в лоб")
        )
        assertEquals(
            SkillType.LORE,
            SkillProgressionEngine.classifyActionSkill("Изучить древний шифр и расшифровать руны")
        )
        assertEquals(
            SkillType.INSIGHT,
            SkillProgressionEngine.classifyActionSkill("Поговорить с трактирщиком и убедить его помочь")
        )
    }

    @Test
    fun skillProgressionCalculatesXpRewardsAndRollsOverLevelsToCap() {
        assertEquals(24, SkillProgressionEngine.calculateXpReward("Осмотреть комнату"))
        assertEquals(
            32,
            SkillProgressionEngine.calculateXpReward(
                "Внимательно осмотреть каждый угол кабинета и изучить пометки на полях старого дневника"
            )
        )
        assertEquals(
            42,
            SkillProgressionEngine.calculateXpReward(
                "Взломать замок [РИСКОВЫЙ ИСХОД: ТРИУМФ]"
            )
        )

        // Level 5 needs 115 XP to reach level 6. Starting with 90 XP + 42 XP = 132 XP -> Level 6 with 17 XP left.
        val levelUpResult = SkillProgressionEngine.evaluateTurnProgression(
            currentSkills = HeroSkills(might = 5L, stealth = 5L, insight = 5L, lore = 5L),
            currentXp = SkillXpState(stealthXp = 90),
            playerAction = "Тихо прокрасться по крыше [РИСКОВЫЙ ИСХОД: ТРИУМФ]",
            turnNumber = 6L
        )
        assertEquals(SkillType.STEALTH, levelUpResult.trainedSkill)
        assertEquals(6L, levelUpResult.updatedSkills.stealth)
        assertEquals(17, levelUpResult.updatedXp.stealthXp)
        assertNotNull(levelUpResult.levelUp)
        assertEquals(5L, levelUpResult.levelUp?.previousLevel)
        assertEquals(6L, levelUpResult.levelUp?.newLevel)
        assertEquals(6L, levelUpResult.levelUp?.turnNumber)

        // Skill already at max level 10 does not exceed 10 and keeps XP at 0
        val cappedResult = SkillProgressionEngine.evaluateTurnProgression(
            currentSkills = HeroSkills(might = 10L),
            currentXp = SkillXpState(mightXp = 0),
            playerAction = "Атаковать противника мечом",
            turnNumber = 18L
        )
        assertEquals(10L, cappedResult.updatedSkills.might)
        assertEquals(0, cappedResult.updatedXp.mightXp)
        assertNull(cappedResult.levelUp)
    }

    @Test
    fun achievementEngineEvaluatesAllTwelveMilestonesDeterministicallyWithoutDuplicates() {
        assertEquals(12, ChronicleAchievementEngine.allDefinitions.size)

        val campaign = Campaign(
            id = "camp_ach_1",
            title = "Огни Новиграда",
            turnCount = 12L,
            skills = HeroSkills(might = 5L, stealth = 7L, insight = 5L, lore = 5L),
            butterflyEvents = listOf(
                ButterflyEvent(
                    turnNumber = 3L,
                    title = "Спасённый гонец",
                    consequence = "Донесение дошло до совета раньше срока",
                    impactLevel = "minor"
                ),
                ButterflyEvent(
                    turnNumber = 8L,
                    title = "Сделка с чародейкой",
                    consequence = "Альянс перекроил баланс сил в городе",
                    impactLevel = "critical"
                )
            ),
            npcs = listOf(
                NpcRelation(name = "Трисс", trustLevel = 80L),
                NpcRelation(name = "Охотник", trustLevel = -60L)
            )
        )

        val turns = listOf(
            CampaignTurn(id = "t1", turnNumber = 1L, playerAction = "Скрытно проскользнуть [РИСКОВЫЙ ИСХОД: ТРИУМФ]"),
            CampaignTurn(id = "t2", turnNumber = 2L, playerAction = "Проверить печать [РИСКОВЫЙ ИСХОД: УСПЕХ С ЦЕНОЙ]"),
            CampaignTurn(id = "t3", turnNumber = 3L, playerAction = "Перехватить письмо [РИСКОВЫЙ ИСХОД: ТРИУМФ]")
        )

        val bookmarks = listOf(
            ChapterBookmark(turnNumber = 1L, excerpt = "Пролог"),
            ChapterBookmark(turnNumber = 3L, excerpt = "Спасённый гонец"),
            ChapterBookmark(turnNumber = 8L, excerpt = "Сделка")
        )

        val firstEval = ChronicleAchievementEngine.evaluate(
            campaign = campaign,
            turns = turns,
            bookmarks = bookmarks,
            existingRecords = emptyList(),
            skillLevelUpCount = 2,
            hasForkedChronicle = true,
            nowMillis = 1_700_000_000_000L
        )

        val unlockedIds = firstEval.statuses.filter { it.isUnlocked }.map { it.def.id }.toSet()
        assertTrue("first_butterfly" in unlockedIds)
        assertTrue("point_of_no_return" in unlockedIds)
        assertTrue("cool_headed_risk" in unlockedIds)
        assertTrue("fate_tempered" in unlockedIds)
        assertTrue("path_of_mastery" in unlockedIds)
        assertTrue("peak_craft" in unlockedIds)
        assertTrue("true_ally" in unlockedIds)
        assertTrue("shadow_of_enmity" in unlockedIds)
        assertTrue("chronicler_10" in unlockedIds)
        assertTrue("crossroads_of_worlds" in unlockedIds)
        assertTrue("marked_pages" in unlockedIds)
        assertFalse("tome_keeper_25" in unlockedIds)
        assertEquals(11, firstEval.newlyUnlocked.size)

        // Second evaluation with persisted records produces zero duplicate newlyUnlocked banners
        val secondEval = ChronicleAchievementEngine.evaluate(
            campaign = campaign,
            turns = turns,
            bookmarks = bookmarks,
            existingRecords = firstEval.updatedRecords,
            skillLevelUpCount = 2,
            hasForkedChronicle = true,
            nowMillis = 1_700_000_005_000L
        )
        assertTrue(secondEval.newlyUnlocked.isEmpty())
        assertEquals(11, secondEval.updatedRecords.size)
    }

    @Test
    fun chapterSearchEngineFiltersByQueryBookmarksAndButterflyEvents() {
        val turns = listOf(
            CampaignTurn(
                id = "t1",
                turnNumber = 1L,
                playerAction = "Зайти в таверну",
                narrative = "В таверне пахло жареным хлебом и смолой. Старый капитан кивнул из угла.",
                hudLine = "Доки | Утро"
            ),
            CampaignTurn(
                id = "t2",
                turnNumber = 2L,
                playerAction = "Расспросить капитана о маяке",
                narrative = "Капитан достал помятую карту северного маяка и прошептал пароль.",
                hudLine = "Таверна | День"
            ),
            CampaignTurn(
                id = "t3",
                turnNumber = 3L,
                playerAction = "Отправиться к маяку в бурю",
                narrative = "Ветер срывал брызги со скал, когда в окне башни вспыхнул зелёный огонь.",
                hudLine = "Северный маяк | Ночь"
            )
        )
        val bookmarks = listOf(ChapterBookmark(turnNumber = 2L, excerpt = "Карта маяка"))
        val events = listOf(
            ButterflyEvent(
                turnNumber = 3L,
                title = "Зелёный сигнал маяка",
                consequence = "Контрабандисты сменили маршрут",
                impactLevel = "major"
            )
        )

        val allMatches = ChapterSearchEngine.filterAndSearch(
            turns = turns,
            bookmarks = bookmarks,
            butterflyEvents = events,
            query = "маяк",
            filterMode = ChapterFilterMode.ALL
        )
        assertEquals(listOf(2L, 3L), allMatches.map { it.turn.turnNumber })
        assertTrue(allMatches.first().isBookmarked)
        assertNotNull(allMatches.last().butterflyEvent)

        val bookmarkOnly = ChapterSearchEngine.filterAndSearch(
            turns = turns,
            bookmarks = bookmarks,
            butterflyEvents = events,
            query = "",
            filterMode = ChapterFilterMode.BOOKMARKS
        )
        assertEquals(listOf(2L), bookmarkOnly.map { it.turn.turnNumber })

        val butterflyOnly = ChapterSearchEngine.filterAndSearch(
            turns = turns,
            bookmarks = bookmarks,
            butterflyEvents = events,
            query = "контрабандисты",
            filterMode = ChapterFilterMode.BUTTERFLY
        )
        assertEquals(listOf(3L), butterflyOnly.map { it.turn.turnNumber })

        val snippet = ChapterSearchEngine.buildSnippet(
            text = "Очень длинное вступление ".repeat(10) + "ключевое слово артефакт находится здесь в тексте.",
            query = "артефакт",
            maxLength = 90
        )
        assertTrue(snippet.contains("артефакт"))
        assertTrue(snippet.startsWith("…"))
    }

    @Test
    fun butterflyBranchGeometryScalesOffsetAndLabelsByImpactTier() {
        val minorOffset = ButterflyBranchGeometry.computeDivergenceOffset("minor")
        val majorOffset = ButterflyBranchGeometry.computeDivergenceOffset("major")
        val criticalOffset = ButterflyBranchGeometry.computeDivergenceOffset("critical")

        assertTrue(minorOffset < majorOffset)
        assertTrue(majorOffset < criticalOffset)
        assertEquals("Лёгкий сдвиг", ButterflyBranchGeometry.tierLabelRu("minor"))
        assertEquals("Серьёзный поворот", ButterflyBranchGeometry.tierLabelRu("major"))
        assertEquals("Точка невозврата", ButterflyBranchGeometry.tierLabelRu("critical"))
    }
}
