package com.example.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CampaignTurn

@Composable
internal fun PastChaptersSection(
    earlierTurns: List<CampaignTurn>,
    chapterSummaries: List<String>,
    bookmarkedTurns: Set<Long> = emptySet(),
    expanded: Boolean,
    isBusy: Boolean,
    accentPrimary: Color,
    accentSecondary: Color,
    onToggleExpanded: () -> Unit,
    onToggleBookmark: (CampaignTurn) -> Unit = {},
    onForkAtTurn: (Long) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showArchive by rememberSaveable { mutableStateOf(false) }

    val filteredTurns = remember(earlierTurns, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            earlierTurns
        } else {
            earlierTurns.filter { turn ->
                turn.narrative.contains(q, ignoreCase = true) ||
                    turn.playerAction.contains(q, ignoreCase = true) ||
                    "глава ${turn.turnNumber}".contains(q, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onToggleExpanded)
                .testTag("toggle_history_chapters")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = accentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (expanded) {
                            "Скрыть прошлые главы (${earlierTurns.size})"
                        } else {
                            "Прошлые главы (${earlierTurns.size})"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                // Live Search across past chapters
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по прошлым главам, именам и действиям…") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Очистить поиск",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                // Optional collapsible Chapter Archive (auto-compressed every 10 turns)
                if (chapterSummaries.isNotEmpty()) {
                    Surface(
                        color = accentSecondary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, accentSecondary.copy(alpha = 0.34f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showArchive = !showArchive }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoStories,
                                        contentDescription = null,
                                        tint = accentSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Архив глав (${chapterSummaries.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (showArchive) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (showArchive) {
                                chapterSummaries.forEachIndexed { idx, summary ->
                                    Text(
                                        text = "${idx + 1}. $summary",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredTurns.isEmpty()) {
                    Text(
                        text = "По запросу «$searchQuery» совпадений не найдено.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                } else {
                    filteredTurns.forEach { pastTurn ->
                        PastChapterCard(
                            turn = pastTurn,
                            isBookmarked = pastTurn.turnNumber in bookmarkedTurns,
                            accentColor = accentPrimary,
                            secondaryColor = accentSecondary,
                            isBusy = isBusy,
                            onToggleBookmark = { onToggleBookmark(pastTurn) },
                            onForkHere = { onForkAtTurn(pastTurn.turnNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastChapterCard(
    turn: CampaignTurn,
    isBookmarked: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    isBusy: Boolean,
    onToggleBookmark: () -> Unit,
    onForkHere: () -> Unit
) {
    val narratorBubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        shape = narratorBubbleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (isBookmarked) secondaryColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Глава ${turn.turnNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Убрать закладку" else "В закладки",
                            tint = if (isBookmarked) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    TextButton(
                        onClick = onForkHere,
                        enabled = !isBusy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("fork_turn_${turn.turnNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ответвить от хода ${turn.turnNumber}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                Text(
                    text = "Вы: ${turn.playerAction}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = turn.narrative,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
            )
        }
    }
}
