package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MoodBad
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EmotionCategory
import com.example.data.model.NpcEmotionSnapshot
import com.example.data.repository.PaperThemeMode
import com.example.ui.theme.EmotionColors
import com.example.ui.theme.EmotionVisualPalette
import com.example.ui.theme.LocalIsDarkPaper

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
    modifier: Modifier = Modifier
) {
    if (!npc.hasActiveNpc || npc.name.isBlank()) return

    val category = EmotionCategory.fromKey(npc.emotionCategory)
    val (containerColor, contentColor) = emotionColorsFor(category)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("npc_emotion_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Собеседник в сцене",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = npc.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                EmotionBadgeChip(
                    emotionLabel = npc.emotionLabel,
                    emotionCategory = npc.emotionCategory
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (npc.note.isNotBlank()) {
                    Text(
                        text = npc.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 5-dot emotion intensity meter
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val level = npc.intensity.coerceIn(1L, 5L).toInt()
                    for (idx in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= level) contentColor else containerColor
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HudStatusPanel(
    hudLocation: String,
    hudTime: String,
    hudStatusAndInventory: String,
    hudRelationsAndCanon: String,
    canonDeviationPercent: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hud_status_panel")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = "Локация",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = hudLocation.ifBlank { "Локация неизвестна" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Время",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = hudTime.ifBlank { "Сейчас" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "[$hudLocation | $hudTime | $hudStatusAndInventory | $hudRelationsAndCanon]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Эффект бабочки",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Отклонение от канона: ${canonDeviationPercent.coerceIn(0L, 100L)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
                LinearProgressIndicator(
                    progress = { (canonDeviationPercent.coerceIn(0L, 100L) / 100f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
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
        PaperThemeMode.LIGHT -> Icons.Outlined.LightMode
        PaperThemeMode.DARK -> Icons.Outlined.DarkMode
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
            contentDescription = "Тема бумаги: ${themeMode.labelRu}"
        )
    }
}
