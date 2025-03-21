package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    millisOfDay: Int,
    is24Hour: Boolean,
    initialDisplayMode: DisplayMode,
    setDisplayMode: (DisplayMode) -> Unit,
    selected: (Int) -> Unit,
    dismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = millisOfDay / (60 * 60_000),
        initialMinute = (millisOfDay / (60_000)) % 60,
        is24Hour = is24Hour
    )
    var displayMode by remember { mutableStateOf(initialDisplayMode) }
    val layoutType = with(LocalConfiguration.current) {
        if (screenHeightDp < screenWidthDp) {
            TimePickerLayoutType.Horizontal
        } else {
            TimePickerLayoutType.Vertical
        }
    }
    BasicAlertDialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = layoutType == TimePickerLayoutType.Vertical)
    ) {
        Surface(
            shape = RoundedCornerShape(28.0.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                // Wrap the content with a Box and Modifier.weight(1f) to ensure that any "confirm"
                // and "dismiss" buttons are not pushed out of view when running on small screens,
                // or when nesting a DateRangePicker.
                // Fill is false to support collapsing the dialog's height when switching to input
                // mode.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center,
                ) {
                    if (displayMode == DisplayMode.Input) {
                        TimeInput(
                            state = state,
                            colors = TimePickerDefaults.colors(
                                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    } else {
                        TimePicker(
                            state = state,
                            layoutType = layoutType,
                            colors = TimePickerDefaults.colors(
                                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                // Buttons
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 8.dp, end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                displayMode = if (displayMode == DisplayMode.Input) {
                                    DisplayMode.Picker
                                } else {
                                    DisplayMode.Input
                                }
                                setDisplayMode(displayMode)
                            },
                        ) {
                            Icon(
                                imageVector = if (displayMode == DisplayMode.Input) {
                                    Icons.Outlined.Schedule
                                } else {
                                    Icons.Outlined.Keyboard
                                },
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = dismiss) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                selected(state.hour * 60 * 60_000 + state.minute * 60_000)
                                dismiss()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
