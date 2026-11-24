package org.tasks.googleapis

import com.google.api.client.http.HttpRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
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

class GoogleTasksAccountSynchronizerTest {

    private val encryption = KeyStoreEncryption(object : KeyProvider {
        private val key: SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun getKey() = key
    })
    private val caldavDao = mock<CaldavDao>()
    private val oauthClient = mock<TasksOAuthClient>()
    private val proxyAuthProvider = mock<ProxyAuthProvider>()
    private val refreshBroadcaster = mock<RefreshBroadcaster>()

    private val credentials = GoogleTasksCredentials(encryption, proxyAuthProvider, caldavDao, oauthClient)

    private val synchronizer = GoogleTasksAccountSynchronizer(
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
        createTask = { Task() },
        credentials = credentials,
        api = { GtasksInvoker(credentials.adapter(it)) },
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

    private var apiRequests = 0

    private fun deviceAccountSynchronizer() = GoogleTasksAccountSynchronizer(
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
        createTask = { Task() },
        credentials = object : GoogleTasksAccountAccess {
            override fun canAccess(account: CaldavAccount) = account.username == "someone@gmail.com"
            override fun needsSignIn(account: CaldavAccount) = false
        },
        api = {
            apiRequests++
            throw Exception("api requested")
        },
    )

    private val deviceAccount = CaldavAccount(
        id = 1,
        uuid = "someone@gmail.com",
        username = "someone@gmail.com",
        accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
    )

    @Test
    fun anAccountWithoutStoredTokensSyncsWhenThePlatformCanAccessIt() {
        runBlocking {
            deviceAccountSynchronizer().sync(deviceAccount.copy(), hasPro = true)

            assertEquals(1, apiRequests)
        }
    }

    @Test
    fun aDeviceAccountKeepsSyncingAfterAnUnauthorizedResponse() {
        runBlocking {
            val account = deviceAccount.copy(error = "401 Unauthorized")

            deviceAccountSynchronizer().sync(account, hasPro = true)

            assertEquals(1, apiRequests)
        }
    }

    @Test
    fun aFailureToBuildTheApiIsRecordedOnTheAccount() {
        runBlocking {
            val account = deviceAccount.copy()

            deviceAccountSynchronizer().sync(account, hasPro = true)

            assertEquals("api requested", account.error)
            verify(caldavDao).setError(1, "api requested")
            verify(refreshBroadcaster).broadcastRefresh()
        }
    }

    @Test
    fun aTokenFetchNetworkFailureIsRecordedAsANetworkError() {
        runBlocking {
            val account = deviceAccount.copy()
            val synchronizer = GoogleTasksAccountSynchronizer(
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
                createTask = { Task() },
                credentials = object : GoogleTasksAccountAccess {
                    override fun canAccess(account: CaldavAccount) = true
                    override fun needsSignIn(account: CaldavAccount) = false
                },
                api = {
                    GtasksInvoker(object : CredentialsAdapter {
                        override fun initialize(request: HttpRequest) = Unit
                        override suspend fun checkToken() = throw IOException("NetworkError")
                        override suspend fun invalidateToken() = true
                    })
                },
            )

            synchronizer.sync(account, hasPro = true)

            verify(caldavDao).setError(1, "NetworkError")
            assertFalse(account.isUnauthorized())
        }
    }
}
