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
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.GoogleTaskMaker.LIST
import org.tasks.makers.GoogleTaskMaker.PARENT
import org.tasks.makers.GoogleTaskMaker.REMOTE_ID
import org.tasks.makers.GoogleTaskMaker.REMOTE_PARENT
import org.tasks.makers.GoogleTaskMaker.TASK
import org.tasks.makers.GoogleTaskMaker.newGoogleTask
import org.tasks.makers.GtaskListMaker.newGtaskList
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class GoogleTaskDaoTests : InjectingTestCase() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskDao: TaskDao

    @Before
    override fun setUp() {
        super.setUp()
        runBlocking {
            googleTaskListDao.insert(newGtaskList())
        }
    }

    @Test
    fun insertAtTopOfEmptyList() = runBlocking {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", task.remoteId)
        assertEquals(0, task.order)
    }

    @Test
    fun insertAtBottomOfEmptyList() = runBlocking {
        insertBottom(newGoogleTask(with(REMOTE_ID, "1234")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(1, tasks.size.toLong())
        val task = tasks[0]
        assertEquals("1234", task.remoteId)
        assertEquals(0, task.order)
    }

    @Test
    fun getPreviousIsNullForTopTask() = runBlocking {
        insert(newGoogleTask())
        assertNull(googleTaskDao.getPrevious("1", 0, 0))
    }

    @Test
    fun getPrevious() = runBlocking {
        insertTop(newGoogleTask())
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        assertEquals("1234", googleTaskDao.getPrevious("1", 0, 1))
    }

    @Test
    fun insertAtTopOfList() = runBlocking {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        insertTop(newGoogleTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(2, tasks.size.toLong())
        val top = tasks[0]
        assertEquals("5678", top.remoteId)
        assertEquals(0, top.order)
    }

    @Test
    fun insertAtTopOfListShiftsExisting() = runBlocking {
        insertTop(newGoogleTask(with(REMOTE_ID, "1234")))
        insertTop(newGoogleTask(with(REMOTE_ID, "5678")))
        val tasks = googleTaskDao.getByLocalOrder("1")
        assertEquals(2, tasks.size.toLong())
        val bottom = tasks[1]
        assertEquals("1234", bottom.remoteId)
        assertEquals(1, bottom.order)
    }

    @Test
    fun getTaskFromRemoteId() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1234")))
        assertEquals(1L, googleTaskDao.getTask("1234"))
    }

    @Test
    fun getRemoteIdForTask() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1234")))
        assertEquals("1234", googleTaskDao.getRemoteId(1L))
    }

    @Test
    fun moveDownInList() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1")))
        insert(newGoogleTask(with(REMOTE_ID, "2")))
        insert(newGoogleTask(with(REMOTE_ID, "3")))
        val two = getByRemoteId("2")
        googleTaskDao.move(two, 0, 0)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("3")!!.order)
    }

    @Test
    fun moveUpInList() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1")))
        insert(newGoogleTask(with(REMOTE_ID, "2")))
        insert(newGoogleTask(with(REMOTE_ID, "3")))
        val one = getByRemoteId("1")
        googleTaskDao.move(one, 0, 1)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("3")!!.order)
    }

    @Test
    fun moveToTop() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1")))
        insert(newGoogleTask(with(REMOTE_ID, "2")))
        insert(newGoogleTask(with(REMOTE_ID, "3")))
        val three = getByRemoteId("3")
        googleTaskDao.move(three, 0, 0)
        assertEquals(0, googleTaskDao.getByRemoteId("3")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("1")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("2")!!.order)
    }

    @Test
    fun moveToBottom() = runBlocking {
        insert(newGoogleTask(with(REMOTE_ID, "1")))
        insert(newGoogleTask(with(REMOTE_ID, "2")))
        insert(newGoogleTask(with(REMOTE_ID, "3")))
        val one = getByRemoteId("1")
        googleTaskDao.move(one, 0, 2)
        assertEquals(0, googleTaskDao.getByRemoteId("2")!!.order)
        assertEquals(1, googleTaskDao.getByRemoteId("3")!!.order)
        assertEquals(2, googleTaskDao.getByRemoteId("1")!!.order)
    }

    @Test
    fun findChildrenInList() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        assertEquals(listOf(2L), googleTaskDao.getChildren(listOf(1L, 2L)))
    }

    @Test
    fun dontAllowEmptyParent() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", "", "0")

        assertNull(googleTaskDao.getByTaskId(1)!!.remoteParent)
    }
    
    @Test
    fun updatePositionWithNullParent() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", null, "0")

        assertNull(googleTaskDao.getByTaskId(1)!!.remoteParent)
    }

    @Test
    fun updatePosition() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "1234")))

        googleTaskDao.updatePosition("1234", "abcd", "0")

        assertEquals("abcd", googleTaskDao.getByTaskId(1)!!.remoteParent)
    }

    @Test
    fun updateParents() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "123")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(REMOTE_PARENT, "123")))

        googleTaskDao.updateParents()

        assertEquals(1, googleTaskDao.getByTaskId(2)!!.parent)
    }

    @Test
    fun updateParentsByList() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "123")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(REMOTE_PARENT, "123")))

        googleTaskDao.updateParents("1")

        assertEquals(1, googleTaskDao.getByTaskId(2)!!.parent)
    }

    @Test
    fun updateParentsMustMatchList() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "123")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "2"), with(REMOTE_PARENT, "123")))

        googleTaskDao.updateParents()

        assertEquals(0, googleTaskDao.getByTaskId(2)!!.parent)
    }

    @Test
    fun updateParentsByListMustMatchList() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "123")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "2"), with(REMOTE_PARENT, "123")))

        googleTaskDao.updateParents("2")

        assertEquals(0, googleTaskDao.getByTaskId(2)!!.parent)
    }

    @Test
    fun ignoreEmptyStringWhenUpdatingParents() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(REMOTE_ID, ""), with(REMOTE_PARENT, "")))

        googleTaskDao.updateParents()

        assertEquals(0, googleTaskDao.getByTaskId(2)!!.parent)
    }

    @Test
    fun ignoreEmptyStringWhenUpdatingParentsForList() = runBlocking {
        insert(newGoogleTask(with(TASK, 1), with(LIST, "1"), with(REMOTE_ID, "")))
        insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(REMOTE_ID, ""), with(REMOTE_PARENT, "")))

        googleTaskDao.updateParents("1")

        assertEquals(0, googleTaskDao.getByTaskId(2)!!.parent)
    }

    private suspend fun insertTop(googleTask: GoogleTask) {
        insert(googleTask, true)
    }

    private suspend fun insertBottom(googleTask: GoogleTask) {
        insert(googleTask, false)
    }

    private suspend fun insert(googleTask: GoogleTask, top: Boolean = false) {
        val task = newTask()
        taskDao.createNew(task)
        googleTask.task = task.id
        googleTaskDao.insertAndShift(googleTask, top)
    }

    private suspend fun getByRemoteId(remoteId: String): SubsetGoogleTask {
        val googleTask = googleTaskDao.getByRemoteId(remoteId)!!
        val result = SubsetGoogleTask()
        result.gt_id = googleTask.id
        result.gt_list_id = googleTask.listId
        result.gt_order = googleTask.order
        result.gt_parent = googleTask.parent
        return result
    }
}