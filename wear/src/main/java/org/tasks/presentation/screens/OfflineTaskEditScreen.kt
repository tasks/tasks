/**
 * OfflineTaskEditScreen.kt — Create / Edit screen for a single task on Wear OS.
 *
 * ## Layout (top → bottom inside a [ScalingLazyColumn]):
 *
 * 1. **Connection status** — small banner showing phone connectivity.
 * 2. **Title field** — inline [BasicTextField]; tapping opens the soft keyboard.
 *    Pressing the IME "Done" action saves the text and dismisses the keyboard.
 * 3. **Notes field** — same pattern as title.
 * 4. **Due date chip** — opens [WearDatePicker] (day / month / year wheels with haptic).
 * 5. **Due time chip** — opens [WearTimePicker] (24 h hour / minute wheels with haptic).
 * 6. **Reminder toggle** — enables/disables the notification for this task.
 * 7. **Save button** — persists the task to Room and queues a sync operation.
 * 8. **Delete button** — only shown when editing an existing task.
 *
 * ## Date/Time pickers
 * [WearDatePicker] and [WearTimePicker] are custom Wear Compose pickers
 * that use the Horologist [Picker] component with rotary-scroll support
 * and haptic feedback on value changes.
 *
 * ## Keyboard handling
 * The screen uses `imeOptions = IME_ACTION_DONE` so that the physical
 * hardware enter key on some Wear keyboards triggers a save-and-close
 * instead of inserting a newline.
 *
 * @see OfflineTaskEditViewModel — drives the state for this screen.
 * @see WearDatePicker             — date selection with day/month/year wheels.
 * @see WearTimePicker             — 24-hour time selection with hour/minute wheels.
 */
package org.tasks.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.rememberPickerState
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import org.tasks.presentation.components.Checkbox
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Task edit screen that works fully offline.
 * Saves to local database and syncs when connected.
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OfflineTaskEditScreen(
    uiState: OfflineEditUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleCompleted: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onSetDueDate: (Long?) -> Unit = {},
    onSetDueTime: (Long?) -> Unit = {},
    onToggleReminder: () -> Unit = {},
    onShowDatePicker: () -> Unit = {},
    onShowTimePicker: () -> Unit = {},
    onClearDueDate: () -> Unit = {},
    onDismissDatePicker: () -> Unit = {},
    onDismissTimePicker: () -> Unit = {},
) {
    // Show date picker dialog if requested
    if (uiState.showDatePicker) {
        WearDatePicker(
            initialDate = uiState.dueDate,
            onDateSelected = { date ->
                onSetDueDate(date)
                onDismissDatePicker()
            },
            onDismiss = onDismissDatePicker,
        )
        return
    }

    // Show time picker dialog if requested
    if (uiState.showTimePicker) {
        WearTimePicker(
            initialTime = uiState.dueTime,
            onTimeSelected = { time ->
                onSetDueTime(time)
                onDismissTimePicker()
            },
            onDismiss = onDismissTimePicker,
        )
        return
    }
    val columnState = rememberResponsiveColumnState()


    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            // Title with connection status
            item(key = "offline_edit_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.taskId != null) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.title3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Connection indicator
                    Icon(
                        imageVector = if (uiState.isConnected)
                            Icons.Outlined.Cloud
                        else
                            Icons.Outlined.CloudOff,
                        contentDescription = if (uiState.isConnected) "Connected" else "Offline",
                        modifier = Modifier.size(14.dp),
                        tint = if (uiState.isConnected)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }

            // Checkbox for completion status
            item(key = "offline_edit_checkbox") {
                Checkbox(
                    completed = uiState.completed,
                    repeating = uiState.repeating,
                    priority = uiState.priority,
                    toggleComplete = onToggleCompleted
                )
            }

            // Title input - inline text field
            item(key = "offline_edit_title_input") {
                val keyboardController = LocalSoftwareKeyboardController.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                    BasicTextField(
                        value = uiState.title,
                        onValueChange = { newValue ->
                            // Remove newlines - Enter should save, not add newline
                            onTitleChange(newValue.replace("\n", ""))
                        },
                        textStyle = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.title.isEmpty()) {
                                    Text(
                                        text = "Enter title...",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    // Underline
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                    )
                }
            }

            // Notes input - inline text field
            item(key = "offline_edit_notes_input") {
                val keyboardController = LocalSoftwareKeyboardController.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                    BasicTextField(
                        value = uiState.notes,
                        onValueChange = { newValue ->
                            // Remove newlines - Enter should save, not add newline
                            onNotesChange(newValue.replace("\n", ""))
                        },
                        textStyle = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.notes.isEmpty()) {
                                    Text(
                                        text = "Add notes...",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    // Underline
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                    )
                }
            }

            // Due date section
            item(key = "offline_edit_duedate_label") {
                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Due date chip
            item(key = "offline_edit_duedate_chip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Chip(
                        onClick = onShowDatePicker,
                        label = {
                            Text(
                                text = uiState.dueDate?.let { formatDate(it) } ?: "Set date",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Date",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.dueDate != null) {
                        Button(
                            onClick = onClearDueDate,
                            colors = ButtonDefaults.iconButtonColors(),
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // Due time chip (only show if date is set)
            if (uiState.dueDate != null) {
                item(key = "offline_edit_duetime_chip") {
                    Chip(
                        onClick = onShowTimePicker,
                        label = {
                            Text(
                                text = uiState.dueTime?.let { formatTime(it) } ?: "Set time",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = "Time",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Reminder toggle
            item(key = "offline_edit_reminder_toggle") {
                ToggleChip(
                    checked = uiState.reminder,
                    onCheckedChange = { onToggleReminder() },
                    label = {
                        Text(
                            text = "Reminder",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    appIcon = {
                        Icon(
                            imageVector = if (uiState.reminder)
                                Icons.Outlined.Notifications
                            else
                                Icons.Outlined.NotificationsOff,
                            contentDescription = "Reminder",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    toggleControl = {
                        Switch(
                            checked = uiState.reminder,
                        )
                    },
                    colors = ToggleChipDefaults.toggleChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            // Save button
            item(key = "offline_edit_save_button") {
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving && uiState.title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary,
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (uiState.isConnected) "Save" else "Save (offline)",
                            maxLines = 1,
                        )
                    }
                }
            }

            // Delete button (only for existing tasks)
            if (uiState.taskId != null) {
                item(key = "offline_edit_delete_button") {
                    Button(
                        onClick = onDelete,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error,
                            contentColor = MaterialTheme.colors.onError,
                        )
                    ) {
                        Text(
                            text = "Delete",
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format a timestamp as a date string.
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Format a timestamp as a time string.
 */
private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Wear OS Date Picker using Picker component with haptic feedback.
 */
@Composable
fun WearDatePicker(
    initialDate: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val calendar = remember {
        Calendar.getInstance().apply {
            initialDate?.let { timeInMillis = it }
        }
    }

    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // Create picker states
    val yearState = rememberPickerState(
        initialNumberOfOptions = 10, // 10 years range
        initiallySelectedOption = 0, // Current year
    )
    val monthState = rememberPickerState(
        initialNumberOfOptions = 12,
        initiallySelectedOption = currentMonth,
    )
    val dayState = rememberPickerState(
        initialNumberOfOptions = 31,
        initiallySelectedOption = currentDay - 1,
    )

    val years = (currentYear..currentYear + 9).toList()
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // Track previous values to trigger haptic on change
    val prevDay = remember { androidx.compose.runtime.mutableIntStateOf(dayState.selectedOption) }
    val prevMonth = remember { androidx.compose.runtime.mutableIntStateOf(monthState.selectedOption) }
    val prevYear = remember { androidx.compose.runtime.mutableIntStateOf(yearState.selectedOption) }

    // Trigger haptic feedback when selection changes
    if (dayState.selectedOption != prevDay.intValue) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        prevDay.intValue = dayState.selectedOption
    }
    if (monthState.selectedOption != prevMonth.intValue) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        prevMonth.intValue = monthState.selectedOption
    }
    if (yearState.selectedOption != prevYear.intValue) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        prevYear.intValue = yearState.selectedOption
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Day picker
                Picker(
                    state = dayState,
                    modifier = Modifier.size(width = 48.dp, height = 100.dp),
                    contentDescription = "Day",
                    readOnly = false,
                    userScrollEnabled = true,
                ) { index ->
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.body1,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Month picker
                Picker(
                    state = monthState,
                    modifier = Modifier.size(width = 56.dp, height = 100.dp),
                    contentDescription = "Month",
                    readOnly = false,
                    userScrollEnabled = true,
                ) { index ->
                    Text(
                        text = months[index],
                        style = MaterialTheme.typography.body1,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Year picker
                Picker(
                    state = yearState,
                    modifier = Modifier.size(width = 64.dp, height = 100.dp),
                    contentDescription = "Year",
                    readOnly = false,
                    userScrollEnabled = true,
                ) { index ->
                    Text(
                        text = years[index].toString(),
                        style = MaterialTheme.typography.body1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp),
                    )
                }

                Button(
                    onClick = {
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, years[yearState.selectedOption])
                            set(Calendar.MONTH, monthState.selectedOption)
                            set(Calendar.DAY_OF_MONTH, dayState.selectedOption + 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateSelected(selectedCalendar.timeInMillis)
                    },
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Wear OS Time Picker using Picker component with haptic feedback (24h format).
 */
@Composable
fun WearTimePicker(
    initialTime: Long?,
    onTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val calendar = remember {
        Calendar.getInstance().apply {
            initialTime?.let { timeInMillis = it }
        }
    }

    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    val hourState = rememberPickerState(
        initialNumberOfOptions = 24,
        initiallySelectedOption = currentHour,
    )
    val minuteState = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = currentMinute,
    )

    // Track previous values to trigger haptic on change
    val prevHour = remember { androidx.compose.runtime.mutableIntStateOf(hourState.selectedOption) }
    val prevMinute = remember { androidx.compose.runtime.mutableIntStateOf(minuteState.selectedOption) }

    // Trigger haptic feedback when selection changes
    if (hourState.selectedOption != prevHour.intValue) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        prevHour.intValue = hourState.selectedOption
    }
    if (minuteState.selectedOption != prevMinute.intValue) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        prevMinute.intValue = minuteState.selectedOption
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Select Time (24h)",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hour picker (24h)
                Picker(
                    state = hourState,
                    modifier = Modifier.size(width = 56.dp, height = 100.dp),
                    contentDescription = "Hour",
                    readOnly = false,
                    userScrollEnabled = true,
                ) { index ->
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", index),
                        style = MaterialTheme.typography.body1,
                    )
                }

                Text(
                    text = ":",
                    style = MaterialTheme.typography.title2,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                // Minute picker
                Picker(
                    state = minuteState,
                    modifier = Modifier.size(width = 56.dp, height = 100.dp),
                    contentDescription = "Minute",
                    readOnly = false,
                    userScrollEnabled = true,
                ) { index ->
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", index),
                        style = MaterialTheme.typography.body1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp),
                    )
                }

                Button(
                    onClick = {
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hourState.selectedOption)
                            set(Calendar.MINUTE, minuteState.selectedOption)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onTimeSelected(selectedCalendar.timeInMillis)
                    },
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
