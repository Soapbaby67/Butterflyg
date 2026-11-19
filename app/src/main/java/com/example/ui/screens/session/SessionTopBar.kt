package com.example.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.EditNote
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
import com.example.ui.components.SoundToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionTopBar(
    title: String,
    subtitle: String?,
    currentPage: Int,
    codexTab: Int,
    hasSessionData: Boolean,
    pinnedFactsCount: Int,
    canRollback: Boolean,
    isBusy: Boolean,
    soundEnabled: Boolean,
    topBarContainerColor: Color,
    accentPrimary: Color,
    onBackClick: () -> Unit,
    onRollbackClick: () -> Unit,
    onFactsClick: () -> Unit,
    onToggleSound: () -> Unit,
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
                if (canRollback) {
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
                IconButton(
                    onClick = onFactsClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_facts_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Блокнот фактов ($pinnedFactsCount)",
                        tint = if (currentPage == 2 && codexTab == 3) {
                            accentPrimary
                        } else if (pinnedFactsCount > 0) {
                            accentPrimary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }
            SoundToggleButton(
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("session_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Настройки чтения и ИИ"
                )
            }
        }
    )
}
