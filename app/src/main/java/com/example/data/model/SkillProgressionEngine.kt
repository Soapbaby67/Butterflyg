package com.example.data.model

data class SkillXpState(
    val mightXp: Int = 0,
    val stealthXp: Int = 0,
    val insightXp: Int = 0,
    val loreXp: Int = 0
) {
    fun xpFor(skill: SkillType): Int = when (skill) {
        SkillType.MIGHT -> mightXp.coerceAtLeast(0)
        SkillType.STEALTH -> stealthXp.coerceAtLeast(0)
        SkillType.INSIGHT -> insightXp.coerceAtLeast(0)
        SkillType.LORE -> loreXp.coerceAtLeast(0)
    }

    fun withXp(skill: SkillType, value: Int): SkillXpState {
        val safe = value.coerceAtLeast(0)
        return when (skill) {
            SkillType.MIGHT -> copy(mightXp = safe)
            SkillType.STEALTH -> copy(stealthXp = safe)
            SkillType.INSIGHT -> copy(insightXp = safe)
            SkillType.LORE -> copy(loreXp = safe)
        }
    }
}

data class SkillLevelUpEvent(
    val skill: SkillType,
    val previousLevel: Long,
    val newLevel: Long,
    val turnNumber: Long,
    val xpEarned: Int
)

data class SkillProgressionResult(
    val updatedSkills: HeroSkills,
    val updatedXp: SkillXpState,
    val trainedSkill: SkillType,
    val xpEarned: Int,
    val levelUp: SkillLevelUpEvent?
)

/**
 * Pure deterministic RPG skill progression engine.
 * Awards XP based on the hero's chosen action, approach, and risky roll outcome,
 * automatically leveling skills from 1 to 10 with smooth pacing.
 */
object SkillProgressionEngine {

    const val MAX_SKILL_LEVEL = 10L
    const val MIN_SKILL_LEVEL = 1L

    fun xpRequiredForNextLevel(currentLevel: Long): Int {
        val level = currentLevel.coerceIn(MIN_SKILL_LEVEL, MAX_SKILL_LEVEL)
        if (level >= MAX_SKILL_LEVEL) return 0
        return when (level) {
            1L -> 40
            2L -> 55
            3L -> 70
            4L -> 90
            5L -> 115
            6L -> 140
            7L -> 175
            8L -> 215
            else -> 260
        }
    }

    fun progressFraction(currentLevel: Long, currentXp: Int): Float {
        if (currentLevel >= MAX_SKILL_LEVEL) return 1f
        val required = xpRequiredForNextLevel(currentLevel)
        if (required <= 0) return 1f
        return (currentXp.coerceAtLeast(0).toFloat() / required.toFloat()).coerceIn(0f, 1f)
    }

    fun classifyActionSkill(
        playerAction: String,
        choiceApproach: String = ""
    ): SkillType {
        val combined = "${choiceApproach.trim()} ${playerAction.trim()}".lowercase()

        if (containsAny(
                combined,
                "скрыт", "тень", "тихо", "подкра", "прокра", "спрят", "взлом", "укра",
                "обман", "блеф", "слеж", "ускольз", "незамет", "маскир", "подслуш",
                "засад", "уклон", "бегств", "осторож"
            )
        ) {
            return SkillType.STEALTH
        }

        if (containsAny(
                combined,
                "натиск", "силов", "атак", "удар", "драк", "схват", "слом", "прорв",
                "защит", "меч", "выстрел", "выбить", "дерзк", "вызов", "напрям",
                "прикрыт", "оттолкну", "рискнуть в бою", "бой"
            )
        ) {
            return SkillType.MIGHT
        }

        if (containsAny(
                combined,
                "изуч", "осмотр", "анализ", "расслед", "провер", "маги", "заклинан",
                "артефакт", "книг", "архив", "шифр", "механизм", "взломать терминал",
                "хак", "техн", "наук", "алхим", "улик", "запис", "расшифр", "нестандарт",
                "эрудиц", "знан", "ритуал"
            )
        ) {
            return SkillType.LORE
        }

        if (containsAny(
                combined,
                "диплом", "убед", "договор", "поговор", "спрос", "успок", "поддерж",
                "помоч", "роман", "флирт", "нежн", "эмоц", "чувств", "союз", "довери",
                "забот", "перегов", "сочувств", "выслуш", "проницат", "обратиться к"
            )
        ) {
            return SkillType.INSIGHT
        }

        // Fallback via RiskyChoiceResolver mapping if explicit keywords didn't match
        return RiskyChoiceResolver.detectRelevantSkill(
            TurnChoice(approach = choiceApproach, text = playerAction)
        )
    }

    fun calculateXpReward(playerAction: String): Int {
        val upper = playerAction.uppercase()
        val base = when {
            upper.contains("ТРИУМФ") -> 42
            upper.contains("С ЦЕНОЙ") || upper.contains("ПОВОРОТ СУДЬБЫ") -> 34
            upper.contains("СРЫВ") || upper.contains("ОСЛОЖНЕНИЕ") -> 28
            else -> 24
        }
        val cleanLength = playerAction
            .substringBefore("[Бросок судьбы")
            .substringBefore("[РИСКОВЫЙ")
            .trim()
            .length
        val detailBonus = if (cleanLength >= 45) 8 else 0
        return base + detailBonus
    }

    fun evaluateTurnProgression(
        currentSkills: HeroSkills,
        currentXp: SkillXpState,
        playerAction: String,
        turnNumber: Long,
        choiceApproach: String = ""
    ): SkillProgressionResult {
        val safeSkills = currentSkills.coerced()
        val skill = classifyActionSkill(playerAction, choiceApproach)
        val earned = calculateXpReward(playerAction)

        var level = safeSkills.levelFor(skill)
        var xp = currentXp.xpFor(skill) + earned
        val startLevel = level

        while (level < MAX_SKILL_LEVEL) {
            val needed = xpRequiredForNextLevel(level)
            if (needed <= 0 || xp < needed) break
            xp -= needed
            level += 1L
        }
        if (level >= MAX_SKILL_LEVEL) {
            xp = 0
        }

        val updatedSkills = setSkillLevel(safeSkills, skill, level)
        val updatedXp = currentXp.withXp(skill, xp)
        val levelUp = if (level > startLevel) {
            SkillLevelUpEvent(
                skill = skill,
                previousLevel = startLevel,
                newLevel = level,
                turnNumber = turnNumber,
                xpEarned = earned
            )
        } else {
            null
        }

        return SkillProgressionResult(
            updatedSkills = updatedSkills,
            updatedXp = updatedXp,
            trainedSkill = skill,
            xpEarned = earned,
            levelUp = levelUp
        )
    }

    private fun setSkillLevel(skills: HeroSkills, skill: SkillType, level: Long): HeroSkills {
        val safe = level.coerceIn(MIN_SKILL_LEVEL, MAX_SKILL_LEVEL)
        return when (skill) {
            SkillType.MIGHT -> skills.copy(might = safe)
            SkillType.STEALTH -> skills.copy(stealth = safe)
            SkillType.INSIGHT -> skills.copy(insight = safe)
            SkillType.LORE -> skills.copy(lore = safe)
        }
    }

    private fun containsAny(text: String, vararg tokens: String): Boolean =
        tokens.any { text.contains(it) }
}
