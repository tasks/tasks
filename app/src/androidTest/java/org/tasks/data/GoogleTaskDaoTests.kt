package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.GoogleTaskMaker.LIST
import org.tasks.makers.GoogleTaskMaker.PARENT
import org.tasks.makers.GoogleTaskMaker.REMOTE_ID
import org.tasks.makers.GoogleTaskMaker.TASK
import org.tasks.makers.GoogleTaskMaker.newGoogleTask
import org.tasks.makers.GtaskListMaker.newGtaskList
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GoogleTaskDaoTests : InjectingTestCase() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskDao: TaskDao

    @Before
    override fun setUp() {
        super.setUp()
        googleTaskListDao.insert(newGtaskList())
    }

    @Test
    fun insertAtTopOfEmptyList() {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", task.remoteId)
        assertEquals(0, task.order)
    }

    @Test
    fun insertAtBottomOfEmptyList() {
        insertBottom(newGoogleTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", task.remoteId)
        assertEquals(0, task.order)
    }

    @Test
    fun getPreviousIsNullForTopTask() {
        googleTaskDao.insertAndShift(newGoogleTask(), true)
        assertNull(googleTaskDao.getPrevious("1", 0, 0))
    }

    @Test
    fun getPrevious() {
        insertTop(newGoogleTask())
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        assertEquals("1234", googleTaskDao.getPrevious("1", 0, 1))
    }

    @Test
    fun insertAtTopOfList() {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        insertTop(newGoogleTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(2, tasks.size.toLong())
        val top = tasks[0]
        assertEquals("5678", top.remoteId)
        assertEquals(0, top.order)
    }

    @Test
    fun insertAtTopOfListShiftsExisting() {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        insertTop(newGoogleTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(2, tasks.size.toLong())
        val bottom = tasks[1]
        assertEquals("1234", bottom.remoteId)
        assertEquals(1, bottom.order)
    }

    @Test
    fun getTaskFromRemoteId() {
        googleTaskDao.insert(newGoogleTask(with(REMOTE_ID, "1234"), with(TASK, 4)))
        assertEquals(4, googleTaskDao.getTask("1234"))
    }

    @Test
    fun getRemoteIdForTask() {
        googleTaskDao.insert(newGoogleTask(with(REMOTE_ID, "1234"), with(TASK, 4)))
        assertEquals("1234", googleTaskDao.getRemoteId(4L))
    }

    @Test
    fun moveDownInList() {
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false)
        val two = getByRemoteId("2")
        googleTaskDao.move(two, 0, 0)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("3")!!.order)
    }

    @Test
    fun moveUpInList() {
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false)
        val one = getByRemoteId("1")
        googleTaskDao.move(one, 0, 1)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("3")!!.order)
    }

    @Test
    fun moveToTop() {
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false)
        val three = getByRemoteId("3")
        googleTaskDao.move(three, 0, 0)
        assertEquals(0, googleTaskDao.getByRemoteId("3")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("2")!!.order)
    }

    @Test
    fun moveToBottom() {
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false)
        googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false)
        val one = getByRemoteId("1")
        googleTaskDao.move(one, 0, 2)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("3")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("1")!!.order)
    }

    @Test
    fun findChildrenInList() {
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        assertEquals(listOf(2L), googleTaskDao.getChildren(listOf(1L, 2L)))
    }

    private fun insertTop(googleTask: GoogleTask) {
        insert(googleTask, true)
    }

    private fun insertBottom(googleTask: GoogleTask) {
        insert(googleTask, false)
    }

    private fun insert(googleTask: GoogleTask, top: Boolean) {
        val task = newTask()
        taskDao.createNew(task)
        googleTask.task = task.id
        googleTaskDao.insertAndShift(googleTask, top)
    }

    private fun getByRemoteId(remoteId: String): SubsetGoogleTask {
        val googleTask = googleTaskDao.getByRemoteId(remoteId)!!
        val result = SubsetGoogleTask()
        result.gt_id = googleTask.id
        result.gt_list_id = googleTask.listId
        result.gt_order = googleTask.order
        result.gt_parent = googleTask.parent
        return result
    }

    override fun inject(component: TestComponent) = component.inject(this)
}