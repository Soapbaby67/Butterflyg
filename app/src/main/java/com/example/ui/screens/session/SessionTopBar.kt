package com.example.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionTopBar(
    title: String,
    subtitle: String?,
    currentPage: Int,
    hasSessionData: Boolean,
    canRollback: Boolean,
    isBusy: Boolean,
    focusReadingMode: Boolean,
    bookmarksCount: Int,
    topBarContainerColor: Color,
    accentPrimary: Color,
    onBackClick: () -> Unit,
    onToggleFocusReading: () -> Unit,
    onOpenChapterSearch: () -> Unit,
    onRollbackClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarContainerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("session_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (currentPage != 1) {
                        "Вернуться к сцене"
                    } else {
                        "Назад к полке хроник"
                    }
                )
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            if (hasSessionData) {
                IconButton(
                    onClick = onToggleFocusReading,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_focus_reading_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = if (focusReadingMode) {
                            "Выйти из режима чтения"
                        } else {
                            "Компактный режим чтения"
                        },
                        tint = if (focusReadingMode) {
                            accentPrimary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
                IconButton(
                    onClick = onOpenChapterSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_chapter_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Поиск по главам и закладки ($bookmarksCount)",
                        tint = if (bookmarksCount > 0) {
                            accentPrimary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
                if (canRollback && !focusReadingMode) {
                    IconButton(
                        onClick = onRollbackClick,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rollback_turn_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Откатить последний ход",
                            tint = if (isBusy) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("session_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Настройки чтения, тем, шрифтов и ИИ"
                )
            }
        }
    )
}
