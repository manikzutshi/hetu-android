package com.aurafarmers.hetu.ui.screens.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import com.aurafarmers.hetu.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {

    fun saveAction(
        description: String,
        category: String,
        expectation: String?,
        checkInDays: Int?
    ) {
        viewModelScope.launch {
            repository.insertAction(
                ActionEntity(
                    description = description,
                    category = category,
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    expectation = expectation,
                    checkInDays = checkInDays,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveOutcome(
        description: String,
        category: String
    ) {
        viewModelScope.launch {
            repository.insertOutcome(
                OutcomeEntity(
                    description = description,
                    category = category,
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
