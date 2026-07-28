package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.http.HttpRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.OAuthTokenRefresh
import org.tasks.auth.OAuthTokenRefresh.withRefreshResult
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.KeyStoreEncryption

class GoogleTasksCredentialsAdapter(
    private val account: CaldavAccount,
    private val encryption: KeyStoreEncryption,
    private val proxyAuthProvider: ProxyAuthProvider,
    private val caldavDao: CaldavDao,
    private val oauthClient: TasksOAuthClient = TasksOAuthClient(),
) : CredentialsAdapter {

    private val mutex = Mutex()
    @Volatile private var accessToken: String? = null
    private var tokenData: OAuthTokenData? = null

    override fun initialize(request: HttpRequest) {
        accessToken?.let {
            request.headers.authorization = "Bearer $it"
        }
    }

    override suspend fun checkToken() = mutex.withLock {
        val data = tokenData ?: loadTokenData().also { tokenData = it }
        if (accessToken != null && !isExpired(data)) return
        if (isExpired(data)) {
            Logger.d(TAG) { "Token expired, refreshing proactively" }
            refreshAndStore(data)
        } else {
            accessToken = data.accessToken
        }
    }

    override suspend fun invalidateToken() = mutex.withLock {
        Logger.d(TAG) { "Invalidating token, will refresh" }
        accessToken = null
        val data = tokenData ?: loadTokenData().also { tokenData = it }
        refreshAndStore(data)
    }

    private suspend fun refreshAndStore(data: OAuthTokenData) {
        val authHeader = proxyAuthProvider.getAuthHeader()
        val result = OAuthTokenRefresh.refreshOrThrowIO {
            oauthClient.refreshToken(
                tokenEndpoint = data.tokenEndpoint,
                clientId = data.clientId,
                refreshToken = data.refreshToken,
                authHeader = authHeader,
            )
        }
        accessToken = result.accessToken
        val updated = data.withRefreshResult(result)
        tokenData = updated
        val encrypted = encryption.encrypt(updated.serialize())
        account.password = encrypted
        caldavDao.setPassword(account.id, encrypted)
        Logger.d(TAG) { "Token refreshed successfully" }
    }

    private fun isExpired(data: OAuthTokenData): Boolean =
        OAuthTokenRefresh.isExpired(data, refreshWhenExpiryUnknown = false)

    private suspend fun loadTokenData(): OAuthTokenData {
        val encrypted = account.password
            ?: throw IllegalStateException("No credentials stored for account")
        val decrypted = encryption.decrypt(encrypted)
            ?: throw IllegalStateException("Failed to decrypt credentials")
        return OAuthTokenData.deserialize(decrypted)
    }

    companion object {
        private const val TAG = "GoogleTasksCredentials"
    }
}
