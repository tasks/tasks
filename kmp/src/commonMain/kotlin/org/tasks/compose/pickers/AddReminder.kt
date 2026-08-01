package org.tasks.compose.pickers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.ClearButton
import org.tasks.compose.CustomDialog
import org.tasks.compose.DialogRow
import org.tasks.compose.DialogTextButton
import org.tasks.compose.PlatformBackHandler
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_GEO_ENTER
import org.tasks.data.entity.Alarm.Companion.TYPE_GEO_EXIT
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.reminders.ReminderControlSetViewModel.ViewState
import org.tasks.reminders.repeatString
import org.tasks.themes.TasksTheme
import org.tasks.time.ONE_DAY
import org.tasks.time.ONE_HOUR
import org.tasks.time.ONE_MINUTE
import org.tasks.time.ONE_WEEK
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.alarm_after
import tasks.kmp.generated.resources.alarm_before
import tasks.kmp.generated.resources.alarm_due
import tasks.kmp.generated.resources.alarm_start
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.custom_notification
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.pick_a_date_and_time
import tasks.kmp.generated.resources.randomly
import tasks.kmp.generated.resources.reminder_days
import tasks.kmp.generated.resources.reminder_hours
import tasks.kmp.generated.resources.reminder_minutes
import tasks.kmp.generated.resources.reminder_week
import tasks.kmp.generated.resources.repeat_option_custom
import tasks.kmp.generated.resources.repeat_option_does_not_repeat
import tasks.kmp.generated.resources.repeat_times
import tasks.kmp.generated.resources.repeats_every
import tasks.kmp.generated.resources.when_due
import tasks.kmp.generated.resources.when_overdue
import tasks.kmp.generated.resources.when_started
import kotlin.math.abs

private val unitOptions = listOf(
    Res.plurals.reminder_minutes,
    Res.plurals.reminder_hours,
    Res.plurals.reminder_days,
    Res.plurals.reminder_week,
)

private fun unitIndexToMillis(unitIndex: Int): Long = when (unitIndex) {
    1 -> ONE_HOUR
    2 -> ONE_DAY
    3 -> ONE_WEEK
    else -> ONE_MINUTE
}

private fun timeToAmountAndUnit(time: Long): Pair<Int, Int> {
    val absTime = abs(time)
    return when {
        absTime == 0L -> 0 to 0
        absTime % ONE_WEEK == 0L -> (absTime / ONE_WEEK).toInt() to 3
        absTime % ONE_DAY == 0L -> (absTime / ONE_DAY).toInt() to 2
        absTime % ONE_HOUR == 0L -> (absTime / ONE_HOUR).toInt() to 1
        else -> (absTime / ONE_MINUTE).toInt() to 0
    }
}

private val AlarmSaver = listSaver<Alarm, Long>(
    save = { listOf(it.id, it.task, it.time, it.type.toLong(), it.repeat.toLong(), it.interval) },
    restore = {
        Alarm(
            id = it[0],
            task = it[1],
            time = it[2],
            type = it[3].toInt(),
            repeat = it[4].toInt(),
            interval = it[5],
        )
    },
)

@Composable
fun AddRandomReminderDialog(
    alarm: Alarm?,
    updateAlarm: (Alarm) -> Unit,
    closeDialog: () -> Unit,
    cancelDialog: () -> Unit = closeDialog,
) {
    var workingCopy by rememberSaveable(stateSaver = AlarmSaver) {
        mutableStateOf(alarm ?: Alarm(time = 15 * ONE_MINUTE, type = TYPE_RANDOM))
    }

    PlatformBackHandler(enabled = true, onBack = cancelDialog)
    AlertDialog(
        onDismissRequest = cancelDialog,
        text = {
            AddRandomReminder(
                alarm = workingCopy,
                updateAlarm = { workingCopy = it },
            )
        },
        confirmButton = {
            DialogTextButton(text = Res.string.ok, onClick = {
                val (amount, _) = timeToAmountAndUnit(workingCopy.time)
                if (amount > 0) {
                    updateAlarm(workingCopy)
                    closeDialog()
                }
            })
        },
        dismissButton = {
            DialogTextButton(text = Res.string.cancel, onClick = cancelDialog)
        },
    )
}

@Composable
fun AddCustomReminderDialog(
    alarm: Alarm?,
    updateAlarm: (Alarm) -> Unit,
    closeDialog: () -> Unit,
    cancelDialog: () -> Unit = closeDialog,
) {
    var workingCopy by rememberSaveable(stateSaver = AlarmSaver) {
        mutableStateOf(alarm ?: Alarm(time = -15 * ONE_MINUTE, type = TYPE_REL_END))
    }
    var showRecurringDialog by rememberSaveable { mutableStateOf(false) }
    var amountEntered by remember(showRecurringDialog) { mutableStateOf(true) }

    if (!showRecurringDialog) {
        PlatformBackHandler(enabled = true, onBack = cancelDialog)
        AlertDialog(
            onDismissRequest = cancelDialog,
            text = {
                AddCustomReminder(
                    alarm = workingCopy,
                    updateAlarm = { workingCopy = it },
                    onAmountEnteredChange = { amountEntered = it },
                    showRecurring = { showRecurringDialog = true },
                )
            },
            confirmButton = {
                DialogTextButton(text = Res.string.ok, onClick = {
                    if (amountEntered) {
                        updateAlarm(workingCopy)
                        closeDialog()
                    }
                })
            },
            dismissButton = {
                DialogTextButton(text = Res.string.cancel, onClick = cancelDialog)
            },
        )
    }

    if (showRecurringDialog) {
        AddRepeatReminderDialog(
            alarm = workingCopy,
            updateAlarm = { workingCopy = it },
            closeDialog = { showRecurringDialog = false },
        )
    }
}

@Composable
fun AddRepeatReminderDialog(
    alarm: Alarm,
    updateAlarm: (Alarm) -> Unit,
    closeDialog: () -> Unit,
) {
    var workingCopy by rememberSaveable(stateSaver = AlarmSaver) {
        mutableStateOf(
            if (alarm.interval == 0L && alarm.repeat == 0) {
                alarm.copy(interval = 15 * ONE_MINUTE, repeat = 4)
            } else {
                alarm
            }
        )
    }

    PlatformBackHandler(enabled = true, onBack = closeDialog)
    AlertDialog(
        onDismissRequest = closeDialog,
        text = {
            AddRecurringReminder(
                alarm = workingCopy,
                updateAlarm = { workingCopy = it },
            )
        },
        confirmButton = {
            DialogTextButton(text = Res.string.ok, onClick = {
                val (intervalAmount, _) = timeToAmountAndUnit(workingCopy.interval)
                if (intervalAmount > 0 && workingCopy.repeat > 0) {
                    updateAlarm(workingCopy)
                    closeDialog()
                }
            })
        },
        dismissButton = {
            DialogTextButton(text = Res.string.cancel, onClick = closeDialog)
        },
    )
}

@Composable
fun AddRandomReminder(
    alarm: Alarm,
    updateAlarm: (Alarm) -> Unit,
) {
    val (_, initialUnit) = timeToAmountAndUnit(alarm.time)
    var selectedUnit by rememberSaveable { mutableStateOf(initialUnit) }
    val amount = if (alarm.time == 0L) 0 else (alarm.time / unitIndexToMillis(selectedUnit)).toInt()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        CenteredH6(text = stringResource(Res.string.randomly))
        val focusRequester = remember { FocusRequester() }
        OutlinedIntInput(
            value = amount,
            onValueChange = { newAmount ->
                val amt = newAmount ?: 0
                updateAlarm(alarm.copy(time = amt * unitIndexToMillis(selectedUnit)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        Spacer(modifier = Modifier.height(16.dp))
        unitOptions.forEachIndexed { index, option ->
            RadioRow(
                index = index,
                option = option,
                timeAmount = amount,
                unitIndex = selectedUnit,
                onUnitSelected = { newUnit ->
                    selectedUnit = newUnit
                    updateAlarm(alarm.copy(time = amount * unitIndexToMillis(newUnit)))
                }
            )
        }
        ShowKeyboard(true, focusRequester)
    }
}

@Composable
fun AddCustomReminder(
    alarm: Alarm,
    updateAlarm: (Alarm) -> Unit,
    showRecurring: () -> Unit,
    onAmountEnteredChange: (Boolean) -> Unit = {},
) {
    val (_, initialUnit) = timeToAmountAndUnit(alarm.time)
    var selectedUnit by rememberSaveable { mutableStateOf(initialUnit) }
    val amount =
        if (alarm.time == 0L) 0 else abs(alarm.time / unitIndexToMillis(selectedUnit)).toInt()

    var isBefore by rememberSaveable { mutableStateOf(alarm.time <= 0) }
    val isStart = alarm.type == TYPE_REL_START
    val sign = if (isBefore) -1 else 1

    val (_, initialIntervalUnit) = timeToAmountAndUnit(alarm.interval)
    val intervalAmount =
        if (alarm.interval == 0L) 0 else (alarm.interval / unitIndexToMillis(initialIntervalUnit)).toInt()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        CenteredH6(text = stringResource(Res.string.custom_notification))
        val focusRequester = remember { FocusRequester() }
        OutlinedIntInput(
            value = amount,
            onValueChange = { newAmount ->
                onAmountEnteredChange(newAmount != null)
                val amt = newAmount ?: 0
                updateAlarm(alarm.copy(time = sign * amt * unitIndexToMillis(selectedUnit)))
            },
            minValue = 0,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        Spacer(modifier = Modifier.height(16.dp))
        unitOptions.forEachIndexed { index, option ->
            if (index == selectedUnit) {
                val optionString = pluralStringResource(option, amount)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedUnit = index
                            updateAlarm(alarm.copy(time = sign * amount * unitIndexToMillis(index)))
                        }
                ) {
                    RadioButton(
                        selected = true,
                        onClick = { },
                        modifier = Modifier.align(CenterVertically)
                    )
                    BodyText(
                        text = optionString,
                        modifier = Modifier.align(CenterVertically),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = {
                            isBefore = !isBefore
                            val newSign = if (isBefore) -1 else 1
                            updateAlarm(alarm.copy(time = newSign * abs(alarm.time)))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = OutlinedTextFieldDefaults.shape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.align(CenterVertically),
                    ) {
                        Text(
                            text = stringResource(
                                if (isBefore) Res.string.alarm_before else Res.string.alarm_after
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = {
                            val newType = if (isStart) TYPE_REL_END else TYPE_REL_START
                            updateAlarm(alarm.copy(type = newType))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = OutlinedTextFieldDefaults.shape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.align(CenterVertically),
                    ) {
                        Text(
                            text = stringResource(
                                if (isStart) Res.string.alarm_start else Res.string.alarm_due
                            ),
                        )
                    }
                }
            } else {
                RadioRow(
                    index = index,
                    option = option,
                    timeAmount = amount,
                    unitIndex = selectedUnit,
                    onUnitSelected = { newUnit ->
                        selectedUnit = newUnit
                        updateAlarm(alarm.copy(time = sign * amount * unitIndexToMillis(newUnit)))
                    }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp)
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { showRecurring() })
        {
            IconButton(onClick = showRecurring) {
                Icon(
                    imageVector = Icons.Outlined.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(CenterVertically),
                )
            }
            val repeating = alarm.repeat > 0 && intervalAmount > 0
            val text = if (repeating) {
                repeatString(alarm.repeat, alarm.interval)
            } else {
                stringResource(Res.string.repeat_option_does_not_repeat)
            }
            BodyText(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .align(CenterVertically)
            )
            if (repeating) {
                ClearButton(onClick = {
                    updateAlarm(alarm.copy(repeat = 0, interval = 0))
                })
            }
        }
        ShowKeyboard(true, focusRequester)
    }
}

@Composable
fun AddRecurringReminder(
    alarm: Alarm,
    updateAlarm: (Alarm) -> Unit,
) {
    val (_, initialIntervalUnit) = timeToAmountAndUnit(alarm.interval)
    var selectedUnit by rememberSaveable { mutableStateOf(initialIntervalUnit) }
    val intervalAmount =
        if (alarm.interval == 0L) 0 else (alarm.interval / unitIndexToMillis(selectedUnit)).toInt()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        CenteredH6(text = stringResource(Res.string.repeats_every))
        val focusRequester = remember { FocusRequester() }
        OutlinedIntInput(
            value = intervalAmount,
            onValueChange = { newAmount ->
                val amt = newAmount ?: 0
                updateAlarm(alarm.copy(interval = amt * unitIndexToMillis(selectedUnit)))
            },
            modifier = Modifier.focusRequester(focusRequester),
        )
        Spacer(modifier = Modifier.height(16.dp))
        unitOptions.forEachIndexed { index, option ->
            RadioRow(
                index = index,
                option = option,
                timeAmount = intervalAmount,
                unitIndex = selectedUnit,
                onUnitSelected = { newUnit ->
                    selectedUnit = newUnit
                    updateAlarm(alarm.copy(interval = intervalAmount * unitIndexToMillis(newUnit)))
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedIntInput(
                value = alarm.repeat,
                onValueChange = { newRepeat ->
                    updateAlarm(alarm.copy(repeat = newRepeat ?: 0))
                },
                modifier = Modifier.weight(0.5f),
                autoSelect = false,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BodyText(
                text = pluralStringResource(Res.plurals.repeat_times, alarm.repeat),
                modifier = Modifier
                    .weight(0.5f)
                    .align(CenterVertically)
            )
        }

        ShowKeyboard(true, focusRequester)
    }
}

private enum class AlarmChooserRoute { Random, DateTime, Custom, Skip }

private fun alarmChooserRoute(
    replace: Alarm?,
    existingAlarms: ImmutableSet<Alarm>,
    showRandom: Boolean,
    showDateTimePicker: Boolean,
): AlarmChooserRoute? {
    when (replace?.type) {
        TYPE_RANDOM -> return AlarmChooserRoute.Random
        TYPE_DATE_TIME -> return AlarmChooserRoute.DateTime
        TYPE_REL_START, TYPE_REL_END -> return AlarmChooserRoute.Custom
        TYPE_SNOOZE, TYPE_GEO_ENTER, TYPE_GEO_EXIT -> return AlarmChooserRoute.Skip
    }
    val hasWhenStarted = existingAlarms.any { it.type == TYPE_REL_START && it.time == 0L }
    val hasWhenDue = existingAlarms.any { it.type == TYPE_REL_END && it.time == 0L }
    val hasWhenOverdue = existingAlarms.any { it.type == TYPE_REL_END && it.time > 0 }
    return if (!showRandom && !showDateTimePicker && hasWhenStarted && hasWhenDue && hasWhenOverdue) {
        AlarmChooserRoute.Custom
    } else {
        null
    }
}

@Composable
fun AddAlarmDialog(
    viewState: ViewState,
    existingAlarms: ImmutableSet<Alarm>,
    addAlarm: (Alarm) -> Unit,
    addRandom: () -> Unit,
    addCustom: () -> Unit,
    pickDateAndTime: () -> Unit,
    dismiss: () -> Unit,
    showRandom: Boolean = true,
    showDateTimePicker: Boolean = true,
) {
    val route = if (viewState.showAddAlarm) {
        alarmChooserRoute(viewState.replace, existingAlarms, showRandom, showDateTimePicker)
    } else {
        null
    }
    LaunchedEffect(route) {
        when (route) {
            AlarmChooserRoute.Random -> addRandom()
            AlarmChooserRoute.DateTime -> pickDateAndTime()
            AlarmChooserRoute.Custom -> addCustom()
            AlarmChooserRoute.Skip -> Unit
            null -> return@LaunchedEffect
        }
        dismiss()
    }
    CustomDialog(visible = viewState.showAddAlarm && route == null, onDismiss = dismiss) {
        Column {
            if (existingAlarms.none { it.type == TYPE_REL_START && it.time == 0L }) {
                DialogRow(text = Res.string.when_started) {
                    addAlarm(whenStarted(0))
                    dismiss()
                }
            }
            if (existingAlarms.none { it.type == TYPE_REL_END && it.time == 0L }) {
                DialogRow(text = Res.string.when_due) {
                    addAlarm(Alarm.whenDue(0))
                    dismiss()
                }
            }
            if (existingAlarms.none { it.type == TYPE_REL_END && it.time == ONE_DAY }) {
                DialogRow(text = Res.string.when_overdue) {
                    addAlarm(Alarm.whenOverdue(0))
                    dismiss()
                }
            }
            if (showRandom) {
                DialogRow(text = Res.string.randomly) {
                    addRandom()
                    dismiss()
                }
            }
            if (showDateTimePicker) {
                DialogRow(text = Res.string.pick_a_date_and_time) {
                    pickDateAndTime()
                    dismiss()
                }
            }
            DialogRow(text = Res.string.repeat_option_custom) {
                addCustom()
                dismiss()
            }
        }
    }
}

@Composable
fun ShowKeyboard(visible: Boolean, focusRequester: FocusRequester) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        focusRequester.freeFocus()
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun OutlinedIntInput(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    autoSelect: Boolean = true,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value?.toString() ?: "",
                selection = if (autoSelect) {
                    TextRange(0, value?.toString()?.length ?: 0)
                } else {
                    TextRange.Zero
                }
            )
        )
    }

    LaunchedEffect(value) {
        val currentParsedValue = textFieldValue.text.toIntOrNull()
        if (currentParsedValue != value && textFieldValue.text.isNotEmpty()) {
            val newText = value?.toString() ?: ""
            textFieldValue = TextFieldValue(
                text = newText,
                selection = if (autoSelect) {
                    TextRange(0, newText.length)
                } else {
                    textFieldValue.selection
                }
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it.copy(text = it.text.filter { t -> t.isDigit() })
            onValueChange(textFieldValue.text.toIntOrNull())
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        ),
        isError = textFieldValue.text.toIntOrNull()?.let { it < minValue } ?: true,
    )
}

@Composable
fun CenteredH6(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge
    )
}

@Composable
fun RadioRow(
    index: Int,
    option: PluralStringResource,
    timeAmount: Int,
    unitIndex: Int,
    onUnitSelected: (Int) -> Unit,
) {
    val optionString = pluralStringResource(option, timeAmount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUnitSelected(index) }
    ) {
        RadioButton(
            selected = index == unitIndex,
            onClick = { onUnitSelected(index) },
            modifier = Modifier.align(CenterVertically)
        )
        BodyText(
            text = optionString,
            modifier = Modifier.align(CenterVertically),
        )
    }
}

@Composable
fun BodyText(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Preview(showBackground = true, name = "Custom reminder, before due - Light")
@Preview(
    showBackground = true,
    uiMode = PREVIEW_NIGHT_MODE,
    name = "Custom reminder, before due - Dark",
)
@Composable
private fun AddCustomReminderBeforeDuePreview() = TasksTheme {
    AddCustomReminder(
        alarm = Alarm(time = -15 * ONE_HOUR, type = TYPE_REL_END),
        updateAlarm = {},
        showRecurring = {},
    )
}

@Preview(showBackground = true, name = "Custom reminder, singular unit")
@Composable
private fun AddCustomReminderSingularPreview() = TasksTheme {
    AddCustomReminder(
        alarm = Alarm(time = -ONE_MINUTE, type = TYPE_REL_END),
        updateAlarm = {},
        showRecurring = {},
    )
}

@Preview(showBackground = true, name = "Custom reminder, after due")
@Composable
private fun AddCustomReminderAfterDuePreview() = TasksTheme {
    AddCustomReminder(
        alarm = Alarm(time = 15 * ONE_HOUR, type = TYPE_REL_END),
        updateAlarm = {},
        showRecurring = {},
    )
}

@Preview(showBackground = true, name = "Custom reminder, before start")
@Composable
private fun AddCustomReminderBeforeStartPreview() = TasksTheme {
    AddCustomReminder(
        alarm = Alarm(time = -15 * ONE_HOUR, type = TYPE_REL_START),
        updateAlarm = {},
        showRecurring = {},
    )
}

@Preview(showBackground = true, name = "Custom reminder, after start")
@Composable
private fun AddCustomReminderAfterStartPreview() = TasksTheme {
    AddCustomReminder(
        alarm = Alarm(time = 15 * ONE_HOUR, type = TYPE_REL_START),
        updateAlarm = {},
        showRecurring = {},
    )
}

@Preview(showBackground = true, name = "Repeating reminder - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Repeating reminder - Dark")
@Composable
private fun AddRepeatingReminderPreview() = TasksTheme {
    AddRecurringReminder(
        alarm = Alarm(
            time = -15 * ONE_HOUR,
            type = TYPE_REL_END,
            interval = 15 * ONE_HOUR,
            repeat = 4,
        ),
        updateAlarm = {},
    )
}

@Preview(showBackground = true, name = "Repeating reminder, singular unit")
@Composable
private fun AddRepeatingReminderSingularPreview() = TasksTheme {
    AddRecurringReminder(
        alarm = Alarm(
            time = -ONE_MINUTE,
            type = TYPE_REL_END,
            interval = ONE_MINUTE,
            repeat = 1,
        ),
        updateAlarm = {},
    )
}

@Preview(showBackground = true, name = "Random reminder - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Random reminder - Dark")
@Composable
private fun AddRandomReminderPreview() = TasksTheme {
    AddRandomReminder(
        alarm = Alarm(time = 15 * ONE_HOUR, type = TYPE_RANDOM),
        updateAlarm = {},
    )
}

@Preview(showBackground = true, name = "Random reminder, singular unit")
@Composable
private fun AddRandomReminderSingularPreview() = TasksTheme {
    AddRandomReminder(
        alarm = Alarm(time = ONE_MINUTE, type = TYPE_RANDOM),
        updateAlarm = {},
    )
}

@Preview(showBackground = true, name = "Add alarm chooser - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Add alarm chooser - Dark")
@Composable
private fun AddAlarmDialogPreview() = TasksTheme {
    AddAlarmDialog(
        viewState = ViewState(showAddAlarm = true),
        existingAlarms = persistentSetOf(),
        addAlarm = {},
        addRandom = {},
        addCustom = {},
        pickDateAndTime = {},
        dismiss = {},
    )
}
