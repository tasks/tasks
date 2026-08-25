package org.tasks.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.GoogleTask
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import java.time.Instant
import java.time.ZoneId

class ParsedTaskMapperTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun caldavList(
        name: String,
        uuid: String = "uuid-$name",
        access: Int = CaldavCalendar.ACCESS_OWNER,
        accountType: Int = CaldavAccount.TYPE_CALDAV,
    ) = CaldavFilter(
        calendar = CaldavCalendar(name = name, uuid = uuid, access = access),
        account = CaldavAccount(accountType = accountType),
    )

    private fun task() = Task(title = "placeholder", priority = Task.Priority.NONE)

    private fun parsed(
        title: String = "Call the dentist",
        notes: String? = null,
        list: String? = null,
        tags: List<String> = emptyList(),
        due: String? = null,
        start: String? = null,
        priority: String? = null,
        recurrence: String? = null,
    ) = ParsedTask(title, notes, list, tags, due, start, priority, recurrence)

    private fun Long.fields(): Triple<Int, Int, Int> {
        val local = Instant.ofEpochMilli(this).atZone(zone)
        return Triple(local.hour, local.minute, local.second)
    }

    @Test
    fun dateOnlyDueIsNoonWithZeroSeconds() {
        val task = task()
        parsed(due = "2026-09-01").applyTo(task, emptyList(), emptyList())

        val (hour, _, second) = task.dueDate.fields()
        assertEquals(12, hour)
        assertEquals(0, second) // seconds == 0 means "no due time"
        assertFalse(Task.hasDueTime(task.dueDate))
    }

    @Test
    fun timedDueKeepsTheStatedHourAndMarksHasTime() {
        val task = task()
        parsed(due = "2026-09-01T14:00").applyTo(task, emptyList(), emptyList())

        val (hour, minute, second) = task.dueDate.fields()
        assertEquals(14, hour)
        assertEquals(0, minute)
        assertEquals(1, second) // seconds > 0 means "has due time"
        assertTrue(Task.hasDueTime(task.dueDate))
    }

    @Test
    fun unparseableDueIsLeftAlone() {
        val task = task()
        val before = task.dueDate
        parsed(due = "next tuesday-ish").applyTo(task, emptyList(), emptyList())

        assertEquals(before, task.dueDate)
    }

    @Test
    fun startDateSetsHideUntil() {
        val task = task()
        parsed(start = "2026-09-01").applyTo(task, emptyList(), emptyList())

        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun prioritiesMapToTheirConstants() {
        for ((text, expected) in listOf(
            "high" to Task.Priority.HIGH,
            "medium" to Task.Priority.MEDIUM,
            "low" to Task.Priority.LOW,
        )) {
            val task = task()
            parsed(priority = text).applyTo(task, emptyList(), emptyList())
            assertEquals(text, expected, task.priority)
        }
    }

    @Test
    fun priorityIsCaseInsensitive() {
        val task = task()
        parsed(priority = "HIGH").applyTo(task, emptyList(), emptyList())

        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun unknownOrNonePriorityKeepsTheConfiguredDefault() {
        for (text in listOf("none", "urgent-ish", null)) {
            val task = Task(title = "x", priority = Task.Priority.MEDIUM)
            parsed(priority = text).applyTo(task, emptyList(), emptyList())
            assertEquals("priority=$text", Task.Priority.MEDIUM, task.priority)
        }
    }

    @Test
    fun caldavListSetsCaldavTransitory() {
        val task = task()
        val lists = listOf(caldavList("Personal", uuid = "cal-1"))
        parsed(list = "Personal").applyTo(task, lists, emptyList())

        assertEquals("cal-1", task.getTransitory<String>(CaldavTask.KEY))
        assertFalse(task.hasTransitory(GoogleTask.KEY))
    }

    @Test
    fun googleTasksListSetsGoogleTransitory() {
        val task = task()
        val lists = listOf(
            caldavList("Personal", uuid = "gt-1", accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        )
        parsed(list = "Personal").applyTo(task, lists, emptyList())

        assertEquals("gt-1", task.getTransitory<String>(GoogleTask.KEY))
        assertFalse(task.hasTransitory(CaldavTask.KEY))
    }

    @Test
    fun listMatchIsCaseInsensitive() {
        val task = task()
        val lists = listOf(caldavList("Personal", uuid = "cal-1"))
        parsed(list = "personal").applyTo(task, lists, emptyList())

        assertEquals("cal-1", task.getTransitory<String>(CaldavTask.KEY))
    }

    @Test
    fun unknownListNameSetsNoTransitory() {
        val task = task()
        val lists = listOf(caldavList("Personal", uuid = "cal-1"))
        parsed(list = "Groceries").applyTo(task, lists, emptyList())

        assertFalse(task.hasTransitory(CaldavTask.KEY))
        assertFalse(task.hasTransitory(GoogleTask.KEY))
    }

    @Test
    fun readOnlyListIsNeverSelected() {
        val task = task()
        val lists = listOf(
            caldavList("Shared", uuid = "ro-1", access = CaldavCalendar.ACCESS_READ_ONLY)
        )
        parsed(list = "Shared").applyTo(task, lists, emptyList())

        assertFalse(task.hasTransitory(CaldavTask.KEY))
        assertFalse(task.hasTransitory(GoogleTask.KEY))
    }

    @Test
    fun knownTagsAreResolvedAndUnknownOnesDropped() {
        val task = task()
        val known = listOf(TagData(name = "errand"), TagData(name = "urgent"))
        parsed(tags = listOf("errand", "invented", "URGENT"))
            .applyTo(task, emptyList(), known)

        assertEquals(listOf("errand", "urgent"), task.getTransitory<ArrayList<String>>(Tag.KEY))
    }

    @Test
    fun noResolvedTagsLeavesExistingTransitoryUntouched() {
        val task = task()
        task.putTransitory(Tag.KEY, arrayListOf("preexisting"))
        parsed(tags = listOf("invented")).applyTo(task, emptyList(), emptyList())

        assertEquals(listOf("preexisting"), task.getTransitory<ArrayList<String>>(Tag.KEY))
    }

    @Test
    fun validRecurrenceIsStoredWithRepeatFromDueDate() {
        val task = task()
        parsed(recurrence = "FREQ=WEEKLY;BYDAY=MO").applyTo(task, emptyList(), emptyList())

        assertEquals("FREQ=WEEKLY;BYDAY=MO", task.recurrence)
        assertEquals(Task.RepeatFrom.DUE_DATE, task.repeatFrom)
    }

    @Test
    fun invalidRecurrenceIsDroppedWithoutThrowing() {
        val task = task()
        parsed(recurrence = "every other blue moon")
            .applyTo(task, emptyList(), emptyList())

        assertNull(task.recurrence)
    }

    @Test
    fun notesAreSetWhenPresentAndSkippedWhenBlank() {
        val withNotes = task()
        parsed(notes = "  bring insurance card ")
            .applyTo(withNotes, emptyList(), emptyList())
        assertEquals("bring insurance card", withNotes.notes)

        val blank = task()
        parsed(notes = "   ").applyTo(blank, emptyList(), emptyList())
        assertNull(blank.notes)
    }

    @Test
    fun titleIsTrimmedAndApplied() {
        val task = task()
        parsed(title = "  Call the dentist  ").applyTo(task, emptyList(), emptyList())

        assertEquals("Call the dentist", task.title)
    }

    @Test
    fun everythingNullLeavesTheTaskUnchanged() {
        val task = Task(title = "original", priority = Task.Priority.LOW)
        val before = task.copy()
        parsed(title = "original").applyTo(task, emptyList(), emptyList())

        assertEquals(before.title, task.title)
        assertEquals(before.priority, task.priority)
        assertEquals(before.dueDate, task.dueDate)
        assertEquals(before.recurrence, task.recurrence)
    }
}
