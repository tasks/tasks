package com.todoroo.astrid.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.tasks.DatabaseTest
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class Upgrade_15_15_StatusTest : DatabaseTest() {
    @get:Rule val folder = TemporaryFolder()

    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val vtodoCache by lazy { VtodoCache(caldavDao, FileStorage(folder.root.absolutePath)) }

    @Test
    fun readsStatusesFromCache() = runBlocking {
        val calendar = calendar()
        val inProcess = cachedTask(calendar, "a", "STATUS:IN-PROCESS")
        val cancelled = cachedTask(calendar, "b", "STATUS:CANCELLED")
        val needsAction = cachedTask(calendar, "c", "STATUS:NEEDS-ACTION")
        val completed = cachedTask(calendar, "d", "STATUS:COMPLETED\nCOMPLETED:20260101T000000Z")
        val noStatus = cachedTask(calendar, "e", null)
        val modified = taskDao.fetch(inProcess)!!.modificationDate

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.IN_PROCESS, status(inProcess))
        assertEquals(Task.Status.CANCELLED, status(cancelled))
        assertEquals(Task.Status.NEEDS_ACTION, status(needsAction))
        assertEquals(Task.Status.NEEDS_ACTION, status(completed))
        assertEquals(Task.Status.NEEDS_ACTION, status(noStatus))
        assertEquals(modified, taskDao.fetch(inProcess)!!.modificationDate)
    }

    @Test
    fun readsStatusOfTaskCompletedLocallyButNotPushed() = runBlocking {
        val calendar = calendar()
        val task = cachedTask(calendar, "a", "STATUS:IN-PROCESS")
        taskDao.update(taskDao.fetch(task)!!.copy(completionDate = 1_700_000_000_000))

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.IN_PROCESS, status(task))
    }

    @Test
    fun readsStatusOfUnsyncedMove() = runBlocking {
        val calendar = calendar()
        val task = cachedTask(calendar, "a", "STATUS:IN-PROCESS")
        caldavDao.update(caldavDao.getTask(task)!!.copy(deleted = 1_700_000_000_000))
        caldavDao.insert(CaldavTask(task = task, calendar = "other", remoteId = "b"))

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.IN_PROCESS, status(task))
    }

    @Test
    fun ignoresTasksWithoutCache() = runBlocking {
        val calendar = calendar()
        val task = Task(title = "uncached").also { taskDao.createNew(it) }
        caldavDao.insert(CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = "x", obj = "x.ics"))

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.NEEDS_ACTION, status(task.id))
    }

    @Test
    fun skipsMicrosoftAccounts() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_MICROSOFT)
        val task = cachedTask(calendar, "a", "STATUS:IN-PROCESS")

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.NEEDS_ACTION, status(task))
    }

    @Test
    fun readsTasksOrgAccounts() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_TASKS)
        val task = cachedTask(calendar, "a", "STATUS:IN-PROCESS")

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.IN_PROCESS, status(task))
    }

    @Test
    fun readsEtebaseAccounts() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_ETEBASE)
        val task = cachedTask(calendar, "a", "STATUS:CANCELLED")

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.CANCELLED, status(task))
    }

    @Test
    fun skipsOpenTasksAccounts() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_OPENTASKS)
        val task = cachedTask(calendar, "a", "STATUS:IN-PROCESS")

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache).run()

        assertEquals(Task.Status.NEEDS_ACTION, status(task))
    }

    @Test
    fun forcesOpenTasksRefetchWhenProviderUnreadable() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_OPENTASKS)
        caldavDao.update(calendar.copy(ctag = "ctag"))
        val task = Task(title = "a").also { taskDao.createNew(it) }
        caldavDao.insert(CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = "a", etag = "etag"))

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache) {
            throw SecurityException("no tasks permission")
        }.run()

        assertNull(caldavDao.getCalendar(calendar.uuid!!)!!.ctag)
        assertNull(caldavDao.getTask(task.id)!!.etag)
    }

    @Test
    fun keepsOpenTasksEtagsWhenProviderReadable() = runBlocking {
        val calendar = calendar(accountType = CaldavAccount.TYPE_OPENTASKS)
        caldavDao.update(calendar.copy(ctag = "ctag"))
        val task = Task(title = "a").also { taskDao.createNew(it) }
        caldavDao.insert(CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = "a", etag = "etag"))

        Upgrade_15_15_Status(db.upgraderDao(), caldavDao, vtodoCache) {}.run()

        assertEquals("ctag", caldavDao.getCalendar(calendar.uuid!!)!!.ctag)
        assertEquals("etag", caldavDao.getTask(task.id)!!.etag)
    }

    private suspend fun status(id: Long) = taskDao.fetch(id)!!.status

    private suspend fun calendar(accountType: Int = CaldavAccount.TYPE_CALDAV): CaldavCalendar {
        val account = CaldavAccount(
            uuid = "account",
            username = "user",
            url = "https://example.com/",
            accountType = accountType,
        )
        caldavDao.insert(account)
        return CaldavCalendar(account = account.uuid, uuid = "calendar", url = "https://example.com/list/")
            .also { caldavDao.insert(it) }
    }

    private suspend fun cachedTask(calendar: CaldavCalendar, uid: String, extra: String?): Long {
        val task = Task(title = uid).also { taskDao.createNew(it) }
        val caldavTask = CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = uid, obj = "$uid.ics")
        caldavDao.insert(caldavTask)
        val lines = listOfNotNull(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:test",
            "BEGIN:VTODO",
            "UID:$uid",
            "DTSTAMP:20260101T000000Z",
            "SUMMARY:$uid",
            extra,
            "END:VTODO",
            "END:VCALENDAR",
        )
        vtodoCache.putVtodo(calendar, caldavTask, lines.joinToString("\r\n").replace("\n", "\r\n").replace("\r\r", "\r") + "\r\n")
        return task.id
    }
}
