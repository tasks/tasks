package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jetbrains.compose.resources.getString
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.CaldavAccount
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDay
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.local_lists

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
    fun aZeroOffsetIsAValueNotAnAbsence() = runBlockingTest {
        val id = newTask("Dentist", Tasks.DUE_DATE to day(1))
        reminder(id, Reminders.TYPE_RELATIVE_DUE, offset = 0L)

        val row = engine.findReminders(ReminderQuery(taskIds = listOf(id))).rows.single()

        assertEquals(0L, row.offsetMs)
        assertEquals("at due time", describeOffset(row.offsetMs!!, row.type))
    }

    @Test
    fun aRandomReminderIsDescribedAsRandom() {
        assertEquals("randomly, about every 1 day", describeOffset(86_400_000L, Reminders.TYPE_RANDOM))
        assertEquals("randomly, about every 12 hours", describeOffset(43_200_000L, Reminders.TYPE_RANDOM))
        assertEquals("1 day after due", describeOffset(86_400_000L, Reminders.TYPE_RELATIVE_DUE))
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
    fun aRefusedBatchNamesTheEntry() = runBlockingTest {
        val error = runCatching {
            engine.createTasks(
                writer,
                listOf(TaskWrite(title = "Fine"), TaskWrite(title = "Orphan", parentId = 999L), TaskWrite(title = "Also fine")),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!, error.message!!.startsWith("Entry 2 of 3 (Orphan): "))
        assertTrue(error.message!!, "999 not found" in error.message!!)
        assertEquals(0, engine.findTasks(TaskQuery(limit = 0)).total)
    }

    @Test
    fun aSingleEntryIsNotNumbered() = runBlockingTest {
        val error = runCatching {
            engine.createTasks(writer, listOf(TaskWrite(title = "Orphan", parentId = 999L)))
        }.exceptionOrNull()

        assertTrue(error!!.message!!, error.message!!.startsWith("parent_id 999"))
    }

    @Test
    fun aNamelessLocalAccountIsCalledWhatTheAppCallsIt() = runBlockingTest {
        caldavDao.insert(CaldavAccount(uuid = UUIDHelper.newUUID(), accountType = CaldavAccount.TYPE_LOCAL))

        val names = engine.findAccounts(AccountQuery()).rows.map { it.name }
        val raw = query(TasksContract.Accounts.PATH).strings(TasksContract.Accounts.NAME)

        assertEquals(listOf(getString(Res.string.local_lists)), names.distinct())
        assertEquals(names, raw)
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
