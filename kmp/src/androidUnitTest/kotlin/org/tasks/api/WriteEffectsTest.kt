package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Tasks
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDay

class WriteEffectsTest : ApiTestCase() {

    @Test
    fun deletingATaskCountsEveryDescendant() = runBlockingTest {
        val root = newTask("Trip")
        val child = newTask("Pack", Tasks.PARENT_ID to root)
        newTask("Tent", Tasks.PARENT_ID to child)
        newTask("Poles", Tasks.PARENT_ID to child)

        val deletion = engine.deleteTask(writer, root)

        assertEquals(1, deletion.rowsDeleted)
        assertEquals(3, deletion.alsoAffected)
        assertEquals(0, engine.findTasks(TaskQuery(status = "any", limit = 0)).total)
    }

    @Test
    fun completingAParentReportsTheSubtasksThatWentWithIt() = runBlockingTest {
        val root = newTask("Trip")
        val child = newTask("Pack", Tasks.PARENT_ID to root)
        val grandchild = newTask("Tent", Tasks.PARENT_ID to child)

        val completion = engine.completeTasks(writer, listOf(root), completed = true)

        assertEquals(listOf(child, grandchild).sorted(), completion.alsoCompletedTaskIds.sorted())
        assertTrue(completion.reopenedTaskIds.isEmpty())
    }

    @Test
    fun reopeningASubtaskReportsTheParentsThatReopened() = runBlockingTest {
        val root = newTask("Trip")
        val child = newTask("Pack", Tasks.PARENT_ID to root)
        val grandchild = newTask("Tent", Tasks.PARENT_ID to child)
        engine.completeTasks(writer, listOf(root), completed = true)

        val completion = engine.completeTasks(writer, listOf(grandchild), completed = false)

        assertEquals(listOf(root, child).sorted(), completion.reopenedTaskIds.sorted())
        assertTrue(completion.alsoCompletedTaskIds.isEmpty())
    }

    @Test
    fun reopeningAParentReportsTheSubtasksThatReopenedWithIt() = runBlockingTest {
        val root = newTask("Trip")
        val child = newTask("Pack", Tasks.PARENT_ID to root)
        val grandchild = newTask("Tent", Tasks.PARENT_ID to child)
        engine.completeTasks(writer, listOf(root), completed = true)

        val completion = engine.completeTasks(writer, listOf(root), completed = false)

        assertEquals(listOf(child, grandchild).sorted(), completion.reopenedTaskIds.sorted())
    }

    @Test
    fun tagsRemindersListsAndParentsAllCountAsChanges() = runBlockingTest {
        val id = newTask("Dentist", Tasks.DUE_DATE to day(1))
        val parent = newTask("Health")
        val elsewhere = newList("Elsewhere")
        val tag = engine.createTag(writer, TagWrite(name = "urgent")).row.id
        var last = modifiedOf(id)

        fun advanced(what: String): Long {
            val now = modifiedOf(id)
            assertTrue("$what did not bump modified", now > last)
            last = now
            return now
        }

        pause()
        engine.setTaskTags(writer, listOf(id), add = listOf(tag), remove = emptyList())
        advanced("tagging")
        pause()
        reminder(id, Reminders.TYPE_RELATIVE_DUE, offset = -60_000L)
        advanced("a reminder")
        pause()
        update(Tasks.PATH, id, Tasks.LIST_ID to elsewhere)
        advanced("a list move")
        pause()
        update(Tasks.PATH, parent, Tasks.LIST_ID to elsewhere)
        update(Tasks.PATH, id, Tasks.PARENT_ID to parent)
        advanced("re-parenting")
    }

    @Test
    fun overdueIsWhatTheAppCallsOverdue() = runBlockingTest {
        val now = currentTimeMillis()
        newTask("Timed, passed", Tasks.DUE_DATE to now - 60_000)
        newTask("Timed, ahead", Tasks.DUE_DATE to now + 60_000)
        newTask("All day, yesterday", Tasks.DUE_DATE to now.startOfDay() - ONE_DAY, Tasks.DUE_ALL_DAY to 1)
        newTask("All day, today", Tasks.DUE_DATE to now, Tasks.DUE_ALL_DAY to 1)

        val titles = engine.findTasks(TaskQuery(due = "overdue")).rows.map { it.title }.sorted()
        val raw = query(Tasks.PATH, "?overdue=1").strings(Tasks.TITLE).sorted()
        val old = runCatching { TaskQuery(due = "today") }.exceptionOrNull()

        assertEquals(listOf("All day, yesterday", "Timed, passed"), titles)
        assertEquals(titles, raw)
        assertTrue(old is IllegalArgumentException)
    }

    @Test
    fun movingASubtaskAloneDetachesItAndSaysSo() = runBlockingTest {
        val parent = newTask("Trip")
        val child = newTask("Pack", Tasks.PARENT_ID to parent)
        val elsewhere = newList("Elsewhere")

        val revision = engine.updateTasks(writer, listOf(TaskUpdate(child, TaskWrite(listId = elsewhere))))

        assertEquals(listOf(child), revision.detachedIds)
        assertEquals(0L, query(Tasks.PATH, "?_id=$child").long(Tasks.PARENT_ID))
    }

    private fun reminder(task: Long, type: String, offset: Long = -3_600_000L): Long =
        insert(Reminders.PATH, Reminders.TASK_ID to task, Reminders.TYPE to type, Reminders.OFFSET_MS to offset)

    private fun modifiedOf(id: Long): Long = query(Tasks.PATH, "?_id=$id").long(Tasks.MODIFIED_AT)

    private fun pause() = Thread.sleep(5)
}
