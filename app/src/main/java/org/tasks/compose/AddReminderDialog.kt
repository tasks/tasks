package org.tasks.compose

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.todoroo.astrid.ui.ReminderControlSetViewModel.ViewState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.android.awaitFrame
import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.reminders.AlarmToString.Companion.getRepeatString
import org.tasks.themes.TasksTheme
import java.util.concurrent.TimeUnit

@ExperimentalComposeUiApi
object AddReminderDialog {
    // Helper functions for converting between Alarm properties and UI state
    private fun unitIndexToMillis(unitIndex: Int): Long = when (unitIndex) {
        1 -> TimeUnit.HOURS.toMillis(1)
        2 -> TimeUnit.DAYS.toMillis(1)
        3 -> TimeUnit.DAYS.toMillis(7)
        else -> TimeUnit.MINUTES.toMillis(1)
    }

    private fun timeToAmountAndUnit(time: Long): Pair<Int, Int> {
        val absTime = kotlin.math.abs(time)
        return when {
            absTime == 0L -> 0 to 0 // Default to minutes when time is 0
            absTime % TimeUnit.DAYS.toMillis(7) == 0L ->
                (absTime / TimeUnit.DAYS.toMillis(7)).toInt() to 3
            absTime % TimeUnit.DAYS.toMillis(1) == 0L ->
                (absTime / TimeUnit.DAYS.toMillis(1)).toInt() to 2
            absTime % TimeUnit.HOURS.toMillis(1) == 0L ->
                (absTime / TimeUnit.HOURS.toMillis(1)).toInt() to 1
            else ->
                (absTime / TimeUnit.MINUTES.toMillis(1)).toInt() to 0
        }
    }

    @Composable
    fun AddRandomReminderDialog(
        alarm: Alarm?,
        updateAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        // Create working copy from alarm or use defaults
        var workingCopy by rememberSaveable {
            mutableStateOf(alarm ?: Alarm(time = 15 * TimeUnit.MINUTES.toMillis(1), type = TYPE_RANDOM))
        }

        AlertDialog(
            onDismissRequest = closeDialog,
            text = {
                AddRandomReminder(
                    alarm = workingCopy,
                    updateAlarm = { workingCopy = it }
                )
            },
            confirmButton = {
                Constants.TextButton(text = R.string.ok, onClick = {
                    val (amount, _) = timeToAmountAndUnit(workingCopy.time)
                    if (amount > 0) {
                        updateAlarm(workingCopy)
                        closeDialog()
                    }
                })
            },
            dismissButton = {
                Constants.TextButton(
                    text = R.string.cancel,
                    onClick = closeDialog
                )
            },
        )
    }

    @Composable
    fun AddCustomReminderDialog(
        alarm: Alarm?,
        updateAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        // Create working copy from alarm or use defaults
        var workingCopy by rememberSaveable {
            mutableStateOf(
                alarm ?: Alarm(
                    time = -1 * 15 * TimeUnit.MINUTES.toMillis(1),
                    type = TYPE_REL_END
                )
            )
        }
        var showRecurringDialog by rememberSaveable { mutableStateOf(false) }

        if (!showRecurringDialog) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = {
                    AddCustomReminder(
                        alarm = workingCopy,
                        updateAlarm = { workingCopy = it },
                        showRecurring = { showRecurringDialog = true }
                    )
                },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        val (amount, _) = timeToAmountAndUnit(workingCopy.time)
                        if (amount >= 0) {
                            updateAlarm(workingCopy)
                            closeDialog()
                        }
                    })
                },
                dismissButton = {
                    Constants.TextButton(
                        text = R.string.cancel,
                        onClick = closeDialog
                    )
                },
            )
        }

        if (showRecurringDialog) {
            AddRepeatReminderDialog(
                alarm = workingCopy,
                updateAlarm = { workingCopy = it },
                closeDialog = { showRecurringDialog = false }
            )
        }
    }

    @Composable
    fun AddRepeatReminderDialog(
        alarm: Alarm,
        updateAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        // Create working copy with defaults if no recurrence set
        var workingCopy by rememberSaveable {
            mutableStateOf(
                if (alarm.interval == 0L && alarm.repeat == 0) {
                    // Default to 15 minutes, 4 times
                    alarm.copy(
                        interval = 15 * TimeUnit.MINUTES.toMillis(1),
                        repeat = 4
                    )
                } else {
                    alarm
                }
            )
        }

        AlertDialog(
            onDismissRequest = closeDialog,
            text = {
                AddRecurringReminder(
                    alarm = workingCopy,
                    updateAlarm = { workingCopy = it }
                )
            },
            confirmButton = {
                Constants.TextButton(text = R.string.ok, onClick = {
                    val (intervalAmount, _) = timeToAmountAndUnit(workingCopy.interval)
                    if (intervalAmount > 0 && workingCopy.repeat > 0) {
                        updateAlarm(workingCopy)
                        closeDialog()
                    }
                })
            },
            dismissButton = {
                Constants.TextButton(
                    text = R.string.cancel,
                    onClick = closeDialog
                )
            },
        )
    }

    @Composable
    fun AddRandomReminder(
        alarm: Alarm,
        updateAlarm: (Alarm) -> Unit,
    ) {
        val (initialAmount, initialUnit) = timeToAmountAndUnit(alarm.time)
        var selectedUnit by rememberSaveable { mutableStateOf(initialUnit) }
        val amount = if (alarm.time == 0L) 0 else (alarm.time / unitIndexToMillis(selectedUnit)).toInt()
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.randomly_every, "").trim())
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
            options.forEachIndexed { index, option ->
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
    ) {
        val (initialAmount, initialUnit) = timeToAmountAndUnit(alarm.time)
        var selectedUnit by rememberSaveable { mutableStateOf(initialUnit) }
        val amount = if (alarm.time == 0L) 0 else kotlin.math.abs(alarm.time / unitIndexToMillis(selectedUnit)).toInt()

        var isBefore by rememberSaveable { mutableStateOf(alarm.time <= 0) }
        val isStart = alarm.type == TYPE_REL_START
        val sign = if (isBefore) -1 else 1

        val formatString = when {
            isBefore && isStart -> R.string.alarm_before_start
            !isBefore && isStart -> R.string.alarm_after_start
            isBefore && !isStart -> R.string.alarm_before_due
            else -> R.string.alarm_after_due
        }

        val (initialIntervalAmount, initialIntervalUnit) = timeToAmountAndUnit(alarm.interval)
        val intervalAmount = if (alarm.interval == 0L) 0 else (alarm.interval / unitIndexToMillis(initialIntervalUnit)).toInt()

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(resId = R.string.custom_notification)
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                value = amount,
                onValueChange = { newAmount ->
                    val amt = newAmount ?: 0
                    updateAlarm(alarm.copy(time = sign * amt * unitIndexToMillis(selectedUnit)))
                },
                minValue = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                if (index == selectedUnit) {
                    val optionString = LocalContext.current.resources.getQuantityString(option, amount)
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
                                updateAlarm(alarm.copy(time = newSign * kotlin.math.abs(alarm.time)))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = OutlinedTextFieldDefaults.shape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.align(CenterVertically),
                        ) {
                            Text(
                                text = stringResource(id = if (isBefore) R.string.alarm_before else R.string.alarm_after),
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
                                text = stringResource(id = if (isStart) R.string.alarm_start else R.string.alarm_due),
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
            Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp)
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { showRecurring() })
            {
                IconButton(onClick = showRecurring) {
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = null,
                        modifier = Modifier
                            .align(CenterVertically)
                            .alpha(
                                ResourcesCompat.getFloat(
                                    LocalContext.current.resources,
                                    R.dimen.alpha_secondary
                                )
                            ),
                    )
                }
                val repeating = alarm.repeat > 0 && intervalAmount > 0
                val text = if (repeating) {
                    LocalContext.current.resources.getRepeatString(
                        alarm.repeat,
                        alarm.interval
                    )
                } else {
                    stringResource(id = R.string.repeat_option_does_not_repeat)
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
        val (initialIntervalAmount, initialIntervalUnit) = timeToAmountAndUnit(alarm.interval)
        var selectedUnit by rememberSaveable { mutableStateOf(initialIntervalUnit) }
        val intervalAmount = if (alarm.interval == 0L) 0 else (alarm.interval / unitIndexToMillis(selectedUnit)).toInt()
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.repeats_plural, "").trim())
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
            options.forEachIndexed { index, option ->
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
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals.repeat_times,
                        alarm.repeat
                    ),
                    modifier = Modifier
                        .weight(0.5f)
                        .align(CenterVertically)
                )
            }

            ShowKeyboard(true, focusRequester)
        }
    }

    private val options = listOf(
        R.plurals.reminder_minutes,
        R.plurals.reminder_hours,
        R.plurals.reminder_days,
        R.plurals.reminder_week,
    )
}

@ExperimentalComposeUiApi
@Composable
fun ShowKeyboard(visible: Boolean, focusRequester: FocusRequester) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        focusRequester.freeFocus()
        awaitFrame()
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

    // Sync when external value changes, but don't interfere with user editing
    LaunchedEffect(value) {
        val currentParsedValue = textFieldValue.text.toIntOrNull()
        // Only sync if the new value is different from what we currently parse to,
        // and don't sync if the text field is empty (user is actively deleting)
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
fun CenteredH6(@StringRes resId: Int) {
    CenteredH6(text = stringResource(id = resId))
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
    option: Int,
    timeAmount: Int,
    unitIndex: Int,
    onUnitSelected: (Int) -> Unit,
    formatString: Int? = null,
) {
    val optionString = LocalContext.current.resources.getQuantityString(option, timeAmount)
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
            text = if (index == unitIndex) {
                formatString
                    ?.let { stringResource(id = formatString, optionString) }
                    ?: optionString

            } else {
                optionString
            },
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

@Composable
fun AddAlarmDialog(
    viewState: ViewState,
    existingAlarms: ImmutableSet<Alarm>,
    addAlarm: (Alarm) -> Unit,
    addRandom: () -> Unit,
    addCustom: () -> Unit,
    pickDateAndTime: () -> Unit,
    dismiss: () -> Unit,
) {
    if (viewState.showAddAlarm) {
        when (viewState.replace?.type) {
            TYPE_RANDOM -> {
                addRandom()
                dismiss()
                return
            }
            TYPE_DATE_TIME -> {
                pickDateAndTime()
                dismiss()
                return
            }
            TYPE_REL_START, TYPE_REL_END -> {
                addCustom()
                dismiss()
                return
            }
        }
    }
    CustomDialog(visible = viewState.showAddAlarm, onDismiss = dismiss) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            if (existingAlarms.none { it.type == TYPE_REL_START && it.time == 0L }) {
                DialogRow(text = R.string.when_started) {
                    addAlarm(whenStarted(0))
                    dismiss()
                }
            }
            if (existingAlarms.none { it.type == TYPE_REL_END && it.time == 0L }) {
                DialogRow(text = R.string.when_due) {
                    addAlarm(Alarm.whenDue(0))
                    dismiss()
                }
            }
            if (existingAlarms.none {
                    it.type == TYPE_REL_END && it.time == TimeUnit.HOURS.toMillis(24)
                }) {
                DialogRow(text = R.string.when_overdue) {
                    addAlarm(Alarm.whenOverdue(0))
                    dismiss()
                }
            }
            DialogRow(text = R.string.randomly) {
                addRandom()
                dismiss()
            }
            DialogRow(text = R.string.pick_a_date_and_time) {
                pickDateAndTime()
                dismiss()
            }
            DialogRow(text = R.string.repeat_option_custom) {
                addCustom()
                dismiss()
            }
        }
    }
}

@ExperimentalComposeUiApi
@PreviewLightDark
@PreviewFontScale
@Composable
fun AddCustomReminderOne() =
    TasksTheme {
        Surface {
            AddReminderDialog.AddCustomReminder(
                alarm = Alarm(
                    time = -1 * TimeUnit.MINUTES.toMillis(1),
                    type = TYPE_REL_END
                ),
                updateAlarm = {},
                showRecurring = {},
            )
        }
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true, locale = "ta", fontScale = 1.3f)
@Composable
fun AddCustomReminderTamil() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            alarm = Alarm(
                time = -15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_END
            ),
            updateAlarm = {},
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderPlural() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            alarm = Alarm(
                time = -15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_END
            ),
            updateAlarm = {},
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderAfterDue() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            alarm = Alarm(
                time = 15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_END
            ),
            updateAlarm = {},
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderBeforeStart() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            alarm = Alarm(
                time = -15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_START
            ),
            updateAlarm = {},
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderAfterStart() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            alarm = Alarm(
                time = 15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_START
            ),
            updateAlarm = {},
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRepeatingReminderOne() =
    TasksTheme {
        AddReminderDialog.AddRecurringReminder(
            alarm = Alarm(
                time = -1 * TimeUnit.MINUTES.toMillis(1),
                type = TYPE_REL_END,
                interval = TimeUnit.MINUTES.toMillis(1),
                repeat = 1
            ),
            updateAlarm = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRepeatingReminder() =
    TasksTheme {
        AddReminderDialog.AddRecurringReminder(
            alarm = Alarm(
                time = -15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_REL_END,
                interval = 15 * TimeUnit.HOURS.toMillis(1),
                repeat = 4
            ),
            updateAlarm = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminderOne() =
    TasksTheme {
        AddReminderDialog.AddRandomReminder(
            alarm = Alarm(
                time = TimeUnit.MINUTES.toMillis(1),
                type = TYPE_RANDOM
            ),
            updateAlarm = {}
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminderPlural() =
    TasksTheme {
        AddReminderDialog.AddRandomReminder(
            alarm = Alarm(
                time = 15 * TimeUnit.HOURS.toMillis(1),
                type = TYPE_RANDOM
            ),
            updateAlarm = {}
        )
    }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddReminderDialog() =
    TasksTheme {
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
