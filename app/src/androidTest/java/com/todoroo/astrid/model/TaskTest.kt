package com.todoroo.astrid.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.Freeze
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.time.DateTimeUtils
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
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

    override fun inject(component: TestComponent) = component.inject(this)
}