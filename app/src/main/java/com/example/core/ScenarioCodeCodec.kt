package com.example.core

import com.example.data.model.HeroSkills
import com.example.data.model.PlayerGender
import com.example.data.model.SessionSetupInput
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object ScenarioCodeCodec {
    private const val PREFIX = "CBW1:"

    fun encode(input: SessionSetupInput): String {
        val json = JSONObject().apply {
            put("t", input.title.trim().take(120))
            put("u", input.universe.trim().take(300))
            put("c", input.characterSummary.trim().take(600))
            put("s", input.startingPoint.trim().take(400))
            put("d", input.toneAndDifficulty.trim().take(300))
            if (input.playerName.isNotBlank()) put("n", input.playerName.trim().take(80))
            put("g", input.playerGender.key)
            put(
                "k",
                JSONObject().apply {
                    put("m", input.skills.might.coerceIn(1L, 10L))
                    put("s", input.skills.stealth.coerceIn(1L, 10L))
                    put("i", input.skills.insight.coerceIn(1L, 10L))
                    put("l", input.skills.lore.coerceIn(1L, 10L))
                }
            )
        }
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        return PREFIX + base64UrlEncode(bytes)
    }

    fun decode(rawCode: String): SessionSetupInput? {
        val cleaned = rawCode.trim()
        if (!cleaned.startsWith(PREFIX)) return null
        val payload = cleaned.removePrefix(PREFIX).trim()
        if (payload.isBlank()) return null

        return runCatching {
            val decodedBytes = base64UrlDecode(payload)
            val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
            val universe = json.optString("u").trim()
            val character = json.optString("c").trim()
            val starting = json.optString("s").trim()
            val tone = json.optString("d").trim()
            if (universe.isBlank() || character.isBlank() || starting.isBlank() || tone.isBlank()) {
                return null
            }
            val skillsObj = json.optJSONObject("k")
            val skills = if (skillsObj != null) {
                HeroSkills(
                    might = skillsObj.optLong("m", 5L),
                    stealth = skillsObj.optLong("s", 5L),
                    insight = skillsObj.optLong("i", 5L),
                    lore = skillsObj.optLong("l", 5L)
                ).coerced()
            } else {
                HeroSkills()
            }
            SessionSetupInput(
                title = json.optString("t").trim().ifBlank { universe.take(48) },
                universe = universe,
                characterSummary = character,
                startingPoint = starting,
                toneAndDifficulty = tone,
                playerName = json.optString("n").trim(),
                playerGender = PlayerGender.fromKey(json.optString("g")),
                skills = skills
            )
        }.getOrNull()
    }

    // Pure Kotlin RFC 4648 base64url (no android.util.Base64 dependency so JVM unit tests run natively)
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private fun base64UrlEncode(data: ByteArray): String {
        val out = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val hasB1 = i + 1 < data.size
            val hasB2 = i + 2 < data.size
            val b1 = if (hasB1) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) data[i + 2].toInt() and 0xFF else 0

            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.append(ALPHABET[(triple ushr 18) and 0x3F])
            out.append(ALPHABET[(triple ushr 12) and 0x3F])
            if (hasB1) out.append(ALPHABET[(triple ushr 6) and 0x3F])
            if (hasB2) out.append(ALPHABET[triple and 0x3F])
            i += 3
        }
        return out.toString()
    }

    private fun base64UrlDecode(text: String): ByteArray {
        val cleaned = text.replace("=", "").filterNot { it.isWhitespace() }
        val table = IntArray(128) { -1 }
        for (idx in ALPHABET.indices) {
            table[ALPHABET[idx].code] = idx
        }
        table['+'.code] = 62
        table['/'.code] = 63

        val out = ByteArray((cleaned.length * 3) / 4)
        var outPos = 0
        var buffer = 0
        var bits = 0
        for (ch in cleaned) {
            val code = ch.code
            val value = if (code < 128) table[code] else -1
            if (value < 0) throw IllegalArgumentException("Invalid base64 character")
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                if (outPos < out.size) {
                    out[outPos++] = ((buffer ushr bits) and 0xFF).toByte()
                }
            }
        }
        return if (outPos == out.size) out else out.copyOf(outPos)
    }
}
