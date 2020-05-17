package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TaskContainerMaker
import org.tasks.makers.TaskContainerMaker.CREATED
import org.tasks.time.DateTime
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class CaldavDaoShiftTests : InjectingTestCase() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao

    private val tasks = ArrayList<TaskContainer>()

    @Test
    fun basicShiftDown() {
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
    fun shiftDownOnlyWhenNecessary() {
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
    fun ignoreUnnecessaryShiftDown() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(2)))

        caldavDao.shiftDown("calendar", 0, created.plusSeconds(1).toAppleEpoch())

        checkOrder(null, tasks[0])
        checkOrder(null, tasks[1])
    }

    @Test
    fun ignoreOtherCalendarWhenShiftingDown() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask("calendar1", with(CREATED, created))
        addTask("calendar2", with(CREATED, created))

        caldavDao.shiftDown("calendar1", 0, created.toAppleEpoch())

        checkOrder(created.plusSeconds(1), tasks[0])
        checkOrder(null, tasks[1])
    }

    @Test
    fun partialShiftDown() {
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
    fun ignoreMovedTasksWhenShiftingDown() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        caldavDao.update(caldavDao.getTask(tasks[0].id).apply { this?.deleted = now() }!!)

        caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())

        assertNull(caldavDao.getTasks(tasks[0].id)[0].order)
    }

    @Test
    fun ignoreDeletedTasksWhenShiftingDown() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        taskDao.update(taskDao.fetch(tasks[0].id).apply { this?.deletionDate = now() }!!)

        caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())

        assertNull(caldavDao.getTasks(tasks[0].id)[0].order)
    }

    @Test
    fun touchShiftedTasks() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATED, created))
        addTask(with(CREATED, created.plusSeconds(1)))

        freezeAt(created.plusMinutes(1)) {
            caldavDao.shiftDown("calendar", 0, created.toAppleEpoch())
        }

        assertEquals(created.plusMinutes(1).millis, taskDao.fetch(tasks[0].id)!!.modificationDate)
        assertEquals(created.plusMinutes(1).millis, taskDao.fetch(tasks[1].id)!!.modificationDate)
    }

    private fun checkOrder(dateTime: DateTime?, task: TaskContainer) {
        if (dateTime == null) {
            assertNull(caldavDao.getTask(task.id)!!.order)
        } else {
            assertEquals(dateTime.toAppleEpoch(), caldavDao.getTask(task.id)!!.order)
        }
    }

    private fun addTask(vararg properties: PropertyValue<in TaskContainer?, *>) = addTask("calendar", *properties)

    private fun addTask(calendar: String, vararg properties: PropertyValue<in TaskContainer?, *>) {
        val t = TaskContainerMaker.newTaskContainer(*properties)
        tasks.add(t)
        val task = t.task
        taskDao.createNew(task)
        val caldavTask = CaldavTask(t.id, calendar)
        if (task.parent > 0) {
            caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.parent)
        }
        caldavTask.id = caldavDao.insert(caldavTask)
        t.caldavTask = caldavTask.toSubset()
    }

    private fun CaldavTask.toSubset(): SubsetCaldav {
        val result = SubsetCaldav()
        result.cd_id = id
        result.cd_calendar = calendar
        result.cd_remote_parent = remoteParent
        return result
    }

    override fun inject(component: TestComponent) = component.inject(this)
}