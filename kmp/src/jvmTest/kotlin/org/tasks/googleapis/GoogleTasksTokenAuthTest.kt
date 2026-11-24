package org.tasks.googleapis

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption

class GoogleTasksTokenAuthTest : DatabaseTest() {

    private val plaintext = object : Encryption {
        override suspend fun encrypt(text: String): String = text
        override suspend fun decrypt(text: String?): String? = text
    }

    private val caldavDao = db.caldavDao()
    private val refreshRequests = mutableListOf<Pair<String, Map<String, String?>>>()
    private val authHeaders = mutableListOf<String?>()

    private val tokenStore = OAuthTokenStore(
        encryption = plaintext,
        caldavDao = caldavDao,
        oauthClient = TasksOAuthClient(HttpClient(MockEngine { request ->
            val form = (request.body as FormDataContent).formData
            refreshRequests += request.url.toString() to form.names().associateWith { form[it] }
            authHeaders += request.headers[HttpHeaders.Authorization]
            respond(
                """{"access_token":"refreshed","expires_in":3600}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })),
        refreshWhenExpiryUnknown = false,
    )

    private suspend fun insert(expiresAt: Long = 4_000_000_000_000L): CaldavAccount {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
            uuid = "someone@gmail.com",
            username = "someone@gmail.com",
            password = OAuthTokenData(
                accessToken = "the-access-token",
                refreshToken = "the-refresh-token",
                tokenEndpoint = "https://oauth2.googleapis.com/token",
                clientId = "ios-client-id",
                expiresAt = expiresAt,
            ).serialize(),
        )
        return account.copy(id = caldavDao.insert(account))
    }

    @Test
    fun anUnexpiredTokenIsUsedWithoutRefreshing() = runBlocking {
        val auth = GoogleTasksTokenAuth(insert(), tokenStore)

        assertEquals("the-access-token", auth.accessToken())
        assertEquals(emptyList<Any>(), refreshRequests)
    }

    @Test
    fun anExpiredTokenRefreshesWithGoogleDirectly() = runBlocking {
        val auth = GoogleTasksTokenAuth(insert(expiresAt = 1), tokenStore)

        assertEquals("refreshed", auth.accessToken())
        assertEquals(
            listOf(
                "https://oauth2.googleapis.com/token" to mapOf(
                    "grant_type" to "refresh_token",
                    "client_id" to "ios-client-id",
                    "refresh_token" to "the-refresh-token",
                ),
            ),
            refreshRequests,
        )
        assertNull(authHeaders.single())
    }

    @Test
    fun aRejectedTokenIsReplaced() = runBlocking {
        val auth = GoogleTasksTokenAuth(insert(), tokenStore)

        val rejected = auth.accessToken()

        assertEquals(true, auth.invalidate(rejected))
        assertEquals("refreshed", auth.accessToken())
        assertEquals(1, refreshRequests.size)
    }

    @Test
    fun rejectingTheTokenJustIssuedReportsNoNewToken() = runBlocking {
        val auth = GoogleTasksTokenAuth(insert(), tokenStore)
        auth.invalidate(auth.accessToken())

        assertEquals(false, auth.invalidate(auth.accessToken()))
        assertEquals(1, refreshRequests.size)
    }

    @Test
    fun theRefreshedTokenIsSaved() = runBlocking {
        val account = insert(expiresAt = 1)

        GoogleTasksTokenAuth(account, tokenStore).accessToken()

        assertEquals(
            "refreshed",
            OAuthTokenData.deserialize(caldavDao.getAccount(account.id)!!.password!!).accessToken,
        )
    }
}
