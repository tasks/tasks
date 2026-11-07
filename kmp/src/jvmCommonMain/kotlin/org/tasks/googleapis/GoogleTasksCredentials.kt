package org.tasks.googleapis

import com.google.api.client.http.HttpRequest
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption

class GoogleTasksCredentials(
    encryption: Encryption,
    proxyAuthProvider: ProxyAuthProvider,
    caldavDao: CaldavDao,
    oauthClient: TasksOAuthClient,
) {
    private val tokenStore = OAuthTokenStore(
        encryption = encryption,
        caldavDao = caldavDao,
        oauthClient = oauthClient,
        refreshWhenExpiryUnknown = false,
        authHeader = proxyAuthProvider::getAuthHeader,
    )

    fun adapter(account: CaldavAccount): CredentialsAdapter =
        GoogleTasksCredentialsAdapter(account, tokenStore)
}

private class GoogleTasksCredentialsAdapter(
    private val account: CaldavAccount,
    private val tokenStore: OAuthTokenStore,
) : CredentialsAdapter {

    @Volatile private var token: OAuthTokenData? = null

    override fun initialize(request: HttpRequest) {
        token?.let {
            request.headers.authorization = "Bearer ${it.accessToken}"
        }
    }

    override suspend fun checkToken() {
        val cached = token
        if (cached != null && !tokenStore.isExpired(cached)) {
            return
        }
        token = tokenStore.getTokenData(account)
    }

    override suspend fun invalidateToken(): Boolean {
        val rejected = token?.accessToken
        val refreshed = tokenStore.getTokenData(account, rejected)
        token = refreshed
        return refreshed.accessToken != rejected
    }
}
