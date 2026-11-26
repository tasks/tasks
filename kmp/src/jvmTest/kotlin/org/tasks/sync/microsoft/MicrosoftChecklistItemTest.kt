package org.tasks.sync.microsoft

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.tasks.DatabaseTest
import org.tasks.analytics.Reporting
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.failFastServer
import org.tasks.data.TaskSaver
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.http.installMicrosoftGraph
import org.tasks.http.toKtor

class MicrosoftChecklistItemTest : DatabaseTest() {
    private val server = failFastServer()
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val vtodoCache: VtodoCache = mock()
    private lateinit var account: CaldavAccount
    private lateinit var synchronizer: MicrosoftSynchronizer

    @Before
    fun setUp() = runBlocking {
        server.start()
        val service = MicrosoftService(
            client = OkHttpClient().toKtor {
                installMicrosoftGraph(
                    cookiesStorage = AcceptAllCookiesStorage(),
                    log = {},
                    bearerToken = { "token" },
                )
            },
            baseUrl = server.url("/me/todo").toString().trimEnd('/'),
        )
        synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            dirtyDao = db.dirtyDao(),
            taskSaver = TaskSaver(
                taskDao = taskDao,
                refreshBroadcaster = mock(),
                notifier = mock(),
                locationService = mock(),
                timerPlugin = mock(),
                backgroundWork = mock(),
                caldavDao = caldavDao,
            ),
            refreshBroadcaster = mock(),
            taskDeleter = mock(),
            reporting = mock<Reporting>(),
            clientProvider = mock {
                on { hasCredentials(any()) } doReturn true
                on { getService(any()) } doReturn service
            },
            tagDao = mock(),
            tagDataDao = mock(),
            appPreferences = mock {
                onBlocking { defaultPriority() } doReturn Task.Priority.NONE
            },
            vtodoCache = vtodoCache,
            createTask = { Task() },
            setDefaultList = {},
        )
        account = CaldavAccount(
            uuid = "account",
            username = "user@example.com",
            accountType = CaldavAccount.TYPE_MICROSOFT,
        ).let { it.copy(id = caldavDao.insert(it)) }
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `occurrences of a recurring task do not steal each other's checklist items`() = runBlocking {
        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())

        synchronizer.sync(account, hasPro = true)

        assertEquals("", caldavDao.getAccountByUuid("account")!!.error)
        val live = taskByRemoteId("live-occurrence")
        val completed = taskByRemoteId("completed-occurrence")
        assertEquals(
            "the live occurrence should keep its own step",
            1,
            taskDao.getChildren(live).size,
        )
        assertEquals(
            "the completed occurrence should keep its own step",
            1,
            taskDao.getChildren(completed).size,
        )
        assertNotEquals(
            "each occurrence needs its own local step",
            taskDao.getChildren(live).single(),
            taskDao.getChildren(completed).single(),
        )
    }

    @Test
    fun `re-syncing shared checklist item ids does not duplicate the steps`() = runBlocking {
        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())
        synchronizer.sync(account, hasPro = true)
        val firstPass = taskDao.getAll().size

        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())
        synchronizer.sync(account, hasPro = true)

        assertEquals("", caldavDao.getAccountByUuid("account")!!.error)
        assertEquals(firstPass, taskDao.getAll().size)
        assertEquals(1, taskDao.getChildren(taskByRemoteId("live-occurrence")).size)
        assertEquals(1, taskDao.getChildren(taskByRemoteId("completed-occurrence")).size)
    }

    @Test
    fun `object names are scoped to the parent task`() = runBlocking {
        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())

        synchronizer.sync(account, hasPro = true)

        assertEquals("", caldavDao.getAccountByUuid("account")!!.error)
        assertEquals("live-occurrence-$SHARED_ITEM_ID.json", stepOf("live-occurrence").obj)
        assertEquals(
            "completed-occurrence-$SHARED_ITEM_ID.json",
            stepOf("completed-occurrence").obj,
        )
    }

    @Test
    fun `object names from before they were scoped are repaired`() = runBlocking {
        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())
        synchronizer.sync(account, hasPro = true)
        listOf("live-occurrence", "completed-occurrence").forEach { parent ->
            caldavDao.update(stepOf(parent).apply { obj = LEGACY_OBJECT_NAME })
        }
        val tasks = taskDao.getAll().size

        server.enqueue(lists())
        server.enqueue(twoOccurrencesSharingAChecklistItemId())
        synchronizer.sync(account, hasPro = true)

        assertEquals("", caldavDao.getAccountByUuid("account")!!.error)
        assertEquals(tasks, taskDao.getAll().size)
        assertEquals("live-occurrence-$SHARED_ITEM_ID.json", stepOf("live-occurrence").obj)
        assertEquals(
            "completed-occurrence-$SHARED_ITEM_ID.json",
            stepOf("completed-occurrence").obj,
        )
        verifyBlocking(vtodoCache, times(2)) {
            delete(any<CaldavCalendar>(), argThat<CaldavTask> { obj == LEGACY_OBJECT_NAME })
        }
    }

    private suspend fun stepOf(parentRemoteId: String): CaldavTask =
        caldavDao
            .getTasks(taskDao.getChildren(taskByRemoteId(parentRemoteId)).single())
            .single()

    private suspend fun taskByRemoteId(remoteId: String) =
        caldavDao.getTaskByRemoteId(LIST_ID, remoteId)!!.task

    private fun lists() = json(
        """
        {
          "@odata.context": "context",
          "value": [
            { "id": "$LIST_ID", "displayName": "Tasks", "isOwner": true }
          ]
        }
        """
    )

    private fun twoOccurrencesSharingAChecklistItemId() = json(
        """
        {
          "@odata.deltaLink": "${server.url("/me/todo/lists/$LIST_ID/tasks/delta?\$deltatoken=next")}",
          "value": [
            {
              "id": "live-occurrence",
              "title": "water the plants",
              "status": "notStarted",
              "createdDateTime": "2022-03-21T10:07:00.0000000Z",
              "lastModifiedDateTime": "2026-11-21T10:07:00.0000000Z",
              "recurrence": {
                "pattern": { "type": "daily", "interval": 1, "daysOfWeek": [] }
              },
              "checklistItems": [
                {
                  "id": "$SHARED_ITEM_ID",
                  "displayName": "fill the watering can",
                  "createdDateTime": "2022-03-21T10:07:00.0000000Z",
                  "isChecked": false
                }
              ]
            },
            {
              "id": "completed-occurrence",
              "title": "water the plants",
              "status": "completed",
              "createdDateTime": "2022-03-21T10:07:00.0000000Z",
              "lastModifiedDateTime": "2022-03-22T10:07:00.0000000Z",
              "completedDateTime": {
                "dateTime": "2022-03-22T00:00:00.0000000",
                "timeZone": "UTC"
              },
              "checklistItems": [
                {
                  "id": "$SHARED_ITEM_ID",
                  "displayName": "fill the watering can",
                  "createdDateTime": "2022-03-21T10:07:00.0000000Z",
                  "isChecked": false
                }
              ]
            }
          ]
        }
        """
    )

    private fun json(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body.trimIndent())

    companion object {
        private const val LIST_ID = "list1"
        private const val SHARED_ITEM_ID = "shared-checklist-item-id"
        private const val LEGACY_OBJECT_NAME = "$SHARED_ITEM_ID.json"
    }
}
