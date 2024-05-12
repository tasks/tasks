package com.todoroo.astrid.adapter

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskMover
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.data.*
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskContainerMaker.PARENT
import org.tasks.makers.TaskContainerMaker.newTaskContainer
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavTaskAdapterTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var taskMover: TaskMover

    private lateinit var adapter: TaskAdapter
    private val tasks = ArrayList<TaskContainer>()

    @Before
    override fun setUp() {
        super.setUp()

        tasks.clear()
        adapter = TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
        adapter.setDataSource(object : TaskAdapterDataSource {
            override fun getItem(position: Int) = tasks[position]

            override fun getTaskCount() = tasks.size
        })
    }

    @Test
    fun canMoveTask() {
        addTask()
        addTask()

        assertTrue(adapter.canMove(tasks[0], 0, tasks[1], 1))
    }

    @Test
    fun cantMoveTaskToChildPosition() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[0]))

        assertFalse(adapter.canMove(tasks[0], 0, tasks[1], 1))
        assertFalse(adapter.canMove(tasks[0], 0, tasks[2], 2))
    }

    @Test
    fun canMoveChildAboveParent() {
        addTask()
        addTask(with(PARENT, tasks[0]))

        assertTrue(adapter.canMove(tasks[1], 1, tasks[0], 0))
    }

    @Test
    fun canMoveChildBetweenSiblings() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[0]))

        assertTrue(adapter.canMove(tasks[1], 1, tasks[2], 2))
        assertTrue(adapter.canMove(tasks[2], 2, tasks[1], 1))
    }

    @Test
    fun maxIndentNoChildren() {
        addTask()
        addTask()

        assertEquals(1, adapter.maxIndent(0, tasks[1]))
    }

    @Test
    fun maxIndentMultiLevelSubtask() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask()

        assertEquals(1, adapter.maxIndent(0, tasks[1]))
        assertEquals(2, adapter.maxIndent(1, tasks[2]))
    }

    @Test
    fun minIndentInMiddleOfSubtasks() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[0]))

        assertEquals(1, adapter.minIndent(2, tasks[1]))
    }

    @Test
    fun minIndentAtEndOfSubtasks() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[0]))
        addTask()

        assertEquals(0, adapter.minIndent(3, tasks[2]))
    }

    @Test
    fun minIndentAtEndOfMultiLevelSubtask() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))
        addTask()

        assertEquals(0, adapter.minIndent(2, tasks[1]))
    }
    
    @Test
    fun minIndentInMiddleOfMultiLevelSubtasks() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))
        addTask(with(PARENT, tasks[0]))
        addTask()

        assertEquals(1, adapter.minIndent(3, tasks[2]))
    }

    @Test
    fun movingTaskToNewParentSetsId() = runBlocking {
        addTask()
        addTask()

        adapter.moved(1, 1, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun movingTaskToNewParentSetsRemoteId() = runBlocking {
        addTask()
        addTask()

        adapter.moved(1, 1, 1)

        val parentId = caldavDao.getTask(tasks[0].id)!!.remoteId!!

        assertTrue(parentId.isNotBlank())
        assertEquals(parentId, caldavDao.getTask(tasks[1].id)!!.remoteParent)
    }

    @Test
    fun unindentingTaskRemovesParent() = runBlocking {
        addTask()
        addTask(with(PARENT, tasks[0]))

        adapter.moved(1, 1, 0)

        assertTrue(caldavDao.getTask(tasks[1].id)!!.remoteParent.isNullOrBlank())
        assertEquals(0, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToParent() = runBlocking {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))

        adapter.moved(2, 2, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[2].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToGrandparent() = runBlocking {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))
        addTask(with(PARENT, tasks[2]))

        adapter.moved(3, 3, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[3].id)!!.parent)
    }

    private fun addTask(vararg properties: PropertyValue<in TaskContainer?, *>) = runBlocking {
        val t = newTaskContainer(*properties)
        val task = t.task
        taskDao.createNew(task)
        val caldavTask = CaldavTask(task = t.id, calendar = "calendar")
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
