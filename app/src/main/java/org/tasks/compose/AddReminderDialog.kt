package org.tasks.compose

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.todoroo.astrid.ui.ReminderControlSetViewModel.ViewState
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
    @Composable
    fun AddRandomReminderDialog(
        viewState: ViewState,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val time = rememberSaveable { mutableStateOf(15) }
        val units = rememberSaveable { mutableStateOf(0) }
        if (viewState.showRandomDialog) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = { AddRandomReminder(time, units) },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        time.value.takeIf { it > 0 }?.let { i ->
                            addAlarm(Alarm(time = i * units.millis, type = TYPE_RANDOM))
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
        } else {
            time.value = 15
            units.value = 0
        }
    }

    @Composable
    fun AddCustomReminderDialog(
        viewState: ViewState,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val openDialog = viewState.showCustomDialog
        val time = rememberSaveable { mutableStateOf(15) }
        val units = rememberSaveable { mutableStateOf(0) }
        val openRecurringDialog = rememberSaveable { mutableStateOf(false) }
        val interval = rememberSaveable { mutableStateOf(0) }
        val recurringUnits = rememberSaveable { mutableStateOf(0) }
        val repeat = rememberSaveable { mutableStateOf(0) }
        if (openDialog) {
            if (!openRecurringDialog.value) {
                AlertDialog(
                    onDismissRequest = closeDialog,
                    text = {
                        AddCustomReminder(
                            time,
                            units,
                            interval,
                            recurringUnits,
                            repeat,
                            showRecurring = {
                                openRecurringDialog.value = true
                            }
                        )
                    },
                    confirmButton = {
                        Constants.TextButton(text = R.string.ok, onClick = {
                            time.value.takeIf { it >= 0 }?.let { i ->
                                addAlarm(
                                    Alarm(
                                        time = -1 * i * units.millis,
                                        type = TYPE_REL_END,
                                        repeat = repeat.value,
                                        interval = interval.value * recurringUnits.millis
                                    )
                                )
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
            AddRepeatReminderDialog(
                openDialog = openRecurringDialog,
                initialInterval = interval.value,
                initialUnits = recurringUnits.value,
                initialRepeat = repeat.value,
                selected = { i, u, r ->
                    interval.value = i
                    recurringUnits.value = u
                    repeat.value = r
                }
            )
        } else {
            time.value = 15
            units.value = 0
            interval.value = 0
            recurringUnits.value = 0
            repeat.value = 0
        }
    }

    @Composable
    fun AddRepeatReminderDialog(
        openDialog: MutableState<Boolean>,
        initialInterval: Int,
        initialUnits: Int,
        initialRepeat: Int,
        selected: (Int, Int, Int) -> Unit,
    ) {
        val interval = rememberSaveable { mutableStateOf(initialInterval) }
        val units = rememberSaveable { mutableStateOf(initialUnits) }
        val repeat = rememberSaveable { mutableStateOf(initialRepeat) }
        val closeDialog = {
            openDialog.value = false
        }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = {
                    AddRecurringReminder(
                        openDialog.value,
                        interval,
                        units,
                        repeat,
                    )
                },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        if (interval.value > 0 && repeat.value > 0) {
                            selected(interval.value, units.value, repeat.value)
                            openDialog.value = false
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
        } else {
            interval.value = initialInterval.takeIf { it > 0 } ?: 15
            units.value = initialUnits
            repeat.value = initialRepeat.takeIf { it > 0 } ?: 4
        }
    }

    @Composable
    fun AddRandomReminder(
        time: MutableState<Int>,
        units: MutableState<Int>,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.randomly_every, "").trim())
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, time, units)
            }
            ShowKeyboard(true, focusRequester)
        }
    }

    @Composable
    fun AddCustomReminder(
        time: MutableState<Int>,
        units: MutableState<Int>,
        interval: MutableState<Int>,
        recurringUnits: MutableState<Int>,
        repeat: MutableState<Int>,
        showRecurring: () -> Unit,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(resId = R.string.custom_notification)
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time,
                minValue = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, time, units, R.string.alarm_before_due)
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
                val repeating = repeat.value > 0 && interval.value > 0
                val text = if (repeating) {
                    LocalContext.current.resources.getRepeatString(
                        repeat.value,
                        interval.value * recurringUnits.millis
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
                    ClearButton {
                        repeat.value = 0
                        interval.value = 0
                        recurringUnits.value = 0
                    }
                }
            }
            ShowKeyboard(true, focusRequester)
        }
    }

    @Composable
    fun AddRecurringReminder(
        openDialog: Boolean,
        interval: MutableState<Int>,
        units: MutableState<Int>,
        repeat: MutableState<Int>
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.repeats_plural, "").trim())
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time = interval,
                modifier = Modifier.focusRequester(focusRequester),
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, interval, units)
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedIntInput(
                    time = repeat,
                    modifier = Modifier.weight(0.5f),
                    autoSelect = false,
                )
                BodyText(
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals.repeat_times,
                        repeat.value
                    ),
                    modifier = Modifier
                        .weight(0.5f)
                        .align(CenterVertically)
                )
            }

            ShowKeyboard(openDialog, focusRequester)
        }
    }

    private val options = listOf(
        R.plurals.reminder_minutes,
        R.plurals.reminder_hours,
        R.plurals.reminder_days,
        R.plurals.reminder_week,
    )

    private val MutableState<Int>.millis: Long
        get() = when (value) {
            1 -> TimeUnit.HOURS.toMillis(1)
            2 -> TimeUnit.DAYS.toMillis(1)
            3 -> TimeUnit.DAYS.toMillis(7)
            else -> TimeUnit.MINUTES.toMillis(1)
        }
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
    time: MutableState<Int>,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    autoSelect: Boolean = true,
) {
    val value = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = time.value.toString()
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(0, if (autoSelect) text.length else 0)
            )
        )
    }
    OutlinedTextField(
        value = value.value,
        onValueChange = {
            value.value = it.copy(text = it.text.filter { t -> t.isDigit() })
            time.value = value.value.text.toIntOrNull() ?: 0
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.padding(horizontal = 16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        ),
        isError = value.value.text.toIntOrNull()?.let { it < minValue } ?: true,
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
    time: MutableState<Int>,
    units: MutableState<Int>,
    formatString: Int? = null,
) {
    val optionString = LocalContext.current.resources.getQuantityString(option, time.value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { units.value = index }
    ) {
        RadioButton(
            selected = index == units.value,
            onClick = { units.value = index },
            modifier = Modifier.align(CenterVertically)
        )
        BodyText(
            text = if (index == units.value) {
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
    existingAlarms: List<Alarm>,
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
            // TODO: if replacing custom alarm show custom picker
            // TODO: prepopulate pickers with existing values
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
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderOne() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            time = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) },
            interval = remember { mutableStateOf(0) },
            recurringUnits = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(0) },
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminder() =
    TasksTheme {
        AddReminderDialog.AddCustomReminder(
            time = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) },
            interval = remember { mutableStateOf(0) },
            recurringUnits = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(0) },
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
            openDialog = true,
            interval = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(1) },
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRepeatingReminder() =
    TasksTheme {
        AddReminderDialog.AddRecurringReminder(
            openDialog = true,
            interval = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) },
            repeat = remember { mutableStateOf(4) },
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminderOne() =
    TasksTheme {
        AddReminderDialog.AddRandomReminder(
            time = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) }
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminder() =
    TasksTheme {
        AddReminderDialog.AddRandomReminder(
            time = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) }
        )
    }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddReminderDialog() =
    TasksTheme {
        AddAlarmDialog(
            viewState = ViewState(showAddAlarm = true),
            existingAlarms = emptyList(),
            addAlarm = {},
            addRandom = {},
            addCustom = {},
            pickDateAndTime = {},
            dismiss = {},
        )
    }