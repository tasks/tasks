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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.tasks.kmp.firstDayOfWeek

open class CustomRecurrenceViewModel(
    rrule: String?,
    dueDate: Long,
    accountType: Int,
) : ViewModel() {
    data class ViewState(
        val interval: Int = 1,
        val frequency: Frequency = WEEKLY,
        val dueDate: Long = currentTimeMillis().startOfDay(),
        val endSelection: Int = 0,
        val endDate: Long = dueDate.toDateTime().plusMonths(1).startOfDay().millis,
        val endCount: Int = 1,
        val frequencyOptions: List<Frequency> = FREQ_ALL,
        val daysOfWeek: List<DayOfWeek> = localeDaysOfWeek(),
        val selectedDays: List<DayOfWeek> = emptyList(),
        val monthDay: ByDay? = null,
        val lastDayOfMonth: Boolean = false,
        val openedWithLastDayOfMonth: Boolean = false,
        val openedWithLastWeekOfMonth: Boolean = false,
        val isMicrosoftTask: Boolean = false,
    ) {
        val dueDayOfWeek: DayOfWeek
            get() = DateTime(dueDate).weekday.dayOfWeek

        val dueDayOfMonth: Int
            get() = DateTime(dueDate).dayOfMonth

        val dueIsLastDayOfMonth: Boolean
            get() = DateTime(dueDate).isLastDayOfMonth

        val showLastDayOfMonth: Boolean
            get() = dueIsLastDayOfMonth || openedWithLastDayOfMonth

        val nthWeek: Int
            get() = DateTime(dueDate).dayOfWeekInMonth

        val lastWeekDayOfMonth: Boolean
            get() = DateTime(dueDate).let { it.dayOfWeekInMonth == it.maxDayOfWeekInMonth }

        val showLastWeekOfMonth: Boolean
            get() = lastWeekDayOfMonth || openedWithLastWeekOfMonth
    }

    private val _state = MutableStateFlow(ViewState())
    val state = _state.asStateFlow()

    init {
        val daysOfWeek = localeDaysOfWeek()
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
                    1 -> ByDay(it.dueDayOfWeek.toWeekday(), it.nthWeek)
                    2 -> ByDay(it.dueDayOfWeek.toWeekday(), -1)
                    else -> throw IllegalArgumentException()
                },
                lastDayOfMonth = selection == 3,
            )
        }
    }

    companion object {
        val FREQ_ALL = listOf(MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY)
        val FREQ_MICROSOFT = listOf(DAILY, WEEKLY, MONTHLY, YEARLY)

        private fun localeDaysOfWeek(): List<DayOfWeek> {
            val first = firstDayOfWeek().isoDayNumber
            return (0..6).map { DayOfWeek((first - 1 + it) % 7 + 1) }
        }

        private fun List<ByDay>.toDaysOfWeek(): List<DayOfWeek> = map { it.day.dayOfWeek }

        private fun List<DayOfWeek>.toByDay(): List<ByDay> =
            sortedBy { it.isoDayNumber }.map { ByDay(it.toWeekday()) }
    }
}
