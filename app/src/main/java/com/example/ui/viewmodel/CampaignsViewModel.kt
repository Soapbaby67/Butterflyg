package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Campaign
import com.example.data.repository.CampaignRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CampaignsViewModel(
    private val repository: CampaignRepository,
    private val currentUserId: String
) : ViewModel() {

    val campaignsState: StateFlow<UiState<List<Campaign>>> = repository.observeCampaigns(currentUserId)
        .map<List<Campaign>, UiState<List<Campaign>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing campaigns", rootError)
            emit(UiState.Error(rootError.message ?: "Не удалось загрузить список хроник."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _forkingCampaignId = MutableStateFlow<String?>(null)
    val forkingCampaignId: StateFlow<String?> = _forkingCampaignId.asStateFlow()

    fun deleteCampaign(campaignId: String) {
        viewModelScope.launch {
            _deleteError.value = null
            try {
                repository.deleteCampaign(campaignId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete campaign $campaignId", e)
                _deleteError.value = e.localizedMessage ?: "Не удалось удалить хронику."
            }
        }
    }

    fun forkCampaign(
        campaign: Campaign,
        onForked: (String) -> Unit
    ) {
        if (_forkingCampaignId.value != null) return
        viewModelScope.launch {
            _forkingCampaignId.value = campaign.id
            _deleteError.value = null
            try {
                val forkedId = repository.duplicateCampaignAtTurn(
                    sourceCampaignId = campaign.id,
                    upToTurnNumber = campaign.turnCount.coerceAtLeast(1L)
                )
                onForked(forkedId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fork campaign ${campaign.id}", e)
                _deleteError.value = e.localizedMessage ?: "Не удалось создать ветку хроники."
            } finally {
                _forkingCampaignId.value = null
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    private companion object {
        const val TAG = "CampaignsVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
