package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import com.example.core.LocalRpgAudioEngine
import com.example.core.SceneNarratorTts
import com.example.core.SfxEvent
import com.example.core.rememberRpgHaptics
import com.example.data.model.EmotionCategory
import com.example.data.model.InventoryItem
import com.example.data.model.RiskyChoiceResolver
import com.example.data.model.TurnChoice
import com.example.ui.components.AchievementUnlockedBanner
import com.example.ui.components.HudStatsParser
import com.example.ui.components.ItemActionSheet
import com.example.ui.components.NpcEmotionCard
import com.example.ui.components.RiskyOutcomeRouletteSheet
import com.example.ui.components.SkillLevelUpBanner
import com.example.ui.components.StatsAndInventoryContent
import com.example.ui.components.WeatherCanvasOverlay
import com.example.ui.screens.session.ActionComposerDock
import com.example.ui.screens.session.ActiveNarrativeWindow
import com.example.ui.screens.session.ChapterNavigatorSheet
import com.example.ui.screens.session.ChoiceActionCard
import com.example.ui.screens.session.GeneratingNarrativeWindow
import com.example.ui.screens.session.PastChaptersSection
import com.example.ui.screens.session.SceneStatusBarStrip
import com.example.ui.screens.session.SessionTopBar
import com.example.ui.screens.session.SwipeNavigationStrip
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.ChroniclesDynamicBackdrop
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.theme.LocalDynamicGradientSpec
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.rememberUniverseVisualPalette
import com.example.ui.theme.resolveSceneBackdropRes
import com.example.ui.viewmodel.GameSessionData
import com.example.ui.viewmodel.GameSessionViewModel
import com.example.ui.viewmodel.TurnSubmissionState
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun GameSessionScreen(
    viewModel: GameSessionViewModel,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean = true,
    autoReadTtsEnabled: Boolean = false,
    focusReadingMode: Boolean = false,
    onToggleFocusReadingMode: (Boolean) -> Unit = {},
    onToggleSound: () -> Unit = {},
    onDynamicContextChange: (DynamicThemeContext) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenForkedCampaign: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val facts by viewModel.facts.collectAsStateWithLifecycle()
    val extendedState by viewModel.extendedState.collectAsStateWithLifecycle()
    val achievementStatuses by viewModel.achievementStatuses.collectAsStateWithLifecycle()
    val recentSkillLevelUp by viewModel.recentSkillLevelUp.collectAsStateWithLifecycle()
    val recentAchievementBanner by viewModel.recentAchievementBanner.collectAsStateWithLifecycle()
    val draftAction by viewModel.draftAction.collectAsStateWithLifecycle()
    val isMutatingHistory by viewModel.isMutatingHistory.collectAsStateWithLifecycle()
    val itemInspectionState by viewModel.itemInspectionState.collectAsStateWithLifecycle()
    val actionNotice by viewModel.actionNotice.collectAsStateWithLifecycle()

    val haptics = rememberRpgHaptics(hapticsEnabled)
    val audio = LocalRpgAudioEngine.current
    val context = LocalContext.current

    val narratorTts = remember(context, audio) {
        SceneNarratorTts(context.applicationContext, audio)
    }
    DisposableEffect(narratorTts) {
        onDispose { narratorTts.shutdown() }
    }
    val isTtsSpeaking by narratorTts.isSpeaking.collectAsStateWithLifecycle()
    val ttsSpeakingTurnId by narratorTts.speakingTurnId.collectAsStateWithLifecycle()
    val isTtsAvailable by narratorTts.isAvailable.collectAsStateWithLifecycle()

    var isCustomActionRisky by rememberSaveable { mutableStateOf(false) }
    var showHistoryTurns by rememberSaveable { mutableStateOf(false) }
    var showChapterSearchSheet by rememberSaveable { mutableStateOf(false) }
    var codexTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedItemForAction by remember { mutableStateOf<InventoryItem?>(null) }
    var pendingRiskyChoice by remember { mutableStateOf<TurnChoice?>(null) }
    var showRollbackConfirm by remember { mutableStateOf(false) }
    var localVoiceNotice by remember { mutableStateOf<String?>(null) }

    val bookmarkedTurns = remember(extendedState.bookmarks) {
        extendedState.bookmarks.map { it.turnNumber }.toSet()
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Page 0 = Stats & Inventory (swipe right), Page 1 = Active Scene (center), Page 2 = Codex (swipe left)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) {
                val merged = if (draftAction.isBlank()) {
                    spoken
                } else {
                    "${draftAction.trimEnd()} $spoken"
                }
                viewModel.updateDraftAction(merged)
                localVoiceNotice = null
            }
        }
    }

    BackHandler(enabled = pagerState.currentPage != 1) {
        coroutineScope.launch { pagerState.animateScrollToPage(1) }
    }

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
        successData?.campaign?.inventoryItems,
        successData?.campaign?.healthPercent,
        successData?.campaign?.currencyAmount,
        successData?.campaign?.currencyLabel,
        successData?.campaign?.skills,
        successData?.campaign?.universe
    ) {
        successData?.campaign?.let { HudStatsParser.parse(it) }
    }

    val activeEmotion = remember(successData?.campaign?.activeNpc?.emotionCategory) {
        EmotionCategory.fromKey(successData?.campaign?.activeNpc?.emotionCategory.orEmpty())
    }

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

    var lastTurnCount by rememberSaveable(successData?.campaign?.id) { mutableStateOf<Long?>(null) }
    var lastButterflyCount by rememberSaveable(successData?.campaign?.id) { mutableStateOf<Int?>(null) }
    var lastEmotionKey by rememberSaveable(successData?.campaign?.id) { mutableStateOf<String?>(null) }
    var lastPagerPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastPagerPage) {
            lastPagerPage = pagerState.currentPage
            audio?.playSfx(SfxEvent.SWIPE_WHOOSH)
        }
    }

    LaunchedEffect(
        successData?.campaign?.turnCount,
        successData?.campaign?.butterflyEvents?.size,
        successData?.campaign?.activeNpc?.emotionCategory,
        autoReadTtsEnabled
    ) {
        val campaign = successData?.campaign ?: return@LaunchedEffect
        val latestTurn = successData.turns.lastOrNull()
        val prevTurns = lastTurnCount
        val prevButterflies = lastButterflyCount
        val prevEmotion = lastEmotionKey
        val currEmotion = campaign.activeNpc.emotionCategory
        if (prevTurns != null && prevButterflies != null) {
            if (campaign.butterflyEvents.size > prevButterflies) {
                haptics.butterflyShift()
                audio?.playSfx(SfxEvent.BUTTERFLY_SHIFT)
            } else if (campaign.turnCount > prevTurns) {
                haptics.impactConfirm()
                audio?.playSfx(SfxEvent.AI_REVEAL)
            } else if (prevEmotion != null && prevEmotion != currEmotion && campaign.activeNpc.hasActiveNpc) {
                audio?.playSfx(SfxEvent.EMOTION_SHIFT)
            }
            if (autoReadTtsEnabled && campaign.turnCount > prevTurns && latestTurn != null) {
                narratorTts.speak(
                    turnId = latestTurn.id.ifBlank { "${latestTurn.turnNumber}" },
                    narrativeText = latestTurn.narrative
                )
            }
        }
        lastTurnCount = campaign.turnCount
        lastButterflyCount = campaign.butterflyEvents.size
        lastEmotionKey = currEmotion
    }

    LaunchedEffect(successData?.turns?.size, submissionState is TurnSubmissionState.Generating) {
        if ((successData?.turns?.size ?: 0) > 0) {
            listState.animateScrollToItem(index = 0)
        }
    }

    val dynamicSpec = LocalDynamicGradientSpec.current
    val isBusy = submissionState is TurnSubmissionState.Generating || isMutatingHistory
    val canRollback = (successData?.turns?.size ?: 0) >= 2

    val submitDraftAction: () -> Unit = {
        val text = draftAction.trim()
        if (text.isNotBlank() && !isBusy && successData != null) {
            haptics.tap()
            narratorTts.stop()
            if (isCustomActionRisky) {
                audio?.playSfx(SfxEvent.UI_TAP)
                pendingRiskyChoice = RiskyChoiceResolver.buildCustomRiskChoice(
                    actionText = text,
                    activeNpcName = successData.campaign.activeNpc.name
                        .takeIf { successData.campaign.activeNpc.hasActiveNpc }
                )
            } else {
                audio?.playSfx(SfxEvent.SEND_ACTION)
                viewModel.submitPlayerAction(text)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                SessionTopBar(
                    title = successData?.campaign?.title ?: "Хроника",
                    subtitle = successData?.let {
                        if (focusReadingMode) {
                            "Режим чтения • Глава ${it.campaign.turnCount}"
                        } else {
                            "Ход ${it.campaign.turnCount} • ${it.campaign.universe}"
                        }
                    },
                    currentPage = pagerState.currentPage,
                    hasSessionData = successData != null,
                    canRollback = canRollback,
                    isBusy = isBusy,
                    focusReadingMode = focusReadingMode,
                    bookmarksCount = bookmarkedTurns.size,
                    topBarContainerColor = dynamicSpec.topColor.copy(alpha = if (focusReadingMode) 0.50f else 0.68f),
                    accentPrimary = dynamicSpec.accentPrimary,
                    onBackClick = {
                        if (pagerState.currentPage != 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        } else {
                            narratorTts.stop()
                            onBack()
                        }
                    },
                    onToggleFocusReading = {
                        haptics.tap()
                        audio?.playSfx(SfxEvent.UI_TAP)
                        onToggleFocusReadingMode(!focusReadingMode)
                    },
                    onOpenChapterSearch = {
                        haptics.tap()
                        audio?.playSfx(SfxEvent.UI_TAP)
                        showChapterSearchSheet = true
                    },
                    onRollbackClick = {
                        haptics.tap()
                        audio?.playSfx(SfxEvent.UI_TAP)
                        showRollbackConfirm = true
                    },
                    onOpenSettings = {
                        haptics.tap()
                        audio?.playSfx(SfxEvent.UI_TAP)
                        onOpenSettings()
                    }
                )
            },
            bottomBar = {
                if (successData != null && pagerState.currentPage == 1 && focusReadingMode) {
                    // Compact thumb-zone pill in Focus Reading Mode (<15% viewport height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.2.dp, dynamicSpec.accentPrimary.copy(alpha = 0.55f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .clickable {
                                    haptics.tap()
                                    audio?.playSfx(SfxEvent.UI_TAP)
                                    onToggleFocusReadingMode(false)
                                }
                                .testTag("focus_reading_answer_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = dynamicSpec.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Ответить / Выборы",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = successData != null && pagerState.currentPage == 1,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (successData != null && parsedStats != null) {
                            ActionComposerDock(
                                draftAction = draftAction,
                                isCustomActionRisky = isCustomActionRisky,
                                isBusy = isBusy,
                                submissionState = submissionState,
                                actionNotice = localVoiceNotice ?: actionNotice,
                                inventoryItems = parsedStats.structuredInventory,
                                accentPrimary = dynamicSpec.accentPrimary,
                                universePalette = universePalette,
                                onDraftChange = viewModel::updateDraftAction,
                                onToggleRiskMode = { enabled ->
                                    haptics.tap()
                                    audio?.playSfx(SfxEvent.UI_TAP)
                                    isCustomActionRisky = enabled
                                },
                                onVoiceInputClick = {
                                    haptics.tap()
                                    audio?.playSfx(SfxEvent.UI_TAP)
                                    narratorTts.stop()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Надиктуйте действие героя…")
                                    }
                                    runCatching { voiceInputLauncher.launch(intent) }
                                        .onFailure {
                                            localVoiceNotice = "Голосовой ввод недоступен на этом устройстве."
                                        }
                                },
                                onSubmit = submitDraftAction,
                                onRetryFailed = { failedAction ->
                                    haptics.tap()
                                    viewModel.submitPlayerAction(failedAction)
                                },
                                onDismissNotice = {
                                    localVoiceNotice = null
                                    viewModel.clearActionNotice()
                                },
                                onItemClick = { item ->
                                    haptics.tap()
                                    viewModel.clearItemInspection()
                                    selectedItemForAction = item
                                }
                            )
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

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedVisibility(
                                visible = !focusReadingMode || pagerState.currentPage != 1,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                SwipeNavigationStrip(
                                    currentPage = pagerState.currentPage,
                                    accentColor = dynamicSpec.accentPrimary,
                                    secondaryColor = dynamicSpec.accentSecondary,
                                    onSelectPage = { page ->
                                        haptics.tap()
                                        coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 720.dp)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("game_horizontal_pager")
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                                shape = MaterialTheme.shapes.large,
                                                border = BorderStroke(1.dp, dynamicSpec.accentSecondary.copy(alpha = 0.34f)),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .widthIn(max = 720.dp)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                StatsAndInventoryContent(
                                                    campaign = data.campaign,
                                                    stats = stats,
                                                    palette = universePalette,
                                                    skillXp = extendedState.skillXp,
                                                    onItemClick = { item ->
                                                        haptics.tap()
                                                        viewModel.clearItemInspection()
                                                        selectedItemForAction = item
                                                    },
                                                    onOpenCodex = {
                                                        haptics.tap()
                                                        codexTab = 0
                                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .navigationBarsPadding()
                                                )
                                            }
                                        }
                                    }

                                    1 -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            LazyColumn(
                                                state = listState,
                                                contentPadding = PaddingValues(
                                                    start = 14.dp,
                                                    end = 14.dp,
                                                    top = 4.dp,
                                                    bottom = 24.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .widthIn(max = 720.dp)
                                            ) {
                                                if (!focusReadingMode) {
                                                    item(key = "scene_location_strip") {
                                                        SceneStatusBarStrip(
                                                            location = data.campaign.hudLocation,
                                                            timeLabel = data.campaign.hudTime,
                                                            weatherMood = stats.weatherMood,
                                                            healthPercent = stats.healthPercent,
                                                            accentColor = dynamicSpec.accentPrimary,
                                                            secondaryColor = dynamicSpec.accentSecondary,
                                                            onInspectScene = {
                                                                if (!isBusy && draftAction.isBlank()) {
                                                                    haptics.tap()
                                                                    viewModel.updateDraftAction(
                                                                        "Осмотреться вокруг (${data.campaign.hudLocation.ifBlank { "в сцене" }}): "
                                                                    )
                                                                }
                                                            },
                                                            onOpenStats = {
                                                                haptics.tap()
                                                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                                            }
                                                        )
                                                    }
                                                }

                                                if (earlierTurns.isNotEmpty()) {
                                                    item(key = "earlier_chapters_toggle") {
                                                        PastChaptersSection(
                                                            earlierTurns = earlierTurns,
                                                            chapterSummaries = data.campaign.chapterSummaries,
                                                            bookmarkedTurns = bookmarkedTurns,
                                                            expanded = showHistoryTurns,
                                                            isBusy = isBusy,
                                                            accentPrimary = dynamicSpec.accentPrimary,
                                                            accentSecondary = dynamicSpec.accentSecondary,
                                                            onToggleExpanded = {
                                                                haptics.tap()
                                                                showHistoryTurns = !showHistoryTurns
                                                            },
                                                            onToggleBookmark = { turn ->
                                                                haptics.tap()
                                                                audio?.playSfx(SfxEvent.UI_TAP)
                                                                viewModel.toggleChapterBookmark(turn)
                                                            },
                                                            onForkAtTurn = { turnNum ->
                                                                haptics.tap()
                                                                audio?.playSfx(SfxEvent.UI_TAP)
                                                                narratorTts.stop()
                                                                viewModel.forkCampaignAtTurn(turnNum) { forkedId ->
                                                                    onOpenForkedCampaign(forkedId)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }

                                                item(key = "narrative_window") {
                                                    if (generatingState != null) {
                                                        GeneratingNarrativeWindow(
                                                            pendingAction = generatingState.pendingAction,
                                                            streamingNarrativeFlow = viewModel.streamingNarrative,
                                                            accentColor = dynamicSpec.accentPrimary
                                                        )
                                                    } else if (latestTurn != null) {
                                                        val turnKey = latestTurn.id.ifBlank { "${latestTurn.turnNumber}" }
                                                        ActiveNarrativeWindow(
                                                            turn = latestTurn,
                                                            accentColor = dynamicSpec.accentPrimary,
                                                            secondaryColor = dynamicSpec.accentSecondary,
                                                            isBookmarked = latestTurn.turnNumber in bookmarkedTurns,
                                                            isSpeakingThisTurn = isTtsSpeaking && ttsSpeakingTurnId == turnKey,
                                                            isTtsAvailable = isTtsAvailable,
                                                            isBusy = isBusy,
                                                            onToggleBookmark = {
                                                                haptics.tap()
                                                                audio?.playSfx(SfxEvent.UI_TAP)
                                                                viewModel.toggleChapterBookmark(latestTurn)
                                                            },
                                                            onToggleTts = {
                                                                haptics.tap()
                                                                narratorTts.toggleSpeak(turnKey, latestTurn.narrative)
                                                            },
                                                            onRerollScene = {
                                                                haptics.tap()
                                                                audio?.playSfx(SfxEvent.SEND_ACTION)
                                                                narratorTts.stop()
                                                                viewModel.rerollLastTurn()
                                                            },
                                                            onEditLastAction = {
                                                                haptics.tap()
                                                                audio?.playSfx(SfxEvent.UI_TAP)
                                                                narratorTts.stop()
                                                                viewModel.editLastPlayerAction()
                                                            }
                                                        )
                                                    }
                                                }

                                                val levelUpEvent = recentSkillLevelUp
                                                if (levelUpEvent != null) {
                                                    item(key = "skill_level_up_banner") {
                                                        SkillLevelUpBanner(
                                                            event = levelUpEvent,
                                                            accentColor = dynamicSpec.accentPrimary,
                                                            onDismiss = { viewModel.dismissSkillLevelUp() }
                                                        )
                                                    }
                                                }

                                                val unlockedMilestone = recentAchievementBanner
                                                if (unlockedMilestone != null) {
                                                    item(key = "achievement_unlocked_banner") {
                                                        AchievementUnlockedBanner(
                                                            achievement = unlockedMilestone,
                                                            accentColor = dynamicSpec.accentSecondary,
                                                            onOpenMilestones = {
                                                                viewModel.dismissAchievementBanner()
                                                                codexTab = 3
                                                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                                            },
                                                            onDismiss = { viewModel.dismissAchievementBanner() }
                                                        )
                                                    }
                                                }

                                                if (data.campaign.activeNpc.hasActiveNpc && data.campaign.activeNpc.name.isNotBlank()) {
                                                    item(key = "npc_dialogue_window") {
                                                        NpcEmotionCard(
                                                            npc = data.campaign.activeNpc,
                                                            universe = data.campaign.universe,
                                                            palette = universePalette,
                                                            npcRelation = activeNpcRelation,
                                                            onClick = {
                                                                if (!isBusy && draftAction.isBlank()) {
                                                                    haptics.tap()
                                                                    viewModel.updateDraftAction(
                                                                        "Обратиться к ${data.campaign.activeNpc.name}: "
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                }

                                                if (!focusReadingMode) {
                                                    item(key = "choices_footer") {
                                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            if (!isBusy && latestTurn != null && latestTurn.choices.isNotEmpty()) {
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
                                                                    val isRisky = remember(choice) {
                                                                        RiskyChoiceResolver.isChoiceRisky(choice)
                                                                    }
                                                                    ChoiceActionCard(
                                                                        choice = choice,
                                                                        isRisky = isRisky,
                                                                        palette = universePalette,
                                                                        onClick = {
                                                                            haptics.tap()
                                                                            narratorTts.stop()
                                                                            if (isRisky) {
                                                                                audio?.playSfx(SfxEvent.UI_TAP)
                                                                                pendingRiskyChoice = choice
                                                                            } else {
                                                                                audio?.playSfx(SfxEvent.SEND_ACTION)
                                                                                viewModel.submitPlayerAction(
                                                                                    rawAction = choice.text,
                                                                                    choiceApproach = choice.approach
                                                                                )
                                                                            }
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

                                    else -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                                shape = MaterialTheme.shapes.large,
                                                border = BorderStroke(1.dp, dynamicSpec.accentPrimary.copy(alpha = 0.34f)),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .widthIn(max = 720.dp)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    .testTag("codex_swipe_page")
                                            ) {
                                                CodexContent(
                                                    campaign = data.campaign,
                                                    turns = data.turns,
                                                    facts = facts,
                                                    achievementStatuses = achievementStatuses,
                                                    isBusy = isBusy,
                                                    selectedTab = codexTab,
                                                    onTabSelect = { codexTab = it },
                                                    onAddFact = { text ->
                                                        haptics.tap()
                                                        audio?.playSfx(SfxEvent.UI_TAP)
                                                        viewModel.addFact(text)
                                                    },
                                                    onToggleFactPinned = { id ->
                                                        haptics.tap()
                                                        audio?.playSfx(SfxEvent.UI_TAP)
                                                        viewModel.toggleFactPinned(id)
                                                    },
                                                    onDeleteFact = { id ->
                                                        haptics.tap()
                                                        audio?.playSfx(SfxEvent.UI_TAP)
                                                        viewModel.deleteFact(id)
                                                    },
                                                    onItemClick = { item ->
                                                        haptics.tap()
                                                        audio?.playSfx(SfxEvent.ITEM_USE)
                                                        viewModel.clearItemInspection()
                                                        selectedItemForAction = item
                                                    },
                                                    onForkAtTurn = { turnNum ->
                                                        haptics.tap()
                                                        audio?.playSfx(SfxEvent.UI_TAP)
                                                        narratorTts.stop()
                                                        viewModel.forkCampaignAtTurn(turnNum) { forkedId ->
                                                            onOpenForkedCampaign(forkedId)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(top = 12.dp)
                                                        .navigationBarsPadding()
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
    }

    // Sync selected item with live campaign state so Equip/Unequip reflects immediately inside the sheet
    val rawSelectedItem = selectedItemForAction
    if (rawSelectedItem != null && successData != null && parsedStats != null) {
        val liveItem = parsedStats.structuredInventory.firstOrNull {
            it.resolvedId() == rawSelectedItem.resolvedId()
        } ?: rawSelectedItem
        val activeNpcName = successData.campaign.activeNpc.name
            .takeIf { successData.campaign.activeNpc.hasActiveNpc && it.isNotBlank() }

        ItemActionSheet(
            item = liveItem,
            activeNpcName = activeNpcName,
            palette = universePalette,
            isBusy = isBusy,
            inspectionState = itemInspectionState,
            onDismiss = {
                selectedItemForAction = null
                viewModel.clearItemInspection()
            },
            onUseInScene = { itemName ->
                haptics.impactConfirm()
                audio?.playSfx(SfxEvent.ITEM_USE)
                narratorTts.stop()
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                viewModel.submitPlayerAction("Использовать предмет из инвентаря: «$itemName» в текущей ситуации.")
            },
            onToggleEquip = { item ->
                haptics.tap()
                audio?.playSfx(SfxEvent.ITEM_USE)
                viewModel.toggleItemEquipped(item)
            },
            onInspectItem = { item ->
                haptics.tap()
                audio?.playSfx(SfxEvent.UI_TAP)
                viewModel.inspectItem(item)
            },
            onGiveToNpc = { itemName, npcName ->
                haptics.impactConfirm()
                audio?.playSfx(SfxEvent.ITEM_USE)
                narratorTts.stop()
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                viewModel.submitPlayerAction("Показать или передать предмет «$itemName» персонажу $npcName.")
            },
            onInsertIntoDraft = { itemName ->
                haptics.tap()
                audio?.playSfx(SfxEvent.UI_TAP)
                val addition = "Использую предмет «$itemName» — "
                val nextDraft = if (draftAction.isBlank()) {
                    addition
                } else {
                    "${draftAction.trimEnd()} ($itemName) "
                }
                viewModel.updateDraftAction(nextDraft)
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
            }
        )
    }

    val activeRiskChoice = pendingRiskyChoice
    if (activeRiskChoice != null && successData != null && parsedStats != null) {
        val activeNpcName = successData.campaign.activeNpc.name
            .takeIf { successData.campaign.activeNpc.hasActiveNpc && it.isNotBlank() }
        RiskyOutcomeRouletteSheet(
            choice = activeRiskChoice,
            activeNpcName = activeNpcName,
            skills = successData.campaign.skills,
            inventoryItems = parsedStats.structuredInventory,
            palette = universePalette,
            haptics = haptics,
            onDismiss = { pendingRiskyChoice = null },
            onOutcomeResolved = { outcome, modifier ->
                val prompt = RiskyChoiceResolver.formatOutcomePrompt(
                    actionText = activeRiskChoice.text,
                    outcome = outcome,
                    modifier = modifier
                )
                if (activeRiskChoice.id == "custom_risk") {
                    viewModel.clearDraftAction()
                    isCustomActionRisky = false
                }
                pendingRiskyChoice = null
                narratorTts.stop()
                viewModel.submitPlayerAction(
                    rawAction = prompt,
                    choiceApproach = activeRiskChoice.approach
                )
            }
        )
    }

    if (showChapterSearchSheet && successData != null) {
        ChapterNavigatorSheet(
            turns = successData.turns,
            bookmarks = extendedState.bookmarks,
            butterflyEvents = successData.campaign.butterflyEvents,
            isBusy = isBusy,
            accentPrimary = dynamicSpec.accentPrimary,
            accentSecondary = dynamicSpec.accentSecondary,
            onToggleBookmark = { turn ->
                haptics.tap()
                audio?.playSfx(SfxEvent.UI_TAP)
                viewModel.toggleChapterBookmark(turn)
            },
            onForkAtTurn = { turnNum ->
                haptics.tap()
                audio?.playSfx(SfxEvent.UI_TAP)
                narratorTts.stop()
                viewModel.forkCampaignAtTurn(turnNum) { forkedId ->
                    onOpenForkedCampaign(forkedId)
                }
            },
            onDismiss = { showChapterSearchSheet = false }
        )
    }

    if (showRollbackConfirm) {
        AlertDialog(
            onDismissRequest = { showRollbackConfirm = false },
            title = { Text("Откатить последний ход?") },
            text = {
                Text(
                    "Последний ответ мастера будет удалён, а локация, инвентарь, здоровье и отношения NPC вернутся к предыдущей главе."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRollbackConfirm = false
                        narratorTts.stop()
                        viewModel.rollbackLastTurn()
                    }
                ) {
                    Text("Откатить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRollbackConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
