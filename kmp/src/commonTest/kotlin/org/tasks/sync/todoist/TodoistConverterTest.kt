package org.tasks.sync.todoist

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.sync.todoist.TodoistConverter.applyRemote
import org.tasks.sync.todoist.TodoistConverter.toArgs
import org.tasks.sync.todoist.TodoistConverter.toDueDate
import org.tasks.sync.todoist.TodoistConverter.toTaskPriority
import org.tasks.sync.todoist.TodoistConverter.toTodoistPriority
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoistConverterTest {
    @Test
    fun priorityMappingIsABijection() {
        // Todoist 4 = urgent maps to Tasks 0 = high; the scales are inverted.
        assertEquals(Task.Priority.HIGH, 4.toTaskPriority())
        assertEquals(Task.Priority.MEDIUM, 3.toTaskPriority())
        assertEquals(Task.Priority.LOW, 2.toTaskPriority())
        assertEquals(Task.Priority.NONE, 1.toTaskPriority())

        assertEquals(4, Task.Priority.HIGH.toTodoistPriority())
        assertEquals(3, Task.Priority.MEDIUM.toTodoistPriority())
        assertEquals(2, Task.Priority.LOW.toTodoistPriority())
        assertEquals(1, Task.Priority.NONE.toTodoistPriority())
    }

    @Test
    fun priorityMappingClampsOutOfRangeInput() {
        assertEquals(Task.Priority.HIGH, 9.toTaskPriority())
        assertEquals(Task.Priority.NONE, 0.toTaskPriority())
        assertEquals(Task.Priority.NONE, (-5).toTaskPriority())
    }

    @Test
    fun parsesAllDayDueDate() {
        val (millis, hasTime) = TodoistDue(date = "2026-10-09").toDueDate()!!
        assertFalse(hasTime)
        val date = DateTime(millis)
        assertEquals(2026, date.year)
        assertEquals(10, date.monthOfYear)
        assertEquals(9, date.dayOfMonth)
    }

    @Test
    fun parsesFloatingDateTime() {
        val (millis, hasTime) = TodoistDue(date = "2026-10-09T15:30:00").toDueDate()!!
        assertTrue(hasTime)
        val date = DateTime(millis)
        assertEquals(2026, date.year)
        assertEquals(10, date.monthOfYear)
        assertEquals(9, date.dayOfMonth)
        assertEquals(15, date.hourOfDay)
        assertEquals(30, date.minuteOfHour)
    }

    @Test
    fun parsesFixedZoneDateTime() {
        val (millis, hasTime) = TodoistDue(
            date = "2026-10-09T15:30:00Z",
            timezone = "UTC",
        ).toDueDate()!!
        assertTrue(hasTime)
        // 2026-10-09T15:30:00Z
        assertEquals(1791559800000L, millis)
    }

    @Test
    fun parsesOffsetDateTime() {
        val (_, hasTime) = TodoistDue(date = "2026-10-09T15:30:00+02:00").toDueDate()!!
        assertTrue(hasTime)
    }

    @Test
    fun returnsNullForUnparseableDate() {
        listOf("", "   ", "not a date", "2026-13-45").forEach {
            assertNull(TodoistDue(date = it).toDueDate(), "expected null for '$it'")
        }
    }

    @Test
    fun appliesRemoteFields() {
        val task = Task()
        task.applyRemote(
            TodoistItem(
                id = "1",
                content = "Buy milk",
                description = "2%",
                priority = 4,
                labels = listOf("errands"),
                due = TodoistDue(date = "2026-10-09"),
            )
        )
        assertEquals("Buy milk", task.title)
        assertEquals("2%", task.notes)
        assertEquals(Task.Priority.HIGH, task.priority)
        assertTrue(task.hasDueDate())
        assertFalse(task.isCompleted)
        assertNull(task.recurrence)
    }

    @Test
    fun appliesCompletion() {
        val task = Task()
        task.applyRemote(
            TodoistItem(
                id = "1",
                content = "Done thing",
                checked = true,
                completedAt = "2026-10-01T12:00:00Z",
            )
        )
        assertTrue(task.isCompleted)
        assertEquals(1790856000000L, task.completionDate)
    }

    @Test
    fun clearsDueDateWhenRemoteHasNone() {
        val task = Task().apply {
            dueDate = DateTime(2026, 1, 1).millis
        }
        task.applyRemote(TodoistItem(id = "1", content = "x"))
        assertFalse(task.hasDueDate())
    }

    @Test
    fun appliesRecognisedRecurrence() {
        val task = Task()
        task.applyRemote(
            TodoistItem(
                id = "1",
                content = "Standup",
                due = TodoistDue(
                    date = "2026-10-09",
                    string = "every weekday",
                    isRecurring = true,
                ),
            )
        )
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", task.recurrence)
        assertEquals(Task.RepeatFrom.DUE_DATE, task.repeatFrom)
    }

    @Test
    fun keepsExistingRecurrenceWhenRemoteRuleIsUnparseable() {
        // A rule we can't read must not silently wipe the local schedule.
        val task = Task().apply { recurrence = "FREQ=DAILY" }
        task.applyRemote(
            TodoistItem(
                id = "1",
                content = "Mystery",
                due = TodoistDue(
                    date = "2026-10-09",
                    string = "every 3rd friday of the month",
                    isRecurring = true,
                ),
            )
        )
        assertEquals("FREQ=DAILY", task.recurrence)
    }

    @Test
    fun buildsCreateArgs() {
        val task = Task().apply {
            title = "Write tests"
            notes = "thoroughly"
            priority = Task.Priority.MEDIUM
        }
        val args = task.toArgs(
            tags = listOf(TagData(name = "work"), TagData(name = "urgent")),
            projectId = "proj1",
            sectionId = "sec1",
            parentId = "parent1",
        )
        assertEquals("Write tests", args.string("content"))
        assertEquals("thoroughly", args.string("description"))
        assertEquals("proj1", args.string("project_id"))
        assertEquals("sec1", args.string("section_id"))
        assertEquals("parent1", args.string("parent_id"))
        assertEquals("3", args["priority"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("work", "urgent"),
            args["labels"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun omitsIdentifiersNotSupplied() {
        val args = Task().apply { title = "x" }.toArgs(tags = emptyList())
        assertFalse(args.containsKey("id"))
        assertFalse(args.containsKey("project_id"))
        assertFalse(args.containsKey("section_id"))
        assertFalse(args.containsKey("parent_id"))
    }

    @Test
    fun clearsDueDateExplicitlyRatherThanOmittingIt() {
        // Omitting `due` would leave the remote date in place, so it has to be null.
        val args = Task().apply { title = "x" }.toArgs(tags = emptyList())
        assertEquals(JsonNull, args["due"])
    }

    @Test
    fun sendsAllDayDueDateWithoutTime() {
        val task = Task().apply {
            title = "x"
            dueDate = org.tasks.data.createDueDate(
                Task.URGENCY_SPECIFIC_DAY,
                DateTime(2026, 10, 9).millis,
            )
        }
        val due = task.toArgs(tags = emptyList())["due"]!!.jsonObject
        assertEquals("2026-10-09", due.string("date"))
        assertFalse(due.containsKey("string"))
    }

    @Test
    fun sendsDueDateWithTime() {
        val task = Task().apply {
            title = "x"
            dueDate = org.tasks.data.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                DateTime(2026, 10, 9, 15, 30).millis,
            )
        }
        val due = task.toArgs(tags = emptyList())["due"]!!.jsonObject
        assertEquals("2026-10-09T15:30:00", due.string("date"))
    }

    @Test
    fun sendsRecurrenceAsDueString() {
        val task = Task().apply {
            title = "x"
            dueDate = org.tasks.data.createDueDate(
                Task.URGENCY_SPECIFIC_DAY,
                DateTime(2026, 10, 9).millis,
            )
            recurrence = "FREQ=WEEKLY;BYDAY=MO"
        }
        val due = task.toArgs(tags = emptyList())["due"]!!.jsonObject
        assertEquals("every mon", due.string("string"))
    }

    @Test
    fun replaysPreviousDueStringWhenRuleIsNotExpressible() {
        // BYSETPOS has no Todoist equivalent, so the user's original text is reused
        // rather than dropping the repeat entirely.
        val task = Task().apply {
            title = "x"
            dueDate = org.tasks.data.createDueDate(
                Task.URGENCY_SPECIFIC_DAY,
                DateTime(2026, 10, 9).millis,
            )
            recurrence = "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"
        }
        val due = task
            .toArgs(tags = emptyList(), previousDueString = "every last weekday")["due"]!!
            .jsonObject
        assertEquals("every last weekday", due.string("string"))
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
}
