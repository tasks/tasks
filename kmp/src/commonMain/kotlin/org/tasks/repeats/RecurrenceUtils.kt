package org.tasks.repeats

import kotlin.jvm.JvmStatic

object RecurrenceUtils {
    const val LAST_DAY_OF_MONTH = -1

    @JvmStatic
    fun newRecur(): Recur = Recur(Frequency.DAILY)

    @JvmStatic
    fun newRecur(rrule: String): Recur = Recur.parse(rrule)

    val Recur.isLastDayOfMonth: Boolean
        get() = frequency == Frequency.MONTHLY &&
                byDay.isEmpty() &&
                byMonthDay.size == 1 &&
                byMonthDay[0] == LAST_DAY_OF_MONTH
}
