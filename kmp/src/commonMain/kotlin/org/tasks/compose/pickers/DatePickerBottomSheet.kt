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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.ok

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    sheetState: SheetState,
    showButtons: Boolean,
    cancel: () -> Unit,
    accept: () -> Unit,
    setDisplayMode: (DisplayMode) -> Unit,
    dateShortcuts: @Composable ColumnScope.() -> Unit,
    timeShortcuts: @Composable ColumnScope.() -> Unit,
    state: DatePickerState,
) {
    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        sheetState = sheetState,
        onDismissRequest = { accept() },
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
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                state.displayMode = if (state.displayMode == DisplayMode.Input) {
                                    DisplayMode.Picker
                                } else {
                                    DisplayMode.Input
                                }
                                setDisplayMode(state.displayMode)
                            },
                        ) {
                            Icon(
                                imageVector = if (state.displayMode == DisplayMode.Input) {
                                    Icons.Outlined.CalendarMonth
                                } else {
                                    Icons.Outlined.Keyboard
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { cancel() }
                        ) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { accept() }
                        ) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                }
            }
        }
    }
}

private const val MORNING = 9 * 60 * 60 * 1000
private const val AFTERNOON = 13 * 60 * 60 * 1000
private const val EVENING = 17 * 60 * 60 * 1000
private const val NIGHT = 20 * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Due date picker - Light")
@Preview(name = "Due date picker - Dark", uiMode = PREVIEW_NIGHT_MODE)
@Composable
private fun DueDatePickerPreview() {
    TasksTheme {
        val today = currentTimeMillis().startOfDay()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true).apply {
            runBlocking { show() }
        }
        DatePickerBottomSheet(
            sheetState = sheetState,
            state = rememberDatePickerState(),
            showButtons = true,
            setDisplayMode = {},
            cancel = {},
            accept = {},
            dateShortcuts = {
                DueDateShortcuts(
                    today = today,
                    tomorrow = today.plusDays(1),
                    nextWeek = today.plusDays(7),
                    selected = today,
                    showNoDate = true,
                    selectedDay = {},
                    clearDate = {},
                )
            },
            timeShortcuts = {
                TimeShortcuts(
                    day = NO_DAY,
                    selected = NO_TIME,
                    morning = MORNING,
                    afternoon = AFTERNOON,
                    evening = EVENING,
                    night = NIGHT,
                    is24HourFormat = true,
                    selectedMillisOfDay = {},
                    pickTime = {},
                    clearTime = {},
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Start date picker - Light")
@Preview(name = "Start date picker - Dark", uiMode = PREVIEW_NIGHT_MODE)
@Composable
private fun StartDatePickerPreview() {
    TasksTheme {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true).apply {
            runBlocking { show() }
        }
        DatePickerBottomSheet(
            sheetState = sheetState,
            state = rememberDatePickerState(),
            showButtons = true,
            setDisplayMode = {},
            cancel = {},
            accept = {},
            dateShortcuts = {
                StartDateShortcuts(
                    selected = DUE_DATE,
                    selectedDay = {},
                    selectedDayTime = { _, _ -> },
                    showDueDate = true,
                    clearDate = {},
                )
            },
            timeShortcuts = {
                TimeShortcuts(
                    day = DUE_DATE,
                    selected = NO_TIME,
                    morning = MORNING,
                    afternoon = AFTERNOON,
                    evening = EVENING,
                    night = NIGHT,
                    is24HourFormat = true,
                    selectedMillisOfDay = {},
                    pickTime = {},
                    clearTime = {},
                )
            },
        )
    }
}
