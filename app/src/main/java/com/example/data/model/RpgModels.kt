package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class EmotionCategory(val key: String, val russianDefault: String) {
    JOY("joy", "Радость"),
    EMBARRASSMENT("embarrassment", "Смущение"),
    ANGER("anger", "Злость"),
    FEAR("fear", "Тревога"),
    SUSPICION("suspicion", "Подозрение"),
    SADNESS("sadness", "Печаль"),
    SURPRISE("surprise", "Удивление"),
    CALM("calm", "Спокойствие"),
    NEUTRAL("neutral", "Нейтрально");

    companion object {
        fun fromKey(key: String?): EmotionCategory {
            val normalized = key?.trim()?.lowercase() ?: return NEUTRAL
            return entries.firstOrNull { it.key == normalized } ?: NEUTRAL
        }
    }
}

enum class PlayerGender(
    val key: String,
    val labelRu: String,
    val grammarHintRu: String
) {
    MALE(
        key = "male",
        labelRu = "Мужской",
        grammarHintRu = "Мужской род (он, сказал, вошёл, заметил, сам). Никогда не путай род глаголов и прилагательных."
    ),
    FEMALE(
        key = "female",
        labelRu = "Женский",
        grammarHintRu = "Женский род (она, сказала, вошла, заметила, сама). Никогда не путай род глаголов и прилагательных."
    ),
    NEUTRAL(
        key = "neutral",
        labelRu = "Нейтральный",
        grammarHintRu = "Обращайся на «ты» без привязки к полу, избегая ошибочной смены грамматического рода."
    );

    companion object {
        fun fromKey(raw: String?): PlayerGender {
            val normalized = raw?.trim()?.lowercase() ?: return NEUTRAL
            return entries.firstOrNull { it.key == normalized } ?: NEUTRAL
        }
    }
}

enum class ItemCategory(val key: String, val labelRu: String) {
    WEAPON("weapon", "Оружие"),
    GEAR("gear", "Снаряжение"),
    CONSUMABLE("consumable", "Расходник"),
    CLUE("clue", "Улика/Документ"),
    KEY_ITEM("key_item", "Ключевой предмет");

    companion object {
        fun fromKey(raw: String?): ItemCategory {
            val normalized = raw?.trim()?.lowercase() ?: return GEAR
            return entries.firstOrNull { it.key == normalized } ?: when {
                normalized.contains("оруж") || normalized.contains("weapon") -> WEAPON
                normalized.contains("расход") || normalized.contains("зель") || normalized.contains("consum") -> CONSUMABLE
                normalized.contains("улик") || normalized.contains("докум") || normalized.contains("clue") -> CLUE
                normalized.contains("ключ") || normalized.contains("квест") || normalized.contains("key") -> KEY_ITEM
                else -> GEAR
            }
        }

        fun inferFromName(name: String): ItemCategory {
            val lower = name.lowercase()
            return when {
                lower.contains("меч") || lower.contains("кинжал") || lower.contains("стилет") ||
                    lower.contains("нож") || lower.contains("пистолет") || lower.contains("клинок") ||
                    lower.contains("лук") || lower.contains("кунай") || lower.contains("посох") ||
                    lower.contains("палочк") || lower.contains("винтовк") || lower.contains("дубинк") -> WEAPON

                lower.contains("зель") || lower.contains("эликсир") || lower.contains("бинт") ||
                    lower.contains("аптеч") || lower.contains("лекар") || lower.contains("кофе") ||
                    lower.contains("хлеб") || lower.contains("еда") || lower.contains("фляг") ||
                    lower.contains("стимулятор") || lower.contains("снадоб") || lower.contains("трав") -> CONSUMABLE

                lower.contains("письм") || lower.contains("записк") || lower.contains("документ") ||
                    lower.contains("дневник") || lower.contains("блокнот") || lower.contains("конспект") ||
                    lower.contains("карта") || lower.contains("досье") || lower.contains("пропуск") ||
                    lower.contains("улик") || lower.contains("чип") || lower.contains("флешк") ||
                    lower.contains("фото") || lower.contains("книг") -> CLUE

                lower.contains("ключ") || lower.contains("медальон") || lower.contains("амулет") ||
                    lower.contains("артефакт") || lower.contains("печат") || lower.contains("кольц") ||
                    lower.contains("жетон") || lower.contains("талисман") -> KEY_ITEM

                else -> GEAR
            }
        }
    }
}

enum class QuestStatus(val key: String, val labelRu: String) {
    ACTIVE("active", "Активен"),
    COMPLETED("completed", "Выполнен"),
    FAILED("failed", "Провален");

    companion object {
        fun fromKey(raw: String?): QuestStatus {
            val normalized = raw?.trim()?.lowercase() ?: return ACTIVE
            return entries.firstOrNull { it.key == normalized } ?: when {
                normalized.contains("выполн") || normalized.contains("заверш") || normalized.contains("done") -> COMPLETED
                normalized.contains("провал") || normalized.contains("fail") -> FAILED
                else -> ACTIVE
            }
        }
    }
}

enum class SkillType(
    val key: String,
    val labelRu: String,
    val shortHintRu: String
) {
    MIGHT("might", "Натиск", "Сила, бой и прямое давление"),
    STEALTH("stealth", "Скрытность", "Ловкость, тень и хитрость"),
    INSIGHT("insight", "Проницательность", "Убеждение, эмпатия и чтение людей"),
    LORE("lore", "Эрудиция", "Знания, магия, техника и анализ");

    companion object {
        fun fromKey(raw: String?): SkillType {
            val normalized = raw?.trim()?.lowercase() ?: return INSIGHT
            return entries.firstOrNull { it.key == normalized } ?: INSIGHT
        }
    }
}

@Keep
@IgnoreExtraProperties
data class HeroSkills(
    val might: Long = 5L,
    val stealth: Long = 5L,
    val insight: Long = 5L,
    val lore: Long = 5L
) {
    fun coerced(): HeroSkills = copy(
        might = might.coerceIn(1L, 10L),
        stealth = stealth.coerceIn(1L, 10L),
        insight = insight.coerceIn(1L, 10L),
        lore = lore.coerceIn(1L, 10L)
    )

    fun levelFor(type: SkillType): Long = when (type) {
        SkillType.MIGHT -> might.coerceIn(1L, 10L)
        SkillType.STEALTH -> stealth.coerceIn(1L, 10L)
        SkillType.INSIGHT -> insight.coerceIn(1L, 10L)
        SkillType.LORE -> lore.coerceIn(1L, 10L)
    }

    fun withSkill(type: SkillType, value: Long): HeroSkills {
        val clamped = value.coerceIn(1L, 10L)
        return when (type) {
            SkillType.MIGHT -> copy(might = clamped)
            SkillType.STEALTH -> copy(stealth = clamped)
            SkillType.INSIGHT -> copy(insight = clamped)
            SkillType.LORE -> copy(lore = clamped)
        }
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "might" to might.coerceIn(1L, 10L),
        "stealth" to stealth.coerceIn(1L, 10L),
        "insight" to insight.coerceIn(1L, 10L),
        "lore" to lore.coerceIn(1L, 10L)
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): HeroSkills {
            if (map == null) return HeroSkills()
            return HeroSkills(
                might = (map["might"] as? Number)?.toLong() ?: 5L,
                stealth = (map["stealth"] as? Number)?.toLong() ?: 5L,
                insight = (map["insight"] as? Number)?.toLong() ?: 5L,
                lore = (map["lore"] as? Number)?.toLong() ?: 5L
            ).coerced()
        }
    }
}

@Keep
@IgnoreExtraProperties
data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = ItemCategory.GEAR.key,
    val quantity: Long = 1L,
    @get:PropertyName("isEquipped")
    @field:PropertyName("isEquipped")
    val isEquipped: Boolean = false,
    val description: String = ""
) {
    fun resolvedCategory(): ItemCategory = ItemCategory.fromKey(category)

    fun resolvedId(): String = id.ifBlank {
        name.trim().lowercase().replace(Regex("[^a-zа-я0-9]+"), "_").trim('_').take(36)
            .ifBlank { "item" }
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to resolvedId().take(48),
        "name" to name.trim().take(120),
        "category" to resolvedCategory().key,
        "quantity" to quantity.coerceIn(1L, 999L),
        "isEquipped" to isEquipped,
        "description" to description.trim().take(240)
    )

    companion object {
        fun fromLegacyName(rawName: String, index: Int = 0): InventoryItem {
            val clean = rawName.trim().replaceFirstChar { it.uppercase() }
            val cat = ItemCategory.inferFromName(clean)
            return InventoryItem(
                id = "legacy_${index}_${clean.lowercase().take(20).replace(Regex("[^a-zа-я0-9]+"), "_")}",
                name = clean.take(120),
                category = cat.key,
                quantity = 1L,
                isEquipped = cat == ItemCategory.WEAPON,
                description = ""
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class QuestEntry(
    val id: String = "",
    val title: String = "",
    val status: String = QuestStatus.ACTIVE.key,
    val updatedAtTurn: Long = 1L
) {
    fun resolvedStatus(): QuestStatus = QuestStatus.fromKey(status)

    fun resolvedId(): String = id.ifBlank {
        title.trim().lowercase().replace(Regex("[^a-zа-я0-9]+"), "_").trim('_').take(40)
            .ifBlank { "quest" }
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to resolvedId().take(48),
        "title" to title.trim().take(200),
        "status" to resolvedStatus().key,
        "updatedAtTurn" to updatedAtTurn.coerceAtLeast(1L)
    )
}

@Keep
@IgnoreExtraProperties
data class NpcEmotionSnapshot(
    val hasActiveNpc: Boolean = false,
    val name: String = "",
    val emotionLabel: String = "",
    val emotionCategory: String = "neutral",
    val intensity: Long = 1L,
    val note: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "hasActiveNpc" to hasActiveNpc,
        "name" to name.take(100),
        "emotionLabel" to emotionLabel.take(80),
        "emotionCategory" to EmotionCategory.fromKey(emotionCategory).key,
        "intensity" to intensity.coerceIn(0L, 5L),
        "note" to note.take(300)
    )
}

@Keep
@IgnoreExtraProperties
data class NpcRelation(
    val name: String = "",
    val role: String = "",
    val trustLevel: Long = 0L,
    val statusLabel: String = "",
    val lastEmotion: String = "",
    val lastEmotionCategory: String = "neutral",
    val archetype: String = "",
    val personality: String = "",
    val boundaries: String = "",
    val recentActions: List<String> = emptyList()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "role" to role.take(120),
        "trustLevel" to trustLevel.coerceIn(-100L, 100L),
        "statusLabel" to statusLabel.take(100),
        "lastEmotion" to lastEmotion.take(80),
        "lastEmotionCategory" to EmotionCategory.fromKey(lastEmotionCategory).key,
        "archetype" to archetype.take(32),
        "personality" to personality.take(180),
        "boundaries" to boundaries.take(180),
        "recentActions" to recentActions
            .map { it.trim().take(140) }
            .filter { it.isNotBlank() }
            .takeLast(3)
    )
}

@Keep
@IgnoreExtraProperties
data class ButterflyEvent(
    val turnNumber: Long = 1L,
    val title: String = "",
    val consequence: String = "",
    val impactLevel: String = "minor"
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "turnNumber" to turnNumber.coerceAtLeast(1L),
        "title" to title.take(160),
        "consequence" to consequence.take(400),
        "impactLevel" to impactLevel.lowercase().let {
            if (it in setOf("minor", "major", "critical")) it else "minor"
        }
    )
}

enum class RiskOutcomeTier(val key: String, val labelRu: String) {
    TRIUMPH("triumph", "Триумф"),
    MIXED("mixed", "С ценой"),
    SETBACK("setback", "Срыв"),
    TWIST("twist", "Поворот судьбы");

    companion object {
        fun fromKey(key: String?): RiskOutcomeTier {
            val normalized = key?.trim()?.lowercase() ?: return MIXED
            return entries.firstOrNull { it.key == normalized } ?: MIXED
        }
    }
}

@Keep
@IgnoreExtraProperties
data class RiskOutcome(
    val id: String = "",
    val label: String = "",
    val description: String = "",
    val tier: String = "mixed",
    val weight: Long = 25L
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id.take(24),
        "label" to label.take(80),
        "description" to description.take(240),
        "tier" to RiskOutcomeTier.fromKey(tier).key,
        "weight" to weight.coerceIn(5L, 80L)
    )
}

@Keep
@IgnoreExtraProperties
data class TurnChoice(
    val id: String = "",
    val approach: String = "",
    val text: String = "",
    @get:PropertyName("isRisky")
    @field:PropertyName("isRisky")
    val isRisky: Boolean = false,
    val riskOutcomes: List<RiskOutcome> = emptyList()
) {
    fun toFirestoreMap(): Map<String, Any> = buildMap {
        put("id", id.take(24))
        put("approach", approach.take(60))
        put("text", text.take(400))
        put("isRisky", isRisky)
        if (riskOutcomes.isNotEmpty()) {
            put("riskOutcomes", riskOutcomes.take(4).map { it.toFirestoreMap() })
        }
    }
}

@Keep
@IgnoreExtraProperties
data class TurnStateSnapshot(
    val hudLocation: String = "",
    val hudTime: String = "",
    val hudStatusAndInventory: String = "",
    val hudRelationsAndCanon: String = "",
    val canonDeviationPercent: Long = 0L,
    val healthPercent: Long = 100L,
    val currencyAmount: Long = 0L,
    val currencyLabel: String = "",
    val skills: HeroSkills = HeroSkills(),
    val inventory: List<String> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val npcs: List<NpcRelation> = emptyList(),
    val butterflyEvents: List<ButterflyEvent> = emptyList(),
    val activeQuests: List<String> = emptyList(),
    val quests: List<QuestEntry> = emptyList(),
    val storySummary: String = "",
    val chapterSummaries: List<String> = emptyList(),
    val stateChangeSummary: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "hudLocation" to hudLocation.take(200),
        "hudTime" to hudTime.take(120),
        "hudStatusAndInventory" to hudStatusAndInventory.take(300),
        "hudRelationsAndCanon" to hudRelationsAndCanon.take(300),
        "canonDeviationPercent" to canonDeviationPercent.coerceIn(0L, 100L),
        "healthPercent" to healthPercent.coerceIn(0L, 100L),
        "currencyAmount" to currencyAmount.coerceAtLeast(0L),
        "currencyLabel" to currencyLabel.take(80),
        "skills" to skills.toFirestoreMap(),
        "inventory" to inventory.take(40).map { it.take(120) },
        "inventoryItems" to inventoryItems.take(40).map { it.toFirestoreMap() },
        "npcs" to npcs.take(40).map { it.toFirestoreMap() },
        "butterflyEvents" to butterflyEvents.take(40).map { it.toFirestoreMap() },
        "activeQuests" to activeQuests.take(10).map { it.take(160) },
        "quests" to quests.take(24).map { it.toFirestoreMap() },
        "storySummary" to storySummary.take(4000),
        "chapterSummaries" to chapterSummaries.take(25).map { it.take(500) },
        "stateChangeSummary" to stateChangeSummary.take(400)
    )
}

@Keep
@IgnoreExtraProperties
data class Campaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val universe: String = "",
    val characterSummary: String = "",
    val playerName: String = "",
    val playerGender: String = PlayerGender.NEUTRAL.key,
    val startingPoint: String = "",
    val toneAndDifficulty: String = "",
    val turnCount: Long = 0L,
    val hudLocation: String = "",
    val hudTime: String = "",
    val hudStatusAndInventory: String = "",
    val hudRelationsAndCanon: String = "",
    val canonDeviationPercent: Long = 0L,
    val healthPercent: Long = -1L,
    val currencyAmount: Long = 0L,
    val currencyLabel: String = "",
    val skills: HeroSkills = HeroSkills(),
    val activeNpc: NpcEmotionSnapshot = NpcEmotionSnapshot(),
    val inventory: List<String> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val npcs: List<NpcRelation> = emptyList(),
    val butterflyEvents: List<ButterflyEvent> = emptyList(),
    val activeQuests: List<String> = emptyList(),
    val quests: List<QuestEntry> = emptyList(),
    val storySummary: String = "",
    val chapterSummaries: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val formattedHudLine: String
        get() = "[$hudLocation | $hudTime | $hudStatusAndInventory | $hudRelationsAndCanon]"

    fun resolvedGender(): PlayerGender = PlayerGender.fromKey(playerGender)

    fun resolvedInventoryItems(): List<InventoryItem> {
        if (inventoryItems.isNotEmpty()) {
            return inventoryItems.filter { it.name.isNotBlank() }
        }
        return inventory
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .mapIndexed { idx, name -> InventoryItem.fromLegacyName(name, idx) }
    }

    fun resolvedQuests(): List<QuestEntry> {
        if (quests.isNotEmpty()) {
            return quests.filter { it.title.isNotBlank() }
        }
        return activeQuests
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .mapIndexed { idx, title ->
                QuestEntry(
                    id = "legacy_quest_$idx",
                    title = title,
                    status = QuestStatus.ACTIVE.key,
                    updatedAtTurn = turnCount.coerceAtLeast(1L)
                )
            }
    }

    fun resolvedActiveQuestTitles(): List<String> =
        resolvedQuests()
            .filter { it.resolvedStatus() == QuestStatus.ACTIVE }
            .map { it.title }
}

@Keep
@IgnoreExtraProperties
data class CampaignTurn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campaignId: String = "",
    val turnNumber: Long = 1L,
    val playerAction: String = "",
    val narrative: String = "",
    val hudLine: String = "",
    val activeNpc: NpcEmotionSnapshot = NpcEmotionSnapshot(),
    val choices: List<TurnChoice> = emptyList(),
    val stateSnapshot: TurnStateSnapshot? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
data class SessionSetupInput(
    val title: String,
    val universe: String,
    val characterSummary: String,
    val startingPoint: String,
    val toneAndDifficulty: String,
    val playerName: String = "",
    val playerGender: PlayerGender = PlayerGender.NEUTRAL,
    val skills: HeroSkills = HeroSkills()
)

enum class ScenarioGenre(val labelRu: String) {
    ALL("Все"),
    ROMANCE("Романтика"),
    ROUTINE("Рутина и уют"),
    FANTASY("Фэнтези"),
    ANIME("Аниме"),
    MYSTERY_SCIFI("Мистика и Sci-Fi")
}

data class UniverseTemplate(
    val id: String,
    val genre: ScenarioGenre,
    val badge: String,
    val title: String,
    val subtitle: String,
    val universe: String,
    val characterSummary: String,
    val startingPoint: String,
    val toneAndDifficulty: String,
    val defaultPlayerName: String = "",
    val defaultGender: PlayerGender = PlayerGender.NEUTRAL,
    val defaultSkills: HeroSkills = HeroSkills()
)

object UniversePresets {
    val templates: List<UniverseTemplate> = listOf(
        UniverseTemplate(
            id = "romance_rainy_cafe",
            genre = ScenarioGenre.ROMANCE,
            badge = "Романтика",
            title = "Кофейня на углу",
            subtitle = "Случайные встречи, взгляды и тёплый слоубёрн под дождём",
            universe = "Современный европейский город, романтическая драма повседневности",
            characterSummary = "Свой персонаж (OC): бариста-иллюстратор с блокнотом зарисовок, тёплым чувством юмора и привычкой прятать волнение за сарказмом.",
            startingPoint = "Тёплая угловая кофейня вечером под проливным дождём, за десять минут до закрытия, когда внутрь заходит незнакомец с промокшим пальто и забытым письмом.",
            toneAndDifficulty = "Романтический слоубёрн с живым сопротивлением: персонажи держат дистанцию, проверяют искренность, отказывают при навязчивости и раскрываются постепенно.",
            defaultPlayerName = "Арина Соколова",
            defaultGender = PlayerGender.FEMALE,
            defaultSkills = HeroSkills(might = 3L, stealth = 5L, insight = 8L, lore = 6L)
        ),
        UniverseTemplate(
            id = "romance_detective_partners",
            genre = ScenarioGenre.ROMANCE,
            badge = "Романтика • Нуар",
            title = "Отдел расследований",
            subtitle = "Напарники, ночные дежурства и запретная искра",
            universe = "Современный мегаполис, детективная романтическая драма (служебный роман)",
            characterSummary = "Свой персонаж (OC): Старший аналитик с безупречной репутацией и скрытой историей из прошлого, вынужденный работать в паре с дерзким оперативником.",
            startingPoint = "Полутёмный кабинет отдела криминалистики в час ночи: на столе остывший кофе, общее дело и слишком тесное расстояние у карты города.",
            toneAndDifficulty = "Романтика на фоне трения характеров: жёсткие личные границы, профессиональное недоверие, сарказм и высокая цена любого сближения.",
            defaultPlayerName = "Артём Волков",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 5L, stealth = 5L, insight = 7L, lore = 7L)
        ),
        UniverseTemplate(
            id = "routine_herbalist_shop",
            genre = ScenarioGenre.ROUTINE,
            badge = "Рутина и уют",
            title = "Лавка травника",
            subtitle = "Утренние заказы, заварка снадобий и городские истории",
            universe = "Уютный фэнтезийный городок, бытовая магия и повседневная рутина (Slice of Life)",
            characterSummary = "Свой персонаж (OC): Хозяин маленькой аптекарской лавки, знаток трав и сплетен квартала, вечно пытающийся свести баланс и успеть выспаться.",
            startingPoint = "Солнечное утро, на прилавке звенят склянки, пахнет мятой и свежей выпечкой, а колокольчик на двери звенит первым посетителем.",
            toneAndDifficulty = "Живая повседневность без подыгрывания: соседи со своими заботами и упрямством, реальная нехватка времени/денег и постепенное доверие.",
            defaultPlayerName = "Лира",
            defaultGender = PlayerGender.FEMALE,
            defaultSkills = HeroSkills(might = 3L, stealth = 4L, insight = 7L, lore = 8L)
        ),
        UniverseTemplate(
            id = "routine_campus_dorm",
            genre = ScenarioGenre.ROUTINE,
            badge = "Повседневность",
            title = "Общежитие №4",
            subtitle = "Учёба, подработки, соседи и большие перемены",
            universe = "Современный университетский кампус, студенческая повседневность (Slice of Life)",
            characterSummary = "Свой персонаж (OC): Первокурсник-перфекционист на стипендии, совмещающий сложные пары, вечернюю подработку и шумных соседей по блоку.",
            startingPoint = "Общая кухня четвёртого этажа в 8 утра в понедельник: кофеварка сломалась, сосед по ошибке взял твой конспект, а до первой пары 25 минут.",
            toneAndDifficulty = "Реалистичная бытовая рутина: у соседей свои дела и раздражение, неловкие отказы при бестактности, бюджет и медленное сближение.",
            defaultPlayerName = "Максим",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 4L, stealth = 5L, insight = 6L, lore = 7L)
        ),
        UniverseTemplate(
            id = "witcher_novigrad",
            genre = ScenarioGenre.FANTASY,
            badge = "Фэнтези",
            title = "Огни Новиграда",
            subtitle = "Интриги вольного города и охота на чародеев",
            universe = "Ведьмак (Северные королевства, эпоха Третьей Северной войны)",
            characterSummary = "Свой персонаж (OC): Веслав из Оксенфурта, бывший лекарь и алхимик-отступник с острой наблюдательностью и серебряным стилетом.",
            startingPoint = "Дождливые доки Новиграда в день первого костра на Иераршей площади.",
            toneAndDifficulty = "Суровый политический реализм и моральная серость, высокая цена каждого решения.",
            defaultPlayerName = "Веслав из Оксенфурта",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 5L, stealth = 6L, insight = 6L, lore = 8L)
        ),
        UniverseTemplate(
            id = "naruto_chunin",
            genre = ScenarioGenre.ANIME,
            badge = "Аниме",
            title = "Тень Конохи",
            subtitle = "Скрытый Лист перед экзаменом на чунина",
            universe = "Наруто (мир шиноби, канон раннего периода)",
            characterSummary = "Попаданец: Рэн Такахаси, генин-тактик, владеет базовыми печатями и анализом чакры, помнит ключевые события канона.",
            startingPoint = "Скрытый Лист, утро за три дня до начала экзамена на чунина.",
            toneAndDifficulty = "Мрачный реализм без сюжетной брони: ошибки ведут к раскрытию, ранениям или гибели.",
            defaultPlayerName = "Рэн Такахаси",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 5L, stealth = 8L, insight = 6L, lore = 7L)
        ),
        UniverseTemplate(
            id = "aot_wall_maria",
            genre = ScenarioGenre.ANIME,
            badge = "Аниме",
            title = "За Стеной Мария",
            subtitle = "Хрупкий мир за мгновение до катастрофы",
            universe = "Атака титанов (Парадиз, 845 год)",
            characterSummary = "Свой персонаж (OC): Марта Крюгер, новобранец гарнизона с отличным владением УПМ и страхом высоты, который приходится преодолевать.",
            startingPoint = "Район Сигансина, дежурство на южном участке стены за час до появления Колоссального титана.",
            toneAndDifficulty = "Жёсткое выживание и триллер: промедление или паника смертельны.",
            defaultPlayerName = "Марта Крюгер",
            defaultGender = PlayerGender.FEMALE,
            defaultSkills = HeroSkills(might = 7L, stealth = 7L, insight = 5L, lore = 4L)
        ),
        UniverseTemplate(
            id = "hogwarts_secrets",
            genre = ScenarioGenre.MYSTERY_SCIFI,
            badge = "Мистика",
            title = "Шёпот Слизерина",
            subtitle = "Запретная секция и тайны старого замка",
            universe = "Гарри Поттер (Хогвартс, 1992 год)",
            characterSummary = "Свой персонаж (OC): Адриан Флинт-младший (или студент-когтевранец), силён в чарах и легилименции, слаб в дуэлях.",
            startingPoint = "Библиотека Хогвартса поздним вечером после первого загадочного нападения.",
            toneAndDifficulty = "Детективная мистика с напряжёнными социальными последствиями и интригами факультетов.",
            defaultPlayerName = "Адриан Флинт",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 3L, stealth = 6L, insight = 7L, lore = 9L)
        ),
        UniverseTemplate(
            id = "cyberpunk_nightcity",
            genre = ScenarioGenre.MYSTERY_SCIFI,
            badge = "Киберпанк",
            title = "Дождь Уотсона",
            subtitle = "Наёмники, корпорации и цена хрома",
            universe = "Cyberpunk 2077 (Найт-Сити, 2077 год)",
            characterSummary = "Свой персонаж (OC): Кай «Вектор», нетраннер-одиночка с изношенной кибердекой и долгом фиксеру.",
            startingPoint = "Тесный бар в Кабуки во время сделки по перехвату корпоративного чипа.",
            toneAndDifficulty = "Нео-нуар и высокий риск: предательство и киберпсихоз всегда рядом.",
            defaultPlayerName = "Кай «Вектор»",
            defaultGender = PlayerGender.MALE,
            defaultSkills = HeroSkills(might = 4L, stealth = 7L, insight = 6L, lore = 8L)
        ),
        UniverseTemplate(
            id = "space_icarus_7",
            genre = ScenarioGenre.MYSTERY_SCIFI,
            badge = "Космос • Sci-Fi",
            title = "Челнок «Икар-7»",
            subtitle = "Дальний рейс, сбои систем и тайны экипажа",
            universe = "Научная фантастика, орбитальная станция и дальний космос",
            characterSummary = "Свой персонаж (OC): Бортинженер связи на исследовательском челноке с недосыпом, ключом к грузовому отсеку и недоверием к бортовому ИИ.",
            startingPoint = "Смотровая палуба челнока на орбите голубой газовой планеты: ночная вахта и странный зашифрованный сигнал на частоте бедствия.",
            toneAndDifficulty = "Психологический сай-фай и технический реализм: нехватка кислорода/времени, конфликты экипажа и цена ошибки.",
            defaultPlayerName = "Елена Громова",
            defaultGender = PlayerGender.FEMALE,
            defaultSkills = HeroSkills(might = 4L, stealth = 5L, insight = 6L, lore = 9L)
        )
    )
}
