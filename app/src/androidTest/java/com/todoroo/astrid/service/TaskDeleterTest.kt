package com.todoroo.astrid.service

import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.TaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskDeleterTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter

    @Test
    fun markTaskAsDeleted() = runBlocking {
        val task = Task()
        taskDao.createNew(task)

        taskDeleter.markDeleted(task)

        assertTrue(taskDao.fetch(task.id)!!.isDeleted)
    }

    @Test
    fun dontDeleteReadOnlyTasks() = runBlocking {
        val task = Task(
            readOnly = true
        )
        taskDao.createNew(task)

        taskDeleter.markDeleted(task)

        assertFalse(taskDao.fetch(task.id)!!.isDeleted)
    }
}
