package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.api.TasksContract.Tasks
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDay
import java.time.LocalDate
import java.time.ZoneId

class DateEditRulesTest : ApiTestCase() {

    @Test
    fun aStartOnTheDueDateMovesWithIt() {
        val id = newTask("Trash", Tasks.DUE_DATE to day(1))
        val due = dueOf(id)
        update(Tasks.PATH, id, Tasks.START_DATE to due)
        assertEquals(due, startOf(id))

        update(Tasks.PATH, id, Tasks.DUE_DATE to due + ONE_DAY)

        assertEquals(dueOf(id), startOf(id))
        assertEquals(due + ONE_DAY, dueOf(id))
    }

    @Test
    fun aStartTheDayBeforeDueStaysTheDayBefore() {
        val id = newTask("Pack", Tasks.DUE_DATE to day(3), Tasks.DUE_ALL_DAY to 1)
        val due = dueOf(id)
        update(Tasks.PATH, id, Tasks.START_DATE to due - ONE_DAY, Tasks.START_ALL_DAY to 1)
        assertEquals((due - ONE_DAY).startOfDay(), startOf(id))

        update(Tasks.PATH, id, Tasks.DUE_DATE to due + 3 * ONE_DAY)

        assertEquals((dueOf(id) - ONE_DAY).startOfDay(), startOf(id))
    }

    @Test
    fun anAbsoluteStartStaysWhereItIs() {
        val id = newTask("Taxes", Tasks.DUE_DATE to day(10), Tasks.DUE_ALL_DAY to 1)
        val start = (dueOf(id) - 3 * ONE_DAY).startOfDay()
        update(Tasks.PATH, id, Tasks.START_DATE to start, Tasks.START_ALL_DAY to 1)

        update(Tasks.PATH, id, Tasks.DUE_DATE to dueOf(id) + 2 * ONE_DAY)

        assertEquals(start, startOf(id))
    }

    @Test
    fun aStartSentWithTheDueDateIsTheOneKept() {
        val id = newTask("Trash", Tasks.DUE_DATE to day(1))
        val due = dueOf(id)
        update(Tasks.PATH, id, Tasks.START_DATE to due)
        val chosen = (due - 5 * ONE_DAY).startOfDay()

        update(
            Tasks.PATH, id,
            Tasks.DUE_DATE to due + ONE_DAY,
            Tasks.START_DATE to chosen,
            Tasks.START_ALL_DAY to 1,
        )

        assertEquals(chosen, startOf(id))
    }

    @Test
    fun clearingTheDueDateClearsARelativeStartAndKeepsAnAbsoluteOne() {
        val relative = newTask("Trash", Tasks.DUE_DATE to day(1))
        update(Tasks.PATH, relative, Tasks.START_DATE to dueOf(relative))
        val absolute = newTask("Taxes", Tasks.DUE_DATE to day(10), Tasks.DUE_ALL_DAY to 1)
        val start = (dueOf(absolute) - 3 * ONE_DAY).startOfDay()
        update(Tasks.PATH, absolute, Tasks.START_DATE to start, Tasks.START_ALL_DAY to 1)

        update(Tasks.PATH, relative, Tasks.DUE_DATE to 0L)
        update(Tasks.PATH, absolute, Tasks.DUE_DATE to 0L)

        assertEquals(0L, startOf(relative))
        assertEquals(start, startOf(absolute))
    }

    @Test
    fun aMonthlyWeekdayRuleFollowsTheDueDate() {
        val secondTuesday = LocalDate.of(2026, 10, 13)
        val thirdWednesday = LocalDate.of(2026, 11, 18)
        val lastFriday = LocalDate.of(2026, 10, 30)
        val id = newTask(
            "Book club",
            Tasks.DUE_DATE to secondTuesday.noon(),
            Tasks.DUE_ALL_DAY to 1,
            Tasks.RECURRENCE to "FREQ=MONTHLY;BYDAY=2TU",
        )

        update(Tasks.PATH, id, Tasks.DUE_DATE to thirdWednesday.noon())
        assertEquals("FREQ=MONTHLY;BYDAY=3WE", recurrenceOf(id))

        update(Tasks.PATH, id, Tasks.DUE_DATE to lastFriday.noon())
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", recurrenceOf(id))
    }

    @Test
    fun aMonthlyWeekdayRuleSentWithADueDateIsAnchoredToIt() {
        val thirdWednesday = LocalDate.of(2026, 11, 18)

        val id = newTask(
            "Book club",
            Tasks.DUE_DATE to thirdWednesday.noon(),
            Tasks.DUE_ALL_DAY to 1,
            Tasks.RECURRENCE to "FREQ=MONTHLY;BYDAY=2TU",
        )

        assertEquals("FREQ=MONTHLY;BYDAY=3WE", recurrenceOf(id))
    }

    @Test
    fun otherRulesAreLeftAloneWhenTheDueDateMoves() {
        val id = newTask("Trash", Tasks.DUE_DATE to day(1), Tasks.RECURRENCE to "FREQ=WEEKLY;BYDAY=TH")

        update(Tasks.PATH, id, Tasks.DUE_DATE to day(2))

        assertEquals("FREQ=WEEKLY;BYDAY=TH", recurrenceOf(id))
    }

    @Test
    fun aRuleOnADatelessTaskDatesItToday() {
        val id = newTask("Water plants", Tasks.RECURRENCE to "FREQ=DAILY")

        assertEquals(currentTimeMillis().startOfDay(), dueOf(id).startOfDay())
        assertEquals(1, query(Tasks.PATH, "?_id=$id").int(Tasks.DUE_ALL_DAY))
    }

    private fun dueOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

    private fun startOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.START_DATE)

    private fun recurrenceOf(id: Long): String = query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE)

    private fun LocalDate.noon(): Long =
        atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
