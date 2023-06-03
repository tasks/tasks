package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavAccountMaker.ACCOUNT_TYPE
import org.tasks.makers.CaldavAccountMaker.newCaldavAccount
import org.tasks.makers.CaldavCalendarMaker.newCaldavCalendar
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class GoogleTaskDaoTests : InjectingTestCase() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao

    @Before
    override fun setUp() {
        super.setUp()
        runBlocking {
            caldavDao.insert(newCaldavAccount(with(ACCOUNT_TYPE, TYPE_GOOGLE_TASKS)))
            caldavDao.insert(newCaldavCalendar())
        }
    }

    @Test
    fun insertAtTopOfEmptyList() = runBlocking {
        insertTop(newCaldavTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("calendar")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", googleTaskDao.getByTaskId(task.id)?.remoteId)
        assertEquals(0L, task.order)
    }

    @Test
    fun insertAtBottomOfEmptyList() = runBlocking {
        insertBottom(newCaldavTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("calendar")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", googleTaskDao.getByTaskId(task.id)?.remoteId)
        assertEquals(0L, task.order)
    }

    @Test
    fun getPreviousIsNullForTopTask() = runBlocking {
        insert(newCaldavTask())
        assertNull(googleTaskDao.getPrevious("1", 0, 0))
    }

    @Test
    fun getPrevious() = runBlocking {
        insertTop(newCaldavTask())
        insertTop(newCaldavTask(with(REMOTE_ID, "1234")))
        assertEquals("1234", googleTaskDao.getPrevious("calendar", 0, 1))
    }

    @Test
    fun insertAtTopOfList() = runBlocking {
        insertTop(newCaldavTask(with(REMOTE_ID, "1234")))
        insertTop(newCaldavTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("calendar")
        assertEquals(2, tasks.size.toLong())
        val top = tasks[0]
        assertEquals("5678", googleTaskDao.getByTaskId(top.id)?.remoteId)
        assertEquals(0L, top.order)
    }

    @Test
    fun insertAtTopOfListShiftsExisting() = runBlocking {
        insertTop(newCaldavTask(with(REMOTE_ID, "1234")))
        insertTop(newCaldavTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("calendar")
        assertEquals(2, tasks.size.toLong())
        val bottom = tasks[1]
        assertEquals("1234", googleTaskDao.getByTaskId(bottom.id)?.remoteId)
        assertEquals(1L, bottom.order)
    }

    @Test
    fun getTaskFromRemoteId() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1234")))
        assertEquals(1L, googleTaskDao.getTask("1234"))
    }

    @Test
    fun getRemoteIdForTask() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1234")))
        assertEquals("1234", googleTaskDao.getRemoteId(1L))
    }

    @Test
    fun moveDownInList() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1")))
        insert(newCaldavTask(with(REMOTE_ID, "2")))
        insert(newCaldavTask(with(REMOTE_ID, "3")))
        val two = getByRemoteId("2")
        googleTaskDao.move(taskDao.fetch(two.task)!!, "calendar", 0, 0)
        assertEquals(0L, getOrder("2"))
        assertEquals(1L, getOrder("1"))
        assertEquals(2L, getOrder("3"))
    }

    @Test
    fun moveUpInList() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1")))
        insert(newCaldavTask(with(REMOTE_ID, "2")))
        insert(newCaldavTask(with(REMOTE_ID, "3")))
        val one = getByRemoteId("1")
        googleTaskDao.move(taskDao.fetch(one.task)!!, "calendar", 0, 1)
        assertEquals(0L, getOrder("2"))
        assertEquals(1L, getOrder("1"))
        assertEquals(2L, getOrder("3"))
    }

    @Test
    fun moveToTop() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1")))
        insert(newCaldavTask(with(REMOTE_ID, "2")))
        insert(newCaldavTask(with(REMOTE_ID, "3")))
        val three = getByRemoteId("3")
        googleTaskDao.move(taskDao.fetch(three.task)!!, "calendar", 0, 0)
        assertEquals(0L, getOrder("3"))
        assertEquals(1L, getOrder("1"))
        assertEquals(2L, getOrder("2"))
    }

    @Test
    fun moveToBottom() = runBlocking {
        insert(newCaldavTask(with(REMOTE_ID, "1")))
        insert(newCaldavTask(with(REMOTE_ID, "2")))
        insert(newCaldavTask(with(REMOTE_ID, "3")))
        val one = getByRemoteId("1")
        googleTaskDao.move(taskDao.fetch(one.task)!!, "calendar", 0, 2)
        assertEquals(0L, getOrder("2"))
        assertEquals(1L, getOrder("3"))
        assertEquals(2L, getOrder("1"))
    }

    @Test
    fun dontAllowEmptyParent() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", "", "0")

        assertNull(googleTaskDao.getByTaskId(1)!!.remoteParent)
    }
    
    @Test
    fun updatePositionWithNullParent() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", null, "0")

        assertNull(googleTaskDao.getByTaskId(1)!!.remoteParent)
    }

    @Test
    fun updatePosition() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", "abcd", "0")

        assertEquals("abcd", googleTaskDao.getByTaskId(1)!!.remoteParent)
    }

    @Test
    fun updateParents() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "123")))
        insert(newCaldavTask(with(TASK, 2), with(REMOTE_PARENT, "123")))

        caldavDao.updateParents()

        assertEquals(1, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun updateParentsByList() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "123")))
        insert(newCaldavTask(with(TASK, 2), with(REMOTE_PARENT, "123")))

        caldavDao.updateParents("calendar")

        assertEquals(1, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun updateParentsMustMatchList() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "123")))
        insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "2"), with(REMOTE_PARENT, "123")))

        caldavDao.updateParents()

        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun updateParentsByListMustMatchList() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "123")))
        insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "2"), with(REMOTE_PARENT, "123")))

        caldavDao.updateParents("2")

        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun ignoreEmptyStringWhenUpdatingParents() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "")))
        insert(newCaldavTask(with(TASK, 2), with(REMOTE_ID, ""), with(REMOTE_PARENT, "")))

        caldavDao.updateParents()

        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun ignoreEmptyStringWhenUpdatingParentsForList() = runBlocking {
        insert(newCaldavTask(with(TASK, 1), with(REMOTE_ID, "")))
        insert(newCaldavTask(with(TASK, 2), with(REMOTE_ID, ""), with(REMOTE_PARENT, "")))

        caldavDao.updateParents("1")

        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    private suspend fun getOrder(remoteId: String): Long? {
        return taskDao.fetch(googleTaskDao.getByRemoteId(remoteId)!!.task)?.order
    }

    private suspend fun insertTop(googleTask: CaldavTask) {
        insert(googleTask, true)
    }

    private suspend fun insertBottom(googleTask: CaldavTask) {
        insert(googleTask, false)
    }

    private suspend fun insert(googleTask: CaldavTask, top: Boolean = false) {
        val task = newTask()
        taskDao.createNew(task)
        googleTaskDao.insertAndShift(
            task,
            googleTask.copy(task = task.id),
            top
        )
    }

    private suspend fun getByRemoteId(remoteId: String): CaldavTask {
        return googleTaskDao.getByRemoteId(remoteId)!!
    }
}