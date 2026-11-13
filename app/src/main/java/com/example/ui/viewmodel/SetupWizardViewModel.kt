package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SessionSetupInput
import com.example.data.model.UniversePresets
import com.example.data.model.UniverseTemplate
import com.example.data.repository.AiModelChoice
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CharacterArchetype(val label: String, val prefix: String) {
    CANON("Каноничный герой", "Каноничный герой"),
    OC("Свой персонаж (OC)", "Свой персонаж (OC)"),
    ISEKAI("Попаданец со знанием сюжета", "Попаданец со знанием сюжета")
}

data class SetupWizardUiState(
    val selectedTemplateId: String? = null,
    val title: String = "",
    val universe: String = "",
    val archetype: CharacterArchetype = CharacterArchetype.OC,
    val characterDetails: String = "",
    val startingPoint: String = "",
    val toneAndDifficulty: String = "Мрачный реализм без сюжетной брони: ошибки ведут к травмам, потере союзников или гибели.",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val createdCampaignId: String? = null
)

class SetupWizardViewModel(
    private val campaignRepository: CampaignRepository,
    private val aiRepository: GameMasterAiRepository,
    private val themePreferencesRepository: ThemePreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun applyTemplateById(templateId: String?) {
        if (templateId.isNullOrBlank()) return
        val template = UniversePresets.templates.firstOrNull { it.id == templateId } ?: return
        applyTemplate(template)
    }

    fun applyTemplate(template: UniverseTemplate) {
        val (archetype, cleanDetails) = when {
            template.characterSummary.startsWith("Попаданец:") ->
                CharacterArchetype.ISEKAI to template.characterSummary.removePrefix("Попаданец:").trim()
            template.characterSummary.startsWith("Свой персонаж (OC):") ->
                CharacterArchetype.OC to template.characterSummary.removePrefix("Свой персонаж (OC):").trim()
            template.characterSummary.startsWith("Каноничный герой:") ->
                CharacterArchetype.CANON to template.characterSummary.removePrefix("Каноничный герой:").trim()
            else -> CharacterArchetype.OC to template.characterSummary
        }
        _uiState.update {
            it.copy(
                selectedTemplateId = template.id,
                title = template.title,
                universe = template.universe,
                archetype = archetype,
                characterDetails = cleanDetails,
                startingPoint = template.startingPoint,
                toneAndDifficulty = template.toneAndDifficulty,
                errorMessage = null
            )
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun updateUniverse(value: String) {
        _uiState.update { it.copy(universe = value, errorMessage = null) }
    }

    fun updateArchetype(archetype: CharacterArchetype) {
        _uiState.update { it.copy(archetype = archetype, errorMessage = null) }
    }

    fun updateCharacterDetails(value: String) {
        _uiState.update { it.copy(characterDetails = value, errorMessage = null) }
    }

    fun updateStartingPoint(value: String) {
        _uiState.update { it.copy(startingPoint = value, errorMessage = null) }
    }

    fun updateToneAndDifficulty(value: String) {
        _uiState.update { it.copy(toneAndDifficulty = value, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startChronicle() {
        val current = _uiState.value
        if (current.isGenerating) return

        val universe = current.universe.trim()
        val details = current.characterDetails.trim()
        val startingPoint = current.startingPoint.trim()
        val tone = current.toneAndDifficulty.trim()

        if (universe.isBlank() || details.isBlank() || startingPoint.isBlank() || tone.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Заполните все 4 вопроса настройки сессии, чтобы мастер открыл первую страницу.")
            }
            return
        }

        val combinedCharacter = "${current.archetype.prefix}: $details"
        val finalTitle = current.title.trim().ifBlank {
            universe.substringBefore("(").trim().take(48)
        }

        val setupInput = SessionSetupInput(
            title = finalTitle,
            universe = universe,
            characterSummary = combinedCharacter,
            startingPoint = startingPoint,
            toneAndDifficulty = tone
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val preferredModel = runCatching {
                    themePreferencesRepository?.settings?.first()?.aiModel
                }.getOrNull() ?: AiModelChoice.FLASH_LITE

                val openingOutcome = aiRepository.generateOpeningScene(
                    setup = setupInput,
                    preferredModel = preferredModel
                )
                val campaignId = campaignRepository.createCampaignWithOpeningTurn(
                    setup = setupInput,
                    openingOutcome = openingOutcome
                )
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        createdCampaignId = campaignId
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start chronicle", e)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.localizedMessage ?: "Не удалось создать стартовую сцену."
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "SetupWizardVM"
    }
}
