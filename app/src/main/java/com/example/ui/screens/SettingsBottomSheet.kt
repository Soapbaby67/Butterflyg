package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiModelChoice
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle
import com.example.data.repository.UserAppSettings
import com.example.ui.theme.readingFontFamilyFor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    settings: UserAppSettings,
    onSelectTheme: (PaperThemeMode) -> Unit,
    onSelectFontStyle: (ReadingFontStyle) -> Unit,
    onSelectFontScale: (ReadingFontScale) -> Unit,
    onSelectAiModel: (AiModelChoice) -> Unit,
    onToggleLiveStreaming: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onOpenPresentation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройки чтения и ИИ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("settings_sheet_title")
            )

            // 1. Paper Theme
            SettingsSectionHeader(title = "Тема бумаги")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperThemeMode.entries.forEach { mode ->
                    val icon = when (mode) {
                        PaperThemeMode.SYSTEM -> Icons.Outlined.Tonality
                        PaperThemeMode.LIGHT -> Icons.Outlined.LightMode
                        PaperThemeMode.DARK -> Icons.Outlined.DarkMode
                    }
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onSelectTheme(mode) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(mode.labelRu) },
                        modifier = Modifier.testTag("settings_theme_${mode.key}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Font Customization (Family + Size + Live Preview)
            SettingsSectionHeader(title = "Шрифт повествования")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingFontStyle.entries.forEach { style ->
                    val family = readingFontFamilyFor(style)
                    FilterChip(
                        selected = settings.fontStyle == style,
                        onClick = { onSelectFontStyle(style) },
                        label = {
                            Text(
                                text = style.labelRu,
                                fontFamily = family
                            )
                        },
                        modifier = Modifier.testTag("settings_font_${style.key}")
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Размер текста",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingFontScale.entries.forEach { scale ->
                    FilterChip(
                        selected = settings.fontScale == scale,
                        onClick = { onSelectFontScale(scale) },
                        label = { Text(scale.labelRu) },
                        modifier = Modifier.testTag("settings_size_${scale.key}")
                    )
                }
            }

            // Live Russian Preview Card
            val previewFamily = readingFontFamilyFor(settings.fontStyle)
            val previewSize = (16f * settings.fontScale.multiplier).sp
            val previewLineHeight = (24.5f * settings.fontScale.multiplier).sp
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${settings.fontStyle.labelRu} • ${settings.fontStyle.subtitleRu}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "«Дождь тихо барабанил по стеклу кофейни, пока собеседник медленно переворачивал страницу потёртого блокнота…»",
                        fontFamily = previewFamily,
                        fontSize = previewSize,
                        lineHeight = previewLineHeight,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. Free-Tier AI Model Selection
            SettingsSectionHeader(title = "Модель ИИ-мастера")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiModelChoice.entries.forEach { model ->
                    val selected = settings.aiModel == model
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectAiModel(model) }
                            .testTag("settings_model_${model.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.titleRu,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = model.subtitleRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. Streaming & Haptics Toggles
            SettingsSectionHeader(title = "Погружение и комфорт")
            SettingsSwitchRow(
                icon = Icons.Outlined.Speed,
                title = "Живой стриминг текста",
                subtitle = "Показывать текст сцены по мере ответа мастера",
                checked = settings.liveStreamingEnabled,
                onCheckedChange = onToggleLiveStreaming,
                testTag = "settings_streaming_switch"
            )
            SettingsSwitchRow(
                icon = Icons.Outlined.Vibration,
                title = "Тактильная вибрация",
                subtitle = "Лёгкий отклик на выбор, предметы и эффект бабочки",
                checked = settings.hapticsEnabled,
                onCheckedChange = onToggleHaptics,
                testTag = "settings_haptics_switch"
            )

            if (onOpenPresentation != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onOpenPresentation()
                        }
                        .testTag("settings_open_presentation_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Презентация приложения",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "9 слайдов: экраны, механики NPC и план развития",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
