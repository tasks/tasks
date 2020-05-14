package com.todoroo.astrid.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.SubsetCaldav
import org.tasks.data.TaskContainer
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TaskContainerMaker.PARENT
import org.tasks.makers.TaskContainerMaker.newTaskContainer
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class CaldavTaskAdapterTest : InjectingTestCase() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao

    private lateinit var adapter: CaldavTaskAdapter
    private val tasks = ArrayList<TaskContainer>()

    @Before
    override fun setUp() {
        super.setUp()

        tasks.clear()
        adapter = CaldavTaskAdapter(taskDao, caldavDao)
        adapter.setDataSource(object : TaskAdapterDataSource {
            override fun getItem(position: Int) = tasks[position]

            override val itemCount get() = tasks.size
        })
    }

    @Test
    fun canMoveTask() {
        add(
                newTaskContainer(),
                newTaskContainer())

        assertTrue(adapter.canMove(tasks[0], 0, tasks[1], 1))
    }

    @Test
    fun cantMoveTaskToChildPosition() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])),
                newTaskContainer(with(PARENT, tasks[0])))

        assertFalse(adapter.canMove(tasks[0], 0, tasks[1], 1))
        assertFalse(adapter.canMove(tasks[0], 0, tasks[2], 2))
    }

    @Test
    fun canMoveChildAboveParent() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])))

        assertTrue(adapter.canMove(tasks[1], 1, tasks[0], 0))
    }

    @Test
    fun canMoveChildBetweenSiblings() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])),
                newTaskContainer(with(PARENT, tasks[0])))

        assertTrue(adapter.canMove(tasks[1], 1, tasks[2], 2))
        assertTrue(adapter.canMove(tasks[2], 2, tasks[1], 1))
    }

    @Test
    fun maxIndentNoChildren() {
        add(newTaskContainer(),
                newTaskContainer())

        assertEquals(1, adapter.maxIndent(0, tasks[1]))
    }

    @Test
    fun maxIndentMultiLevelSubtask() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])),
                newTaskContainer())

        assertEquals(2, adapter.maxIndent(1, tasks[2]))
    }

    @Test
    fun movingTaskToNewParentSetsId() {
        add(newTaskContainer(),
                newTaskContainer())

        adapter.moved(1, 1, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun movingTaskToNewParentSetsRemoteId() {
        add(newTaskContainer(),
                newTaskContainer())

        adapter.moved(1, 1, 1)

        val parentId = caldavDao.getTask(tasks[0].id)!!.remoteId!!

        assertTrue(parentId.isNotBlank())
        assertEquals(parentId, caldavDao.getTask(tasks[1].id)!!.remoteParent)
    }

    @Test
    fun unindentingTaskRemovesParent() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])))

        adapter.moved(1, 1, 0)

        assertTrue(caldavDao.getTask(tasks[1].id)!!.remoteParent.isNullOrBlank())
        assertEquals(0, taskDao.fetch(tasks[1].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToParent() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])))
        add(newTaskContainer(with(PARENT, tasks[1])))

        adapter.moved(2, 2, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[2].id)!!.parent)
    }

    @Test
    fun moveSubtaskUpToGrandparent() {
        add(newTaskContainer())
        add(newTaskContainer(with(PARENT, tasks[0])))
        add(newTaskContainer(with(PARENT, tasks[1])))
        add(newTaskContainer(with(PARENT, tasks[2])))

        adapter.moved(3, 3, 1)

        assertEquals(tasks[0].id, taskDao.fetch(tasks[3].id)!!.parent)
    }

    private fun add(vararg tasks: TaskContainer) {
        this.tasks.addAll(tasks)
        tasks.forEach {
            val task = it.task
            taskDao.createNew(task)
            val caldavTask = CaldavTask(it.id, "calendar")
            if (task.parent > 0) {
                caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.parent)
            }
            caldavTask.id = caldavDao.insert(caldavTask)
            it.caldavTask = caldavTask.toSubset()
        }
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