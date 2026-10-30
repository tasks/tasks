package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.service.TaskDeleter

class MakeParentTaskTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()

    private val taskSaver = TaskSaver(
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = mock(),
        locationService = mock(),
        timerPlugin = mock(),
        backgroundWork = mock(),
        caldavDao = caldavDao,
    )

    private val appPreferences: AppPreferences = mock()

    private val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = taskDao,
        caldavDao = caldavDao,
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : org.tasks.service.TaskCleanup {},
    )

    private val taskMover = TaskMover(
        taskDao = taskDao,
        caldavDao = caldavDao,
        googleTaskDao = db.googleTaskDao(),
        dirtyDao = db.dirtyDao(),
        appPreferences = appPreferences,
        refreshBroadcaster = mock(),
        taskDeleter = taskDeleter,
    )

    private suspend fun fromList() = CaldavFilter(
        calendar = caldavDao.getCalendar(LIST)!!,
        account = caldavDao.getAccountByUuid(ACCOUNT)!!,
    )

    private suspend fun newTask(title: String, parent: Long = 0): Task {
        val task = Task(title = title, parent = parent, remoteId = "uuid-$title")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = LIST, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    private suspend fun isPending(taskId: Long): Boolean =
        db.dirtyDao().getDirtyStateByTaskIds(listOf(taskId))[taskId]
            ?.let { it.dirtyVersion > it.syncedVersion }
            ?: false

    @Before
    fun setUp() = runBlocking {
        whenever(appPreferences.addTasksToTop()).thenReturn(false)
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = LIST, account = ACCOUNT))
    }

    @Test
    fun createAddsATopLevelTaskAtTheEndOfTheList() = runBlocking {
        val first = newTask("first")
        val second = newTask("second")

        val parent = MakeParentTask.create(taskDao, caldavDao, taskSaver, fromList(), "  Project  ")

        assertTrue("the created row should be stamped with its id", parent.id > 0)
        val stored = taskDao.fetch(parent.id)!!
        assertEquals("the title should be trimmed", "Project", stored.title)
        assertEquals(0L, stored.parent)
        assertEquals(
            "the new top-level task lands after the existing ones",
            second.order?.plus(1),
            stored.order,
        )
        assertEquals(LIST, caldavDao.getTask(parent.id)!!.calendar)
        assertTrue("the created task should be queued for sync", isPending(parent.id))
        assertEquals(3, taskDao.getChildren(0L).size)
    }

    @Test
    fun nestMakesTheTaskADirectChildOfTheNewParentAndKeepsItsSubtree() = runBlocking {
        val grandchild = newTask("grandchild")
        val child = newTask("child")
        taskDao.setParent(child.id, listOf(grandchild.id))
        // A nested task's CalDAV remote parent is what drives the parent columns on an
        // icalendar account; real data always carries it, so the fixture must too.
        caldavDao.update(caldavDao.getTask(grandchild.id)!!.id, child.remoteId)

        val parent = MakeParentTask.create(taskDao, caldavDao, taskSaver, fromList(), "Project")
        MakeParentTask.nest(taskMover, LIST, child.id, parent.id)

        assertEquals(parent.id, taskDao.fetch(child.id)!!.parent)
        assertEquals(
            "the moved task's own subtree must stay intact",
            child.id,
            taskDao.fetch(grandchild.id)!!.parent,
        )
        assertEquals(
            "the CalDAV remote parent (the parent's VTODO UID) drives the server-side parent",
            caldavDao.getTask(parent.id)!!.remoteId,
            caldavDao.getTask(child.id)!!.remoteParent,
        )
        assertTrue("the moved task should be queued for sync", isPending(child.id))
        assertTrue("its children should be queued for sync too", isPending(grandchild.id))
    }

    @Test
    fun nestRejectsAnUncreatedParent() = runBlocking {
        val child = newTask("child")
        val failure = runCatching { MakeParentTask.nest(taskMover, LIST, child.id, parentId = 0) }
        assertTrue("an uncreated parent must be rejected", failure.isFailure)
        assertEquals(0L, taskDao.fetch(child.id)!!.parent)
    }

    @Test
    fun aTaskWithUnsavedOrderStillNestsCleanly() = runBlocking {
        // The pane only offers the action on a saved task, but the data path must not
        // depend on it: a task that has never had an order assigned still nests.
        val child = Task(title = "child", remoteId = "uuid-child")
        taskDao.createNew(child)
        val parent = MakeParentTask.create(taskDao, caldavDao, taskSaver, fromList(), "Project")
        MakeParentTask.nest(taskMover, LIST, child.id, parent.id)

        assertEquals(parent.id, taskDao.fetch(child.id)!!.parent)
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val LIST = "list"
    }
}
