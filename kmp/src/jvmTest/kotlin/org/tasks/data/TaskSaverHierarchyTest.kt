package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class TaskSaverHierarchyTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()
    private val dirtyDao = db.dirtyDao()

    private val taskSaver = TaskSaver(
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = mock(),
        locationService = mock(),
        timerPlugin = mock(),
        backgroundWork = mock(),
        caldavDao = caldavDao,
    )

    private suspend fun newTask(title: String, parent: Long = 0, order: Long? = null): Task {
        val task = Task(title = title, parent = parent, order = order, remoteId = "uuid-$title")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = CALENDAR, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    private suspend fun setUpAccount() {
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = CALENDAR, account = ACCOUNT))
    }

    @Test
    fun aSaveThatPreservesHierarchyLeavesTheStoredParentAlone() = runBlocking {
        setUpAccount()
        val parent = newTask("parent")
        val task = newTask("task", parent = parent.id, order = 700_000_000L)

        val editing = task.copy(title = "renamed", parent = 0, order = null)
        taskSaver.save(editing, task, preserveHierarchy = true)

        val stored = taskDao.fetch(task.id)!!
        assertEquals("renamed", stored.title)
        assertEquals(parent.id, stored.parent)
        assertEquals(700_000_000L, stored.order)
    }

    @Test
    fun aSaveThatPreservesHierarchyPicksUpAReparentingItNeverSaw() = runBlocking {
        setUpAccount()
        val first = newTask("first")
        val second = newTask("second")
        val task = newTask("task", parent = first.id, order = 1L)

        val editing = task.copy(title = "renamed")
        taskDao.setParent(second.id, listOf(task.id))
        taskSaver.save(editing, task, preserveHierarchy = true)

        val stored = taskDao.fetch(task.id)!!
        assertEquals("renamed", stored.title)
        assertEquals(second.id, stored.parent)
    }

    @Test
    fun aSaveThatMeansItStillWritesWhereTheRowSits() = runBlocking {
        setUpAccount()
        val first = newTask("first")
        val second = newTask("second")
        val task = newTask("task", parent = first.id, order = 1L)

        val moved = task.copy(parent = second.id, order = 9L)
        taskSaver.save(moved, task)

        val stored = taskDao.fetch(task.id)!!
        assertEquals(second.id, stored.parent)
        assertEquals(9L, stored.order)
    }

    @Test
    fun aPreservedSaveIsNotMarkedDirtyForAMoveItDidNotMake() = runBlocking {
        setUpAccount()
        val first = newTask("first")
        newTask("second")
        val task = newTask("task", parent = first.id, order = 1L)
        dirtyDao.setDirtyState(caldavDao.getTask(task.id)!!.id, 1L, 1L)

        val editing = task.copy(parent = 0, order = null)
        taskSaver.save(editing, task, preserveHierarchy = true)

        assertEquals(false, dirtyDao.isDirty(caldavDao.getTask(task.id)!!.id))
        assertEquals(first.id, taskDao.fetch(task.id)!!.parent)
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val CALENDAR = "calendar"
    }
}
