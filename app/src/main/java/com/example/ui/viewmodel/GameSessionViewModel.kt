package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.repository.AiModelChoice
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.PlayerFactNote
import com.example.data.repository.PlayerFactsRepository
import com.example.data.repository.ThemePreferencesRepository
import com.example.data.repository.UserAppSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameSessionData(
    val campaign: Campaign,
    val turns: List<CampaignTurn>
)

sealed interface TurnSubmissionState {
    data object Idle : TurnSubmissionState
    data class Generating(
        val pendingAction: String,
        val streamingNarrative: String = ""
    ) : TurnSubmissionState

    data class Error(val message: String, val failedAction: String) : TurnSubmissionState
}

class GameSessionViewModel(
    private val campaignRepository: CampaignRepository,
    private val aiRepository: GameMasterAiRepository,
    private val factsRepository: PlayerFactsRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val currentUserId: String,
    private val campaignId: String
) : ViewModel() {

    val sessionState: StateFlow<UiState<GameSessionData>> = combine(
        campaignRepository.observeCampaign(currentUserId, campaignId),
        campaignRepository.observeTurns(currentUserId, campaignId)
    ) { campaign, turns ->
        if (campaign == null) {
            UiState.Error("Хроника не найдена или была удалена.")
        } else {
            UiState.Success(GameSessionData(campaign = campaign, turns = turns))
        }
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing game session", rootError)
        emit(UiState.Error(rootError.message ?: "Ошибка загрузки игровой сессии."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val facts: StateFlow<List<PlayerFactNote>> = factsRepository.observeFacts(campaignId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _submissionState = MutableStateFlow<TurnSubmissionState>(TurnSubmissionState.Idle)
    val submissionState: StateFlow<TurnSubmissionState> = _submissionState.asStateFlow()

    fun submitPlayerAction(rawAction: String) {
        val action = rawAction.trim()
        if (action.isBlank()) return
        if (_submissionState.value is TurnSubmissionState.Generating) return

        val currentData = (sessionState.value as? UiState.Success)?.data ?: return

        viewModelScope.launch {
            _submissionState.value = TurnSubmissionState.Generating(action)
            try {
                val appSettings = runCatching {
                    themePreferencesRepository.settings.first()
                }.getOrDefault(UserAppSettings())
                val pinnedFacts = facts.value
                    .filter { it.isPinned }
                    .map { it.text }

                val outcome = aiRepository.generateNextTurn(
                    campaign = currentData.campaign,
                    recentTurns = currentData.turns,
                    playerAction = action,
                    preferredModel = appSettings.aiModel,
                    pinnedFacts = pinnedFacts,
                    onStreamingNarrative = { partialNarrative ->
                        if (appSettings.liveStreamingEnabled) {
                            _submissionState.update { state ->
                                if (state is TurnSubmissionState.Generating) {
                                    state.copy(streamingNarrative = partialNarrative)
                                } else {
                                    state
                                }
                            }
                        }
                    }
                )
                campaignRepository.recordTurn(
                    campaign = currentData.campaign,
                    playerAction = action,
                    outcome = outcome
                )
                _submissionState.value = TurnSubmissionState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process turn", e)
                _submissionState.value = TurnSubmissionState.Error(
                    message = e.localizedMessage ?: "Не удалось выполнить ход. Попробуйте ещё раз.",
                    failedAction = action
                )
            }
        }
    }

    fun addFact(text: String) {
        viewModelScope.launch {
            factsRepository.addFact(campaignId, text)
        }
    }

    fun toggleFactPinned(factId: String) {
        viewModelScope.launch {
            factsRepository.toggleFactPinned(campaignId, factId)
        }
    }

    fun deleteFact(factId: String) {
        viewModelScope.launch {
            factsRepository.deleteFact(campaignId, factId)
        }
    }

    fun dismissSubmissionError() {
        _submissionState.value = TurnSubmissionState.Idle
    }

    private companion object {
        const val TAG = "GameSessionVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
