package com.aurafarmers.hetu.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.repository.ActionRepository
import com.aurafarmers.hetu.data.repository.InsightRepository
import com.aurafarmers.hetu.data.repository.OutcomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalEntries: Int = 0,
    val totalDays: Int = 0,
    val totalInsights: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val actionRepository: ActionRepository,
    private val outcomeRepository: OutcomeRepository,
    private val insightRepository: InsightRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            val actionCount = actionRepository.getCount()
            val outcomeCount = outcomeRepository.getCount()
            val insightCount = insightRepository.getCount()
            
            // Collect distinct dates
            actionRepository.getDistinctDates().collect { dates ->
                _uiState.value = HomeUiState(
                    totalEntries = actionCount + outcomeCount,
                    totalDays = dates.size,
                    totalInsights = insightCount,
                    isLoading = false
                )
            }
        }
    }
    
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadStats()
    }
}
