package com.example.data.model

enum class ChapterFilterMode(val labelRu: String) {
    ALL("Все главы"),
    BOOKMARKS("Закладки"),
    BUTTERFLY("Сдвиги канона")
}

data class ChapterSearchItem(
    val turn: CampaignTurn,
    val isBookmarked: Boolean,
    val butterflyEvent: ButterflyEvent?,
    val snippet: String
)

/**
 * Pure search & filter engine across all chapters of a chronicle.
 */
object ChapterSearchEngine {

    fun filterAndSearch(
        turns: List<CampaignTurn>,
        bookmarks: List<ChapterBookmark>,
        butterflyEvents: List<ButterflyEvent>,
        query: String,
        filterMode: ChapterFilterMode
    ): List<ChapterSearchItem> {
        val bookmarkedTurns = bookmarks.map { it.turnNumber }.toSet()
        val butterflyByTurn = butterflyEvents.associateBy { it.turnNumber }
        val normalizedQuery = query.trim()

        return turns.asSequence()
            .filter { turn ->
                when (filterMode) {
                    ChapterFilterMode.ALL -> true
                    ChapterFilterMode.BOOKMARKS -> turn.turnNumber in bookmarkedTurns
                    ChapterFilterMode.BUTTERFLY -> butterflyByTurn.containsKey(turn.turnNumber)
                }
            }
            .filter { turn ->
                if (normalizedQuery.isBlank()) {
                    true
                } else {
                    val event = butterflyByTurn[turn.turnNumber]
                    turn.narrative.contains(normalizedQuery, ignoreCase = true) ||
                        turn.playerAction.contains(normalizedQuery, ignoreCase = true) ||
                        turn.hudLine.contains(normalizedQuery, ignoreCase = true) ||
                        "глава ${turn.turnNumber}".contains(normalizedQuery, ignoreCase = true) ||
                        (event != null && (
                            event.title.contains(normalizedQuery, ignoreCase = true) ||
                                event.consequence.contains(normalizedQuery, ignoreCase = true)
                            ))
                }
            }
            .map { turn ->
                ChapterSearchItem(
                    turn = turn,
                    isBookmarked = turn.turnNumber in bookmarkedTurns,
                    butterflyEvent = butterflyByTurn[turn.turnNumber],
                    snippet = buildSnippet(turn.narrative, normalizedQuery)
                )
            }
            .toList()
    }

    fun buildSnippet(text: String, query: String, maxLength: Int = 150): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= maxLength) return clean
        if (query.isBlank()) {
            return clean.take(maxLength).trimEnd() + "…"
        }
        val idx = clean.indexOf(query, ignoreCase = true)
        if (idx < 0) {
            return clean.take(maxLength).trimEnd() + "…"
        }
        val start = (idx - 42).coerceAtLeast(0)
        val end = (start + maxLength).coerceAtMost(clean.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < clean.length) "…" else ""
        return prefix + clean.substring(start, end).trim() + suffix
    }
}
