package com.aurafarmers.hetu.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {

    val stats: StateFlow<InsightsStats> = combine(
        repository.getAllActions(),
        repository.getAllOutcomes()
    ) { actions, outcomes ->
        InsightsStats(
            totalEntries = actions.size + outcomes.size,
            daysTracked = (actions.map { it.date } + outcomes.map { it.date }).distinct().size,
            patternsFound = 0 // Real analysis not implemented yet
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsStats()
    )
}

data class InsightsStats(
    val totalEntries: Int = 0,
    val daysTracked: Int = 0,
    val patternsFound: Int = 0
)
