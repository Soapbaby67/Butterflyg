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
    val lastEmotionCategory: String = "neutral",
    val archetype: String = "",
    val personality: String = "",
    val boundaries: String = ""
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
        "boundaries" to boundaries.take(180)
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
    val toneAndDifficulty: String
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
            characterSummary = "Свой персонаж (OC): Арина (или Артём) Соколов, бариста-иллюстратор с блокнотом зарисовок, тёплым чувством юмора и привычкой прятать волнение за сарказмом.",
            startingPoint = "Тёплая угловая кофейня вечером под проливным дождём, за десять минут до закрытия, когда внутрь заходит незнакомец с промокшим пальто и забытым письмом.",
            toneAndDifficulty = "Романтический слоубёрн с живым сопротивлением: персонажи держат дистанцию, проверяют искренность, отказывают при навязчивости и раскрываются постепенно."
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
            toneAndDifficulty = "Романтика на фоне трения характеров: жёсткие личные границы, профессиональное недоверие, сарказм и высокая цена любого сближения."
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
            toneAndDifficulty = "Живая повседневность без подыгрывания: соседи со своими заботами и упрямством, реальная нехватка времени/денег и постепенное доверие."
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
            toneAndDifficulty = "Реалистичная бытовая рутина: у соседей свои дела и раздражение, неловкие отказы при бестактности, бюджет и медленное сближение."
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
            toneAndDifficulty = "Суровый политический реализм и моральная серость, высокая цена каждого решения."
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
            toneAndDifficulty = "Мрачный реализм без сюжетной брони: ошибки ведут к раскрытию, ранениям или гибели."
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
            toneAndDifficulty = "Жёсткое выживание и триллер: промедление или паника смертельны."
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
            toneAndDifficulty = "Детективная мистика с напряжёнными социальными последствиями и интригами факультетов."
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
            toneAndDifficulty = "Нео-нуар и высокий риск: предательство и киберпсихоз всегда рядом."
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
            toneAndDifficulty = "Психологический сай-фай и технический реализм: нехватка кислорода/времени, конфликты экипажа и цена ошибки."
        )
    )
}
