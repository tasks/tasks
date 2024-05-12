package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class OpenTasksSynchronizerTest : OpenTasksTest() {

    @Test
    fun createNewAccounts() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync()

        val accounts = caldavDao.getAccounts()
        assertEquals(1, accounts.size)
        with(accounts[0]) {
            assertEquals("bitfire.at.davdroid:test_account", uuid)
            assertEquals("test_account", name)
            assertEquals(TYPE_OPENTASKS, accountType)
        }
    }

    @Test
    fun deleteRemovedAccounts() = runBlocking {
        caldavDao.insert(
            CaldavAccount(
                uuid = "bitfire.at.davdroid:test_account",
                accountType = TYPE_OPENTASKS,
            )
        )

        synchronizer.sync()

        assertTrue(caldavDao.getAccounts().isEmpty())
    }

    @Test
    fun createNewLists() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync()

        val lists = caldavDao.getCalendarsByAccount("bitfire.at.davdroid:test_account")
        assertEquals(1, lists.size)
        with(lists[0]) {
            assertEquals(name, "default_list")
        }
    }

    @Test
    fun removeMissingLists() = runBlocking {
        val (_, list) = openTaskDao.insertList(url = "url1")
        caldavDao.insert(
            CaldavCalendar(
                account = list.account,
                url = "url2",
            )
        )

        synchronizer.sync()

        assertEquals(listOf(list), caldavDao.getCalendars())
    }

    @Test
    fun simplePushNewTask() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask())
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "1234"),
                with(TASK, taskId)
        ))

        synchronizer.sync()

        assertNotNull(openTaskDao.getTask(listId.toLong(), "1234"))
    }

    @Test
    fun sanitizeRecurrenceRule() = runBlocking {
        val (_, list) = openTaskDao.insertList()
        val taskId = taskDao.insert(newTask(with(RECUR, "RRULE:FREQ=WEEKLY;COUNT=-1")))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(TASK, taskId)
        ))

        synchronizer.sync()

        val task = openTaskDao.getTasks().first()
        assertEquals("FREQ=WEEKLY", task.rRule?.value)
    }
}