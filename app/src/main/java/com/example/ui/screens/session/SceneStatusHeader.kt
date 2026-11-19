package com.example.ui.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.weatherIconFor
import com.example.ui.theme.WeatherMood

@Composable
internal fun SwipeNavigationStrip(
    currentPage: Int,
    accentColor: Color,
    secondaryColor: Color,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwipeTabPill(
            label = "Статус и вещи",
            selected = currentPage == 0,
            accentColor = secondaryColor,
            leadingIcon = Icons.Outlined.ChevronLeft,
            trailingIcon = null,
            onClick = { onSelectPage(0) },
            modifier = Modifier
                .weight(1f)
                .testTag("swipe_tab_stats")
        )
        SwipeTabPill(
            label = "Сцена",
            selected = currentPage == 1,
            accentColor = accentColor,
            leadingIcon = null,
            trailingIcon = null,
            onClick = { onSelectPage(1) },
            modifier = Modifier
                .weight(0.75f)
                .testTag("swipe_tab_scene")
        )
        SwipeTabPill(
            label = "Кодекс",
            selected = currentPage == 2,
            accentColor = accentColor,
            leadingIcon = null,
            trailingIcon = Icons.Outlined.ChevronRight,
            onClick = { onSelectPage(2) },
            modifier = Modifier
                .weight(1f)
                .testTag("swipe_tab_codex")
        )
    }
}

@Composable
private fun SwipeTabPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            accentColor.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
internal fun SceneStatusBarStrip(
    location: String,
    timeLabel: String,
    weatherMood: WeatherMood,
    healthPercent: Int,
    accentColor: Color,
    secondaryColor: Color,
    onInspectScene: () -> Unit,
    onOpenStats: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.34f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onInspectScene)
                .testTag("scene_atmosphere_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = weatherIconFor(weatherMood),
                    contentDescription = weatherMood.russianLabel,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = location.ifBlank { "Текущая сцена" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (timeLabel.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.48f)),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenStats)
                .testTag("hud_expand_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Статус",
                    tint = secondaryColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Статус $healthPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
