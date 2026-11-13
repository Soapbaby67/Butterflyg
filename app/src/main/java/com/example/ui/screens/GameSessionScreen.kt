package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CampaignTurn
import com.example.data.model.TurnChoice
import com.example.data.repository.PaperThemeMode
import com.example.ui.components.HudStatsParser
import com.example.ui.components.HudStatusPanel
import com.example.ui.components.NpcEmotionCard
import com.example.ui.components.PaperThemeToggleButton
import com.example.ui.components.SceneAtmosphereBanner
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.theme.rememberUniverseVisualPalette
import com.example.ui.viewmodel.GameSessionData
import com.example.ui.viewmodel.GameSessionViewModel
import com.example.ui.viewmodel.TurnSubmissionState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSessionScreen(
    viewModel: GameSessionViewModel,
    themeMode: PaperThemeMode,
    onCycleTheme: () -> Unit,
    onBack: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()

    var customActionText by rememberSaveable { mutableStateOf("") }
    var showCodex by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val successData = (sessionState as? UiState.Success<GameSessionData>)?.data
    val universePalette = rememberUniverseVisualPalette(
        universe = successData?.campaign?.universe.orEmpty()
    )

    LaunchedEffect(successData?.turns?.size, submissionState) {
        val count = successData?.turns?.size ?: 0
        if (count > 0) {
            listState.animateScrollToItem(index = count + 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (successData != null) {
                            Text(
                                text = "Ход ${successData.campaign.turnCount} • ${successData.campaign.universe}",
                                style = MaterialTheme.typography.bodySmall,
                                color = universePalette.accentPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (successData != null) {
                        IconButton(
                            onClick = { showCodex = true },
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
                    PaperThemeToggleButton(
                        themeMode = themeMode,
                        onCycleTheme = onCycleTheme
                    )
                }
            )
        },
        bottomBar = {
            if (successData != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
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
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
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
                                            onClick = { viewModel.submitPlayerAction(err.failedAction) }
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (customActionText.isNotBlank()) {
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
                                    val text = customActionText
                                    customActionText = ""
                                    viewModel.submitPlayerAction(text)
                                },
                                enabled = customActionText.isNotBlank() &&
                                    submissionState !is TurnSubmissionState.Generating,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = universePalette.accentPrimary,
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is UiState.Success -> {
                    val data = state.data
                    val latestTurn = data.turns.lastOrNull()
                    val isGenerating = submissionState is TurnSubmissionState.Generating
                    val parsedStats = remember(
                        data.campaign.hudStatusAndInventory,
                        data.campaign.hudRelationsAndCanon,
                        data.campaign.hudTime,
                        data.campaign.hudLocation,
                        data.campaign.inventory,
                        data.campaign.universe
                    ) {
                        HudStatsParser.parse(data.campaign)
                    }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        HudStatusPanel(
                            campaign = data.campaign,
                            stats = parsedStats,
                            palette = universePalette,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item(key = "scene_banner") {
                                SceneAtmosphereBanner(
                                    universe = data.campaign.universe,
                                    location = data.campaign.hudLocation,
                                    timeLabel = data.campaign.hudTime,
                                    weatherMood = parsedStats.weatherMood,
                                    palette = universePalette
                                )
                            }

                            itemsIndexed(
                                items = data.turns,
                                key = { _, turn -> turn.id.ifBlank { turn.turnNumber.toString() } }
                            ) { index, turn ->
                                val isLatest = index == data.turns.lastIndex
                                TurnChapterBlock(
                                    turn = turn,
                                    isLatest = isLatest,
                                    palette = universePalette
                                )
                            }

                            item(key = "npc_and_choices_footer") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    NpcEmotionCard(
                                        npc = data.campaign.activeNpc,
                                        universe = data.campaign.universe,
                                        palette = universePalette,
                                        npcRelation = activeNpcRelation
                                    )

                                    if (isGenerating) {
                                        val pending = (submissionState as TurnSubmissionState.Generating).pendingAction
                                        GeneratingTurnCard(
                                            pendingAction = pending,
                                            palette = universePalette
                                        )
                                    } else if (latestTurn != null && latestTurn.choices.isNotEmpty()) {
                                        Text(
                                            text = "Ваш выбор",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        latestTurn.choices.forEach { choice ->
                                            ChoiceActionCard(
                                                choice = choice,
                                                palette = universePalette,
                                                onClick = { viewModel.submitPlayerAction(choice.text) }
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

    if (showCodex && successData != null) {
        CodexBottomSheet(
            campaign = successData.campaign,
            onDismiss = { showCodex = false }
        )
    }
}

@Composable
private fun TurnChapterBlock(
    turn: CampaignTurn,
    isLatest: Boolean,
    palette: UniverseVisualPalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
            Surface(
                color = palette.accentPrimary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Вы: ${turn.playerAction}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Глава ${turn.turnNumber}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.accentPrimary
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = turn.narrative,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isLatest) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
            }
        )
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
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, approachColor.copy(alpha = 0.38f)),
        tonalElevation = 2.dp,
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
                        color = approachColor.copy(alpha = 0.14f),
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

@Composable
private fun GeneratingTurnCard(
    pendingAction: String,
    palette: UniverseVisualPalette
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Вы: $pendingAction",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = palette.accentPrimary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = "Мастер плетёт нити событий и оценивает последствия…",
                style = MaterialTheme.typography.bodySmall,
                color = palette.accentPrimary
            )
        }
    }
}
