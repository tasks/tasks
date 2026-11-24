package org.tasks.googleapis

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.analytics.Reporting
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter

class GoogleTaskSynchronizerTest : DatabaseTest() {
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val reporting = mock<Reporting>()
    private val api = FakeGoogleTasksApi()
    private lateinit var account: CaldavAccount
    private lateinit var synchronizer: GoogleTaskSynchronizer

    @Before
    fun setUp() = runBlocking {
        account = CaldavAccount(
            uuid = "user@example.com",
            username = "user@example.com",
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
        ).let { it.copy(id = caldavDao.insert(it)) }
        val localList = CaldavFilter(
            calendar = CaldavCalendar(uuid = "local"),
            account = CaldavAccount(accountType = CaldavAccount.TYPE_LOCAL),
        )
        synchronizer = GoogleTaskSynchronizer(
            caldavDao = caldavDao,
            gtasksListService = GtasksListService(caldavDao, mock(), mock()),
            taskDao = taskDao,
            dirtyDao = db.dirtyDao(),
            taskSaver = mock(),
            reporting = reporting,
            googleTaskDao = db.googleTaskDao(),
            defaultListProvider = mock {
                onBlocking { getDefaultList() } doReturn localList
            },
            refreshBroadcaster = mock(),
            taskDeleter = mock(),
            alarmDao = db.alarmDao(),
            appPreferences = mock {
                onBlocking { defaultAlarms() } doReturn emptyList()
                onBlocking { defaultRingMode() } doReturn 0
                onBlocking { isDefaultDueTimeEnabled() } doReturn false
            },
            repeatTaskHelper = mock(),
            taskCompleter = mock(),
            createTask = { Task() },
        )
    }

    @Test
    fun pullsANewRemoteTask() = runBlocking {
        api.lists += GtasksTaskList(id = "list", title = "My Tasks", etag = "e")
        api.tasks += GtasksTask(
            id = "remote",
            title = "Title",
            notes = "Notes",
            status = "needsAction",
            due = "2026-11-24T00:00:00.000Z",
            position = "00000000000000000001",
            updated = "2026-11-24T17:45:12.345Z",
        )

        synchronizer.sync(account) { api }

        val task = taskDao.getAll().single()
        assertEquals("Title", task.title)
        assertEquals("Notes", task.notes)
        val due = org.tasks.time.DateTime(task.dueDate)
        assertEquals(listOf(2026, 11, 24), listOf(due.year, due.monthOfYear, due.dayOfMonth))
        assertTrue(!task.hasDueTime())
        assertEquals("My Tasks", caldavDao.getCalendarByUuid("list")!!.name)
        assertEquals("", caldavDao.getAccountByUuid(account.uuid!!)!!.error)
    }

    @Test
    fun aRemoteRejectionIsShownAndReported() = runBlocking {
        api.failure = GoogleTasksException(500, "500 Internal Server Error", null)

        synchronizer.sync(account) { api }

        assertEquals("500 Internal Server Error", caldavDao.getAccountByUuid(account.uuid!!)!!.error)
        verify(reporting).reportException(any(), anyOrNull())
    }

    @Test
    fun aWrappedRejectionReportsTheOriginalException() = runBlocking {
        val original = java.io.IOException("original")
        api.failure = GoogleTasksException(500, "500 Internal Server Error", null, original)

        synchronizer.sync(account) { api }

        verify(reporting).reportException(eq(original), anyOrNull())
    }

    @Test
    fun anUnauthorizedResponseIsShownWithoutReporting() = runBlocking {
        api.failure = GoogleTasksException(401, "401 Unauthorized", null)

        synchronizer.sync(account) { api }

        assertEquals("401 Unauthorized", caldavDao.getAccountByUuid(account.uuid!!)!!.error)
        verify(reporting, never()).reportException(any(), anyOrNull())
    }

    @Test
    fun aMissingListIsReportedAndSkipped() = runBlocking {
        api.lists += GtasksTaskList(id = "list", title = "My Tasks")
        api.listTasksFailure = HttpNotFoundException("404 Not Found")

        synchronizer.sync(account) { api }

        assertTrue(caldavDao.getAccountByUuid(account.uuid!!)!!.error.isNullOrEmpty())
        verify(reporting).reportException(any(), anyOrNull())
    }

    private class FakeGoogleTasksApi : GoogleTasksApi {
        val lists = mutableListOf<GtasksTaskList>()
        val tasks = mutableListOf<GtasksTask>()
        var failure: Exception? = null
        var listTasksFailure: Exception? = null

        override suspend fun allGtaskLists(pageToken: String?): GtasksTaskLists {
            failure?.let { throw it }
            return GtasksTaskLists(items = lists.toList())
        }

        override suspend fun getAllGtasksFromListId(
            listId: String?,
            lastSyncDate: Long,
            pageToken: String?,
        ): GtasksTasks {
            listTasksFailure?.let { throw it }
            return GtasksTasks(items = tasks.toList())
        }

        override suspend fun getAllPositions(listId: String?, pageToken: String?) =
            GtasksTasks(items = tasks.map { GtasksTask(id = it.id, parent = it.parent, position = it.position) })

        override suspend fun createGtask(listId: String?, task: GtasksTask, parent: String?, previous: String?) =
            task.copy(id = "created")

        override suspend fun updateGtask(listId: String?, task: GtasksTask) = task

        override suspend fun moveGtask(listId: String?, taskId: String?, parentId: String?, previousId: String?) =
            null

        override suspend fun deleteGtask(listId: String?, taskId: String?) = Unit

        override suspend fun createGtaskList(title: String) = GoogleTaskList(id = "new", title = title)

        override suspend fun renameGtaskList(listId: String, title: String) = Unit

        override suspend fun deleteGtaskList(listId: String) = Unit
    }
}
