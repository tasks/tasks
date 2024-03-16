package org.tasks.repeats

import androidx.lifecycle.SavedStateHandle
import net.fortuna.ical4j.model.Recur.Frequency.DAILY
import net.fortuna.ical4j.model.Recur.Frequency.HOURLY
import net.fortuna.ical4j.model.Recur.Frequency.MINUTELY
import net.fortuna.ical4j.model.Recur.Frequency.MONTHLY
import net.fortuna.ical4j.model.Recur.Frequency.SECONDLY
import net.fortuna.ical4j.model.Recur.Frequency.YEARLY
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_DATE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_RRULE
import org.tasks.time.DateTime
import java.time.DayOfWeek
import java.util.Locale

class CustomRecurrenceViewModelTest {
    @Test
    fun defaultStateValue() {
        val state = newVM().state.value
        assertEquals(CustomRecurrenceViewModel.ViewState(), state)
    }

    @Test
    fun setFrequencies() {
        assertEquals("FREQ=SECONDLY", newVM { setFrequency(SECONDLY) }.getRecur())
        assertEquals("FREQ=MINUTELY", newVM { setFrequency(MINUTELY) }.getRecur())
        assertEquals("FREQ=HOURLY", newVM { setFrequency(HOURLY) }.getRecur())
        assertEquals("FREQ=DAILY", newVM { setFrequency(DAILY) }.getRecur())
        assertEquals("FREQ=WEEKLY", newVM().getRecur())
        assertEquals("FREQ=MONTHLY", newVM { setFrequency(MONTHLY) }.getRecur())
        assertEquals("FREQ=YEARLY", newVM { setFrequency(YEARLY) }.getRecur())
    }

    @Test
    fun setInterval() {
        assertEquals("FREQ=WEEKLY;INTERVAL=4", newVM { setInterval(4) }.getRecur())
    }

    @Test
    fun ignoreCountWhenChangingToNever() {
        assertEquals(
            "FREQ=WEEKLY",
            newVM("FREQ=WEEKLY;COUNT=2") { setEndType(0) }.getRecur()
        )
    }

    @Test
    fun setEndDate() {
        assertEquals(
            "FREQ=WEEKLY;UNTIL=20230726",
            newVM {
                setEndDate(DateTime(2023, 7, 26).millis)
                setEndType(1)
            }.getRecur()
        )
    }

    @Test
    fun ignoreEndDateWhenChangingToNever() {
        assertEquals(
            "FREQ=WEEKLY",
            newVM("FREQ=WEEKLY;UNTIL=20230726") { setEndType(0) }.getRecur()
        )
    }

    @Test
    fun setDaysInOrder() {
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,TU,WE",
            newVM {
                toggleDay(DayOfWeek.MONDAY)
                toggleDay(DayOfWeek.WEDNESDAY)
                toggleDay(DayOfWeek.TUESDAY)
            }
                .getRecur()
        )
    }

    @Test
    fun ignoreDaysForNonWeekly() {
        assertEquals(
            "FREQ=MONTHLY",
            newVM {
                setFrequency(MONTHLY)
                toggleDay(DayOfWeek.MONDAY)
            }
                .getRecur()
        )
    }

    @Test
    fun setCount() {
        assertEquals(
            "FREQ=WEEKLY;COUNT=3",
            newVM {
                setEndType(2)
                setOccurrences(3)
            }
                .getRecur()
        )
    }

    @Test
    fun toggleDayOff() {
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO",
            newVM("FREQ=WEEKLY;BYDAY=MO,TU") { toggleDay(DayOfWeek.TUESDAY) }.getRecur()
        )
    }

    @Test
    fun nthDayOfMonth() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=4TH",
            newVM(dueDate = DateTime(2023, 7, 27)) {
                setFrequency(MONTHLY)
                setMonthSelection(1)
            }.getRecur()
        )
    }

    @Test
    fun lastDayOfMonth() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=-1TH",
            newVM(dueDate = DateTime(2023, 7, 27)) {
                setFrequency(MONTHLY)
                setMonthSelection(2)
            }.getRecur()
        )
    }

    @Test
    fun restoreMonthDay() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=-1TH",
            newVM(
                recur = "FREQ=MONTHLY;BYDAY=-1TH",
                dueDate = DateTime(2023, 7, 27)
            ).getRecur()
        )
    }

    @Test
    fun changeMonthDay() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=4TH",
            newVM(
                recur = "FREQ=MONTHLY;BYDAY=-1TH",
                dueDate = DateTime(2023, 7, 27)
            ) {
                setMonthSelection(1)
            }.getRecur()
        )
    }

    private fun newVM(
        recur: String? = null,
        dueDate: DateTime = DateTime(0),
        block: CustomRecurrenceViewModel.() -> Unit = {}
    ) =
        CustomRecurrenceViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    EXTRA_RRULE to recur,
                    EXTRA_DATE to dueDate.millis,
                )
            ),
            locale = Locale.getDefault()
        ).also(block)
}
