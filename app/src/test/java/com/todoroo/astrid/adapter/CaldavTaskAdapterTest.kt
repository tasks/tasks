package com.todoroo.astrid.adapter

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.tasks.data.CaldavDao
import org.tasks.data.TaskContainer
import org.tasks.makers.TaskContainerMaker.ID
import org.tasks.makers.TaskContainerMaker.PARENT
import org.tasks.makers.TaskContainerMaker.newTaskContainer

class CaldavTaskAdapterTest {

    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var adapter: CaldavTaskAdapter
    private val tasks = ArrayList<TaskContainer>()

    @Before
    fun setup() {
        taskDao = mock(TaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        tasks.clear()
        adapter = CaldavTaskAdapter(taskDao, caldavDao)
        adapter.setDataSource(object : TaskAdapterDataSource {
            override fun getItem(position: Int) = tasks[position]

            override val itemCount get() = tasks.size
        })
    }

    @After
    fun teardown() = verifyNoMoreInteractions(taskDao, caldavDao)

    @Test
    fun canMoveTask() {
        add(
                newTaskContainer(),
                newTaskContainer())

        assertTrue(adapter.canMove(tasks[0], 0, tasks[1], 1))
    }

    @Test
    fun cantMoveTaskToChildPosition() {
        add(newTaskContainer(with(ID, 1L)))
        add(newTaskContainer(with(PARENT, tasks[0])),
                newTaskContainer(with(PARENT, tasks[0])))

        assertFalse(adapter.canMove(tasks[0], 0, tasks[1], 1))
        assertFalse(adapter.canMove(tasks[0], 0, tasks[2], 2))
    }

    @Test
    fun canMoveChildAboveParent() {
        add(newTaskContainer(with(ID, 1L)))
        add(newTaskContainer(with(PARENT, tasks[0])))

        assertTrue(adapter.canMove(tasks[1], 1, tasks[0], 0))
    }

    @Test
    fun canMoveChildBetweenSiblings() {
        add(newTaskContainer(with(ID, 1L)))
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
        add(newTaskContainer(with(ID, 1L)))
        add(newTaskContainer(with(PARENT, tasks[0])),
                newTaskContainer())

        assertEquals(2, adapter.maxIndent(1, tasks[2]))
    }

    private fun add(vararg tasks: TaskContainer) = this.tasks.addAll(tasks.toList())
}