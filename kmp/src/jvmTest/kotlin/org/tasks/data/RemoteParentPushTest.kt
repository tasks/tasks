package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.service.TaskDeleter

class RemoteParentPushTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()

    private val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = taskDao,
        caldavDao = caldavDao,
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : org.tasks.service.TaskCleanup {},
    )

    private val appPreferences: AppPreferences = mock()

    private val mover = TaskMover(
        taskDao = taskDao,
        caldavDao = caldavDao,
        googleTaskDao = db.googleTaskDao(),
        appPreferences = appPreferences,
        refreshBroadcaster = mock(),
        taskDeleter = taskDeleter,
    )

    @Before
    fun setUp() = runBlocking {
        whenever(appPreferences.addTasksToTop()).thenReturn(false)
        caldavDao.insert(CaldavAccount(uuid = FROM_ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = FROM_LIST, account = FROM_ACCOUNT))
    }

    private suspend fun destination(accountType: Int): CaldavFilter {
        val account = CaldavAccount(uuid = TO_ACCOUNT, accountType = accountType)
        val calendar = CaldavCalendar(uuid = TO_LIST, account = TO_ACCOUNT)
        caldavDao.insert(account)
        caldavDao.insert(calendar)
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun newTask(title: String, parent: Long = 0): Task {
        val task = Task(title = title, parent = parent, remoteId = "uuid-$title")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = FROM_LIST, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    @Test
    fun movingASubtreeOntoACaldavListFilesTheParentRemotely() = runBlocking {
        val parent = newTask("parent")
        val child = newTask("child", parent = parent.id)

        mover.move(listOf(parent.id), destination(TYPE_CALDAV))

        assertEquals(
            caldavDao.getTask(parent.id)!!.remoteId,
            caldavDao.getTask(child.id)!!.remoteParent,
        )
    }

    @Test
    fun movingASubtreeOntoAMicrosoftListLeavesTheRemoteParentAlone() = runBlocking {
        val parent = newTask("parent")
        val child = newTask("child", parent = parent.id)

        mover.move(listOf(parent.id), destination(TYPE_MICROSOFT))

        assertNull(caldavDao.getTask(child.id)!!.remoteParent)
    }

    @Test
    fun theAccountForATaskIsTheOneItIsOnRatherThanOneItHasLeft() = runBlocking {
        val task = newTask("moved")

        mover.move(listOf(task.id), destination(TYPE_MICROSOFT))

        assertEquals(TYPE_MICROSOFT, caldavDao.getAccountForTask(task.id)!!.accountType)
    }

    companion object {
        private const val FROM_ACCOUNT = "account-from"
        private const val FROM_LIST = "list-from"
        private const val TO_ACCOUNT = "account-to"
        private const val TO_LIST = "list-to"
    }
}
