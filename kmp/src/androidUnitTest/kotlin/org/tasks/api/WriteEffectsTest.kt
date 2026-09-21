package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jetbrains.compose.resources.getString
import org.tasks.api.TasksContract.Places
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
    fun aRelativeReminderNeedsItsDate() {
        val dateless = newTask("Someday")
        val dated = newTask("Dentist", Tasks.DUE_DATE to day(1))

        val dueError = runCatching { reminder(dateless, Reminders.TYPE_RELATIVE_DUE) }.exceptionOrNull()
        val startError = runCatching { reminder(dated, Reminders.TYPE_RELATIVE_START) }.exceptionOrNull()
        reminder(dated, Reminders.TYPE_RELATIVE_DUE)

        assertTrue(dueError is IllegalArgumentException)
        assertTrue(dueError!!.message!!, "no due date" in dueError.message!!)
        assertTrue(startError is IllegalArgumentException)
        assertTrue(startError!!.message!!, "no start date" in startError.message!!)
        assertEquals(0, query(Reminders.PATH, "?task_id=$dateless").rows())
        assertEquals(1, query(Reminders.PATH, "?task_id=$dated").rows())
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

        fun advanced(what: String) {
            val now = modifiedOf(id)
            assertTrue("$what did not bump modified", now > last)
            last = now
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

        assertTrue(names.toString(), getString(Res.string.local_lists) in names)
        assertTrue(names.toString(), names.none { it.isNullOrBlank() })
        assertEquals(names, raw)
    }

    @Test
    fun aReminderThatCouldNeverFireIsRefused() {
        val id = newTask("Dentist", Tasks.DUE_DATE to day(1))

        val timeless = runCatching { insert(Reminders.PATH, Reminders.TASK_ID to id, Reminders.TYPE to Reminders.TYPE_DATE_TIME) }.exceptionOrNull()
        val backwards = runCatching { reminder(id, Reminders.TYPE_RANDOM, offset = -60_000L) }.exceptionOrNull()
        val gapless = runCatching {
            insert(Reminders.PATH, Reminders.TASK_ID to id, Reminders.TYPE to Reminders.TYPE_RELATIVE_DUE, Reminders.OFFSET_MS to 0L, Reminders.REPEAT_COUNT to 3)
        }.exceptionOrNull()
        val frantic = runCatching {
            insert(Reminders.PATH, Reminders.TASK_ID to id, Reminders.TYPE to Reminders.TYPE_RELATIVE_DUE, Reminders.OFFSET_MS to 0L, Reminders.REPEAT_COUNT to 3, Reminders.INTERVAL_MS to 15L)
        }.exceptionOrNull()
        val jittery = runCatching { reminder(id, Reminders.TYPE_RANDOM, offset = 15L) }.exceptionOrNull()

        assertTrue(timeless?.message ?: "no error", "trigger_at is required" in timeless?.message.orEmpty())
        assertTrue(backwards?.message ?: "no error", "must be positive" in backwards?.message.orEmpty())
        assertTrue(gapless?.message ?: "no error", "interval_ms is required" in gapless?.message.orEmpty())
        assertTrue(frantic?.message ?: "no error", "at least a minute" in frantic?.message.orEmpty())
        assertTrue(jittery?.message ?: "no error", "at least a minute" in jittery?.message.orEmpty())
        assertEquals(0, query(Reminders.PATH, "?task_id=$id").rows())
    }

    @Test
    fun aReminderIsOnlyRemovedThroughItsOwnTask() = runBlockingTest {
        val mine = newTask("Dentist", Tasks.DUE_DATE to day(1))
        val other = newTask("Taxes")
        val id = reminder(mine, Reminders.TYPE_RELATIVE_DUE)

        val error = runCatching { engine.setTaskReminders(writer, other, emptyList(), listOf(id)) }.exceptionOrNull()

        assertTrue(error?.message ?: "no error", "belongs to task $mine" in error?.message.orEmpty())
        assertEquals(1, query(Reminders.PATH, "?task_id=$mine").rows())
    }

    @Test
    fun aPlaceIsOnTheMapWithARealRadius() {
        val offMap = runCatching { insert(Places.PATH, Places.LATITUDE to 91.0, Places.LONGITUDE to 0.0) }.exceptionOrNull()
        val flat = runCatching { insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0, Places.RADIUS to 0) }.exceptionOrNull()
        val id = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0, Places.NAME to "   ", Places.ADDRESS to "1 Main St")
        val shrunk = runCatching { update(Places.PATH, id, Places.RADIUS to -10) }.exceptionOrNull()

        assertTrue(offMap?.message ?: "no error", "not on the map" in offMap?.message.orEmpty())
        assertTrue(flat?.message ?: "no error", "radius" in flat?.message.orEmpty())
        assertTrue(shrunk?.message ?: "no error", "radius" in shrunk?.message.orEmpty())
        assertEquals("1 Main St", query(Places.PATH, "?_id=$id").string(Places.DISPLAY_NAME))
    }

    @Test
    fun negativePagingIsRefusedRatherThanReadAsACount() {
        assertTrue(runCatching { TaskQuery(limit = -1) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { TaskQuery(offset = -1) }.exceptionOrNull() is IllegalArgumentException)
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
