package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementCategory
import com.example.data.model.AchievementStatus
import com.example.data.model.ChronicleAchievementDef
import com.example.data.model.SkillLevelUpEvent
import com.example.ui.theme.UniverseVisualPalette

/**
 * Ex-libris style achievement & chronicle milestones showcase rendered purely on-device.
 */
@Composable
fun ChronicleAchievementsTab(
    statuses: List<AchievementStatus>,
    palette: UniverseVisualPalette,
    modifier: Modifier = Modifier
) {
    val unlockedCount = statuses.count { it.isUnlocked }
    val totalCount = statuses.size.coerceAtLeast(1)

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("codex_achievements_tab")
    ) {
        item {
            Surface(
                color = palette.accentPrimary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.36f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Вехи хроники",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$unlockedCount из $totalCount",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { unlockedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = palette.accentSecondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Достижения открываются автоматически по мере ваших решений, роста навыков и поворотов сюжета.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val sorted = statuses.sortedByDescending { it.isUnlocked }
        items(sorted, key = { it.def.id }) { status ->
            AchievementExLibrisCard(
                status = status,
                palette = palette
            )
        }
    }
}

@Composable
private fun AchievementExLibrisCard(
    status: AchievementStatus,
    palette: UniverseVisualPalette
) {
    val accent = when (status.def.category) {
        AchievementCategory.STORY -> palette.accentPrimary
        AchievementCategory.RISK -> palette.choiceForceColor
        AchievementCategory.MASTERY -> palette.choiceWildcardColor
        AchievementCategory.BONDS -> palette.choiceDiplomacyColor
        AchievementCategory.EXPLORATION -> palette.accentSecondary
    }
    val emblemColor = if (status.isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Surface(
        color = if (status.isUnlocked) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (status.isUnlocked) 1.3.dp else 1.dp,
            color = if (status.isUnlocked) accent.copy(alpha = 0.52f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_card_${status.def.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vector Ex-Libris Medallion
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (status.isUnlocked) {
                            accent.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (status.isUnlocked) {
                        achievementIconFor(status.def.id)
                    } else {
                        Icons.Outlined.Lock
                    },
                    contentDescription = status.def.titleRu,
                    tint = emblemColor,
                    modifier = Modifier.size(22.dp)
                )
            }

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
                        text = status.def.titleRu,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (status.isUnlocked) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (status.isUnlocked) {
                            status.unlockedAtTurn?.let { "Ход $it" } ?: "Открыто"
                        } else {
                            "${status.currentProgress}/${status.targetProgress}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (status.isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = status.def.descriptionRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!status.isUnlocked && status.targetProgress > 1) {
                    LinearProgressIndicator(
                        progress = { status.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent.copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SkillLevelUpBanner(
    event: SkillLevelUpEvent,
    accentColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.18f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("skill_level_up_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.TrendingUp,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Навык вырос: ${event.skill.labelRu} → уровень ${event.newLevel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Повышение за опыт действий (+${event.xpEarned} XP). Шансы в проверках увеличены.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Скрыть уведомление",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AchievementUnlockedBanner(
    achievement: ChronicleAchievementDef,
    accentColor: Color,
    onOpenMilestones: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.16f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.52f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenMilestones)
            .testTag("achievement_unlocked_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Новая веха: ${achievement.titleRu}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = achievement.descriptionRu,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Скрыть веху",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun achievementIconFor(id: String): ImageVector = when (id) {
    "first_butterfly" -> Icons.Outlined.AutoAwesome
    "point_of_no_return" -> Icons.Outlined.LocalFireDepartment
    "cool_headed_risk" -> Icons.Outlined.Casino
    "fate_tempered" -> Icons.Outlined.Shield
    "path_of_mastery" -> Icons.Outlined.TrendingUp
    "peak_craft" -> Icons.Outlined.MilitaryTech
    "true_ally" -> Icons.Outlined.Favorite
    "shadow_of_enmity" -> Icons.Outlined.Whatshot
    "chronicler_10" -> Icons.Outlined.AutoStories
    "tome_keeper_25" -> Icons.Outlined.MenuBook
    "crossroads_of_worlds" -> Icons.Outlined.CallSplit
    "marked_pages" -> Icons.Outlined.Bookmark
    else -> Icons.Outlined.EmojiEvents
}
