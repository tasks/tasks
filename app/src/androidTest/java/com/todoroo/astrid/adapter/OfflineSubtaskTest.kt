package com.todoroo.astrid.adapter

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.entity.Task
import org.tasks.filters.MyTasksFilter
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.newTask
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltAndroidTest
class OfflineSubtaskTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences

    private val filter = runBlocking { MyTasksFilter.create() }

    @Before
    override fun setUp() {
        super.setUp()
        preferences.clear()
    }

    @Test
    fun singleLevelSubtask() {
        val parent = addTask()
        val child = addTask(with(PARENT, parent))

        val tasks = query()

        assertEquals(child, tasks[1].id)
        assertEquals(parent, tasks[1].parent)
        assertEquals(1, tasks[1].indent)
    }

    @Test
    fun multiLevelSubtasks() {
        val grandparent = addTask()
        val parent = addTask(with(PARENT, grandparent))
        val child = addTask(with(PARENT, parent))

        val tasks = query()

        assertEquals(child, tasks[2].id)
        assertEquals(parent, tasks[2].parent)
        assertEquals(2, tasks[2].indent)
    }

    @Test
    fun parentWithOneChildHasChildrenCountOne() {
        val parent = addTask()
        addTask(with(PARENT, parent))

        val tasks = query()

        val parentTask = tasks.find { it.id == parent }!!
        assertEquals(1, parentTask.children)
    }

    @Test
    fun parentWithMultipleChildrenHasCorrectCount() {
        val parent = addTask()
        addTask(with(PARENT, parent))
        addTask(with(PARENT, parent))
        addTask(with(PARENT, parent))

        val tasks = query()

        val parentTask = tasks.find { it.id == parent }!!
        assertEquals(3, parentTask.children)
    }

    @Test
    fun grandparentCountsAllDescendants() {
        val grandparent = addTask()
        val parent = addTask(with(PARENT, grandparent))
        addTask(with(PARENT, parent))

        val tasks = query()

        val grandparentTask = tasks.find { it.id == grandparent }!!
        assertEquals(2, grandparentTask.children)
    }

    @Test
    fun leafTaskHasNoChildren() {
        val parent = addTask()
        val child = addTask(with(PARENT, parent))

        val tasks = query()

        val childTask = tasks.find { it.id == child }!!
        assertEquals(0, childTask.children)
    }

    @Test
    fun deepHierarchyCountsAllDescendants() {
        val root = addTask()
        val level1 = addTask(with(PARENT, root))
        val level2 = addTask(with(PARENT, level1))
        val level3 = addTask(with(PARENT, level2))
        addTask(with(PARENT, level3))

        val tasks = query()

        val rootTask = tasks.find { it.id == root }!!
        assertEquals(4, rootTask.children)
    }

    private fun addTask(vararg properties: PropertyValue<in Task?, *>): Long = runBlocking {
        val task = newTask(*properties)
        taskDao.createNew(task)
        task.id
    }

    private fun query(): List<TaskContainer> = runBlocking {
        taskDao.fetchTasks(getQuery(preferences, filter))
    }
}
