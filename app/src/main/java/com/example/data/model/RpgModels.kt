package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
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

@IgnoreExtraProperties
data class NpcRelation(
    val name: String = "",
    val role: String = "",
    val trustLevel: Long = 0L,
    val statusLabel: String = "",
    val lastEmotion: String = "",
    val lastEmotionCategory: String = "neutral"
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "role" to role.take(120),
        "trustLevel" to trustLevel.coerceIn(-100L, 100L),
        "statusLabel" to statusLabel.take(100),
        "lastEmotion" to lastEmotion.take(80),
        "lastEmotionCategory" to EmotionCategory.fromKey(lastEmotionCategory).key
    )
}

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

@IgnoreExtraProperties
data class TurnChoice(
    val id: String = "",
    val approach: String = "",
    val text: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id.take(24),
        "approach" to approach.take(60),
        "text" to text.take(400)
    )
}

@IgnoreExtraProperties
data class Campaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val universe: String = "",
    val characterSummary: String = "",
    val startingPoint: String = "",
    val toneAndDifficulty: String = "",
    val turnCount: Long = 0L,
    val hudLocation: String = "",
    val hudTime: String = "",
    val hudStatusAndInventory: String = "",
    val hudRelationsAndCanon: String = "",
    val canonDeviationPercent: Long = 0L,
    val activeNpc: NpcEmotionSnapshot = NpcEmotionSnapshot(),
    val inventory: List<String> = emptyList(),
    val npcs: List<NpcRelation> = emptyList(),
    val butterflyEvents: List<ButterflyEvent> = emptyList(),
    val storySummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val formattedHudLine: String
        get() = "[$hudLocation | $hudTime | $hudStatusAndInventory | $hudRelationsAndCanon]"
}

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
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class SessionSetupInput(
    val title: String,
    val universe: String,
    val characterSummary: String,
    val startingPoint: String,
    val toneAndDifficulty: String
)

data class UniverseTemplate(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val universe: String,
    val characterSummary: String,
    val startingPoint: String,
    val toneAndDifficulty: String
)

object UniversePresets {
    val templates: List<UniverseTemplate> = listOf(
        UniverseTemplate(
            id = "naruto_chunin",
            badge = "Аниме",
            title = "Тень Конохи",
            subtitle = "Скрытый Лист перед экзаменом на чунина",
            universe = "Наруто (мир шиноби, канон раннего периода)",
            characterSummary = "Попаданец: Рэн Такахаси, генин-тактик, владеет базовыми печатями и анализом чакры, помнит ключевые события канона.",
            startingPoint = "Скрытый Лист, утро за три дня до начала экзамена на чунина.",
            toneAndDifficulty = "Мрачный реализм без сюжетной брони: ошибки ведут к раскрытию, ранениям или гибели."
        ),
        UniverseTemplate(
            id = "witcher_novigrad",
            badge = "Фэнтези",
            title = "Огни Новиграда",
            subtitle = "Интриги вольного города и охота на чародеев",
            universe = "Ведьмак (Северные королевства, эпоха Третьей Северной войны)",
            characterSummary = "Свой персонаж (OC): Веслав из Оксенфурта, бывший лекарь и алхимик-отступник с острой наблюдательностью и серебряным стилетом.",
            startingPoint = "Дождливые доки Новиграда в день первого костра на Иераршей площади.",
            toneAndDifficulty = "Суровый политический реализм и моральная серость, высокая цена каждого решения."
        ),
        UniverseTemplate(
            id = "aot_wall_maria",
            badge = "Аниме",
            title = "За Стеной Мария",
            subtitle = "Хрупкий мир за мгновение до катастрофы",
            universe = "Атака титанов (Парадиз, 845 год)",
            characterSummary = "Свой персонаж (OC): Марта Крюгер, новобранец гарнизона с отличным владением УПМ и страхом высоты, который приходится преодолевать.",
            startingPoint = "Район Сигансина, дежурство на южном участке стены за час до появления Колоссального титана.",
            toneAndDifficulty = "Жёсткое выживание и триллер: промедление или паника смертельны."
        ),
        UniverseTemplate(
            id = "hogwarts_secrets",
            badge = "Книга",
            title = "Шёпот Слизерина",
            subtitle = "Запретная секция и тайны старого замка",
            universe = "Гарри Поттер (Хогвартс, 1992 год)",
            characterSummary = "Свой персонаж (OC): Адриан Флинт-младший (или студент-когтевранец), силён в чарах и легилименции, слаб в дуэлях.",
            startingPoint = "Библиотека Хогвартса поздним вечером после первого загадочного нападения.",
            toneAndDifficulty = "Детективная мистика с напряжёнными социальными последствиями и интригами факультетов."
        ),
        UniverseTemplate(
            id = "cyberpunk_nightcity",
            badge = "Киберпанк",
            title = "Дождь Уотсона",
            subtitle = "Наёмники, корпорации и цена хрома",
            universe = "Cyberpunk 2077 (Найт-Сити, 2077 год)",
            characterSummary = "Свой персонаж (OC): Кай «Вектор», нетраннер-одиночка с изношенной кибердекой и долгом фиксеру.",
            startingPoint = "Тесный бар в Кабуки во время сделки по перехвату корпоративного чипа.",
            toneAndDifficulty = "Нео-нуар и высокий риск: предательство и киберпсихоз всегда рядом."
        )
    )
}
