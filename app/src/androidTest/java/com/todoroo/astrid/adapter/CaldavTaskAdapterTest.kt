package com.todoroo.astrid.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.LocalBroadcastManager
import org.tasks.data.*
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TaskContainerMaker.PARENT
import org.tasks.makers.TaskContainerMaker.newTaskContainer
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class CaldavTaskAdapterTest : InjectingTestCase() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var adapter: TaskAdapter
    private val tasks = ArrayList<TaskContainer>()

    @Before
    override fun setUp() {
        super.setUp()

        tasks.clear()
        adapter = TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager)
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
    fun movingTaskToNewParentSetsId() {
        addTask()
        addTask()

        adapter.moved(1, 1, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun movingTaskToNewParentSetsRemoteId() {
        addTask()
        addTask()

        adapter.moved(1, 1, 1)

        val parentId = caldavDao.getTask(tasks[0].id)!!.remoteId!!

        assertTrue(parentId.isNotBlank())
        assertEquals(parentId, caldavDao.getTask(tasks[1].id)!!.remoteParent)
    }

    @Test
    fun unindentingTaskRemovesParent() {
        addTask()
        addTask(with(PARENT, tasks[0]))

        adapter.moved(1, 1, 0)

        assertTrue(caldavDao.getTask(tasks[1].id)!!.remoteParent.isNullOrBlank())
        assertEquals(0, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToParent() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))

        adapter.moved(2, 2, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[2].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToGrandparent() {
        addTask()
        addTask(with(PARENT, tasks[0]))
        addTask(with(PARENT, tasks[1]))
        addTask(with(PARENT, tasks[2]))

        adapter.moved(3, 3, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[3].id)!!.parent)
    }

    private fun addTask(vararg properties: PropertyValue<in TaskContainer?, *>) {
        val t = newTaskContainer(*properties)
        tasks.add(t)
        val task = t.task
        taskDao.createNew(task)
        val caldavTask = CaldavTask(t.id, "calendar")
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