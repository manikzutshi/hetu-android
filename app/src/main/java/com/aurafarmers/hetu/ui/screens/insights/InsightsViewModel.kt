package com.aurafarmers.hetu.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.entity.InsightEntity
import com.aurafarmers.hetu.data.repository.ActionRepository
import com.aurafarmers.hetu.data.repository.InsightRepository
import com.aurafarmers.hetu.data.repository.OutcomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val insights: List<InsightEntity> = emptyList(),
    val totalEntries: Int = 0,
    val totalDays: Int = 0,
    val totalPatterns: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightRepository: InsightRepository,
    private val actionRepository: ActionRepository,
    private val outcomeRepository: OutcomeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    
    init {
        loadInsights()
        loadStats()
    }
    
    private fun loadInsights() {
        viewModelScope.launch {
            insightRepository.getAllInsights().collect { insights ->
                _uiState.value = _uiState.value.copy(
                    insights = insights,
                    totalPatterns = insights.size,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            val actionCount = actionRepository.getCount()
            val outcomeCount = outcomeRepository.getCount()
            
            actionRepository.getDistinctDates().collect { dates ->
                _uiState.value = _uiState.value.copy(
                    totalEntries = actionCount + outcomeCount,
                    totalDays = dates.size
                )
            }
        }
    }
    
    fun generateSampleInsights() {
        // Generate sample insights if none exist
        viewModelScope.launch {
            val count = insightRepository.getCount()
            if (count == 0) {
                val sampleInsights = listOf(
                    InsightEntity(
                        title = "Sleep & Energy",
                        description = "When you log 'no screens after 9pm', you report better energy the next day.",
                        emoji = "😴",
                        confidence = "high",
                        actionCategory = "😴 Sleep",
                        outcomeCategory = "⚡ Energy",
                        occurrences = 3
                    ),
                    InsightEntity(
                        title = "Meditation & Mood",
                        description = "Morning meditation entries are often followed by improved mood logs.",
                        emoji = "🧘",
                        confidence = "medium",
                        actionCategory = "🧘 Wellness",
                        outcomeCategory = "😊 Mood",
                        occurrences = 2
                    ),
                    InsightEntity(
                        title = "Diet Pattern",
                        description = "You tend to log lower energy on days when you skip tracking meals.",
                        emoji = "🥗",
                        confidence = "needs_data",
                        actionCategory = "🥗 Food",
                        outcomeCategory = "⚡ Energy",
                        occurrences = 1
                    )
                )
                
                sampleInsights.forEach { insight ->
                    insightRepository.insert(insight)
                }
            }
        }
    }
}
