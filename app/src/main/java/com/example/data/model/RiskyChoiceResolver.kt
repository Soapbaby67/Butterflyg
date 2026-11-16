package com.example.data.model

import kotlin.random.Random

object RiskyChoiceResolver {

    private val riskyApproachKeywords = listOf(
        "сил", "дерз", "риск", "скрыт", "атак", "нестандарт", "вызов", "обман"
    )

    private val riskyActionKeywords = listOf(
        "взлом", "укра", "подлож", "атаков", "ударить", "напасть", "угрож", "солгать",
        "блеф", "прорв", "спрыг", "перепрыг", "риск", "сбеж", "перехват", "подслуш",
        "признаться", "поцелов", "перебить", "проверить на прочность", "сыграть ва-банк"
    )

    fun isChoiceRisky(choice: TurnChoice): Boolean {
        if (choice.isRisky || choice.riskOutcomes.size >= 2) return true
        val lowerApproach = choice.approach.lowercase()
        val lowerText = choice.text.lowercase()
        return riskyApproachKeywords.any { lowerApproach.contains(it) } ||
            riskyActionKeywords.any { lowerText.contains(it) }
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
            val pct = ((cleanWeights[index] * 100.0) / total).toLong().coerceAtLeast(5L)
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

    fun formatOutcomePrompt(actionText: String, outcome: RiskOutcome): String {
        val tierLabel = RiskOutcomeTier.fromKey(outcome.tier).labelRu
        return "${actionText.trim()}\n[Бросок судьбы • $tierLabel — «${outcome.label}»: ${outcome.description}]"
    }
}
