package org.tasks.repeats

import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

fun String?.anchoredToDueDate(dueDate: Long): String? {
    val rule = this?.takeIf { it.isNotBlank() } ?: return this
    val recur = try {
        Recur.parse(rule)
    } catch (e: IllegalArgumentException) {
        return this
    }
    if (recur.frequency != Frequency.MONTHLY || recur.byDay.isEmpty()) {
        return this
    }
    val date = DateTime(dueDate.takeIf { it > 0 } ?: currentTimeMillis())
    val dayOfWeekInMonth = date.dayOfWeekInMonth
    val num = if (recur.byDay[0].offset == -1 || dayOfWeekInMonth == 5) {
        if (dayOfWeekInMonth == date.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
    } else {
        dayOfWeekInMonth
    }
    return recur.copy(byDay = listOf(ByDay(date.weekday, num))).toString()
}
