package org.tasks.googleapis

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption
import java.util.concurrent.atomic.AtomicInteger

class GoogleTasksCredentialsTest : DatabaseTest() {

    private val plaintext = object : Encryption {
        override suspend fun encrypt(text: String): String = text
        override suspend fun decrypt(text: String?): String? = text
    }

    private val caldavDao = db.caldavDao()
    private val refreshes = AtomicInteger()
    private val proxyHeaders = mutableListOf<String?>()

    private val oauthClient = TasksOAuthClient(HttpClient(MockEngine { request ->
        refreshes.incrementAndGet()
        synchronized(proxyHeaders) { proxyHeaders += request.headers[HttpHeaders.Authorization] }
        delay(200)
        respond(
            """{"access_token":"refreshed-access-token","expires_in":3600,"refresh_token":"rotated"}""",
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }))

    private val credentials = GoogleTasksCredentials(
        encryption = plaintext,
        proxyAuthProvider = ProxyAuthProvider(caldavDao, mock()) { "proxy-jwt" },
        caldavDao = caldavDao,
        oauthClient = oauthClient,
    )

    private fun adapter(account: CaldavAccount) = credentials.adapter(account)

    private suspend fun insert(): CaldavAccount {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
            uuid = "account-uuid",
            username = "someone@gmail.com",
            password = OAuthTokenData(
                accessToken = "the-access-token",
                refreshToken = "the-refresh-token",
                tokenEndpoint = "https://oauth2.googleapis.com/token",
                clientId = "client-id",
                expiresAt = 4_000_000_000_000L,
            ).serialize(),
        )
        return account.copy(id = caldavDao.insert(account))
    }

    @Test
    fun adaptersForTheSameAccountShareOneRefreshOfARejectedToken() = runBlocking {
        val account = insert()
        val adapters = (1..2).map { adapter(account.copy()).apply { checkToken() } }

        adapters.map { async(Dispatchers.Default) { it.invalidateToken() } }.awaitAll()

        assertEquals(1, refreshes.get())
        assertEquals(listOf<String?>("Bearer proxy-jwt"), proxyHeaders)
        assertEquals(
            "rotated",
            OAuthTokenData.deserialize(caldavDao.getAccount(account.id)!!.password!!).refreshToken,
        )
    }

    @Test
    fun aRejectionOfTheTokenJustIssuedReportsThatNoNewTokenIsAvailable() = runBlocking {
        val adapter = adapter(insert()).apply { checkToken() }

        assertEquals(listOf(true, false), listOf(adapter.invalidateToken(), adapter.invalidateToken()))
        assertEquals(1, refreshes.get())
    }

    @Test
    fun adaptersForTheSameAccountDecryptItsCredentialOnce() = runBlocking {
        val account = insert()
        val decrypts = AtomicInteger()
        val counting = GoogleTasksCredentials(
            encryption = object : Encryption {
                override suspend fun encrypt(text: String): String = text
                override suspend fun decrypt(text: String?): String? = text.also { decrypts.incrementAndGet() }
            },
            proxyAuthProvider = ProxyAuthProvider(caldavDao, mock()) { "proxy-jwt" },
            caldavDao = caldavDao,
            oauthClient = oauthClient,
        )

        repeat(2) { counting.adapter(account.copy()).checkToken() }

        assertEquals(1, decrypts.get())
    }

    @Test
    fun anUnexpiredTokenIsReusedWithoutReadingTheDatabase() = runBlocking {
        val account = insert()
        val adapter = adapter(account).apply { checkToken() }
        caldavDao.setPassword(account.id, "unreadable")

        adapter.checkToken()

        val request = NetHttpTransport().createRequestFactory().buildGetRequest(GenericUrl("https://example.com"))
        adapter.initialize(request)
        assertEquals("Bearer the-access-token", request.headers.authorization)
        assertEquals(0, refreshes.get())
    }
}
