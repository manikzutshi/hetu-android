package com.aurafarmers.hetu.ui.screens.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import com.aurafarmers.hetu.data.repository.ActionRepository
import com.aurafarmers.hetu.data.repository.OutcomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TrackUiState(
    val isAction: Boolean = true,
    val description: String = "",
    val selectedCategory: String? = null,
    val expectation: String = "",
    val daysUntilCheck: Int = 3,
    val rating: Int? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val actionRepository: ActionRepository,
    private val outcomeRepository: OutcomeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()
    
    fun setIsAction(isAction: Boolean) {
        _uiState.value = _uiState.value.copy(isAction = isAction, selectedCategory = null)
    }
    
    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
    
    fun setExpectation(expectation: String) {
        _uiState.value = _uiState.value.copy(expectation = expectation)
    }
    
    fun setDaysUntilCheck(days: Int) {
        _uiState.value = _uiState.value.copy(daysUntilCheck = days)
    }
    
    fun setRating(rating: Int) {
        _uiState.value = _uiState.value.copy(rating = rating)
    }
    
    fun save() {
        val state = _uiState.value
        if (state.description.isBlank() || state.selectedCategory == null) {
            _uiState.value = state.copy(error = "Please fill in all required fields")
            return
        }
        
        _uiState.value = state.copy(isSaving = true, error = null)
        
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                if (state.isAction) {
                    val action = ActionEntity(
                        description = state.description,
                        category = state.selectedCategory,
                        date = today,
                        expectation = state.expectation.ifBlank { null },
                        checkInDays = state.daysUntilCheck
                    )
                    actionRepository.insert(action)
                } else {
                    val outcome = OutcomeEntity(
                        description = state.description,
                        category = state.selectedCategory,
                        date = today,
                        rating = state.rating
                    )
                    outcomeRepository.insert(outcome)
                }
                
                _uiState.value = state.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = state.copy(isSaving = false, error = e.message ?: "Failed to save")
            }
        }
    }
    
    fun reset() {
        _uiState.value = TrackUiState()
    }
}
