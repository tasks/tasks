package com.todoroo.astrid.repeats

import org.tasks.data.entity.Task
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.TaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class RepeatWithSubtasksTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskCompleter: TaskCompleter

    @Test
    fun uncompleteGrandchildren() = runBlocking {
        val grandparent = taskDao.createNew(
            Task(
                recurrence = "RRULE:FREQ=DAILY"
            )
        )
        val parent = taskDao.createNew(
            Task(
                parent = grandparent
            )
        )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        assertTrue(taskDao.fetch(child)!!.isCompleted)

        taskCompleter.setComplete(grandparent)

        assertFalse(taskDao.fetch(child)!!.isCompleted)
    }

    @Test
    fun uncompleteGoogleTaskChildren() = runBlocking {
        val parent = taskDao.createNew(
                Task(
                    recurrence = "RRULE:FREQ=DAILY"
                )
            )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        assertTrue(taskDao.fetch(child)!!.isCompleted)

        taskCompleter.setComplete(parent)

        assertFalse(taskDao.fetch(child)!!.isCompleted)
    }
}