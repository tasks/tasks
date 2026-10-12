package org.tasks.sync.microsoft

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.analytics.Reporting
import org.tasks.caldav.failFastServer
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.http.installMicrosoftGraph
import org.tasks.http.toKtor

class MicrosoftDeltaTokenTest : DatabaseTest() {
    private val server = failFastServer()
    private val caldavDao = db.caldavDao()
    private val reporting = mock<Reporting>()
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
            taskDao = db.taskDao(),
            dirtyDao = db.dirtyDao(),
            taskSaver = mock(),
            refreshBroadcaster = mock(),
            taskDeleter = mock(),
            reporting = reporting,
            clientProvider = mock {
                on { hasCredentials(any()) } doReturn true
                on { getService(any()) } doReturn service
            },
            tagDao = mock(),
            tagDataDao = mock(),
            appPreferences = mock(),
            vtodoCache = mock(),
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
    fun `an expired delta token resets the ctag instead of wedging the list`() = runBlocking {
        caldavDao.insert(
            CaldavCalendar(
                account = account.uuid,
                uuid = LIST_ID,
                url = LIST_ID,
                ctag = server.url("/me/todo/lists/$LIST_ID/tasks/delta?\$deltatoken=dead").toString(),
            )
        )
        server.enqueue(lists())
        server.enqueue(gone(code = "SyncStateNotFound"))

        synchronizer.sync(account, hasPro = true)

        assertNull(caldavDao.getCalendarByUuid(LIST_ID)!!.ctag)
        assertFalse(caldavDao.getAccountByUuid("account")!!.hasError)
        verify(reporting, never()).reportException(any(), any())
        assertEquals(2, server.requestCount)
    }

    private fun lists() = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(
            """
            {
              "@odata.context": "context",
              "value": [
                { "id": "$LIST_ID", "displayName": "Tasks", "isOwner": true }
              ]
            }
            """.trimIndent()
        )

    private fun gone(code: String) = MockResponse()
        .setResponseCode(410)
        .setHeader("Content-Type", "application/json")
        .setBody(
            """
            {
              "error": {
                "code": "$code",
                "message": "The delta token is no longer valid, and the app must reset the sync state."
              }
            }
            """.trimIndent()
        )

    companion object {
        private const val LIST_ID = "list1"
    }
}
