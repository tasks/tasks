package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.CaldavDao.Companion.LOCAL
import org.tasks.data.dao.DeletionDao
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class DeletionDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var deletionDao: DeletionDao
    @Inject lateinit var caldavDao: CaldavDao

    @Test
    fun deleting1000DoesntCrash() = runBlocking {
        deletionDao.delete((1L..1000L).toList())
    }

    @Test
    fun marking998ForDeletionDoesntCrash() = runBlocking {
        deletionDao.markDeleted(1L..1000L)
    }

    @Test
    fun markDeletedUpdatesModificationTime() = runBlocking {
        var task = newTask(with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.id))
        task = taskDao.fetch(task.id)!!
        assertTrue(task.modificationDate > task.creationDate)
        assertTrue(task.modificationDate < currentTimeMillis())
    }

    @Test
    fun markDeletedUpdatesDeletionTime() = runBlocking {
        var task = newTask(with(CREATION_TIME, DateTime().minusMinutes(1)))
        taskDao.createNew(task)
        deletionDao.markDeleted(listOf(task.id))
        task = taskDao.fetch(task.id)!!
        assertTrue(task.deletionDate > task.creationDate)
        assertTrue(task.deletionDate < currentTimeMillis())
    }

    @Test
    fun purgeDeletedLocalTask() = runBlocking {
        val task = newTask(with(DELETION_TIME, newDateTime()))
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar(name = "", uuid = "1234", account = LOCAL))
        caldavDao.insert(CaldavTask(task = task.id, calendar = "1234"))

        deletionDao.purgeDeleted()

        assertNull(taskDao.fetch(task.id))
    }

    @Test
    fun dontPurgeActiveTasks() = runBlocking {
        val task = newTask()
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar(name = "", uuid = "1234", account = LOCAL))
        caldavDao.insert(CaldavTask(task = task.id, calendar = "1234"))

        deletionDao.purgeDeleted()

        assertNotNull(taskDao.fetch(task.id))
    }

    @Test
    fun dontPurgeDeletedCaldavTask() = runBlocking {
        val task = newTask(with(DELETION_TIME, newDateTime()))
        taskDao.createNew(task)
        caldavDao.insert(CaldavCalendar(name = "", uuid = "1234", account = UUIDHelper.newUUID()))
        caldavDao.insert(CaldavTask(task = task.id, calendar = "1234"))

        deletionDao.purgeDeleted()

        assertNotNull(taskDao.fetch(task.id))
    }
}
