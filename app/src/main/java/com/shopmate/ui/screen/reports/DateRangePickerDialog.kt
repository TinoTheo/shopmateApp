package com.shopmate.ui.screen.reports

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shopmate.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3 date-range picker used when the "Custom" period chip is tapped. Selection is confirmed
 * only when both a start and end date have been chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState()
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis
    val confirmEnabled = startMillis != null && endMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = {
                    val start = startMillis?.toLocalDate()
                    val end = endMillis?.toLocalDate()
                    if (start != null && end != null) onConfirm(start, end)
                },
            ) {
                Text(stringResource(R.string.report_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DateRangePicker(state = state)
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
