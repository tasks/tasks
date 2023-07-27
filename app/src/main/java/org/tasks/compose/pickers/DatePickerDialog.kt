package org.tasks.compose.pickers

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.time.DateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Long,
    selected: (Long) -> Unit,
    dismiss: () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        val initialDateUTC by remember(initialDate) {
            derivedStateOf {
                DateTime(initialDate).toUTC().millis
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateUTC,
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { dismiss() },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState
                            .selectedDateMillis
                            ?.let { selected(it - DateTime(it).offset) }
                        dismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DatePickerPreview() {
    MdcTheme {
        DatePickerDialog(
            initialDate = DateTime().plusDays(1).millis,
            selected = {},
            dismiss = {}
        )
    }
}