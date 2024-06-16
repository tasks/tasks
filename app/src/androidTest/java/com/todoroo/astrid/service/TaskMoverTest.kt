package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.filters.CaldavFilter
import org.tasks.filters.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.jobs.WorkManager
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.PARENT
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
        val deleted = googleTaskDao.getDeletedByTaskId(1)
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
        val deleted = googleTaskDao.getDeletedByTaskId(2)
        assertEquals(1, deleted.size.toLong())
        assertEquals(2, deleted[0].task)
        assertTrue(deleted[0].deleted > 0)
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
    fun moveGoogleTaskChildrenToCaldav() = runBlocking {
        setAccountType("account1", TYPE_GOOGLE_TASKS)
        setAccountType("account2", TYPE_CALDAV)
        createTasks(1)
        createSubtask(2, 1)
        googleTaskDao.insert(newCaldavTask(with(TASK, 1), with(CALENDAR, "1")))
        googleTaskDao.insert(newCaldavTask(with(TASK, 2), with(CALENDAR, "1")))
        moveToCaldavList("1", 1)
        val task = caldavDao.getTask(2)
        assertEquals("1", task!!.calendar)
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
        moveToGoogleTasks("1", 1)
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
        assertTrue(googleTaskDao.getDeletedByTaskId(1).isEmpty())
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
        taskMover.move(tasks.toList(), GtasksFilter(CaldavCalendar(uuid = list)))
    }

    private suspend fun moveToCaldavList(calendar: String, vararg tasks: Long) {
        taskMover.move(tasks.toList(), CaldavFilter(CaldavCalendar(name = "", uuid = calendar)))
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