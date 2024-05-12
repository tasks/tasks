package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavTask
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var caldavDao: CaldavDao

    @Test
    fun insertNewTaskAtTopOfEmptyList() = runBlocking {
        val task = newTask()
        taskDao.createNew(task)
        caldavDao.insert(task, CaldavTask(task = task.id, calendar = "calendar"), true)

        checkOrder(null, task.id)
    }

    @Test
    fun insertNewTaskAboveExistingTask() = runBlocking {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created.plusSeconds(1)))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(task = first.id, calendar = "calendar"), true)
        caldavDao.insert(second, CaldavTask(task = second.id, calendar = "calendar"), true)

        checkOrder(null, first.id)
        checkOrder(created.minusSeconds(1), second.id)
    }

    @Test
    fun insertNewTaskBelowExistingTask() = runBlocking {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created.plusSeconds(1)))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(task = first.id, calendar = "calendar"), false)
        caldavDao.insert(second, CaldavTask(task = second.id, calendar = "calendar"), false)

        checkOrder(null, first.id)
        checkOrder(null, second.id)
    }

    @Test
    fun insertNewTaskBelowExistingTaskWithSameCreationDate() = runBlocking {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(task = first.id, calendar = "calendar"), false)
        caldavDao.insert(second, CaldavTask(task = second.id, calendar = "calendar"), false)

        checkOrder(null, first.id)
        checkOrder(created.plusSeconds(1), second.id)
    }

    @Test
    fun insertNewTaskAtBottomOfEmptyList() = runBlocking {
        val task = newTask()
        taskDao.createNew(task)
        caldavDao.insert(task, CaldavTask(task = task.id, calendar = "calendar"), false)

        checkOrder(null, task.id)
    }

    @Test
    fun noResultsForEmptyAccounts() = runBlocking {
        val caldavAccount = CaldavAccount(uuid = UUIDHelper.newUUID())
        caldavDao.insert(caldavAccount)
        assertTrue(caldavDao.getCaldavFilters(caldavAccount.uuid!!).isEmpty())
    }

    private suspend fun checkOrder(dateTime: DateTime, task: Long) = checkOrder(dateTime.toAppleEpoch(), task)

    private suspend fun checkOrder(order: Long?, task: Long) {
        val sortOrder = taskDao.fetch(task)!!.order
        if (order == null) {
            assertNull(sortOrder)
        } else {
            assertEquals(order, sortOrder)
        }
    }
}
