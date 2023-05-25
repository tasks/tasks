package com.todoroo.astrid.adapter

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskMover
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.newTask
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavManualSortTaskAdapterTest : InjectingTestCase() {
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var taskMover: TaskMover

    private lateinit var adapter: CaldavManualSortTaskAdapter
    private val tasks = ArrayList<TaskContainer>()
    private val filter = CaldavFilter(CaldavCalendar(name = "calendar", uuid = "1234"))
    private val dataSource = object : TaskAdapterDataSource {
        override fun getItem(position: Int) = tasks[position]

        override fun getTaskCount() = tasks.size
    }

    @Before
    override fun setUp() {
        super.setUp()
        preferences.clear()
        preferences.setBoolean(R.string.p_manual_sort, true)
        tasks.clear()
        adapter = CaldavManualSortTaskAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
        adapter.setDataSource(dataSource)
    }

    @Test
    fun moveToSamePositionIsNoop() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))

        move(0, 0)

        checkOrder(null, 0)
        checkOrder(null, 1)
    }

    @Test
    fun moveTaskToTopOfList() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))

        move(1, 0)

        checkOrder(created.minusSeconds(1), 1)
        checkOrder(null, 0)
    }

    @Test
    fun moveTaskToBottomOfList() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))

        move(0, 1)

        checkOrder(null, 1)
        checkOrder(created.plusSeconds(2), 0)
    }

    @Test
    fun moveDownToMiddleOfList() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))
        addTask(with(CREATION_TIME, created.plusSeconds(3)))
        addTask(with(CREATION_TIME, created.plusSeconds(4)))

        move(0, 2)

        checkOrder(null, 1)
        checkOrder(null, 2)
        checkOrder(created.plusSeconds(3), 0)
        checkOrder(created.plusSeconds(4), 3)
        checkOrder(created.plusSeconds(5), 4)
    }

    @Test
    fun moveUpToMiddleOfList() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))
        addTask(with(CREATION_TIME, created.plusSeconds(3)))
        addTask(with(CREATION_TIME, created.plusSeconds(4)))

        move(3, 1)

        checkOrder(null, 0)
        checkOrder(created.plusSeconds(1), 3)
        checkOrder(created.plusSeconds(2), 1)
        checkOrder(created.plusSeconds(3), 2)
        checkOrder(null, 4)
    }

    @Test
    fun moveDownNoShiftRequired() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(1)))
        addTask(with(CREATION_TIME, created.plusSeconds(3)))
        addTask(with(CREATION_TIME, created.plusSeconds(4)))

        move(0, 1)

        checkOrder(null, 1)
        checkOrder(created.plusSeconds(2), 0)
        checkOrder(null, 2)
        checkOrder(null, 3)
    }
    
    @Test
    fun moveUpNoShiftRequired() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))
        addTask(with(CREATION_TIME, created.plusSeconds(3)))
        addTask(with(CREATION_TIME, created.plusSeconds(4)))

        move(2, 1)

        checkOrder(null, 0)
        checkOrder(created.plusSeconds(1), 2)
        checkOrder(null, 1)
        checkOrder(null, 3)
    }

    @Test
    fun moveToNewSubtask() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))

        move(1, 1, 1)

        checkOrder(null, 0)
        checkOrder(null, 1)
    }

    @Test
    fun moveToTopOfExistingSubtasks() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        val parent = addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(5)), with(PARENT, parent))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))

        move(2, 1, 1)

        checkOrder(null, 0)
        checkOrder(created.plusSeconds(4), 2)
        checkOrder(null, 1)
    }

    @Test
    fun indentingChangesParent() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        addTask(with(CREATION_TIME, created))
        addTask(with(CREATION_TIME, created.plusSeconds(2)))

        move(1, 1, 1)

        assertEquals(tasks[0].id, tasks[1].parent)
    }

    @Test
    fun deindentLastMultiLevelSubtask() {
        val created = DateTime(2020, 5, 17, 9, 53, 17)
        val grandparent = addTask(with(CREATION_TIME, created))
        val parent = addTask(with(CREATION_TIME, created.plusSeconds(5)), with(PARENT, grandparent))
        addTask(with(CREATION_TIME, created.plusSeconds(1)), with(PARENT, parent))
        addTask(with(CREATION_TIME, created.plusSeconds(2)), with(PARENT, parent))

        move(3, 3, 1)

        assertEquals(grandparent, tasks[3].parent)
        checkOrder(created.plusSeconds(6), 3)
    }

    private fun move(from: Int, to: Int, indent: Int = 0) = runBlocking {
        tasks.addAll(taskDao.fetchTasks { getQuery(preferences, filter) })
        val adjustedTo = if (from < to) to + 1 else to // match DragAndDropRecyclerAdapter behavior
        adapter.moved(from, adjustedTo, indent)
    }

    private fun checkOrder(dateTime: DateTime, index: Int) = checkOrder(dateTime.toAppleEpoch(), index)

    private fun checkOrder(order: Long?, index: Int) = runBlocking {
        val sortOrder = taskDao.fetch(adapter.getTask(index).id)!!.order
        if (order == null) {
            assertNull(sortOrder)
        } else {
            assertEquals(order, sortOrder)
        }
    }

    private fun addTask(vararg properties: PropertyValue<in Task?, *>): Long = runBlocking {
        val task = newTask(*properties)
        taskDao.createNew(task)
        val remoteParent = if (task.parent > 0) caldavDao.getRemoteIdForTask(task.parent) else null
        caldavDao.insert(
                newCaldavTask(
                        with(TASK, task.id),
                        with(CALENDAR, "1234"),
                        with(REMOTE_PARENT, remoteParent)))
        task.id
    }
}