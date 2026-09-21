package com.todoroo.astrid.service

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.TaskMover
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

@HiltAndroidTest
class TaskDuplicatorTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDuplicator: TaskDuplicator
    @Inject lateinit var taskMover: TaskMover

    @Before
    fun setup() = runBlocking {
        caldavDao.insert(CaldavAccount(uuid = CALDAV_ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = CALDAV_LIST, account = CALDAV_ACCOUNT))
        caldavDao.insert(CaldavAccount(uuid = MS_ACCOUNT, accountType = TYPE_MICROSOFT))
        caldavDao.insert(CaldavCalendar(uuid = MS_LIST, account = MS_ACCOUNT))
    }

    private suspend fun newTask(title: String, list: String, parent: Long = 0): Task {
        val task = Task(title = title, parent = parent, remoteId = "uuid-$title")
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = list, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    @Test
    fun duplicatingASubtaskOnACaldavListFilesItUnderItsParentRemotely() = runBlocking {
        val parent = newTask("parent", CALDAV_LIST)
        val child = newTask("child", CALDAV_LIST, parent = parent.id)

        val copy = taskDuplicator.duplicate(listOf(child.id)).single()

        assertNotEquals(child.id, copy.id)
        assertEquals(parent.id, copy.parent)
        assertEquals(
            caldavDao.getTask(parent.id)!!.remoteId,
            caldavDao.getTask(copy.id)!!.remoteParent,
        )
    }

    @Test
    fun duplicatingASubtaskOnAMicrosoftListLeavesTheRemoteParentAlone() = runBlocking {
        val parent = newTask("parent", MS_LIST)
        val child = newTask("child", MS_LIST, parent = parent.id)

        val copy = taskDuplicator.duplicate(listOf(child.id)).single()

        assertEquals(parent.id, copy.parent)
        assertNull(caldavDao.getTask(copy.id)!!.remoteParent)
    }

    @Test
    fun theListATaskHasLeftDoesNotDecideHowItsCopyIsFiled() = runBlocking {
        val parent = newTask("parent", CALDAV_LIST)
        val child = newTask("child", CALDAV_LIST, parent = parent.id)
        taskMover.move(
            listOf(parent.id),
            CaldavFilter(
                calendar = CaldavCalendar(uuid = MS_LIST, account = MS_ACCOUNT),
                account = CaldavAccount(uuid = MS_ACCOUNT, accountType = TYPE_MICROSOFT),
            ),
        )
        assertTrue(caldavDao.getTask(child.id)!!.calendar == MS_LIST)

        val copy = taskDuplicator.duplicate(listOf(child.id)).single()

        assertNull(caldavDao.getTask(copy.id)!!.remoteParent)
    }

    companion object {
        private const val CALDAV_ACCOUNT = "caldav-account"
        private const val CALDAV_LIST = "caldav-list"
        private const val MS_ACCOUNT = "ms-account"
        private const val MS_LIST = "ms-list"
    }
}
