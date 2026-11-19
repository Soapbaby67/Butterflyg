package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ScenarioCodeCodec
import com.example.data.model.HeroSkills
import com.example.data.model.PlayerGender
import com.example.data.model.ScenarioGenre
import com.example.data.model.SessionSetupInput
import com.example.data.model.SkillType
import com.example.data.model.UniversePresets
import com.example.data.model.UniverseTemplate
import com.example.data.repository.AiModelChoice
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.NarrativePace
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
    val playerName: String = "",
    val playerGender: PlayerGender = PlayerGender.NEUTRAL,
    val skills: HeroSkills = HeroSkills(),
    val archetype: CharacterArchetype = CharacterArchetype.OC,
    val characterDetails: String = "",
    val startingPoint: String = "",
    val toneAndDifficulty: String = "Мрачный реализм без сюжетной брони: ошибки ведут к травмам, потере союзников или гибели.",
    val isGenerating: Boolean = false,
    val isGeneratingPremise: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
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
                playerName = template.defaultPlayerName,
                playerGender = template.defaultGender,
                skills = template.defaultSkills.coerced(),
                archetype = archetype,
                characterDetails = cleanDetails,
                startingPoint = template.startingPoint,
                toneAndDifficulty = template.toneAndDifficulty,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun updateUniverse(value: String) {
        _uiState.update { it.copy(universe = value, errorMessage = null) }
    }

    fun updatePlayerName(value: String) {
        _uiState.update { it.copy(playerName = value, errorMessage = null) }
    }

    fun updatePlayerGender(gender: PlayerGender) {
        _uiState.update { it.copy(playerGender = gender, errorMessage = null) }
    }

    fun updateSkill(skillType: SkillType, value: Long) {
        _uiState.update { it.copy(skills = it.skills.withSkill(skillType, value)) }
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

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun generatePremiseWithAi(genre: ScenarioGenre) {
        if (_uiState.value.isGeneratingPremise || _uiState.value.isGenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPremise = true, errorMessage = null, infoMessage = null) }
            try {
                val premise = aiRepository.generateScenarioPremise(genre)
                applySessionInput(premise)
                _uiState.update {
                    it.copy(
                        isGeneratingPremise = false,
                        infoMessage = "Завязка сгенерирована ИИ — можно подправить детали и начинать."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate scenario premise", e)
                _uiState.update {
                    it.copy(
                        isGeneratingPremise = false,
                        errorMessage = e.localizedMessage ?: "Не удалось сгенерировать завязку через ИИ."
                    )
                }
            }
        }
    }

    fun buildCurrentWorldCode(): String? {
        val current = _uiState.value
        val universe = current.universe.trim()
        val details = current.characterDetails.trim()
        val startingPoint = current.startingPoint.trim()
        val tone = current.toneAndDifficulty.trim()
        if (universe.isBlank() || details.isBlank() || startingPoint.isBlank() || tone.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Сначала заполните параметры мира, чтобы создать код приглашения.")
            }
            return null
        }
        val input = SessionSetupInput(
            title = current.title.trim().ifBlank { universe.take(48) },
            universe = universe,
            characterSummary = "${current.archetype.prefix}: $details",
            startingPoint = startingPoint,
            toneAndDifficulty = tone,
            playerName = current.playerName.trim(),
            playerGender = current.playerGender,
            skills = current.skills.coerced()
        )
        return ScenarioCodeCodec.encode(input)
    }

    fun importWorldCode(rawCode: String): Boolean {
        val decoded = ScenarioCodeCodec.decode(rawCode)
        if (decoded == null) {
            _uiState.update {
                it.copy(errorMessage = "Некорректный код мира. Ожидается код формата CBW1:...")
            }
            return false
        }
        applySessionInput(decoded)
        _uiState.update {
            it.copy(
                errorMessage = null,
                infoMessage = "Шаблон мира «${decoded.title}» импортирован."
            )
        }
        return true
    }

    private fun applySessionInput(input: SessionSetupInput) {
        val (archetype, cleanDetails) = when {
            input.characterSummary.startsWith("Попаданец") ->
                CharacterArchetype.ISEKAI to input.characterSummary.substringAfter(":").trim().ifBlank { input.characterSummary }
            input.characterSummary.startsWith("Каноничный герой") ->
                CharacterArchetype.CANON to input.characterSummary.substringAfter(":").trim().ifBlank { input.characterSummary }
            input.characterSummary.startsWith("Свой персонаж") ->
                CharacterArchetype.OC to input.characterSummary.substringAfter(":").trim().ifBlank { input.characterSummary }
            else -> CharacterArchetype.OC to input.characterSummary
        }
        _uiState.update {
            it.copy(
                selectedTemplateId = null,
                title = input.title,
                universe = input.universe,
                playerName = input.playerName,
                playerGender = input.playerGender,
                skills = input.skills.coerced(),
                archetype = archetype,
                characterDetails = cleanDetails,
                startingPoint = input.startingPoint,
                toneAndDifficulty = input.toneAndDifficulty
            )
        }
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

        val playerName = current.playerName.trim()
        val combinedCharacter = buildString {
            append(current.archetype.prefix)
            append(": ")
            if (playerName.isNotBlank() && !details.contains(playerName, ignoreCase = true)) {
                append(playerName)
                append(" — ")
            }
            append(details)
        }
        val finalTitle = current.title.trim().ifBlank {
            universe.substringBefore("(").trim().take(48)
        }

        val setupInput = SessionSetupInput(
            title = finalTitle,
            universe = universe,
            characterSummary = combinedCharacter,
            startingPoint = startingPoint,
            toneAndDifficulty = tone,
            playerName = playerName,
            playerGender = current.playerGender,
            skills = current.skills.coerced()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val appSettings = runCatching {
                    themePreferencesRepository?.settings?.first()
                }.getOrNull()
                val preferredModel = appSettings?.aiModel ?: AiModelChoice.FLASH_LITE
                val narrativePace = appSettings?.narrativePace ?: NarrativePace.BOOKISH

                val openingOutcome = aiRepository.generateOpeningScene(
                    setup = setupInput,
                    preferredModel = preferredModel,
                    narrativePace = narrativePace
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
