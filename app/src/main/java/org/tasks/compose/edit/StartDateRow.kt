package org.tasks.compose.edit

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.compose.pickers.StartDateTimePicker.DAY_BEFORE_DUE
import org.tasks.compose.pickers.StartDateTimePicker.DUE_DATE
import org.tasks.compose.pickers.StartDateTimePicker.DUE_TIME
import org.tasks.compose.pickers.StartDateTimePicker.NO_DAY
import org.tasks.compose.pickers.StartDateTimePicker.NO_TIME
import org.tasks.compose.pickers.StartDateTimePicker.StartDateTimePicker
import org.tasks.compose.pickers.StartDateTimePicker.WEEK_BEFORE_DUE
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
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
    val showPicker = remember { mutableStateOf(false) }
    val context = LocalContext.current
    // TODO: provide some standard way to get Properties in @Composable's
    val preferences = remember { Preferences(context) }

    val initial = remember {
        if (current <= 0) {
            if (isNew) {
                when (preferences.getIntegerFromString(
                    R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE
                )) {
                    Task.HIDE_UNTIL_DUE -> DUE_DATE
                    Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
                    Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
                    Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
                    else -> 0L
                }
            } else 0L
        } else current
    }

    // Note that these States are the primary storage for the startDate value,
    // e.g. StartDateRow sends changes to the viewModel, but not updates itself if/when "current"
    // parameter is changed outside, like a local viewModel
    val selectedDay = rememberSaveable {
        mutableLongStateOf(encodeDay(initial, dueDate))
    }
    val selectedTime = rememberSaveable {
        mutableIntStateOf(encodeTime(initial, selectedDay.longValue, dueDate))
    }

    // This state variable is to prevent using "runBlocking" for formatting relative date text
    val currentText = remember { mutableStateOf("") }
    LaunchedEffect(dueDate, selectedDay.longValue, selectedTime.intValue) {
        currentText.value =
            formatDateTimeText(selectedDay.longValue, selectedTime.intValue, dueDate, context, preferences)
    }

    StartDateRow(
        startDate = current,
        selectedDay = selectedDay.longValue,
        hasStartAlarm = hasStartAlarm,
        hasDueDate = dueDate > 0L,
        dateTimeText = currentText.value,
        onClick = { showPicker.value = true}
    )

    LaunchedEffect(dueDate) {
        setCurrent(decodeStartDay(dueDate, selectedDay.longValue, selectedTime.intValue))
    }

    if (showPicker.value) {
        StartDateTimePicker(
            selectedDay = selectedDay.longValue,
            selectedTime = selectedTime.intValue,
            updateValues = { day, time -> selectedDay.longValue = day; selectedTime.intValue = time },
            accept = {
                setCurrent(decodeStartDay(dueDate, selectedDay.longValue, selectedTime.intValue))
                showPicker.value = false
            },
            dismiss = { showPicker.value = false },
            autoclose = preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen,false),
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

private fun encodeDay(current: Long, dueDate: Long): Long
{
    if (current <= 0L) return current
    return encodeStartDay(
        dueDate,
        current.startOfDay(),
        current.millisOfDay)
}

private fun encodeTime(current: Long, day: Long, dueDate: Long): Int =
    if (current > 0L &&
        day == dueDate.startOfDay() &&
        current.millisOfDay == dueDate.millisOfDay)
    {
        NO_TIME
    } else {
        current.millisOfDay
    }

private fun encodeStartDay(
    dueDate: Long,
    selectedDay: Long,
    selectedTime: Int
): Long {
    val dueDay = dueDate.startOfDay()
    val dueTime = dueDate.millisOfDay
    return when {
        dueDate <= 0 -> selectedDay
        dueDay == selectedDay -> if (selectedTime == dueTime) {
            DUE_TIME
        } else {
            DUE_DATE
        }
        dueDay.toDateTime().minusDays(1).millis == selectedDay ->
            DAY_BEFORE_DUE
        dueDay.toDateTime().minusDays(7).millis == selectedDay ->
            WEEK_BEFORE_DUE
        else -> selectedDay
    }
}

private fun decodeStartDay(dueDate: Long, selectedDay: Long, selectedTime: Int): Long {
    val due = dueDate.takeIf { it > 0 }?.toDateTime()
    return when (selectedDay) {
        DUE_DATE -> due?.withMillisOfDay(selectedTime)?.millis ?: 0
        DUE_TIME -> due?.millis ?: 0
        DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(selectedTime)?.millis ?: 0
        WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(selectedTime)?.millis ?: 0
        else -> selectedDay + selectedTime
    }
}

private suspend fun formatDateTimeText(
    selectedDay: Long,
    selectedTime: Int,
    dueDate: Long,
    context: Context,
    preferences: Preferences
): String {
    return when (encodeStartDay(dueDate, selectedDay, selectedTime)) {
        DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
        DUE_TIME -> context.getString(R.string.due_time)
        DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
        WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
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

// TODO: delete this code after migration together
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
            DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
            DUE_TIME -> context.getString(R.string.due_time)
            DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
            WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
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

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoStartDate() {
    TasksTheme {
        StartDateRow(
            startDate = 0L,
            selectedDay = NO_DAY,
            selectedTime = NO_TIME,
            currentTime = 1657080392000L,
            hasStartAlarm = true,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun FutureStartDate() {
    TasksTheme {
        StartDateRow(
            startDate = 1657080392000L,
            selectedDay = DUE_DATE,
            selectedTime = NO_TIME,
            currentTime = 1657080392000L,
            hasStartAlarm = true,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PastStartDate() {
    TasksTheme {
        StartDateRow(
            startDate = 1657080392000L,
            selectedDay = DUE_TIME,
            selectedTime = NO_TIME,
            currentTime = 1657080392001L,
            hasStartAlarm = true,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}