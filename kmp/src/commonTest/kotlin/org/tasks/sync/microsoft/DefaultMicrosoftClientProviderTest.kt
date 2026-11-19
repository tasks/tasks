package org.tasks.sync.microsoft

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.CookieBackend
import org.tasks.http.PersistentCookiesStorage
import org.tasks.http.UnauthorizedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultMicrosoftClientProviderTest {

    private class RecordingStorage {
        var deleted = false
        val scope = CoroutineScope(SupervisorJob())
        val storage = PersistentCookiesStorage(
            object : CookieBackend {
                override suspend fun read() = emptyList<String>()

                override suspend fun write(cookies: List<String>) = true

                override suspend fun delete() {
                    deleted = true
                }
            },
            scope,
        )
        val closed get() = !scope.isActive
    }

    private val sentTokens = mutableListOf<String?>()
    private val timeouts = mutableListOf<Triple<Long?, Long?, Long?>>()
    private val stores = mutableMapOf<String, MutableList<RecordingStorage>>()
    private val tokens = mutableMapOf<String, Int>()
    private val refreshes = mutableListOf<String?>()
    private val engines = mutableListOf<MockEngine>()
    private var liveAccounts = setOf<Long>(1, 2)
    private var accountLookups = 0
    private var unauthorized = false
    private var lookupFails = false

    private fun newEngine() = MockEngine { request ->
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
        sentTokens += token
        request.getCapabilityOrNull(HttpTimeoutCapability)?.let {
            timeouts += Triple(it.requestTimeoutMillis, it.connectTimeoutMillis, it.socketTimeoutMillis)
        }
        if (unauthorized && token?.endsWith("-refreshed") != true) {
            respond(
                """{"error":{"code":"InvalidAuthenticationToken","message":"no"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        } else {
            respond("")
        }
    }

    private inner class Tokens(
        private val canRefresh: Boolean = false,
        private val credentials: Boolean? = null,
    ) : MicrosoftTokenSource {
        override suspend fun getToken(account: CaldavAccount): String {
            val count = tokens.getOrElse(account.uuid!!) { 0 } + 1
            tokens[account.uuid!!] = count
            return "${account.uuid}-$count"
        }

        override suspend fun refreshToken(account: CaldavAccount, rejected: String?): String? {
            refreshes += rejected
            return if (canRefresh) "${account.uuid}-refreshed" else null
        }

        override suspend fun hasCredentials(account: CaldavAccount): Boolean =
            credentials ?: super.hasCredentials(account)
    }

    private fun provider(tokenSource: MicrosoftTokenSource = Tokens()) = DefaultMicrosoftClientProvider(
        engine = { newEngine().also { engines += it } },
        cookiesStorage = { key -> RecordingStorage().also { stores.getOrPut(key) { mutableListOf() } += it }.storage },
        tokens = tokenSource,
        accountIds = {
            accountLookups++
            if (lookupFails) throw IllegalStateException("database closed")
            liveAccounts
        },
    )

    @Test
    fun accountsWithoutAUsernameDoNotShareAClient() = runTest {
        val provider = provider()
        val a = provider.getService(CaldavAccount(id = 1, uuid = "a", username = null))
        provider.getService(CaldavAccount(id = 2, uuid = "b", username = null))

        a.deleteList("list")

        assertEquals(listOf<String?>("a-1"), sentTokens)
        assertEquals(setOf("a", "b"), stores.keys)
    }

    @Test
    fun cookiesAreKeyedByUsername() = runTest {
        provider().getService(CaldavAccount(id = 1, uuid = "a", username = "user@example.com"))

        assertEquals(setOf("user@example.com"), stores.keys)
    }

    @Test
    fun anAccountKeepsItsClientButSendsTheLatestToken() = runTest {
        val provider = provider()
        val account = CaldavAccount(id = 1, uuid = "a", username = "user")
        val first = provider.getService(account)
        provider.getService(account)

        first.deleteList("list")

        assertEquals(listOf<String?>("a-2"), sentTokens)
        assertEquals(1, stores.getValue("user").size)
    }

    @Test
    fun aRefreshUsesTheAccountFromTheLatestGetService() = runTest {
        unauthorized = true
        val refreshedFor = mutableListOf<String?>()
        val source = object : MicrosoftTokenSource {
            override suspend fun getToken(account: CaldavAccount) = "token"

            override suspend fun refreshToken(account: CaldavAccount, rejected: String?): String {
                refreshedFor += account.username
                return "token-refreshed"
            }
        }
        val provider = provider(source)
        val first = provider.getService(CaldavAccount(id = 1, uuid = "a", username = "user@example.com"))
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "subject"))

        first.deleteList("list")

        assertEquals(listOf<String?>("subject"), refreshedFor)
    }

    @Test
    fun accountsSharingAUsernameKeepTheirOwnClients() = runTest {
        val provider = provider()
        val a = provider.getService(CaldavAccount(id = 1, uuid = "a", username = "user"))
        val b = provider.getService(CaldavAccount(id = 2, uuid = "b", username = "user"))

        a.deleteList("list")
        b.deleteList("list")

        assertEquals(listOf<String?>("a-1", "b-1"), sentTokens)
        assertTrue(stores.getValue("user").none { it.closed })
    }

    @Test
    fun aRemovedAccountsClientIsClosedWhenAnotherAccountSyncs() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "alice"))
        liveAccounts = setOf(2)

        provider.getService(CaldavAccount(id = 2, uuid = "b", username = "bob"))

        assertTrue(stores.getValue("alice").single().closed)
        assertFalse(stores.getValue("bob").single().closed)
    }

    @Test
    fun aRemovedAccountsCookiesAreDeleted() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "alice"))
        liveAccounts = setOf(2)

        provider.getService(CaldavAccount(id = 2, uuid = "b", username = "bob"))

        assertTrue(stores.getValue("alice").single().deleted)
        assertFalse(stores.getValue("bob").single().deleted)
    }

    @Test
    fun aFailedAccountLookupStillProvidesAClient() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "alice"))
        lookupFails = true

        provider.getService(CaldavAccount(id = 2, uuid = "b", username = "bob")).deleteList("list")

        assertEquals(listOf<String?>("b-1"), sentTokens)
        assertFalse(stores.getValue("alice").single().deleted)
    }

    @Test
    fun aCachedClientDoesNotLookUpAccounts() = runTest {
        val provider = provider()
        val account = CaldavAccount(id = 1, uuid = "a", username = "user")
        provider.getService(account)
        provider.getService(CaldavAccount(id = 2, uuid = "b", username = "other"))
        val lookups = accountLookups

        provider.getService(account)

        assertEquals(lookups, accountLookups)
    }

    @Test
    fun anAccountWithANewUsernameKeepsItsClient() = runTest {
        val provider = provider()
        val first = provider.getService(CaldavAccount(id = 1, uuid = "a", username = "user@example.com"))

        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "subject"))
        first.deleteList("list")

        assertEquals(listOf<String?>("a-2"), sentTokens)
        assertFalse(stores.getValue("user@example.com").single().closed)
        assertEquals(setOf("user@example.com"), stores.keys)
    }

    @Test
    fun everyAccountSharesOneEngine() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "alice"))
        provider.getService(CaldavAccount(id = 2, uuid = "b", username = "bob"))

        assertEquals(1, engines.size)
    }

    @Test
    fun closingTheProviderClosesTheEngine() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "user"))

        provider.close()

        assertFalse(engines.single().coroutineContext.isActive)
    }

    @Test
    fun closingTheProviderClosesCookieStorage() = runTest {
        val provider = provider()
        provider.getService(CaldavAccount(id = 1, uuid = "a", username = "user"))

        provider.close()

        assertTrue(stores.getValue("user").single().closed)
    }

    @Test
    fun anAccountGetsAWorkingClientAfterTheProviderCloses() = runTest {
        val provider = provider()
        val account = CaldavAccount(id = 1, uuid = "a", username = "user")
        provider.getService(account)
        provider.close()

        provider.getService(account).deleteList("list")

        assertEquals(listOf<String?>("a-2"), sentTokens)
        assertFalse(stores.getValue("user").last().closed)
        assertEquals(2, engines.size)
    }

    @Test
    fun graphRequestsWaitThirtySecondsForEveryTimeout() = runTest {
        provider()
            .getService(CaldavAccount(id = 1, uuid = "a", username = "user"))
            .deleteList("list")

        assertEquals(listOf(Triple<Long?, Long?, Long?>(30_000, 30_000, 30_000)), timeouts)
    }

    @Test
    fun aRejectedTokenIsRefreshedAndKeptForLaterRequests() = runTest {
        unauthorized = true
        val service = provider(Tokens(canRefresh = true))
            .getService(CaldavAccount(id = 1, uuid = "a", username = "user"))

        service.deleteList("list")
        service.deleteList("list")

        assertEquals(listOf<String?>("a-1"), refreshes)
        assertEquals(listOf<String?>("a-1", "a-refreshed", "a-refreshed"), sentTokens)
    }

    @Test
    fun aTokenSourceThatCannotRefreshFailsOnTheFirstRejection() = runTest {
        unauthorized = true
        val service = provider()
            .getService(CaldavAccount(id = 1, uuid = "a", username = "user"))

        val thrown = runCatching { service.deleteList("list") }.exceptionOrNull()

        assertTrue(thrown is UnauthorizedException)
        assertEquals(listOf<String?>("a-1"), sentTokens)
    }

    @Test
    fun credentialsDefaultToAStoredPassword() = runTest {
        val provider = provider()

        assertTrue(provider.hasCredentials(CaldavAccount(password = "secret")))
        assertFalse(provider.hasCredentials(CaldavAccount(password = null)))
    }

    @Test
    fun theTokenSourceDecidesWhetherThereAreCredentials() = runTest {
        val provider = provider(Tokens(credentials = false))

        assertFalse(provider.hasCredentials(CaldavAccount(password = "secret")))
    }
}
