package org.tasks.compose.pickers

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NextWeek
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.DateTimePicker.Companion.MULTIPLE_DAYS
import org.tasks.dialogs.DateTimePicker.Companion.MULTIPLE_TIMES
import org.tasks.dialogs.DateTimePicker.Companion.NO_DAY
import org.tasks.dialogs.DateTimePicker.Companion.NO_TIME
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getFullDate
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.minusDays
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY

@Composable
fun DatePickerShortcuts(
    dateShortcuts: @Composable ColumnScope.() -> Unit,
    timeShortcuts: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            dateShortcuts()
        }
        Spacer(modifier = Modifier.weight(1f))
        Column {
            timeShortcuts()
        }
    }
}

@Composable
fun StartDateShortcuts(
    selected: Long,
    selectedDay: (Long) -> Unit,
    selectedDayTime: (Long, Int) -> Unit,
    clearDate: () -> Unit,
) {
    var custom by remember { mutableLongStateOf(0) }
    LaunchedEffect(selected) {
        custom = if (selected !in listOf(DUE_DATE, DUE_TIME, DAY_BEFORE_DUE, WEEK_BEFORE_DUE, NO_DAY)) {
            selected
        } else {
            custom
        }
    }

    if (custom > 0 || custom == MULTIPLE_DAYS) {
        ShortcutButton(
            icon = Icons.Outlined.Today,
            text = if (custom == MULTIPLE_DAYS) {
                stringResource(R.string.date_picker_multiple)
            } else {
                remember(custom) {
                    runBlocking {
                        if (custom < currentTimeMillis().startOfDay().minusDays(1)) {
                            getFullDate(custom, style = DateStyle.LONG)
                        } else {
                            getRelativeDay(custom, style = DateStyle.LONG)
                        }

                    }
                }
            },
            selected = selected == custom,
            onClick = { selectedDay(custom) },
        )
    }
    ShortcutButton(
        icon = Icons.Outlined.Today,
        text = stringResource(R.string.due_date),
        selected = selected == DUE_DATE,
        onClick = { selectedDay(DUE_DATE) },
    )
    ShortcutButton(
        icon = Icons.Outlined.Schedule,
        text = stringResource(R.string.due_time),
        selected = selected == DUE_TIME,
        onClick = { selectedDayTime(DUE_TIME, NO_TIME) },
    )
    ShortcutButton(
        icon = Icons.Outlined.WbSunny,
        text = stringResource(R.string.day_before_due),
        selected = selected == DAY_BEFORE_DUE,
        onClick = { selectedDay(DAY_BEFORE_DUE) },
    )
    ShortcutButton(
        icon = Icons.Outlined.CalendarViewWeek,
        text = stringResource(R.string.week_before_due),
        selected = selected == WEEK_BEFORE_DUE,
        onClick = { selectedDay(WEEK_BEFORE_DUE) },
    )
    ShortcutButton(
        icon = Icons.Outlined.Block,
        text = stringResource(R.string.no_date),
        selected = selected == NO_DAY,
        onClick = { clearDate() },
    )
}

@Composable
fun DueDateShortcuts(
    today: Long,
    tomorrow: Long,
    nextWeek: Long,
    selected: Long,
    showNoDate: Boolean,
    selectedDay: (Long) -> Unit,
    clearDate: () -> Unit,
) {
    var custom by remember { mutableLongStateOf(0) }
    LaunchedEffect(selected) {
        custom = if (selected == MULTIPLE_DAYS || selected !in listOf(today, tomorrow, nextWeek, NO_DAY)) {
            selected
        } else {
            custom
        }
    }

    if (custom > 0 || custom == MULTIPLE_DAYS) {
        ShortcutButton(
            icon = Icons.Outlined.Today,
            text = if (custom == MULTIPLE_DAYS) {
                stringResource(R.string.date_picker_multiple)
            } else {
                remember(custom) {
                    runBlocking {
                        if (custom < today.minusDays(1)) {
                            getFullDate(custom, style = DateStyle.LONG)
                        } else {
                            getRelativeDay(custom, style = DateStyle.LONG)
                        }

                    }
                }
            },
            selected = selected == custom,
            onClick = { selectedDay(custom) },
        )
    }
    ShortcutButton(
        icon = Icons.Outlined.Today,
        text = stringResource(R.string.today),
        selected = selected == today,
        onClick = { selectedDay(today) },
    )
    ShortcutButton(
        icon = Icons.Outlined.WbSunny,
        text = stringResource(R.string.tomorrow),
        selected = selected == tomorrow,
        onClick = { selectedDay(tomorrow) },
    )
    ShortcutButton(
        icon = Icons.AutoMirrored.Outlined.NextWeek,
        text = stringResource(
            remember {
                when (newDateTime(nextWeek).dayOfWeek) {
                    SUNDAY -> R.string.next_sunday
                    MONDAY -> R.string.next_monday
                    TUESDAY -> R.string.next_tuesday
                    WEDNESDAY -> R.string.next_wednesday
                    THURSDAY -> R.string.next_thursday
                    FRIDAY -> R.string.next_friday
                    SATURDAY -> R.string.next_saturday
                    else -> throw IllegalArgumentException()
                }
            }
        ),
        selected = selected == nextWeek,
        onClick = { selectedDay(nextWeek) },
    )
    if (showNoDate) {
        ShortcutButton(
            icon = Icons.Outlined.Block,
            text = stringResource(R.string.no_date),
            selected = selected == NO_DAY,
            onClick = { clearDate() },
        )
    }
}

@Composable
fun TimeShortcuts(
    day: Long,
    selected: Int,
    morning: Int,
    afternoon: Int,
    evening: Int,
    night: Int,
    selectedMillisOfDay: (Int) -> Unit,
    pickTime: () -> Unit,
    clearTime: () -> Unit,
) {
    var custom by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected) {
        custom = if (selected == MULTIPLE_TIMES || selected !in listOf(morning, afternoon, evening, night, NO_TIME)) {
            selected
        } else {
            custom
        }
    }

    val is24HourFormat = LocalContext.current.is24HourFormat
    val now = remember { currentTimeMillis() }
    if (custom > 0 || custom == MULTIPLE_TIMES) {
        ShortcutButton(
            icon = Icons.Outlined.AccessTime,
            text = if (custom == MULTIPLE_TIMES) {
                stringResource(R.string.date_picker_multiple)
            } else {
                remember(custom) {
                    getTimeString(now.withMillisOfDay(custom), is24HourFormat)
                }
            },
            selected = selected == custom,
            onClick = { selectedMillisOfDay(custom) },
        )
    }
    ShortcutButton(
        icon = Icons.Outlined.Coffee,
        text = remember {
            getTimeString(now.withMillisOfDay(morning), is24HourFormat)
        },
        selected = selected == morning,
        onClick = { selectedMillisOfDay(morning) },
    )
    ShortcutButton(
        icon = Icons.Outlined.WbSunny,
        text = remember {
            getTimeString(now.withMillisOfDay(afternoon), is24HourFormat)
        },
        selected = selected == afternoon,
        onClick = { selectedMillisOfDay(afternoon) },
    )
    ShortcutButton(
        icon = Icons.Outlined.WbTwilight,
        text = remember {
            getTimeString(now.withMillisOfDay(evening), is24HourFormat)
        },
        selected = selected == evening,
        onClick = { selectedMillisOfDay(evening) },
    )
    ShortcutButton(
        icon = Icons.Outlined.NightsStay,
        text = remember {
            getTimeString(now.withMillisOfDay(night), is24HourFormat)
        },
        selected = selected == night,
        onClick = { selectedMillisOfDay(night) },
    )
    ShortcutButton(
        icon = Icons.Outlined.AccessTime,
        text = stringResource(R.string.shortcut_pick_time),
        selected = false,
        onClick = { pickTime() },
    )
    ShortcutButton(
        icon = Icons.Outlined.Block,
        text = stringResource(R.string.no_time),
        selected = day != DUE_TIME && selected == NO_TIME,
        onClick = { clearTime() },
    )
}

@Composable
fun ShortcutButton(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    TextButton(
        onClick = { onClick() },
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(color)
            )
            Text(
                text = text,
            )
        }
    }
}
