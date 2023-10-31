package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.andlib.utility.DateUtilities.now
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskContainerMaker
import org.tasks.makers.TaskContainerMaker.CREATED
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavDaoShiftTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao

    private val tasks = ArrayList<TaskContainer>()

    @Test
    fun basicShiftDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(1)))
        addTask(with(CREATED, created.plusSeconds(2)))

        caldavDao.shiftDown("calendar", 0, created.plusSeconds(1).toAppleEpoch())

        checkOrder(null, tasks[0])
        checkOrder(created.plusSeconds(2), tasks[1])
        checkOrder(created.plusSeconds(3), tasks[2])
    }

    @Test
    fun shiftDownOnlyWhenNecessary() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(1)))
        addTask(with(CREATED, created.plusSeconds(3)))
        addTask(with(CREATED, created.plusSeconds(4)))

        caldavDao.shiftDown("calendar", 0, created.plusSeconds(1).toAppleEpoch())

        checkOrder(null, tasks[0])
        checkOrder(created.plusSeconds(2), tasks[1])
        checkOrder(null, tasks[2])
        checkOrder(null, tasks[3])
    }

    @Test
    fun ignoreUnnecessaryShiftDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(2)))

        caldavDao.shiftDown("calendar", 0, created.plusSeconds(1).toAppleEpoch())

        checkOrder(null, tasks[0])
        checkOrder(null, tasks[1])
    }

    @Test
    fun ignoreOtherCalendarWhenShiftingDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask("calendar1", with(CREATED, created))
        addTask("calendar2", with(CREATED, created))

        caldavDao.shiftDown("calendar1", 0, created.toAppleEpoch())

        checkOrder(created.plusSeconds(1), tasks[0])
        checkOrder(null, tasks[1])
    }

    @Test
    fun partialShiftDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(1)))
        addTask(with(CREATED, created.plusSeconds(2)))
        addTask(with(CREATED, created.plusSeconds(3)))
        addTask(with(CREATED, created.plusSeconds(4)))

        caldavDao.shiftDown("calendar", 0, created.toAppleEpoch(), created.plusSeconds(3).toAppleEpoch())

        checkOrder(created.plusSeconds(1), tasks[0])
        checkOrder(created.plusSeconds(2), tasks[1])
        checkOrder(created.plusSeconds(3), tasks[2])
        checkOrder(null, tasks[3])
        checkOrder(null, tasks[4])
    }

    @Test
    fun ignoreMovedTasksWhenShiftingDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        caldavDao.update(caldavDao.getTask(tasks[0].id).apply { this?.deleted = now() }!!)

        caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())

        assertNull(taskDao.fetch(tasks[0].id)!!.order)
    }

    @Test
    fun ignoreDeletedTasksWhenShiftingDown() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        taskDao.update(taskDao.fetch(tasks[0].id).apply { this?.deletionDate = now() }!!, null)

        caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())

        assertNull(taskDao.fetch(tasks[0].id)!!.order)
    }

    @Test
    fun touchShiftedTasks() = runBlocking {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(1)))

        freezeAt(created.plusMinutes(1)) {
            caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())
        }

        assertEquals(created.plusMinutes(1).millis, taskDao.fetch(tasks[0].id)!!.modificationDate)
        assertEquals(created.plusMinutes(1).millis, taskDao.fetch(tasks[1].id)!!.modificationDate)
    }

    private suspend fun checkOrder(dateTime: DateTime?, task: TaskContainer) {
        val order = taskDao.fetch(task.id)!!.order
        if (dateTime == null) {
            assertNull(order)
        } else {
            assertEquals(dateTime.toAppleEpoch(), order)
        }
    }

    private suspend fun addTask(vararg properties: PropertyValue<in TaskContainer?, *>) = addTask("calendar", *properties)

    private suspend fun addTask(calendar: String, vararg properties: PropertyValue<in TaskContainer?, *>) {
        val t = TaskContainerMaker.newTaskContainer(*properties)
        val task = t.task
        taskDao.createNew(task)
        val caldavTask = CaldavTask(task = t.id, calendar = calendar)
        if (task.parent > 0) {
            caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.parent)
        }
        tasks.add(
            t.copy(
                caldavTask = caldavTask.copy(
                    id = caldavDao.insert(caldavTask)
                )
            )
        )
    }
}
