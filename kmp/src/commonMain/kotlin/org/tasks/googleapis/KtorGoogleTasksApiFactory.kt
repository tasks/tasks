package org.tasks.googleapis

import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.auth.OAuthTokenStore
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.KtorClientFactory

class KtorGoogleTasksApiFactory(
    private val httpClientFactory: KtorClientFactory,
    private val tokenStore: OAuthTokenStore,
) {
    private val lock = Mutex()
    private var client: HttpClient? = null

    suspend fun forAccount(account: CaldavAccount): GoogleTasksApi =
        KtorGoogleTasksApi(httpClient(), GoogleTasksTokenAuth(account, tokenStore))

    private suspend fun httpClient(): HttpClient = lock.withLock {
        client ?: httpClientFactory.newClient().also { client = it }
    }
}
