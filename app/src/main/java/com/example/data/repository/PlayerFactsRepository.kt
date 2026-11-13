package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PlayerFactNote(
    val id: String,
    val text: String,
    val isPinned: Boolean = true
)

class PlayerFactsRepository(private val context: Context) {

    private fun keyForCampaign(campaignId: String) =
        stringPreferencesKey("player_facts_$campaignId")

    fun observeFacts(campaignId: String): Flow<List<PlayerFactNote>> {
        if (campaignId.isBlank()) {
            return context.themeDataStore.data.map { emptyList() }
        }
        val key = keyForCampaign(campaignId)
        return context.themeDataStore.data.map { prefs ->
            parseFacts(prefs[key])
        }
    }

    suspend fun addFact(campaignId: String, rawText: String) {
        val text = rawText.trim().take(240)
        if (campaignId.isBlank() || text.isBlank()) return
        val key = keyForCampaign(campaignId)
        context.themeDataStore.edit { prefs ->
            val current = parseFacts(prefs[key]).toMutableList()
            current.add(
                0,
                PlayerFactNote(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    isPinned = true
                )
            )
            prefs[key] = serializeFacts(current.take(25))
        }
    }

    suspend fun toggleFactPinned(campaignId: String, factId: String) {
        if (campaignId.isBlank()) return
        val key = keyForCampaign(campaignId)
        context.themeDataStore.edit { prefs ->
            val updated = parseFacts(prefs[key]).map { fact ->
                if (fact.id == factId) fact.copy(isPinned = !fact.isPinned) else fact
            }
            prefs[key] = serializeFacts(updated)
        }
    }

    suspend fun deleteFact(campaignId: String, factId: String) {
        if (campaignId.isBlank()) return
        val key = keyForCampaign(campaignId)
        context.themeDataStore.edit { prefs ->
            val updated = parseFacts(prefs[key]).filterNot { it.id == factId }
            prefs[key] = serializeFacts(updated)
        }
    }

    private fun parseFacts(raw: String?): List<PlayerFactNote> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    val text = obj.optString("text").trim()
                    if (id.isNotBlank() && text.isNotBlank()) {
                        add(
                            PlayerFactNote(
                                id = id,
                                text = text,
                                isPinned = obj.optBoolean("isPinned", true)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeFacts(facts: List<PlayerFactNote>): String {
        val array = JSONArray()
        facts.forEach { fact ->
            array.put(
                JSONObject().apply {
                    put("id", fact.id)
                    put("text", fact.text)
                    put("isPinned", fact.isPinned)
                }
            )
        }
        return array.toString()
    }
}
