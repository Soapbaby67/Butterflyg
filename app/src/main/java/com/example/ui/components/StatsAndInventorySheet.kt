package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Campaign
import com.example.ui.theme.UniverseVisualPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsAndInventorySheet(
    campaign: Campaign,
    stats: ParsedHudStats,
    palette: UniverseVisualPalette,
    onItemClick: (String) -> Unit,
    onOpenCodex: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        StatsAndInventoryContent(
            campaign = campaign,
            stats = stats,
            palette = palette,
            onItemClick = { itemName ->
                onDismiss()
                onItemClick(itemName)
            },
            onOpenCodex = {
                onDismiss()
                onOpenCodex()
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
fun StatsAndInventoryContent(
    campaign: Campaign,
    stats: ParsedHudStats,
    palette: UniverseVisualPalette,
    onItemClick: (String) -> Unit,
    onOpenCodex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val healthFraction by animateFloatAsState(
        targetValue = stats.healthPercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(),
        label = "sheet_health_fraction"
    )
    val canonFraction by animateFloatAsState(
        targetValue = campaign.canonDeviationPercent.coerceIn(0L, 100L) / 100f,
        animationSpec = spring(),
        label = "sheet_canon_fraction"
    )
    val healthColor = when {
        stats.healthPercent >= 70 -> palette.healthHigh
        stats.healthPercent >= 40 -> palette.healthMid
        else -> palette.healthLow
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_inventory_sheet"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Статистика и снаряжение",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ход ${campaign.turnCount} • ${campaign.universe}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = weatherIconFor(stats.weatherMood),
                                contentDescription = stats.weatherMood.russianLabel,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stats.weatherMood.russianLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Location & Time Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Explore,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = campaign.hudLocation.ifBlank { "Текущая локация" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (campaign.hudTime.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = campaign.hudTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Health, Currency & Butterfly Meter Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = null,
                                    tint = healthColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${stats.healthLabel} (${stats.healthPercent}%)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (stats.currencyLabel.isNotBlank()) {
                                Surface(
                                    color = palette.accentSecondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Paid,
                                            contentDescription = "Ресурсы",
                                            tint = palette.accentSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stats.currencyLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.accentSecondary
                                        )
                                    }
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { healthFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = healthColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Эффект бабочки",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "${campaign.canonDeviationPercent.coerceIn(0L, 100L)}% • событий: ${campaign.butterflyEvents.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentSecondary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { canonFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = palette.accentSecondary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Отношения: ${stats.relationsSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Visual Inventory Section (every item has its own picture tile!)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Инвентарь (${stats.inventoryItems.size}) — нажмите на предмет",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stats.inventoryItems.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "В карманах пока пусто. Предметы появятся по мере развития истории.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            stats.inventoryItems.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { itemName ->
                                        val spec = remember(itemName) { resolveItemVisualSpec(itemName) }
                                        Surface(
                                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                            shape = MaterialTheme.shapes.medium,
                                            border = BorderStroke(1.dp, spec.accent.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    onItemClick(itemName)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                ItemPictureTile(
                                                    itemName = itemName,
                                                    palette = palette,
                                                    size = 42.dp
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = itemName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = spec.kind.categoryTitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onOpenCodex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stats_open_codex_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть полный Кодекс и досье NPC")
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
