package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.NpcRelation
import com.example.data.model.PlayerGender
import com.example.data.model.QuestEntry
import com.example.data.model.TurnStateSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

data class CampaignExtendedState(
    val playerName: String = "",
    val playerGender: String = PlayerGender.NEUTRAL.key,
    val healthPercent: Long = -1L,
    val currencyAmount: Long = 0L,
    val currencyLabel: String = "",
    val skills: HeroSkills = HeroSkills(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val quests: List<QuestEntry> = emptyList(),
    val chapterSummaries: List<String> = emptyList(),
    val turnSnapshots: Map<Long, TurnStateSnapshot> = emptyMap()
)

/**
 * Local on-device sidecar store for Stage 1-4 extended campaign fields and per-turn snapshots.
 * Ensures full RPG mechanics (structured inventory, quests, skills, numeric vitals, chapter
 * archives, rollback snapshots) work seamlessly even when the remote Firestore security rules
 * have not yet been updated to accept the newer document fields.
 */
class CampaignExtendedStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    @Synchronized
    fun read(campaignId: String): CampaignExtendedState? {
        if (campaignId.isBlank()) return null
        val raw = prefs.getString(keyFor(campaignId), null) ?: return null
        return runCatching { decodeState(JSONObject(raw)) }.getOrNull()
    }

    @Synchronized
    fun saveCampaignState(
        campaignId: String,
        playerName: String? = null,
        playerGender: String? = null,
        healthPercent: Long? = null,
        currencyAmount: Long? = null,
        currencyLabel: String? = null,
        skills: HeroSkills? = null,
        inventoryItems: List<InventoryItem>? = null,
        quests: List<QuestEntry>? = null,
        chapterSummaries: List<String>? = null,
        turnSnapshot: Pair<Long, TurnStateSnapshot>? = null,
        pruneSnapshotsAfterTurn: Long? = null
    ) {
        if (campaignId.isBlank()) return
        val existing = read(campaignId) ?: CampaignExtendedState()
        val updatedSnapshots = LinkedHashMap(existing.turnSnapshots)
        if (pruneSnapshotsAfterTurn != null) {
            val iter = updatedSnapshots.keys.iterator()
            while (iter.hasNext()) {
                if (iter.next() > pruneSnapshotsAfterTurn) iter.remove()
            }
        }
        if (turnSnapshot != null) {
            updatedSnapshots[turnSnapshot.first] = turnSnapshot.second
        }
        val trimmedSnapshots = if (updatedSnapshots.size > MAX_STORED_SNAPSHOTS) {
            updatedSnapshots.entries
                .sortedBy { it.key }
                .takeLast(MAX_STORED_SNAPSHOTS)
                .associate { it.key to it.value }
        } else {
            updatedSnapshots
        }

        val merged = existing.copy(
            playerName = playerName ?: existing.playerName,
            playerGender = playerGender ?: existing.playerGender,
            healthPercent = healthPercent ?: existing.healthPercent,
            currencyAmount = currencyAmount ?: existing.currencyAmount,
            currencyLabel = currencyLabel ?: existing.currencyLabel,
            skills = skills ?: existing.skills,
            inventoryItems = inventoryItems ?: existing.inventoryItems,
            quests = quests ?: existing.quests,
            chapterSummaries = chapterSummaries ?: existing.chapterSummaries,
            turnSnapshots = trimmedSnapshots
        )
        prefs.edit().putString(keyFor(campaignId), encodeState(merged).toString()).apply()
        _revision.update { it + 1L }
    }

    @Synchronized
    fun saveSnapshots(campaignId: String, snapshots: Map<Long, TurnStateSnapshot>) {
        if (campaignId.isBlank() || snapshots.isEmpty()) return
        val existing = read(campaignId) ?: CampaignExtendedState()
        val combined = LinkedHashMap(existing.turnSnapshots)
        combined.putAll(snapshots)
        val trimmed = combined.entries
            .sortedBy { it.key }
            .takeLast(MAX_STORED_SNAPSHOTS)
            .associate { it.key to it.value }
        val merged = existing.copy(turnSnapshots = trimmed)
        prefs.edit().putString(keyFor(campaignId), encodeState(merged).toString()).apply()
        _revision.update { it + 1L }
    }

    @Synchronized
    fun deleteCampaign(campaignId: String) {
        if (campaignId.isBlank()) return
        prefs.edit().remove(keyFor(campaignId)).apply()
        _revision.update { it + 1L }
    }

    fun mergeInto(campaign: Campaign): Campaign {
        val local = read(campaign.id) ?: return campaign
        val defaultSkills = HeroSkills()
        return campaign.copy(
            playerName = campaign.playerName.ifBlank { local.playerName },
            playerGender = if (campaign.playerGender == PlayerGender.NEUTRAL.key &&
                local.playerGender != PlayerGender.NEUTRAL.key
            ) {
                local.playerGender
            } else {
                campaign.playerGender
            },
            healthPercent = if (campaign.healthPercent < 0L && local.healthPercent >= 0L) {
                local.healthPercent
            } else {
                campaign.healthPercent
            },
            currencyAmount = if (campaign.currencyAmount == 0L && campaign.currencyLabel.isBlank() &&
                (local.currencyAmount > 0L || local.currencyLabel.isNotBlank())
            ) {
                local.currencyAmount
            } else {
                campaign.currencyAmount
            },
            currencyLabel = campaign.currencyLabel.ifBlank { local.currencyLabel },
            skills = if (campaign.skills == defaultSkills && local.skills != defaultSkills) {
                local.skills
            } else {
                campaign.skills
            },
            inventoryItems = campaign.inventoryItems.ifEmpty { local.inventoryItems },
            quests = campaign.quests.ifEmpty { local.quests },
            chapterSummaries = campaign.chapterSummaries.ifEmpty { local.chapterSummaries }
        )
    }

    fun mergeInto(campaignId: String, turns: List<CampaignTurn>): List<CampaignTurn> {
        val local = read(campaignId) ?: return turns
        if (local.turnSnapshots.isEmpty()) return turns
        return turns.map { turn ->
            if (turn.stateSnapshot != null) {
                turn
            } else {
                val snap = local.turnSnapshots[turn.turnNumber]
                if (snap != null) turn.copy(stateSnapshot = snap) else turn
            }
        }
    }

    private fun keyFor(campaignId: String): String = "campaign_ext_$campaignId"

    companion object {
        private const val PREFS_NAME = "chronicles_campaign_extended_state"
        private const val MAX_STORED_SNAPSHOTS = 40

        @Volatile
        private var instance: CampaignExtendedStore? = null

        fun getInstance(context: Context): CampaignExtendedStore {
            return instance ?: synchronized(this) {
                instance ?: CampaignExtendedStore(context.applicationContext).also { instance = it }
            }
        }

        internal fun encodeState(state: CampaignExtendedState): JSONObject = JSONObject().apply {
            put("playerName", state.playerName)
            put("playerGender", state.playerGender)
            put("healthPercent", state.healthPercent)
            put("currencyAmount", state.currencyAmount)
            put("currencyLabel", state.currencyLabel)
            put("skills", encodeSkills(state.skills))
            put("inventoryItems", JSONArray().apply {
                state.inventoryItems.forEach { put(encodeItem(it)) }
            })
            put("quests", JSONArray().apply {
                state.quests.forEach { put(encodeQuest(it)) }
            })
            put("chapterSummaries", JSONArray().apply {
                state.chapterSummaries.forEach { put(it) }
            })
            put("turnSnapshots", JSONObject().apply {
                state.turnSnapshots.forEach { (turnNum, snap) ->
                    put(turnNum.toString(), encodeSnapshot(snap))
                }
            })
        }

        internal fun decodeState(obj: JSONObject): CampaignExtendedState {
            val skillsObj = obj.optJSONObject("skills")
            val itemsArr = obj.optJSONArray("inventoryItems")
            val questsArr = obj.optJSONArray("quests")
            val chaptersArr = obj.optJSONArray("chapterSummaries")
            val snapshotsObj = obj.optJSONObject("turnSnapshots")

            val snapshots = LinkedHashMap<Long, TurnStateSnapshot>()
            if (snapshotsObj != null) {
                val keys = snapshotsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val turnNum = key.toLongOrNull() ?: continue
                    val snapJson = snapshotsObj.optJSONObject(key) ?: continue
                    snapshots[turnNum] = decodeSnapshot(snapJson)
                }
            }

            return CampaignExtendedState(
                playerName = obj.optString("playerName", ""),
                playerGender = obj.optString("playerGender", PlayerGender.NEUTRAL.key),
                healthPercent = obj.optLong("healthPercent", -1L),
                currencyAmount = obj.optLong("currencyAmount", 0L),
                currencyLabel = obj.optString("currencyLabel", ""),
                skills = decodeSkills(skillsObj),
                inventoryItems = decodeItems(itemsArr),
                quests = decodeQuests(questsArr),
                chapterSummaries = decodeStringList(chaptersArr),
                turnSnapshots = snapshots
            )
        }

        private fun encodeSkills(skills: HeroSkills): JSONObject = JSONObject().apply {
            val c = skills.coerced()
            put("might", c.might)
            put("stealth", c.stealth)
            put("insight", c.insight)
            put("lore", c.lore)
        }

        private fun decodeSkills(obj: JSONObject?): HeroSkills {
            if (obj == null) return HeroSkills()
            return HeroSkills(
                might = obj.optLong("might", 5L),
                stealth = obj.optLong("stealth", 5L),
                insight = obj.optLong("insight", 5L),
                lore = obj.optLong("lore", 5L)
            ).coerced()
        }

        private fun encodeItem(item: InventoryItem): JSONObject = JSONObject().apply {
            put("id", item.resolvedId())
            put("name", item.name)
            put("category", item.resolvedCategory().key)
            put("quantity", item.quantity)
            put("isEquipped", item.isEquipped)
            put("description", item.description)
        }

        private fun decodeItems(arr: JSONArray?): List<InventoryItem> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name", "").trim()
                    if (name.isBlank()) continue
                    add(
                        InventoryItem(
                            id = o.optString("id", ""),
                            name = name,
                            category = o.optString("category", "gear"),
                            quantity = o.optLong("quantity", 1L).coerceIn(1L, 999L),
                            isEquipped = o.optBoolean("isEquipped", false),
                            description = o.optString("description", "")
                        )
                    )
                }
            }
        }

        private fun encodeQuest(q: QuestEntry): JSONObject = JSONObject().apply {
            put("id", q.resolvedId())
            put("title", q.title)
            put("status", q.resolvedStatus().key)
            put("updatedAtTurn", q.updatedAtTurn)
        }

        private fun decodeQuests(arr: JSONArray?): List<QuestEntry> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("title", "").trim()
                    if (title.isBlank()) continue
                    add(
                        QuestEntry(
                            id = o.optString("id", ""),
                            title = title,
                            status = o.optString("status", "active"),
                            updatedAtTurn = o.optLong("updatedAtTurn", 1L)
                        )
                    )
                }
            }
        }

        private fun encodeNpc(npc: NpcRelation): JSONObject = JSONObject().apply {
            put("name", npc.name)
            put("role", npc.role)
            put("trustLevel", npc.trustLevel)
            put("statusLabel", npc.statusLabel)
            put("lastEmotion", npc.lastEmotion)
            put("lastEmotionCategory", npc.lastEmotionCategory)
            put("archetype", npc.archetype)
            put("personality", npc.personality)
            put("boundaries", npc.boundaries)
            put("recentActions", JSONArray().apply {
                npc.recentActions.forEach { put(it) }
            })
        }

        private fun decodeNpcs(arr: JSONArray?): List<NpcRelation> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name", "").trim()
                    if (name.isBlank()) continue
                    add(
                        NpcRelation(
                            name = name,
                            role = o.optString("role", ""),
                            trustLevel = o.optLong("trustLevel", 0L),
                            statusLabel = o.optString("statusLabel", ""),
                            lastEmotion = o.optString("lastEmotion", ""),
                            lastEmotionCategory = o.optString("lastEmotionCategory", "neutral"),
                            archetype = o.optString("archetype", ""),
                            personality = o.optString("personality", ""),
                            boundaries = o.optString("boundaries", ""),
                            recentActions = decodeStringList(o.optJSONArray("recentActions"))
                        )
                    )
                }
            }
        }

        private fun encodeButterfly(ev: ButterflyEvent): JSONObject = JSONObject().apply {
            put("turnNumber", ev.turnNumber)
            put("title", ev.title)
            put("consequence", ev.consequence)
            put("impactLevel", ev.impactLevel)
        }

        private fun decodeButterflies(arr: JSONArray?): List<ButterflyEvent> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("title", "").trim()
                    if (title.isBlank()) continue
                    add(
                        ButterflyEvent(
                            turnNumber = o.optLong("turnNumber", 1L),
                            title = title,
                            consequence = o.optString("consequence", ""),
                            impactLevel = o.optString("impactLevel", "minor")
                        )
                    )
                }
            }
        }

        private fun encodeSnapshot(snap: TurnStateSnapshot): JSONObject = JSONObject().apply {
            put("hudLocation", snap.hudLocation)
            put("hudTime", snap.hudTime)
            put("hudStatusAndInventory", snap.hudStatusAndInventory)
            put("hudRelationsAndCanon", snap.hudRelationsAndCanon)
            put("canonDeviationPercent", snap.canonDeviationPercent)
            put("healthPercent", snap.healthPercent)
            put("currencyAmount", snap.currencyAmount)
            put("currencyLabel", snap.currencyLabel)
            put("skills", encodeSkills(snap.skills))
            put("inventory", JSONArray().apply { snap.inventory.forEach { put(it) } })
            put("inventoryItems", JSONArray().apply { snap.inventoryItems.forEach { put(encodeItem(it)) } })
            put("npcs", JSONArray().apply { snap.npcs.forEach { put(encodeNpc(it)) } })
            put("butterflyEvents", JSONArray().apply { snap.butterflyEvents.forEach { put(encodeButterfly(it)) } })
            put("activeQuests", JSONArray().apply { snap.activeQuests.forEach { put(it) } })
            put("quests", JSONArray().apply { snap.quests.forEach { put(encodeQuest(it)) } })
            put("storySummary", snap.storySummary)
            put("chapterSummaries", JSONArray().apply { snap.chapterSummaries.forEach { put(it) } })
            put("stateChangeSummary", snap.stateChangeSummary)
        }

        private fun decodeSnapshot(o: JSONObject): TurnStateSnapshot = TurnStateSnapshot(
            hudLocation = o.optString("hudLocation", ""),
            hudTime = o.optString("hudTime", ""),
            hudStatusAndInventory = o.optString("hudStatusAndInventory", ""),
            hudRelationsAndCanon = o.optString("hudRelationsAndCanon", ""),
            canonDeviationPercent = o.optLong("canonDeviationPercent", 0L),
            healthPercent = o.optLong("healthPercent", 100L),
            currencyAmount = o.optLong("currencyAmount", 0L),
            currencyLabel = o.optString("currencyLabel", ""),
            skills = decodeSkills(o.optJSONObject("skills")),
            inventory = decodeStringList(o.optJSONArray("inventory")),
            inventoryItems = decodeItems(o.optJSONArray("inventoryItems")),
            npcs = decodeNpcs(o.optJSONArray("npcs")),
            butterflyEvents = decodeButterflies(o.optJSONArray("butterflyEvents")),
            activeQuests = decodeStringList(o.optJSONArray("activeQuests")),
            quests = decodeQuests(o.optJSONArray("quests")),
            storySummary = o.optString("storySummary", ""),
            chapterSummaries = decodeStringList(o.optJSONArray("chapterSummaries")),
            stateChangeSummary = o.optString("stateChangeSummary", "")
        )

        private fun decodeStringList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i, "").trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }
    }
}
