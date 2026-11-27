package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.data.TaskCreator
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences

class DatabaseApiTaskFactoryTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()
    private val googleTaskDao = db.googleTaskDao()
    private val appPreferences: AppPreferences = mock {
        onBlocking { addTasksToTop() } doReturn true
    }

    private val factory = DatabaseApiTaskFactory(
        taskDao = taskDao,
        caldavDao = caldavDao,
        googleTaskDao = googleTaskDao,
        taskCreator = TaskCreator(),
        taskSaver = mock(),
        defaultListProvider = mock(),
        appPreferences = appPreferences,
    )

    @Test
    fun googleTasksAreCreatedWithoutARemoteId() = runBlocking {
        val list = list(TYPE_GOOGLE_TASKS)

        val task = factory.create("new", list) {}

        assertNull(caldavDao.getTask(task.id)!!.remoteId)
    }

    @Test
    fun googleTasksAddedToTopShiftTheRestDown() = runBlocking {
        val list = list(TYPE_GOOGLE_TASKS)
        val existing = existingTask(list, order = 0)
        whenever(appPreferences.addTasksToTop()).thenReturn(true)

        val task = factory.create("new", list) {}

        assertEquals(0L, taskDao.fetch(task.id)!!.order)
        assertEquals(1L, taskDao.fetch(existing)!!.order)
    }

    @Test
    fun googleTasksAddedToBottomGoAfterTheRest() = runBlocking {
        val list = list(TYPE_GOOGLE_TASKS)
        existingTask(list, order = 0)
        whenever(appPreferences.addTasksToTop()).thenReturn(false)

        val task = factory.create("new", list) {}

        assertEquals(1L, taskDao.fetch(task.id)!!.order)
    }

    @Test
    fun caldavTasksStillGetARemoteId() = runBlocking {
        val list = list(TYPE_CALDAV)

        val task = factory.create("new", list) {}

        assertNotNull(caldavDao.getTask(task.id)!!.remoteId)
    }

    private suspend fun list(type: Int): CaldavFilter {
        val account = CaldavAccount(uuid = "account-$type", accountType = type)
        caldavDao.insert(account)
        val calendar = CaldavCalendar(account = account.uuid, uuid = "calendar-$type")
        caldavDao.insert(calendar)
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun existingTask(list: CaldavFilter, order: Long): Long {
        val id = taskDao.createNew(Task(title = "existing", order = order))
        caldavDao.insert(CaldavTask(task = id, calendar = list.uuid, remoteId = "remote-existing"))
        return id
    }
}
