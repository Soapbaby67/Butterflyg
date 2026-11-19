package com.example.data.model

import kotlin.math.roundToLong
import kotlin.random.Random

data class RiskModifierSummary(
    val relevantSkill: SkillType,
    val skillLevel: Long,
    val matchedItemName: String?,
    val matchedItemEquipped: Boolean,
    val setbackDelta: Long,
    val triumphDelta: Long,
    val summaryRu: String
)

data class ResolvedRiskEvaluation(
    val outcomes: List<RiskOutcome>,
    val modifier: RiskModifierSummary
)

object RiskyChoiceResolver {

    private val riskyApproachKeywords = listOf(
        "сил", "дерз", "риск", "скрыт", "атак", "нестандарт", "вызов", "обман"
    )

    private val riskyActionKeywords = listOf(
        "взлом", "укра", "подлож", "атаков", "ударить", "напасть", "угрож", "солгать",
        "блеф", "прорв", "спрыг", "перепрыг", "риск", "сбеж", "перехват", "подслуш",
        "признаться", "поцелов", "перебить", "проверить на прочность", "сыграть ва-банк"
    )

    private val mightKeywords = listOf(
        "сил", "бой", "атак", "удар", "напа", "выб", "слом", "прорв", "схват",
        "оттолк", "обезоруж", "защит", "угроз", "натиск", "рывок"
    )

    private val stealthKeywords = listOf(
        "скрыт", "тень", "тихо", "краст", "взлом", "отмыч", "укра", "подлож",
        "подслуш", "спрят", "ускольз", "обход", "маскир", "проскольз", "сбеж", "перехват"
    )

    private val loreKeywords = listOf(
        "анализ", "маги", "чар", "закл", "алхим", "взломать сеть", "хак", "дешифр",
        "код", "терминал", "сигнал", "ритуал", "руна", "исслед", "провер", "изуч",
        "чип", "кибер", "схем", "архив", "книг", "лечен", "перевяз"
    )

    fun isChoiceRisky(choice: TurnChoice): Boolean {
        if (choice.isRisky || choice.riskOutcomes.size >= 2) return true
        val lowerApproach = choice.approach.lowercase()
        val lowerText = choice.text.lowercase()
        return riskyApproachKeywords.any { lowerApproach.contains(it) } ||
            riskyActionKeywords.any { lowerText.contains(it) }
    }

    fun detectRelevantSkill(choice: TurnChoice): SkillType {
        val combined = "${choice.approach.lowercase()} ${choice.text.lowercase()}"
        return when {
            stealthKeywords.any { combined.contains(it) } -> SkillType.STEALTH
            mightKeywords.any { combined.contains(it) } -> SkillType.MIGHT
            loreKeywords.any { combined.contains(it) } -> SkillType.LORE
            else -> SkillType.INSIGHT
        }
    }

    fun findMatchingInventoryItem(
        choice: TurnChoice,
        relevantSkill: SkillType,
        items: List<InventoryItem>
    ): InventoryItem? {
        if (items.isEmpty()) return null
        val textLower = "${choice.approach.lowercase()} ${choice.text.lowercase()}"

        val directMention = items
            .sortedByDescending { it.isEquipped }
            .firstOrNull { item ->
                val itemTokens = item.name.lowercase()
                    .split(Regex("[^a-zа-я0-9]+"))
                    .filter { it.length >= 4 }
                itemTokens.any { token -> textLower.contains(token.take(5)) }
            }
        if (directMention != null) return directMention

        val skillSpecificMatch = items
            .sortedByDescending { it.isEquipped }
            .firstOrNull { item ->
                val lower = "${item.name.lowercase()} ${item.description.lowercase()}"
                val cat = item.resolvedCategory()
                when (relevantSkill) {
                    SkillType.MIGHT -> cat == ItemCategory.WEAPON ||
                        lower.contains("меч") || lower.contains("кинжал") ||
                        lower.contains("стилет") || lower.contains("пистолет") ||
                        lower.contains("брон") || lower.contains("упм")
                    SkillType.STEALTH -> lower.contains("отмыч") || lower.contains("плащ") ||
                        lower.contains("дым") || lower.contains("кунай") ||
                        lower.contains("ключ") || lower.contains("пропуск") ||
                        cat == ItemCategory.GEAR && item.isEquipped
                    SkillType.LORE -> lower.contains("дек") || lower.contains("чип") ||
                        lower.contains("палочк") || lower.contains("книг") ||
                        lower.contains("дневник") || lower.contains("сканер") ||
                        lower.contains("алхим") || cat == ItemCategory.CLUE
                    SkillType.INSIGHT -> lower.contains("письм") || lower.contains("досье") ||
                        lower.contains("печат") || lower.contains("жетон") ||
                        lower.contains("амулет") || lower.contains("блокнот") ||
                        cat == ItemCategory.KEY_ITEM
                }
            }
        return skillSpecificMatch
    }

    fun buildCustomRiskChoice(
        actionText: String,
        activeNpcName: String? = null
    ): TurnChoice {
        val trimmed = actionText.trim()
        val raw = TurnChoice(
            id = "custom_risk",
            approach = "Рискованный шаг",
            text = trimmed,
            isRisky = true
        )
        return raw.copy(riskOutcomes = resolveOutcomes(raw, activeNpcName))
    }

    fun resolveOutcomesWithModifiers(
        choice: TurnChoice,
        activeNpcName: String? = null,
        skills: HeroSkills = HeroSkills(),
        inventoryItems: List<InventoryItem> = emptyList()
    ): ResolvedRiskEvaluation {
        val baseOutcomes = resolveOutcomes(choice, activeNpcName)
        val relevantSkill = detectRelevantSkill(choice)
        val skillLevel = skills.levelFor(relevantSkill)
        val matchedItem = findMatchingInventoryItem(choice, relevantSkill, inventoryItems)

        val skillOffset = skillLevel - 5L // -4..+5
        val itemSetbackReduction = when {
            matchedItem == null -> 0L
            matchedItem.isEquipped -> 8L
            else -> 4L
        }
        val itemTriumphBoost = when {
            matchedItem == null -> 0L
            matchedItem.isEquipped -> 8L
            else -> 4L
        }

        val rawSetbackDelta = -(skillOffset * 2L) - itemSetbackReduction
        val rawTriumphDelta = (skillOffset * 2L) + itemTriumphBoost

        val adjusted = baseOutcomes.map { outcome ->
            val tier = RiskOutcomeTier.fromKey(outcome.tier)
            val updatedWeight = when (tier) {
                RiskOutcomeTier.TRIUMPH -> (outcome.weight + rawTriumphDelta).coerceIn(10L, 60L)
                RiskOutcomeTier.SETBACK -> (outcome.weight + rawSetbackDelta).coerceIn(10L, 50L)
                RiskOutcomeTier.MIXED -> outcome.weight.coerceIn(15L, 50L)
                RiskOutcomeTier.TWIST -> outcome.weight.coerceIn(8L, 28L)
            }
            outcome.copy(weight = updatedWeight)
        }

        val normalized = normalizeWeights(adjusted)
        val baseSetback = baseOutcomes.firstOrNull { RiskOutcomeTier.fromKey(it.tier) == RiskOutcomeTier.SETBACK }?.weight ?: 25L
        val finalSetback = normalized.firstOrNull { RiskOutcomeTier.fromKey(it.tier) == RiskOutcomeTier.SETBACK }?.weight ?: baseSetback
        val baseTriumph = baseOutcomes.firstOrNull { RiskOutcomeTier.fromKey(it.tier) == RiskOutcomeTier.TRIUMPH }?.weight ?: 25L
        val finalTriumph = normalized.firstOrNull { RiskOutcomeTier.fromKey(it.tier) == RiskOutcomeTier.TRIUMPH }?.weight ?: baseTriumph

        val summaryParts = mutableListOf<String>()
        summaryParts.add("${relevantSkill.labelRu} $skillLevel/10")
        if (matchedItem != null) {
            val tag = if (matchedItem.isEquipped) "экип." else "в сумке"
            summaryParts.add("${matchedItem.name} ($tag)")
        }
        val deltaText = when {
            finalSetback < baseSetback -> "риск срыва ${baseSetback}% → ${finalSetback}%"
            finalSetback > baseSetback -> "риск срыва ${baseSetback}% → ${finalSetback}%"
            else -> "шансы сбалансированы"
        }
        summaryParts.add(deltaText)

        return ResolvedRiskEvaluation(
            outcomes = normalized,
            modifier = RiskModifierSummary(
                relevantSkill = relevantSkill,
                skillLevel = skillLevel,
                matchedItemName = matchedItem?.name,
                matchedItemEquipped = matchedItem?.isEquipped == true,
                setbackDelta = finalSetback - baseSetback,
                triumphDelta = finalTriumph - baseTriumph,
                summaryRu = summaryParts.joinToString(" • ")
            )
        )
    }

    fun resolveOutcomes(
        choice: TurnChoice,
        activeNpcName: String? = null
    ): List<RiskOutcome> {
        val provided = choice.riskOutcomes
            .filter { it.label.isNotBlank() && it.description.isNotBlank() }
            .take(4)
        if (provided.size >= 2) {
            return normalizeWeights(provided)
        }

        val lowerApproach = choice.approach.lowercase()
        val targetNpc = activeNpcName?.takeIf { it.isNotBlank() }

        val synthesized = when {
            lowerApproach.contains("сил") || lowerApproach.contains("бой") || lowerApproach.contains("атак") -> listOf(
                RiskOutcome(
                    id = "triumph",
                    label = "Решительный перевес",
                    description = "Натиск срабатывает безотказно: цель выбита из колеи, а путь впереди открыт.",
                    tier = RiskOutcomeTier.TRIUMPH.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "mixed",
                    label = "Победа с ценой",
                    description = "Вы добиваетесь своего, но получаете ссадину или привлекаете лишнее внимание.",
                    tier = RiskOutcomeTier.MIXED.key,
                    weight = 35L
                ),
                RiskOutcome(
                    id = "setback",
                    label = "Жёсткий отпор",
                    description = "Противник перехватывает инициативу — шаг срывается, а ситуация обостряется.",
                    tier = RiskOutcomeTier.SETBACK.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "twist",
                    label = "Эффект бабочки",
                    description = "Столкновение вскрывает скрытую деталь или вмешивается третья сторона.",
                    tier = RiskOutcomeTier.TWIST.key,
                    weight = 15L
                )
            )

            lowerApproach.contains("скрыт") || lowerApproach.contains("обман") || lowerApproach.contains("тень") -> listOf(
                RiskOutcome(
                    id = "triumph",
                    label = "Безупречный след",
                    description = "Никто ничего не заметил — вы получаете преимущество и скрытую информацию.",
                    tier = RiskOutcomeTier.TRIUMPH.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "mixed",
                    label = "На волоске",
                    description = "Задуманное почти удаётся, но остаётся мелкая зацепка или чужое подозрение.",
                    tier = RiskOutcomeTier.MIXED.key,
                    weight = 35L
                ),
                RiskOutcome(
                    id = "setback",
                    label = "Разоблачение",
                    description = if (targetNpc != null) {
                        "$targetNpc замечает уловку, дистанция растёт, а доверие заметно падает."
                    } else {
                        "Маскировка даёт трещину — вы оказываетесь под прицелом взглядов."
                    },
                    tier = RiskOutcomeTier.SETBACK.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "twist",
                    label = "Чужой секрет",
                    description = "Вместо привычного исхода вы случайно перехватываете чужую тайну.",
                    tier = RiskOutcomeTier.TWIST.key,
                    weight = 15L
                )
            )

            else -> listOf(
                RiskOutcome(
                    id = "triumph",
                    label = "Чистый прорыв",
                    description = if (targetNpc != null) {
                        "Смелый шаг попадает в точку: $targetNpc реагирует глубже и искреннее, чем ожидалось."
                    } else {
                        "Авантюра срабатывает блестяще и поворачивает сцену в вашу пользу."
                    },
                    tier = RiskOutcomeTier.TRIUMPH.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "mixed",
                    label = "Хрупкий компромисс",
                    description = "Вы двигаетесь вперёд, но возникает условие, пауза или новая цена за доверие.",
                    tier = RiskOutcomeTier.MIXED.key,
                    weight = 35L
                ),
                RiskOutcome(
                    id = "setback",
                    label = "Холодный отказ",
                    description = if (targetNpc != null) {
                        "$targetNpc ставит жёсткую границу: момент упущен, тон становится сдержаннее."
                    } else {
                        "Ставка не сыграла — шанс уходит, а обстановка становится напряжённее."
                    },
                    tier = RiskOutcomeTier.SETBACK.key,
                    weight = 25L
                ),
                RiskOutcome(
                    id = "twist",
                    label = "Неожиданный поворот",
                    description = "Событие меняет расклад всей сцены и запускает новый виток последствий.",
                    tier = RiskOutcomeTier.TWIST.key,
                    weight = 15L
                )
            )
        }
        return normalizeWeights(synthesized)
    }

    fun normalizeWeights(outcomes: List<RiskOutcome>): List<RiskOutcome> {
        if (outcomes.isEmpty()) return emptyList()
        val cleanWeights = outcomes.map { it.weight.coerceIn(5L, 80L) }
        val total = cleanWeights.sum().coerceAtLeast(1L)
        return outcomes.mapIndexed { index, outcome ->
            val pct = ((cleanWeights[index] * 100.0) / total).roundToLong().coerceAtLeast(5L)
            outcome.copy(
                id = outcome.id.ifBlank { "outcome_${index + 1}" },
                tier = RiskOutcomeTier.fromKey(outcome.tier).key,
                weight = pct
            )
        }
    }

    fun pickWeightedOutcome(
        outcomes: List<RiskOutcome>,
        random: Random = Random.Default
    ): Pair<Int, RiskOutcome> {
        require(outcomes.isNotEmpty()) { "Outcomes list must not be empty" }
        val weights = outcomes.map { it.weight.coerceAtLeast(5L) }
        val total = weights.sum()
        var roll = random.nextLong(total)
        for (i in outcomes.indices) {
            roll -= weights[i]
            if (roll < 0L) {
                return i to outcomes[i]
            }
        }
        return outcomes.lastIndex to outcomes.last()
    }

    fun formatOutcomePrompt(
        actionText: String,
        outcome: RiskOutcome,
        modifier: RiskModifierSummary? = null
    ): String {
        val tierLabel = RiskOutcomeTier.fromKey(outcome.tier).labelRu
        val modSuffix = modifier?.let { " (${it.summaryRu})" }.orEmpty()
        return "${actionText.trim()}\n[Бросок судьбы • $tierLabel — «${outcome.label}»$modSuffix: ${outcome.description}]"
    }
}
