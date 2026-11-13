package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.model.SessionSetupInput
import com.example.data.model.TurnChoice
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
    val choices: List<TurnChoice>
) {
    val hudLine: String
        get() = "[$hudLocation | $hudTime | $hudStatusAndInventory | $hudRelationsAndCanon]"
}

class CampaignRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
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
        emitAll(
            campaignsCollection(userId)
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
        )
    }

    fun observeCampaign(userId: String, campaignId: String): Flow<Campaign?> = flow {
        val docRef = campaignsCollection(userId).document(campaignId)
        emitAll(
            docRef.snapshots()
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
        )
    }

    fun observeTurns(userId: String, campaignId: String): Flow<List<CampaignTurn>> = flow {
        val path = "users/$userId/campaigns/$campaignId/turns"
        emitAll(
            turnsCollection(userId, campaignId)
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
        )
    }

    suspend fun getCampaign(campaignId: String): Campaign? {
        val uid = requireUserId()
        val docRef = campaignsCollection(uid).document(campaignId)
        return try {
            val snap = docRef.get().await()
            snap.toObject(
                Campaign::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
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
            ?.let { listOf(it.copy(turnNumber = 1L).toFirestoreMap()) }
            ?: emptyList()

        val campaignPayload = mapOf(
            "userId" to uid,
            "title" to setup.title.trim().ifBlank { setup.universe.take(48) }.take(120),
            "universe" to setup.universe.trim().take(300),
            "characterSummary" to setup.characterSummary.trim().take(600),
            "startingPoint" to setup.startingPoint.trim().take(400),
            "toneAndDifficulty" to setup.toneAndDifficulty.trim().take(300),
            "turnCount" to 1L,
            "hudLocation" to openingOutcome.hudLocation.take(200),
            "hudTime" to openingOutcome.hudTime.take(120),
            "hudStatusAndInventory" to openingOutcome.hudStatusAndInventory.take(300),
            "hudRelationsAndCanon" to openingOutcome.hudRelationsAndCanon.take(300),
            "canonDeviationPercent" to openingOutcome.canonDeviationPercent.coerceIn(0L, 100L),
            "activeNpc" to openingOutcome.activeNpc.toFirestoreMap(),
            "inventory" to openingOutcome.inventory.take(60).map { it.take(120) },
            "npcs" to openingOutcome.npcs.take(60).map { it.toFirestoreMap() },
            "butterflyEvents" to initialEvents,
            "storySummary" to openingOutcome.storySummary.take(4000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val turnPayload = mapOf(
            "userId" to uid,
            "campaignId" to campaignId,
            "turnNumber" to 1L,
            "playerAction" to "Начало хроники",
            "narrative" to openingOutcome.narrative.take(12000),
            "hudLine" to openingOutcome.hudLine.take(600),
            "activeNpc" to openingOutcome.activeNpc.toFirestoreMap(),
            "choices" to openingOutcome.choices.take(6).map { it.toFirestoreMap() },
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.runBatch { batch ->
                batch.set(campaignRef, campaignPayload)
                batch.set(turnRef, turnPayload)
            }.await()
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
        outcome: GmTurnOutcome
    ) {
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

        val campaignUpdate = mapOf(
            "turnCount" to nextTurnNumber,
            "hudLocation" to outcome.hudLocation.take(200),
            "hudTime" to outcome.hudTime.take(120),
            "hudStatusAndInventory" to outcome.hudStatusAndInventory.take(300),
            "hudRelationsAndCanon" to outcome.hudRelationsAndCanon.take(300),
            "canonDeviationPercent" to outcome.canonDeviationPercent.coerceIn(0L, 100L),
            "activeNpc" to outcome.activeNpc.toFirestoreMap(),
            "inventory" to outcome.inventory.take(60).map { it.take(120) },
            "npcs" to outcome.npcs.take(60).map { it.toFirestoreMap() },
            "butterflyEvents" to updatedEvents.map { it.toFirestoreMap() },
            "storySummary" to outcome.storySummary.take(4000),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val turnPayload = mapOf(
            "userId" to uid,
            "campaignId" to campaign.id,
            "turnNumber" to nextTurnNumber,
            "playerAction" to playerAction.trim().take(1200),
            "narrative" to outcome.narrative.take(12000),
            "hudLine" to outcome.hudLine.take(600),
            "activeNpc" to outcome.activeNpc.toFirestoreMap(),
            "choices" to outcome.choices.take(6).map { it.toFirestoreMap() },
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runBatch { batch ->
                batch.update(campaignRef, campaignUpdate)
                batch.set(turnRef, turnPayload)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, campaignRef.path)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, campaignRef.path)
            throw e
        }
    }
}
