package com.example.data.repository

import android.util.Log
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
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

class GameMasterAiRepository(
    private val debugTokenProvider: () -> String? = { null }
) {
    @Volatile
    private var primaryCooldownUntilMs: Long = 0L

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
            "healthPercent" to Schema.integer(description = "Здоровье героя в процентах от 0 до 100"),
            "healthStatus" to Schema.string(description = "Краткое состояние героя (2-3 слова), например: В норме, Ранен, Утомлён"),
            "currencyLabel" to Schema.string(description = "Текущие деньги или ресурс (например: 15 крон, 300 эдди) или пустая строка"),
            "hudStatusAndInventory" to Schema.string(description = "Краткое состояние персонажа для HUD"),
            "hudRelationsAndCanon" to Schema.string(description = "Краткий статус отношений и влияния на канон для HUD"),
            "canonDeviationPercent" to Schema.integer(description = "Оценка отклонения от канона от 0 до 100"),
            "activeNpc" to Schema.obj(
                properties = mapOf(
                    "hasActiveNpc" to Schema.boolean(description = "true, если игрок сейчас общается или напрямую взаимодействует с NPC"),
                    "name" to Schema.string(description = "Имя NPC или пустая строка, если рядом никого нет"),
                    "emotionLabel" to Schema.string(description = "Точная эмоция NPC на русском, например: Смущение, Злость, Радость, Подозрение"),
                    "emotionCategory" to Schema.enumeration(
                        values = emotionCategories,
                        description = "Категория эмоции NPC"
                    ),
                    "intensity" to Schema.integer(description = "Интенсивность эмоции от 1 до 5"),
                    "note" to Schema.string(description = "Краткая ремарка о мимике, тоне голоса или поведении NPC")
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
                        "trustLevel" to Schema.integer(description = "От -100 до 100"),
                        "statusLabel" to Schema.string(),
                        "lastEmotion" to Schema.string(),
                        "lastEmotionCategory" to Schema.enumeration(values = emotionCategories),
                        "archetype" to Schema.enumeration(values = npcArchetypes)
                    )
                ),
                description = "Актуальный список ключевых встреченных NPC и их отношений с игроком"
            ),
            "hasButterflyEvent" to Schema.boolean(description = "true, если на этом ходе произошло значимое изменение канона или важный поворот"),
            "butterflyEventTitle" to Schema.string(description = "Короткий заголовок события эффекта бабочки (или пустая строка)"),
            "butterflyEventConsequence" to Schema.string(description = "Описание долгосрочного последствия для сюжета"),
            "butterflyEventImpact" to Schema.enumeration(values = listOf("minor", "major", "critical")),
            "storySummary" to Schema.string(description = "Обновлённое краткое резюме сюжета кампании (2-5 предложений)"),
            "choices" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "approach" to Schema.string(description = "Подход: Силовой, Дипломатический, Скрытный или Нестандартный"),
                        "text" to Schema.string(description = "Краткое описание действия игрока на русском языке")
                    )
                ),
                description = "3 или 4 принципиально разных варианта действий игрока"
            )
        ),
        optionalProperties = listOf("currencyLabel", "activeNpcArchetype", "sceneWeather")
    )

    private val systemPrompt = content {
        text(
            """
            Ты — опытный гейм-мастер и интерактивный рассказчик текстовых ролевых игр (RPG) с открытым миром и эффектом бабочки. Твоя задача — вести глубокий, нелинейный сюжет в выбранной игроком вселенной на русском языке, строго соблюдая её лор, законы, атмосферу и психологию персонажей.

            СТРОГИЕ ПРАВИЛА ИГРЫ:
            1. Никакого контроля над игроком (Anti-Godmodding): описывай только мир, окружение, действия и реплики NPC, а также последствия поступков игрока. НИКОГДА не описывай мысли, чувства, слова или будущие действия за игрока. Останавливай повествование ровно в момент, когда требуется решение игрока.
            2. Канон и эффект бабочки: изначально мир и персонажи строго соответствуют канону (их характерам, мотивациям, силам и знаниям). Однако любое вмешательство игрока способно необратимо изменить сюжет. Персонажи реагируют реалистично, исходя из их личности, а не подыгрывают.
            3. Реальные последствия и отсутствие сюжетной брони: ошибки, слабая подготовка, неверные слова или безрассудство приводят к провалам, травмам, потере союзников или гибели.
            4. Динамика и темп: пиши ярко, литературно, кинематографично, но без «воды». 2–4 выразительных абзаца в каждом ходе.
            5. Эмоции NPC: при любом контакте с персонажем обязательно заполняй activeNpc (hasActiveNpc = true), указывая его точную эмоцию (например: Смущение, Злость, Счастье, Подозрение, Страх, Удивление, Холодное спокойствие), интенсивность (1–5) и краткое наблюдение за мимикой/интонацией.
            6. Варианты действий: всегда предлагай 3–4 принципиально разных варианта (например: Силовой, Дипломатический, Скрытный, Нестандартный), сформулированных от лица выбора игрока, не дублирующих друг друга.
            7. Язык: весь ответ исключительно на русском языке (кроме перечислений enum, требуемых схемой).
            """.trimIndent()
        )
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = turnResponseSchema
                temperature = 0.85f
                maxOutputTokens = 4096
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = systemPrompt
        )
    }

    private val fastFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = turnResponseSchema
                temperature = 0.85f
                maxOutputTokens = 4096
            },
            systemInstruction = systemPrompt
        )
    }

    suspend fun generateOpeningScene(setup: SessionSetupInput): GmTurnOutcome {
        val prompt = """
            НАЧАЛО НОВОЙ ХРОНИКИ.
            Игрок ответил на 4 стартовых вопроса:
            1. Вселенная: ${setup.universe}
            2. Персонаж: ${setup.characterSummary}
            3. Точка старта: ${setup.startingPoint}
            4. Тон и сложность: ${setup.toneAndDifficulty}

            Сгенерируй захватывающую стартовую сцену, установи стартовый HUD, инвентарь, начальное состояние NPC (если рядом есть персонаж в точке старта — обязательно передай его эмоцию в activeNpc) и дай первые 3–4 принципиально разных варианта действий. Останови сцену в момент, требующий первого решения игрока.
        """.trimIndent()

        return invokeModel(prompt, defaultTurnNumber = 1L)
    }

    suspend fun generateNextTurn(
        campaign: Campaign,
        recentTurns: List<CampaignTurn>,
        playerAction: String
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
                "- ${npc.name} (${npc.role}): статус «${npc.statusLabel}», доверие ${npc.trustLevel}, последняя эмоция: ${npc.lastEmotion}"
            }
        }

        val butterflyBlock = if (campaign.butterflyEvents.isEmpty()) {
            "Пока без значительных отклонений от канона."
        } else {
            campaign.butterflyEvents.takeLast(10).joinToString("\n") { ev ->
                "- Ход ${ev.turnNumber} [${ev.impactLevel}]: ${ev.title} — ${ev.consequence}"
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

            ИЗВЕСТНЫЕ NPC И ОТНОШЕНИЯ:
            $npcsBlock

            ЭФФЕКТ БАБОЧКИ (КЛЮЧЕВЫЕ ПОВОРОТЫ):
            $butterflyBlock

            ПОСЛЕДНИЕ ХОДЫ:
            $historyBlock

            НОВОЕ ДЕЙСТВИЕ ИГРОКА:
            "$playerAction"

            Оцени логичность, подготовку и контекст действия игрока. Опиши реакцию мира и NPC (строго без контроля над игроком!). Обнови HUD, эмоции активного NPC, инвентарь, отношения, процент отклонения от канона и предложи 3–4 новых разных выбора.
        """.trimIndent()

        return invokeModel(prompt, defaultTurnNumber = campaign.turnCount + 1L)
    }

    private suspend fun invokeModel(prompt: String, defaultTurnNumber: Long): GmTurnOutcome {
        try {
            val now = System.currentTimeMillis()
            val useFastOnly = now < primaryCooldownUntilMs

            val jsonString = if (useFastOnly) {
                streamModelJson(fastFallbackModel, prompt, FALLBACK_TIMEOUT_MS)
            } else {
                try {
                    streamModelJson(primaryModel, prompt, PRIMARY_TIMEOUT_MS)
                } catch (timeout: TimeoutCancellationException) {
                    Log.w(TAG, "Primary model gemini-3.8-flash timed out; switching to gemini-3.5-flash-lite", timeout)
                    primaryCooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                    streamModelJson(fastFallbackModel, prompt, FALLBACK_TIMEOUT_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isQuotaOrRateLimitError(e)) {
                        Log.w(TAG, "Primary model hit quota/rate-limit; switching to gemini-3.5-flash-lite", e)
                        primaryCooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                        streamModelJson(fastFallbackModel, prompt, FALLBACK_TIMEOUT_MS)
                    } else {
                        throw e
                    }
                }
            }

            if (jsonString.isBlank()) {
                throw IllegalStateException("ИИ-мастер вернул пустой ответ. Попробуйте повторить ход.")
            }
            return parseTurnOutcome(jsonString, defaultTurnNumber)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            throw IllegalStateException("ИИ-мастер слишком долго обдумывал сцену. Проверьте интернет и попробуйте ещё раз.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            val rawMessage = e.localizedMessage ?: "Не удалось получить ответ от ИИ-мастера."
            val debugToken = if (
                rawMessage.contains("App Check", ignoreCase = true) ||
                rawMessage.contains("token is invalid", ignoreCase = true) ||
                rawMessage.contains("403", ignoreCase = true)
            ) {
                debugTokenProvider()
            } else {
                null
            }
            val finalMessage = when {
                !debugToken.isNullOrBlank() ->
                    "$rawMessage\n\nDebug-токен этого телефона:\n$debugToken\n\nДобавьте его в Firebase Console → App Check → Apps → Manage debug tokens."
                isQuotaOrRateLimitError(e) ->
                    formatQuotaErrorMessage(rawMessage)
                else -> rawMessage
            }
            throw IllegalStateException(finalMessage, e)
        }
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

    private suspend fun streamModelJson(
        targetModel: GenerativeModel,
        prompt: String,
        timeoutMs: Long
    ): String = withTimeout(timeoutMs) {
        val builder = StringBuilder()
        targetModel.generateContentStream(prompt).collect { chunk ->
            chunk.text?.let { builder.append(it) }
        }
        builder.toString().trim()
    }

    private fun parseTurnOutcome(jsonString: String, turnNumber: Long): GmTurnOutcome {
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
                    val cat = EmotionCategory.fromKey(obj.optString("lastEmotionCategory", "neutral"))
                    val parsedArchetype = obj.optString("archetype").trim().lowercase()
                        .ifBlank {
                            if (name.equals(activeNpc.name, ignoreCase = true)) activeNpcArchetype else ""
                        }
                    add(
                        NpcRelation(
                            name = name,
                            role = obj.optString("role").trim(),
                            trustLevel = obj.optLong("trustLevel", 0L).coerceIn(-100L, 100L),
                            statusLabel = obj.optString("statusLabel").trim(),
                            lastEmotion = obj.optString("lastEmotion").trim().ifBlank { cat.russianDefault },
                            lastEmotionCategory = cat.key,
                            archetype = parsedArchetype
                        )
                    )
                }
            }
            if (activeNpc.hasActiveNpc &&
                activeNpc.name.isNotBlank() &&
                none { it.name.equals(activeNpc.name, ignoreCase = true) }
            ) {
                add(
                    NpcRelation(
                        name = activeNpc.name,
                        role = "Собеседник в сцене",
                        trustLevel = 0L,
                        statusLabel = "Первая встреча",
                        lastEmotion = activeNpc.emotionLabel,
                        lastEmotionCategory = activeNpc.emotionCategory,
                        archetype = activeNpcArchetype
                    )
                )
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
                    add(
                        TurnChoice(
                            id = obj.optString("id").ifBlank { (i + 1).toString() },
                            approach = obj.optString("approach").trim().ifBlank { "Действие" },
                            text = text
                        )
                    )
                }
            }
        }

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
            choices = choices
        )
    }

    private fun weatherHintAlreadyPresent(timeText: String): Boolean {
        val lower = timeText.lowercase()
        return listOf("дожд", "ливен", "гроз", "шторм", "туман", "снег", "ноч", "рассвет", "сумер", "пасмур", "ясн", "свеч", "неон")
            .any { lower.contains(it) }
    }

    private companion object {
        const val TAG = "GameMasterAiRepo"
        const val PRIMARY_TIMEOUT_MS = 20_000L
        const val FALLBACK_TIMEOUT_MS = 35_000L
        const val COOLDOWN_MS = 120_000L
    }
}
