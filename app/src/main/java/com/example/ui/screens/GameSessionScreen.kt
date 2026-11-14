package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.rememberRpgHaptics
import com.example.data.model.CampaignTurn
import com.example.data.model.EmotionCategory
import com.example.data.model.TurnChoice
import com.example.ui.components.HudStatsParser
import com.example.ui.components.ItemActionSheet
import com.example.ui.components.ItemPictureTile
import com.example.ui.components.NpcEmotionCard
import com.example.ui.components.StatsAndInventorySheet
import com.example.ui.components.WeatherCanvasOverlay
import com.example.ui.components.weatherIconFor
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.ChroniclesDynamicBackdrop
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.LocalDynamicGradientSpec
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.rememberUniverseVisualPalette
import com.example.ui.theme.resolveSceneBackdropRes
import com.example.ui.viewmodel.GameSessionData
import com.example.ui.viewmodel.GameSessionViewModel
import com.example.ui.viewmodel.TurnSubmissionState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSessionScreen(
    viewModel: GameSessionViewModel,
    hapticsEnabled: Boolean,
    onDynamicContextChange: (DynamicThemeContext) -> Unit = {},
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val facts by viewModel.facts.collectAsStateWithLifecycle()
    val haptics = rememberRpgHaptics(hapticsEnabled)

    var customActionText by rememberSaveable { mutableStateOf("") }
    var showCodex by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var showHistoryTurns by rememberSaveable { mutableStateOf(false) }
    var codexInitialTab by remember { mutableIntStateOf(0) }
    var selectedItemForAction by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val successData = (sessionState as? UiState.Success<GameSessionData>)?.data
    val universePalette = rememberUniverseVisualPalette(
        universe = successData?.campaign?.universe.orEmpty()
    )
    val parsedStats = remember(
        successData?.campaign?.hudStatusAndInventory,
        successData?.campaign?.hudRelationsAndCanon,
        successData?.campaign?.hudTime,
        successData?.campaign?.hudLocation,
        successData?.campaign?.inventory,
        successData?.campaign?.universe
    ) {
        successData?.campaign?.let { HudStatsParser.parse(it) }
    }

    val activeEmotion = remember(successData?.campaign?.activeNpc?.emotionCategory) {
        EmotionCategory.fromKey(successData?.campaign?.activeNpc?.emotionCategory.orEmpty())
    }

    // Broadcast live story mood, weather, universe, and NPC emotion to root DynamicGradientEngine
    LaunchedEffect(
        successData?.campaign?.universe,
        parsedStats?.weatherMood,
        activeEmotion
    ) {
        val campaign = successData?.campaign
        onDynamicContextChange(
            DynamicThemeContext(
                location = AppVisualLocation.GAME,
                universe = campaign?.universe.orEmpty(),
                weatherMood = parsedStats?.weatherMood ?: WeatherMood.DAY,
                activeEmotion = activeEmotion
            )
        )
    }

    // Haptic feedback when new turn arrives or butterfly event triggers
    var lastTurnCount by rememberSaveable(successData?.campaign?.id) {
        mutableStateOf<Long?>(null)
    }
    var lastButterflyCount by rememberSaveable(successData?.campaign?.id) {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(successData?.campaign?.turnCount, successData?.campaign?.butterflyEvents?.size) {
        val campaign = successData?.campaign ?: return@LaunchedEffect
        val prevTurns = lastTurnCount
        val prevButterflies = lastButterflyCount
        if (prevTurns != null && prevButterflies != null) {
            if (campaign.butterflyEvents.size > prevButterflies) {
                haptics.butterflyShift()
            } else if (campaign.turnCount > prevTurns) {
                haptics.impactConfirm()
            }
        }
        lastTurnCount = campaign.turnCount
        lastButterflyCount = campaign.butterflyEvents.size
    }

    LaunchedEffect(successData?.turns?.size, submissionState is TurnSubmissionState.Generating) {
        if ((successData?.turns?.size ?: 0) > 0) {
            listState.animateScrollToItem(index = 0)
        }
    }

    val dynamicSpec = LocalDynamicGradientSpec.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed live scene background reflecting current universe, location, weather, and mood
        if (successData != null && parsedStats != null) {
            val backdropRes = resolveSceneBackdropRes(
                universe = successData.campaign.universe,
                location = successData.campaign.hudLocation,
                weatherMood = parsedStats.weatherMood
            )
            Crossfade(
                targetState = backdropRes,
                animationSpec = tween(durationMillis = 700),
                label = "fullscreen_scene_backdrop"
            ) { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Фон текущей сцены",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dynamicSpec.topColor.copy(alpha = 0.62f),
                                dynamicSpec.middleColor.copy(alpha = 0.56f),
                                dynamicSpec.bottomColor.copy(alpha = 0.86f)
                            )
                        )
                    )
            )
            WeatherCanvasOverlay(
                weatherMood = parsedStats.weatherMood,
                accentColor = dynamicSpec.accentPrimary,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ChroniclesDynamicBackdrop(modifier = Modifier.fillMaxSize()) {}
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = dynamicSpec.topColor.copy(alpha = 0.68f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("session_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад к полке хроник"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = successData?.campaign?.title ?: "Хроника",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (successData != null) {
                                Text(
                                    text = "Ход ${successData.campaign.turnCount} • ${successData.campaign.universe}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dynamicSpec.accentPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        if (successData != null) {
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    showStatsSheet = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_stats_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Analytics,
                                    contentDescription = "Открыть статистику и инвентарь",
                                    tint = dynamicSpec.accentSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    codexInitialTab = 3
                                    showCodex = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_facts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = "Блокнот фактов (${facts.count { it.isPinned }})",
                                    tint = if (facts.any { it.isPinned }) {
                                        dynamicSpec.accentPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    codexInitialTab = 0
                                    showCodex = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_codex_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = "Открыть Кодекс хроники"
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                haptics.tap()
                                onOpenSettings()
                            },
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
            },
            bottomBar = {
                if (successData != null) {
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
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = err.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    haptics.tap()
                                                    viewModel.submitPlayerAction(err.failedAction)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Повторить")
                                            }
                                        }
                                    }
                                }
                            }

                            // Real-time interactive Visual Inventory Tray (every item has its own picture!)
                            val inventoryItems = successData.campaign.inventory
                            if (inventoryItems.isNotEmpty() && submissionState !is TurnSubmissionState.Generating) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 680.dp)
                                        .padding(bottom = 8.dp)
                                        .testTag("quick_inventory_tray"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(inventoryItems) { item ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                            shape = MaterialTheme.shapes.medium,
                                            border = BorderStroke(1.dp, dynamicSpec.accentPrimary.copy(alpha = 0.36f)),
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    haptics.tap()
                                                    selectedItemForAction = item
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                                modifier = Modifier.padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                                            ) {
                                                ItemPictureTile(
                                                    itemName = item,
                                                    palette = universePalette,
                                                    size = 26.dp
                                                )
                                                Text(
                                                    text = item,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp)
                            ) {
                                OutlinedTextField(
                                    value = customActionText,
                                    onValueChange = { customActionText = it },
                                    placeholder = { Text(stringResource(R.string.custom_action_hint)) },
                                    enabled = submissionState !is TurnSubmissionState.Generating,
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (customActionText.isNotBlank()) {
                                                haptics.tap()
                                                val text = customActionText
                                                customActionText = ""
                                                viewModel.submitPlayerAction(text)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_action_input")
                                )
                                FilledIconButton(
                                    onClick = {
                                        haptics.tap()
                                        val text = customActionText
                                        customActionText = ""
                                        viewModel.submitPlayerAction(text)
                                    },
                                    enabled = customActionText.isNotBlank() &&
                                        submissionState !is TurnSubmissionState.Generating,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = dynamicSpec.accentPrimary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .testTag("send_custom_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.send_action)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = sessionState) {
                    UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is UiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .padding(24.dp)
                                .align(Alignment.Center)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }

                    is UiState.Success -> {
                        val data = state.data
                        val stats = parsedStats ?: HudStatsParser.parse(data.campaign)
                        val latestTurn = data.turns.lastOrNull()
                        val earlierTurns = remember(data.turns) {
                            if (data.turns.size > 1) data.turns.dropLast(1) else emptyList()
                        }
                        val generatingState = submissionState as? TurnSubmissionState.Generating
                        val activeNpcRelation = remember(data.campaign.activeNpc.name, data.campaign.npcs) {
                            val npcName = data.campaign.activeNpc.name.trim()
                            if (npcName.isBlank()) {
                                null
                            } else {
                                data.campaign.npcs.firstOrNull {
                                    it.name.equals(npcName, ignoreCase = true)
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 8.dp,
                                bottom = 22.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                        ) {
                            // 1. Slim atmospheric Scene Pill + Quick Action Buttons for Stats & Codex
                            item(key = "scene_location_strip") {
                                SceneStatusBarStrip(
                                    location = data.campaign.hudLocation,
                                    timeLabel = data.campaign.hudTime,
                                    weatherMood = stats.weatherMood,
                                    healthPercent = stats.healthPercent,
                                    accentColor = dynamicSpec.accentPrimary,
                                    secondaryColor = dynamicSpec.accentSecondary,
                                    onInspectScene = {
                                        if (generatingState == null && customActionText.isBlank()) {
                                            haptics.tap()
                                            customActionText = "Осмотреться вокруг (${data.campaign.hudLocation.ifBlank { "в сцене" }}): "
                                        }
                                    },
                                    onOpenStats = {
                                        haptics.tap()
                                        showStatsSheet = true
                                    }
                                )
                            }

                            // 2. Collapsible Previous Chapters Drawer (keeps focus on the active scene window)
                            if (earlierTurns.isNotEmpty()) {
                                item(key = "earlier_chapters_toggle") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable {
                                                haptics.tap()
                                                showHistoryTurns = !showHistoryTurns
                                            }
                                            .testTag("toggle_history_chapters")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.History,
                                                    contentDescription = null,
                                                    tint = dynamicSpec.accentSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (showHistoryTurns) {
                                                        "Скрыть прошлые главы (${earlierTurns.size})"
                                                    } else {
                                                        "Прошлые главы (${earlierTurns.size})"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Icon(
                                                imageVector = if (showHistoryTurns) {
                                                    Icons.Outlined.ExpandLess
                                                } else {
                                                    Icons.Outlined.ExpandMore
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = showHistoryTurns,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(top = 10.dp)
                                        ) {
                                            earlierTurns.forEach { pastTurn ->
                                                PastChapterCard(
                                                    turn = pastTurn,
                                                    accentColor = dynamicSpec.accentPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Dedicated Narrative Window (отдельное окно повествования)
                            item(key = "narrative_window") {
                                if (generatingState != null) {
                                    GeneratingNarrativeWindow(
                                        pendingAction = generatingState.pendingAction,
                                        streamingNarrative = generatingState.streamingNarrative,
                                        accentColor = dynamicSpec.accentPrimary
                                    )
                                } else if (latestTurn != null) {
                                    ActiveNarrativeWindow(
                                        turn = latestTurn,
                                        accentColor = dynamicSpec.accentPrimary,
                                        secondaryColor = dynamicSpec.accentSecondary
                                    )
                                }
                            }

                            // 4. Dedicated NPC Window with Avatar inside the text window
                            if (data.campaign.activeNpc.hasActiveNpc && data.campaign.activeNpc.name.isNotBlank()) {
                                item(key = "npc_dialogue_window") {
                                    NpcEmotionCard(
                                        npc = data.campaign.activeNpc,
                                        universe = data.campaign.universe,
                                        palette = universePalette,
                                        npcRelation = activeNpcRelation,
                                        onClick = {
                                            if (generatingState == null && customActionText.isBlank()) {
                                                haptics.tap()
                                                customActionText = "Обратиться к ${data.campaign.activeNpc.name}: "
                                            }
                                        }
                                    )
                                }
                            }

                            // 5. Choices & Quick Codex/Stats Footer Buttons
                            item(key = "choices_footer") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (generatingState == null && latestTurn != null && latestTurn.choices.isNotEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                            shape = RoundedCornerShape(999.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = "Ваш выбор",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                        latestTurn.choices.forEach { choice ->
                                            ChoiceActionCard(
                                                choice = choice,
                                                palette = universePalette,
                                                onClick = {
                                                    haptics.tap()
                                                    viewModel.submitPlayerAction(choice.text)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStatsSheet && successData != null) {
        val stats = parsedStats ?: HudStatsParser.parse(successData.campaign)
        StatsAndInventorySheet(
            campaign = successData.campaign,
            stats = stats,
            palette = universePalette,
            onItemClick = { item ->
                haptics.tap()
                selectedItemForAction = item
            },
            onOpenCodex = {
                haptics.tap()
                codexInitialTab = 0
                showCodex = true
            },
            onDismiss = { showStatsSheet = false }
        )
    }

    if (showCodex && successData != null) {
        CodexBottomSheet(
            campaign = successData.campaign,
            facts = facts,
            initialTabIndex = codexInitialTab,
            onAddFact = { text ->
                haptics.tap()
                viewModel.addFact(text)
            },
            onToggleFactPinned = { id ->
                haptics.tap()
                viewModel.toggleFactPinned(id)
            },
            onDeleteFact = { id ->
                haptics.tap()
                viewModel.deleteFact(id)
            },
            onItemClick = { item ->
                haptics.tap()
                showCodex = false
                selectedItemForAction = item
            },
            onDismiss = { showCodex = false }
        )
    }

    val activeItem = selectedItemForAction
    if (activeItem != null && successData != null) {
        val activeNpcName = if (successData.campaign.activeNpc.hasActiveNpc &&
            successData.campaign.activeNpc.name.isNotBlank()
        ) {
            successData.campaign.activeNpc.name
        } else {
            null
        }

        ItemActionSheet(
            itemName = activeItem,
            activeNpcName = activeNpcName,
            palette = universePalette,
            isBusy = submissionState is TurnSubmissionState.Generating,
            onDismiss = { selectedItemForAction = null },
            onUseInScene = { item ->
                haptics.impactConfirm()
                viewModel.submitPlayerAction("Использовать предмет из инвентаря: «$item» в текущей ситуации.")
            },
            onGiveToNpc = { item, npcName ->
                haptics.impactConfirm()
                viewModel.submitPlayerAction("Показать или передать предмет «$item» персонажу $npcName.")
            },
            onInsertIntoDraft = { item ->
                haptics.tap()
                val addition = "Использую предмет «$item» — "
                customActionText = if (customActionText.isBlank()) {
                    addition
                } else {
                    "${customActionText.trimEnd()} ($item) "
                }
            }
        )
    }
}

@Composable
private fun SceneStatusBarStrip(
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

@Composable
private fun ActiveNarrativeWindow(
    turn: CampaignTurn,
    accentColor: Color,
    secondaryColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.36f)),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("narrative_window")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    contentColor = accentColor,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Глава ${turn.turnNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Повествование",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
            }

            if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ваш шаг: ${turn.playerAction}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }

            EditorialNarrativeText(
                narrative = turn.narrative,
                dropCapColor = accentColor
            )
        }
    }
}

@Composable
private fun EditorialNarrativeText(
    narrative: String,
    dropCapColor: Color
) {
    val trimmed = narrative.trim()
    val bodyStyle = MaterialTheme.typography.bodyLarge
    if (trimmed.length >= 2) {
        val annotated = remember(trimmed, dropCapColor, bodyStyle.fontSize) {
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = (bodyStyle.fontSize.value * 1.65f).sp,
                        fontWeight = FontWeight.Bold,
                        color = dropCapColor
                    )
                ) {
                    append(trimmed.substring(0, 1))
                }
                append(trimmed.substring(1))
            }
        }
        SelectionContainer {
            Text(
                text = annotated,
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Text(
            text = trimmed,
            style = bodyStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PastChapterCard(
    turn: CampaignTurn,
    accentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Глава ${turn.turnNumber}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
                Text(
                    text = "Вы: ${turn.playerAction}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = turn.narrative,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
            )
        }
    }
}

@Composable
private fun GeneratingNarrativeWindow(
    pendingAction: String,
    streamingNarrative: String,
    accentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generating_turn_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ваш шаг: $pendingAction",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            if (streamingNarrative.isNotBlank()) {
                Text(
                    text = streamingNarrative,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("streaming_narrative_text")
                )
            } else {
                Text(
                    text = "Мастер плетёт нити событий и оценивает последствия…",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun ChoiceActionCard(
    choice: TurnChoice,
    palette: UniverseVisualPalette,
    onClick: () -> Unit
) {
    val (approachColor, approachIcon) = resolveApproachVisuals(choice.approach, palette)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, approachColor.copy(alpha = 0.42f)),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("choice_button_${choice.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .minimumInteractiveComponentSize()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(approachColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (choice.approach.isNotBlank()) {
                    Surface(
                        color = approachColor.copy(alpha = 0.16f),
                        contentColor = approachColor,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = approachIcon,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = choice.approach,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text = choice.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun resolveApproachVisuals(
    approach: String,
    palette: UniverseVisualPalette
): Pair<Color, ImageVector> {
    val lower = approach.lowercase()
    return when {
        lower.contains("роман") || lower.contains("флирт") || lower.contains("нежн") ->
            palette.choiceDiplomacyColor to Icons.Outlined.FavoriteBorder
        lower.contains("забот") || lower.contains("поддерж") || lower.contains("эмоц") ->
            palette.healthHigh to Icons.Outlined.Spa
        lower.contains("быт") || lower.contains("рутин") || lower.contains("уют") ->
            palette.accentSecondary to Icons.Outlined.LocalCafe
        lower.contains("дерз") || lower.contains("вызов") || lower.contains("риск") ->
            palette.choiceForceColor to Icons.Outlined.Whatshot
        lower.contains("сил") || lower.contains("атак") || lower.contains("бой") ->
            palette.choiceForceColor to Icons.Outlined.Bolt
        lower.contains("диплом") || lower.contains("перегов") || lower.contains("убеж") ->
            palette.choiceDiplomacyColor to Icons.Outlined.Forum
        lower.contains("скрыт") || lower.contains("тайн") || lower.contains("обман") ||
            lower.contains("тень") ->
            palette.choiceStealthColor to Icons.Outlined.VisibilityOff
        lower.contains("нестандарт") || lower.contains("хитр") || lower.contains("маг") ->
            palette.choiceWildcardColor to Icons.Outlined.AutoAwesome
        else ->
            palette.accentPrimary to Icons.Outlined.Explore
    }
}
