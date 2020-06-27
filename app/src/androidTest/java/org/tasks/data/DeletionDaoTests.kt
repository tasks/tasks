package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDaoBlocking
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.*
import org.junit.Test
import org.tasks.data.CaldavDao.Companion.LOCAL
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class DeletionDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDaoBlocking
    @Inject lateinit var deletionDao: DeletionDaoBlocking
    @Inject lateinit var caldavDao: CaldavDaoBlocking

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
        var task = newTask(with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.id))
        task = taskDao.fetchBlocking(task.id)!!
        assertTrue(task.modificationDate > task.creationDate)
        assertTrue(task.modificationDate < DateTimeUtils.currentTimeMillis())
    }

    @Test
    fun markDeletedUpdatesDeletionTime() {
        var task = newTask(with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.id))
        task = taskDao.fetchBlocking(task.id)!!
        assertTrue(task.deletionDate > task.creationDate)
        assertTrue(task.deletionDate < DateTimeUtils.currentTimeMillis())
    }

    @Test
    fun purgeDeletedLocalTask() {
        val task = newTask(with(DELETION_TIME, newDateTime()))
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar("", "1234").apply { account = LOCAL })
        caldavDao.insert(CaldavTask(task.id, "1234"))

        deletionDao.purgeDeleted()

        assertNull(taskDao.fetchBlocking(task.id))
    }

    @Test
    fun dontPurgeActiveTasks() {
        val task = newTask()
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar("", "1234").apply { account = LOCAL })
        caldavDao.insert(CaldavTask(task.id, "1234"))

        deletionDao.purgeDeleted()

        assertNotNull(taskDao.fetchBlocking(task.id))
    }

    @Test
    fun dontPurgeDeletedCaldavTask() {
        val task = newTask(with(DELETION_TIME, newDateTime()))
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar("", "1234").apply { account = UUIDHelper.newUUID() })
        caldavDao.insert(CaldavTask(task.id, "1234"))

        deletionDao.purgeDeleted()

        assertNotNull(taskDao.fetchBlocking(task.id))
    }
}