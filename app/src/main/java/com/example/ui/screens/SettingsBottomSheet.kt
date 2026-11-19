package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalRpgAudioEngine
import com.example.core.SfxEvent
import com.example.data.repository.AiModelChoice
import com.example.data.repository.NarrativePace
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle
import com.example.data.repository.UserAppSettings
import com.example.ui.theme.readingFontFamilyFor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    settings: UserAppSettings,
    onSelectTheme: (PaperThemeMode) -> Unit,
    onSelectFontStyle: (ReadingFontStyle) -> Unit,
    onSelectFontScale: (ReadingFontScale) -> Unit,
    onSelectAiModel: (AiModelChoice) -> Unit,
    onSelectNarrativePace: (NarrativePace) -> Unit = {},
    onToggleLiveStreaming: (Boolean) -> Unit,
    onToggleAutoReadTts: (Boolean) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit = {},
    onOstVolumeChange: (Float) -> Unit = {},
    onSfxVolumeChange: (Float) -> Unit = {},
    activeSoundscapeLabel: String = "",
    onOpenPresentation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audio = LocalRpgAudioEngine.current

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

            // 1. App Theme (Dynamic Gradient default + Paper/Neon modes)
            SettingsSectionHeader(title = "Тема оформления")
            Text(
                text = "«Динамический градиент» подстраивает цвета под раздел приложения, вселенную и настроение текущей сцены.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperThemeMode.entries.forEach { mode ->
                    val swatchColors = when (mode) {
                        PaperThemeMode.DYNAMIC -> listOf(Color(0xFFE58C8A), Color(0xFFDFB268), Color(0xFF4BE1F2))
                        PaperThemeMode.LIGHT -> listOf(Color(0xFFFAF6EE), Color(0xFF7C2D32))
                        PaperThemeMode.DARK -> listOf(Color(0xFF1C1A17), Color(0xFFD4757B))
                        PaperThemeMode.NEON -> listOf(Color(0xFF0A0E1B), Color(0xFF4BE1F2), Color(0xFFF26D9A))
                        PaperThemeMode.SYSTEM -> listOf(Color(0xFFFAF6EE), Color(0xFF1C1A17))
                    }
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onSelectTheme(mode) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(swatchColors))
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

            // 3b. AI Narrative Pace Selector
            SettingsSectionHeader(title = "Темп повествования ИИ")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NarrativePace.entries.forEach { pace ->
                    val selected = settings.narrativePace == pace
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectNarrativePace(pace) }
                            .testTag("settings_pace_${pace.key}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pace.labelRu,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pace.subtitleRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. Streaming, Sound & Haptics
            SettingsSectionHeader(title = "Саундтрек и звуки")
            if (activeSoundscapeLabel.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Сейчас звучит: $activeSoundscapeLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            SettingsSwitchRow(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                title = "Звук, OST и атмосфера",
                subtitle = "Динамическая музыка, звуки погоды, эмоций и интерфейса",
                checked = settings.soundEnabled,
                onCheckedChange = onToggleSound,
                testTag = "settings_sound_switch"
            )
            AnimatedVisibility(visible = settings.soundEnabled) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                                    imageVector = Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Громкость OST",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = "${(settings.ostVolume * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.ostVolume,
                            onValueChange = onOstVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("settings_ost_slider")
                        )

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
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Погода и эффекты (SFX)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = "${(settings.sfxVolume * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = settings.sfxVolume,
                            onValueChange = onSfxVolumeChange,
                            onValueChangeFinished = { audio?.playSfx(SfxEvent.UI_TAP) },
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("settings_sfx_slider")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 5. Streaming & Haptics Toggles
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
                icon = Icons.Outlined.RecordVoiceOver,
                title = "Авточтение новых ответов (TTS)",
                subtitle = "Автоматически озвучивать новые сцены мастером, приглушая OST",
                checked = settings.autoReadTtsEnabled,
                onCheckedChange = onToggleAutoReadTts,
                testTag = "settings_tts_autoread_switch"
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
