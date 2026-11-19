package com.example.data.repository

import android.util.Log
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCategory
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.model.PlayerGender
import com.example.data.model.QuestEntry
import com.example.data.model.QuestStatus
import com.example.data.model.RiskOutcome
import com.example.data.model.RiskOutcomeTier
import com.example.data.model.ScenarioGenre
import com.example.data.model.SessionSetupInput
import com.example.data.model.TurnChoice
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class StreamingNarrativeExtractor {
    private enum class State {
        SCAN,
        IN_KEY,
        KEY_ESCAPE,
        AFTER_KEY_EXPECT_COLON,
        AFTER_COLON_EXPECT_VALUE,
        IN_NARRATIVE,
        NARRATIVE_ESCAPE,
        NARRATIVE_UNICODE,
        IN_OTHER_STRING,
        OTHER_ESCAPE,
        DONE
    }

    private var state = State.SCAN
    private var braceDepth = 0
    private var expectObjectKeyAtDepth1 = false
    private val keyBuffer = StringBuilder()
    private val unicodeBuffer = StringBuilder(4)
    private val narrativeBuffer = StringBuilder()
    private var targetIsNarrative = false

    fun append(chunk: String): String? {
        if (chunk.isEmpty() || state == State.DONE) return null
        val prevLen = narrativeBuffer.length

        for (ch in chunk) {
            if (state == State.DONE) break
            when (state) {
                State.SCAN -> {
                    when (ch) {
                        '{' -> {
                            braceDepth++
                            expectObjectKeyAtDepth1 = (braceDepth == 1)
                        }
                        '}' -> {
                            if (braceDepth > 0) braceDepth--
                            expectObjectKeyAtDepth1 = (braceDepth == 1)
                        }
                        ',' -> {
                            if (braceDepth == 1) {
                                expectObjectKeyAtDepth1 = true
                            }
                        }
                        '"' -> {
                            if (braceDepth == 1 && expectObjectKeyAtDepth1) {
                                keyBuffer.clear()
                                state = State.IN_KEY
                            } else {
                                state = State.IN_OTHER_STRING
                            }
                        }
                    }
                }

                State.IN_KEY -> {
                    when (ch) {
                        '\\' -> state = State.KEY_ESCAPE
                        '"' -> {
                            targetIsNarrative = (keyBuffer.toString() == "narrative")
                            state = State.AFTER_KEY_EXPECT_COLON
                        }
                        else -> keyBuffer.append(ch)
                    }
                }

                State.KEY_ESCAPE -> {
                    keyBuffer.append(ch)
                    state = State.IN_KEY
                }

                State.AFTER_KEY_EXPECT_COLON -> {
                    when {
                        ch.isWhitespace() -> Unit
                        ch == ':' -> state = State.AFTER_COLON_EXPECT_VALUE
                        else -> {
                            targetIsNarrative = false
                            expectObjectKeyAtDepth1 = false
                            state = State.SCAN
                        }
                    }
                }

                State.AFTER_COLON_EXPECT_VALUE -> {
                    when {
                        ch.isWhitespace() -> Unit
                        ch == '"' -> {
                            state = if (targetIsNarrative) {
                                State.IN_NARRATIVE
                            } else {
                                State.IN_OTHER_STRING
                            }
                        }
                        ch == '{' -> {
                            braceDepth++
                            expectObjectKeyAtDepth1 = false
                            targetIsNarrative = false
                            state = State.SCAN
                        }
                        else -> {
                            expectObjectKeyAtDepth1 = false
                            targetIsNarrative = false
                            state = State.SCAN
                        }
                    }
                }

                State.IN_NARRATIVE -> {
                    when (ch) {
                        '\\' -> state = State.NARRATIVE_ESCAPE
                        '"' -> state = State.DONE
                        else -> narrativeBuffer.append(ch)
                    }
                }

                State.NARRATIVE_ESCAPE -> {
                    when (ch) {
                        'n' -> {
                            narrativeBuffer.append('\n')
                            state = State.IN_NARRATIVE
                        }
                        'r' -> state = State.IN_NARRATIVE
                        't' -> {
                            narrativeBuffer.append("    ")
                            state = State.IN_NARRATIVE
                        }
                        '"', '\\', '/' -> {
                            narrativeBuffer.append(ch)
                            state = State.IN_NARRATIVE
                        }
                        'u' -> {
                            unicodeBuffer.clear()
                            state = State.NARRATIVE_UNICODE
                        }
                        else -> {
                            narrativeBuffer.append(ch)
                            state = State.IN_NARRATIVE
                        }
                    }
                }

                State.NARRATIVE_UNICODE -> {
                    unicodeBuffer.append(ch)
                    if (unicodeBuffer.length == 4) {
                        val code = unicodeBuffer.toString().toIntOrNull(16)
                        if (code != null) {
                            narrativeBuffer.append(code.toChar())
                        }
                        state = State.IN_NARRATIVE
                    }
                }

                State.IN_OTHER_STRING -> {
                    when (ch) {
                        '\\' -> state = State.OTHER_ESCAPE
                        '"' -> {
                            if (braceDepth == 1) expectObjectKeyAtDepth1 = false
                            state = State.SCAN
                        }
                    }
                }

                State.OTHER_ESCAPE -> {
                    state = State.IN_OTHER_STRING
                }

                State.DONE -> break
            }
        }

        return if (narrativeBuffer.length > prevLen) {
            narrativeBuffer.toString()
        } else {
            null
        }
    }

    fun currentNarrative(): String = narrativeBuffer.toString()
}

object NpcTrustGuardrail {
    private const val MAX_TRUST_GAIN_PER_TURN = 12L
    private const val MAX_TRUST_LOSS_PER_TURN = 35L
    private const val MAX_STRANGER_INITIAL_TRUST = 25L
    private const val ROMANTIC_TRUST_THRESHOLD = 45L

    private val preExistingCloseKeywords = listOf(
        "брат", "сестр", "мать", "мама", "отец", "папа",
        "муж", "жена", "супруг", "давн", "лучший друг", "лучшая подруг"
    )

    private val prematureRomanceLabels = listOf(
        "влюб", "любов", "без ума", "обожа", "парень", "девушка", "невеста", "жених"
    )

    fun apply(
        incoming: NpcRelation,
        previous: NpcRelation?
    ): NpcRelation {
        val clampedTrust = if (previous != null) {
            val minAllowed = (previous.trustLevel - MAX_TRUST_LOSS_PER_TURN).coerceAtLeast(-100L)
            val maxAllowed = (previous.trustLevel + MAX_TRUST_GAIN_PER_TURN).coerceAtMost(100L)
            incoming.trustLevel.coerceIn(minAllowed, maxAllowed)
        } else {
            val combinedRole = "${incoming.role.lowercase()} ${incoming.statusLabel.lowercase()}"
            val isPreExistingClose = preExistingCloseKeywords.any { combinedRole.contains(it) }
            if (isPreExistingClose) {
                incoming.trustLevel.coerceIn(-100L, 100L)
            } else {
                incoming.trustLevel.coerceIn(-100L, MAX_STRANGER_INITIAL_TRUST)
            }
        }

        val resolvedRole = incoming.role.ifBlank { previous?.role.orEmpty() }
        val resolvedArchetype = incoming.archetype.ifBlank { previous?.archetype.orEmpty() }
        val resolvedPersonality = incoming.personality.ifBlank { previous?.personality.orEmpty() }
        val resolvedBoundaries = incoming.boundaries.ifBlank { previous?.boundaries.orEmpty() }

        val mergedActions = buildList {
            previous?.recentActions?.forEach { action ->
                if (action.isNotBlank()) add(action.trim())
            }
            incoming.recentActions.forEach { action ->
                val clean = action.trim()
                if (clean.isNotBlank() && none { it.equals(clean, ignoreCase = true) }) {
                    add(clean)
                }
            }
        }.takeLast(3)

        val rawStatus = incoming.statusLabel.ifBlank { previous?.statusLabel.orEmpty() }
        val lowerStatus = rawStatus.lowercase()
        val resolvedStatus = if (
            clampedTrust < ROMANTIC_TRUST_THRESHOLD &&
            prematureRomanceLabels.any { lowerStatus.contains(it) }
        ) {
            if (previous != null && previous.statusLabel.isNotBlank() &&
                prematureRomanceLabels.none { previous.statusLabel.lowercase().contains(it) }
            ) {
                previous.statusLabel
            } else if (clampedTrust >= 15L) {
                "Осторожный интерес"
            } else {
                "Держит дистанцию"
            }
        } else {
            rawStatus
        }

        return incoming.copy(
            role = resolvedRole,
            trustLevel = clampedTrust,
            statusLabel = resolvedStatus,
            archetype = resolvedArchetype,
            personality = resolvedPersonality,
            boundaries = resolvedBoundaries,
            recentActions = mergedActions
        )
    }
}

private data class ModelCacheKey(
    val model: AiModelChoice,
    val pace: NarrativePace,
    val reroll: Boolean
)

class GameMasterAiRepository(
    private val debugTokenProvider: () -> String? = { null },
    private val appCheckDiagnosticProvider: suspend () -> String? = { null }
) {
    @Volatile
    private var flash38CooldownUntilMs: Long = 0L

    private val modelCache = ConcurrentHashMap<ModelCacheKey, GenerativeModel>()

    private val emotionCategories = listOf(
        "joy",
        "embarrassment",
        "anger",
        "fear",
        "suspicion",
        "sadness",
        "surprise",
        "calm",
        "neutral"
    )

    private val weatherMoods = listOf(
        "rain",
        "storm",
        "fog",
        "night",
        "dawn",
        "day",
        "snow",
        "indoor_candle",
        "neon_night"
    )

    private val npcArchetypes = listOf(
        "warrior",
        "rogue",
        "mage",
        "noble",
        "cyber",
        "merchant",
        "commoner",
        "creature"
    )

    private val itemCategories = listOf(
        "weapon",
        "gear",
        "consumable",
        "clue",
        "key_item"
    )

    private val questStatuses = listOf(
        "active",
        "completed",
        "failed"
    )

    private val turnResponseSchema = Schema.obj(
        properties = mapOf(
            "narrative" to Schema.string(
                description = "Повествование на русском языке в выбранном темпе, строго без описания мыслей, слов или действий игрока (Anti-Godmodding)."
            ),
            "hudLocation" to Schema.string(description = "Текущая локация для HUD"),
            "hudTime" to Schema.string(description = "Текущее внутриигровое время и погода для HUD"),
            "sceneWeather" to Schema.enumeration(
                values = weatherMoods,
                description = "Визуальное настроение/погода сцены для фона"
            ),
            "healthPercent" to Schema.integer(description = "Здоровье или ресурс сил героя в процентах от 0 до 100"),
            "healthStatus" to Schema.string(description = "Краткое состояние героя (2-3 слова), например: В норме, Ранен, Утомлён"),
            "currencyAmount" to Schema.integer(description = "Числовое количество валюты/ресурса у героя (например: 15, 300, 0)"),
            "currencyLabel" to Schema.string(description = "Подпись валюты с числом или единицей (например: 15 крон, 300 эдди, 2400 ₽) или пустая строка"),
            "hudStatusAndInventory" to Schema.string(description = "Краткое состояние персонажа для HUD"),
            "hudRelationsAndCanon" to Schema.string(description = "Краткий статус отношений и влияния на канон для HUD"),
            "canonDeviationPercent" to Schema.integer(description = "Оценка отклонения от канона/привычного уклада от 0 до 100"),
            "activeNpc" to Schema.obj(
                properties = mapOf(
                    "hasActiveNpc" to Schema.boolean(description = "true, если игрок сейчас общается или напрямую взаимодействует с NPC"),
                    "name" to Schema.string(description = "Имя NPC или пустая строка, если рядом никого нет"),
                    "emotionLabel" to Schema.string(description = "Точная эмоция NPC на русском, например: Смущение, Теплота, Злость, Радость, Подозрение"),
                    "emotionCategory" to Schema.enumeration(
                        values = emotionCategories,
                        description = "Категория эмоции NPC"
                    ),
                    "intensity" to Schema.integer(description = "Интенсивность эмоции от 1 до 5"),
                    "note" to Schema.string(description = "Краткая ремарка о мимике, взгляде, жестах или тоне голоса NPC")
                )
            ),
            "activeNpcArchetype" to Schema.enumeration(
                values = npcArchetypes,
                description = "Визуальный архетип активного NPC для подбора портрета"
            ),
            "inventory" to Schema.array(
                items = Schema.string(),
                description = "Актуальный список названий предметов в инвентаре игрока"
            ),
            "inventoryItems" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(description = "Короткий id предмета латиницей"),
                        "name" to Schema.string(description = "Название предмета на русском"),
                        "category" to Schema.enumeration(values = itemCategories),
                        "quantity" to Schema.integer(description = "Количество (обычно 1)"),
                        "isEquipped" to Schema.boolean(description = "true, если предмет надет/в руках"),
                        "description" to Schema.string(description = "Короткое описание предмета (до 120 символов)")
                    ),
                    optionalProperties = listOf("id", "quantity", "isEquipped", "description")
                ),
                description = "Структурированный инвентарь героя с категориями, количеством и описанием"
            ),
            "npcs" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(),
                        "role" to Schema.string(),
                        "trustLevel" to Schema.integer(description = "От -100 до 100. Меняется плавно: +2..+10 за искренний удачный шаг, при навязчивости падает."),
                        "statusLabel" to Schema.string(),
                        "lastEmotion" to Schema.string(),
                        "lastEmotionCategory" to Schema.enumeration(values = emotionCategories),
                        "archetype" to Schema.enumeration(values = npcArchetypes),
                        "personality" to Schema.string(description = "Короткий психологический портрет (до 120 символов)"),
                        "boundaries" to Schema.string(description = "Личные границы и красные линии NPC (до 120 символов)"),
                        "recentAction" to Schema.string(description = "1 короткая запись (до 100 символов) о последнем поступке игрока, который запомнил этот NPC")
                    ),
                    optionalProperties = listOf("recentAction")
                ),
                description = "Актуальный список ключевых встреченных NPC, их характеров, личных границ и отношений с игроком"
            ),
            "hasButterflyEvent" to Schema.boolean(description = "true, если на этом ходе произошло значимое изменение канона или поворот отношений/судьбы"),
            "butterflyEventTitle" to Schema.string(description = "Короткий заголовок события эффекта бабочки (или пустая строка)"),
            "butterflyEventConsequence" to Schema.string(description = "Описание долгосрочного последствия для сюжета"),
            "butterflyEventImpact" to Schema.enumeration(values = listOf("minor", "major", "critical")),
            "storySummary" to Schema.string(description = "Обновлённое краткое резюме текущей арки кампании (2-5 предложений)"),
            "chapterArchiveSummary" to Schema.string(description = "Заполняй ТОЛЬКО когда запрошена архивация главы (каждый 10-й ход): 2-3 предложения с итогами прошедших 10 ходов"),
            "stateChangeSummary" to Schema.string(description = "Краткая сводка изменений за этот ход (доверие, предметы, раны, квесты — до 150 символов)"),
            "activeQuests" to Schema.array(
                items = Schema.string(),
                description = "1-3 актуальные цели/зацепки героя на текущий момент"
            ),
            "quests" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "title" to Schema.string(description = "Формулировка цели до 100 символов"),
                        "status" to Schema.enumeration(values = questStatuses)
                    ),
                    optionalProperties = listOf("id")
                ),
                description = "Структурированный трекер квестов со статусами active, completed, failed"
            ),
            "choices" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "approach" to Schema.string(description = "Подход под жанр сцены: Силовой, Дипломатический, Скрытный, Нестандартный, Романтический, Заботливый, Бытовой, Эмоциональный или Дерзкий"),
                        "text" to Schema.string(description = "Краткое описание действия игрока на русском языке"),
                        "isRisky" to Schema.boolean(description = "true, если выбор сопряжён с риском, авантюрой, боем, скрытностью или смелым шагом"),
                        "riskOutcomes" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "id" to Schema.string(),
                                    "label" to Schema.string(description = "Короткое название исхода (2-4 слова)"),
                                    "description" to Schema.string(description = "Что конкретно произойдёт при этом исходе (до 130 символов)"),
                                    "tier" to Schema.enumeration(values = listOf("triumph", "mixed", "setback", "twist")),
                                    "weight" to Schema.integer(description = "Вероятность/вес исхода от 10 до 50 (в сумме ~100)")
                                )
                            ),
                            description = "Если isRisky=true, дай 3 или 4 яркие развилки исхода (триумф, с ценой, срыв, поворот)"
                        )
                    ),
                    optionalProperties = listOf("isRisky", "riskOutcomes")
                ),
                description = "3 или 4 принципиально разных варианта действий игрока"
            )
        ),
        optionalProperties = listOf(
            "currencyAmount",
            "currencyLabel",
            "activeNpcArchetype",
            "sceneWeather",
            "activeQuests",
            "quests",
            "inventoryItems",
            "chapterArchiveSummary",
            "stateChangeSummary"
        )
    )

    private val premiseSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Атмосферное название хроники (2-5 слов)"),
            "universe" to Schema.string(description = "Описание мира/вселенной (1 предложение)"),
            "playerName" to Schema.string(description = "Имя героя"),
            "playerGender" to Schema.enumeration(values = listOf("male", "female", "neutral")),
            "characterSummary" to Schema.string(description = "Краткий образ героя, его профессия, сильная сторона и слабость"),
            "startingPoint" to Schema.string(description = "Яркая стартовая точка, место и завязка конфликта"),
            "toneAndDifficulty" to Schema.string(description = "Атмосфера, жанровый тон и цена решений"),
            "might" to Schema.integer(description = "Натиск 1-10"),
            "stealth" to Schema.integer(description = "Скрытность 1-10"),
            "insight" to Schema.integer(description = "Проницательность 1-10"),
            "lore" to Schema.integer(description = "Эрудиция 1-10")
        )
    )

    private val systemPrompt = content {
        text(
            """
            Ты — опытный гейм-мастер и интерактивный рассказчик текстовых ролевых игр (RPG) в любых жанрах: от мрачного фэнтези и сай-фая до романтических историй, служебных драм и уютной повседневной рутины (Slice of Life). Твоя задача — вести глубокий, нелинейный сюжет в выбранной игроком вселенной на русском языке, строго соблюдая её лор, законы, атмосферу и психологию персонажей.

            СТРОГИЕ ПРАВИЛА ИГРЫ:
            1. Никакого контроля над игроком (Anti-Godmodding): описывай только мир, окружение, действия и реплики NPC, а также последствия поступков игрока. НИКОГДА не описывай мысли, чувства, слова или будущие действия за игрока. Останавливай повествование ровно в момент, когда требуется решение игрока.
            2. РОД И ИМЯ ГЕРОЯ: строго соблюдай указанные в контексте имя и грамматический род героя (мужской / женский / нейтральный). Никогда не меняй и не путай род глаголов и прилагательных, обращённых к герою или описывающих его прошлое действие.
            3. ПСИХОЛОГИЯ, СОПРОТИВЛЕНИЕ И ЛИЧНЫЕ ГРАНИЦЫ NPC (КРИТИЧЕСКИ ВАЖНО):
               - Запрещено мгновенное угождение: в романтике и повседневности NPC НЕ влюбляются, НЕ краснеют от первого же комплимента и НЕ соглашаются на просьбы с 1–3 сообщения. У каждого персонажа есть свои дела, смена, усталость, гордость, скепсис, комплексы и личные заботы.
               - Жёсткие личные границы и право на отказ: если игрок форсирует сближение, давит, льстит слишком очевидно, нарушает личное пространство, просит об одолжении без доверия или хамит — NPC обязан отказать, отшутиться с дистанцией, перевести тему, поставить границу или раздражённо отстраниться, снизив trustLevel.
               - Медленный слоубёрн: незнакомец стартует с trustLevel около -10..+15. Один удачный разговор даёт +2..+8, не больше.
               - Обязательно задавай каждому важному NPC поля "personality", "boundaries" и "recentAction" (что именно он запомнил из поступков игрока).
            4. Жанр, навыки и реализм: строго придерживайся выбранного тона и учитывай 4 навыка героя (Натиск, Скрытность, Проницательность, Эрудиция) и его экипировку.
            5. Эффект бабочки, структурированный инвентарь и квесты:
               - Заполняй "inventoryItems" с категориями (weapon, gear, consumable, clue, key_item), количеством и флагом isEquipped, дублируя названия в "inventory".
               - Обновляй "quests" со статусами active, completed или failed.
               - Обязательно указывай числовые "healthPercent" (0..100) и "currencyAmount" + "currencyLabel".
            6. Динамика и темп: следуй указанному в запросе темпу повествования (Динамичный / Книжный / Глубокий). В JSON-ответе ВСЕГДА первым полем выдавай "narrative", чтобы игрок сразу видел живой текст.
            7. Эмоции NPC: при любом контакте с персонажем обязательно заполняй activeNpc (hasActiveNpc = true), указывая его точную эмоцию, интенсивность (1–5) и краткое наблюдение за мимикой/взглядом/интонацией.
            8. Предметы и факты: если игрок использует или показывает предмет из инвентаря, обязательно учитывай этот предмет в сцене. Обязательно учитывай блок «ЗАКРЕПЛЁННЫЕ ФАКТЫ ИГРОКА», если он передан.
            9. Варианты действий и рискованные развилки: всегда предлагай 3–4 принципиально разных варианта. Для 1–2 рискованных вариантов ставь "isRisky": true и заполняй 3–4 исхода в "riskOutcomes". Если в действии игрока есть блок «[Бросок судьбы • ...]» — строй сцену строго по выпавшему исходу!
            10. Язык: весь ответ исключительно на русском языке (кроме перечислений enum, требуемых схемой).
            """.trimIndent()
        )
    }

    private fun getOrCreateModel(
        choice: AiModelChoice,
        pace: NarrativePace,
        isReroll: Boolean
    ): GenerativeModel {
        val key = ModelCacheKey(choice, pace, isReroll)
        return modelCache.getOrPut(key) {
            val effectiveTemp = if (isReroll) {
                (pace.temperature + 0.12f).coerceAtMost(1.0f)
            } else {
                pace.temperature
            }
            Firebase.ai.generativeModel(
                modelName = choice.modelId,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = turnResponseSchema
                    temperature = effectiveTemp
                    maxOutputTokens = pace.maxOutputTokens
                    if (choice == AiModelChoice.FLASH_38) {
                        thinkingConfig = thinkingConfig {
                            thinkingLevel = ThinkingLevel.LOW
                        }
                    }
                },
                systemInstruction = systemPrompt
            )
        }
    }

    private val lightTextModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelChoice.FLASH_LITE.modelId,
            generationConfig = generationConfig {
                temperature = 0.8f
                maxOutputTokens = 512
            }
        )
    }

    private val premiseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelChoice.FLASH_LITE.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = premiseSchema
                temperature = 0.92f
                maxOutputTokens = 1024
            }
        )
    }

    suspend fun generateOpeningScene(
        setup: SessionSetupInput,
        preferredModel: AiModelChoice = AiModelChoice.FLASH_LITE,
        narrativePace: NarrativePace = NarrativePace.BOOKISH
    ): GmTurnOutcome {
        val heroIdentityLine = buildString {
            if (setup.playerName.isNotBlank()) {
                append("Имя героя: ${setup.playerName.trim()}. ")
            }
            append("Грамматический род героя: ${setup.playerGender.grammarHintRu}")
        }
        val skills = setup.skills.coerced()
        val prompt = """
            НАЧАЛО НОВОЙ ХРОНИКИ.
            ${narrativePace.promptDirectiveRu}
            $heroIdentityLine
            Навыки героя (1-10): Натиск ${skills.might}, Скрытность ${skills.stealth}, Проницательность ${skills.insight}, Эрудиция ${skills.lore}.

            Стартовые параметры:
            1. Вселенная: ${setup.universe}
            2. Персонаж: ${setup.characterSummary}
            3. Точка старта: ${setup.startingPoint}
            4. Тон и сложность: ${setup.toneAndDifficulty}

            Сгенерируй захватывающую стартовую сцену, установи стартовый HUD, числовое здоровье и валюту, структурированный инвентарь (inventoryItems + inventory), стартовые цели (quests + activeQuests), начальное состояние NPC (если рядом есть персонаж — обязательно передай его эмоцию в activeNpc) и дай первые 3–4 принципиально разных варианта действий. Останови сцену в момент, требующий первого решения игрока.
        """.trimIndent()

        return invokeModel(
            prompt = prompt,
            defaultTurnNumber = 1L,
            previousNpcs = emptyList(),
            defaultSkills = skills,
            preferredModel = preferredModel,
            narrativePace = narrativePace,
            isReroll = false,
            useStreaming = false,
            onStreamingNarrative = {}
        )
    }

    suspend fun generateNextTurn(
        campaign: Campaign,
        recentTurns: List<CampaignTurn>,
        playerAction: String,
        preferredModel: AiModelChoice = AiModelChoice.FLASH_LITE,
        narrativePace: NarrativePace = NarrativePace.BOOKISH,
        pinnedFacts: List<String> = emptyList(),
        isReroll: Boolean = false,
        onStreamingNarrative: (String) -> Unit = {}
    ): GmTurnOutcome {
        val nextTurnNumber = campaign.turnCount + 1L
        val prompt = buildNextTurnPrompt(
            campaign = campaign,
            recentTurns = recentTurns,
            playerAction = playerAction,
            narrativePace = narrativePace,
            pinnedFacts = pinnedFacts,
            isReroll = isReroll
        )

        return invokeModel(
            prompt = prompt,
            defaultTurnNumber = nextTurnNumber,
            previousNpcs = campaign.npcs,
            defaultSkills = campaign.skills,
            preferredModel = preferredModel,
            narrativePace = narrativePace,
            isReroll = isReroll,
            useStreaming = true,
            onStreamingNarrative = onStreamingNarrative
        )
    }

    internal fun buildNextTurnPrompt(
        campaign: Campaign,
        recentTurns: List<CampaignTurn>,
        playerAction: String,
        narrativePace: NarrativePace = NarrativePace.BOOKISH,
        pinnedFacts: List<String> = emptyList(),
        isReroll: Boolean = false
    ): String {
        val nextTurnNumber = campaign.turnCount + 1L
        val shouldArchiveChapter = nextTurnNumber >= 10L && nextTurnNumber % 10L == 0L

        val historyBlock = recentTurns.takeLast(SLIDING_CONTEXT_TURNS).joinToString("\n\n") { turn ->
            buildString {
                appendLine("--- Ход ${turn.turnNumber} ---")
                if (turn.playerAction.isNotBlank()) {
                    appendLine("Действие игрока: ${turn.playerAction}")
                }
                appendLine("Повествование: ${turn.narrative}")
                appendLine("HUD: ${turn.hudLine}")
                if (turn.activeNpc.hasActiveNpc && turn.activeNpc.name.isNotBlank()) {
                    appendLine("Активный NPC: ${turn.activeNpc.name} (${turn.activeNpc.emotionLabel}, ${turn.activeNpc.intensity}/5)")
                }
                val changeNote = turn.stateSnapshot?.stateChangeSummary?.trim().orEmpty()
                if (changeNote.isNotBlank()) {
                    appendLine("Изменения хода: $changeNote")
                }
            }.trimEnd()
        }

        val chapterArchiveBlock = if (campaign.chapterSummaries.isEmpty()) {
            ""
        } else {
            buildString {
                appendLine()
                appendLine("ЛЕТОПИСЬ ПРОШЛЫХ ГЛАВ (ДОЛГОСРОЧНАЯ ПАМЯТЬ):")
                campaign.chapterSummaries.takeLast(12).forEach { summary ->
                    appendLine("- $summary")
                }
            }
        }

        val npcsBlock = if (campaign.npcs.isEmpty()) {
            "Пока не зафиксированы."
        } else {
            campaign.npcs.joinToString("\n") { npc ->
                buildString {
                    append("- ${npc.name} (${npc.role}): статус «${npc.statusLabel}», доверие ${npc.trustLevel}, эмоция: ${npc.lastEmotion}")
                    if (npc.personality.isNotBlank()) {
                        append(" | Характер: ${npc.personality}")
                    }
                    if (npc.boundaries.isNotBlank()) {
                        append(" | Границы: ${npc.boundaries}")
                    }
                    if (npc.recentActions.isNotEmpty()) {
                        append(" | Помнит: ${npc.recentActions.joinToString("; ")}")
                    }
                }
            }
        }

        val butterflyBlock = if (campaign.butterflyEvents.isEmpty()) {
            "Пока без значительных отклонений от канона."
        } else {
            campaign.butterflyEvents.takeLast(10).joinToString("\n") { ev ->
                "- Ход ${ev.turnNumber} [${ev.impactLevel}]: ${ev.title} — ${ev.consequence}"
            }
        }

        val inventoryBlock = campaign.resolvedInventoryItems().let { items ->
            if (items.isEmpty()) {
                "Пусто"
            } else {
                items.joinToString(", ") { item ->
                    val equipMark = if (item.isEquipped) " [экипировано]" else ""
                    val qtyMark = if (item.quantity > 1L) " ×${item.quantity}" else ""
                    "${item.name}$qtyMark (${item.resolvedCategory().labelRu})$equipMark"
                }
            }
        }

        val questsBlock = campaign.resolvedQuests().let { quests ->
            if (quests.isEmpty()) {
                "Нет зафиксированных квестов."
            } else {
                quests.joinToString("\n") { q ->
                    "- [${q.resolvedStatus().labelRu}] ${q.title}"
                }
            }
        }

        val factsSection = if (pinnedFacts.isEmpty()) {
            ""
        } else {
            buildString {
                appendLine()
                appendLine("ЗАКРЕПЛЁННЫЕ ФАКТЫ ИГРОКА (ОБЯЗАТЕЛЬНО УЧИТЫВАЙ В СЦЕНЕ):")
                pinnedFacts.take(15).forEach { fact ->
                    appendLine("- $fact")
                }
            }
        }

        val gender = campaign.resolvedGender()
        val heroIdentityLine = buildString {
            if (campaign.playerName.isNotBlank()) {
                append("Имя героя: ${campaign.playerName}. ")
            }
            append("Грамматический род героя: ${gender.grammarHintRu}")
        }

        val archiveDirective = if (shouldArchiveChapter) {
            "\nАРХИВАЦИЯ ГЛАВЫ (ХОД $nextTurnNumber): ОБЯЗАТЕЛЬНО заполни поле \"chapterArchiveSummary\" (2–3 ёмких предложения, сжимающих ключевые события ходов ${nextTurnNumber - 9L}–$nextTurnNumber), а в \"storySummary\" оставь только актуальную текущую сюжетную арку!"
        } else {
            ""
        }

        val rerollDirective = if (isReroll) {
            "\nРЕЖИМ ПЕРЕИГРОВКИ СЦЕНЫ: предложи свежую, заметно иную по подаче, деталям и репликам альтернативную версию развития этой сцены на то же действие игрока."
        } else {
            ""
        }

        val skills = campaign.skills.coerced()
        val healthText = if (campaign.healthPercent in 0L..100L) "${campaign.healthPercent}%" else campaign.hudStatusAndInventory
        val currencyText = campaign.currencyLabel.ifBlank { "${campaign.currencyAmount}" }

        return """
            ${narrativePace.promptDirectiveRu}
            $heroIdentityLine$archiveDirective$rerollDirective

            КОНТЕКСТ КАМПАНИИ:
            - Вселенная: ${campaign.universe}
            - Персонаж игрока: ${campaign.characterSummary}
            - Навыки (1-10): Натиск ${skills.might}, Скрытность ${skills.stealth}, Проницательность ${skills.insight}, Эрудиция ${skills.lore}
            - Здоровье сейчас: $healthText | Валюта: $currencyText
            - Точка старта: ${campaign.startingPoint}
            - Тон и сложность: ${campaign.toneAndDifficulty}
            - Текущий ход: $nextTurnNumber
            - Отклонение от канона сейчас: ${campaign.canonDeviationPercent}%
            - Текущий инвентарь: $inventoryBlock
            - Текущая сюжетная арка: ${campaign.storySummary.ifBlank { "Начало истории." }}
            $chapterArchiveBlock
            $factsSection

            ЖУРНАЛ КВЕСТОВ:
            $questsBlock

            ИЗВЕСТНЫЕ NPC, ИХ ХАРАКТЕРЫ, ГРАНИЦЫ И ПАМЯТЬ:
            $npcsBlock

            ЭФФЕКТ БАБОЧКИ (КЛЮЧЕВЫЕ ПОВОРОТЫ):
            $butterflyBlock

            ПОСЛЕДНИЕ ХОДЫ (ДО $SLIDING_CONTEXT_TURNS):
            $historyBlock

            НОВОЕ ДЕЙСТВИЕ ИГРОКА:
            "$playerAction"

            ПРОВЕРКА ГРАНИЦ: не позволяй NPC мгновенно соглашаться или влюбляться без веских оснований. Если игрок торопит события, давит или нарушает границы персонажа, NPC должен отказать, поставить условие или отстраниться.
            Оцени логичность, навыки, экипировку и контекст действия игрока. Опиши реакцию мира и NPC (строго без контроля над игроком!). Обнови HUD, healthPercent, currencyAmount/currencyLabel, структурированный инвентарь (inventoryItems + inventory), квесты (quests + activeQuests), эмоции активного NPC, отношения (trustLevel меняй плавно), stateChangeSummary и предложи 3–4 новых разных выбора.
        """.trimIndent()
    }

    /**
     * Quick non-turn AI lore inspection of an inventory item in the context of the current chronicle.
     */
    suspend fun inspectItem(
        campaign: Campaign,
        item: InventoryItem
    ): String {
        val prompt = """
            Ты — гейм-мастер текстовой RPG в мире «${campaign.universe}».
            Игрок осматривает предмет из своего инвентаря без траты хода.
            Локация сейчас: ${campaign.hudLocation}.
            Предмет: ${item.name} (категория: ${item.resolvedCategory().labelRu}, количество: ${item.quantity}, ${if (item.isEquipped) "экипирован" else "в сумке"}).
            ${if (item.description.isNotBlank()) "Известное описание: ${item.description}" else ""}
            Краткий сюжет: ${campaign.storySummary.take(400)}

            Дай атмосферное, полезное описание этого предмета в 2–4 предложениях на русском языке: внешний вид, мелкие скрытые детали, состояние и чем он может пригодиться в этой истории. Без контроля над действиями игрока.
        """.trimIndent()

        return try {
            withTimeout(20_000L) {
                lightTextModel.generateContent(prompt).text.orEmpty().trim()
                    .ifBlank { "Вы внимательно осматриваете «${item.name}», но не находите ничего сверх обычного." }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(extractDetailedErrorMessage(e), e)
        }
    }

    /**
     * Generates an original RPG premise for the SetupWizard based on the chosen genre.
     */
    suspend fun generateScenarioPremise(
        genre: ScenarioGenre
    ): SessionSetupInput {
        val genreFocus = if (genre == ScenarioGenre.ALL) {
            "любой яркий небанальный жанр (фэнтези, романтический нуар, уютная повседневность, киберпанк или мистика)"
        } else {
            "жанр «${genre.labelRu}»"
        }

        val prompt = """
            Придумай оригинальную, цепляющую завязку для интерактивной текстовой RPG на русском языке.
            Направление: $genreFocus.
            Сделай героя живым и приземлённым (со слабостью и личным мотивом), стартовую точку — конкретной сценой с интригой, а тон — реалистичным без сюжетной брони.
            Распредели 4 навыка героя (might, stealth, insight, lore) в диапазоне от 2 до 9 так, чтобы сумма была около 22–25.
        """.trimIndent()

        return try {
            val rawJson = withTimeout(25_000L) {
                premiseModel.generateContent(prompt).text.orEmpty().trim()
            }
            parseScenarioPremiseJson(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(extractDetailedErrorMessage(e), e)
        }
    }

    internal fun parseScenarioPremiseJson(rawJson: String): SessionSetupInput {
        val root = JSONObject(rawJson)
        val universe = root.optString("universe").trim()
        val characterSummary = root.optString("characterSummary").trim()
        val startingPoint = root.optString("startingPoint").trim()
        val toneAndDifficulty = root.optString("toneAndDifficulty").trim()
        require(universe.isNotBlank() && startingPoint.isNotBlank()) {
            "ИИ вернул неполную завязку мира."
        }
        return SessionSetupInput(
            title = root.optString("title").trim().ifBlank { universe.take(48) },
            universe = universe,
            characterSummary = characterSummary.ifBlank { "Свой персонаж (OC) с непростым прошлым." },
            startingPoint = startingPoint,
            toneAndDifficulty = toneAndDifficulty.ifBlank { "Реалистичные последствия и живые характеры NPC." },
            playerName = root.optString("playerName").trim(),
            playerGender = PlayerGender.fromKey(root.optString("playerGender")),
            skills = HeroSkills(
                might = root.optLong("might", 5L),
                stealth = root.optLong("stealth", 5L),
                insight = root.optLong("insight", 5L),
                lore = root.optLong("lore", 5L)
            ).coerced()
        )
    }

    private suspend fun invokeModel(
        prompt: String,
        defaultTurnNumber: Long,
        previousNpcs: List<NpcRelation>,
        defaultSkills: HeroSkills,
        preferredModel: AiModelChoice,
        narrativePace: NarrativePace,
        isReroll: Boolean,
        useStreaming: Boolean = true,
        onStreamingNarrative: (String) -> Unit
    ): GmTurnOutcome {
        try {
            val now = System.currentTimeMillis()
            val shouldUseFlash38 = preferredModel == AiModelChoice.FLASH_38 && now >= flash38CooldownUntilMs

            val flashLite = getOrCreateModel(AiModelChoice.FLASH_LITE, narrativePace, isReroll)
            val jsonString = if (!shouldUseFlash38) {
                fetchModelJson(
                    targetModel = flashLite,
                    prompt = prompt,
                    timeoutMs = FLASH_LITE_TIMEOUT_MS,
                    useStreaming = useStreaming,
                    defaultTurnNumber = defaultTurnNumber,
                    previousNpcs = previousNpcs,
                    defaultSkills = defaultSkills,
                    onStreamingNarrative = onStreamingNarrative
                )
            } else {
                val flash38 = getOrCreateModel(AiModelChoice.FLASH_38, narrativePace, isReroll)
                try {
                    fetchModelJson(
                        targetModel = flash38,
                        prompt = prompt,
                        timeoutMs = FLASH_38_TIMEOUT_MS,
                        useStreaming = useStreaming,
                        defaultTurnNumber = defaultTurnNumber,
                        previousNpcs = previousNpcs,
                        defaultSkills = defaultSkills,
                        onStreamingNarrative = onStreamingNarrative
                    )
                } catch (timeout: TimeoutCancellationException) {
                    Log.w(TAG, "gemini-3.8-flash timed out; switching to gemini-3.5-flash-lite", timeout)
                    flash38CooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                    onStreamingNarrative("")
                    fetchModelJson(
                        targetModel = flashLite,
                        prompt = prompt,
                        timeoutMs = FLASH_LITE_TIMEOUT_MS,
                        useStreaming = useStreaming,
                        defaultTurnNumber = defaultTurnNumber,
                        previousNpcs = previousNpcs,
                        defaultSkills = defaultSkills,
                        onStreamingNarrative = onStreamingNarrative
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isQuotaOrRateLimitError(e)) {
                        Log.w(TAG, "gemini-3.8-flash hit quota/rate-limit; switching to gemini-3.5-flash-lite", e)
                        flash38CooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                        onStreamingNarrative("")
                        fetchModelJson(
                            targetModel = flashLite,
                            prompt = prompt,
                            timeoutMs = FLASH_LITE_TIMEOUT_MS,
                            useStreaming = useStreaming,
                            defaultTurnNumber = defaultTurnNumber,
                            previousNpcs = previousNpcs,
                            defaultSkills = defaultSkills,
                            onStreamingNarrative = onStreamingNarrative
                        )
                    } else {
                        throw e
                    }
                }
            }

            if (jsonString.isBlank()) {
                throw IllegalStateException("ИИ-мастер вернул пустой ответ. Попробуйте повторить ход.")
            }
            return parseTurnOutcome(jsonString, defaultTurnNumber, previousNpcs, defaultSkills)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            throw IllegalStateException("ИИ-мастер слишком долго обдумывал сцену. Проверьте интернет и попробуйте ещё раз.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            val detailedMessage = extractDetailedErrorMessage(e)
            val isAppCheck = isAppCheckError(e)
            val debugToken = if (isAppCheck) debugTokenProvider() else null
            val releaseDiag = if (isAppCheck && debugToken.isNullOrBlank()) {
                runCatching { appCheckDiagnosticProvider() }.getOrNull()
            } else {
                null
            }
            val finalMessage = when {
                !debugToken.isNullOrBlank() ->
                    "$detailedMessage\n\nDebug-токен этого телефона:\n$debugToken\n\nДобавьте его в Firebase Console → App Check → Apps → Manage debug tokens."
                !releaseDiag.isNullOrBlank() ->
                    "$detailedMessage\n$releaseDiag"
                isQuotaOrRateLimitError(e) ->
                    formatQuotaErrorMessage(detailedMessage)
                else -> detailedMessage
            }
            throw IllegalStateException(finalMessage, e)
        }
    }

    internal fun extractDetailedErrorMessage(error: Throwable): String {
        val messages = generateSequence(error) { it.cause }
            .mapNotNull { it.localizedMessage ?: it.message }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        val meaningful = messages.firstOrNull { !it.equals("Something unexpected happened.", ignoreCase = true) }
        return meaningful ?: messages.firstOrNull() ?: "Не удалось получить ответ от ИИ-мастера."
    }

    private fun isAppCheckError(error: Throwable): Boolean {
        val all = generateSequence(error) { it.cause }
            .mapNotNull { it.message ?: it.localizedMessage }
            .joinToString(" ")
        return all.contains("App Check", ignoreCase = true) ||
            all.contains("token is invalid", ignoreCase = true) ||
            all.contains("403", ignoreCase = true)
    }

    private fun isQuotaOrRateLimitError(error: Throwable): Boolean {
        val msg = generateSequence(error) { it.cause }
            .mapNotNull { it.message ?: it.localizedMessage }
            .joinToString(" ")
        return msg.contains("quota", ignoreCase = true) ||
            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("rate-limit", ignoreCase = true) ||
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            msg.contains("429")
    }

    private fun formatQuotaErrorMessage(rawMessage: String): String {
        val retryMatch = Regex("""retry in (\d+(?:\.\d+)?)s""", RegexOption.IGNORE_CASE).find(rawMessage)
        val seconds = retryMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { kotlin.math.ceil(it).toInt() }
        return if (seconds != null && seconds > 0) {
            "Достигнут минутный лимит запросов бесплатного тарифа Gemini. Подождите $seconds сек. и нажмите «Повторить»."
        } else {
            "Достигнут минутный лимит запросов бесплатного тарифа Gemini. Подождите ~20 сек. и нажмите «Повторить»."
        }
    }

    private suspend fun fetchModelJson(
        targetModel: GenerativeModel,
        prompt: String,
        timeoutMs: Long,
        useStreaming: Boolean,
        defaultTurnNumber: Long,
        previousNpcs: List<NpcRelation>,
        defaultSkills: HeroSkills,
        onStreamingNarrative: (String) -> Unit
    ): String {
        if (!useStreaming) {
            return generateUnaryJson(targetModel, prompt, timeoutMs)
        }

        val builder = StringBuilder()
        val extractor = StreamingNarrativeExtractor()
        return try {
            withTimeout(timeoutMs) {
                targetModel.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text ?: return@collect
                    builder.append(chunkText)
                    extractor.append(chunkText)?.let { partialNarrative ->
                        onStreamingNarrative(partialNarrative)
                    }
                }
                builder.toString().trim()
            }
        } catch (e: TimeoutCancellationException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (streamError: Exception) {
            if (isQuotaOrRateLimitError(streamError) || isAppCheckError(streamError)) {
                throw streamError
            }
            val accumulated = builder.toString().trim()
            if (accumulated.isNotBlank() &&
                runCatching { parseTurnOutcome(accumulated, defaultTurnNumber, previousNpcs, defaultSkills) }.isSuccess
            ) {
                Log.w(TAG, "Stream threw at EOF after valid JSON arrived; using accumulated response", streamError)
                return accumulated
            }
            Log.w(TAG, "Streaming failed (${extractDetailedErrorMessage(streamError)}); falling back to unary generateContent", streamError)
            generateUnaryJson(targetModel, prompt, timeoutMs)
        }
    }

    private suspend fun generateUnaryJson(
        targetModel: GenerativeModel,
        prompt: String,
        timeoutMs: Long
    ): String = withTimeout(timeoutMs) {
        targetModel.generateContent(prompt).text.orEmpty().trim()
    }

    internal fun parseTurnOutcome(
        jsonString: String,
        turnNumber: Long,
        previousNpcs: List<NpcRelation> = emptyList(),
        defaultSkills: HeroSkills = HeroSkills()
    ): GmTurnOutcome {
        val root = JSONObject(jsonString)

        val narrative = root.optString("narrative").trim()
        require(narrative.isNotBlank()) { "В ответе мастера отсутствует текст сцены." }

        val activeNpcJson = root.optJSONObject("activeNpc")
        val activeNpc = if (activeNpcJson != null) {
            val categoryKey = activeNpcJson.optString("emotionCategory", "neutral")
            val category = EmotionCategory.fromKey(categoryKey)
            NpcEmotionSnapshot(
                hasActiveNpc = activeNpcJson.optBoolean("hasActiveNpc", false),
                name = activeNpcJson.optString("name").trim(),
                emotionLabel = activeNpcJson.optString("emotionLabel").trim()
                    .ifBlank { category.russianDefault },
                emotionCategory = category.key,
                intensity = activeNpcJson.optLong("intensity", 2L).coerceIn(0L, 5L),
                note = activeNpcJson.optString("note").trim()
            )
        } else {
            NpcEmotionSnapshot()
        }

        val inventoryArray = root.optJSONArray("inventory")
        val flatInventory = buildList {
            if (inventoryArray != null) {
                for (i in 0 until inventoryArray.length()) {
                    val item = inventoryArray.optString(i).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }

        val structuredArray = root.optJSONArray("inventoryItems")
        val structuredInventory = buildList {
            if (structuredArray != null) {
                for (i in 0 until structuredArray.length()) {
                    val obj = structuredArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    if (name.isBlank()) continue
                    val cat = ItemCategory.fromKey(obj.optString("category"))
                    add(
                        InventoryItem(
                            id = obj.optString("id").trim(),
                            name = name,
                            category = cat.key,
                            quantity = obj.optLong("quantity", 1L).coerceIn(1L, 999L),
                            isEquipped = obj.optBoolean("isEquipped", false),
                            description = obj.optString("description").trim()
                        )
                    )
                }
            }
        }.ifEmpty {
            flatInventory.mapIndexed { idx, name -> InventoryItem.fromLegacyName(name, idx) }
        }
        val effectiveFlatInventory = flatInventory.ifEmpty { structuredInventory.map { it.name } }

        val activeNpcArchetype = root.optString("activeNpcArchetype").trim().lowercase()
        val npcsArray = root.optJSONArray("npcs")
        val npcs = buildList {
            if (npcsArray != null) {
                for (i in 0 until npcsArray.length()) {
                    val obj = npcsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    if (name.isBlank()) continue
                    val prev = previousNpcs.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    val cat = EmotionCategory.fromKey(obj.optString("lastEmotionCategory", "neutral"))
                    val parsedArchetype = obj.optString("archetype").trim().lowercase()
                        .ifBlank {
                            if (name.equals(activeNpc.name, ignoreCase = true)) activeNpcArchetype else ""
                        }
                    val recentAction = obj.optString("recentAction").trim()
                    val recentActionsArray = obj.optJSONArray("recentActions")
                    val parsedRecentActions = buildList {
                        if (recentActionsArray != null) {
                            for (j in 0 until recentActionsArray.length()) {
                                val entry = recentActionsArray.optString(j).trim()
                                if (entry.isNotBlank()) add(entry)
                            }
                        }
                        if (recentAction.isNotBlank() && none { it.equals(recentAction, ignoreCase = true) }) {
                            add(recentAction)
                        }
                    }.takeLast(3)
                    val rawRelation = NpcRelation(
                        name = name,
                        role = obj.optString("role").trim(),
                        trustLevel = obj.optLong("trustLevel", 0L).coerceIn(-100L, 100L),
                        statusLabel = obj.optString("statusLabel").trim(),
                        lastEmotion = obj.optString("lastEmotion").trim().ifBlank { cat.russianDefault },
                        lastEmotionCategory = cat.key,
                        archetype = parsedArchetype,
                        personality = obj.optString("personality").trim(),
                        boundaries = obj.optString("boundaries").trim(),
                        recentActions = parsedRecentActions
                    )
                    add(NpcTrustGuardrail.apply(rawRelation, prev))
                }
            }
            if (activeNpc.hasActiveNpc &&
                activeNpc.name.isNotBlank() &&
                none { it.name.equals(activeNpc.name, ignoreCase = true) }
            ) {
                val prev = previousNpcs.firstOrNull { it.name.equals(activeNpc.name, ignoreCase = true) }
                val fallbackRelation = NpcRelation(
                    name = activeNpc.name,
                    role = prev?.role?.ifBlank { "Собеседник в сцене" } ?: "Собеседник в сцене",
                    trustLevel = prev?.trustLevel ?: 0L,
                    statusLabel = prev?.statusLabel?.ifBlank { "Держит дистанцию" } ?: "Держит дистанцию",
                    lastEmotion = activeNpc.emotionLabel,
                    lastEmotionCategory = activeNpc.emotionCategory,
                    archetype = activeNpcArchetype,
                    personality = prev?.personality.orEmpty(),
                    boundaries = prev?.boundaries.orEmpty(),
                    recentActions = prev?.recentActions.orEmpty()
                )
                add(NpcTrustGuardrail.apply(fallbackRelation, prev))
            }
            previousNpcs.forEach { prev ->
                if (none { it.name.equals(prev.name, ignoreCase = true) } && size < 40) {
                    add(prev)
                }
            }
        }

        val hasButterfly = root.optBoolean("hasButterflyEvent", false)
        val butterflyTitle = root.optString("butterflyEventTitle").trim()
        val butterflyConsequence = root.optString("butterflyEventConsequence").trim()
        val butterflyEvent = if (hasButterfly && butterflyTitle.isNotBlank()) {
            ButterflyEvent(
                turnNumber = turnNumber,
                title = butterflyTitle,
                consequence = butterflyConsequence.ifBlank { butterflyTitle },
                impactLevel = root.optString("butterflyEventImpact", "minor").lowercase()
            )
        } else {
            null
        }

        val choicesArray = root.optJSONArray("choices")
        val choices = buildList {
            if (choicesArray != null) {
                for (i in 0 until choicesArray.length()) {
                    val obj = choicesArray.optJSONObject(i) ?: continue
                    val text = obj.optString("text").trim()
                    if (text.isBlank()) continue
                    val outcomesArray = obj.optJSONArray("riskOutcomes")
                    val parsedOutcomes = buildList {
                        if (outcomesArray != null) {
                            for (j in 0 until outcomesArray.length()) {
                                val oObj = outcomesArray.optJSONObject(j) ?: continue
                                val oLabel = oObj.optString("label").trim()
                                val oDesc = oObj.optString("description").trim()
                                if (oLabel.isBlank() || oDesc.isBlank()) continue
                                add(
                                    RiskOutcome(
                                        id = oObj.optString("id").ifBlank { "outcome_${j + 1}" },
                                        label = oLabel,
                                        description = oDesc,
                                        tier = RiskOutcomeTier.fromKey(oObj.optString("tier")).key,
                                        weight = oObj.optLong("weight", 25L).coerceIn(5L, 80L)
                                    )
                                )
                            }
                        }
                    }.take(4)
                    val isRisky = obj.optBoolean("isRisky", false) || parsedOutcomes.size >= 2
                    add(
                        TurnChoice(
                            id = obj.optString("id").ifBlank { (i + 1).toString() },
                            approach = obj.optString("approach").trim().ifBlank { "Действие" },
                            text = text,
                            isRisky = isRisky,
                            riskOutcomes = parsedOutcomes
                        )
                    )
                }
            }
        }

        val questsArray = root.optJSONArray("activeQuests")
        val legacyActiveQuests = buildList {
            if (questsArray != null) {
                for (i in 0 until questsArray.length()) {
                    val q = questsArray.optString(i).trim()
                    if (q.isNotBlank()) add(q.take(160))
                }
            }
        }.take(6)

        val structuredQuestsArray = root.optJSONArray("quests")
        val structuredQuests = buildList {
            if (structuredQuestsArray != null) {
                for (i in 0 until structuredQuestsArray.length()) {
                    val obj = structuredQuestsArray.optJSONObject(i) ?: continue
                    val title = obj.optString("title").trim()
                    if (title.isBlank()) continue
                    add(
                        QuestEntry(
                            id = obj.optString("id").trim(),
                            title = title.take(180),
                            status = QuestStatus.fromKey(obj.optString("status")).key,
                            updatedAtTurn = turnNumber
                        )
                    )
                }
            }
        }.ifEmpty {
            legacyActiveQuests.mapIndexed { idx, title ->
                QuestEntry(
                    id = "quest_${turnNumber}_$idx",
                    title = title,
                    status = QuestStatus.ACTIVE.key,
                    updatedAtTurn = turnNumber
                )
            }
        }

        val activeQuests = structuredQuests
            .filter { it.resolvedStatus() == QuestStatus.ACTIVE }
            .map { it.title }
            .ifEmpty { legacyActiveQuests }
            .take(6)

        val healthPercent = root.optLong("healthPercent", 100L).coerceIn(0L, 100L)
        val healthStatus = root.optString("healthStatus").trim()
        val currencyLabel = root.optString("currencyLabel").trim()
        val parsedCurrencyAmount = if (root.has("currencyAmount")) {
            root.optLong("currencyAmount", 0L).coerceAtLeast(0L)
        } else {
            Regex("""(\d+)""").find(currencyLabel)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        }

        val rawStatus = root.optString("hudStatusAndInventory").trim()
        val formattedStatus = buildString {
            if (healthStatus.isNotBlank()) {
                append("Здоровье: ")
                append(healthStatus)
                append(" (")
                append(healthPercent)
                append("%)")
            } else if (rawStatus.isNotBlank()) {
                append(rawStatus)
            } else {
                append("Здоровье: В норме (")
                append(healthPercent)
                append("%)")
            }
            if (currencyLabel.isNotBlank() && !toString().contains(currencyLabel, ignoreCase = true)) {
                append(" • ")
                append(currencyLabel)
            }
        }.take(280)

        val sceneWeather = root.optString("sceneWeather").trim().lowercase()
        val rawTime = root.optString("hudTime").trim().ifBlank { "Сейчас" }
        val normalizedTime = if (sceneWeather.isNotBlank() && !weatherHintAlreadyPresent(rawTime)) {
            val tag = when (sceneWeather) {
                "rain" -> "дождь"
                "storm" -> "гроза"
                "fog" -> "туман"
                "snow" -> "снег"
                "night" -> "ночь"
                "dawn" -> "рассвет"
                else -> ""
            }
            if (tag.isNotBlank()) "$rawTime, $tag" else rawTime
        } else {
            rawTime
        }

        val chapterArchiveSummary = root.optString("chapterArchiveSummary").trim().takeIf { it.isNotBlank() }
        val stateChangeSummary = root.optString("stateChangeSummary").trim().take(400)

        return GmTurnOutcome(
            narrative = narrative,
            hudLocation = root.optString("hudLocation").trim().ifBlank { "Неизвестно" },
            hudTime = normalizedTime,
            hudStatusAndInventory = formattedStatus,
            hudRelationsAndCanon = root.optString("hudRelationsAndCanon").trim().ifBlank { "Канон стабилен" },
            canonDeviationPercent = root.optLong("canonDeviationPercent", 0L).coerceIn(0L, 100L),
            activeNpc = activeNpc,
            inventory = effectiveFlatInventory,
            npcs = npcs,
            newButterflyEvent = butterflyEvent,
            storySummary = root.optString("storySummary").trim(),
            choices = choices,
            activeQuests = activeQuests,
            healthPercent = healthPercent,
            currencyAmount = parsedCurrencyAmount,
            currencyLabel = currencyLabel,
            skills = defaultSkills.coerced(),
            inventoryItems = structuredInventory,
            quests = structuredQuests,
            chapterArchiveSummary = chapterArchiveSummary,
            stateChangeSummary = stateChangeSummary
        )
    }

    private fun weatherHintAlreadyPresent(timeText: String): Boolean {
        val lower = timeText.lowercase()
        return listOf("дожд", "ливен", "гроз", "шторм", "туман", "снег", "ноч", "рассвет", "сумер", "пасмур", "ясн", "свеч", "неон")
            .any { lower.contains(it) }
    }

    internal companion object {
        const val TAG = "GameMasterAiRepo"
        const val SLIDING_CONTEXT_TURNS = 7
        const val FLASH_38_TIMEOUT_MS = 35_000L
        const val FLASH_LITE_TIMEOUT_MS = 55_000L
        const val COOLDOWN_MS = 120_000L
    }
}
