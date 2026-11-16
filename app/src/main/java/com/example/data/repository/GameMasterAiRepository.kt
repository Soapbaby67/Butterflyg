package com.example.data.repository

import android.util.Log
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.model.RiskOutcome
import com.example.data.model.RiskOutcomeTier
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
            boundaries = resolvedBoundaries
        )
    }
}

class GameMasterAiRepository(
    private val debugTokenProvider: () -> String? = { null },
    private val appCheckDiagnosticProvider: suspend () -> String? = { null }
) {
    @Volatile
    private var flash38CooldownUntilMs: Long = 0L

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

    private val turnResponseSchema = Schema.obj(
        properties = mapOf(
            "narrative" to Schema.string(
                description = "2-4 ярких абзаца повествования на русском языке, строго без описания мыслей, слов или действий игрока (Anti-Godmodding)."
            ),
            "hudLocation" to Schema.string(description = "Текущая локация для HUD"),
            "hudTime" to Schema.string(description = "Текущее внутриигровое время и погода для HUD"),
            "sceneWeather" to Schema.enumeration(
                values = weatherMoods,
                description = "Визуальное настроение/погода сцены для фона"
            ),
            "healthPercent" to Schema.integer(description = "Здоровье или ресурс сил героя в процентах от 0 до 100"),
            "healthStatus" to Schema.string(description = "Краткое состояние героя (2-3 слова), например: В норме, Ранен, Утомлён, Влюблён"),
            "currencyLabel" to Schema.string(description = "Текущие деньги или ресурс (например: 15 крон, 300 эдди, 2400 ₽) или пустая строка"),
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
                description = "Актуальный список предметов в инвентаре игрока"
            ),
            "npcs" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(),
                        "role" to Schema.string(),
                        "trustLevel" to Schema.integer(description = "От -100 до 100. Меняется плавно: +2..+10 за искренний удачный шаг, при навязчивости или нарушении границ падает."),
                        "statusLabel" to Schema.string(),
                        "lastEmotion" to Schema.string(),
                        "lastEmotionCategory" to Schema.enumeration(values = emotionCategories),
                        "archetype" to Schema.enumeration(values = npcArchetypes),
                        "personality" to Schema.string(description = "Короткий психологический портрет: характер, гордость, комплексы или скрытый мотив (до 120 символов)"),
                        "boundaries" to Schema.string(description = "Личные границы и красные линии NPC: на чём он отказывает, отстраняется или злится (до 120 символов)")
                    )
                ),
                description = "Актуальный список ключевых встреченных NPC, их характеров, личных границ и отношений с игроком"
            ),
            "hasButterflyEvent" to Schema.boolean(description = "true, если на этом ходе произошло значимое изменение канона или поворот отношений/судьбы"),
            "butterflyEventTitle" to Schema.string(description = "Короткий заголовок события эффекта бабочки (или пустая строка)"),
            "butterflyEventConsequence" to Schema.string(description = "Описание долгосрочного последствия для сюжета"),
            "butterflyEventImpact" to Schema.enumeration(values = listOf("minor", "major", "critical")),
            "storySummary" to Schema.string(description = "Обновлённое краткое резюме сюжета кампании (2-5 предложений)"),
            "activeQuests" to Schema.array(
                items = Schema.string(),
                description = "1-3 актуальные цели/зацепки героя на текущий момент (коротко, до 90 символов)"
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
        optionalProperties = listOf("currencyLabel", "activeNpcArchetype", "sceneWeather", "activeQuests")
    )

    private val systemPrompt = content {
        text(
            """
            Ты — опытный гейм-мастер и интерактивный рассказчик текстовых ролевых игр (RPG) в любых жанрах: от мрачного фэнтези и сай-фая до романтических историй, служебных драм и уютной повседневной рутины (Slice of Life). Твоя задача — вести глубокий, нелинейный сюжет в выбранной игроком вселенной на русском языке, строго соблюдая её лор, законы, атмосферу и психологию персонажей.

            СТРОГИЕ ПРАВИЛА ИГРЫ:
            1. Никакого контроля над игроком (Anti-Godmodding): описывай только мир, окружение, действия и реплики NPC, а также последствия поступков игрока. НИКОГДА не описывай мысли, чувства, слова или будущие действия за игрока. Останавливай повествование ровно в момент, когда требуется решение игрока.
            2. ПСИХОЛОГИЯ, СОПРОТИВЛЕНИЕ И ЛИЧНЫЕ ГРАНИЦЫ NPC (КРИТИЧЕСКИ ВАЖНО):
               - Запрещено мгновенное угождение: в романтике и повседневности NPC НЕ влюбляются, НЕ краснеют от первого же комплимента и НЕ соглашаются на просьбы с 1–3 сообщения. У каждого персонажа есть свои дела, смена, усталость, гордость, скепсис, комплексы и личные заботы.
               - Жёсткие личные границы и право на отказ: если игрок форсирует сближение, давит, льстит слишком очевидно, нарушает личное пространство, просит об одолжении без доверия или хамит — NPC обязан отказать, отшутиться с дистанцией, перевести тему, поставить границу или раздражённо отстраниться, снизив trustLevel.
               - Медленный слоубёрн: незнакомец стартует с trustLevel около -10..+15. Один удачный разговор даёт +2..+8, не больше. Настоящая симпатия и доверие зарабатываются постепенно через множество сцен, поступков и преодоление трений характеров.
               - Обязательно задавай каждому важному NPC поля "personality" и "boundaries" в JSON при знакомстве и строго придерживайся их дальше.
            3. Жанр и реализм: строго придерживайся выбранного тона. В боевом или мрачном сеттинге — нет сюжетной брони, ошибки ведут к травмам. В романтике и повседневной рутине — не выдумывай искусственные катастрофы на пустом месте; делай ставку на живые диалоги, бытовые детали, бюджет, неловкие паузы и конфликт интересов.
            4. Эффект бабочки и цели: любое вмешательство или слово игрока способно необратимо изменить сюжет или отношения. Персонажи помнят прошлые промахи и удачи. Веди 1–3 текущие цели в "activeQuests".
            5. Динамика и темп: пиши ярко, литературно, живо, но без «воды». 2–4 выразительных абзаца в каждом ходе. В JSON-ответе ВСЕГДА первым полем выдавай "narrative", чтобы игрок сразу видел живой текст.
            6. Эмоции NPC: при любом контакте с персонажем обязательно заполняй activeNpc (hasActiveNpc = true), указывая его точную эмоцию (например: Скепсис, Раздражение, Сдержанная улыбка, Смущение, Усталость, Подозрение, Теплота), интенсивность (1–5) и краткое наблюдение за мимикой/взглядом/интонацией.
            7. Предметы и контекст: если игрок использует или показывает предмет из инвентаря, обязательно учитывай этот предмет в сцене и при необходимости обновляй инвентарь. Обязательно учитывай блок «ЗАКРЕПЛЁННЫЕ ФАКТЫ ИГРОКА», если он передан.
            8. Варианты действий и рискованные развилки: всегда предлагай 3–4 принципиально разных варианта с подходящими к жанру тегами подхода (Силовой, Дипломатический, Скрытный, Нестандартный, Романтический, Заботливый, Бытовой, Эмоциональный, Дерзкий). Для 1–2 рискованных/дерзких/скрытных вариантов ставь "isRisky": true и заполняй 3–4 исхода в "riskOutcomes" (triumph, mixed, setback, twist). Если в действии игрока есть блок «[Бросок судьбы • ...]» — строй сцену строго по выпавшему исходу!
            9. Язык: весь ответ исключительно на русском языке (кроме перечислений enum, требуемых схемой).
            """.trimIndent()
        )
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelChoice.FLASH_LITE.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = turnResponseSchema
                temperature = 0.85f
                maxOutputTokens = 8192
            },
            systemInstruction = systemPrompt
        )
    }

    private val flash38Model by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelChoice.FLASH_38.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = turnResponseSchema
                temperature = 0.85f
                maxOutputTokens = 8192
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = systemPrompt
        )
    }

    suspend fun generateOpeningScene(
        setup: SessionSetupInput,
        preferredModel: AiModelChoice = AiModelChoice.FLASH_LITE
    ): GmTurnOutcome {
        val prompt = """
            НАЧАЛО НОВОЙ ХРОНИКИ.
            Игрок ответил на 4 стартовых вопроса:
            1. Вселенная: ${setup.universe}
            2. Персонаж: ${setup.characterSummary}
            3. Точка старта: ${setup.startingPoint}
            4. Тон и сложность: ${setup.toneAndDifficulty}

            Сгенерируй захватывающую стартовую сцену, установи стартовый HUD, инвентарь, начальное состояние NPC (если рядом есть персонаж в точке старта — обязательно передай его эмоцию в activeNpc) и дай первые 3–4 принципиально разных варианта действий. Останови сцену в момент, требующий первого решения игрока.
        """.trimIndent()

        return invokeModel(
            prompt = prompt,
            defaultTurnNumber = 1L,
            previousNpcs = emptyList(),
            preferredModel = preferredModel,
            useStreaming = false,
            onStreamingNarrative = {}
        )
    }

    suspend fun generateNextTurn(
        campaign: Campaign,
        recentTurns: List<CampaignTurn>,
        playerAction: String,
        preferredModel: AiModelChoice = AiModelChoice.FLASH_LITE,
        pinnedFacts: List<String> = emptyList(),
        onStreamingNarrative: (String) -> Unit = {}
    ): GmTurnOutcome {
        val historyBlock = recentTurns.takeLast(4).joinToString("\n\n") { turn ->
            buildString {
                appendLine("--- Ход ${turn.turnNumber} ---")
                if (turn.playerAction.isNotBlank()) {
                    appendLine("Действие игрока: ${turn.playerAction}")
                }
                appendLine("Повествование: ${turn.narrative}")
                appendLine("HUD: ${turn.hudLine}")
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

        val prompt = """
            КОНТЕКСТ КАМПАНИИ:
            - Вселенная: ${campaign.universe}
            - Персонаж игрока: ${campaign.characterSummary}
            - Точка старта: ${campaign.startingPoint}
            - Тон и сложность: ${campaign.toneAndDifficulty}
            - Текущий ход: ${campaign.turnCount + 1}
            - Отклонение от канона сейчас: ${campaign.canonDeviationPercent}%
            - Текущий инвентарь: ${campaign.inventory.joinToString(", ").ifBlank { "Пусто" }}
            - Резюме сюжета: ${campaign.storySummary.ifBlank { "Начало истории." }}
            $factsSection

            ИЗВЕСТНЫЕ NPC, ИХ ХАРАКТЕРЫ И ЛИЧНЫЕ ГРАНИЦЫ:
            $npcsBlock

            ЭФФЕКТ БАБОЧКИ (КЛЮЧЕВЫЕ ПОВОРОТЫ):
            $butterflyBlock

            ПОСЛЕДНИЕ ХОДЫ:
            $historyBlock

            НОВОЕ ДЕЙСТВИЕ ИГРОКА:
            "$playerAction"

            ПРОВЕРКА ГРАНИЦ: не позволяй NPC мгновенно соглашаться или влюбляться без веских оснований. Если игрок торопит события, давит или нарушает границы персонажа, NPC должен отказать, поставить условие или отстраниться.
            Оцени логичность, подготовку и контекст действия игрока. Опиши реакцию мира и NPC (строго без контроля над игроком!). Обнови HUD, эмоции активного NPC, инвентарь, отношения (trustLevel меняй плавно), процент отклонения от канона и предложи 3–4 новых разных выбора.
        """.trimIndent()

        return invokeModel(
            prompt = prompt,
            defaultTurnNumber = campaign.turnCount + 1L,
            previousNpcs = campaign.npcs,
            preferredModel = preferredModel,
            useStreaming = true,
            onStreamingNarrative = onStreamingNarrative
        )
    }

    private suspend fun invokeModel(
        prompt: String,
        defaultTurnNumber: Long,
        previousNpcs: List<NpcRelation>,
        preferredModel: AiModelChoice,
        useStreaming: Boolean = true,
        onStreamingNarrative: (String) -> Unit
    ): GmTurnOutcome {
        try {
            val now = System.currentTimeMillis()
            val shouldUseFlash38 = preferredModel == AiModelChoice.FLASH_38 && now >= flash38CooldownUntilMs

            val jsonString = if (!shouldUseFlash38) {
                fetchModelJson(
                    targetModel = flashLiteModel,
                    prompt = prompt,
                    timeoutMs = FLASH_LITE_TIMEOUT_MS,
                    useStreaming = useStreaming,
                    defaultTurnNumber = defaultTurnNumber,
                    previousNpcs = previousNpcs,
                    onStreamingNarrative = onStreamingNarrative
                )
            } else {
                try {
                    fetchModelJson(
                        targetModel = flash38Model,
                        prompt = prompt,
                        timeoutMs = FLASH_38_TIMEOUT_MS,
                        useStreaming = useStreaming,
                        defaultTurnNumber = defaultTurnNumber,
                        previousNpcs = previousNpcs,
                        onStreamingNarrative = onStreamingNarrative
                    )
                } catch (timeout: TimeoutCancellationException) {
                    Log.w(TAG, "gemini-3.8-flash timed out; switching to gemini-3.5-flash-lite", timeout)
                    flash38CooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                    onStreamingNarrative("")
                    fetchModelJson(
                        targetModel = flashLiteModel,
                        prompt = prompt,
                        timeoutMs = FLASH_LITE_TIMEOUT_MS,
                        useStreaming = useStreaming,
                        defaultTurnNumber = defaultTurnNumber,
                        previousNpcs = previousNpcs,
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
                            targetModel = flashLiteModel,
                            prompt = prompt,
                            timeoutMs = FLASH_LITE_TIMEOUT_MS,
                            useStreaming = useStreaming,
                            defaultTurnNumber = defaultTurnNumber,
                            previousNpcs = previousNpcs,
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
            return parseTurnOutcome(jsonString, defaultTurnNumber, previousNpcs)
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
            // Preserve quota and App Check errors directly without duplicate retries
            if (isQuotaOrRateLimitError(streamError) || isAppCheckError(streamError)) {
                throw streamError
            }
            // If SSE failed right at the end after full JSON already arrived, salvage it immediately
            val accumulated = builder.toString().trim()
            if (accumulated.isNotBlank() &&
                runCatching { parseTurnOutcome(accumulated, defaultTurnNumber, previousNpcs) }.isSuccess
            ) {
                Log.w(TAG, "Stream threw at EOF after valid JSON arrived; using accumulated response", streamError)
                return accumulated
            }
            // Otherwise seamlessly fall back to unary generateContent to bypass SSE chunk decoding issues
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
        previousNpcs: List<NpcRelation> = emptyList()
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
        val inventory = buildList {
            if (inventoryArray != null) {
                for (i in 0 until inventoryArray.length()) {
                    val item = inventoryArray.optString(i).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }

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
                    val rawRelation = NpcRelation(
                        name = name,
                        role = obj.optString("role").trim(),
                        trustLevel = obj.optLong("trustLevel", 0L).coerceIn(-100L, 100L),
                        statusLabel = obj.optString("statusLabel").trim(),
                        lastEmotion = obj.optString("lastEmotion").trim().ifBlank { cat.russianDefault },
                        lastEmotionCategory = cat.key,
                        archetype = parsedArchetype,
                        personality = obj.optString("personality").trim(),
                        boundaries = obj.optString("boundaries").trim()
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
                    boundaries = prev?.boundaries.orEmpty()
                )
                add(NpcTrustGuardrail.apply(fallbackRelation, prev))
            }
            // Keep previously tracked NPCs so long-term boundaries & trust persist across scenes
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
        val activeQuests = buildList {
            if (questsArray != null) {
                for (i in 0 until questsArray.length()) {
                    val q = questsArray.optString(i).trim()
                    if (q.isNotBlank()) add(q.take(160))
                }
            }
        }.take(6)

        val healthPercent = root.optLong("healthPercent", 100L).coerceIn(0L, 100L)
        val healthStatus = root.optString("healthStatus").trim()
        val currencyLabel = root.optString("currencyLabel").trim()
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

        return GmTurnOutcome(
            narrative = narrative,
            hudLocation = root.optString("hudLocation").trim().ifBlank { "Неизвестно" },
            hudTime = normalizedTime,
            hudStatusAndInventory = formattedStatus,
            hudRelationsAndCanon = root.optString("hudRelationsAndCanon").trim().ifBlank { "Канон стабилен" },
            canonDeviationPercent = root.optLong("canonDeviationPercent", 0L).coerceIn(0L, 100L),
            activeNpc = activeNpc,
            inventory = inventory,
            npcs = npcs,
            newButterflyEvent = butterflyEvent,
            storySummary = root.optString("storySummary").trim(),
            choices = choices,
            activeQuests = activeQuests
        )
    }

    private fun weatherHintAlreadyPresent(timeText: String): Boolean {
        val lower = timeText.lowercase()
        return listOf("дожд", "ливен", "гроз", "шторм", "туман", "снег", "ноч", "рассвет", "сумер", "пасмур", "ясн", "свеч", "неон")
            .any { lower.contains(it) }
    }

    private companion object {
        const val TAG = "GameMasterAiRepo"
        const val FLASH_38_TIMEOUT_MS = 35_000L
        const val FLASH_LITE_TIMEOUT_MS = 55_000L
        const val COOLDOWN_MS = 120_000L
    }
}
