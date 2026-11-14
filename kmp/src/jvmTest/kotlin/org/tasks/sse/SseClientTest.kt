package org.tasks.sse

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.auth.TasksServerEnvironment
import org.tasks.caldav.testEncryption
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.http.KtorClientFactory
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncSource
import java.util.Base64

class SseClientTest {
    private val db = Room.inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .addCallback(Database.CALLBACK)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val encryption = testEncryption()
    private val requests = mutableListOf<HttpRequestData>()
    private val synced = mutableListOf<SyncSource>()
    private val requestTimes = mutableListOf<Long>()
    private var status = HttpStatusCode.OK

    private val backgroundWork = object : BackgroundWork {
        override fun updateCalendar(task: Task) {}
        override suspend fun scheduleRefresh(timestamp: Long) {}
        override suspend fun scheduleBlogFeedCheck() {}
        override suspend fun sync(source: SyncSource) {
            synced += source
        }
    }

    private val clientFactory = object : KtorClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: HttpClientConfig<*>.() -> Unit,
        ) = HttpClient(MockEngine { request ->
            requests += request
            requestTimes += System.currentTimeMillis()
            val body = if (requests.size == 1) {
                ByteReadChannel(": heartbeat\ndata: {\"sync\": true}\ndata: {\"other\": 1}\n")
            } else {
                ByteChannel()
            }
            respond(body, status, headersOf("Content-Type", "text/event-stream"))
        }) { block() }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun insertAccount() {
        db.caldavDao().insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_TASKS,
                uuid = "cloud",
                username = "google_1",
                password = encryption.encrypt("secret"),
                url = "https://caldav.tasks.org/calendars/google_1/",
                serverType = CaldavAccount.SERVER_TASKS,
            )
        )
    }

    private fun newClient() = SseClient(
        scope = scope,
        backgroundWork = backgroundWork,
        caldavDao = db.caldavDao(),
        encryption = encryption,
        environment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore())),
        httpClientFactory = clientFactory,
        token = { "t0ken" },
    )

    @Test
    fun syncsOnEachSyncEventOfTheAccountsStream() = runBlocking {
        insertAccount()
        val client = newClient()

        client.start()
        withTimeout(5_000) { while (synced.isEmpty()) delay(10) }
        delay(200)

        assertEquals(listOf(SyncSource.PUSH_NOTIFICATION), synced)
        val request = requests.first()
        assertEquals("https://caldav.tasks.org/sse?token=t0ken", request.url.toString())
        assertEquals("text/event-stream", request.headers["Accept"])
        val basic = Base64.getEncoder().encodeToString("google_1:secret".toByteArray())
        assertEquals("Basic $basic", request.headers["Authorization"])

        client.stop()
        delay(200)
        val connections = requests.size
        delay(1_500)
        assertTrue(requests.size == connections)
    }

    @Test
    fun backsOffWhenTheConnectionIsRejected() = runBlocking {
        status = HttpStatusCode.InternalServerError
        insertAccount()
        val client = newClient()

        client.start()
        withTimeout(5_000) { while (requestTimes.size < 3) delay(10) }
        client.stop()

        assertTrue(requestTimes[1] - requestTimes[0] >= 900)
        assertTrue(requestTimes[2] - requestTimes[1] >= 1_900)
        assertTrue(synced.isEmpty())
    }

    @Test
    fun waitsForANewPasswordAfterAnAuthError() = runBlocking {
        status = HttpStatusCode.Unauthorized
        insertAccount()
        val client = newClient()

        client.start()
        withTimeout(5_000) { while (requests.isEmpty()) delay(10) }
        delay(2_500)
        assertEquals(1, requests.size)

        val account = db.caldavDao().getAccountByUuid("cloud")!!
        db.caldavDao().update(account.copy(password = encryption.encrypt("new secret")))
        withTimeout(5_000) { while (requests.size < 2) delay(10) }
        client.stop()

        val basic = Base64.getEncoder().encodeToString("google_1:new secret".toByteArray())
        assertEquals("Basic $basic", requests[1].headers["Authorization"])
    }
}
