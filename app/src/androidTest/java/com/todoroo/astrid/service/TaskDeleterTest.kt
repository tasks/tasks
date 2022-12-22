package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.core.BuiltInFilterExposer.Companion.getMyTasksFilter
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskDeleterTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var googleTaskDao: GoogleTaskDao

    @Test
    fun clearCompletedTask() = runBlocking {
        val task = taskDao.createNew(newTask(with(COMPLETION_TIME, DateTime())))

        clearCompleted()

        assertTrue(taskDao.fetch(task)!!.isDeleted)
    }

    @Test
    fun dontDeleteTaskWithRecurringParent() = runBlocking {
        val parent = taskDao.createNew(newTask(with(RECUR, "RRULE:FREQ=DAILY;INTERVAL=1")))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        clearCompleted()

        assertFalse(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun dontDeleteTaskWithRecurringGrandparent() = runBlocking {
        val grandparent = taskDao.createNew(newTask(with(RECUR, "RRULE:FREQ=DAILY;INTERVAL=1")))
        val parent = taskDao.createNew(newTask(with(PARENT, grandparent)))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        clearCompleted()

        assertFalse(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun clearGrandchildWithNoRecurringAncestors() = runBlocking {
        val grandparent = taskDao.createNew(newTask())
        val parent = taskDao.createNew(newTask(with(PARENT, grandparent)))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        clearCompleted()

        assertTrue(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun clearGrandchildWithCompletedRecurringAncestor() = runBlocking {
        val grandparent = taskDao.createNew(newTask(
                with(RECUR, "RRULE:FREQ=DAILY;INTERVAL=1"),
                with(COMPLETION_TIME, DateTime())
        ))
        val parent = taskDao.createNew(newTask(with(PARENT, grandparent)))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        clearCompleted()

        assertTrue(taskDao.fetch(child)!!.isDeleted)
    }

    private suspend fun clearCompleted() =
            taskDeleter.clearCompleted(getMyTasksFilter(context.resources))
}