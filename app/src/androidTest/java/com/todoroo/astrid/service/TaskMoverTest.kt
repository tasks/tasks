package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.jobs.WorkManager
import org.tasks.makers.CaldavCalendarMaker.UUID
import org.tasks.makers.CaldavCalendarMaker.newCaldavCalendar
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

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskMoverTest : InjectingTestCase() {
    @Inject lateinit var taskDaoAsync: TaskDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskMover: TaskMover

    @Test
    fun moveBetweenGoogleTaskLists() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1)!!.listId)
    }

    @Test
    fun deleteGoogleTaskAfterMove() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("2", 1)
        val deleted = googleTaskDao.getDeletedByTaskId(1)
        assertEquals(1, deleted.size.toLong())
        assertEquals(1, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
    }

    @Test
    fun moveChildrenBetweenGoogleTaskLists() = runBlocking {
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
    fun moveBetweenCaldavList() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavCalendar(with(UUID, "1")))
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun deleteCaldavTaskAfterMove() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavCalendar(with(UUID, "1")))
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
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
        caldavDao.insert(newCaldavCalendar(with(UUID, "1")))
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
        val deleted = caldavDao.getMoved("1")
        assertEquals(3, deleted.size.toLong())
        val task = caldavDao.getTask(3)
        assertEquals("2", task!!.calendar)
        assertEquals(2, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveGoogleTaskChildrenToCaldav() = runBlocking {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToCaldavList("1", 1)
        val task = caldavDao.getTask(2)
        assertEquals("1", task!!.calendar)
        assertEquals(1, taskDao.fetch(2)!!.parent)
    }

    @Test
    fun flattenLocalSubtasksWhenMovingToGoogleTasks() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToGoogleTasks("1", 1)
        assertEquals(1, googleTaskDao.getByTaskId(3)!!.parent)
        assertEquals(0, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveLocalChildToGoogleTasks() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        moveToGoogleTasks("1", 2)
        assertEquals(0, taskDao.fetch(2)!!.parent)
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
    fun moveGoogleTaskChildWithoutParent() = runBlocking {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToGoogleTasks("2", 2)
        val task = googleTaskDao.getByTaskId(2)!!
        assertEquals(0L, task.parent)
        assertEquals("2", task.listId)
    }

    @Test
    fun moveCaldavChildWithoutParent() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(newCaldavCalendar(with(UUID, "1")))
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
    fun moveGoogleTaskToCaldav() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToCaldavList("2", 1)
        assertEquals("2", caldavDao.getTask(1)!!.calendar)
    }

    @Test
    fun moveCaldavToGoogleTask() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToGoogleTasks("2", 1)
        assertEquals("2", googleTaskDao.getByTaskId(1L)!!.listId)
    }

    @Test
    fun moveLocalToCaldav() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        createSubtask(3, 2)
        moveToCaldavList("1", 1)
        assertEquals("1", caldavDao.getTask(3)!!.calendar)
        assertEquals(2, taskDao.fetch(3)!!.parent)
    }

    @Test
    fun moveToSameGoogleTaskListIsNoop() = runBlocking {
        createTasks(1)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        moveToGoogleTasks("1", 1)
        assertTrue(googleTaskDao.getDeletedByTaskId(1).isEmpty())
        assertEquals(1, googleTaskDao.getAllByTaskId(1).size.toLong())
    }

    @Test
    fun moveToSameCaldavListIsNoop() = runBlocking {
        createTasks(1)
        caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")))
        moveToCaldavList("1", 1)
        assertTrue(caldavDao.getMoved("1").isEmpty())
        assertEquals(1, caldavDao.getTasks(1).size.toLong())
    }

    @Test
    fun dontDuplicateWhenParentAndChildGoogleTaskMoved() = runBlocking {
        createTasks(1, 2)
        googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")))
        googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)))
        moveToGoogleTasks("2", 1, 2)
        assertEquals(1, googleTaskDao.getAllByTaskId(2).filter { it.deleted == 0L }.size)
    }

    @Test
    fun dontDuplicateWhenParentAndChildCaldavMoved() = runBlocking {
        createTasks(1)
        createSubtask(2, 1)
        caldavDao.insert(newCaldavCalendar(with(UUID, "1")))
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

    private suspend fun createTasks(vararg ids: Long) {
        for (id in ids) {
            taskDao.createNew(newTask(with(ID, id)))
        }
    }

    private suspend fun createSubtask(id: Long, parent: Long) {
        taskDao.createNew(newTask(with(ID, id), with(TaskMaker.PARENT, parent)))
    }

    private suspend fun moveToGoogleTasks(list: String, vararg tasks: Long) {
        taskMover.move(tasks.toList(), GtasksFilter(newGtaskList(with(GtaskListMaker.REMOTE_ID, list))))
    }

    private suspend fun moveToCaldavList(calendar: String, vararg tasks: Long) {
        taskMover.move(tasks.toList(), CaldavFilter(CaldavCalendar(name = "", uuid = calendar)))
    }
}