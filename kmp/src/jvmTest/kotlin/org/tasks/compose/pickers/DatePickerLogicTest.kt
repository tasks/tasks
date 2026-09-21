package org.tasks.compose.pickers

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.hourOfDay
import org.tasks.time.minuteOfHour
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay

class DatePickerLogicTest {

    private val base = 1_800_000_000_000L.startOfDay()
    private val noon = base.withMillisOfDay(12 * HOUR)
    private val nineAm = 9 * HOUR + TIME_MARKER
    private val fivePm = 17 * HOUR + TIME_MARKER

    @Before
    fun setUp() {
        DateTimeUtils2.setCurrentMillisFixed(noon)
    }

    @After
    fun tearDown() {
        DateTimeUtils2.setCurrentMillisSystem()
    }

    // region dueDateFromSelection / dueDateToSelection

    @Test
    fun dueDateFromSelectionNoDayIsZero() {
        assertEquals(0L, dueDateFromSelection(NO_DAY, NO_TIME))
        assertEquals(0L, dueDateFromSelection(NO_DAY, nineAm))
    }

    @Test
    fun dueDateFromSelectionDateOnly() {
        assertEquals(base, dueDateFromSelection(base, NO_TIME))
    }

    @Test
    fun dueDateFromSelectionTimed() {
        assertEquals(base.withMillisOfDay(nineAm), dueDateFromSelection(base, nineAm))
    }

    @Test
    fun dueDateFromSelectionMultipleTimesDoesNotThrow() {
        assertEquals(base, dueDateFromSelection(base, MULTIPLE_TIMES))
    }

    @Test
    fun dueDateToSelectionNoDate() {
        assertEquals(NO_DAY to NO_TIME, dueDateToSelection(0))
    }

    @Test
    fun dueDateTimedRoundTrips() {
        val timed = base.withMillisOfDay(nineAm)
        val (day, time) = dueDateToSelection(timed)
        assertEquals(base, day)
        assertEquals(timed, dueDateFromSelection(day, time))
    }

    @Test
    fun dueDateFoldRoundTripsANoonValueInIsolation() {
        val (day, time) = dueDateToSelection(noon)
        assertEquals(base, day)
        assertEquals(noon, dueDateFromSelection(day, time))
    }

    @Test
    fun dueDateFromSelectionTimedComposesDayAndTime() {
        val result = dueDateFromSelection(base, nineAm)
        assertEquals(base, result.startOfDay())
        assertEquals(9, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertTrue(Task.hasDueTime(result))
    }

    @Test
    fun initialDueTimeKeepsRealTimeStripsDateOnly() {
        assertEquals(nineAm, initialDueTime(nineAm))
        assertEquals(MULTIPLE_TIMES, initialDueTime(MULTIPLE_TIMES))
        assertEquals(NO_TIME, initialDueTime(12 * HOUR))
        assertEquals(NO_TIME, initialDueTime(NO_TIME))
    }

    // endregion

    // region alarmToSelection / alarmFromSelection

    @Test
    fun alarmTimeSurvivesTheDueDateSheet() {
        val nineAmExactly = base.withMillisOfDay(9 * HOUR)
        val (day, time) = alarmToSelection(nineAmExactly)

        assertEquals(base, day)
        assertEquals(time, initialDueTime(time))
        assertEquals(nineAmExactly, alarmFromSelection(day, time))
    }

    @Test
    fun alarmNoonSurvivesTheDueDateSheet() {
        val (day, time) = alarmToSelection(noon)

        assertEquals(time, initialDueTime(time))
        assertEquals(noon, alarmFromSelection(day, time))
    }

    @Test
    fun alarmFromSelectionStripsTheTimeMarker() {
        assertEquals(base.withMillisOfDay(9 * HOUR), alarmFromSelection(base, nineAm))
        assertEquals(base.withMillisOfDay(17 * HOUR), alarmFromSelection(base, fivePm))
    }

    @Test
    fun alarmFromSelectionNoDayIsZero() {
        assertEquals(0L, alarmFromSelection(NO_DAY, nineAm))
        assertEquals(0L, alarmFromSelection(NO_DAY, NO_TIME))
    }

    @Test
    fun alarmToSelectionNoDate() {
        assertEquals(NO_DAY to NO_TIME, alarmToSelection(0))
    }

    // endregion

    // region dayForNewTime (today-vs-tomorrow rollover, clock frozen at noon)

    @Test
    fun dayForNewTimeStaysTodayWhenTimeIsStillAhead() {
        assertEquals(base, dayForNewTime(base, fivePm))
    }

    @Test
    fun dayForNewTimeRollsToTomorrowWhenTimeHasPassed() {
        assertEquals(base.plusDays(1), dayForNewTime(base, nineAm))
    }

    @Test
    fun dayForAppliedTimeDerivesDayOnlyWhenNoneAndReal() {
        assertEquals(base, dayForAppliedTime(NO_DAY, base, fivePm))
        assertEquals(base.plusDays(1), dayForAppliedTime(NO_DAY, base, nineAm))
        assertEquals(NO_DAY, dayForAppliedTime(NO_DAY, base, NO_TIME))
        assertEquals(NO_DAY, dayForAppliedTime(NO_DAY, base, MULTIPLE_TIMES))
        assertEquals(base, dayForAppliedTime(base, base, nineAm))
        assertEquals(base, dayForAppliedTime(base, base, MULTIPLE_TIMES))
    }

    // endregion

    // region default quick-pick times

    @Test
    fun defaultQuickPickTimesMatchLegacyHoursAndCarryMarker() {
        assertEquals(9 * HOUR, DEFAULT_MORNING)
        assertEquals(13 * HOUR, DEFAULT_AFTERNOON)
        assertEquals(17 * HOUR, DEFAULT_EVENING)
        assertEquals(20 * HOUR, DEFAULT_NIGHT)
        assertFalse(Task.hasDueTime(DEFAULT_MORNING.toLong()))
        assertTrue(Task.hasDueTime((DEFAULT_MORNING + TIME_MARKER).toLong()))
    }

    // endregion

    // region showKeyboardDateInput

    @Test
    fun autoCloseForcesCalendarModeEvenWhenKeyboardInputWasPersisted() {
        assertFalse(showKeyboardDateInput(initialDateInputMode = true, autoClose = true))
        assertTrue(showKeyboardDateInput(initialDateInputMode = true, autoClose = false))
        assertFalse(showKeyboardDateInput(initialDateInputMode = false, autoClose = true))
        assertFalse(showKeyboardDateInput(initialDateInputMode = false, autoClose = false))
    }

    // endregion

    // region demoteDueTime

    @Test
    fun demoteDueTimeDowngradesDueTimeToDueDate() {
        assertEquals(DUE_DATE, demoteDueTime(DUE_TIME))
    }

    @Test
    fun demoteDueTimeLeavesOtherDaysUnchanged() {
        assertEquals(DUE_DATE, demoteDueTime(DUE_DATE))
        assertEquals(DAY_BEFORE_DUE, demoteDueTime(DAY_BEFORE_DUE))
        assertEquals(WEEK_BEFORE_DUE, demoteDueTime(WEEK_BEFORE_DUE))
        assertEquals(base, demoteDueTime(base))
        assertEquals(NO_DAY, demoteDueTime(NO_DAY))
    }

    // endregion

    // region defaultHideUntilDay

    @Test
    fun defaultHideUntilDayMapsSettingsToSentinels() {
        assertEquals(DUE_DATE, defaultHideUntilDay(Task.HIDE_UNTIL_DUE))
        assertEquals(DUE_TIME, defaultHideUntilDay(Task.HIDE_UNTIL_DUE_TIME))
        assertEquals(DAY_BEFORE_DUE, defaultHideUntilDay(Task.HIDE_UNTIL_DAY_BEFORE))
        assertEquals(WEEK_BEFORE_DUE, defaultHideUntilDay(Task.HIDE_UNTIL_WEEK_BEFORE))
        assertEquals(NO_DAY, defaultHideUntilDay(Task.HIDE_UNTIL_NONE))
    }

    // endregion

    // region DateSheetState commit / autoCommit gating

    private class Recorder {
        var selected: Pair<Long, Int>? = null
        var dismissed = 0
        fun state(initialDay: Long, initialTime: Int, autoClose: Boolean) = DateSheetState(
            initialDay = initialDay,
            initialTime = initialTime,
            autoClose = autoClose,
            onSelected = { day, time -> selected = day to time },
            onDismiss = { dismissed++ },
        )
    }

    @Test
    fun commitWithoutChangeDismissesInsteadOfSelecting() {
        val r = Recorder()
        val state = r.state(initialDay = base, initialTime = NO_TIME, autoClose = false)
        state.commit()
        assertEquals(null, r.selected)
        assertEquals(1, r.dismissed)
    }

    @Test
    fun commitAfterChangeSelects() {
        val r = Recorder()
        val state = r.state(initialDay = base, initialTime = NO_TIME, autoClose = false)
        state.selectedDay = base.plusDays(1)
        state.commit()
        assertEquals(base.plusDays(1) to NO_TIME, r.selected)
        assertEquals(0, r.dismissed)
    }

    @Test
    fun withoutAutoCloseSettersDoNotCommit() {
        val r = Recorder()
        val state = r.state(initialDay = NO_DAY, initialTime = NO_TIME, autoClose = false)
        state.setDay(base)
        state.setDayTime(base, nineAm)
        state.clearDate()
        assertEquals(null, r.selected)
        assertEquals(0, r.dismissed)
    }

    @Test
    fun autoCloseSetDayCommitsImmediately() {
        val r = Recorder()
        val state = r.state(initialDay = NO_DAY, initialTime = NO_TIME, autoClose = true)
        state.setDay(base)
        assertEquals(base to NO_TIME, r.selected)
    }

    @Test
    fun autoCloseClearDateCommitsCleared() {
        val r = Recorder()
        val state = r.state(initialDay = base, initialTime = nineAm, autoClose = true)
        state.clearDate()
        assertEquals(NO_DAY to NO_TIME, r.selected)
    }

    @Test
    fun autoCloseClearTimeKeepsDayDropsTime() {
        val r = Recorder()
        val state = r.state(initialDay = base, initialTime = nineAm, autoClose = true)
        state.selectedTime = NO_TIME
        state.autoCommit()
        assertEquals(base to NO_TIME, r.selected)
    }

    // endregion

    companion object {
        private const val HOUR = 60 * 60 * 1000
    }
}
