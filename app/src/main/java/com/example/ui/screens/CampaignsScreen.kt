package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.LocalRpgAudioEngine
import com.example.core.SfxEvent
import com.example.core.rememberRpgHaptics
import com.example.data.model.Campaign
import com.example.data.model.ScenarioGenre
import com.example.data.model.UniversePresets
import com.example.data.model.UniverseTemplate
import com.example.data.repository.PaperThemeMode
import com.example.ui.components.EmotionBadgeChip
import com.example.ui.components.HudStatsParser
import com.example.ui.components.NpcPortraitAvatar
import com.example.ui.components.PaperThemeToggleButton
import com.example.ui.components.SoundToggleButton
import com.example.ui.theme.ChroniclesDynamicBackdrop
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.rememberUniverseVisualPalette
import com.example.ui.theme.resolveSceneBackdropRes
import com.example.ui.viewmodel.CampaignsViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignsScreen(
    viewModel: CampaignsViewModel,
    userDisplayName: String,
    themeMode: PaperThemeMode,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    onCycleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPresentation: () -> Unit,
    onSignOut: () -> Unit,
    onCreateNewCampaign: (templateId: String?) -> Unit,
    onOpenCampaign: (campaignId: String) -> Unit
) {
    val campaignsState by viewModel.campaignsState.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    var campaignToDelete by remember { mutableStateOf<Campaign?>(null) }
    var selectedGenre by remember { mutableStateOf(ScenarioGenre.ALL) }
    val haptics = rememberRpgHaptics(hapticsEnabled)
    val audio = LocalRpgAudioEngine.current

    val filteredTemplates = remember(selectedGenre) {
        if (selectedGenre == ScenarioGenre.ALL) {
            UniversePresets.templates
        } else {
            UniversePresets.templates.filter { it.genre == selectedGenre }
        }
    }

    ChroniclesDynamicBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.shelf_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (userDisplayName.isNotBlank()) {
                            Text(
                                text = userDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    SoundToggleButton(
                        soundEnabled = soundEnabled,
                        onToggleSound = {
                            haptics.tap()
                            audio?.playSfx(SfxEvent.UI_TAP)
                            onToggleSound()
                        }
                    )
                    PaperThemeToggleButton(
                        themeMode = themeMode,
                        onCycleTheme = {
                            haptics.tap()
                            audio?.playSfx(SfxEvent.UI_TAP)
                            onCycleTheme()
                        }
                    )
                    IconButton(
                        onClick = {
                            haptics.tap()
                            audio?.playSfx(SfxEvent.UI_TAP)
                            onOpenPresentation()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_presentation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Презентация приложения"
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.tap()
                            audio?.playSfx(SfxEvent.UI_TAP)
                            onOpenSettings()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("shelf_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Настройки чтения и ИИ"
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Выйти из аккаунта"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.tap()
                    onCreateNewCampaign(null)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.new_chronicle),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.testTag("new_chronicle_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!deleteError.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = deleteError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearDeleteError() }) {
                                    Text("Скрыть")
                                }
                            }
                        }
                    }
                }

                // Quick-start universe presets with genre filter + visual world covers
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Сценарии и вселенные",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ScenarioGenre.entries, key = { it.name }) { genre ->
                                FilterChip(
                                    selected = selectedGenre == genre,
                                    onClick = {
                                        haptics.tap()
                                        selectedGenre = genre
                                    },
                                    label = { Text(genre.labelRu) },
                                    modifier = Modifier.testTag("genre_filter_${genre.name.lowercase()}")
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTemplates, key = { it.id }) { template ->
                                UniverseTemplateCard(
                                    template = template,
                                    onClick = {
                                        haptics.tap()
                                        onCreateNewCampaign(template.id)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Text(
                        text = "Ваши кампании",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                when (val state = campaignsState) {
                    UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                EmptyShelfCard(
                                    onStartCustom = {
                                        haptics.tap()
                                        onCreateNewCampaign(null)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(state.data, key = { it.id }) { campaign ->
                                CampaignSlotCard(
                                    campaign = campaign,
                                    onOpen = {
                                        haptics.tap()
                                        onOpenCampaign(campaign.id)
                                    },
                                    onRequestDelete = { campaignToDelete = campaign },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    val targetDelete = campaignToDelete
    if (targetDelete != null) {
        AlertDialog(
            onDismissRequest = { campaignToDelete = null },
            title = {
                Text(
                    text = "Удалить хронику?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Кампания «${targetDelete.title}» и вся история её ходов будут удалены безвозвратно.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCampaign(targetDelete.id)
                        campaignToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { campaignToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun UniverseTemplateCard(
    template: UniverseTemplate,
    onClick: () -> Unit
) {
    val palette = rememberUniverseVisualPalette(template.universe)
    val coverRes = remember(template.id) {
        resolveSceneBackdropRes(
            universe = template.universe,
            location = template.startingPoint,
            weatherMood = WeatherMood.DAY
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.30f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .width(232.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("template_card_${template.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                Image(
                    painter = painterResource(id = coverRes),
                    contentDescription = template.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.16f),
                                    palette.atmosphericTint.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.56f),
                    contentColor = Color(0xFFF6EFE4),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = template.badge,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = template.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CampaignSlotCard(
    campaign: Campaign,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberUniverseVisualPalette(campaign.universe)
    val parsedStats = remember(
        campaign.hudStatusAndInventory,
        campaign.hudRelationsAndCanon,
        campaign.hudTime,
        campaign.hudLocation,
        campaign.inventory,
        campaign.universe
    ) {
        HudStatsParser.parse(campaign)
    }
    val coverRes = remember(campaign.id, campaign.universe, campaign.hudLocation, parsedStats.weatherMood) {
        resolveSceneBackdropRes(
            universe = campaign.universe,
            location = campaign.hudLocation,
            weatherMood = parsedStats.weatherMood
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.30f)),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("campaign_card_${campaign.id}")
    ) {
        Column {
            // World Cover Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            ) {
                Image(
                    painter = painterResource(id = coverRes),
                    contentDescription = campaign.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    palette.bannerScrimTop,
                                    palette.bannerScrimBottom
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "Ход ${campaign.turnCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6EFE4),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_campaign_${campaign.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Удалить хронику",
                            tint = Color(0xFFF6EFE4)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = campaign.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFFFBF4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = campaign.universe,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE7DFD1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (campaign.activeNpc.hasActiveNpc && campaign.activeNpc.name.isNotBlank()) {
                        val npcRel = campaign.npcs.firstOrNull {
                            it.name.equals(campaign.activeNpc.name, ignoreCase = true)
                        }
                        NpcPortraitAvatar(
                            name = campaign.activeNpc.name,
                            role = npcRel?.role.orEmpty(),
                            universe = campaign.universe,
                            explicitArchetype = npcRel?.archetype.orEmpty(),
                            emotionCategory = campaign.activeNpc.emotionCategory,
                            intensity = campaign.activeNpc.intensity,
                            palette = palette,
                            size = 34.dp
                        )
                        EmotionBadgeChip(
                            emotionLabel = "${campaign.activeNpc.name}: ${campaign.activeNpc.emotionLabel}",
                            emotionCategory = campaign.activeNpc.emotionCategory,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Здоровье",
                                tint = palette.healthHigh,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${parsedStats.healthPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        color = palette.accentSecondary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Канон: ${campaign.canonDeviationPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (campaign.hudLocation.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${campaign.hudLocation} • ${campaign.hudTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyShelfCard(
    onStartCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onStartCustom)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_chronicle_bookplate),
                contentDescription = "Пустая полка хроник",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Полка хроник пока пуста",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Выберите готовую вселенную выше или нажмите «Новая хроника», чтобы ответить на 4 вопроса мастера и открыть первую сцену.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
