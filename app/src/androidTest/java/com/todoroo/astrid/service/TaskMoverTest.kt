package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.TaskMover
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.filters.CaldavFilter
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@HiltAndroidTest
class TaskMoverTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskMover: TaskMover

    @Before
    fun setup() {
        runBlocking {
            caldavDao.insert(CaldavCalendar(uuid = "1", account = "account1"))
            caldavDao.insert(CaldavCalendar(uuid = "2", account = "account2"))
        }
    }

    @Test
    fun moveBetweenGoogleTaskLists() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        setAccountType("account2", TYPE_GOOGLE_TASKS)
        createTasks(1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1)?.calendar)
    }

    @Test
    fun deleteGoogleTaskAfterMove() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        val deleted = caldavDao.getMovedByAccount("account1")
        assertEquals(1, deleted.size.toLong())
        assertEquals(1, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
    }

    @Test
    fun moveChildrenBetweenGoogleTaskLists() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        setAccountType("account2", TYPE_GOOGLE_TASKS)
        createTasks(1)
        createSubtask(2, 1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        val deleted = caldavDao.getMovedByAccount("account1")
        assertEquals(2, deleted.size.toLong())
        assertTrue(deleted.all { it.deleted > 0 })
        assertEquals(1L, taskDao.fetch(2)?.parent)
        assertEquals("2", googleTaskDao.getByTaskId(2)?.calendar)
    }

    @Test
    fun moveBetweenCaldavList() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun deleteCaldavTaskAfterMove() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        val deleted = caldavDao.getMoved("1")
        assertEquals(1, deleted.size.toLong())
        assertEquals(1, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
    }

    @Test
    fun moveRecursiveCaldavChildren() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "b"))))
        moveToCaldavList("2", 1)
        val deleted = caldavDao.getMoved("1")
        assertEquals(3, deleted.size.toLong())
        val task = caldavDao.getTask(3)
        assertEquals("2", task!!.calendar)
        assertEquals(2, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveSubtreeUnderParentInAnotherList() = runBlocking {
        // existing parent in list "2"
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "2"), with(REMOTE_ID, "p")))
        // task with a subtask in list "1"
        createTasks(2)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(with(TASK, 2L), with(CALENDAR, "1"), with(REMOTE_ID, "x")),
                        newCaldavTask(with(TASK, 3L), with(CALENDAR, "1"), with(REMOTE_PARENT, "x"))))
        // drag task 2 (with subtask 3) under task 1 in the other list
        taskMover.move(
            ids = listOf(2L),
            selectedList = CaldavFilter(CaldavCalendar(name = "", uuid = "2"), CaldavAccount(accountType = TYPE_CALDAV)),
            newParent = 1L,
        )
        // whole subtree moved to the destination list
        assertEquals("2", caldavDao.getTask(2)!!.calendar)
        assertEquals("2", caldavDao.getTask(3)!!.calendar)
        // task 2 nested under task 1, and its own subtree preserved
        assertEquals(1L, taskDao.fetch(2)!!.parent)
        assertEquals(2L, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun rewriteUidsWhenMovingCaldavSubtree() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "c"),
                                with(REMOTE_PARENT, "b"))))
        moveToCaldavList("2", 1)
        val newParent = caldavDao.getTask(1)!!
        val newChild = caldavDao.getTask(2)!!
        val newGrandchild = caldavDao.getTask(3)!!
        // UIDs are rewritten so the moved tasks don't transiently share a UID with the
        // about-to-be-deleted copies in the source list, which corrupts the parent
        // relations in the dmfs provider and flattens the hierarchy locally
        assertNotEquals("a", newParent.remoteId)
        assertNotEquals("b", newChild.remoteId)
        assertNotEquals("c", newGrandchild.remoteId)
        // remoteParent is rewired to the new UIDs at every level of the subtree
        assertEquals(newParent.remoteId, newChild.remoteParent)
        assertEquals(newChild.remoteId, newGrandchild.remoteParent)
        // and the local hierarchy is preserved
        assertEquals(1L, taskDao.fetch(2)!!.parent)
        assertEquals(2L, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveGoogleTaskChildrenToCaldav() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        setAccountType("account2", TYPE_CALDAV)
        createTasks(1)
        createSubtask(2, 1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        val task = caldavDao.getTask(2)
        assertEquals("2", task!!.calendar)
        assertEquals(1L, taskDao.fetch(2)?.parent)
    }

    @Test
    fun flattenLocalSubtasksWhenMovingToGoogleTasks() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToGoogleTasks("1", 1)
        assertEquals(1L, taskDao.fetch(3)?.parent)
    }

    @Test
    fun moveLocalChildToGoogleTasks() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        moveToGoogleTasks("1", 2)
        assertEquals(0L, taskDao.fetch(2)?.parent)
    }

    @Test
    fun moveLocalChildToCaldav() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        moveToCaldavList("1", 2)
        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun flattenCaldavSubtasksWhenMovingToGoogleTasks() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "b"))))
        moveToGoogleTasks("2", 1)
        val task = taskDao.fetch(3L)
        assertEquals(1L, task?.parent)
    }

    @Test
    fun moveGoogleTaskChildWithoutParent() = runBlocking {
        setAccountType("account2", TYPE_GOOGLE_TASKS)
        createTasks(1)
        createSubtask(2, 1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 2)
        assertEquals(0L, taskDao.fetch(2)?.parent)
        assertEquals("2", googleTaskDao.getByTaskId(2)?.calendar)
    }

    @Test
    fun moveCaldavChildWithoutParent() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "a"))))
        moveToCaldavList("2", 2)
        assertEquals("2", caldavDao.getTask(2)!!.calendar)
        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun moveGoogleTaskToCaldav() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun moveCaldavToGoogleTask() = runBlocking {
        setAccountType("account1", TYPE_CALDAV)
        setAccountType("account2", TYPE_GOOGLE_TASKS)
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1L)?.calendar)
    }

    @Test
    fun moveLocalToCaldav() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToCaldavList("1", 1)
        assertEquals("1", caldavDao.getTask(3)?.calendar)
        assertEquals(2L, taskDao.fetch(3)?.parent)
    }

    @Test
    fun moveToSameGoogleTaskListIsNoop() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        createTasks(1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        moveToGoogleTasks("1", 1)
        assertTrue(caldavDao.getMovedByAccount("account1").isEmpty())
        assertEquals(1, googleTaskDao.getAllByTaskId(1).size.toLong())
    }

    @Test
    fun moveToSameCaldavListIsNoop() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("1", 1)
        assertTrue(caldavDao.getMoved("1").isEmpty())
        assertEquals(1, caldavDao.getTasks(1).size.toLong())
    }

    @Test
    fun flattenCaldavSubtasksWhenMovingToMicrosoft() = runBlocking {
        setAccountType("account2", TYPE_MICROSOFT)
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "c"),
                                with(REMOTE_PARENT, "b"))))
        moveToMicrosoftList("2", 1)
        // Microsoft To Do is single-level: the grandchild collapses to a direct child of the root
        assertEquals("2", caldavDao.getTask(3)!!.calendar)
        assertEquals(1L, taskDao.fetch(2)!!.parent)
        assertEquals(1L, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun flattenSubtreeWhenNestingUnderMicrosoftParent() = runBlocking {
        setAccountType("account2", TYPE_MICROSOFT)
        // existing top-level parent in Microsoft list "2"
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "2"), with(REMOTE_ID, "p")))
        // task with a subtask in list "1"
        createTasks(2)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(with(TASK, 2L), with(CALENDAR, "1"), with(REMOTE_ID, "x")),
                        newCaldavTask(with(TASK, 3L), with(CALENDAR, "1"), with(REMOTE_PARENT, "x"))))
        // nest task 2 (with subtask 3) under task 1 in the single-level Microsoft list
        taskMover.move(
            ids = listOf(2L),
            selectedList = CaldavFilter(CaldavCalendar(name = "", uuid = "2"), CaldavAccount(accountType = TYPE_MICROSOFT)),
            newParent = 1L,
        )
        // whole subtree moved to the destination list
        assertEquals("2", caldavDao.getTask(2)!!.calendar)
        assertEquals("2", caldavDao.getTask(3)!!.calendar)
        // Microsoft is single-level: both the moved task and its subtask become direct children
        // of the destination parent, not a 2-level hierarchy nested under task 2
        assertEquals(1L, taskDao.fetch(2)!!.parent)
        assertEquals(1L, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun flattenSubtreeWhenNestingUnderGoogleParent() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        setAccountType("account2", TYPE_GOOGLE_TASKS)
        // existing top-level parent in Google list "2"
        createTasks(1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "2")))
        // task with a subtask in list "1"
        createTasks(2)
        createSubtask(3, 2)
        googleTaskDao.insert(newCaldavTask(with(TASK, 2L), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 3L), with(CALENDAR, "1")))
        // nest task 2 (with subtask 3) under task 1 in the single-level Google list
        taskMover.move(
            ids = listOf(2L),
            selectedList = CaldavFilter(CaldavCalendar(name = "", uuid = "2"), CaldavAccount(accountType = TYPE_GOOGLE_TASKS)),
            newParent = 1L,
        )
        // whole subtree moved to the destination list
        assertEquals("2", googleTaskDao.getByTaskId(2)?.calendar)
        assertEquals("2", googleTaskDao.getByTaskId(3)?.calendar)
        // Google Tasks is single-level: both the moved task and its subtask become direct
        // children of the destination parent
        assertEquals(1L, taskDao.fetch(2)!!.parent)
        assertEquals(1L, taskDao.fetch(3)!!.parent)
        // the moved task is ordered as a child of the new parent (first child), not left with
        // the top-level order it was given before being nested
        assertEquals(0L, taskDao.fetch(2)!!.order)
    }

    @Test
    fun moveToMicrosoftPreservesExistingParent() = runBlocking {
        // Create a parent-child in a Microsoft list
        setAccountType("account2", TYPE_MICROSOFT)
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(TASK, 1L), with(CALENDAR, "2"), with(REMOTE_ID, "parent-remote")))
        createSubtask(2, 1)
        caldavDao.insert(CaldavTask(task = 2L, calendar = "2", remoteParent = ""))
        // Move a different task into the same Microsoft list
        createTasks(3)
        caldavDao.insert(newCaldavTask(with(TASK, 3L), with(CALENDAR, "1")))
        moveToMicrosoftList("2", 3)
        // Don't clobber the existing parent-child
        assertEquals(1L, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun dontDuplicateWhenParentAndChildGoogleTaskMoved() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1, 2)
        assertEquals(1, googleTaskDao.getAllByTaskId(2).filter { it.deleted == 0L }.size)
    }

    @Test
    fun dontDuplicateWhenParentAndChildCaldavMoved() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "a"))))
        moveToCaldavList("2", 1, 2)
        assertEquals(1, caldavDao.getTasks(2).filter { it.deleted == 0L }.size)
    }

    private suspend fun createTasks(vararg ids: Long) {
        for (id in ids) {
            taskDao.createNew(newTask(with(ID, id)))
        }
    }

    private suspend fun createSubtask(id: Long, parent: Long) {
        taskDao.createNew(newTask(with(ID, id), with(PARENT, parent)))
    }

    private suspend fun moveToGoogleTasks(list: String, vararg tasks: Long) {
        taskMover.move(
            tasks.toList(),
            CaldavFilter(
                calendar = CaldavCalendar(uuid = list),
                account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
            )
        )
    }

    private suspend fun moveToCaldavList(calendar: String, vararg tasks: Long) {
        taskMover.move(
            tasks.toList(),
            CaldavFilter(
                CaldavCalendar(name = "", uuid = calendar),
                account = CaldavAccount(accountType = TYPE_CALDAV)
            )
        )
    }

    private suspend fun moveToMicrosoftList(calendar: String, vararg tasks: Long) {
        taskMover.move(
            tasks.toList(),
            CaldavFilter(
                CaldavCalendar(name = "", uuid = calendar),
                account = CaldavAccount(accountType = TYPE_MICROSOFT)
            )
        )
    }

    private suspend fun setAccountType(account: String, type: Int) {
        caldavDao.insert(
            CaldavAccount(
                uuid = account,
                accountType = type,
            )
        )
    }
}