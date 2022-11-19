package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.data.Task
import org.junit.Assert
import org.junit.Test
import org.tasks.TestUtilities
import org.tasks.makers.TaskMaker
import org.tasks.time.DateTime

class ConvertFromMicrosoftTests {
    @Test
    fun titleFromRemote() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.txt")
        Assert.assertEquals("Basic task", local.title)
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
        Assert.assertEquals(Task.Priority.MEDIUM, local.priority)
    }

    @Test
    fun useDefaultPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.txt",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.LOW
        )
        Assert.assertEquals(Task.Priority.LOW, local.priority)
    }

    @Test
    fun noPriorityWhenDefaultIsHigh() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.txt",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.HIGH
        )
        Assert.assertEquals(Task.Priority.NONE, local.priority)
    }

    @Test
    fun noCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.txt")
        Assert.assertEquals(0, local.completionDate)
    }

    @Test
    fun parseCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/completed_task.txt")
        Assert.assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.completionDate)
    }
}