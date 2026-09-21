package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.entity.Alarm
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

    @Test
    fun clearingADateDropsTheRemindersRelativeToIt() {
        val id = newTask("Dentist", Tasks.DUE_DATE to day(1))
        update(Tasks.PATH, id, Tasks.START_DATE to dueOf(id) - ONE_DAY)
        reminder(id, Reminders.TYPE_RELATIVE_DUE, Reminders.OFFSET_MS to -60_000L)
        reminder(id, Reminders.TYPE_RELATIVE_START, Reminders.OFFSET_MS to 0L)
        reminder(id, Reminders.TYPE_DATE_TIME, Reminders.TRIGGER_AT to day(1))
        assertEquals(3, query(Reminders.PATH, "?task_id=$id").rows())

        update(Tasks.PATH, id, Tasks.START_DATE to 0L)
        assertEquals(
            listOf(Reminders.TYPE_RELATIVE_DUE, Reminders.TYPE_DATE_TIME).sorted(),
            reminderTypes(id),
        )

        update(Tasks.PATH, id, Tasks.DUE_DATE to 0L)
        assertEquals(listOf(Reminders.TYPE_DATE_TIME), reminderTypes(id))
    }

    @Test
    fun aDateAddedThroughTheApiEarnsNoDefaultReminders() {
        appPreferences.stub {
            onBlocking { defaultAlarms() } doReturn listOf(Alarm.whenDue(0), Alarm.whenStarted(0))
            onBlocking { isDefaultDueTimeEnabled() } doReturn true
        }

        val created = newTask("Trip", Tasks.DUE_DATE to day(5), Tasks.START_DATE to day(2))
        val dated = newTask("Dentist").also { update(Tasks.PATH, it, Tasks.DUE_DATE to day(1)) }
        val timed = newTask("Anniversary", Tasks.DUE_DATE to day(1), Tasks.DUE_ALL_DAY to 1)
            .also { update(Tasks.PATH, it, Tasks.DUE_ALL_DAY to 0) }

        listOf(created, dated, timed).forEach {
            assertEquals(0, query(Reminders.PATH, "?task_id=$it").rows())
        }
    }

    private fun dueOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

    private fun startOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.START_DATE)

    private fun recurrenceOf(id: Long): String = query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE)

    private fun reminderTypes(id: Long): List<String> =
        query(Reminders.PATH, "?task_id=$id").strings(Reminders.TYPE).sorted()

    private fun reminder(task: Long, type: String, time: Pair<String, Long>): Long =
        insert(Reminders.PATH, Reminders.TASK_ID to task, Reminders.TYPE to type, time)

    private fun LocalDate.noon(): Long =
        atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
