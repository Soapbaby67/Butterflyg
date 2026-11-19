package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Campaign
import com.example.data.model.CampaignTurn
import com.example.data.model.InventoryItem
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.PlayerFactNote
import com.example.data.repository.PlayerFactsRepository
import com.example.data.repository.ThemePreferencesRepository
import com.example.data.repository.UserAppSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameSessionData(
    val campaign: Campaign,
    val turns: List<CampaignTurn>
)

sealed interface TurnSubmissionState {
    data object Idle : TurnSubmissionState
    data class Generating(val pendingAction: String) : TurnSubmissionState
    data class Error(val message: String, val failedAction: String) : TurnSubmissionState
}

sealed interface ItemInspectionState {
    data object Idle : ItemInspectionState
    data class Loading(val itemName: String) : ItemInspectionState
    data class Result(val itemName: String, val description: String) : ItemInspectionState
    data class Error(val itemName: String, val message: String) : ItemInspectionState
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

    // Dedicated high-frequency stream so only the narrative text node recomposes during AI token streaming
    private val _streamingNarrative = MutableStateFlow("")
    val streamingNarrative: StateFlow<String> = _streamingNarrative.asStateFlow()

    private val _draftAction = MutableStateFlow("")
    val draftAction: StateFlow<String> = _draftAction.asStateFlow()

    private val _isMutatingHistory = MutableStateFlow(false)
    val isMutatingHistory: StateFlow<Boolean> = _isMutatingHistory.asStateFlow()

    private val _itemInspectionState = MutableStateFlow<ItemInspectionState>(ItemInspectionState.Idle)
    val itemInspectionState: StateFlow<ItemInspectionState> = _itemInspectionState.asStateFlow()

    private val _actionNotice = MutableStateFlow<String?>(null)
    val actionNotice: StateFlow<String?> = _actionNotice.asStateFlow()

    private var draftSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val savedDraft = runCatching {
                themePreferencesRepository.observeDraftAction(campaignId).first()
            }.getOrDefault("")
            if (savedDraft.isNotBlank() && _draftAction.value.isBlank()) {
                _draftAction.value = savedDraft
            }
        }
    }

    fun updateDraftAction(text: String) {
        _draftAction.value = text
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(250L)
            runCatching {
                themePreferencesRepository.saveDraftAction(campaignId, text)
            }
        }
    }

    fun clearDraftAction() {
        draftSaveJob?.cancel()
        _draftAction.value = ""
        viewModelScope.launch {
            runCatching { themePreferencesRepository.clearDraftAction(campaignId) }
        }
    }

    fun clearActionNotice() {
        _actionNotice.value = null
    }

    fun submitPlayerAction(
        rawAction: String,
        isReroll: Boolean = false
    ) {
        val action = rawAction.trim()
        if (action.isBlank()) return
        if (_submissionState.value is TurnSubmissionState.Generating || _isMutatingHistory.value) return

        val currentData = (sessionState.value as? UiState.Success)?.data ?: return
        executeTurnGeneration(
            campaign = currentData.campaign,
            recentTurns = currentData.turns,
            action = action,
            isReroll = isReroll
        )
    }

    private fun executeTurnGeneration(
        campaign: Campaign,
        recentTurns: List<CampaignTurn>,
        action: String,
        isReroll: Boolean
    ) {
        viewModelScope.launch {
            _streamingNarrative.value = ""
            _submissionState.value = TurnSubmissionState.Generating(action)
            try {
                val appSettings = runCatching {
                    themePreferencesRepository.settings.first()
                }.getOrDefault(UserAppSettings())
                val pinnedFacts = facts.value
                    .filter { it.isPinned }
                    .map { it.text }

                val outcome = aiRepository.generateNextTurn(
                    campaign = campaign,
                    recentTurns = recentTurns,
                    playerAction = action,
                    preferredModel = appSettings.aiModel,
                    narrativePace = appSettings.narrativePace,
                    pinnedFacts = pinnedFacts,
                    isReroll = isReroll,
                    onStreamingNarrative = { partialNarrative ->
                        if (appSettings.liveStreamingEnabled) {
                            _streamingNarrative.value = partialNarrative
                        }
                    }
                )
                campaignRepository.recordTurn(
                    campaign = campaign,
                    playerAction = action,
                    outcome = outcome
                )
                clearDraftAction()
                _streamingNarrative.value = ""
                _submissionState.value = TurnSubmissionState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process turn", e)
                _streamingNarrative.value = ""
                _submissionState.value = TurnSubmissionState.Error(
                    message = e.localizedMessage ?: "Не удалось выполнить ход. Попробуйте ещё раз.",
                    failedAction = action
                )
            }
        }
    }

    fun rollbackLastTurn(onRolledBack: (String?) -> Unit = {}) {
        if (_submissionState.value is TurnSubmissionState.Generating || _isMutatingHistory.value) return
        val currentData = (sessionState.value as? UiState.Success)?.data ?: return
        if (currentData.turns.size < 2) {
            _actionNotice.value = "Стартовую главу нельзя откатить."
            return
        }

        viewModelScope.launch {
            _isMutatingHistory.value = true
            try {
                val removedAction = campaignRepository.rollbackLastTurn(campaignId)
                _actionNotice.value = "Последний ход откачен."
                onRolledBack(removedAction)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Rollback failed", e)
                _actionNotice.value = e.localizedMessage ?: "Не удалось откатить ход."
            } finally {
                _isMutatingHistory.value = false
            }
        }
    }

    fun editLastPlayerAction() {
        rollbackLastTurn { removedAction ->
            if (!removedAction.isNullOrBlank()) {
                updateDraftAction(removedAction)
                _actionNotice.value = "Ход возвращён в поле ввода — можно отредактировать."
            }
        }
    }

    fun rerollLastTurn() {
        if (_submissionState.value is TurnSubmissionState.Generating || _isMutatingHistory.value) return
        val currentData = (sessionState.value as? UiState.Success)?.data ?: return
        if (currentData.turns.size < 2) return

        val lastTurn = currentData.turns.last()
        val actionToReplay = lastTurn.playerAction.trim()
        if (actionToReplay.isBlank()) return

        viewModelScope.launch {
            _isMutatingHistory.value = true
            try {
                campaignRepository.rollbackLastTurn(campaignId)
                val restoredCampaign = campaignRepository.getCampaign(campaignId)
                val previousTurns = currentData.turns.dropLast(1)
                _isMutatingHistory.value = false
                if (restoredCampaign != null) {
                    executeTurnGeneration(
                        campaign = restoredCampaign,
                        recentTurns = previousTurns,
                        action = actionToReplay,
                        isReroll = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isMutatingHistory.value = false
                Log.e(TAG, "Reroll scene failed", e)
                _actionNotice.value = e.localizedMessage ?: "Не удалось переиграть сцену."
            }
        }
    }

    fun forkCampaignAtTurn(
        turnNumber: Long,
        onForked: (String) -> Unit
    ) {
        if (_submissionState.value is TurnSubmissionState.Generating || _isMutatingHistory.value) return
        viewModelScope.launch {
            _isMutatingHistory.value = true
            try {
                val forkedId = campaignRepository.duplicateCampaignAtTurn(
                    sourceCampaignId = campaignId,
                    upToTurnNumber = turnNumber
                )
                _actionNotice.value = "Создана новая ветка от хода $turnNumber."
                onForked(forkedId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fork campaign failed", e)
                _actionNotice.value = e.localizedMessage ?: "Не удалось ответвить хронику."
            } finally {
                _isMutatingHistory.value = false
            }
        }
    }

    fun toggleItemEquipped(item: InventoryItem) {
        val currentData = (sessionState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                campaignRepository.toggleItemEquipped(currentData.campaign, item.resolvedId())
                _actionNotice.value = if (item.isEquipped) {
                    "Снято: ${item.name}"
                } else {
                    "Экипировано: ${item.name}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Toggle equip failed", e)
                _actionNotice.value = e.localizedMessage ?: "Не удалось обновить экипировку."
            }
        }
    }

    fun inspectItem(item: InventoryItem) {
        val currentData = (sessionState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _itemInspectionState.value = ItemInspectionState.Loading(item.name)
            try {
                val result = aiRepository.inspectItem(currentData.campaign, item)
                _itemInspectionState.value = ItemInspectionState.Result(item.name, result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _itemInspectionState.value = ItemInspectionState.Error(
                    itemName = item.name,
                    message = e.localizedMessage ?: "Не удалось осмотреть предмет."
                )
            }
        }
    }

    fun clearItemInspection() {
        _itemInspectionState.value = ItemInspectionState.Idle
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
