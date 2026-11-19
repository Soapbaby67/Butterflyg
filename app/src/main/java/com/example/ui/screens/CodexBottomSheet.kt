package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ChronicleExporter
import com.example.core.LocalRpgAudioEngine
import com.example.core.SfxEvent
import com.example.data.model.AchievementStatus
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.InventoryItem
import com.example.data.model.NpcRelation
import com.example.data.model.QuestStatus
import com.example.data.repository.PlayerFactNote
import com.example.ui.components.ButterflyBranchMapCard
import com.example.ui.components.ChronicleAchievementsTab
import com.example.ui.components.EmotionBadgeChip
import com.example.ui.components.ItemPictureTile
import com.example.ui.components.NpcPortraitAvatar
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.theme.rememberUniverseVisualPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexBottomSheet(
    campaign: Campaign,
    turns: List<CampaignTurn> = emptyList(),
    facts: List<PlayerFactNote> = emptyList(),
    achievementStatuses: List<AchievementStatus> = emptyList(),
    isBusy: Boolean = false,
    initialTabIndex: Int = 0,
    onAddFact: (String) -> Unit = {},
    onToggleFactPinned: (String) -> Unit = {},
    onDeleteFact: (String) -> Unit = {},
    onItemClick: (InventoryItem) -> Unit = {},
    onForkAtTurn: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTabIndex) {
        mutableIntStateOf(initialTabIndex.coerceIn(0, 4))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        CodexContent(
            campaign = campaign,
            turns = turns,
            facts = facts,
            achievementStatuses = achievementStatuses,
            isBusy = isBusy,
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            onAddFact = onAddFact,
            onToggleFactPinned = onToggleFactPinned,
            onDeleteFact = onDeleteFact,
            onItemClick = onItemClick,
            onForkAtTurn = onForkAtTurn,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexContent(
    campaign: Campaign,
    turns: List<CampaignTurn> = emptyList(),
    facts: List<PlayerFactNote> = emptyList(),
    achievementStatuses: List<AchievementStatus> = emptyList(),
    isBusy: Boolean = false,
    selectedTab: Int = 0,
    onTabSelect: (Int) -> Unit = {},
    onAddFact: (String) -> Unit = {},
    onToggleFactPinned: (String) -> Unit = {},
    onDeleteFact: (String) -> Unit = {},
    onItemClick: (InventoryItem) -> Unit = {},
    onForkAtTurn: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val unlockedCount = achievementStatuses.count { it.isUnlocked }
    val tabs = listOf(
        "Герой",
        "NPC (${campaign.npcs.size})",
        "Бабочка",
        "Вехи ($unlockedCount)",
        "Факты (${facts.size})"
    )
    val universePalette = rememberUniverseVisualPalette(campaign.universe)
    val clampedTab = selectedTab.coerceIn(0, 4)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Кодекс: ${campaign.title}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            text = campaign.universe,
            style = MaterialTheme.typography.bodySmall,
            color = universePalette.accentPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PrimaryScrollableTabRow(
            selectedTabIndex = clampedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = clampedTab == index,
                    onClick = { onTabSelect(index) },
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

        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            when (clampedTab) {
                0 -> CharacterAndInventoryTab(
                    campaign = campaign,
                    turns = turns,
                    palette = universePalette,
                    onItemClick = onItemClick
                )
                1 -> NpcRelationsTab(
                    npcs = campaign.npcs,
                    universe = campaign.universe,
                    palette = universePalette
                )
                2 -> ButterflyTimelineTab(
                    campaign = campaign,
                    palette = universePalette,
                    isBusy = isBusy,
                    onForkAtTurn = onForkAtTurn
                )
                3 -> ChronicleAchievementsTab(
                    statuses = achievementStatuses,
                    palette = universePalette
                )
                4 -> PlayerFactsTab(
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
    turns: List<CampaignTurn>,
    palette: UniverseVisualPalette,
    onItemClick: (InventoryItem) -> Unit
) {
    val context = LocalContext.current
    val audio = LocalRpgAudioEngine.current
    val resolvedQuests = remember(campaign.quests, campaign.activeQuests) {
        campaign.resolvedQuests()
    }
    val resolvedItems = remember(campaign.inventoryItems, campaign.inventory) {
        campaign.resolvedInventoryItems()
    }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val markdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            val content = ChronicleExporter.formatChronicleMarkdown(campaign, turns)
            val saved = ChronicleExporter.writeTextToUri(context, uri, content)
            exportStatus = if (saved) {
                "Хроника сохранена в формате Markdown (.md)."
            } else {
                "Не удалось записать файл Markdown."
            }
        }
    }

    val htmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            val content = ChronicleExporter.formatChronicleHtml(campaign, turns)
            val saved = ChronicleExporter.writeTextToUri(context, uri, content)
            exportStatus = if (saved) {
                "Книга хроники сохранена в формате HTML (.html)."
            } else {
                "Не удалось записать файл HTML."
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (resolvedQuests.isNotEmpty()) {
            item {
                Surface(
                    color = palette.accentPrimary.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("codex_active_quests_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Журнал квестов и нитей сюжета",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        resolvedQuests.forEach { quest ->
                            val status = quest.resolvedStatus()
                            val (statusIcon, statusColor) = when (status) {
                                QuestStatus.ACTIVE -> Icons.Outlined.RadioButtonUnchecked to palette.accentPrimary
                                QuestStatus.COMPLETED -> Icons.Outlined.CheckCircle to palette.healthHigh
                                QuestStatus.FAILED -> Icons.Outlined.Cancel to palette.healthLow
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = status.labelRu,
                                    tint = statusColor,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = quest.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = if (status == QuestStatus.COMPLETED) {
                                                TextDecoration.LineThrough
                                            } else {
                                                TextDecoration.None
                                            }
                                        ),
                                        color = if (status == QuestStatus.ACTIVE) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    val turnBadge = if (quest.updatedAtTurn > 0L) " • Ход ${quest.updatedAtTurn}" else ""
                                    Text(
                                        text = "${status.labelRu}$turnBadge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (campaign.chapterSummaries.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("codex_chapter_archive_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Архив глав (${campaign.chapterSummaries.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        campaign.chapterSummaries.forEachIndexed { idx, summary ->
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

        item {
            val heroHeader = buildString {
                if (campaign.playerName.isNotBlank()) {
                    append(campaign.playerName)
                    append(" (")
                    append(campaign.resolvedGender().labelRu)
                    append(")\n")
                }
                append(campaign.characterSummary)
            }
            CodexInfoBlock(title = "Персонаж", body = heroHeader)
        }
        item {
            CodexInfoBlock(title = "Точка старта", body = campaign.startingPoint)
        }
        item {
            CodexInfoBlock(title = "Тон и правила", body = campaign.toneAndDifficulty)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        audio?.playSfx(SfxEvent.UI_TAP)
                        ChronicleExporter.shareChronicle(context, campaign, turns)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("share_chronicle_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Поделиться текстом")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            audio?.playSfx(SfxEvent.UI_TAP)
                            markdownLauncher.launch(
                                ChronicleExporter.suggestedFileName(campaign, "md")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("export_markdown_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить .md", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            audio?.playSfx(SfxEvent.UI_TAP)
                            htmlLauncher.launch(
                                ChronicleExporter.suggestedFileName(campaign, "html")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("export_html_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить .html", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (!exportStatus.isNullOrBlank()) {
                    Text(
                        text = exportStatus!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentPrimary
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Инвентарь (нажмите для действия)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (resolvedItems.isEmpty()) {
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
                        resolvedItems.forEach { item ->
                            Surface(
                                color = if (item.isEquipped) {
                                    palette.healthHigh.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                                },
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(
                                    width = if (item.isEquipped) 1.4.dp else 1.dp,
                                    color = if (item.isEquipped) {
                                        palette.healthHigh.copy(alpha = 0.65f)
                                    } else {
                                        palette.accentPrimary.copy(alpha = 0.35f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onItemClick(item) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                                ) {
                                    ItemPictureTile(
                                        itemName = item.name,
                                        palette = palette,
                                        size = 30.dp
                                    )
                                    val qtySuffix = if (item.quantity > 1L) " ×${item.quantity}" else ""
                                    Text(
                                        text = "${item.name}$qtySuffix",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (item.isEquipped) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(palette.healthHigh)
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

private val relationshipStages = listOf(
    "Вражда",
    "Настороженность",
    "Незнакомец",
    "Осторожный интерес",
    "Доверие",
    "Союзник / Романтика"
)

private fun relationshipStageIndex(trustLevel: Long): Int = when {
    trustLevel <= -50L -> 0
    trustLevel <= -15L -> 1
    trustLevel < 15L -> 2
    trustLevel < 45L -> 3
    trustLevel < 75L -> 4
    else -> 5
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
            val stageIdx = relationshipStageIndex(npc.trustLevel)
            val stageColor = if (npc.trustLevel >= 0L) palette.trustPositive else palette.trustNegative
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

                    // Visual Relationship Stage Progression Scale
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Стадия: ${relationshipStages[stageIdx]}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = stageColor
                            )
                            Text(
                                text = "${npc.statusLabel.ifBlank { "Нейтралитет" }} (${if (npc.trustLevel > 0) "+${npc.trustLevel}" else "${npc.trustLevel}"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            relationshipStages.indices.forEach { idx ->
                                val filled = idx <= stageIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (filled) {
                                                stageColor.copy(alpha = if (idx == stageIdx) 1f else 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                )
                            }
                        }
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
                    if (npc.recentActions.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Запомнил поступки игрока",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentPrimary
                                )
                                npc.recentActions.takeLast(3).forEach { entry ->
                                    Text(
                                        text = "• $entry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
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
private fun ButterflyTimelineTab(
    campaign: Campaign,
    palette: UniverseVisualPalette,
    isBusy: Boolean,
    onForkAtTurn: ((Long) -> Unit)?
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ButterflyBranchMapCard(
                campaign = campaign,
                palette = palette,
                isBusy = isBusy,
                onForkAtTurn = onForkAtTurn
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
