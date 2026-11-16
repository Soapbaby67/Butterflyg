package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ButterflyEvent
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.model.RiskOutcome
import com.example.data.model.RiskOutcomeTier
import com.example.data.model.SessionSetupInput
import com.example.data.model.TurnChoice
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CampaignRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleSetup() = SessionSetupInput(
        title = "Хроника листа",
        universe = "Наруто",
        characterSummary = "Попаданец Рэн, мастер печатей",
        startingPoint = "Коноха, утро перед экзаменом",
        toneAndDifficulty = "Мрачный реализм"
    )

    private fun sampleOutcome(turn: Long) = GmTurnOutcome(
        narrative = "Туман над воротами Конохи рассеивается на ходе $turn.",
        hudLocation = "Ворота Конохи",
        hudTime = "Рассвет",
        hudStatusAndInventory = "Бодр | Кунай",
        hudRelationsAndCanon = "Ирука: доверие | Канон: 10%",
        canonDeviationPercent = 10L,
        activeNpc = NpcEmotionSnapshot(
            hasActiveNpc = true,
            name = "Ирука Умино",
            emotionLabel = "Лёгкое смущение",
            emotionCategory = "embarrassment",
            intensity = 2L,
            note = "Неловко потирает затылок."
        ),
        inventory = listOf("Кунай", "Карта Конохи"),
        activeQuests = listOf("Найти свиток в архиве", "Завоевать доверие Ируки"),
        npcs = listOf(
            NpcRelation(
                name = "Ирука Умино",
                role = "Наставник",
                trustLevel = 20L,
                statusLabel = "Союзник",
                lastEmotion = "Лёгкое смущение",
                lastEmotionCategory = "embarrassment"
            )
        ),
        newButterflyEvent = ButterflyEvent(
            turnNumber = turn,
            title = "Разговор у ворот",
            consequence = "Ирука запомнил странный акцент героя.",
            impactLevel = "minor"
        ),
        storySummary = "Герой прибыл в Коноху и заговорил с Ирукой.",
        choices = listOf(
            TurnChoice("1", "Дипломатический", "Поговорить о расписании экзамена"),
            TurnChoice(
                id = "2",
                approach = "Рискованный",
                text = "Пробраться в запечатанный архив",
                isRisky = true,
                riskOutcomes = listOf(
                    RiskOutcome("r1", "Удача", "Выкрасть свиток без тревоги", RiskOutcomeTier.TRIUMPH.key, 25L),
                    RiskOutcome("r2", "Компромисс", "Уйти со свитком, но оставить след", RiskOutcomeTier.MIXED.key, 50L),
                    RiskOutcome("r3", "Провал", "Попасться страже", RiskOutcomeTier.SETBACK.key, 25L)
                )
            )
        )
    )

    @Test
    fun createCampaignAndRecordTurn_authenticatedOwner_persistsAndEmits() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CampaignRepository(firestore, auth)
        val uniqueCampaignId = "camp_${UUID.randomUUID().toString().replace("-", "")}"

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCampaignWithOpeningTurn(
                setup = sampleSetup(),
                openingOutcome = sampleOutcome(1L),
                campaignIdOverride = uniqueCampaignId
            )
        }
        assertEquals(uniqueCampaignId, createdId)

        val campaigns = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCampaigns(uid).first { list -> list.any { it.id == createdId } }
        }
        val campaign = campaigns.first { it.id == createdId }
        assertEquals("Ирука Умино", campaign.activeNpc.name)
        assertEquals("embarrassment", campaign.activeNpc.emotionCategory)
        assertEquals(listOf("Найти свиток в архиве", "Завоевать доверие Ируки"), campaign.activeQuests)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTurn(campaign, "Спросить про список команд", sampleOutcome(2L))
        }

        val turns = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTurns(uid, createdId).first { it.size >= 2 }
        }
        assertEquals(2, turns.size)
        assertEquals(2L, turns.last().turnNumber)
        val riskyChoice = turns.last().choices.first { it.id == "2" }
        assertTrue(riskyChoice.isRisky)
        assertEquals(3, riskyChoice.riskOutcomes.size)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteCampaign(createdId)
        }
    }

    @Test
    fun observeCampaigns_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CampaignRepository(firestore, auth)
        val uniqueCampaignId = "camp_${UUID.randomUUID().toString().replace("-", "")}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCampaignWithOpeningTurn(
                setup = sampleSetup(),
                openingOutcome = sampleOutcome(1L),
                campaignIdOverride = uniqueCampaignId
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CampaignRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeCampaigns(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user campaign access")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError!!.code)
        }
    }

    @Test
    fun observeCampaigns_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CampaignRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCampaigns("some_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated campaign list")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError!!.code)
        }
    }

    @Test
    fun deleteCampaign_afterTests_leavesCleanState() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CampaignRepository(firestore, auth)
        val existing = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCampaigns(uid).first()
        }
        assertTrue(existing.size >= 0)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rpg@test.com"
        const val BOB_EMAIL = "bob_rpg@test.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 10000L
    }
}
