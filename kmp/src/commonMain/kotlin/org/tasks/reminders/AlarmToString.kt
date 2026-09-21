package org.tasks.reminders

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.entity.Alarm
import org.tasks.extensions.localizedNumber
import org.tasks.compose.rememberDateFormatter
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.time.ONE_DAY
import org.tasks.time.ONE_HOUR
import org.tasks.time.ONE_MINUTE
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.alarm_after_due
import tasks.kmp.generated.resources.alarm_after_start
import tasks.kmp.generated.resources.alarm_before_due
import tasks.kmp.generated.resources.alarm_before_start
import tasks.kmp.generated.resources.randomly_every
import tasks.kmp.generated.resources.repeat_n_days
import tasks.kmp.generated.resources.repeat_n_hours
import tasks.kmp.generated.resources.repeat_n_minutes
import tasks.kmp.generated.resources.repeat_n_weeks
import tasks.kmp.generated.resources.repeat_times
import tasks.kmp.generated.resources.repeats_plural_number_of_times
import tasks.kmp.generated.resources.snoozed_until
import tasks.kmp.generated.resources.when_due
import tasks.kmp.generated.resources.when_started
import kotlin.math.absoluteValue

private fun durationComponents(duration: Long): List<Pair<PluralStringResource, Int>> {
    val millis = duration.absoluteValue
    val totalDays = millis / ONE_DAY
    val weeks = (totalDays / 7).toInt()
    val days = (totalDays - weeks * 7).toInt()
    val hours = (millis / ONE_HOUR - totalDays * 24).toInt()
    val minutes = (millis / ONE_MINUTE - millis / ONE_HOUR * 60).toInt()
    return buildList {
        if (weeks > 0) add(Res.plurals.repeat_n_weeks to weeks)
        if (days > 0) add(Res.plurals.repeat_n_days to days)
        if (hours > 0) add(Res.plurals.repeat_n_hours to hours)
        if (minutes > 0) add(Res.plurals.repeat_n_minutes to minutes)
    }
}

@Composable
fun durationString(duration: Long): String =
    durationComponents(duration)
        .map { (resource, count) -> pluralStringResource(resource, count, localizedNumber(count)) }
        .joinToString(" ")

suspend fun getDurationString(duration: Long): String =
    durationComponents(duration)
        .map { (resource, count) -> getPluralString(resource, count, localizedNumber(count)) }
        .joinToString(" ")

@Composable
fun repeatString(repeat: Int, interval: Long): String = stringResource(
    Res.string.repeats_plural_number_of_times,
    durationString(interval),
    localizedNumber(repeat),
    pluralStringResource(Res.plurals.repeat_times, repeat),
)

suspend fun getRepeatString(repeat: Int, interval: Long): String = getString(
    Res.string.repeats_plural_number_of_times,
    getDurationString(interval),
    localizedNumber(repeat),
    getPluralString(Res.plurals.repeat_times, repeat),
)

private sealed interface AlarmLabel {
    data class Literal(val text: String) : AlarmLabel
    data class Simple(val resource: StringResource) : AlarmLabel
    data class WithDuration(val resource: StringResource, val duration: Long) : AlarmLabel
    data class WithText(val resource: StringResource, val arg: String) : AlarmLabel
}

private fun alarmLabel(
    alarm: Alarm,
    dateFormatter: DateFormatter?,
): AlarmLabel? = when (alarm.type) {
    Alarm.TYPE_REL_START ->
        if (alarm.time == 0L) {
            AlarmLabel.Simple(Res.string.when_started)
        } else {
            AlarmLabel.WithDuration(
                if (alarm.time < 0) Res.string.alarm_before_start else Res.string.alarm_after_start,
                alarm.time,
            )
        }
    Alarm.TYPE_REL_END ->
        if (alarm.time == 0L) {
            AlarmLabel.Simple(Res.string.when_due)
        } else {
            AlarmLabel.WithDuration(
                if (alarm.time < 0) Res.string.alarm_before_due else Res.string.alarm_after_due,
                alarm.time,
            )
        }
    Alarm.TYPE_RANDOM ->
        AlarmLabel.WithDuration(Res.string.randomly_every, alarm.time)
    Alarm.TYPE_SNOOZE ->
        dateFormatter?.let {
            AlarmLabel.WithText(Res.string.snoozed_until, it.fullDateTime(alarm.time))
        }
    else ->
        dateFormatter?.let { AlarmLabel.Literal(it.fullDateTime(alarm.time)) }
}

@Composable
fun alarmText(alarm: Alarm, is24HourFormat: Boolean): String {
    val reminder = when (val label = alarmLabel(alarm, rememberDateFormatter(is24HourFormat))) {
        null -> return ""
        is AlarmLabel.Literal -> label.text
        is AlarmLabel.Simple -> stringResource(label.resource)
        is AlarmLabel.WithDuration -> stringResource(label.resource, durationString(label.duration))
        is AlarmLabel.WithText -> stringResource(label.resource, label.arg)
    }
    return if (alarm.repeat > 0) {
        reminder + "\n" + repeatString(alarm.repeat, alarm.interval)
    } else {
        reminder
    }
}

suspend fun getAlarmText(alarm: Alarm, is24HourFormat: Boolean): String {
    val reminder = when (val label = alarmLabel(alarm, DateFormatter.create(is24HourFormat))) {
        null -> return ""
        is AlarmLabel.Literal -> label.text
        is AlarmLabel.Simple -> getString(label.resource)
        is AlarmLabel.WithDuration -> getString(label.resource, getDurationString(label.duration))
        is AlarmLabel.WithText -> getString(label.resource, label.arg)
    }
    return if (alarm.repeat > 0) {
        reminder + "\n" + getRepeatString(alarm.repeat, alarm.interval)
    } else {
        reminder
    }
}
