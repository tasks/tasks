package org.tasks.sync.microsoft

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.analytics.Reporting
import org.tasks.caldav.failFastServer
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.http.installMicrosoftGraph
import org.tasks.http.toKtor

class MicrosoftStreamErrorTest : DatabaseTest() {
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
    fun `an error appended mid-stream surfaces the graph error and is reported`() = runBlocking {
        server.enqueue(lists())
        server.enqueue(truncatedDelta())

        synchronizer.sync(account)

        assertEquals(
            "ErrorInternalServerError: An internal server error occurred. The operation failed. (Request ID: f7e56ad5-d9ea-46d9-9df0-325e31aa983f)",
            caldavDao.getAccountByUuid("account")!!.error,
        )
        verify(reporting).reportException(any(), any())
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

    private fun truncatedDelta() = MockResponse()
        .setHeader("Content-Type", "application/json;odata.metadata=minimal;odata.streaming=true")
        .setBody(
            """{"@odata.context":"context","value":[{"@odata.etag":"W/\"1\"","id":"task1","title":"Title","status":"notStarted","body":{"content":"","contentType":"text"},"startDateTime":{""" +
                """{"error":{"code":"ErrorInternalServerError","message":"An internal server error occurred. The operation failed.",""" +
                """"innerError":{"date":"2026-11-15T06:47:21","request-id":"f7e56ad5-d9ea-46d9-9df0-325e31aa983f","client-request-id":"f7e56ad5-d9ea-46d9-9df0-325e31aa983f"}}}"""
        )

    companion object {
        private const val LIST_ID = "list1"
    }
}
