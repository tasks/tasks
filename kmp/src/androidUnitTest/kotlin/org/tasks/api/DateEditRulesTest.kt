package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.api.TasksContract.Tasks
import java.time.LocalDate
import java.time.ZoneId

class DateEditRulesTest : ApiTestCase() {

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

    private fun dueOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

    private fun recurrenceOf(id: Long): String = query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE)

    private fun LocalDate.noon(): Long =
        atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
