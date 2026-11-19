package com.example.data.model

enum class AchievementCategory(val labelRu: String) {
    STORY("Сюжет"),
    RISK("Риск"),
    MASTERY("Мастерство"),
    BONDS("Связи"),
    EXPLORATION("Летопись")
}

data class ChronicleAchievementDef(
    val id: String,
    val titleRu: String,
    val descriptionRu: String,
    val category: AchievementCategory,
    val targetProgress: Int = 1
)

data class UnlockedAchievementRecord(
    val id: String,
    val unlockedAtTurn: Long = 1L,
    val unlockedAtMillis: Long = 0L
)

data class ChapterBookmark(
    val turnNumber: Long,
    val excerpt: String = "",
    val createdAtMillis: Long = 0L
)

data class AchievementStatus(
    val def: ChronicleAchievementDef,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val targetProgress: Int,
    val unlockedAtTurn: Long? = null
) {
    val progressFraction: Float
        get() = if (targetProgress <= 0) {
            if (isUnlocked) 1f else 0f
        } else {
            (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f)
        }
}

data class AchievementEvaluationResult(
    val statuses: List<AchievementStatus>,
    val updatedRecords: List<UnlockedAchievementRecord>,
    val newlyUnlocked: List<ChronicleAchievementDef>
)

/**
 * Pure deterministic engine evaluating 12 chronicle milestones on-device without network traffic.
 */
object ChronicleAchievementEngine {

    val allDefinitions: List<ChronicleAchievementDef> = listOf(
        ChronicleAchievementDef(
            id = "first_butterfly",
            titleRu = "Первый взмах крыла",
            descriptionRu = "Зафиксировать первый сдвиг канона в «Эффекте бабочки».",
            category = AchievementCategory.STORY,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "point_of_no_return",
            titleRu = "Точка невозврата",
            descriptionRu = "Совершить критический поворот, навсегда меняющий ход истории.",
            category = AchievementCategory.STORY,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "cool_headed_risk",
            titleRu = "Хладнокровный риск",
            descriptionRu = "Одержать триумф в рискованной проверке рулетки судьбы.",
            category = AchievementCategory.RISK,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "fate_tempered",
            titleRu = "Закалённый судьбой",
            descriptionRu = "Успешно пройти 3 рискованные проверки (триумф или успех с ценой).",
            category = AchievementCategory.RISK,
            targetProgress = 3
        ),
        ChronicleAchievementDef(
            id = "path_of_mastery",
            titleRu = "Путь мастера",
            descriptionRu = "Повысить любой навык героя автоматически за счёт своих действий.",
            category = AchievementCategory.MASTERY,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "peak_craft",
            titleRu = "Вершина ремесла",
            descriptionRu = "Развить любую характеристику героя до уровня 7 или выше.",
            category = AchievementCategory.MASTERY,
            targetProgress = 7
        ),
        ChronicleAchievementDef(
            id = "true_ally",
            titleRu = "Верный союзник",
            descriptionRu = "Заслужить глубокое доверие персонажа (стадия «Союзник», +75 и выше).",
            category = AchievementCategory.BONDS,
            targetProgress = 75
        ),
        ChronicleAchievementDef(
            id = "shadow_of_enmity",
            titleRu = "Тень вражды",
            descriptionRu = "Нажить непримиримого соперника (доверие NPC −50 и ниже).",
            category = AchievementCategory.BONDS,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "chronicler_10",
            titleRu = "Летописец",
            descriptionRu = "Пройти 10 глав в рамках одной хроники.",
            category = AchievementCategory.EXPLORATION,
            targetProgress = 10
        ),
        ChronicleAchievementDef(
            id = "tome_keeper_25",
            titleRu = "Хранитель тома",
            descriptionRu = "Пройти 25 глав и превратить кампанию в полноценную книгу.",
            category = AchievementCategory.EXPLORATION,
            targetProgress = 25
        ),
        ChronicleAchievementDef(
            id = "crossroads_of_worlds",
            titleRu = "Перекрёсток миров",
            descriptionRu = "Создать альтернативную ветку реальности («Форк хроники»).",
            category = AchievementCategory.EXPLORATION,
            targetProgress = 1
        ),
        ChronicleAchievementDef(
            id = "marked_pages",
            titleRu = "Отмеченные страницы",
            descriptionRu = "Сохранить в закладки 3 любимые главы хроники.",
            category = AchievementCategory.EXPLORATION,
            targetProgress = 3
        )
    )

    fun evaluate(
        campaign: Campaign,
        turns: List<CampaignTurn>,
        bookmarks: List<ChapterBookmark>,
        existingRecords: List<UnlockedAchievementRecord>,
        skillLevelUpCount: Int,
        hasForkedChronicle: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): AchievementEvaluationResult {
        val existingById = existingRecords.associateBy { it.id }
        val currentTurn = maxOf(campaign.turnCount, turns.lastOrNull()?.turnNumber ?: 1L)

        val butterflyCount = campaign.butterflyEvents.size
        val hasCriticalButterfly = campaign.butterflyEvents.any {
            it.impactLevel.equals("critical", ignoreCase = true)
        }
        val triumphCount = turns.count {
            it.playerAction.contains("ТРИУМФ", ignoreCase = true)
        }
        val successfulRiskCount = turns.count { action ->
            val text = action.playerAction
            text.contains("ТРИУМФ", ignoreCase = true) ||
                text.contains("С ЦЕНОЙ", ignoreCase = true) ||
                text.contains("ПОВОРОТ СУДЬБЫ", ignoreCase = true)
        }
        val maxSkillLevel = SkillType.entries.maxOf { campaign.skills.levelFor(it) }.toInt()
        val maxNpcTrust = campaign.npcs.maxOfOrNull { it.trustLevel }?.toInt()?.coerceAtLeast(0) ?: 0
        val hasEnemyNpc = campaign.npcs.any { it.trustLevel <= -50L }
        val chaptersCount = currentTurn.toInt()
        val isForkedTitle = campaign.title.contains("(ветка", ignoreCase = true)
        val forkedFlag = hasForkedChronicle || isForkedTitle
        val bookmarksCount = bookmarks.size

        val newlyUnlocked = mutableListOf<ChronicleAchievementDef>()
        val recordsMap = LinkedHashMap<String, UnlockedAchievementRecord>(existingById)

        val statuses = allDefinitions.map { def ->
            val progress = when (def.id) {
                "first_butterfly" -> butterflyCount.coerceAtMost(1)
                "point_of_no_return" -> if (hasCriticalButterfly) 1 else 0
                "cool_headed_risk" -> triumphCount.coerceAtMost(1)
                "fate_tempered" -> successfulRiskCount.coerceAtMost(3)
                "path_of_mastery" -> skillLevelUpCount.coerceAtMost(1)
                "peak_craft" -> maxSkillLevel.coerceAtMost(7)
                "true_ally" -> maxNpcTrust.coerceAtMost(75)
                "shadow_of_enmity" -> if (hasEnemyNpc) 1 else 0
                "chronicler_10" -> chaptersCount.coerceAtMost(10)
                "tome_keeper_25" -> chaptersCount.coerceAtMost(25)
                "crossroads_of_worlds" -> if (forkedFlag) 1 else 0
                "marked_pages" -> bookmarksCount.coerceAtMost(3)
                else -> 0
            }

            val previouslyUnlocked = recordsMap[def.id]
            val unlockedNow = previouslyUnlocked != null || progress >= def.targetProgress
            if (unlockedNow && previouslyUnlocked == null) {
                recordsMap[def.id] = UnlockedAchievementRecord(
                    id = def.id,
                    unlockedAtTurn = currentTurn,
                    unlockedAtMillis = nowMillis
                )
                newlyUnlocked.add(def)
            }

            val record = recordsMap[def.id]
            AchievementStatus(
                def = def,
                isUnlocked = unlockedNow,
                currentProgress = if (unlockedNow) def.targetProgress else progress,
                targetProgress = def.targetProgress,
                unlockedAtTurn = record?.unlockedAtTurn
            )
        }

        return AchievementEvaluationResult(
            statuses = statuses,
            updatedRecords = recordsMap.values.toList(),
            newlyUnlocked = newlyUnlocked
        )
    }
}
