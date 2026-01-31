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
        checkInDays: Int?,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            // If backdating, set time to noon. If today, use current time.
            val timestamp = if (date == LocalDate.now()) {
                System.currentTimeMillis()
            } else {
                date.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            }

            repository.insertAction(
                ActionEntity(
                    description = description,
                    category = category,
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    expectation = expectation,
                    checkInDays = checkInDays,
                    timestamp = timestamp
                )
            )
        }
    }

    fun saveOutcome(
        description: String,
        category: String,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            val timestamp = if (date == LocalDate.now()) {
                System.currentTimeMillis()
            } else {
                date.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            }

            repository.insertOutcome(
                OutcomeEntity(
                    description = description,
                    category = category,
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    timestamp = timestamp
                )
            )
        }
    }
}
