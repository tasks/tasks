package org.tasks.googleapis

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.TasksOAuthClient
import org.tasks.auth.TokenError
import org.tasks.auth.isUnauthorized
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class DesktopGoogleTasksSynchronizerTest {

    private val encryption = KeyStoreEncryption(object : KeyProvider {
        private val key: SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun getKey() = key
    })
    private val caldavDao = mock<CaldavDao>()
    private val oauthClient = mock<TasksOAuthClient>()
    private val proxyAuthProvider = mock<ProxyAuthProvider>()
    private val refreshBroadcaster = mock<RefreshBroadcaster>()

    private val synchronizer = DesktopGoogleTasksSynchronizer(
        caldavDao = caldavDao,
        taskDao = mock(),
        dirtyDao = mock(),
        taskSaver = mock(),
        reporting = mock(),
        googleTaskDao = mock(),
        defaultListProvider = mock(),
        refreshBroadcaster = refreshBroadcaster,
        taskDeleter = mock(),
        alarmDao = mock(),
        appPreferences = mock(),
        repeatTaskHelper = mock(),
        taskCompleter = mock(),
        encryption = encryption,
        createTask = { Task() },
        proxyAuthProvider = proxyAuthProvider,
        oauthClient = oauthClient,
    )

    private fun accountWithExpiredToken(): CaldavAccount = runBlocking {
        val tokenData = OAuthTokenData(
            accessToken = "stale",
            refreshToken = "dead",
            tokenEndpoint = "https://example.org/oauth/google/token",
            clientId = "client",
            expiresAt = 1,
        )
        CaldavAccount(
            id = 1,
            uuid = "someone@example.org",
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
            password = encryption.encrypt(tokenData.serialize()),
        )
    }

    @Test
    fun aRejectedRefreshIsNotRetriedUntilTheUserSignsInAgain() {
        runBlocking {
            val account = accountWithExpiredToken()
            whenever(proxyAuthProvider.getAuthHeader()).thenReturn("Bearer jwt")
            whenever(oauthClient.refreshToken(any(), any(), any(), anyOrNull()))
                .thenAnswer { throw Exception("${TokenError.REFRESH_FAILED}: 401") }

            synchronizer.sync(account, hasPro = true)

            verify(oauthClient).refreshToken(any(), any(), any(), anyOrNull())
            assertTrue(account.isUnauthorized())

            synchronizer.sync(account, hasPro = true)
            synchronizer.sync(account, hasPro = true)

            verifyNoMoreInteractions(oauthClient)
        }
    }

    @Test
    fun aTransientTokenEndpointFailureIsRetriedOnTheNextSync() {
        runBlocking {
            val account = accountWithExpiredToken()
            whenever(proxyAuthProvider.getAuthHeader()).thenReturn("Bearer jwt")
            whenever(oauthClient.refreshToken(any(), any(), any(), anyOrNull()))
                .thenAnswer { throw Exception("Token endpoint unavailable: 429") }

            synchronizer.sync(account, hasPro = true)
            synchronizer.sync(account, hasPro = true)

            verify(oauthClient, times(2)).refreshToken(any(), any(), any(), anyOrNull())
        }
    }

    @Test
    fun anAccountAlreadyMarkedUnauthorizedNeverHitsTheTokenEndpoint() {
        runBlocking {
            val account = accountWithExpiredToken().apply {
                error = "${TokenError.REFRESH_FAILED}: Token has been expired or revoked."
            }

            synchronizer.sync(account, hasPro = true)

            verifyNoMoreInteractions(oauthClient)
            verify(caldavDao, times(0)).setError(any(), anyOrNull())
        }
    }
}
