package com.example.ui.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ButterflyEvent
import com.example.data.model.CampaignTurn
import com.example.data.model.ChapterBookmark
import com.example.data.model.ChapterFilterMode
import com.example.data.model.ChapterSearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterNavigatorSheet(
    turns: List<CampaignTurn>,
    bookmarks: List<ChapterBookmark>,
    butterflyEvents: List<ButterflyEvent>,
    isBusy: Boolean,
    accentPrimary: Color,
    accentSecondary: Color,
    onToggleBookmark: (CampaignTurn) -> Unit,
    onForkAtTurn: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableStateOf(ChapterFilterMode.ALL) }
    var readingTurn by remember { mutableStateOf<CampaignTurn?>(null) }

    val itemsList = remember(turns, bookmarks, butterflyEvents, searchQuery, filterMode) {
        ChapterSearchEngine.filterAndSearch(
            turns = turns,
            bookmarks = bookmarks,
            butterflyEvents = butterflyEvents,
            query = searchQuery,
            filterMode = filterMode
        ).reversed()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Поиск и закладки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("chapter_search_sheet_title")
                    )
                    Text(
                        text = "Всего глав: ${turns.size} • в закладках: ${bookmarks.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Закрыть поиск"
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Найти сцену, имя, локацию или событие…") },
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
                    .testTag("global_chapter_search_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChapterFilterMode.entries.forEach { mode ->
                    val count = when (mode) {
                        ChapterFilterMode.ALL -> turns.size
                        ChapterFilterMode.BOOKMARKS -> bookmarks.size
                        ChapterFilterMode.BUTTERFLY -> butterflyEvents.map { it.turnNumber }.distinct().size
                    }
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { filterMode = mode },
                        label = {
                            Text(
                                text = "${mode.labelRu} ($count)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("chapter_filter_${mode.name.lowercase()}")
                    )
                }
            }

            if (itemsList.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                filterMode == ChapterFilterMode.BOOKMARKS && bookmarks.isEmpty() ->
                                    "Пока нет сохранённых закладок."
                                filterMode == ChapterFilterMode.BUTTERFLY && butterflyEvents.isEmpty() ->
                                    "Сдвиги канона ещё не зафиксированы."
                                else -> "Совпадений по запросу «$searchQuery» не найдено."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (filterMode) {
                                ChapterFilterMode.BOOKMARKS ->
                                    "Нажмите значок закладки на любой главе, чтобы быстро возвращаться к важным сценам."
                                ChapterFilterMode.BUTTERFLY ->
                                    "Решающие выборы появятся здесь и на карте «Эффекта бабочки»."
                                ChapterFilterMode.ALL ->
                                    "Попробуйте другое слово или очистите фильтр."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    items(itemsList, key = { it.turn.turnNumber }) { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (item.isBookmarked) {
                                    accentSecondary.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { readingTurn = item.turn }
                                .testTag("search_result_turn_${item.turn.turnNumber}")
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Глава ${item.turn.turnNumber}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentPrimary
                                        )
                                        if (item.butterflyEvent != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AutoAwesome,
                                                    contentDescription = "Сдвиг канона",
                                                    tint = accentSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = item.butterflyEvent.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { onToggleBookmark(item.turn) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("search_bookmark_turn_${item.turn.turnNumber}")
                                    ) {
                                        Icon(
                                            imageVector = if (item.isBookmarked) {
                                                Icons.Filled.Bookmark
                                            } else {
                                                Icons.Outlined.BookmarkBorder
                                            },
                                            contentDescription = if (item.isBookmarked) {
                                                "Убрать закладку"
                                            } else {
                                                "Добавить в закладки"
                                            },
                                            tint = if (item.isBookmarked) {
                                                accentSecondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (item.turn.turnNumber > 1L && item.turn.playerAction.isNotBlank()) {
                                    Text(
                                        text = "Вы: ${item.turn.playerAction}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = item.snippet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { readingTurn = item.turn },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Читать полностью",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            onDismiss()
                                            onForkAtTurn(item.turn.turnNumber)
                                        },
                                        enabled = !isBusy,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CallSplit,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ветка отсюда",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeReaderTurn = readingTurn
    if (activeReaderTurn != null) {
        val isBookmarked = bookmarks.any { it.turnNumber == activeReaderTurn.turnNumber }
        AlertDialog(
            onDismissRequest = { readingTurn = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Глава ${activeReaderTurn.turnNumber}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { onToggleBookmark(activeReaderTurn) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Закладка",
                            tint = if (isBookmarked) accentSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeReaderTurn.turnNumber > 1L && activeReaderTurn.playerAction.isNotBlank()) {
                        Text(
                            text = "Ваш ход: ${activeReaderTurn.playerAction}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = accentPrimary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Text(
                        text = activeReaderTurn.narrative,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { readingTurn = null }) {
                    Text("Закрыть")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val turnNum = activeReaderTurn.turnNumber
                        readingTurn = null
                        onDismiss()
                        onForkAtTurn(turnNum)
                    },
                    enabled = !isBusy
                ) {
                    Text("Создать ветку")
                }
            }
        )
    }
}
