package com.todoroo.astrid.adapter

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.newTask
import org.tasks.preferences.Preferences
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class RecursiveLoopTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences

    @Before
    override fun setUp() {
        super.setUp()
        preferences.clear()
    }

    @Test
    @Ignore("infinite loop")
    fun handleSelfLoop() = runBlocking {
        addTask(with(DUE_DATE, newDateTime()), with(PARENT, 1L))

        val tasks = getTasks()

        assertEquals(1, tasks.size)
        assertEquals(1L, tasks[0].id)
    }

    @Test
    @Ignore("infinite loop")
    fun handleSingleLevelLoop() = runBlocking {
        val parent = addTask(with(DUE_DATE, newDateTime()))
        val child = addTask(with(PARENT, parent))

        taskDao.setParent(child, listOf(parent))

        val tasks = getTasks()
        assertEquals(2, tasks.size)
        assertEquals(parent, tasks[0].id)
        assertEquals(child, tasks[1].id)
    }

    @Test
    @Ignore("infinite loop")
    fun handleMultiLevelLoop() = runBlocking {
        val parent = addTask(with(DUE_DATE, newDateTime()))
        val child = addTask(with(PARENT, parent))
        val grandchild = addTask(with(PARENT, child))

        taskDao.setParent(grandchild, listOf(parent))

        val tasks = getTasks()
        assertEquals(3, tasks.size)
        assertEquals(parent, tasks[0].id)
        assertEquals(child, tasks[1].id)
        assertEquals(grandchild, tasks[2].id)
    }

    private suspend fun getTasks() = taskDao.fetchTasks {
        getQuery(preferences, BuiltInFilterExposer.getTodayFilter(context.resources))
    }

    private suspend fun addTask(vararg properties: PropertyValue<in Task?, *>): Long {
        val task = newTask(*properties)
        taskDao.createNew(task)
        return task.id
    }
}