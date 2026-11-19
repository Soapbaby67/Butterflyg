package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.LocalRpgAudioEngine
import com.example.core.SfxEvent
import com.example.data.model.PlayerGender
import com.example.data.model.ScenarioGenre
import com.example.data.model.SkillType
import com.example.data.model.UniversePresets
import com.example.ui.theme.ChroniclesDynamicBackdrop
import com.example.ui.viewmodel.CharacterArchetype
import com.example.ui.viewmodel.SetupWizardViewModel

private val tonePresets = listOf(
    "Романтический слоубёрн и живая химия: тонкие диалоги, невербальные реакции, ревность и последствия каждого откровенного слова.",
    "Тёплая повседневность и рутина: внимание к мелочам, репутация среди соседей, бюджет и постепенное раскрытие характеров.",
    "Мрачный реализм без сюжетной брони: ошибки ведут к травмам, потере союзников или гибели.",
    "Каноничный сёнэн с высоким накалом битв, но без неуязвимости героя.",
    "Политические интриги, шпионаж и высокая цена доверия.",
    "Напряжённый мистический детектив с тяжёлыми моральными выборами."
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupWizardScreen(
    viewModel: SetupWizardViewModel,
    initialTemplateId: String?,
    onBack: () -> Unit,
    onCampaignCreated: (campaignId: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val audio = LocalRpgAudioEngine.current
    val context = LocalContext.current

    var showAiGenrePicker by remember { mutableStateOf(false) }
    var showWorldCodeDialog by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var importCodeInput by remember { mutableStateOf("") }
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(initialTemplateId) {
        viewModel.applyTemplateById(initialTemplateId)
    }

    LaunchedEffect(state.createdCampaignId) {
        val createdId = state.createdCampaignId
        if (!createdId.isNullOrBlank()) {
            onCampaignCreated(createdId)
        }
    }

    val isBusy = state.isGenerating || state.isGeneratingPremise

    ChroniclesDynamicBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            enabled = !state.isGenerating,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("setup_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.setup_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                audio?.playSfx(SfxEvent.SEND_ACTION)
                                viewModel.startChronicle()
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .height(54.dp)
                                .testTag("start_chronicle_button")
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Мастер открывает первую сцену…")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.start_game))
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Прежде чем начнётся сюжет, ответьте на 4 вопроса мастера. Или выберите шаблон вселенной — любое поле можно изменить.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(UniversePresets.templates, key = { it.id }) { template ->
                            FilterChip(
                                selected = state.selectedTemplateId == template.id,
                                onClick = {
                                    audio?.playSfx(SfxEvent.UI_TAP)
                                    viewModel.applyTemplate(template)
                                },
                                enabled = !isBusy,
                                label = { Text(template.title) }
                            )
                        }
                    }

                    // AI Premise Generator & Shareable World Code bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                audio?.playSfx(SfxEvent.UI_TAP)
                                showAiGenrePicker = !showAiGenrePicker
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setup_ai_premise_button")
                        ) {
                            if (state.isGeneratingPremise) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ИИ придумывает…", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Завязка от ИИ", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                audio?.playSfx(SfxEvent.UI_TAP)
                                copiedFeedback = false
                                generatedCode = viewModel.buildCurrentWorldCode()
                                importCodeInput = ""
                                showWorldCodeDialog = true
                            },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("setup_world_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Код мира")
                        }
                    }

                    AnimatedVisibility(visible = showAiGenrePicker && !state.isGeneratingPremise) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Выберите жанр для мгновенной завязки:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ScenarioGenre.entries
                                        .filter { it != ScenarioGenre.ALL }
                                        .forEach { genre ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    audio?.playSfx(SfxEvent.UI_TAP)
                                                    showAiGenrePicker = false
                                                    viewModel.generatePremiseWithAi(genre)
                                                },
                                                label = { Text(genre.labelRu) },
                                                modifier = Modifier.testTag("ai_genre_${genre.name.lowercase()}")
                                            )
                                        }
                                }
                            }
                        }
                    }

                    if (!state.infoMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.infoMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Название хроники (необязательно)") },
                        placeholder = { Text("Например: Тень Конохи") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_title_input")
                    )

                    // Question 1: Universe
                    QuestionCard(
                        number = "1",
                        title = "Вселенная",
                        prompt = "В какой мир (аниме, сериал, фильм, книга, игра или авторский мир) мы отправляемся?"
                    ) {
                        OutlinedTextField(
                            value = state.universe,
                            onValueChange = viewModel::updateUniverse,
                            placeholder = { Text("Например: Наруто, Ведьмак, Атака титанов или свой мир") },
                            minLines = 2,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_universe_input")
                        )
                    }

                    // Question 2: Character + Name + Gender Lock + 4 Hero Skills
                    QuestionCard(
                        number = "2",
                        title = "Персонаж",
                        prompt = "Кем вы играете? Зафиксируйте имя, род глаголов и стартовые навыки."
                    ) {
                        OutlinedTextField(
                            value = state.playerName,
                            onValueChange = viewModel::updatePlayerName,
                            label = { Text("Имя героя") },
                            placeholder = { Text("Например: Кирилл, Алина, Рэн…") },
                            singleLine = true,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_player_name_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Грамматический род (ИИ-мастер будет строго держать род глаголов):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlayerGender.entries.forEach { gender ->
                                FilterChip(
                                    selected = state.playerGender == gender,
                                    onClick = { viewModel.updatePlayerGender(gender) },
                                    enabled = !isBusy,
                                    label = { Text(gender.labelRu) },
                                    modifier = Modifier.testTag("setup_gender_${gender.key}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CharacterArchetype.entries.forEach { archetype ->
                                FilterChip(
                                    selected = state.archetype == archetype,
                                    onClick = { viewModel.updateArchetype(archetype) },
                                    enabled = !isBusy,
                                    label = { Text(archetype.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = state.characterDetails,
                            onValueChange = viewModel::updateCharacterDetails,
                            placeholder = { Text("Возраст, способности, снаряжение, слабые стороны…") },
                            minLines = 3,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_character_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4 Universal RPG Skills (1..10)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Характеристики героя (1–10)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val totalSkillPoints = state.skills.might +
                                        state.skills.stealth +
                                        state.skills.insight +
                                        state.skills.lore
                                    Text(
                                        text = "Сумма: $totalSkillPoints",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                SkillType.entries.forEach { skill ->
                                    val value = state.skills.levelFor(skill)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = skill.labelRu,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = skill.shortHintRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.updateSkill(skill, value - 1L) },
                                                enabled = !isBusy && value > 1L,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("skill_dec_${skill.key}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Уменьшить ${skill.labelRu}",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = "$value",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { viewModel.updateSkill(skill, value + 1L) },
                                                enabled = !isBusy && value < 10L,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("skill_inc_${skill.key}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Увеличить ${skill.labelRu}",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Question 3: Starting point
                    QuestionCard(
                        number = "3",
                        title = "Точка старта",
                        prompt = "В какой момент сюжета или в какой локации начинается игра?"
                    ) {
                        OutlinedTextField(
                            value = state.startingPoint,
                            onValueChange = viewModel::updateStartingPoint,
                            placeholder = { Text("Например: Новиград в ночь первого костра, за день до…") },
                            minLines = 2,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_starting_point_input")
                        )
                    }

                    // Question 4: Tone and Difficulty
                    QuestionCard(
                        number = "4",
                        title = "Тон и сложность",
                        prompt = "Какая атмосфера предпочтительнее и насколько жёсткими должны быть последствия ошибок?"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tonePresets.forEachIndexed { index, preset ->
                                val shortLabel = when (index) {
                                    0 -> "Романтика"
                                    1 -> "Рутина и уют"
                                    2 -> "Мрачный реализм"
                                    3 -> "Каноничный сёнэн"
                                    4 -> "Интриги"
                                    else -> "Детектив"
                                }
                                FilterChip(
                                    selected = state.toneAndDifficulty == preset,
                                    onClick = { viewModel.updateToneAndDifficulty(preset) },
                                    enabled = !isBusy,
                                    label = { Text(shortLabel) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.toneAndDifficulty,
                            onValueChange = viewModel::updateToneAndDifficulty,
                            minLines = 2,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_tone_input")
                        )
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = state.errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showWorldCodeDialog) {
        AlertDialog(
            onDismissRequest = { showWorldCodeDialog = false },
            title = {
                Text(
                    text = "Код шаблона мира",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!generatedCode.isNullOrBlank()) {
                        Text(
                            text = "Поделитесь этим кодом, чтобы друг открыл ваш мир с теми же стартовыми параметрами:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = generatedCode!!.take(120) + if (generatedCode!!.length > 120) "…" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Код мира", generatedCode))
                                copiedFeedback = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (copiedFeedback) "Код скопирован" else "Скопировать код текущего мира")
                        }
                    } else {
                        Text(
                            text = "Заполните 4 вопроса мира, чтобы создать свой код, или вставьте код друга ниже:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = importCodeInput,
                        onValueChange = { importCodeInput = it },
                        label = { Text("Импорт кода (CBW1:...)") },
                        placeholder = { Text("Вставьте код шаблона мира") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("world_code_import_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.importWorldCode(importCodeInput)) {
                            showWorldCodeDialog = false
                        }
                    },
                    enabled = importCodeInput.isNotBlank()
                ) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorldCodeDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun QuestionCard(
    number: String,
    title: String,
    prompt: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
