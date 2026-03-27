package org.tasks.compose.edit

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.compose.pickers.StartDateTimePicker
import org.tasks.compose.pickers.StartDateTimePicker.StartDateTimePicker
import org.tasks.compose.pickers.StartDateTimePicker.getRelativeDateString
import org.tasks.data.entity.Task
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay

@Composable
fun StartDateRow(
    current: Long,
    setCurrent: (Long) -> Unit,
    dueDate: Long,
    isNew: Boolean,
    hasStartAlarm: Boolean,
    showDueDate: Boolean
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    val day =
        if (current <= 0L) {
            if (isNew) when (preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)) {
                Task.HIDE_UNTIL_DUE -> StartDatePicker.DUE_DATE
                Task.HIDE_UNTIL_DUE_TIME -> StartDatePicker.DUE_TIME
                Task.HIDE_UNTIL_DAY_BEFORE -> StartDatePicker.DAY_BEFORE_DUE
                Task.HIDE_UNTIL_WEEK_BEFORE -> StartDatePicker.WEEK_BEFORE_DUE
                else -> 0L
            }
            else current
        } else {
            val dueDay = dueDate.startOfDay()
            val dueTime = dueDate.millisOfDay
            val hideUntil = current.toDateTime()
            when (current) {
                dueDay -> if (hideUntil.millisOfDay == dueTime) {
                    StartDatePicker.DUE_TIME
                } else {
                    StartDatePicker.DUE_DATE
                }
                dueDay.toDateTime().minusDays(1).millis ->
                    StartDatePicker.DAY_BEFORE_DUE
                dueDay.toDateTime().minusDays(7).millis ->
                    StartDatePicker.WEEK_BEFORE_DUE
                else -> current
            }
        }

    val time =
        if (current > 0L &&
            day == dueDate.startOfDay() &&
            current.millisOfDay == dueDate.millisOfDay)
            StartDatePicker.NO_TIME
        else
            current.millisOfDay

    val selectedDay = rememberSaveable { mutableLongStateOf(day) }
    val selectedTime = rememberSaveable { mutableIntStateOf(time) }

    fun getSelectedValue(dueDate: Long): Long {
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (selectedDay.longValue) {
            StartDatePicker.DUE_DATE -> due?.withMillisOfDay(selectedTime.intValue)?.millis ?: 0
            StartDatePicker.DUE_TIME -> due?.millis ?: 0
            StartDatePicker.DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(selectedTime.intValue)?.millis ?: 0
            StartDatePicker.WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(selectedTime.intValue)?.millis ?: 0
            else -> selectedDay.longValue + selectedTime.intValue
        }
    }

    val showPicker = remember { mutableStateOf(false) }

    StartDateRow(
        startDate = current,
        selectedDay = selectedDay.longValue,
        selectedTime = selectedTime.intValue,
        hasStartAlarm = hasStartAlarm,
        hasDueDate = dueDate > 0L,
        printDate = {
            runBlocking {
                getRelativeDateTime(
                    selectedDay.longValue + selectedTime.intValue,
                    context.is24HourFormat,
                    DateStyle.FULL,
                    alwaysDisplayFullDate = preferences.alwaysDisplayFullDate
                )
            }
        },
        onClick = { showPicker.value = true}
    )

    LaunchedEffect(dueDate) { setCurrent(getSelectedValue(dueDate)) }

    if (showPicker.value) {
        StartDateTimePicker(
            selectedDay = selectedDay.longValue,
            selectedTime = selectedTime.intValue,
            updateValues = { day, time -> selectedDay.longValue = day; selectedTime.intValue = time },
            accept = { setCurrent(getSelectedValue(dueDate)); showPicker.value = false },
            dismiss = { showPicker.value = false },
            autoclose = preferences.getBoolean(
                R.string.p_auto_dismiss_datetime_edit_screen,
                false
            ),
            showDueDate = showDueDate
        )
    }
}

@Composable
fun StartDateRow(
    current: Long,
    setCurrent: (Long) -> Unit,
    dueDate: Long,
    isNew: Boolean,
    hasStartAlarm: Boolean,
    showDueDate: Boolean
) {
    val showPicker = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    val initial = remember { // to avoid recalculations on recompose
        if (current > 0) current
        else if (isNew) defaultStartDate(preferences)
        else 0L
    }

    // These States are the primary storage for the startDate values like a local viewModel,
    // e.g. StartDateRow sends changes to the viewModel, but not updates itself if/when "current"
    // parameter is changed outside
    val selectedDay = rememberSaveable {
        mutableLongStateOf(StartDateTimePicker.encodeDay(initial, dueDate))
    }
    val selectedTime = rememberSaveable {
        mutableIntStateOf(StartDateTimePicker.encodeTime(initial, selectedDay.longValue, dueDate))
    }

    // to prevent using "runBlocking" for formatting relative date text
    val currentText by produceState(initialValue = "", dueDate, selectedDay.longValue, selectedTime.intValue) {
        value = formatDateTimeText(selectedDay.longValue, selectedTime.intValue, dueDate, context, preferences)
    }

    StartDateRow(
        startDate = current,
        selectedDay = selectedDay.longValue,
        hasStartAlarm = hasStartAlarm,
        hasDueDate = dueDate > 0L,
        dateTimeText = currentText,
        onClick = { showPicker.value = true}
    )

    LaunchedEffect(dueDate, selectedDay.longValue, selectedTime.intValue) {
        setCurrent(StartDateTimePicker.decodeStartDay(dueDate, selectedDay.longValue, selectedTime.intValue))
    }

    if (showPicker.value) {
        StartDateTimePicker(
            selectedDay = selectedDay.longValue,
            selectedTime = selectedTime.intValue,
            updateValues = { day, time ->
                selectedDay.longValue = day
                selectedTime.intValue = time
            },
            accept = { showPicker.value = false },
            dismiss = { showPicker.value = false },
            autoclose = preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false),
            showDueDate = showDueDate
        )
    }
}

@Composable
private fun StartDateRow(
    startDate: Long,
    selectedDay: Long,
    currentTime: Long = currentTimeMillis(),
    hasStartAlarm: Boolean,
    hasDueDate: Boolean,
    dateTimeText: String,
    onClick: () -> Unit,
) {
    val color = when {
        selectedDay < 0 && !hasDueDate -> MaterialTheme.colorScheme.error
        startDate == 0L && hasStartAlarm -> MaterialTheme.colorScheme.error
        // Currently use of 0.38f looks like the standard way for ContentAlpha.disabled of the Material2 in the Material3
        startDate == 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        startDate < currentTime -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    TaskEditRow(
        iconRes = R.drawable.ic_pending_actions_24px,
        content = {
            Text(
                text = dateTimeText,
                color = color,
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .height(24.dp)
            )
        },
        onClick = onClick
    )
}

private fun defaultStartDate(preferences: Preferences): Long = when (preferences.getIntegerFromString(
    R.string.p_default_hideUntil_key,
    Task.HIDE_UNTIL_NONE
)) {
    Task.HIDE_UNTIL_DUE -> StartDateTimePicker.DUE_DATE
    Task.HIDE_UNTIL_DUE_TIME -> StartDateTimePicker.DUE_TIME
    Task.HIDE_UNTIL_DAY_BEFORE -> StartDateTimePicker.DAY_BEFORE_DUE
    Task.HIDE_UNTIL_WEEK_BEFORE -> StartDateTimePicker.WEEK_BEFORE_DUE
    else -> 0L
}

private suspend fun formatDateTimeText(
    selectedDay: Long,
    selectedTime: Int,
    dueDate: Long,
    context: Context,
    preferences: Preferences
): String {
    return when (StartDateTimePicker.encodeStartDay(dueDate, selectedDay, selectedTime)) {
        StartDateTimePicker.DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
        StartDateTimePicker.DUE_TIME -> context.getString(R.string.due_time)
        StartDateTimePicker.DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
        StartDateTimePicker.WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
        in 1..Long.MAX_VALUE -> {
            getRelativeDateTime(
                selectedDay + selectedTime,
                context.is24HourFormat,
                DateStyle.FULL,
                alwaysDisplayFullDate = preferences.alwaysDisplayFullDate
            )
        }
        else -> context.getString(R.string.no_start_date)
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoStartDateAlarm() {
    TasksTheme {
        StartDateRow(
            current = 0L,
            setCurrent = {},
            dueDate = 0L,
            isNew = true,
            hasStartAlarm = true,
            showDueDate = false
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoStartDateNoAlarm() {
    TasksTheme {
        StartDateRow(
            current = 0L,
            setCurrent = {},
            dueDate = 0L,
            isNew = true,
            hasStartAlarm = false,
            showDueDate = false
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PastStartDate() {
    TasksTheme {
        StartDateRow(
            current = 1657080392000L,
            setCurrent = {},
            dueDate = 0L,
            isNew = true,
            hasStartAlarm = false,
            showDueDate = false
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PastStartDueDate() {
    TasksTheme {
        StartDateRow(
            current = 1657080392000L,
            setCurrent = {},
            dueDate = 1657080392000L,
            isNew = false,
            hasStartAlarm = false,
            showDueDate = false
        )
    }
}

//189216000000L
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun FutureStartDate() {
    TasksTheme {
        StartDateRow(
            current = 1892160000000L,
            setCurrent = {},
            dueDate = 1657080392000L,
            isNew = false,
            hasStartAlarm = false,
            showDueDate = false
        )
    }
}

// TODO: delete the code below after migration together
//  with StartDateControlSet and StartDatePicker
@Composable
fun StartDateRow(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    currentTime: Long = currentTimeMillis(),
    hasStartAlarm: Boolean,
    hasDueDate: Boolean,
    printDate: () -> String,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_pending_actions_24px,
        content = {
            StartDate(
                startDate = startDate,
                selectedDay = selectedDay,
                selectedTime = selectedTime,
                currentTime = currentTime,
                hasStartAlarm = hasStartAlarm,
                hasDueDate = hasDueDate,
                printDate = printDate,
            )
        },
        onClick = onClick
    )
}

@Composable
fun StartDate(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    currentTime: Long,
    hasStartAlarm: Boolean,
    hasDueDate: Boolean,
    printDate: () -> String,
) {
    val context = LocalContext.current
    Text(
        text = when (selectedDay) {
            StartDateTimePicker.DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
            StartDateTimePicker.DUE_TIME -> context.getString(R.string.due_time)
            StartDateTimePicker.DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
            StartDateTimePicker.WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
            in 1..Long.MAX_VALUE -> printDate()
            else -> stringResource(id = R.string.no_start_date)
        },
        color = when {
            selectedDay < 0 && !hasDueDate -> MaterialTheme.colorScheme.error
            startDate == 0L && hasStartAlarm -> MaterialTheme.colorScheme.error
            startDate == 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // ContentAlpha.disabled
            startDate < currentTime -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .padding(vertical = 20.dp)
            .height(24.dp),
    )
}
