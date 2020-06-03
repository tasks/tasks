package com.todoroo.astrid.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.jobs.WorkManager
import org.tasks.makers.CaldavTaskMaker
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.GoogleTaskMaker.LIST
import org.tasks.makers.GoogleTaskMaker.PARENT
import org.tasks.makers.GoogleTaskMaker.TASK
import org.tasks.makers.GoogleTaskMaker.newGoogleTask
import org.tasks.makers.GtaskListMaker
import org.tasks.makers.GtaskListMaker.newGtaskList
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class TaskMoverTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskMover: TaskMover

    @Before
    override fun setUp() {
        super.setUp()
        taskDao.initialize(workManager)
    }

    @Test
    fun moveBetweenGoogleTaskLists() {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1)!!.listId)
    }

    @Test
    fun deleteGoogleTaskAfterMove() {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("2", 1)
        val deleted = googleTaskDao.getDeletedByTaskId(1)
        assertEquals(1, deleted.size.toLong())
        assertEquals(1, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
    }

    @Test
    fun moveChildrenBetweenGoogleTaskLists() {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToGoogleTasks("2", 1)
        val deleted = googleTaskDao.getDeletedByTaskId(2)
        assertEquals(1, deleted.size.toLong())
        assertEquals(2, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
        val task = googleTaskDao.getByTaskId(2)!!
        assertEquals(1, task.parent)
        assertEquals("2", task.listId)
    }

    @Test
    fun moveBetweenCaldavList() {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun deleteCaldavTaskAfterMove() {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        val deleted = caldavDao.getDeleted("1")
        assertEquals(1, deleted.size.toLong())
        assertEquals(1, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
    }

    @Test
    fun moveRecursiveCaldavChildren() {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "b"))))
        moveToCaldavList("2", 1)
        val deleted = caldavDao.getDeleted("1")
        assertEquals(3, deleted.size.toLong())
        val task = caldavDao.getTask(3)
        assertEquals("2", task!!.calendar)
        assertEquals(2, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveGoogleTaskChildrenToCaldav() {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToCaldavList("1", 1)
        val task = caldavDao.getTask(2)
        assertEquals("1", task!!.calendar)
        assertEquals(1, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun flattenLocalSubtasksWhenMovingToGoogleTasks() {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToGoogleTasks("1", 1)
        assertEquals(1, googleTaskDao.getByTaskId(3)!!.parent)
        assertEquals(0, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveLocalChildToGoogleTasks() {
        createTasks(1)
        createSubtask(2, 1)
        moveToGoogleTasks("1", 2)
        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun moveLocalChildToCaldav() {
        createTasks(1)
        createSubtask(2, 1)
        moveToCaldavList("1", 2)
        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun flattenCaldavSubtasksWhenMovingToGoogleTasks() {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_ID, "b"),
                                with(REMOTE_PARENT, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 3L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "b"))))
        moveToGoogleTasks("1", 1)
        val task = googleTaskDao.getByTaskId(3L)!!
        assertEquals(1, task.parent)
    }

    @Test
    fun moveGoogleTaskChildWithoutParent() {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToGoogleTasks("2", 2)
        val task = googleTaskDao.getByTaskId(2)!!
        assertEquals(0L, task.parent)
        assertEquals("2", task.listId)
    }

    @Test
    fun moveCaldavChildWithoutParent() {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "a"))))
        moveToCaldavList("2", 2)
        assertEquals("2", caldavDao.getTask(2)!!.calendar)
        assertEquals(0, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun moveGoogleTaskToCaldav() {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun moveCaldavToGoogleTask() {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1L)!!.listId)
    }

    @Test
    fun moveLocalToCaldav() {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToCaldavList("1", 1)
        assertEquals("1", caldavDao.getTask(3)!!.calendar)
        assertEquals(2, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveToSameGoogleTaskListIsNoop() {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("1", 1)
        assertTrue(googleTaskDao.getDeletedByTaskId(1).isEmpty())
        assertEquals(1, googleTaskDao.getAllByTaskId(1).size.toLong())
    }

    @Test
    fun moveToSameCaldavListIsNoop() {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("1", 1)
        assertTrue(caldavDao.getDeleted("1").isEmpty())
        assertEquals(1, caldavDao.getTasks(1).size.toLong())
    }

    @Test
    fun dontDuplicateWhenParentAndChildGoogleTaskMoved() {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToGoogleTasks("2", 1, 2)
        assertEquals(1, googleTaskDao.getAllByTaskId(2).filter { it.deleted == 0L }.size)
    }

    @Test
    fun dontDuplicateWhenParentAndChildCaldavMoved() {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(
                listOf(
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
                        newCaldavTask(
                                with(CaldavTaskMaker.TASK, 2L),
                                with(CALENDAR, "1"),
                                with(REMOTE_PARENT, "a"))))
        moveToCaldavList("2", 1, 2)
        assertEquals(1, caldavDao.getTasks(2).filter { it.deleted == 0L }.size)
    }

    private fun createTasks(vararg ids: Long) {
        for (id in ids) {
            taskDao.createNew(newTask(with(ID, id)))
        }
    }

    private fun createSubtask(id: Long, parent: Long) {
        taskDao.createNew(newTask(with(ID, id), with(TaskMaker.PARENT, parent)))
    }

    private fun moveToGoogleTasks(list: String, vararg tasks: Long) {
        taskMover.move(tasks.toList(), GtasksFilter(newGtaskList(with(GtaskListMaker.REMOTE_ID, list))))
    }

    private fun moveToCaldavList(calendar: String, vararg tasks: Long) {
        taskMover.move(tasks.toList(), CaldavFilter(CaldavCalendar("", calendar)))
    }

    override fun inject(component: TestComponent) = component.inject(this)
}