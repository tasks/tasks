package org.tasks.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavDiscoveryClient
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AccountSetupTest {

    private val calls = mutableListOf<String>()

    private val discoveryClient = object : CaldavDiscoveryClient {
        override suspend fun homeSet(): String {
            calls.add("homeSet")
            return "https://caldav.example.com/caldav/google_1234/"
        }

        override fun close() = Unit
    }

    private val clientFactory = object : CaldavClientFactory {
        override suspend fun forUrl(url: String?, username: String?, password: String?) =
            discoveryClient

        override suspend fun forAccount(account: CaldavAccount, url: String?) =
            throw UnsupportedOperationException()

        override suspend fun forTasksAccount(account: CaldavAccount) =
            throw UnsupportedOperationException()

        override suspend fun forTasksCredentials(username: String, token: String) =
            throw UnsupportedOperationException()
    }

    private val encryption = object : Encryption {
        override suspend fun encrypt(text: String) = "encrypted($text)"
        override suspend fun decrypt(text: String?) = text
    }

    private var claim: HeldPurchaseClaim = HeldPurchaseClaim.Claimed

    private val purchaseClaim = PurchaseClaim { username, token ->
        calls.add("claim $username $token")
        claim
    }

    @Test
    fun aHeldPurchaseIsClaimedBeforeTheHomeSetIsAskedFor() = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }

        setupTasksAccount(
            oauthResult = oauthResult(),
            issuer = "google",
            caldavUrl = "https://caldav.example.com",
            caldavDao = caldavDao,
            encryption = encryption,
            provider = clientFactory,
            purchaseClaim = purchaseClaim,
        )

        assertEquals(listOf("claim google_1234 access-token", "homeSet"), calls)
    }

    @Test
    fun aClaimThatFoundNothingToMoveStillSignsIn() = runBlocking {
        claim = HeldPurchaseClaim.NotClaimed
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }

        setupTasksAccount(
            oauthResult = oauthResult(),
            issuer = "google",
            caldavUrl = "https://caldav.example.com",
            caldavDao = caldavDao,
            encryption = encryption,
            provider = clientFactory,
            purchaseClaim = purchaseClaim,
        )

        assertEquals(listOf("claim google_1234 access-token", "homeSet"), calls)
    }

    @Test
    fun aSubscriptionCoveringAnotherAccountStopsBeforeTheAccountIsCreated() = runBlocking {
        claim = HeldPurchaseClaim.CoveringAnotherAccount("b**@e**.com")
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
        }

        val thrown = runCatching {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = encryption,
                provider = clientFactory,
                purchaseClaim = purchaseClaim,
            )
        }.exceptionOrNull()

        verify(caldavDao, never()).insert(any<CaldavAccount>())
        assertEquals("b**@e**.com", (thrown as PurchaseTokenInUseException).existingAccount)
        assertEquals(listOf("claim google_1234 access-token"), calls)
    }

    @Test
    fun aReturningAccountNeedsNeitherAClaimNorAProbe() = runBlocking {
        val existing = CaldavAccount(
            id = 1,
            accountType = CaldavAccount.TYPE_TASKS,
            username = "google_1234",
            url = "https://caldav.example.com/caldav/google_1234/",
        )
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn existing
        }

        val account = setupTasksAccount(
            oauthResult = oauthResult(),
            issuer = "google",
            caldavUrl = "https://caldav.example.com",
            caldavDao = caldavDao,
            encryption = encryption,
            provider = clientFactory,
            purchaseClaim = purchaseClaim,
        )

        assertEquals(1L, account.id)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun overlappingSignInsCreateOneAccount() = runBlocking {
        var stored: CaldavAccount? = null
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doAnswer { stored }
            onBlocking { insert(any<CaldavAccount>()) } doAnswer {
                stored = it.getArgument<CaldavAccount>(0).copy(id = 1L)
                1L
            }
        }
        val slowFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient {
                    override suspend fun homeSet(): String {
                        delay(100)
                        return discoveryClient.homeSet()
                    }

                    override fun close() = Unit
                }
        }

        val accounts = List(2) {
            async(Dispatchers.Default) {
                setupTasksAccount(
                    oauthResult = oauthResult(),
                    issuer = "google",
                    caldavUrl = "https://caldav.example.com",
                    caldavDao = caldavDao,
                    encryption = encryption,
                    provider = slowFactory,
                    purchaseClaim = purchaseClaim,
                )
            }
        }.awaitAll()

        verify(caldavDao, times(1)).insert(any<CaldavAccount>())
        assertEquals(listOf(1L, 1L), accounts.map { it.id })
        assertEquals(listOf("claim google_1234 access-token", "homeSet"), calls)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun oauthResult() = OAuthResult(
        accessToken = "access-token",
        idToken = IdToken(
            "header." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
                .encode("""{"sub":"1234","email":"alice@example.com"}""".toByteArray()) +
                    ".signature"
        ),
    )
}
