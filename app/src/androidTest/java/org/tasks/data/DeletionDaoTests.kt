package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class DeletionDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var deletionDao: DeletionDao

    @Test
    fun deleting1000DoesntCrash() {
        deletionDao.delete((1L..1000L).toList())
    }

    @Test
    fun marking998ForDeletionDoesntCrash() {
        deletionDao.markDeleted(1L..1000L)
    }

    @Test
    fun markDeletedUpdatesModificationTime() {
        var task = newTask(MakeItEasy.with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.getId()))
        task = taskDao.fetch(task.getId())
        assertTrue(task.modificationDate > task.creationDate)
        assertTrue(task.modificationDate < DateTimeUtils.currentTimeMillis())
    }

    @Test
    fun markDeletedUpdatesDeletionTime() {
        var task = newTask(MakeItEasy.with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.getId()))
        task = taskDao.fetch(task.getId())
        assertTrue(task.deletionDate > task.creationDate)
        assertTrue(task.deletionDate < DateTimeUtils.currentTimeMillis())
    }

    override fun inject(component: TestComponent) = component.inject(this)
}