package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import org.tasks.security.Encryption
import org.tasks.time.DateTimeUtils2
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.io.IOException

class OAuthTokenStoreTest : DatabaseTest() {

    private val plaintext = object : Encryption {
        override suspend fun encrypt(text: String): String = text
        override suspend fun decrypt(text: String?): String? = text
    }

    private val caldavDao = db.caldavDao()
    private val refreshes = AtomicInteger()
    private val sentRefreshTokens = mutableListOf<String>()

    private val tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
    private val clientId = "9d4babd5-e7ba-4286-ba4b-17274495a901"
    private val farFuture = 4_000_000_000_000L

    private fun stored(
        accessToken: String = "the-access-token",
        refreshToken: String = "the-refresh-token",
        expiresAt: Long = farFuture,
    ) = OAuthTokenData(accessToken, refreshToken, tokenEndpoint, clientId, expiresAt).serialize()

    private fun refreshResponse(accessToken: String, refreshToken: String? = null) =
        """{"access_token":"$accessToken","expires_in":3600""" +
            (refreshToken?.let { ""","refresh_token":"$it"""" } ?: "") + "}"

    private fun oauthClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        },
    ) = TasksOAuthClient(HttpClient(MockEngine { request ->
        refreshes.incrementAndGet()
        val form = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        synchronized(sentRefreshTokens) {
            sentRefreshTokens += form.substringAfter("refresh_token=").substringBefore('&')
        }
        handler(request)
    }))

    private fun MockRequestHandleScope.respondJson(body: String) =
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))

    private fun store(
        client: TasksOAuthClient = oauthClient(),
        encryption: Encryption = plaintext,
    ) = OAuthTokenStore(encryption, caldavDao, client)

    private suspend fun insert(password: String?): CaldavAccount {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_MICROSOFT,
            uuid = "account-uuid",
            username = "someone",
            password = password,
        )
        return account.copy(id = caldavDao.insert(account))
    }

    private suspend fun storedPassword(account: CaldavAccount) =
        caldavDao.getAccount(account.id)?.password

    @After
    fun resetClock() {
        DateTimeUtils2.setCurrentMillisSystem()
    }

    private fun Throwable.chain() = generateSequence(this) { it.cause }

    private fun asksToSignInAgain(error: Throwable) =
        CaldavAccount(error = error.message).isUnauthorized()

    @Test
    fun anUnreadableCredentialIsReportedAndAsksToSignInAgainWithoutCarryingItsContents() = runBlocking {
        val account = insert("""{"refreshToken": "the-secret-refresh-token", """)

        val thrown = runCatching { store().getToken(account) }.exceptionOrNull()!!

        assertFalse(thrown is UnauthorizedException)
        assertTrue(asksToSignInAgain(thrown))
        assertFalse(thrown.chain().any { it.message.orEmpty().contains("the-secret-refresh-token") })
    }

    @Test
    fun anUndecryptableCredentialIsReportedAndAsksToSignInAgain() = runBlocking {
        val account = insert(stored())
        val broken = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = null
        }

        val thrown = runCatching { store(encryption = broken).getToken(account) }.exceptionOrNull()!!

        assertFalse(thrown is UnauthorizedException)
        assertTrue(asksToSignInAgain(thrown))
    }

    @Test
    fun anUnavailableKeyKeepsRetryingInsteadOfAskingToSignInAgain() = runBlocking {
        val account = insert(stored())
        val keyUnavailable = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? =
                throw IllegalStateException("Encryption key unavailable, refusing to replace key for existing data")
        }

        val thrown = runCatching { store(encryption = keyUnavailable).getToken(account) }.exceptionOrNull()!!

        assertFalse(asksToSignInAgain(thrown))
    }

    @Test
    fun anUnexpiredTokenWithoutARefreshTokenIsStillUsed() = runBlocking {
        val account = insert(stored(refreshToken = ""))

        assertEquals("the-access-token", store().getToken(account))
        assertEquals(0, refreshes.get())
    }

    @Test
    fun anExpiredTokenWithoutARefreshTokenAsksToSignInAgain() = runBlocking {
        val account = insert(stored(refreshToken = "", expiresAt = 1))

        val thrown = runCatching { store().getToken(account) }.exceptionOrNull()!!

        assertTrue(thrown is IOException)
        assertTrue(asksToSignInAgain(thrown))
        assertEquals(0, refreshes.get())
    }

    @Test
    fun missingCredentialsAskToSignInAgainWithoutBeingReported() = runBlocking {
        val account = insert(null)

        val thrown = runCatching { store().getToken(account) }.exceptionOrNull()!!

        assertTrue(thrown is IOException)
        assertTrue(asksToSignInAgain(thrown))
    }

    @Test
    fun aRefusedRefreshAndAnUnreadableCredentialAreDifferentTypes() = runBlocking {
        val unreadable = runCatching { store().getToken(insert("{")) }.exceptionOrNull()!!

        assertFalse(unreadable is IOException)
    }

    @Test
    fun aTokenIssuedByARejectionIsNotRefreshedAgainWhenItIsAlsoRejected() = runBlocking {
        val account = insert(stored())
        val store = store()

        val first = store.getToken(account, rejected = "the-access-token")
        val second = store.getToken(account, rejected = first)

        assertEquals("refreshed-access-token", second)
        assertEquals(1, refreshes.get())
    }

    @Test
    fun aTokenIssuedByARejectionIsRefreshedWhenRejectedAfterTheInterval() = runBlocking {
        val account = insert(stored())
        val store = store(oauthClient {
            respondJson(refreshResponse("refreshed-${refreshes.get()}", "rotated-refresh-token"))
        })
        DateTimeUtils2.setCurrentMillisFixed(1_000_000_000_000L)
        val first = store.getToken(account, rejected = "the-access-token")

        DateTimeUtils2.setCurrentMillisFixed(1_000_000_000_000L + OAuthTokenStore.FORCED_REFRESH_INTERVAL_MS - 1)
        val withinInterval = store.getToken(account, rejected = first)
        DateTimeUtils2.setCurrentMillisFixed(1_000_000_000_000L + OAuthTokenStore.FORCED_REFRESH_INTERVAL_MS)
        val afterInterval = store.getToken(account, rejected = withinInterval)

        assertEquals(listOf("refreshed-1", "refreshed-1", "refreshed-2"), listOf(first, withinInterval, afterInterval))
    }

    @Test
    fun aTokenSignedInAgainAfterARefreshIsRefreshedWhenRejectedWithinTheInterval() = runBlocking {
        val account = insert(stored())
        val store = store(oauthClient {
            respondJson(refreshResponse("refreshed-${refreshes.get()}", "rotated-refresh-token"))
        })
        store.getToken(account, rejected = "the-access-token")
        caldavDao.setPassword(account.id, stored(accessToken = "signed-in-again", refreshToken = "new-refresh-token"))

        val token = store.getToken(account, rejected = "signed-in-again")

        assertEquals("refreshed-2", token)
        assertEquals(listOf("the-refresh-token", "new-refresh-token"), sentRefreshTokens)
    }

    @Test
    fun locksAreReleasedOnceNoRefreshHoldsThem() = runBlocking {
        val account = insert(stored())

        val store = store()

        store.getToken(account, rejected = "the-access-token")

        assertEquals(0, store.lockCount())
    }

    @Test
    fun detachedAccountsOfDifferentTypesWithTheSameUsernameDoNotShareALock() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val blocked = CompletableDeferred<Unit>()
        val store = store(oauthClient {
            if (refreshes.get() == 1) {
                blocked.complete(Unit)
                gate.await()
            }
            respondJson(refreshResponse("refreshed-access-token"))
        })
        val detached = { type: Int ->
            CaldavAccount(accountType = type, username = "someone@example.com", password = stored(expiresAt = 1))
        }
        val microsoft = async(Dispatchers.Default) { store.getToken(detached(CaldavAccount.TYPE_MICROSOFT)) }
        blocked.await()

        val google = withTimeout(5_000) { store.getToken(detached(CaldavAccount.TYPE_GOOGLE_TASKS)) }
        gate.complete(Unit)

        assertEquals("refreshed-access-token", google)
        assertEquals("refreshed-access-token", microsoft.await())
    }

    @Test
    fun manyRejectionsWithinTheIntervalRefreshOnce() = runBlocking {
        val account = insert(stored())
        val store = store()

        var token = "the-access-token"
        repeat(10) { token = store.getToken(account, rejected = token) }

        assertEquals(1, refreshes.get())
    }

    @Test
    fun aTokenIssuedByARejectionInOneStoreIsRefreshedWhenAnotherStoreSeesItRejected() = runBlocking {
        val account = insert(stored())

        val first = store().getToken(account, rejected = "the-access-token")
        store().getToken(account, rejected = first)

        assertEquals(2, refreshes.get())
    }

    @Test
    fun aTokenIssuedByARejectionIsStillRefreshedWhenItExpires() = runBlocking {
        val account = insert(stored())
        val issued = store().getToken(account, rejected = "the-access-token")
        caldavDao.setPassword(
            account.id,
            stored(accessToken = issued, refreshToken = "rotated-refresh-token", expiresAt = 1),
        )

        store().getToken(account, rejected = issued)

        assertEquals(2, refreshes.get())
    }

    @Test
    fun aTokenRefreshedByExpiryIsRefreshedOnceWhenRejected() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val store = store(oauthClient {
            respondJson(refreshResponse("refreshed-${refreshes.get()}", "rotated-refresh-token"))
        })

        val expired = store.getToken(account)
        val rejected = store.getToken(account, rejected = expired)
        val rejectedAgain = store.getToken(account, rejected = rejected)

        assertEquals(listOf("refreshed-1", "refreshed-2", "refreshed-2"), listOf(expired, rejected, rejectedAgain))
    }

    @Test
    fun aStaleRewriteDuringARefreshDoesNotDiscardTheRefreshedToken() = runBlocking {
        val account = insert(stored())
        val sameTokenRewritten = stored() + " "
        val client = oauthClient {
            caldavDao.setPassword(account.id, sameTokenRewritten)
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        }

        val token = store(client).getToken(account, rejected = "the-access-token")

        assertEquals("refreshed-access-token", token)
        assertEquals("refreshed-access-token", OAuthTokenData.deserialize(storedPassword(account)!!).accessToken)
        assertEquals(storedPassword(account), account.password)
    }

    @Test
    fun anExpiredRewriteDuringARefreshDoesNotDiscardTheRefreshedToken() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val client = oauthClient {
            caldavDao.setPassword(account.id, stored(accessToken = "also-expired", expiresAt = 1))
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        }

        assertEquals("refreshed-access-token", store(client).getToken(account))
        assertEquals("rotated-refresh-token", OAuthTokenData.deserialize(storedPassword(account)!!).refreshToken)
    }

    @Test
    fun anUnchangedCredentialIsDecryptedOnce() = runBlocking {
        val account = insert(stored())
        val decrypts = AtomicInteger()
        val counting = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = text.also { decrypts.incrementAndGet() }
        }
        val store = store(encryption = counting)

        repeat(3) { store.getToken(account) }

        assertEquals(1, decrypts.get())
    }

    @Test
    fun aRefreshedCredentialIsNotDecryptedAgain() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val decrypts = AtomicInteger()
        val counting = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = text.also { decrypts.incrementAndGet() }
        }
        val store = store(encryption = counting)

        store.getToken(account)
        store.getToken(account)

        assertEquals(1, decrypts.get())
    }

    @Test
    fun aRefusedRefreshAsksToSignInAgain() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val client = oauthClient {
            respond(
                """{"error":"invalid_grant","error_description":"AADSTS70000"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val thrown = runCatching { store(client).getToken(account) }.exceptionOrNull()!!

        assertTrue(asksToSignInAgain(thrown))
    }

    @Test
    fun anExpiredTokenIsRefreshedAndTheRotatedRefreshTokenIsStored() = runBlocking {
        val account = insert(stored(expiresAt = 1))

        val token = store().getToken(account)

        assertEquals("refreshed-access-token", token)
        assertEquals(listOf("the-refresh-token"), sentRefreshTokens)
        val saved = OAuthTokenData.deserialize(storedPassword(account)!!)
        assertEquals("refreshed-access-token", saved.accessToken)
        assertEquals("rotated-refresh-token", saved.refreshToken)
        assertEquals(storedPassword(account), account.password)
    }

    @Test
    fun aRefreshThatDoesNotRotateKeepsTheOldRefreshToken() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val client = oauthClient { respondJson(refreshResponse("refreshed-access-token")) }

        store(client).getToken(account)

        assertEquals("the-refresh-token", OAuthTokenData.deserialize(storedPassword(account)!!).refreshToken)
    }

    @Test
    fun concurrentCallsRefreshOnce() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val store = store(oauthClient {
            delay(200)
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        })

        val tokens = (1..2).map {
            async(Dispatchers.Default) { store.getToken(account.copy()) }
        }.awaitAll()

        assertEquals(listOf("refreshed-access-token", "refreshed-access-token"), tokens)
        assertEquals(1, refreshes.get())
    }

    @Test
    fun aRejectedUnexpiredTokenIsRefreshed() = runBlocking {
        val account = insert(stored())

        val token = store().getToken(account, rejected = "the-access-token")

        assertEquals("refreshed-access-token", token)
        assertEquals(1, refreshes.get())
    }

    @Test
    fun aRejectionOfATokenAlreadyReplacedDoesNotRefreshAgain() = runBlocking {
        val account = insert(stored(accessToken = "newer-access-token"))

        val token = store().getToken(account, rejected = "older-access-token")

        assertEquals("newer-access-token", token)
        assertEquals(0, refreshes.get())
    }

    @Test
    fun aSignInDuringARefreshIsNotOverwritten() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val signedInAgain = stored(accessToken = "signed-in-again", refreshToken = "new-chain")
        val client = oauthClient {
            caldavDao.setPassword(account.id, signedInAgain)
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        }
        val store = store(client)

        val token = store.getToken(account)

        assertEquals("signed-in-again", token)
        assertEquals(signedInAgain, account.password)
        assertEquals(signedInAgain, storedPassword(account))
        assertEquals("signed-in-again", store.getToken(account))
    }

    @Test
    fun aSignInWithAnUnknownExpiryDuringARefreshIsNotOverwritten() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val signedInAgain = stored(accessToken = "signed-in-again", refreshToken = "new-chain", expiresAt = 0)
        val client = oauthClient {
            caldavDao.setPassword(account.id, signedInAgain)
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        }

        store(client).getToken(account)

        assertEquals(signedInAgain, storedPassword(account))
    }

    @Test
    fun anUnreadableRewriteDuringARefreshIsReplacedWithTheRefreshedToken() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val client = oauthClient {
            caldavDao.setPassword(account.id, "{")
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        }

        assertEquals("refreshed-access-token", store(client).getToken(account))
        assertEquals("rotated-refresh-token", OAuthTokenData.deserialize(storedPassword(account)!!).refreshToken)
    }

    @Test
    fun aPasswordChangedInTheDatabaseIsUsedInsteadOfTheCache() = runBlocking {
        val account = insert(stored())
        val store = store()
        store.getToken(account)

        caldavDao.setPassword(account.id, stored(accessToken = "replaced-access-token"))

        assertEquals("replaced-access-token", store.getToken(account))
    }

    @Test
    fun refreshesOfOneAccountAreSerialisedWhenItsUsernameChanges() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        val store = store(oauthClient {
            delay(200)
            respondJson(refreshResponse("refreshed-access-token", "rotated-refresh-token"))
        })

        val tokens = listOf("someone", "renamed").map { username ->
            async(Dispatchers.Default) { store.getToken(account.copy(username = username)) }
        }.awaitAll()

        assertEquals(listOf("refreshed-access-token", "refreshed-access-token"), tokens)
        assertEquals(1, refreshes.get())
    }

    @Test
    fun aProxyAuthHeaderIsSentWithTheRefresh() = runBlocking {
        val account = insert(stored(expiresAt = 1))
        var sentHeader: String? = null
        val client = oauthClient {
            sentHeader = it.headers[HttpHeaders.Authorization]
            respondJson(refreshResponse("refreshed-access-token"))
        }

        OAuthTokenStore(plaintext, caldavDao, client, authHeader = { "Bearer proxy-jwt" })
            .getToken(account)

        assertEquals("Bearer proxy-jwt", sentHeader)
    }

    @Test
    fun anUnknownExpiryIsRefreshedOnlyWhenAskedTo() = runBlocking {
        val account = insert(stored(expiresAt = 0))

        val kept = OAuthTokenStore(plaintext, caldavDao, oauthClient(), refreshWhenExpiryUnknown = false)
            .getToken(account)

        assertEquals("the-access-token", kept)
        assertEquals(0, refreshes.get())
        assertEquals("refreshed-access-token", store().getToken(account))
    }
}
