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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.UniverseVisualPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionSheet(
    itemName: String,
    activeNpcName: String?,
    palette: UniverseVisualPalette,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onUseInScene: (String) -> Unit,
    onGiveToNpc: (String, String) -> Unit,
    onInsertIntoDraft: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (categoryTitle, inspectionHint, icon) = describeInventoryItem(itemName)

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = palette.accentPrimary.copy(alpha = 0.14f),
                    contentColor = palette.accentPrimary,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = itemName,
                        modifier = Modifier.padding(11.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentPrimary
                    )
                    Text(
                        text = itemName,
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
                    text = inspectionHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = {
                    onUseInScene(itemName)
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
                Text("Применить в текущей сцене")
            }

            if (!activeNpcName.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = {
                        onGiveToNpc(itemName, activeNpcName)
                        onDismiss()
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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
                    onInsertIntoDraft(itemName)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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

fun itemCategoryIcon(item: String): ImageVector = describeInventoryItem(item).third

private fun describeInventoryItem(item: String): Triple<String, String, ImageVector> {
    val lower = item.lowercase()
    return when {
        lower.contains("клин") || lower.contains("стилет") || lower.contains("меч") ||
            lower.contains("нож") || lower.contains("пистол") || lower.contains("брон") ||
            lower.contains("щит") || lower.contains("палоч") || lower.contains("куна") ->
            Triple(
                "Снаряжение / защита",
                "Можно пустить в ход при угрозе, вскрыть преграду или намекнуть на готовность защищаться.",
                Icons.Outlined.Security
            )
        lower.contains("алхим") || lower.contains("зель") || lower.contains("эликсир") ||
            lower.contains("аптеч") || lower.contains("лекар") || lower.contains("трав") ||
            lower.contains("мазь") ->
            Triple(
                "Медицина / эликсир",
                "Поможет восстановить силы, снять усталость или помочь раненому персонажу рядом.",
                Icons.Outlined.Science
            )
        lower.contains("кофе") || lower.contains("чай") || lower.contains("пирог") ||
            lower.contains("булоч") || lower.contains("круж") || lower.contains("плед") ||
            lower.contains("зонт") || lower.contains("цвет") ->
            Triple(
                "Уютная деталь / жест внимания",
                "Отличный способ сменить тон беседы, проявить заботу и сблизиться с собеседником.",
                Icons.Outlined.LocalCafe
            )
        lower.contains("ключ") || lower.contains("пропуск") || lower.contains("печать") ||
            lower.contains("чип") || lower.contains("карт") ->
            Triple(
                "Ключ / допуск",
                "Открывает закрытые помещения, архивы, служебные отсеки или подтверждает ваши полномочия.",
                Icons.Outlined.Key
            )
        lower.contains("книг") || lower.contains("свиток") || lower.contains("блокнот") ||
            lower.contains("запис") || lower.contains("дневник") || lower.contains("письм") ||
            lower.contains("конспект") ->
            Triple(
                "Записи / артефакт",
                "Хранит сведения, подсказки или личные заметки — удобно изучить или показать нужному человеку.",
                Icons.AutoMirrored.Outlined.MenuBook
            )
        else ->
            Triple(
                "Предмет инвентаря",
                "Предмет при вас: примените его напрямую в сцене, передайте собеседнику или впишите в собственный ход.",
                Icons.Outlined.Backpack
            )
    }
}
