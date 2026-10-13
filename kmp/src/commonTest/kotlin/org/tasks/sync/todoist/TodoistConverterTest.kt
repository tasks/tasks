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
            target = TodoistConverter.ArgTarget(
                projectId = "proj1",
                sectionId = "sec1",
                parentId = "parent1",
            ),
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
        val task = Task().apply {
            title = "x"
            dueDate = org.tasks.data.createDueDate(
                Task.URGENCY_SPECIFIC_DAY,
                DateTime(2026, 10, 9).millis,
            )
            recurrence = "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1"
        }
        val due = task
            .toArgs(
                tags = emptyList(),
                target = TodoistConverter.ArgTarget(previousDueString = "every last weekday"),
            )["due"]!!
            .jsonObject
        assertEquals("every last weekday", due.string("string"))
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
}
