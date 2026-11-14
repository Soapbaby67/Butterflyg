package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MoodBad
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Campaign
import com.example.data.model.EmotionCategory
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.model.NpcRelation
import com.example.data.repository.PaperThemeMode
import com.example.ui.theme.EmotionColors
import com.example.ui.theme.EmotionVisualPalette
import com.example.ui.theme.LocalIsDarkPaper
import com.example.ui.theme.UniverseVisualPalette

@Composable
fun emotionColorsFor(category: EmotionCategory): Pair<Color, Color> {
    val isDark = LocalIsDarkPaper.current
    val palette: EmotionVisualPalette = when (category) {
        EmotionCategory.JOY -> EmotionColors.Joy
        EmotionCategory.EMBARRASSMENT -> EmotionColors.Embarrassment
        EmotionCategory.ANGER -> EmotionColors.Anger
        EmotionCategory.FEAR -> EmotionColors.Fear
        EmotionCategory.SUSPICION -> EmotionColors.Suspicion
        EmotionCategory.SADNESS -> EmotionColors.Sadness
        EmotionCategory.SURPRISE -> EmotionColors.Surprise
        EmotionCategory.CALM -> EmotionColors.Calm
        EmotionCategory.NEUTRAL -> EmotionColors.Neutral
    }
    return if (isDark) {
        palette.containerDark to palette.contentDark
    } else {
        palette.containerLight to palette.contentLight
    }
}

fun emotionIconFor(category: EmotionCategory): ImageVector = when (category) {
    EmotionCategory.JOY -> Icons.Outlined.Mood
    EmotionCategory.EMBARRASSMENT -> Icons.Outlined.Favorite
    EmotionCategory.ANGER -> Icons.Outlined.Whatshot
    EmotionCategory.FEAR -> Icons.Outlined.WarningAmber
    EmotionCategory.SUSPICION -> Icons.Outlined.Visibility
    EmotionCategory.SADNESS -> Icons.Outlined.MoodBad
    EmotionCategory.SURPRISE -> Icons.Outlined.Bolt
    EmotionCategory.CALM -> Icons.Outlined.Spa
    EmotionCategory.NEUTRAL -> Icons.Outlined.Psychology
}

@Composable
fun EmotionBadgeChip(
    emotionLabel: String,
    emotionCategory: String,
    modifier: Modifier = Modifier
) {
    val category = EmotionCategory.fromKey(emotionCategory)
    val (containerColor, contentColor) = emotionColorsFor(category)
    val icon = emotionIconFor(category)

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = emotionLabel,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = emotionLabel.ifBlank { category.russianDefault },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NpcEmotionCard(
    npc: NpcEmotionSnapshot,
    universe: String,
    palette: UniverseVisualPalette,
    npcRelation: NpcRelation? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!npc.hasActiveNpc || npc.name.isBlank()) return

    val category = EmotionCategory.fromKey(npc.emotionCategory)
    val (containerColor, contentColor) = emotionColorsFor(category)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f)),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag("npc_emotion_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NpcPortraitAvatar(
                    name = npc.name,
                    role = npcRelation?.role.orEmpty(),
                    universe = universe,
                    explicitArchetype = npcRelation?.archetype.orEmpty(),
                    emotionCategory = npc.emotionCategory,
                    intensity = npc.intensity,
                    palette = palette,
                    size = 66.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = npcRelation?.role?.takeIf { it.isNotBlank() } ?: "Собеседник в сцене",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 5-dot emotion intensity meter
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val level = npc.intensity.coerceIn(1L, 5L).toInt()
                            for (idx in 1..5) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx <= level) contentColor else containerColor
                                        )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = npc.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        EmotionBadgeChip(
                            emotionLabel = npc.emotionLabel,
                            emotionCategory = npc.emotionCategory
                        )
                    }

                    if (npc.note.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "«${npc.note}»",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (npcRelation != null) {
                val trust = npcRelation.trustLevel.coerceIn(-100L, 100L)
                val normalizedTrust = (trust + 100L) / 200f
                val trustColor = when {
                    trust >= 15L -> palette.trustPositive
                    trust <= -15L -> palette.trustNegative
                    else -> palette.accentSecondary
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Доверие: ${npcRelation.statusLabel.ifBlank { "Нейтрально" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { normalizedTrust },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = trustColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = if (trust > 0) "+$trust" else "$trust",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = trustColor
                    )
                }

                if (npcRelation.boundaries.isNotBlank()) {
                    Text(
                        text = "Границы: ${npcRelation.boundaries}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (npcRelation.personality.isNotBlank()) {
                    Text(
                        text = "Характер: ${npcRelation.personality}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HudStatusPanel(
    campaign: Campaign,
    stats: ParsedHudStats,
    palette: UniverseVisualPalette,
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable(campaign.id) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(),
        label = "hud_chevron_rotation"
    )
    val healthFraction by animateFloatAsState(
        targetValue = stats.healthPercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(),
        label = "hud_health_fraction"
    )
    val canonFraction by animateFloatAsState(
        targetValue = campaign.canonDeviationPercent.coerceIn(0L, 100L) / 100f,
        animationSpec = spring(),
        label = "hud_canon_fraction"
    )

    val healthColor = when {
        stats.healthPercent >= 70 -> palette.healthHigh
        stats.healthPercent >= 40 -> palette.healthMid
        else -> palette.healthLow
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.28f)),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("hud_status_panel")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact collapsible header strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { isExpanded = !isExpanded }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("hud_expand_toggle")
            ) {
                // Health compact pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Здоровье",
                        tint = healthColor,
                        modifier = Modifier.size(15.dp)
                    )
                    LinearProgressIndicator(
                        progress = { healthFraction },
                        modifier = Modifier
                            .width(44.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = healthColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "${stats.healthPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stats.currencyLabel.isNotBlank()) {
                        Surface(
                            color = palette.accentSecondary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = stats.currencyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Inventory count badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backpack,
                            contentDescription = "Инвентарь",
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${stats.inventoryItems.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Canon deviation compact pill
                Surface(
                    color = palette.accentSecondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Канон",
                            tint = palette.accentSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${campaign.canonDeviationPercent.coerceIn(0L, 100L)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть характеристики" else "Развернуть характеристики",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Health + Resource Card Row
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Состояние: ${stats.healthLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Paid,
                                            contentDescription = "Ресурсы",
                                            tint = palette.accentSecondary,
                                            modifier = Modifier.size(13.dp)
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
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = healthColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // Inventory Visual Chips (interactive!)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Снаряжение и предметы (нажмите для действия)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (stats.inventoryItems.isEmpty()) {
                            Text(
                                text = "Пока пусто",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                stats.inventoryItems.forEach { item ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.32f)),
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable { onItemClick(item) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(start = 5.dp, end = 9.dp, top = 5.dp, bottom = 5.dp)
                                        ) {
                                            ItemPictureTile(
                                                itemName = item,
                                                palette = palette,
                                                size = 24.dp
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Relations + Canon Butterfly Effect Meter
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Отношения: ${stats.relationsSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Эффект бабочки: ${campaign.canonDeviationPercent.coerceIn(0L, 100L)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentSecondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { canonFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = palette.accentSecondary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaperThemeToggleButton(
    themeMode: PaperThemeMode,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (themeMode) {
        PaperThemeMode.DYNAMIC -> Icons.Outlined.AutoAwesome
        PaperThemeMode.LIGHT -> Icons.Outlined.LightMode
        PaperThemeMode.DARK -> Icons.Outlined.DarkMode
        PaperThemeMode.NEON -> Icons.Outlined.Flare
        PaperThemeMode.SYSTEM -> Icons.Outlined.Tonality
    }
    IconButton(
        onClick = onCycleTheme,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("theme_toggle_button")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Тема: ${themeMode.labelRu}"
        )
    }
}
