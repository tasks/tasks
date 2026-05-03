package org.tasks.data

import net.fortuna.ical4j.model.Recur
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.ONE_WEEK
import org.tasks.time.startOfDay

/** Checks whether task is hidden. Requires HIDDEN_UNTIL  */
val Task.isHidden
    get() = hideUntil > currentTimeMillis()

/**
 * Create hide until for this task.
 *
 * @param setting one of the HIDE_UNTIL_* constants
 * @param customDate if specific day is set, this value
 */
fun Task.createHideUntil(setting: Int, customDate: Long): Long {
    val date: Long = when (setting) {
        Task.HIDE_UNTIL_NONE -> return 0
        Task.HIDE_UNTIL_DUE, Task.HIDE_UNTIL_DUE_TIME -> dueDate
        Task.HIDE_UNTIL_DAY_BEFORE -> dueDate - ONE_DAY
        Task.HIDE_UNTIL_WEEK_BEFORE -> dueDate - ONE_WEEK
        Task.HIDE_UNTIL_SPECIFIC_DAY, Task.HIDE_UNTIL_SPECIFIC_DAY_TIME -> customDate
        else -> throw IllegalArgumentException("Unknown setting $setting")
    }
    if (date <= 0) {
        return date
    }
    return if (setting == Task.HIDE_UNTIL_SPECIFIC_DAY_TIME ||
        setting == Task.HIDE_UNTIL_DUE_TIME && Task.hasDueTime(dueDate)
    ) {
        date.toDateTime().withSecondOfMinute(1).withMillisOfSecond(0).millis
    } else {
        date.startOfDay()
    }
}

val Task.isOverdue: Boolean
    get() {
        if (isCompleted || !hasDueDate()) {
            return false
        }
        val compareTo = if (hasDueTime()) currentTimeMillis() else currentTimeMillis().startOfDay()
        return dueDate < compareTo
    }

fun Task.setRecurrence(rrule: Recur?) {
    recurrence = rrule?.toString()
}

fun Task.hasNotes(): Boolean {
    return !notes.isNullOrEmpty()
}

/**
 * Creates due date for this task. If this due date has no time associated, we move it to the last
 * millisecond of the day.
 *
 * @param setting one of the URGENCY_* constants
 * @param customDate if specific day or day & time is set, this value
 */
fun createDueDate(setting: Int, customDate: Long): Long {
    val date: Long = when (setting) {
        Task.URGENCY_NONE -> 0
        Task.URGENCY_TODAY -> currentTimeMillis()
        Task.URGENCY_TOMORROW -> currentTimeMillis() + ONE_DAY
        Task.URGENCY_DAY_AFTER -> currentTimeMillis() + 2 * ONE_DAY
        Task.URGENCY_NEXT_WEEK -> currentTimeMillis() + ONE_WEEK
        Task.URGENCY_IN_TWO_WEEKS -> currentTimeMillis() + 2 * ONE_WEEK
        Task.URGENCY_SPECIFIC_DAY, Task.URGENCY_SPECIFIC_DAY_TIME -> customDate
        else -> throw IllegalArgumentException("Unknown setting $setting")
    }
    if (date <= 0) {
        return date
    }
    var dueDate = DateTimeUtils.newDateTime(date).withMillisOfSecond(0)
    dueDate = if (setting != Task.URGENCY_SPECIFIC_DAY_TIME) {
        dueDate
            .withHourOfDay(12)
            .withMinuteOfHour(0)
            .withSecondOfMinute(0) // Seconds == 0 means no due time
    } else {
        dueDate.withSecondOfMinute(1) // Seconds > 0 means due time exists
    }
    return dueDate.millis
}
