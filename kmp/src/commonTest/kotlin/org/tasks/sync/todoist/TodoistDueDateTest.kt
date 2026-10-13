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

class TodoistDueDateTest {    @Test
    fun parsesAllDayDueDate() {
        val (millis, hasTime) = TodoistDue(date = "2026-10-09").toDueDate()!!
        assertFalse(hasTime)
        val date = DateTime(millis)
        assertEquals(2026, date.year)
        assertEquals(10, date.monthOfYear)
        assertEquals(9, date.dayOfMonth)
    }    @Test
    fun parsesFloatingDateTime() {
        val (millis, hasTime) = TodoistDue(date = "2026-10-09T15:30:00").toDueDate()!!
        assertTrue(hasTime)
        val date = DateTime(millis)
        assertEquals(2026, date.year)
        assertEquals(10, date.monthOfYear)
        assertEquals(9, date.dayOfMonth)
        assertEquals(15, date.hourOfDay)
        assertEquals(30, date.minuteOfHour)
    }    @Test
    fun parsesFixedZoneDateTime() {
        val (millis, hasTime) = TodoistDue(
            date = "2026-10-09T15:30:00Z",
            timezone = "UTC",
        ).toDueDate()!!
        assertTrue(hasTime)
        assertEquals(1791559800000L, millis)
    }    @Test
    fun parsesOffsetDateTime() {
        val (_, hasTime) = TodoistDue(date = "2026-10-09T15:30:00+02:00").toDueDate()!!
        assertTrue(hasTime)
    }    @Test
    fun returnsNullForUnparseableDate() {
        listOf("", "   ", "not a date", "2026-13-45").forEach {
            assertNull(TodoistDue(date = it).toDueDate(), "expected null for '$it'")
        }
    }    @Test
    fun clearsDueDateExplicitlyRatherThanOmittingIt() {
        val args = Task().apply { title = "x" }.toArgs(tags = emptyList())
        assertEquals(JsonNull, args["due"])
    }    @Test
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
    }    @Test
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
}


private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
