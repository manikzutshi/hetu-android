package com.aurafarmers.hetu.ui.screens.timeline

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineEntry(
    val id: Long,
    val date: String,
    val type: String, // "action" or "outcome"
    val category: String,
    val description: String,
    val timestamp: Long
)

data class TimelineUiState(
    val entries: List<TimelineEntry> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val actionRepository: ActionRepository,
    private val outcomeRepository: OutcomeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    init {
        loadTimeline()
    }
    
    private fun loadTimeline() {
        viewModelScope.launch {
            combine(
                actionRepository.getAllActions(),
                outcomeRepository.getAllOutcomes()
            ) { actions, outcomes ->
                val actionEntries = actions.map { action ->
                    TimelineEntry(
                        id = action.id,
                        date = formatDate(action.date),
                        type = "action",
                        category = action.category,
                        description = action.description,
                        timestamp = action.timestamp
                    )
                }
                
                val outcomeEntries = outcomes.map { outcome ->
                    TimelineEntry(
                        id = outcome.id,
                        date = formatDate(outcome.date),
                        type = "outcome",
                        category = outcome.category,
                        description = outcome.description,
                        timestamp = outcome.timestamp
                    )
                }
                
                (actionEntries + outcomeEntries).sortedByDescending { it.timestamp }
            }.collect { entries ->
                _uiState.value = TimelineUiState(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }
    
    private fun formatDate(isoDate: String): String {
        // Simple date formatting - could be enhanced with proper relative dates
        val today = java.time.LocalDate.now().toString()
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        
        return when (isoDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.parse(isoDate),
                    java.time.LocalDate.now()
                )
                if (daysAgo < 7) "$daysAgo days ago" else isoDate
            }
        }
    }
    
    suspend fun deleteAction(id: Long) {
        actionRepository.getActionById(id)?.let { action ->
            actionRepository.delete(action)
        }
    }
    
    suspend fun deleteOutcome(id: Long) {
        outcomeRepository.getOutcomeById(id)?.let { outcome ->
            outcomeRepository.delete(outcome)
        }
    }
}
