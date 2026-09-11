package org.tasks.repeats

import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

fun String?.anchoredToDueDate(dueDate: Long): String? {
    val rule = this?.takeIf { it.isNotBlank() } ?: return this
    val recur = try {
        newRecur(rule)
    } catch (e: Exception) {
        return this
    }
    if (recur.frequency != Recur.Frequency.MONTHLY || recur.dayList.isEmpty()) {
        return this
    }
    val date = DateTime(dueDate.takeIf { it > 0 } ?: currentTimeMillis())
    val dayOfWeekInMonth = date.dayOfWeekInMonth
    val num = if (recur.dayList[0].offset == -1 || dayOfWeekInMonth == 5) {
        if (dayOfWeekInMonth == date.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
    } else {
        dayOfWeekInMonth
    }
    recur.dayList.let {
        it.clear()
        it.add(WeekDay(date.weekDay, num))
    }
    return recur.toString()
}
