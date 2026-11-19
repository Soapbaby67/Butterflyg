package com.example.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.InventoryItem
import com.example.ui.components.ItemPictureTile
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.viewmodel.TurnSubmissionState

@Composable
internal fun ActionComposerDock(
    draftAction: String,
    isCustomActionRisky: Boolean,
    isBusy: Boolean,
    submissionState: TurnSubmissionState,
    actionNotice: String?,
    inventoryItems: List<InventoryItem>,
    accentPrimary: Color,
    universePalette: UniverseVisualPalette,
    onDraftChange: (String) -> Unit,
    onToggleRiskMode: (Boolean) -> Unit,
    onVoiceInputClick: () -> Unit,
    onSubmit: () -> Unit,
    onRetryFailed: (String) -> Unit,
    onDismissNotice: () -> Unit,
    onItemClick: (InventoryItem) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !actionNotice.isNullOrBlank()) {
                val notice = actionNotice
                if (!notice.isNullOrBlank()) {
                    Surface(
                        color = accentPrimary.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, accentPrimary.copy(alpha = 0.36f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(bottom = 8.dp)
                            .testTag("session_action_notice")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissNotice,
                                modifier = Modifier.size(32.dp)
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
            }

            AnimatedVisibility(visible = submissionState is TurnSubmissionState.Error) {
                val err = submissionState as? TurnSubmissionState.Error
                if (err != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = err.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(
                                onClick = { onRetryFailed(err.failedAction) },
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Повторить",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Real-time interactive Visual Inventory Tray (structured)
            if (inventoryItems.isNotEmpty() && !isBusy) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(bottom = 8.dp)
                        .testTag("quick_inventory_tray"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(inventoryItems, key = { it.resolvedId() }) { item ->
                        Surface(
                            color = if (item.isEquipped) {
                                universePalette.healthHigh.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                            },
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                width = if (item.isEquipped) 1.3.dp else 1.dp,
                                color = if (item.isEquipped) {
                                    universePalette.healthHigh.copy(alpha = 0.65f)
                                } else {
                                    accentPrimary.copy(alpha = 0.36f)
                                }
                            ),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onItemClick(item) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                ItemPictureTile(
                                    itemName = item.name,
                                    palette = universePalette,
                                    size = 26.dp
                                )
                                val qtySuffix = if (item.quantity > 1L) " ×${item.quantity}" else ""
                                Text(
                                    text = "${item.name}$qtySuffix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.isEquipped) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(universePalette.healthHigh)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isCustomActionRisky) {
                Text(
                    text = "Режим рулетки: при отправке появятся возможные развилки исхода с учётом навыков",
                    style = MaterialTheme.typography.labelSmall,
                    color = universePalette.choiceForceColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(bottom = 6.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                OutlinedTextField(
                    value = draftAction,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.custom_action_hint)) },
                    enabled = !isBusy,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draftAction.isNotBlank() && !isBusy) {
                                onSubmit()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_action_input")
                )

                FilledTonalIconButton(
                    onClick = onVoiceInputClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Надиктовать действие голосом",
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledTonalIconToggleButton(
                    checked = isCustomActionRisky,
                    onCheckedChange = onToggleRiskMode,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("custom_action_risk_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Casino,
                        contentDescription = if (isCustomActionRisky) {
                            "Выключить рулетку риска"
                        } else {
                            "Сделать ход рискованным (показать развилки)"
                        },
                        tint = if (isCustomActionRisky) {
                            universePalette.choiceForceColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onSubmit,
                    enabled = draftAction.isNotBlank() && !isBusy,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isCustomActionRisky) {
                            universePalette.choiceForceColor
                        } else {
                            accentPrimary
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_custom_action_button")
                ) {
                    Icon(
                        imageVector = if (isCustomActionRisky) {
                            Icons.Outlined.Casino
                        } else {
                            Icons.AutoMirrored.Filled.Send
                        },
                        contentDescription = stringResource(R.string.send_action)
                    )
                }
            }
        }
    }
}
