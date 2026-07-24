package org.tasks.compose.pickers

import org.tasks.data.entity.Task
import org.tasks.time.millisOfDay
import org.tasks.time.minusDays
import org.tasks.time.startOfDay
import org.tasks.time.startOfMinute
import org.tasks.time.withMillisOfDay

sealed interface StartDate {
    data object None : StartDate
    data object DueDate : StartDate
    data object DueTime : StartDate
    data object DayBeforeDue : StartDate
    data object WeekBeforeDue : StartDate
    data class Absolute(val startOfDay: Long) : StartDate

    val isRelative: Boolean
        get() = this == DueDate || this == DueTime || this == DayBeforeDue || this == WeekBeforeDue
}

fun StartDate.toStartDay(): Long = when (this) {
    StartDate.None -> NO_DAY
    StartDate.DueDate -> DUE_DATE
    StartDate.DueTime -> DUE_TIME
    StartDate.DayBeforeDue -> DAY_BEFORE_DUE
    StartDate.WeekBeforeDue -> WEEK_BEFORE_DUE
    is StartDate.Absolute -> startOfDay
}

fun startDayOf(day: Long): StartDate = when (day) {
    NO_DAY -> StartDate.None
    DUE_DATE -> StartDate.DueDate
    DUE_TIME -> StartDate.DueTime
    DAY_BEFORE_DUE -> StartDate.DayBeforeDue
    WEEK_BEFORE_DUE -> StartDate.WeekBeforeDue
    else -> StartDate.Absolute(day)
}

fun defaultHideUntilDay(setting: Int): Long = when (setting) {
    Task.HIDE_UNTIL_DUE -> DUE_DATE
    Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
    Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
    Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
    else -> NO_DAY
}

data class StartDateSelection(val day: StartDate, val time: Int)

fun resolveStartDate(day: StartDate, time: Int, dueDate: Long): Long {
    val due = dueDate.takeIf { it > 0 }
    val resolved = when (day) {
        StartDate.None -> 0L
        StartDate.DueTime -> return due ?: 0L
        StartDate.DueDate -> due?.withStartTime(time) ?: 0L
        StartDate.DayBeforeDue -> due?.minusDays(1)?.withStartTime(time) ?: 0L
        StartDate.WeekBeforeDue -> due?.minusDays(7)?.withStartTime(time) ?: 0L
        is StartDate.Absolute -> day.startOfDay.withStartTime(time)
    }
    return resolved.withTimeMarkerOr { it.startOfDay() }
}

private fun Long.withStartTime(time: Int): Long =
    if (time > NO_TIME) withMillisOfDay(time) else startOfDay()

fun Long.withTimeMarkerOr(floor: (Long) -> Long): Long = when {
    this <= 0 -> 0L
    Task.hasDueTime(this) -> startOfMinute() + 1000
    else -> floor(this)
}

fun initialStartSelection(
    hideUntil: Long,
    dueDate: Long,
    isNew: Boolean,
    defaultHideUntil: Int,
): Pair<Long, Int> = when {
    hideUntil > 0 -> startSelectionDays(hideUntil, dueDate)
    isNew -> defaultHideUntilDay(defaultHideUntil) to NO_TIME
    else -> NO_DAY to NO_TIME
}

fun startSelectionDays(hideUntil: Long, dueDate: Long): Pair<Long, Int> =
    startDateSelection(hideUntil, dueDate).let { it.day.toStartDay() to it.time }

fun startDateSelection(hideUntil: Long, dueDate: Long): StartDateSelection {
    if (hideUntil <= 0) return StartDateSelection(StartDate.None, NO_TIME)
    val day = hideUntil.startOfDay()
    var time = hideUntil.millisOfDay
    val dueDay = dueDate.startOfDay()
    val dueTime = dueDate.millisOfDay
    val resolvedDay: StartDate = when (day) {
        dueDay -> if (time == dueTime) {
            time = NO_TIME
            StartDate.DueTime
        } else {
            StartDate.DueDate
        }
        dueDay.minusDays(1) -> StartDate.DayBeforeDue
        dueDay.minusDays(7) -> StartDate.WeekBeforeDue
        else -> StartDate.Absolute(day)
    }
    return StartDateSelection(resolvedDay, time)
}
