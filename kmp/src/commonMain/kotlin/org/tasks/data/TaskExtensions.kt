package org.tasks.data

import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.repeats.Recur
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.daysUntil
import org.tasks.time.dueDateOverdue
import org.tasks.time.endOfDay
import org.tasks.time.minusDays
import org.tasks.time.plusDays
import org.tasks.time.shiftFollowingDueDate
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
        Task.HIDE_UNTIL_DAY_BEFORE -> dueDate.minusDays(1)
        Task.HIDE_UNTIL_WEEK_BEFORE -> dueDate.minusDays(7)
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

fun Task.setDueDateAdjustingHideUntil(newDueDate: Long, shiftWithinDay: Boolean = false) {
    if (dueDate > 0 && hideUntil > 0) {
        hideUntil = when {
            newDueDate <= 0 -> 0
            hideUntil == dueDate -> newDueDate
            else -> {
                val shifted = if (shiftWithinDay) {
                    hideUntil.shiftFollowingDueDate(dueDate, newDueDate)
                } else {
                    hideUntil.plusDays(dueDate.daysUntil(newDueDate))
                }
                val startDate = if (hasStartTime()) shifted else shifted.startOfDay()
                val latestStartDate = if (Task.hasDueTime(newDueDate)) {
                    newDueDate
                } else {
                    newDueDate.endOfDay()
                }
                if (hideUntil < dueDate) startDate.coerceAtMost(latestStartDate) else startDate
            }
        }
    }
    dueDate = newDueDate
}

val Task.isOverdue: Boolean
    get() = !isCompleted && dueDateOverdue(dueDate)

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
        Task.URGENCY_TOMORROW -> currentTimeMillis().plusDays(1)
        Task.URGENCY_DAY_AFTER -> currentTimeMillis().plusDays(2)
        Task.URGENCY_NEXT_WEEK -> currentTimeMillis().plusDays(7)
        Task.URGENCY_IN_TWO_WEEKS -> currentTimeMillis().plusDays(14)
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
