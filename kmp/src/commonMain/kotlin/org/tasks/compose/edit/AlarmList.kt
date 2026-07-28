package org.tasks.compose.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.ClearButton
import org.tasks.compose.pickers.RING_FIVE_TIMES
import org.tasks.compose.pickers.RING_NONSTOP
import org.tasks.data.entity.Alarm
import org.tasks.reminders.alarmText
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_reminder
import tasks.kmp.generated.resources.ring_five_times
import tasks.kmp.generated.resources.ring_nonstop
import tasks.kmp.generated.resources.ring_once

private const val DisabledAlpha = 0.38f

@Composable
fun AlarmList(
    alarms: ImmutableSet<Alarm>,
    ringMode: Int,
    isNew: Boolean,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
    is24HourFormat: Boolean,
    replaceAlarm: (Alarm) -> Unit,
    addAlarm: () -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        alarms.forEach { alarm ->
            AlarmListItem(
                alarm = alarm,
                hasStartDate = hasStartDate,
                hasDueDate = hasDueDate,
                is24HourFormat = is24HourFormat,
                onClick = { replaceAlarm(alarm) },
                remove = { deleteAlarm(alarm) },
            )
        }
        val showError = alarms.missingReminder(isNew, hasStartDate, hasDueDate)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.add_reminder),
                color = when {
                    showError -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                    .defaultMinSize(24.dp)
                    .clickable(onClick = addAlarm)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (alarms.isNotEmpty()) {
                Text(
                    text = stringResource(ringModeString(ringMode)),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .clickable(onClick = openRingType),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun ringModeString(ringMode: Int) = when (ringMode) {
    RING_NONSTOP -> Res.string.ring_nonstop
    RING_FIVE_TIMES -> Res.string.ring_five_times
    else -> Res.string.ring_once
}

fun ImmutableSet<Alarm>.missingReminder(
    isNew: Boolean,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
): Boolean = isNew && isEmpty() && (hasDueDate || hasStartDate)

val Alarm.isEditable: Boolean
    get() = when (type) {
        Alarm.TYPE_DATE_TIME, Alarm.TYPE_REL_START, Alarm.TYPE_REL_END, Alarm.TYPE_RANDOM -> true
        else -> false
    }

@Composable
fun AlarmListItem(
    alarm: Alarm,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
    is24HourFormat: Boolean,
    onClick: () -> Unit,
    remove: () -> Unit,
    modifier: Modifier = Modifier,
    textPadding: Dp = 0.dp,
) {
    val color = when (alarm.type) {
        Alarm.TYPE_REL_START -> if (hasStartDate) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.error
        }
        Alarm.TYPE_REL_END -> if (hasDueDate) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.error
        }
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .let { if (alarm.isEditable) it.clickable { onClick() } else it }
    ) {
        Text(
            text = alarmText(alarm, is24HourFormat),
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(start = textPadding)
                .weight(weight = 1f),
            color = color,
        )
        ClearButton(onClick = remove)
    }
}
