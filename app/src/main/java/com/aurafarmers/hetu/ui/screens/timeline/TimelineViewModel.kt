package com.aurafarmers.hetu.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {

    val timelineItems: StateFlow<List<TimelineItem>> = combine(
        repository.getAllActions(),
        repository.getAllOutcomes()
    ) { actions, outcomes ->
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        fun formatDate(isoDate: String): String {
            return try {
                val date = LocalDate.parse(isoDate)
                when (date) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
                }
            } catch (e: Exception) {
                isoDate
            }
        }

        val actionItems = actions.map { action ->
            TimelineItem(
                id = action.id,
                date = formatDate(action.date),
                type = "action",
                category = action.category,
                description = action.description,
                timestamp = action.timestamp
            )
        }
        
        val outcomeItems = outcomes.map { outcome ->
            TimelineItem(
                id = outcome.id,
                date = formatDate(outcome.date),
                type = "outcome",
                category = outcome.category,
                description = outcome.description,
                timestamp = outcome.timestamp
            )
        }
        
        (actionItems + outcomeItems).sortedByDescending { it.timestamp }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

data class TimelineItem(
    val id: Long,
    val date: String,
    val type: String,
    val category: String,
    val description: String,
    val timestamp: Long
)
