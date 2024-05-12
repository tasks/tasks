package com.todoroo.astrid.model

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeClock
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao

    @Test
    fun testSavedTaskHasCreationDate() = runBlocking {
        freezeClock {
            val task = Task()
            taskDao.createNew(task)
            assertEquals(currentTimeMillis(), task.creationDate)
        }
    }

    @Test
    fun testReadTaskFromDb() = runBlocking {
        val task = Task()
        taskDao.createNew(task)
        val fromDb = taskDao.fetch(task.id)
        assertEquals(task, fromDb)
    }
}