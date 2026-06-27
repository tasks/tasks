package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskMover
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask
import org.tasks.service.TaskDeleter
import javax.inject.Inject

@HiltAndroidTest
class OpenTasksSynchronizerTest : OpenTasksTest() {

    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var taskDeleter: TaskDeleter

    @Test
    fun createNewAccounts() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync(hasPro = true)

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

        synchronizer.sync(hasPro = true)

        assertTrue(caldavDao.getAccounts().isEmpty())
    }

    @Test
    fun createNewLists() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync(hasPro = true)

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

        synchronizer.sync(hasPro = true)

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

        synchronizer.sync(hasPro = true)

        assertNotNull(openTaskDao.getTask(listId, "1234"))
    }

    @Test
    fun sanitizeRecurrenceRule() = runBlocking {
        val (_, list) = openTaskDao.insertList()
        val taskId = taskDao.insert(newTask(with(RECUR, "RRULE:FREQ=WEEKLY;COUNT=-1")))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(TASK, taskId)
        ))

        synchronizer.sync(hasPro = true)

        val task = openTaskDao.getTasks().first()
        assertEquals("FREQ=WEEKLY", task.rRule?.value)
    }

    @Test
    fun pushLocalDeletionAfterFetch() = runBlocking {
        val (listId, list) = withVtodo(VTODO)
        synchronizer.sync(hasPro = true)
        val taskId = caldavDao.getTaskByRemoteId(list.uuid!!, UID)!!.task

        taskDeleter.markDeleted(listOf(taskId))

        synchronizer.sync(hasPro = true)

        assertNull(openTaskDao.getTask(listId, UID))
    }

    @Test
    fun moveBetweenListsRemovesFromSourceProvider() = runBlocking {
        val (sourceListId, source) = withVtodo(VTODO)
        val (_, destination) = openTaskDao.insertList()
        synchronizer.sync(hasPro = true)
        val taskId = caldavDao.getTaskByRemoteId(source.uuid!!, UID)!!.task

        taskMover.move(taskId, destination.uuid!!, 0L)

        synchronizer.sync(hasPro = true)

        assertNull(openTaskDao.getTask(sourceListId, UID))
        assertEquals(destination.uuid, caldavDao.getTask(taskId)?.calendar)
        assertEquals("original title", openTaskDao.getTasks().single().summary)
    }

    companion object {
        private const val UID = "1234"
        private val VTODO = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210201T204211Z
            UID:1234
            CREATED:20210201T204143Z
            LAST-MODIFIED:20210201T204209Z
            SUMMARY:original title
            DESCRIPTION:original notes
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
