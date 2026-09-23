package org.tasks.location

import android.content.Context
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.http.HttpClientFactory
import java.util.concurrent.CountDownLatch

class PlaceSearchGoogleCloseTest {
    private val client = mock<HttpClient>()
    private val creating = CountDownLatch(1)
    private val release = CountDownLatch(1)

    @Test
    fun closesAClientThatArrivesAfterClose() = runBlocking {
        val search = PlaceSearchGoogle(mock<Context>(), httpClientFactory(), caldavDao(), environment())

        val query = launch(Dispatchers.IO) { runCatching { search.search("portillo's", null) } }
        creating.await()
        search.close()
        release.countDown()
        query.join()

        verify(client).close()
    }

    private fun httpClientFactory() = mock<HttpClientFactory> {
        onBlocking {
            newAuthenticatedClient(any(), any(), any(), any())
        } doSuspendableAnswer {
            creating.countDown()
            release.await()
            client
        }
    }

    private fun caldavDao() = mock<CaldavDao> {
        onBlocking { getAccounts(TYPE_TASKS) } doReturn listOf(
            CaldavAccount(username = "user", password = "encrypted")
        )
    }

    private fun environment() = mock<TasksServerEnvironment> {
        on { placesUrl } doReturn "https://places.example.com"
    }
}
