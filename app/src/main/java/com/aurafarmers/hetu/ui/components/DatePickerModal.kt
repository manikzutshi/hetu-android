package com.aurafarmers.hetu.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                // Allow selectable range: 30 days ago to Today (inclusive)
                return utcTimeMillis in thirtyDaysAgo..now
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val date = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC")) // DatePicker uses UTC
                            .toLocalDate()
                        onDateSelected(date)
                    } else {
                        onDateSelected(null)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
