package org.tasks.repeats

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.repeats.Frequency.DAILY
import org.tasks.repeats.Frequency.HOURLY
import org.tasks.repeats.Frequency.MINUTELY
import org.tasks.repeats.Frequency.MONTHLY
import org.tasks.repeats.Frequency.WEEKLY
import org.tasks.repeats.Frequency.YEARLY
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.repeats.RecurrenceUtils.LAST_DAY_OF_MONTH
import org.tasks.repeats.RecurrenceUtils.isLastDayOfMonth
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Calendar.DAY_OF_WEEK_IN_MONTH
import java.util.Locale

open class CustomRecurrenceViewModel(
    rrule: String?,
    dueDate: Long,
    accountType: Int,
    locale: Locale,
) : ViewModel() {
    data class ViewState(
        val interval: Int = 1,
        val frequency: Frequency = WEEKLY,
        val dueDate: Long = currentTimeMillis().startOfDay(),
        val endSelection: Int = 0,
        val endDate: Long = dueDate.toDateTime().plusMonths(1).startOfDay().millis,
        val endCount: Int = 1,
        val frequencyOptions: List<Frequency> = FREQ_ALL,
        val daysOfWeek: List<DayOfWeek> = Locale.getDefault().daysOfWeek(),
        val selectedDays: List<DayOfWeek> = emptyList(),
        val locale: Locale = Locale.getDefault(),
        val monthDay: ByDay? = null,
        val lastDayOfMonth: Boolean = false,
        val openedWithLastDayOfMonth: Boolean = false,
        val openedWithLastWeekOfMonth: Boolean = false,
        val isMicrosoftTask: Boolean = false,
    ) {
        val dueDayOfWeek: DayOfWeek
            get() = Instant.ofEpochMilli(dueDate).atZone(ZoneId.systemDefault()).dayOfWeek

        val dueDayOfMonth: Int
            get() = DateTime(dueDate).dayOfMonth

        val dueIsLastDayOfMonth: Boolean
            get() = DateTime(dueDate).isLastDayOfMonth

        val showLastDayOfMonth: Boolean
            get() = dueIsLastDayOfMonth || openedWithLastDayOfMonth

        val nthWeek: Int
            get() =
                Calendar.getInstance(locale)
                    .apply { timeInMillis = dueDate }
                    .get(DAY_OF_WEEK_IN_MONTH)

        val lastWeekDayOfMonth: Boolean
            get() =
                Calendar.getInstance(locale)
                    .apply { timeInMillis = dueDate }
                    .let { it[DAY_OF_WEEK_IN_MONTH] == it.getActualMaximum(DAY_OF_WEEK_IN_MONTH) }

        val showLastWeekOfMonth: Boolean
            get() = lastWeekDayOfMonth || openedWithLastWeekOfMonth
    }

    private val _state = MutableStateFlow(ViewState())
    val state = _state.asStateFlow()

    init {
        val daysOfWeek = locale.daysOfWeek()
        val recur = rrule
            ?.takeIf { it.isNotBlank() }
            ?.let { Recur.parse(it) }
        val resolvedDueDate = dueDate
            .takeIf { it > 0 }
            ?: currentTimeMillis().startOfDay()
        val isMicrosoftTask = accountType == TYPE_MICROSOFT
        val frequencies = if (isMicrosoftTask) FREQ_MICROSOFT else FREQ_ALL
        val lastDayOfMonth = recur
            ?.takeIf { !isMicrosoftTask }
            ?.isLastDayOfMonth
            ?: false
        val monthDay = recur
            ?.byDay
            ?.takeIf { recur.frequency == MONTHLY && !isMicrosoftTask }
            ?.firstOrNull()
        _state.update { state ->
            state.copy(
                interval = recur?.interval?.takeIf { it > 0 } ?: 1,
                frequency = recur?.frequency?.takeIf { frequencies.contains(it) } ?: WEEKLY,
                dueDate = resolvedDueDate,
                endSelection = when {
                    isMicrosoftTask -> 0
                    recur == null -> 0
                    recur.until != null -> 1
                    recur.count != null -> 2
                    else -> 0
                },
                endDate = DateTime(resolvedDueDate).plusMonths(1).startOfDay().millis,
                endCount = recur?.count ?: 1,
                daysOfWeek = daysOfWeek,
                selectedDays = recur
                    ?.byDay
                    ?.takeIf { recur.frequency == WEEKLY }
                    ?.toDaysOfWeek()
                    ?: emptyList(),
                locale = locale,
                monthDay = monthDay,
                lastDayOfMonth = lastDayOfMonth,
                openedWithLastDayOfMonth = lastDayOfMonth,
                openedWithLastWeekOfMonth = monthDay?.offset == -1,
                isMicrosoftTask = isMicrosoftTask,
                frequencyOptions = frequencies,
            )
        }
    }

    fun setEndType(endType: Int) {
        _state.update {
            it.copy(endSelection = endType)
        }
    }

    fun setFrequency(frequency: Frequency) {
        _state.update {
            it.copy(frequency = frequency)
        }
    }

    fun setEndDate(endDate: Long) {
        _state.update {
            it.copy(endDate = endDate)
        }
    }

    fun setInterval(interval: Int) {
        _state.update {
            it.copy(interval = interval)
        }
    }

    fun setOccurrences(occurrences: Int) {
        _state.update {
            it.copy(endCount = occurrences)
        }
    }

    fun toggleDay(dayOfWeek: DayOfWeek) {
        _state.update { state ->
            state.copy(
                selectedDays = state.selectedDays.toMutableList().also {
                    if (!it.remove(dayOfWeek)) {
                        it.add(dayOfWeek)
                    }
                }
            )
        }
    }

    fun getRecur(): String {
        val state = _state.value
        return Recur(
            frequency = state.frequency,
            byDay = when (state.frequency) {
                WEEKLY -> state.selectedDays.toByDay()
                MONTHLY -> listOfNotNull(state.monthDay.takeUnless { state.lastDayOfMonth })
                else -> emptyList()
            },
            byMonthDay = if (state.frequency == MONTHLY && state.lastDayOfMonth) listOf(LAST_DAY_OF_MONTH) else emptyList(),
            interval = state.interval.takeIf { it > 1 },
            until = if (state.endSelection == 1) {
                DateTime(state.endDate).let { Until.Date(it.year, it.monthOfYear, it.dayOfMonth) }
            } else {
                null
            },
            count = if (state.endSelection == 2) state.endCount.coerceAtLeast(1) else null,
        ).toString()
    }

    fun setMonthSelection(selection: Int) {
        _state.update {
            it.copy(
                monthDay = when (selection) {
                    0, 3 -> null
                    1 -> ByDay(it.dueDayOfWeek.weekday, it.nthWeek)
                    2 -> ByDay(it.dueDayOfWeek.weekday, -1)
                    else -> throw IllegalArgumentException()
                },
                lastDayOfMonth = selection == 3,
            )
        }
    }

    companion object {
        val FREQ_ALL = listOf(MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY)
        val FREQ_MICROSOFT = listOf(DAILY, WEEKLY, MONTHLY, YEARLY)

        private fun Locale.daysOfWeek(): List<DayOfWeek> {
            val values = DayOfWeek.values()
            val weekFields = WeekFields.of(this)
            var index = values.indexOf(weekFields.firstDayOfWeek)
            return (0..6).map {
                values[index].also { index = (index + 1) % 7 }
            }
        }

        private fun List<ByDay>.toDaysOfWeek(): List<DayOfWeek> = map {
            when (it.day) {
                Weekday.SU -> DayOfWeek.SUNDAY
                Weekday.MO -> DayOfWeek.MONDAY
                Weekday.TU -> DayOfWeek.TUESDAY
                Weekday.WE -> DayOfWeek.WEDNESDAY
                Weekday.TH -> DayOfWeek.THURSDAY
                Weekday.FR -> DayOfWeek.FRIDAY
                Weekday.SA -> DayOfWeek.SATURDAY
            }
        }

        private fun List<DayOfWeek>.toByDay(): List<ByDay> =
            sortedBy { it.value }.map { ByDay(it.weekday) }

        private val DayOfWeek.weekday: Weekday
            get() = when (this) {
                DayOfWeek.SUNDAY -> Weekday.SU
                DayOfWeek.MONDAY -> Weekday.MO
                DayOfWeek.TUESDAY -> Weekday.TU
                DayOfWeek.WEDNESDAY -> Weekday.WE
                DayOfWeek.THURSDAY -> Weekday.TH
                DayOfWeek.FRIDAY -> Weekday.FR
                DayOfWeek.SATURDAY -> Weekday.SA
            }
    }
}
