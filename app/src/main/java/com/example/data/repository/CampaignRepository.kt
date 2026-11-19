package com.example.data.repository

import android.content.Context
import com.example.core.FirestoreClientProvider
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.model.QuestEntry
import com.example.data.model.QuestStatus
import com.example.data.model.SessionSetupInput
import com.example.data.model.SkillProgressionEngine
import com.example.data.model.SkillProgressionResult
import com.example.data.model.SkillXpState
import com.example.data.model.TurnChoice
import com.example.data.model.TurnStateSnapshot
import com.example.data.model.UnlockedAchievementRecord
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class GmTurnOutcome(
    val narrative: String,
    val hudLocation: String,
    val hudTime: String,
    val hudStatusAndInventory: String,
    val hudRelationsAndCanon: String,
    val canonDeviationPercent: Long,
    val activeNpc: NpcEmotionSnapshot,
    val inventory: List<String>,
    val npcs: List<NpcRelation>,
    val newButterflyEvent: ButterflyEvent?,
    val storySummary: String,
    val choices: List<TurnChoice>,
    val activeQuests: List<String> = emptyList(),
    val healthPercent: Long = 100L,
    val currencyAmount: Long = 0L,
    val currencyLabel: String = "",
    val skills: HeroSkills = HeroSkills(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val quests: List<QuestEntry> = emptyList(),
    val chapterArchiveSummary: String? = null,
    val stateChangeSummary: String = ""
) {
    val hudLine: String
        get() = "[$hudLocation | $hudTime | $hudStatusAndInventory | $hudRelationsAndCanon]"

    fun resolvedInventoryItems(): List<InventoryItem> {
        if (inventoryItems.isNotEmpty()) return inventoryItems
        return inventory
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .mapIndexed { idx, name -> InventoryItem.fromLegacyName(name, idx) }
    }

    fun resolvedQuests(turnNumber: Long): List<QuestEntry> {
        if (quests.isNotEmpty()) return quests
        return activeQuests
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .mapIndexed { idx, title ->
                QuestEntry(
                    id = "quest_${turnNumber}_$idx",
                    title = title,
                    status = QuestStatus.ACTIVE.key,
                    updatedAtTurn = turnNumber
                )
            }
    }
}

class CampaignRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val extendedStore: CampaignExtendedStore? = null
) {
    constructor(context: Context) : this(
        db = FirestoreClientProvider.getDatabase(context),
        auth = Firebase.auth,
        extendedStore = CampaignExtendedStore.getInstance(context)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Требуется вход через Google-аккаунт для доступа к хроникам.")
    }

    private fun campaignsCollection(uid: String) =
        db.collection("users").document(uid).collection("campaigns")

    private fun turnsCollection(uid: String, campaignId: String) =
        campaignsCollection(uid).document(campaignId).collection("turns")

    fun observeCampaigns(userId: String): Flow<List<Campaign>> = flow {
        val path = "users/$userId/campaigns"
        val baseFlow = campaignsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Campaign::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
        val mergedFlow = if (extendedStore != null) {
            combine(baseFlow, extendedStore.revision) { list, _ ->
                list.map { extendedStore.mergeInto(it) }
            }
        } else {
            baseFlow
        }
        emitAll(mergedFlow)
    }

    fun observeCampaign(userId: String, campaignId: String): Flow<Campaign?> = flow {
        val docRef = campaignsCollection(userId).document(campaignId)
        val baseFlow = docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        Campaign::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
        val mergedFlow = if (extendedStore != null) {
            combine(baseFlow, extendedStore.revision) { campaign, _ ->
                campaign?.let { extendedStore.mergeInto(it) }
            }
        } else {
            baseFlow
        }
        emitAll(mergedFlow)
    }

    fun observeTurns(userId: String, campaignId: String): Flow<List<CampaignTurn>> = flow {
        val path = "users/$userId/campaigns/$campaignId/turns"
        val baseFlow = turnsCollection(userId, campaignId)
            .orderBy("turnNumber", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CampaignTurn::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
        val mergedFlow = if (extendedStore != null) {
            combine(baseFlow, extendedStore.revision) { turns, _ ->
                extendedStore.mergeInto(campaignId, turns)
            }
        } else {
            baseFlow
        }
        emitAll(mergedFlow)
    }

    fun observeExtendedState(campaignId: String): Flow<CampaignExtendedState> {
        val store = extendedStore ?: return flow { emit(CampaignExtendedState()) }
        return store.revision.map { store.read(campaignId) ?: CampaignExtendedState() }
    }

    fun toggleChapterBookmark(campaignId: String, turnNumber: Long, excerpt: String): Boolean {
        return extendedStore?.toggleBookmark(campaignId, turnNumber, excerpt) ?: false
    }

    fun saveUnlockedAchievements(campaignId: String, records: List<UnlockedAchievementRecord>) {
        extendedStore?.saveCampaignState(
            campaignId = campaignId,
            unlockedAchievements = records
        )
    }

    suspend fun getCampaign(campaignId: String): Campaign? {
        val uid = requireUserId()
        val docRef = campaignsCollection(uid).document(campaignId)
        return try {
            val snap = docRef.get().await()
            val campaign = snap.toObject(
                Campaign::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            campaign?.let { extendedStore?.mergeInto(it) ?: it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createCampaignWithOpeningTurn(
        setup: SessionSetupInput,
        openingOutcome: GmTurnOutcome,
        campaignIdOverride: String? = null
    ): String {
        val uid = requireUserId()
        val campaignRef = if (campaignIdOverride != null) {
            campaignsCollection(uid).document(campaignIdOverride)
        } else {
            campaignsCollection(uid).document()
        }
        val campaignId = campaignRef.id
        val turnRef = turnsCollection(uid, campaignId).document()

        val initialEvents = openingOutcome.newButterflyEvent
            ?.let { listOf(it.copy(turnNumber = 1L)) }
            ?: emptyList()

        val structuredItems = openingOutcome.resolvedInventoryItems().take(60)
        val flatInventory = structuredItems.map { it.name.take(120) }.ifEmpty {
            openingOutcome.inventory.take(60).map { it.take(120) }
        }
        val structuredQuests = openingOutcome.resolvedQuests(1L).take(30)
        val activeQuestTitles = structuredQuests
            .filter { it.resolvedStatus() == QuestStatus.ACTIVE }
            .map { it.title.take(180) }
            .take(12)
        val resolvedSkills = setup.skills.coerced()

        val snapshot = TurnStateSnapshot(
            hudLocation = openingOutcome.hudLocation,
            hudTime = openingOutcome.hudTime,
            hudStatusAndInventory = openingOutcome.hudStatusAndInventory,
            hudRelationsAndCanon = openingOutcome.hudRelationsAndCanon,
            canonDeviationPercent = openingOutcome.canonDeviationPercent,
            healthPercent = openingOutcome.healthPercent,
            currencyAmount = openingOutcome.currencyAmount,
            currencyLabel = openingOutcome.currencyLabel,
            skills = resolvedSkills,
            inventory = flatInventory,
            inventoryItems = structuredItems,
            npcs = openingOutcome.npcs.take(60),
            butterflyEvents = initialEvents,
            activeQuests = activeQuestTitles,
            quests = structuredQuests,
            storySummary = openingOutcome.storySummary,
            chapterSummaries = emptyList(),
            stateChangeSummary = openingOutcome.stateChangeSummary
        )

        val campaignPayload = mapOf(
            "userId" to uid,
            "title" to setup.title.trim().ifBlank { setup.universe.take(48) }.take(120),
            "universe" to setup.universe.trim().take(300),
            "characterSummary" to setup.characterSummary.trim().take(600),
            "playerName" to setup.playerName.trim().take(80),
            "playerGender" to setup.playerGender.key,
            "startingPoint" to setup.startingPoint.trim().take(400),
            "toneAndDifficulty" to setup.toneAndDifficulty.trim().take(300),
            "turnCount" to 1L,
            "hudLocation" to openingOutcome.hudLocation.take(200),
            "hudTime" to openingOutcome.hudTime.take(120),
            "hudStatusAndInventory" to openingOutcome.hudStatusAndInventory.take(300),
            "hudRelationsAndCanon" to openingOutcome.hudRelationsAndCanon.take(300),
            "canonDeviationPercent" to openingOutcome.canonDeviationPercent.coerceIn(0L, 100L),
            "healthPercent" to openingOutcome.healthPercent.coerceIn(0L, 100L),
            "currencyAmount" to openingOutcome.currencyAmount.coerceIn(0L, 100_000_000L),
            "currencyLabel" to openingOutcome.currencyLabel.take(80),
            "skills" to resolvedSkills.toFirestoreMap(),
            "activeNpc" to openingOutcome.activeNpc.toFirestoreMap(),
            "inventory" to flatInventory,
            "inventoryItems" to structuredItems.map { it.toFirestoreMap() },
            "npcs" to openingOutcome.npcs.take(60).map { it.toFirestoreMap() },
            "butterflyEvents" to initialEvents.map { it.toFirestoreMap() },
            "activeQuests" to activeQuestTitles,
            "quests" to structuredQuests.map { it.toFirestoreMap() },
            "storySummary" to openingOutcome.storySummary.take(4000),
            "chapterSummaries" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val turnPayload = mapOf(
            "userId" to uid,
            "campaignId" to campaignId,
            "turnNumber" to 1L,
            "playerAction" to "Начало хроники",
            "narrative" to openingOutcome.narrative.ifBlank { "История начинается." }.take(12000),
            "hudLine" to openingOutcome.hudLine.take(600),
            "activeNpc" to openingOutcome.activeNpc.toFirestoreMap(),
            "choices" to openingOutcome.choices.take(6).map { it.toFirestoreMap() },
            "stateSnapshot" to snapshot.toFirestoreMap(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            runWithLegacyFallback(
                primary = {
                    db.runBatch { batch ->
                        batch.set(campaignRef, campaignPayload)
                        batch.set(turnRef, turnPayload)
                    }.await()
                },
                legacy = {
                    db.runBatch { batch ->
                        batch.set(campaignRef, filterLegacyCampaignPayload(campaignPayload, includeActiveQuests = true))
                        batch.set(turnRef, filterLegacyTurnPayload(turnPayload))
                    }.await()
                },
                minimalLegacy = {
                    db.runBatch { batch ->
                        batch.set(campaignRef, filterLegacyCampaignPayload(campaignPayload, includeActiveQuests = false))
                        batch.set(turnRef, filterLegacyTurnPayload(turnPayload))
                    }.await()
                }
            )
            extendedStore?.saveCampaignState(
                campaignId = campaignId,
                playerName = setup.playerName.trim().take(80),
                playerGender = setup.playerGender.key,
                healthPercent = openingOutcome.healthPercent.coerceIn(0L, 100L),
                currencyAmount = openingOutcome.currencyAmount.coerceIn(0L, 100_000_000L),
                currencyLabel = openingOutcome.currencyLabel.take(80),
                skills = resolvedSkills,
                inventoryItems = structuredItems,
                quests = structuredQuests,
                chapterSummaries = emptyList(),
                turnSnapshot = 1L to snapshot
            )
            campaignId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, campaignRef.path)
            throw e
        }
    }

    suspend fun recordTurn(
        campaign: Campaign,
        playerAction: String,
        outcome: GmTurnOutcome,
        choiceApproach: String = ""
    ): SkillProgressionResult {
        val uid = requireUserId()
        require(campaign.id.isNotBlank()) { "Campaign ID must not be blank" }
        val nextTurnNumber = campaign.turnCount + 1L
        val campaignRef = campaignsCollection(uid).document(campaign.id)
        val turnRef = turnsCollection(uid, campaign.id).document()

        val updatedEvents = buildList {
            addAll(campaign.butterflyEvents)
            if (outcome.newButterflyEvent != null) {
                add(outcome.newButterflyEvent.copy(turnNumber = nextTurnNumber))
            }
        }.takeLast(60)

        val updatedChapters = buildList {
            addAll(campaign.chapterSummaries)
            val archive = outcome.chapterArchiveSummary?.trim().orEmpty()
            if (archive.isNotBlank()) {
                val rangeStart = (nextTurnNumber - 9L).coerceAtLeast(1L)
                val formatted = if (archive.startsWith("Ходы", ignoreCase = true) || archive.startsWith("Глава", ignoreCase = true)) {
                    archive
                } else {
                    "Ходы $rangeStart–$nextTurnNumber: $archive"
                }
                add(formatted.take(500))
            }
        }.takeLast(40)

        val structuredItems = mergeInventoryPreserveEquipment(
            previous = campaign.resolvedInventoryItems(),
            incoming = outcome.resolvedInventoryItems()
        ).take(60)
        val flatInventory = structuredItems.map { it.name.take(120) }

        val mergedQuests = mergeQuests(
            previous = campaign.resolvedQuests(),
            incoming = outcome.resolvedQuests(nextTurnNumber),
            turnNumber = nextTurnNumber
        ).take(30)
        val activeQuestTitles = mergedQuests
            .filter { it.resolvedStatus() == QuestStatus.ACTIVE }
            .map { it.title.take(180) }
            .take(12)

        val existingExtended = extendedStore?.read(campaign.id) ?: CampaignExtendedState()
        val baseSkills = campaign.skills.coerced()
        val progression = SkillProgressionEngine.evaluateTurnProgression(
            currentSkills = baseSkills,
            currentXp = existingExtended.skillXp,
            playerAction = playerAction,
            turnNumber = nextTurnNumber,
            choiceApproach = choiceApproach
        )
        val updatedSkills = progression.updatedSkills.coerced()
        val nextLevelUpCount = existingExtended.skillLevelUpCount +
            if (progression.levelUp != null) 1 else 0

        val snapshot = TurnStateSnapshot(
            hudLocation = outcome.hudLocation,
            hudTime = outcome.hudTime,
            hudStatusAndInventory = outcome.hudStatusAndInventory,
            hudRelationsAndCanon = outcome.hudRelationsAndCanon,
            canonDeviationPercent = outcome.canonDeviationPercent,
            healthPercent = outcome.healthPercent,
            currencyAmount = outcome.currencyAmount,
            currencyLabel = outcome.currencyLabel,
            skills = updatedSkills,
            inventory = flatInventory,
            inventoryItems = structuredItems,
            npcs = outcome.npcs.take(60),
            butterflyEvents = updatedEvents,
            activeQuests = activeQuestTitles,
            quests = mergedQuests,
            storySummary = outcome.storySummary,
            chapterSummaries = updatedChapters,
            stateChangeSummary = outcome.stateChangeSummary
        )

        val campaignUpdate = mapOf(
            "turnCount" to nextTurnNumber,
            "hudLocation" to outcome.hudLocation.take(200),
            "hudTime" to outcome.hudTime.take(120),
            "hudStatusAndInventory" to outcome.hudStatusAndInventory.take(300),
            "hudRelationsAndCanon" to outcome.hudRelationsAndCanon.take(300),
            "canonDeviationPercent" to outcome.canonDeviationPercent.coerceIn(0L, 100L),
            "healthPercent" to outcome.healthPercent.coerceIn(0L, 100L),
            "currencyAmount" to outcome.currencyAmount.coerceIn(0L, 100_000_000L),
            "currencyLabel" to outcome.currencyLabel.take(80),
            "skills" to updatedSkills.toFirestoreMap(),
            "activeNpc" to outcome.activeNpc.toFirestoreMap(),
            "inventory" to flatInventory,
            "inventoryItems" to structuredItems.map { it.toFirestoreMap() },
            "npcs" to outcome.npcs.take(60).map { it.toFirestoreMap() },
            "butterflyEvents" to updatedEvents.map { it.toFirestoreMap() },
            "activeQuests" to activeQuestTitles,
            "quests" to mergedQuests.map { it.toFirestoreMap() },
            "storySummary" to outcome.storySummary.take(4000),
            "chapterSummaries" to updatedChapters,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val turnPayload = mapOf(
            "userId" to uid,
            "campaignId" to campaign.id,
            "turnNumber" to nextTurnNumber,
            "playerAction" to playerAction.trim().take(1200),
            "narrative" to outcome.narrative.ifBlank { "Сцена продолжается." }.take(12000),
            "hudLine" to outcome.hudLine.take(600),
            "activeNpc" to outcome.activeNpc.toFirestoreMap(),
            "choices" to outcome.choices.take(6).map { it.toFirestoreMap() },
            "stateSnapshot" to snapshot.toFirestoreMap(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            runWithLegacyFallback(
                primary = {
                    db.runBatch { batch ->
                        batch.update(campaignRef, campaignUpdate)
                        batch.set(turnRef, turnPayload)
                    }.await()
                },
                legacy = {
                    db.runBatch { batch ->
                        batch.update(campaignRef, filterLegacyCampaignPayload(campaignUpdate, includeActiveQuests = true))
                        batch.set(turnRef, filterLegacyTurnPayload(turnPayload))
                    }.await()
                },
                minimalLegacy = {
                    db.runBatch { batch ->
                        batch.update(campaignRef, filterLegacyCampaignPayload(campaignUpdate, includeActiveQuests = false))
                        batch.set(turnRef, filterLegacyTurnPayload(turnPayload))
                    }.await()
                }
            )
            extendedStore?.saveCampaignState(
                campaignId = campaign.id,
                healthPercent = outcome.healthPercent.coerceIn(0L, 100L),
                currencyAmount = outcome.currencyAmount.coerceIn(0L, 100_000_000L),
                currencyLabel = outcome.currencyLabel.take(80),
                skills = updatedSkills,
                skillXp = progression.updatedXp,
                skillLevelUpCount = nextLevelUpCount,
                inventoryItems = structuredItems,
                quests = mergedQuests,
                chapterSummaries = updatedChapters,
                turnSnapshot = nextTurnNumber to snapshot
            )
            return progression
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, campaignRef.path)
            throw e
        }
    }

    /**
     * Atomically deletes the last turn in a campaign and restores Campaign state from the previous turn's
     * stateSnapshot (or HUD fallback for legacy turns). Returns the removed turn's playerAction.
     */
    suspend fun rollbackLastTurn(campaignId: String): String? {
        val uid = requireUserId()
        require(campaignId.isNotBlank()) { "Campaign ID must not be blank" }
        val campaignRef = campaignsCollection(uid).document(campaignId)
        val turnsRef = turnsCollection(uid, campaignId)

        return try {
            val campaignSnap = campaignRef.get().await()
            val rawCampaign = campaignSnap.toObject(
                Campaign::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Кампания не найдена.")
            val campaign = extendedStore?.mergeInto(rawCampaign) ?: rawCampaign

            val turnsSnap = turnsRef.orderBy("turnNumber", Query.Direction.ASCENDING).get().await()
            val docs = turnsSnap.documents
            if (docs.size < 2) {
                return null
            }

            val lastDoc = docs.last()
            val previousDoc = docs[docs.size - 2]
            val lastTurn = lastDoc.toObject(CampaignTurn::class.java)
            val rawPrevTurn = previousDoc.toObject(CampaignTurn::class.java)
                ?: throw IllegalStateException("Не удалось прочитать предыдущий ход для отката.")
            val prevTurn = extendedStore?.mergeInto(campaignId, listOf(rawPrevTurn))?.firstOrNull()
                ?: rawPrevTurn

            val restoreUpdate = buildCampaignRestoreMap(campaign, prevTurn)

            runWithLegacyFallback(
                primary = {
                    db.runBatch { batch ->
                        batch.delete(lastDoc.reference)
                        batch.update(campaignRef, restoreUpdate)
                    }.await()
                },
                legacy = {
                    db.runBatch { batch ->
                        batch.delete(lastDoc.reference)
                        batch.update(campaignRef, filterLegacyCampaignPayload(restoreUpdate, includeActiveQuests = true))
                    }.await()
                },
                minimalLegacy = {
                    db.runBatch { batch ->
                        batch.delete(lastDoc.reference)
                        batch.update(campaignRef, filterLegacyCampaignPayload(restoreUpdate, includeActiveQuests = false))
                    }.await()
                }
            )

            val snap = prevTurn.stateSnapshot
            extendedStore?.saveCampaignState(
                campaignId = campaignId,
                healthPercent = snap?.healthPercent ?: campaign.healthPercent,
                currencyAmount = snap?.currencyAmount ?: campaign.currencyAmount,
                currencyLabel = snap?.currencyLabel ?: campaign.currencyLabel,
                skills = snap?.skills ?: campaign.skills,
                inventoryItems = snap?.inventoryItems ?: campaign.resolvedInventoryItems(),
                quests = snap?.quests ?: campaign.resolvedQuests(),
                chapterSummaries = snap?.chapterSummaries ?: campaign.chapterSummaries,
                pruneSnapshotsAfterTurn = prevTurn.turnNumber
            )

            lastTurn?.playerAction?.takeIf { it.isNotBlank() && it != "Начало хроники" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, campaignRef.path)
            throw e
        }
    }

    /**
     * Forks a chronicle at any past turnNumber, cloning the campaign and turns 1..upToTurnNumber into a
     * brand-new Campaign slot so the player can explore an alternate Butterfly Effect branch.
     */
    suspend fun duplicateCampaignAtTurn(
        sourceCampaignId: String,
        upToTurnNumber: Long,
        branchSuffix: String = " (ветка)"
    ): String {
        val uid = requireUserId()
        require(sourceCampaignId.isNotBlank()) { "Source campaign ID must not be blank" }
        val sourceCampaignRef = campaignsCollection(uid).document(sourceCampaignId)

        return try {
            val rawSource = sourceCampaignRef.get().await().toObject(
                Campaign::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Исходная хроника не найдена.")
            val sourceCampaign = extendedStore?.mergeInto(rawSource) ?: rawSource

            val turnsSnap = turnsCollection(uid, sourceCampaignId)
                .orderBy("turnNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            val allSourceTurns = turnsSnap.documents
                .mapNotNull { it.toObject(CampaignTurn::class.java) }
                .let { list -> extendedStore?.mergeInto(sourceCampaignId, list) ?: list }

            val turnsToCopy = allSourceTurns
                .filter { it.turnNumber <= upToTurnNumber }
                .ifEmpty { allSourceTurns.take(1) }

            val targetTurn = turnsToCopy.lastOrNull()
                ?: throw IllegalStateException("В хронике нет ходов для создания ветки.")

            val newCampaignRef = campaignsCollection(uid).document()
            val newCampaignId = newCampaignRef.id
            val newTitle = buildForkTitle(sourceCampaign.title, targetTurn.turnNumber, branchSuffix)

            val snap = targetTurn.stateSnapshot
            val restoredEvents = snap?.butterflyEvents ?: sourceCampaign.butterflyEvents
                .filter { it.turnNumber <= targetTurn.turnNumber }
            val restoredInventoryItems = snap?.inventoryItems ?: sourceCampaign.resolvedInventoryItems()
            val restoredFlatInventory = snap?.inventory?.ifEmpty { restoredInventoryItems.map { it.name } }
                ?: sourceCampaign.inventory
            val restoredQuests = snap?.quests ?: sourceCampaign.resolvedQuests()
            val restoredActiveQuests = snap?.activeQuests ?: sourceCampaign.activeQuests

            val (fallbackLoc, fallbackTime, fallbackStatus, fallbackRel) = parseHudLineParts(
                targetTurn.hudLine,
                sourceCampaign
            )

            val newCampaignPayload = mapOf(
                "userId" to uid,
                "title" to newTitle.take(120),
                "universe" to sourceCampaign.universe.take(300),
                "characterSummary" to sourceCampaign.characterSummary.take(600),
                "playerName" to sourceCampaign.playerName.take(80),
                "playerGender" to sourceCampaign.resolvedGender().key,
                "startingPoint" to sourceCampaign.startingPoint.take(400),
                "toneAndDifficulty" to sourceCampaign.toneAndDifficulty.take(300),
                "turnCount" to targetTurn.turnNumber.coerceAtLeast(1L),
                "hudLocation" to (snap?.hudLocation ?: fallbackLoc).take(200),
                "hudTime" to (snap?.hudTime ?: fallbackTime).take(120),
                "hudStatusAndInventory" to (snap?.hudStatusAndInventory ?: fallbackStatus).take(300),
                "hudRelationsAndCanon" to (snap?.hudRelationsAndCanon ?: fallbackRel).take(300),
                "canonDeviationPercent" to (snap?.canonDeviationPercent ?: sourceCampaign.canonDeviationPercent).coerceIn(0L, 100L),
                "healthPercent" to (snap?.healthPercent ?: sourceCampaign.healthPercent.coerceIn(-1L, 100L)),
                "currencyAmount" to (snap?.currencyAmount ?: sourceCampaign.currencyAmount).coerceIn(0L, 100_000_000L),
                "currencyLabel" to (snap?.currencyLabel ?: sourceCampaign.currencyLabel).take(80),
                "skills" to (snap?.skills ?: sourceCampaign.skills).toFirestoreMap(),
                "activeNpc" to targetTurn.activeNpc.toFirestoreMap(),
                "inventory" to restoredFlatInventory.take(60).map { it.take(120) },
                "inventoryItems" to restoredInventoryItems.take(60).map { it.toFirestoreMap() },
                "npcs" to (snap?.npcs ?: sourceCampaign.npcs).take(60).map { it.toFirestoreMap() },
                "butterflyEvents" to restoredEvents.take(60).map { it.toFirestoreMap() },
                "activeQuests" to restoredActiveQuests.take(12).map { it.take(180) },
                "quests" to restoredQuests.take(30).map { it.toFirestoreMap() },
                "storySummary" to (snap?.storySummary ?: sourceCampaign.storySummary).take(4000),
                "chapterSummaries" to (snap?.chapterSummaries ?: sourceCampaign.chapterSummaries).take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Write campaign + first batch of turns atomically, then remaining turns in chunks
            val firstBatchTurns = turnsToCopy.take(350)
            var useLegacyTurnClone = false
            runWithLegacyFallback(
                primary = {
                    db.runBatch { batch ->
                        batch.set(newCampaignRef, newCampaignPayload)
                        firstBatchTurns.forEach { turn ->
                            val newTurnRef = turnsCollection(uid, newCampaignId).document()
                            batch.set(newTurnRef, buildTurnPayloadForClone(uid, newCampaignId, turn))
                        }
                    }.await()
                },
                legacy = {
                    useLegacyTurnClone = true
                    db.runBatch { batch ->
                        batch.set(newCampaignRef, filterLegacyCampaignPayload(newCampaignPayload, includeActiveQuests = true))
                        firstBatchTurns.forEach { turn ->
                            val newTurnRef = turnsCollection(uid, newCampaignId).document()
                            batch.set(newTurnRef, filterLegacyTurnPayload(buildTurnPayloadForClone(uid, newCampaignId, turn)))
                        }
                    }.await()
                },
                minimalLegacy = {
                    useLegacyTurnClone = true
                    db.runBatch { batch ->
                        batch.set(newCampaignRef, filterLegacyCampaignPayload(newCampaignPayload, includeActiveQuests = false))
                        firstBatchTurns.forEach { turn ->
                            val newTurnRef = turnsCollection(uid, newCampaignId).document()
                            batch.set(newTurnRef, filterLegacyTurnPayload(buildTurnPayloadForClone(uid, newCampaignId, turn)))
                        }
                    }.await()
                }
            )

            if (turnsToCopy.size > 350) {
                for (chunk in turnsToCopy.drop(350).chunked(350)) {
                    db.runBatch { batch ->
                        chunk.forEach { turn ->
                            val newTurnRef = turnsCollection(uid, newCampaignId).document()
                            val payload = buildTurnPayloadForClone(uid, newCampaignId, turn)
                            batch.set(newTurnRef, if (useLegacyTurnClone) filterLegacyTurnPayload(payload) else payload)
                        }
                    }.await()
                }
            }

            val sourceExt = extendedStore?.read(sourceCampaignId)
            extendedStore?.saveCampaignState(
                campaignId = sourceCampaignId,
                hasForkedChronicle = true
            )
            extendedStore?.saveCampaignState(
                campaignId = newCampaignId,
                playerName = sourceCampaign.playerName.take(80),
                playerGender = sourceCampaign.resolvedGender().key,
                healthPercent = snap?.healthPercent ?: sourceCampaign.healthPercent.coerceIn(-1L, 100L),
                currencyAmount = (snap?.currencyAmount ?: sourceCampaign.currencyAmount).coerceIn(0L, 100_000_000L),
                currencyLabel = (snap?.currencyLabel ?: sourceCampaign.currencyLabel).take(80),
                skills = snap?.skills ?: sourceCampaign.skills,
                skillXp = sourceExt?.skillXp ?: SkillXpState(),
                skillLevelUpCount = sourceExt?.skillLevelUpCount ?: 0,
                inventoryItems = restoredInventoryItems.take(60),
                quests = restoredQuests.take(30),
                chapterSummaries = (snap?.chapterSummaries ?: sourceCampaign.chapterSummaries).take(40),
                bookmarks = sourceExt?.bookmarks?.filter { it.turnNumber <= targetTurn.turnNumber }.orEmpty(),
                hasForkedChronicle = true
            )
            val clonedSnapshots = turnsToCopy.mapNotNull { t -> t.stateSnapshot?.let { t.turnNumber to it } }.toMap()
            if (clonedSnapshots.isNotEmpty()) {
                extendedStore?.saveSnapshots(newCampaignId, clonedSnapshots)
            }

            newCampaignId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sourceCampaignRef.path)
            throw e
        }
    }

    /**
     * Equips or unequips a structured inventory item directly without spending a game turn.
     */
    suspend fun toggleItemEquipped(
        campaign: Campaign,
        itemIdOrName: String
    ): List<InventoryItem> {
        val uid = requireUserId()
        require(campaign.id.isNotBlank()) { "Campaign ID must not be blank" }
        val currentItems = campaign.resolvedInventoryItems()
        val updatedItems = currentItems.map { item ->
            if (item.resolvedId().equals(itemIdOrName, ignoreCase = true) ||
                item.name.equals(itemIdOrName, ignoreCase = true)
            ) {
                item.copy(isEquipped = !item.isEquipped)
            } else {
                item
            }
        }
        val campaignRef = campaignsCollection(uid).document(campaign.id)
        val fullUpdate = mapOf(
            "inventoryItems" to updatedItems.take(60).map { it.toFirestoreMap() },
            "inventory" to updatedItems.take(60).map { it.name.take(120) },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            runWithLegacyFallback(
                primary = { campaignRef.update(fullUpdate).await() },
                legacy = {
                    campaignRef.update(
                        mapOf(
                            "inventory" to updatedItems.take(60).map { it.name.take(120) },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            )
            extendedStore?.saveCampaignState(
                campaignId = campaign.id,
                inventoryItems = updatedItems.take(60)
            )
            return updatedItems
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, campaignRef.path)
            throw e
        }
    }

    suspend fun deleteCampaign(campaignId: String) {
        val uid = requireUserId()
        require(campaignId.isNotBlank()) { "Campaign ID must not be blank" }
        val campaignRef = campaignsCollection(uid).document(campaignId)
        val turnsRef = turnsCollection(uid, campaignId)
        try {
            val turnsSnap = turnsRef.get().await()
            val docs = turnsSnap.documents
            for (chunk in docs.chunked(400)) {
                db.runBatch { batch ->
                    chunk.forEach { batch.delete(it.reference) }
                }.await()
            }
            campaignRef.delete().await()
            extendedStore?.deleteCampaign(campaignId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, campaignRef.path)
            throw e
        }
    }

    private fun buildTurnPayloadForClone(
        uid: String,
        newCampaignId: String,
        turn: CampaignTurn
    ): Map<String, Any> = buildMap {
        put("userId", uid)
        put("campaignId", newCampaignId)
        put("turnNumber", turn.turnNumber.coerceAtLeast(1L))
        put("playerAction", turn.playerAction.take(1200))
        put("narrative", turn.narrative.take(12000))
        put("hudLine", turn.hudLine.take(600))
        put("activeNpc", turn.activeNpc.toFirestoreMap())
        put("choices", turn.choices.take(6).map { it.toFirestoreMap() })
        turn.stateSnapshot?.let { put("stateSnapshot", it.toFirestoreMap()) }
        put("createdAt", FieldValue.serverTimestamp())
    }

    private fun buildCampaignRestoreMap(
        campaign: Campaign,
        prevTurn: CampaignTurn
    ): Map<String, Any> {
        val snap = prevTurn.stateSnapshot
        val (fallbackLoc, fallbackTime, fallbackStatus, fallbackRel) = parseHudLineParts(
            prevTurn.hudLine,
            campaign
        )
        val restoredEvents = snap?.butterflyEvents ?: campaign.butterflyEvents
            .filter { it.turnNumber <= prevTurn.turnNumber }
        val restoredItems = snap?.inventoryItems ?: campaign.resolvedInventoryItems()
        val restoredFlatInventory = snap?.inventory?.ifEmpty { restoredItems.map { it.name } }
            ?: campaign.inventory
        val restoredQuests = snap?.quests ?: campaign.resolvedQuests()
        val restoredActiveQuests = snap?.activeQuests ?: restoredQuests
            .filter { it.resolvedStatus() == QuestStatus.ACTIVE }
            .map { it.title }

        return mapOf(
            "turnCount" to prevTurn.turnNumber.coerceAtLeast(1L),
            "hudLocation" to (snap?.hudLocation ?: fallbackLoc).take(200),
            "hudTime" to (snap?.hudTime ?: fallbackTime).take(120),
            "hudStatusAndInventory" to (snap?.hudStatusAndInventory ?: fallbackStatus).take(300),
            "hudRelationsAndCanon" to (snap?.hudRelationsAndCanon ?: fallbackRel).take(300),
            "canonDeviationPercent" to (snap?.canonDeviationPercent ?: campaign.canonDeviationPercent).coerceIn(0L, 100L),
            "healthPercent" to (snap?.healthPercent ?: campaign.healthPercent.coerceIn(-1L, 100L)),
            "currencyAmount" to (snap?.currencyAmount ?: campaign.currencyAmount).coerceIn(0L, 100_000_000L),
            "currencyLabel" to (snap?.currencyLabel ?: campaign.currencyLabel).take(80),
            "skills" to (snap?.skills ?: campaign.skills).toFirestoreMap(),
            "activeNpc" to prevTurn.activeNpc.toFirestoreMap(),
            "inventory" to restoredFlatInventory.take(60).map { it.take(120) },
            "inventoryItems" to restoredItems.take(60).map { it.toFirestoreMap() },
            "npcs" to (snap?.npcs ?: campaign.npcs).take(60).map { it.toFirestoreMap() },
            "butterflyEvents" to restoredEvents.take(60).map { it.toFirestoreMap() },
            "activeQuests" to restoredActiveQuests.take(12).map { it.take(180) },
            "quests" to restoredQuests.take(30).map { it.toFirestoreMap() },
            "storySummary" to (snap?.storySummary ?: campaign.storySummary).take(4000),
            "chapterSummaries" to (snap?.chapterSummaries ?: campaign.chapterSummaries).take(40),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseHudLineParts(
        hudLine: String,
        fallbackCampaign: Campaign
    ): List<String> {
        val stripped = hudLine.trim().removePrefix("[").removeSuffix("]")
        val parts = stripped.split("|").map { it.trim() }
        return listOf(
            parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: fallbackCampaign.hudLocation,
            parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: fallbackCampaign.hudTime,
            parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: fallbackCampaign.hudStatusAndInventory,
            parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: fallbackCampaign.hudRelationsAndCanon
        )
    }

    private fun buildForkTitle(baseTitle: String, turnNumber: Long, suffix: String): String {
        val cleanBase = baseTitle.trim().ifBlank { "Хроника" }
        val tag = if (suffix.contains("ход", ignoreCase = true)) suffix else " • Ход $turnNumber$suffix"
        return (cleanBase.take(90) + tag).take(120)
    }

    private fun mergeInventoryPreserveEquipment(
        previous: List<InventoryItem>,
        incoming: List<InventoryItem>
    ): List<InventoryItem> {
        if (incoming.isEmpty()) return emptyList()
        val prevByName = previous.associateBy { it.name.trim().lowercase() }
        return incoming.map { item ->
            val prev = prevByName[item.name.trim().lowercase()]
            if (prev != null) {
                item.copy(
                    id = prev.resolvedId(),
                    isEquipped = item.isEquipped || prev.isEquipped,
                    description = item.description.ifBlank { prev.description }
                )
            } else {
                item
            }
        }.distinctBy { it.name.trim().lowercase() }
    }

    private fun mergeQuests(
        previous: List<QuestEntry>,
        incoming: List<QuestEntry>,
        turnNumber: Long
    ): List<QuestEntry> {
        if (incoming.isEmpty()) return previous
        val map = linkedMapOf<String, QuestEntry>()
        previous.forEach { q ->
            map[q.title.trim().lowercase()] = q
        }
        incoming.forEach { q ->
            val key = q.title.trim().lowercase()
            val prev = map[key]
            val changedStatus = prev == null || prev.resolvedStatus() != q.resolvedStatus()
            map[key] = q.copy(
                id = prev?.resolvedId() ?: q.resolvedId(),
                updatedAtTurn = if (changedStatus) turnNumber else prev?.updatedAtTurn ?: turnNumber
            )
        }
        return map.values.toList()
    }

    private suspend fun <T> runWithLegacyFallback(
        primary: suspend () -> T,
        legacy: suspend () -> T,
        minimalLegacy: (suspend () -> T)? = null
    ): T {
        return try {
            primary()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isPermissionDenied(e)) throw e
            try {
                legacy()
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Exception) {
                if (!isPermissionDenied(e2) || minimalLegacy == null) throw e2
                minimalLegacy()
            }
        }
    }

    private fun isPermissionDenied(error: Throwable): Boolean {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return true
        return error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
    }

    companion object {
        private val LEGACY_CAMPAIGN_KEYS = setOf(
            "userId",
            "title",
            "universe",
            "characterSummary",
            "startingPoint",
            "toneAndDifficulty",
            "turnCount",
            "hudLocation",
            "hudTime",
            "hudStatusAndInventory",
            "hudRelationsAndCanon",
            "canonDeviationPercent",
            "activeNpc",
            "inventory",
            "npcs",
            "butterflyEvents",
            "activeQuests",
            "storySummary",
            "createdAt",
            "updatedAt"
        )

        private val LEGACY_TURN_KEYS = setOf(
            "userId",
            "campaignId",
            "turnNumber",
            "playerAction",
            "narrative",
            "hudLine",
            "activeNpc",
            "choices",
            "createdAt"
        )

        internal fun filterLegacyCampaignPayload(
            payload: Map<String, Any>,
            includeActiveQuests: Boolean = true
        ): Map<String, Any> = payload.filterKeys { key ->
            key in LEGACY_CAMPAIGN_KEYS && (includeActiveQuests || key != "activeQuests")
        }

        internal fun filterLegacyTurnPayload(
            payload: Map<String, Any>
        ): Map<String, Any> = payload.filterKeys { key -> key in LEGACY_TURN_KEYS }
    }
}
