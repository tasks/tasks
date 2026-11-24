package org.tasks.googleapis

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.OAuthTokenStore
import org.tasks.data.entity.CaldavAccount

class GoogleTasksTokenAuth(
    private val account: CaldavAccount,
    private val tokenStore: OAuthTokenStore,
) : GoogleTasksAuth {

    private val lock = Mutex()
    private var token: OAuthTokenData? = null

    override suspend fun accessToken(): String = lock.withLock {
        val cached = token
        if (cached != null && !tokenStore.isExpired(cached)) {
            return@withLock cached.accessToken
        }
        tokenStore.getTokenData(account).also { token = it }.accessToken
    }

    override suspend fun invalidate(rejected: String): Boolean = lock.withLock {
        val refreshed = tokenStore.getTokenData(account, rejected)
        token = refreshed
        refreshed.accessToken != rejected
    }
}
