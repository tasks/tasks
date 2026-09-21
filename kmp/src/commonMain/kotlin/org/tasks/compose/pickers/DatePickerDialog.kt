package org.tasks.compose.pickers

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.tasks.time.toLocalDateMillis
import org.tasks.time.toUtcDateMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.ok

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Long,
    displayMode: DisplayMode,
    setDisplayMode: (DisplayMode) -> Unit,
    selected: (Long) -> Unit,
    dismiss: () -> Unit,
) {
    val initialDateUTC by remember(initialDate) {
        derivedStateOf {
            initialDate.toUtcDateMillis()
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateUTC,
        initialDisplayMode = displayMode,
    )
    LaunchedEffect(datePickerState.displayMode) {
        setDisplayMode(datePickerState.displayMode)
    }
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = { dismiss() },
        dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(Res.string.cancel))
                }
        },
        confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState
                            .selectedDateMillis
                            ?.let { selected(it.toLocalDateMillis()) }
                    }
                ) {
                    Text(text = stringResource(Res.string.ok))
                }
            }
    ) {
        DatePicker(state = datePickerState)
    }
}
