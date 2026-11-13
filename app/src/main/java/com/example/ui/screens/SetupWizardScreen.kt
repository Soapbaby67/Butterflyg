package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UniversePresets
import com.example.ui.viewmodel.CharacterArchetype
import com.example.ui.viewmodel.SetupWizardViewModel

private val tonePresets = listOf(
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

    LaunchedEffect(initialTemplateId) {
        viewModel.applyTemplateById(initialTemplateId)
    }

    LaunchedEffect(state.createdCampaignId) {
        val createdId = state.createdCampaignId
        if (!createdId.isNullOrBlank()) {
            onCampaignCreated(createdId)
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
                color = MaterialTheme.colorScheme.surface,
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
                        onClick = { viewModel.startChronicle() },
                        enabled = !state.isGenerating,
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
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
                            onClick = { viewModel.applyTemplate(template) },
                            label = { Text(template.title) }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Название хроники (необязательно)") },
                    placeholder = { Text("Например: Тень Конохи") },
                    singleLine = true,
                    enabled = !state.isGenerating,
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
                        enabled = !state.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_universe_input")
                    )
                }

                // Question 2: Character
                QuestionCard(
                    number = "2",
                    title = "Персонаж",
                    prompt = "Кем вы играете? Укажите имя, навыки, слабости и стартовые данные."
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CharacterArchetype.entries.forEach { archetype ->
                            FilterChip(
                                selected = state.archetype == archetype,
                                onClick = { viewModel.updateArchetype(archetype) },
                                enabled = !state.isGenerating,
                                label = { Text(archetype.label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.characterDetails,
                        onValueChange = viewModel::updateCharacterDetails,
                        placeholder = { Text("Имя, возраст, способности, снаряжение, слабые стороны…") },
                        minLines = 3,
                        enabled = !state.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_character_input")
                    )
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
                        enabled = !state.isGenerating,
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
                                0 -> "Мрачный реализм"
                                1 -> "Каноничный сёнэн"
                                2 -> "Интриги"
                                else -> "Детектив"
                            }
                            FilterChip(
                                selected = state.toneAndDifficulty == preset,
                                onClick = { viewModel.updateToneAndDifficulty(preset) },
                                enabled = !state.isGenerating,
                                label = { Text(shortLabel) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.toneAndDifficulty,
                        onValueChange = viewModel::updateToneAndDifficulty,
                        minLines = 2,
                        enabled = !state.isGenerating,
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
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
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
