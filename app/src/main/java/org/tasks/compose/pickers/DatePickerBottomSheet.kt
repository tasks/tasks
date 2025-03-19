package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    showButtons: Boolean,
    dismiss: () -> Unit,
    accept: () -> Unit,
    dateShortcuts: @Composable ColumnScope.() -> Unit,
    timeShortcuts: @Composable ColumnScope.() -> Unit,
    state: DatePickerState,
) {
    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = { dismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                DatePicker(
                    state = state,
                    showModeToggle = false,
                    title = {},
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    headline = {
                        DatePickerShortcuts(
                            dateShortcuts = dateShortcuts,
                            timeShortcuts = timeShortcuts,
                        )
                    },
                )
                if (showButtons) {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
            if (showButtons) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { dismiss() }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { accept() }
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
