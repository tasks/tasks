package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
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
class RepeatWithSubtasksTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var repeat: RepeatTaskHelper

    @Test
    fun uncompleteGrandchildren() = runBlocking {
        val grandparent = taskDao.createNew(newTask(with(RECUR, "RRULE:FREQ=DAILY")))
        val parent = taskDao.createNew(newTask(with(PARENT, grandparent)))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        repeat.handleRepeat(taskDao.fetch(grandparent)!!)

        assertFalse(taskDao.fetch(child)!!.isCompleted)
    }

    @Test
    fun uncompleteGoogleTaskChildren() = runBlocking {
        val parent = taskDao.createNew(newTask(with(RECUR, "RRULE:FREQ=DAILY")))
        val child = taskDao.createNew(newTask(
                with(PARENT, parent),
                with(COMPLETION_TIME, DateTime())
        ))

        repeat.handleRepeat(taskDao.fetch(parent)!!)

        assertFalse(taskDao.fetch(child)!!.isCompleted)
    }
}