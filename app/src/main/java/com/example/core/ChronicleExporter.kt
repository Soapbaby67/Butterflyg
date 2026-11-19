package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.QuestStatus

object ChronicleExporter {

    fun formatChronicleText(
        campaign: Campaign,
        turns: List<CampaignTurn>
    ): String = buildString {
        appendLine("═══ ХРОНИКА: ${campaign.title.ifBlank { "Без названия" }} ═══")
        appendLine("Вселенная: ${campaign.universe}")
        if (campaign.playerName.isNotBlank()) {
            appendLine("Имя героя: ${campaign.playerName} (${campaign.resolvedGender().labelRu})")
        }
        if (campaign.characterSummary.isNotBlank()) {
            appendLine("Герой: ${campaign.characterSummary}")
        }
        appendLine("Отклонение от канона: ${campaign.canonDeviationPercent}%")
        appendLine()

        val quests = campaign.resolvedQuests()
        if (quests.isNotEmpty()) {
            appendLine("Задачи и квесты:")
            quests.forEach { quest ->
                val mark = when (quest.resolvedStatus()) {
                    QuestStatus.ACTIVE -> "•"
                    QuestStatus.COMPLETED -> "✓"
                    QuestStatus.FAILED -> "✕"
                }
                appendLine("$mark ${quest.title} [${quest.resolvedStatus().labelRu}]")
            }
            appendLine()
        }

        if (turns.isEmpty()) {
            appendLine("Главы пока не записаны.")
        } else {
            turns.forEach { turn ->
                appendLine("── Глава ${turn.turnNumber} ──")
                if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                    appendLine("Выбор игрока: ${turn.playerAction}")
                }
                appendLine(turn.narrative.trim())
                appendLine()
            }
        }

        if (campaign.butterflyEvents.isNotEmpty()) {
            appendLine("── Эффект бабочки ──")
            campaign.butterflyEvents.forEach { ev ->
                appendLine("• Ход ${ev.turnNumber}: ${ev.title} — ${ev.consequence}")
            }
        }
    }.trim()

    fun formatChronicleMarkdown(
        campaign: Campaign,
        turns: List<CampaignTurn>
    ): String = buildString {
        val title = campaign.title.ifBlank { "Хроника без названия" }
        appendLine("# $title")
        appendLine()
        appendLine("- **Вселенная:** ${campaign.universe}")
        if (campaign.playerName.isNotBlank()) {
            appendLine("- **Герой:** ${campaign.playerName} (${campaign.resolvedGender().labelRu})")
        }
        if (campaign.characterSummary.isNotBlank()) {
            appendLine("- **Образ героя:** ${campaign.characterSummary}")
        }
        appendLine("- **Отклонение от канона:** ${campaign.canonDeviationPercent}%")
        appendLine("- **Навыки:** Натиск ${campaign.skills.might}/10 · Скрытность ${campaign.skills.stealth}/10 · Проницательность ${campaign.skills.insight}/10 · Эрудиция ${campaign.skills.lore}/10")
        appendLine()

        if (campaign.chapterSummaries.isNotEmpty()) {
            appendLine("## Архив глав")
            campaign.chapterSummaries.forEachIndexed { idx, summary ->
                appendLine("${idx + 1}. $summary")
            }
            appendLine()
        }

        val quests = campaign.resolvedQuests()
        if (quests.isNotEmpty()) {
            appendLine("## Журнал задач")
            quests.forEach { quest ->
                val checkbox = when (quest.resolvedStatus()) {
                    QuestStatus.COMPLETED -> "[x]"
                    QuestStatus.FAILED -> "[-]"
                    QuestStatus.ACTIVE -> "[ ]"
                }
                appendLine("- $checkbox ${quest.title} _(${quest.resolvedStatus().labelRu})_")
            }
            appendLine()
        }

        if (campaign.npcs.isNotEmpty()) {
            appendLine("## Действующие лица")
            campaign.npcs.forEach { npc ->
                appendLine("- **${npc.name}** (${npc.role.ifBlank { "Персонаж" }}) — доверие `${npc.trustLevel}`, статус: _${npc.statusLabel.ifBlank { "Нейтрально" }}_")
                if (npc.personality.isNotBlank()) {
                    appendLine("  - Характер: ${npc.personality}")
                }
            }
            appendLine()
        }

        appendLine("## Летопись")
        appendLine()
        if (turns.isEmpty()) {
            appendLine("_Главы пока не записаны._")
        } else {
            turns.forEach { turn ->
                appendLine("### Глава ${turn.turnNumber}")
                if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                    appendLine()
                    appendLine("> **Выбор игрока:** ${turn.playerAction.replace("\n", " ")}")
                }
                appendLine()
                appendLine(turn.narrative.trim())
                appendLine()
                if (turn.hudLine.isNotBlank()) {
                    appendLine("`${turn.hudLine}`")
                    appendLine()
                }
            }
        }

        if (campaign.butterflyEvents.isNotEmpty()) {
            appendLine("## Эффект бабочки")
            campaign.butterflyEvents.forEach { ev ->
                appendLine("- **Ход ${ev.turnNumber} (${ev.impactLevel}):** ${ev.title} — ${ev.consequence}")
            }
        }
    }.trim()

    fun formatChronicleHtml(
        campaign: Campaign,
        turns: List<CampaignTurn>
    ): String {
        val title = escapeHtml(campaign.title.ifBlank { "Хроника без названия" })
        val universe = escapeHtml(campaign.universe)
        val heroLine = escapeHtml(
            listOfNotNull(
                campaign.playerName.takeIf { it.isNotBlank() },
                campaign.characterSummary.takeIf { it.isNotBlank() }
            ).joinToString(" — ")
        )
        val tocHtml = if (turns.isNotEmpty()) {
            turns.joinToString("\n") { t ->
                """<li><a href="#turn-${t.turnNumber}">Глава ${t.turnNumber}</a></li>"""
            }
        } else {
            "<li>Главы пока не записаны</li>"
        }

        val chaptersHtml = turns.joinToString("\n") { turn ->
            val actionBlock = if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                """<blockquote class="action"><strong>Выбор игрока:</strong> ${escapeHtml(turn.playerAction)}</blockquote>"""
            } else {
                ""
            }
            val paragraphs = turn.narrative.trim()
                .split(Regex("""\n\s*\n"""))
                .filter { it.isNotBlank() }
                .joinToString("\n") { p -> "<p>${escapeHtml(p.trim()).replace("\n", "<br/>")}</p>" }
            """
            <article class="chapter" id="turn-${turn.turnNumber}">
              <h2>Глава ${turn.turnNumber}</h2>
              $actionBlock
              $paragraphs
              <div class="hud">${escapeHtml(turn.hudLine)}</div>
            </article>
            """.trimIndent()
        }

        val npcCardsHtml = if (campaign.npcs.isNotEmpty()) {
            val cards = campaign.npcs.joinToString("\n") { npc ->
                val actionsList = if (npc.recentActions.isNotEmpty()) {
                    "<ul>" + npc.recentActions.joinToString("") { "<li>${escapeHtml(it)}</li>" } + "</ul>"
                } else {
                    ""
                }
                """
                <div class="card">
                  <div class="card-title">${escapeHtml(npc.name)} <span class="badge">${npc.trustLevel}</span></div>
                  <div class="meta">${escapeHtml(npc.role)} • ${escapeHtml(npc.statusLabel)}</div>
                  ${if (npc.personality.isNotBlank()) "<p class=\"small\">${escapeHtml(npc.personality)}</p>" else ""}
                  $actionsList
                </div>
                """.trimIndent()
            }
            """<section id="npcs"><h2>Действующие лица</h2><div class="grid">$cards</div></section>"""
        } else {
            ""
        }

        val butterflyHtml = if (campaign.butterflyEvents.isNotEmpty()) {
            val items = campaign.butterflyEvents.joinToString("\n") { ev ->
                """<li><strong>Ход ${ev.turnNumber}: ${escapeHtml(ev.title)}</strong> — ${escapeHtml(ev.consequence)}</li>"""
            }
            """<section id="butterfly"><h2>Эффект бабочки</h2><ul class="timeline">$items</ul></section>"""
        } else {
            ""
        }

        return """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$title — Хроники Бабочки</title>
          <style>
            :root { --bg: #F6EFE4; --card: #FDF9F1; --ink: #241D18; --muted: #6C5D52; --accent: #8B2635; --brass: #A67C37; --line: #DFD2BE; }
            body { margin: 0; padding: 28px 18px 64px; background: var(--bg); color: var(--ink); font-family: Georgia, 'Times New Roman', serif; line-height: 1.72; }
            .book { max-width: 780px; margin: 0 auto; background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 32px 28px; box-shadow: 0 10px 30px rgba(36,29,24,0.08); }
            header { border-bottom: 2px solid var(--brass); padding-bottom: 18px; margin-bottom: 24px; }
            h1 { margin: 0 0 8px; font-size: 2rem; color: var(--accent); }
            h2 { color: var(--accent); margin-top: 28px; font-size: 1.35rem; border-bottom: 1px solid var(--line); padding-bottom: 6px; }
            .meta { color: var(--muted); font-family: system-ui, -apple-system, sans-serif; font-size: 0.92rem; }
            .toc { background: #F5ECE0; border-radius: 10px; padding: 14px 18px; margin: 18px 0 26px; }
            .toc ul { columns: 2; padding-left: 18px; margin: 8px 0 0; }
            .toc a { color: var(--accent); text-decoration: none; }
            .chapter { margin-bottom: 30px; }
            .action { margin: 12px 0; padding: 10px 14px; border-left: 3px solid var(--brass); background: #F8F1E5; font-style: italic; }
            .hud { margin-top: 10px; font-family: ui-monospace, monospace; font-size: 0.8rem; color: var(--muted); }
            .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
            .card { border: 1px solid var(--line); border-radius: 10px; padding: 12px; background: #FFFDF9; }
            .card-title { font-weight: bold; display: flex; justify-content: space-between; }
            .badge { color: var(--accent); font-family: system-ui, sans-serif; font-size: 0.85rem; }
            .small { font-size: 0.88rem; margin: 6px 0; }
            .timeline li { margin-bottom: 8px; }
          </style>
        </head>
        <body>
          <main class="book">
            <header>
              <h1>$title</h1>
              <div class="meta">$universe • Отклонение от канона: ${campaign.canonDeviationPercent}%</div>
              ${if (heroLine.isNotBlank()) "<p class=\"meta\">$heroLine</p>" else ""}
            </header>
            <nav class="toc">
              <strong>Оглавление</strong>
              <ul>$tocHtml</ul>
            </nav>
            $chaptersHtml
            $npcCardsHtml
            $butterflyHtml
          </main>
        </body>
        </html>
        """.trimIndent()
    }

    fun suggestedFileName(campaign: Campaign, extension: String): String {
        val base = campaign.title.ifBlank { "chronicle" }
            .lowercase()
            .replace(Regex("""[^a-zа-я0-9]+"""), "_")
            .trim('_')
            .take(40)
            .ifBlank { "chronicle" }
        val ext = extension.trimStart('.')
        return "${base}.$ext"
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun shareChronicle(
        context: Context,
        campaign: Campaign,
        turns: List<CampaignTurn>
    ) {
        val text = formatChronicleText(campaign, turns)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Хроника: ${campaign.title}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Поделиться хроникой").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeHtml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
