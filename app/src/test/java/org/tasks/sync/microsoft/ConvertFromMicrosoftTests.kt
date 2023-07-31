package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.data.Task
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities
import org.tasks.TestUtilities.withTZ
import org.tasks.makers.TaskMaker
import org.tasks.time.DateTime

class ConvertFromMicrosoftTests {
    @Test
    fun titleFromRemote() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.txt")
        assertEquals("Basic task", local.title)
    }

    @Test
    fun useNullForBlankBody() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.txt")
        Assert.assertNull(local.notes)
    }

    @Test
    fun keepPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.txt",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.MEDIUM)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.MEDIUM, local.priority)
    }

    @Test
    fun useDefaultPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.txt",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.LOW, local.priority)
    }

    @Test
    fun noPriorityWhenDefaultIsHigh() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.txt",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.HIGH
        )
        assertEquals(Task.Priority.NONE, local.priority)
    }

    @Test
    fun noCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.txt")
        assertEquals(0, local.completionDate)
    }

    @Test
    fun parseCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/completed_task.txt")
        withTZ("America/Chicago") {
            assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.completionDate)
        }
    }

    @Test
    fun parseDueDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.txt")
        withTZ("America/Chicago") {
            assertEquals(DateTime(2023, 7, 19, 0, 0).millis, local.dueDate)
        }
    }

    @Test
    fun parseCreationDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.txt")
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 23, 20, 56, 9).millis,
                local.creationDate
            )
        }
    }

    @Test
    fun parseModificationDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.txt")
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 23, 21, 6, 269).millis,
                local.modificationDate
            )
        }
    }

    @Test
    fun parseDailyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_daily.txt")
            assertEquals("FREQ=DAILY", local.recurrence)
        }
    }

    @Test
    fun parseWeekdayRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_weekdays.txt")
            assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,TU,WE,TH,FR", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteMonthlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_monthly.txt")
            assertEquals("FREQ=MONTHLY", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteYearlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_yearly.txt")
            assertEquals("FREQ=YEARLY", local.recurrence)
        }
    }
}
