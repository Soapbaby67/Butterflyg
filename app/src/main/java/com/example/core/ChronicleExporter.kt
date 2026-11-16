package com.example.core

import android.content.Context
import android.content.Intent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn

object ChronicleExporter {

    fun formatChronicleText(
        campaign: Campaign,
        turns: List<CampaignTurn>
    ): String = buildString {
        appendLine("═══ ХРОНИКА: ${campaign.title.ifBlank { "Без названия" }} ═══")
        appendLine("Вселенная: ${campaign.universe}")
        if (campaign.characterSummary.isNotBlank()) {
            appendLine("Герой: ${campaign.characterSummary}")
        }
        appendLine("Отклонение от канона: ${campaign.canonDeviationPercent}%")
        appendLine()

        if (campaign.activeQuests.isNotEmpty()) {
            appendLine("Активные цели:")
            campaign.activeQuests.forEach { quest ->
                appendLine("• $quest")
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
}
