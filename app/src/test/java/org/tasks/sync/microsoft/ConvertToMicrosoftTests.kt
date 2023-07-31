package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
import org.tasks.data.CaldavTask
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TagDataMaker.NAME
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DESCRIPTION
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.PRIORITY
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.TITLE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import org.tasks.sync.microsoft.Tasks.Task.Importance
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceDayOfWeek
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceType
import org.tasks.time.DateTime

class ConvertToMicrosoftTests {
    @Test
    fun noIdForNewTask() {
        val remote = newTask().toRemote(newCaldavTask(with(REMOTE_ID, null as String?)))
        assertNull(remote.id)
    }

    @Test
    fun setTitle() {
        val remote = newTask(with(TITLE, "title")).toRemote(newCaldavTask())
        assertEquals("title", remote.title)
    }

    @Test
    fun noBody() {
        val remote = newTask(with(DESCRIPTION, null as String?)).toRemote()
        assertNull(remote.body)
    }

    @Test
    fun setBody() {
        val remote = newTask(with(DESCRIPTION, "Description")).toRemote()
        assertEquals("Description", remote.body?.content)
        assertEquals("text", remote.body?.contentType)
    }

    @Test
    fun setHighPriority() {
        val remote = newTask(with(PRIORITY, Task.Priority.HIGH)).toRemote()
        assertEquals(Importance.high, remote.importance)
    }

    @Test
    fun setNormalPriority() {
        val remote = newTask(with(PRIORITY, Task.Priority.MEDIUM)).toRemote()
        assertEquals(Importance.normal, remote.importance)
    }

    @Test
    fun setLowPriority() {
        val remote = newTask(with(PRIORITY, Task.Priority.LOW)).toRemote()
        assertEquals(Importance.low, remote.importance)
    }

    @Test
    fun setNoPriorityToLow() {
        val remote = newTask(with(PRIORITY, Task.Priority.NONE)).toRemote()
        assertEquals(Importance.low, remote.importance)
    }

    @Test
    fun statusForUncompletedTask() {
        val remote = newTask().toRemote()
        assertEquals(Tasks.Task.Status.notStarted, remote.status)
    }

    @Test
    fun statusForCompletedTask() {
        val remote =
            newTask(with(COMPLETION_TIME, DateTime())).toRemote()
        assertEquals(Tasks.Task.Status.completed, remote.status)
    }

    @Test
    fun noCategories() {
        val remote = newTask().toRemote()
        assertNull(remote.categories)
    }

    @Test
    fun setCategories() {
        val remote = newTask().toRemote(
            newCaldavTask(),
            listOf(
                newTagData(with(NAME, "tag1")),
                newTagData(with(NAME, "tag2")),
            )
        )
        assertEquals(listOf("tag1", "tag2"), remote.categories)
    }

    @Test
    fun setCreationTime() {
        withTZ("America/Chicago") {
            val remote = Task(
                creationDate = DateTime(2023, 7, 21, 0, 42, 13, 475).millis,
            ).toRemote()
            assertEquals(
                "2023-07-21T05:42:13.4750000Z",
                remote.createdDateTime
            )
        }
    }

    @Test
    fun setModificationTime() {
        withTZ("America/Chicago") {
            val remote = Task(
                modificationDate = DateTime(2023, 7, 21, 0, 49, 4, 3).millis,
            ).toRemote()
            assertEquals(
                "2023-07-21T05:49:04.0030000Z",
                remote.lastModifiedDateTime
            )
        }
    }

    @Test
    fun setDueDateTime() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 21, 13, 30))
            )
                .toRemote()
            assertEquals("2023-07-21T05:00:00.0000000", remote.dueDateTime?.dateTime)
            assertEquals("UTC", remote.dueDateTime?.timeZone)
        }
    }

    @Test
    fun setDailyRecurrence() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=DAILY")
            )
                .toRemote()
                .recurrence
                ?: throw IllegalStateException()
            assertEquals(RecurrenceType.daily, remote.pattern.type)
            assertEquals(1, remote.pattern.interval)
            assertTrue(remote.pattern.daysOfWeek.isEmpty())
        }
    }

    @Test
    fun setWeeklyRecurrence() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 31, 0, 26, 48)),
                with(RECUR, "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,TU,WE,TH,FR")
            )
                .toRemote()
                .recurrence
                ?: throw IllegalStateException()
            assertEquals(RecurrenceType.weekly, remote.pattern.type)
            assertEquals(2, remote.pattern.interval)
            assertEquals(
                listOf(
                    RecurrenceDayOfWeek.monday,
                    RecurrenceDayOfWeek.tuesday,
                    RecurrenceDayOfWeek.wednesday,
                    RecurrenceDayOfWeek.thursday,
                    RecurrenceDayOfWeek.friday,
                ),
                remote.pattern.daysOfWeek
            )
        }
    }

    @Test
    fun setMonthlyRecurrence() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 31, 0, 26, 48)),
                with(RECUR, "FREQ=MONTHLY")
            )
                .toRemote()
                .recurrence
                ?: throw IllegalStateException()
            assertEquals(RecurrenceType.absoluteMonthly, remote.pattern.type)
            assertEquals(31, remote.pattern.dayOfMonth)
            assertEquals(1, remote.pattern.interval)
        }
    }

    @Test
    fun setAnnualRecurrence() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 23, 9, 7)),
                with(RECUR, "FREQ=YEARLY")
            )
                .toRemote()
                .recurrence
                ?: throw IllegalStateException()
            assertEquals(RecurrenceType.absoluteYearly, remote.pattern.type)
            assertEquals(8, remote.pattern.month)
            assertEquals(2, remote.pattern.dayOfMonth)
            assertEquals(1, remote.pattern.interval)
        }
    }

    @Test
    fun setAnnualRecurrenceWithoutDueDate() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 8, 2, 23, 17, 22).millis) {
                val remote = newTask(
                    with(RECUR, "FREQ=YEARLY")
                )
                    .toRemote()
                    .recurrence
                    ?: throw IllegalStateException()
                assertEquals(8, remote.pattern.month)
                assertEquals(2, remote.pattern.dayOfMonth)
            }
        }
    }

    private fun Task.toRemote(caldavTask: CaldavTask = newCaldavTask()) = toRemote(caldavTask, emptyList())
}
