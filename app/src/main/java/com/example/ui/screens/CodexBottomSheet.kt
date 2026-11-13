package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.NpcRelation
import com.example.data.repository.PlayerFactNote
import com.example.ui.components.EmotionBadgeChip
import com.example.ui.components.NpcPortraitAvatar
import com.example.ui.components.itemCategoryIcon
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.theme.rememberUniverseVisualPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexBottomSheet(
    campaign: Campaign,
    facts: List<PlayerFactNote> = emptyList(),
    initialTabIndex: Int = 0,
    onAddFact: (String) -> Unit = {},
    onToggleFactPinned: (String) -> Unit = {},
    onDeleteFact: (String) -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTabIndex) {
        mutableIntStateOf(initialTabIndex.coerceIn(0, 3))
    }
    val tabs = listOf(
        "Герой",
        "NPC (${campaign.npcs.size})",
        "Бабочка",
        "Факты (${facts.size})"
    )
    val universePalette = rememberUniverseVisualPalette(campaign.universe)

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
        ) {
            Text(
                text = "Кодекс: ${campaign.title}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                text = campaign.universe,
                style = MaterialTheme.typography.bodySmall,
                color = universePalette.accentPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> CharacterAndInventoryTab(
                    campaign = campaign,
                    palette = universePalette,
                    onItemClick = onItemClick
                )
                1 -> NpcRelationsTab(
                    npcs = campaign.npcs,
                    universe = campaign.universe,
                    palette = universePalette
                )
                2 -> ButterflyTimelineTab(campaign)
                3 -> PlayerFactsTab(
                    facts = facts,
                    palette = universePalette,
                    onAddFact = onAddFact,
                    onTogglePinned = onToggleFactPinned,
                    onDeleteFact = onDeleteFact
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterAndInventoryTab(
    campaign: Campaign,
    palette: UniverseVisualPalette,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            CodexInfoBlock(title = "Персонаж", body = campaign.characterSummary)
        }
        item {
            CodexInfoBlock(title = "Точка старта", body = campaign.startingPoint)
        }
        item {
            CodexInfoBlock(title = "Тон и правила", body = campaign.toneAndDifficulty)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Инвентарь (нажмите для действия)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (campaign.inventory.isEmpty()) {
                    Text(
                        text = "Инвентарь пока пуст.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        campaign.inventory.forEach { itemName ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onItemClick(itemName) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = itemCategoryIcon(itemName),
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = itemName,
                                        style = MaterialTheme.typography.bodyMedium
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

@Composable
private fun PlayerFactsTab(
    facts: List<PlayerFactNote>,
    palette: UniverseVisualPalette,
    onAddFact: (String) -> Unit,
    onTogglePinned: (String) -> Unit,
    onDeleteFact: (String) -> Unit
) {
    var draftFact by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Surface(
                color = palette.accentPrimary.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Закреплённые факты автоматически передаются ИИ-мастеру на каждом ходе — удобно для обещаний, тайн, бюджета или деталей внешности.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draftFact,
                    onValueChange = { draftFact = it },
                    placeholder = { Text("Например: Обещал принести зонт завтра") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (draftFact.isNotBlank()) {
                                onAddFact(draftFact)
                                draftFact = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fact_input")
                )
                FilledIconButton(
                    onClick = {
                        if (draftFact.isNotBlank()) {
                            onAddFact(draftFact)
                            draftFact = ""
                        }
                    },
                    enabled = draftFact.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_fact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить факт"
                    )
                }
            }
        }

        if (facts.isEmpty()) {
            item {
                Text(
                    text = "Заметок пока нет. Добавьте первый факт, чтобы мастер помнил важную деталь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(facts, key = { it.id }) { fact ->
                Surface(
                    color = if (fact.isPinned) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (fact.isPinned) {
                            palette.accentPrimary.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fact_card_${fact.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onTogglePinned(fact.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (fact.isPinned) {
                                    Icons.Filled.PushPin
                                } else {
                                    Icons.Outlined.PushPin
                                },
                                contentDescription = if (fact.isPinned) {
                                    "Открепить из контекста ИИ"
                                } else {
                                    "Закрепить для ИИ"
                                },
                                tint = if (fact.isPinned) {
                                    palette.accentPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fact.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (fact.isPinned) "В контексте ИИ" else "Только для себя",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fact.isPinned) {
                                    palette.accentPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(
                            onClick = { onDeleteFact(fact.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Удалить заметку",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NpcRelationsTab(
    npcs: List<NpcRelation>,
    universe: String,
    palette: UniverseVisualPalette
) {
    if (npcs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ключевые персонажи пока не зафиксированы.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(npcs) { npc ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NpcPortraitAvatar(
                            name = npc.name,
                            role = npc.role,
                            universe = universe,
                            explicitArchetype = npc.archetype,
                            emotionCategory = npc.lastEmotionCategory,
                            intensity = 3L,
                            palette = palette,
                            size = 50.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = npc.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (npc.role.isNotBlank()) {
                                Text(
                                    text = npc.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (npc.lastEmotion.isNotBlank()) {
                            EmotionBadgeChip(
                                emotionLabel = npc.lastEmotion,
                                emotionCategory = npc.lastEmotionCategory
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = npc.statusLabel.ifBlank { "Нейтралитет" },
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentPrimary
                        )
                        val normalizedTrust = ((npc.trustLevel + 100L).coerceIn(0L, 200L)) / 200f
                        LinearProgressIndicator(
                            progress = { normalizedTrust },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (npc.trustLevel >= 0L) {
                                palette.trustPositive
                            } else {
                                palette.trustNegative
                            },
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = if (npc.trustLevel > 0) "+${npc.trustLevel}" else "${npc.trustLevel}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (npc.personality.isNotBlank()) {
                        Text(
                            text = "Характер: ${npc.personality}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (npc.boundaries.isNotBlank()) {
                        Text(
                            text = "Личные границы: ${npc.boundaries}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButterflyTimelineTab(campaign: Campaign) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Отклонение от канона: ${campaign.canonDeviationPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    LinearProgressIndicator(
                        progress = { campaign.canonDeviationPercent.coerceIn(0L, 100L) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (campaign.storySummary.isNotBlank()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = campaign.storySummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (campaign.butterflyEvents.isEmpty()) {
            item {
                Text(
                    text = "Пока события движутся близко к канону. Ваши смелые решения появятся здесь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(campaign.butterflyEvents.reversed()) { event ->
                ButterflyEventRow(event)
            }
        }
    }
}

@Composable
private fun ButterflyEventRow(event: ButterflyEvent) {
    val impactLabel = when (event.impactLevel.lowercase()) {
        "critical" -> "Критично"
        "major" -> "Значимо"
        else -> "Сдвиг"
    }
    val impactColor = when (event.impactLevel.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "major" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ход ${event.turnNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = impactLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = impactColor
                )
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.consequence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodexInfoBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
