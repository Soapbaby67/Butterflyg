package com.example.ui.components

import com.example.data.model.Campaign
import com.example.ui.theme.UniverseArchetype
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.resolveUniverseArchetype

data class ParsedHudStats(
    val healthPercent: Int,
    val healthLabel: String,
    val currencyLabel: String,
    val inventoryItems: List<String>,
    val relationsSummary: String,
    val weatherMood: WeatherMood
)

object HudStatsParser {
    private val percentRegex = Regex("""(\d{1,3})\s*%""")
    private val currencyRegex = Regex(
        """\b(\d[\d\s]{0,8}(?:крон\p{L}*|эдди|золот\p{L}*|монет\p{L}*|орен\p{L}*|флорин\p{L}*|галлеон\p{L}*|сикл\p{L}*|рю|йен\p{L}*|кредит\p{L}*|доллар\p{L}*))""",
        RegexOption.IGNORE_CASE
    )

    fun parse(campaign: Campaign): ParsedHudStats {
        val rawStatus = campaign.hudStatusAndInventory.trim()
        val rawRelations = campaign.hudRelationsAndCanon.trim()
        val rawTime = campaign.hudTime.trim()
        val rawLocation = campaign.hudLocation.trim()

        val (healthPercent, healthLabel) = parseHealth(rawStatus)
        val currencyLabel = parseCurrency(rawStatus, campaign.inventory)
        val inventoryItems = parseInventory(rawStatus, campaign.inventory, currencyLabel)
        val relationsSummary = parseRelations(rawRelations)
        val weatherMood = parseWeatherMood(
            universe = campaign.universe,
            location = rawLocation,
            time = rawTime
        )

        return ParsedHudStats(
            healthPercent = healthPercent,
            healthLabel = healthLabel,
            currencyLabel = currencyLabel,
            inventoryItems = inventoryItems,
            relationsSummary = relationsSummary,
            weatherMood = weatherMood
        )
    }

    private fun parseHealth(rawStatus: String): Pair<Int, String> {
        val statusLower = rawStatus.lowercase()
        val explicitPercent = percentRegex.find(rawStatus)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.coerceIn(0, 100)

        val rawHealthPart = rawStatus
            .substringBefore("|")
            .substringBefore("•")
            .replace(Regex("""здоровье\s*:""", RegexOption.IGNORE_CASE), "")
            .replace(percentRegex, "")
            .replace("()", "")
            .trim()
            .trim(',', '.', ';', '-', ' ')

        val inferredPercent = explicitPercent ?: when {
            statusLower.contains("крит") || statusLower.contains("при смерт") ||
                statusLower.contains("тяжел") || statusLower.contains("истека") -> 25
            statusLower.contains("ранен") || statusLower.contains("ушиб") ||
                statusLower.contains("травм") || statusLower.contains("слаб") -> 55
            statusLower.contains("утомл") || statusLower.contains("устал") ||
                statusLower.contains("царапин") || statusLower.contains("истощ") -> 75
            statusLower.contains("норм") || statusLower.contains("здоров") ||
                statusLower.contains("бодр") || statusLower.contains("отлич") ||
                statusLower.contains("цел") -> 100
            else -> 95
        }

        val cleanLabel = rawHealthPart.ifBlank {
            when {
                inferredPercent >= 85 -> "В норме"
                inferredPercent >= 55 -> "Лёгкий урон"
                inferredPercent >= 30 -> "Ранен"
                else -> "Критично"
            }
        }.replaceFirstChar { it.uppercase() }

        return inferredPercent to cleanLabel
    }

    private fun parseCurrency(rawStatus: String, inventory: List<String>): String {
        currencyRegex.find(rawStatus)?.groupValues?.getOrNull(1)?.trim()?.let {
            return it
        }
        for (item in inventory) {
            if (currencyRegex.matches(item.trim())) {
                return item.trim()
            }
        }
        return ""
    }

    private fun parseInventory(
        rawStatus: String,
        campaignInventory: List<String>,
        currencyLabel: String
    ): List<String> {
        val baseItems = if (campaignInventory.isNotEmpty()) {
            campaignInventory
        } else {
            val invSection = Regex("""инвентар[ья]\s*:\s*([^|\]]+)""", RegexOption.IGNORE_CASE)
                .find(rawStatus)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
            invSection.split(",")
        }

        return baseItems
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { item ->
                currencyLabel.isNotBlank() && item.equals(currencyLabel, ignoreCase = true)
            }
            .map { item -> item.replaceFirstChar { it.uppercase() } }
            .distinctBy { it.lowercase() }
    }

    private fun parseRelations(rawRelations: String): String {
        val cleaned = rawRelations
            .split(",")
            .map { it.trim() }
            .filterNot { part ->
                part.isBlank() || part.contains("канон", ignoreCase = true)
            }
            .joinToString(", ")
        return cleaned.ifBlank {
            rawRelations.ifBlank { "Нейтральные" }
        }.replaceFirstChar { it.uppercase() }
    }

    private fun parseWeatherMood(
        universe: String,
        location: String,
        time: String
    ): WeatherMood {
        val combined = "${location.lowercase()} ${time.lowercase()}"
        val universeArch = resolveUniverseArchetype(universe)

        return when {
            combined.contains("гроз") || combined.contains("шторм") ||
                combined.contains("буря") -> WeatherMood.STORM
            combined.contains("дожд") || combined.contains("ливен") ||
                combined.contains("морос") -> WeatherMood.RAIN
            combined.contains("снег") || combined.contains("метел") ||
                combined.contains("мороз") -> WeatherMood.SNOW
            combined.contains("туман") || combined.contains("дымк") ||
                combined.contains("пасмур") -> WeatherMood.FOG
            universeArch == UniverseArchetype.CYBERPUNK_SCIFI &&
                (combined.contains("ноч") || combined.contains("неон") ||
                    combined.contains("вечер") || combined.contains("бар")) -> WeatherMood.NEON_NIGHT
            combined.contains("таверн") || combined.contains("библиотек") ||
                combined.contains("кабинет") || combined.contains("зал") ||
                combined.contains("комнат") || combined.contains("подвал") ||
                combined.contains("свеч") -> WeatherMood.INDOOR_CANDLE
            combined.contains("рассвет") || combined.contains("утр") -> WeatherMood.DAWN
            combined.contains("ноч") || combined.contains("полноч") ||
                combined.contains("сумер") -> WeatherMood.NIGHT
            else -> WeatherMood.DAY
        }
    }
}
