package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.InventoryItem
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.viewmodel.ItemInspectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionSheet(
    item: InventoryItem,
    activeNpcName: String?,
    palette: UniverseVisualPalette,
    isBusy: Boolean,
    inspectionState: ItemInspectionState,
    onDismiss: () -> Unit,
    onUseInScene: (String) -> Unit,
    onToggleEquip: (InventoryItem) -> Unit,
    onInspectItem: (InventoryItem) -> Unit,
    onGiveToNpc: (String, String) -> Unit,
    onInsertIntoDraft: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val visualSpec = remember(item.name) { resolveItemVisualSpec(item.name) }
    val category = item.resolvedCategory()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ItemPictureTile(
                    itemName = item.name,
                    palette = palette,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.labelRu,
                            style = MaterialTheme.typography.labelMedium,
                            color = visualSpec.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.quantity > 1L) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "×${item.quantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.isEquipped) {
                            Surface(
                                color = palette.healthHigh.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, palette.healthHigh.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "Экипировано",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.healthHigh,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.description.ifBlank { visualSpec.kind.defaultHint },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Inline AI Inspection Result (does not spend a turn)
            when (inspectionState) {
                is ItemInspectionState.Loading -> if (inspectionState.itemName == item.name) {
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = palette.accentPrimary
                            )
                            Text(
                                text = "Мастер осматривает детали предмета…",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentPrimary
                            )
                        }
                    }
                }
                is ItemInspectionState.Result -> if (inspectionState.itemName == item.name) {
                    Surface(
                        color = palette.accentSecondary.copy(alpha = 0.10f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Осмотр (без траты хода)",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = inspectionState.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is ItemInspectionState.Error -> if (inspectionState.itemName == item.name) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = inspectionState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                ItemInspectionState.Idle -> Unit
            }

            // 1. Use in scene (spends a turn)
            Button(
                onClick = {
                    onUseInScene(item.name)
                    onDismiss()
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("item_use_in_scene_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Использовать в сцене")
            }

            // 2 & 3. Equip/Unequip & Inspect without spending a turn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onToggleEquip(item) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("item_equip_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.isEquipped) "Снять" else "Экипировать")
                }

                OutlinedButton(
                    onClick = { onInspectItem(item) },
                    enabled = inspectionState !is ItemInspectionState.Loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("item_inspect_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Осмотреть")
                }
            }

            if (!activeNpcName.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = {
                        onGiveToNpc(item.name, activeNpcName)
                        onDismiss()
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("item_give_npc_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Показать / передать: $activeNpcName")
                }
            }

            OutlinedButton(
                onClick = {
                    onInsertIntoDraft(item.name)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("item_draft_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Вставить в свой ход и дописать")
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
