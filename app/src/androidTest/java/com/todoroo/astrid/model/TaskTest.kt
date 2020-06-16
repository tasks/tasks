package com.todoroo.astrid.model

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.Freeze
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.time.DateTimeUtils
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao

    @Test
    fun testSavedTaskHasCreationDate() {
        Freeze.freezeClock {
            val task = Task()
            taskDao.createNew(task)
            assertEquals(DateTimeUtils.currentTimeMillis(), task.creationDate)
        }
    }

    @Test
    fun testReadTaskFromDb() {
        val task = Task()
        taskDao.createNew(task)
        val fromDb = taskDao.fetch(task.id)
        assertEquals(task, fromDb)
    }
}