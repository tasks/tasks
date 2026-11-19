package org.tasks.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavDiscoveryClient
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences
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

    private val events = mutableListOf<Pair<String, Map<String, Any>>>()

    private var analyticsFailure: Exception? = null

    private val analytics = object : Analytics {
        override val tasksPreferences: TasksPreferences
            get() = throw UnsupportedOperationException()
        override val analyticsEnabled = true

        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            analyticsFailure?.let { throw it }
            events.add(event to params.toMap())
        }

        override fun addTask(source: String) = Unit
        override fun completeTask(source: String) = Unit
        override fun setSku(sku: String?) = Unit
    }

    private val failingEncryption = object : Encryption {
        override suspend fun encrypt(text: String): String? = null
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

    @Test
    fun aMicrosoftAccountStoredUnderItsSignInNameMovesOntoTheSubject(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking {
                findMicrosoftAccount(eq("1234"), eq("alice@example.com"), isNull())
            } doReturn microsoftAccount(2L, "alice@example.com")
        }

        setupMicrosoftAccount(microsoftResult(), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) {
            setMicrosoftReauth(eq(2L), eq("1234"), eq("alice@example.com"), any())
        }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aMicrosoftAccountStoredUnderItsEmailMovesOntoTheSubject(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking {
                findMicrosoftAccount("1234", "alice@live.com", "alice@outlook.com")
            } doReturn microsoftAccount(2L, "alice@outlook.com")
        }

        setupMicrosoftAccount(
            microsoftResult(email = "alice@outlook.com", preferredUsername = "alice@live.com"),
            caldavDao,
            encryption,
            analytics,
        )

        verifyBlocking(caldavDao) {
            setMicrosoftReauth(eq(2L), eq("1234"), eq("alice@live.com"), any())
        }
    }

    @Test
    fun aNewMicrosoftAccountIsStoredUnderTheSubject(): Unit = runBlocking {
        val caldavDao = microsoftAccounts()

        setupMicrosoftAccount(microsoftResult(), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) {
            insert(argThat<CaldavAccount> {
                accountType == CaldavAccount.TYPE_MICROSOFT &&
                        username == "1234" &&
                        name == "alice@example.com" &&
                        password?.startsWith("encrypted(") == true
            })
        }
        assertEquals(listOf(accountAdded(Constants.SYNC_TYPE_MICROSOFT)), events)
    }

    @Test
    fun signingInToAnExistingMicrosoftAccountIsNotReportedAsAdded(): Unit = runBlocking {
        setupMicrosoftAccount(
            microsoftResult(),
            microsoftAccounts(microsoftAccount(3L, "1234")),
            encryption,
            analytics,
        )

        assertEquals(emptyList<Any>(), events)
    }

    @Test
    fun aMicrosoftResultWithoutASubjectIsRejected(): Unit = runBlocking {
        val caldavDao = microsoftAccounts(microsoftAccount(2L, "alice@example.com"))

        val failure = runCatching {
            setupMicrosoftAccount(microsoftResult(subject = null), caldavDao, encryption, analytics)
        }.exceptionOrNull()

        assertEquals("No subject in Microsoft OAuth response", failure?.message)
        verifyBlocking(caldavDao, never()) { setMicrosoftReauth(any(), any(), any(), any()) }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aMicrosoftTokenThatFailsToEncryptLeavesTheAccountAlone(): Unit = runBlocking {
        val caldavDao = microsoftAccounts(microsoftAccount(3L, "1234"))

        val failure = runCatching {
            setupMicrosoftAccount(microsoftResult(), caldavDao, failingEncryption, analytics)
        }.exceptionOrNull()

        assertNotNull(failure)
        verifyBlocking(caldavDao, never()) { setMicrosoftReauth(any(), any(), any(), any()) }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aFailureToReportANewMicrosoftAccountDoesNotFailTheSignIn(): Unit = runBlocking {
        val caldavDao = microsoftAccounts()

        analyticsFailure = IllegalStateException("analytics")

        setupMicrosoftAccount(microsoftResult(), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun overlappingMicrosoftSignInsCreateOneAccount(): Unit = runBlocking {
        var stored: CaldavAccount? = null
        val caldavDao = mock<CaldavDao> {
            onBlocking { findMicrosoftAccount(any(), anyOrNull(), anyOrNull()) } doAnswer { stored }
            onBlocking { insert(any<CaldavAccount>()) } doAnswer {
                Thread.sleep(100)
                stored = it.getArgument<CaldavAccount>(0).copy(id = 1L)
                1L
            }
        }

        List(2) {
            async(Dispatchers.Default) {
                setupMicrosoftAccount(microsoftResult(), caldavDao, encryption, analytics)
            }
        }.awaitAll()

        verify(caldavDao, times(1)).insert(any<CaldavAccount>())
    }

    @Test
    fun aNewGoogleTasksAccountIsStoredUnderItsEmail(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }

        setupGoogleTasksAccount(googleTasksResult(), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) {
            insert(argThat<CaldavAccount> {
                accountType == CaldavAccount.TYPE_GOOGLE_TASKS &&
                        username == "alice@example.com" &&
                        uuid == "alice@example.com" &&
                        password?.startsWith("encrypted(") == true
            })
        }
        assertEquals(listOf(accountAdded(Constants.SYNC_TYPE_GOOGLE_TASKS)), events)
    }

    @Test
    fun signingInToAnExistingGoogleTasksAccountReplacesItsTokensAndClearsTheError(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking {
                getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "alice@example.com")
            } doReturn CaldavAccount(id = 2, accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        }

        setupGoogleTasksAccount(googleTasksResult(), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) { setGoogleTasksReauth(eq(2L), any()) }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
        assertEquals(emptyList<Any>(), events)
    }

    @Test
    fun aMicrosoftGrantWithoutTasksReadWriteIsRejected(): Unit = runBlocking {
        assertMicrosoftGrantRejected("https://graph.microsoft.com/User.Read openid profile email")
    }

    @Test
    fun aReadOnlyMicrosoftGrantIsRejected(): Unit = runBlocking {
        assertMicrosoftGrantRejected("https://graph.microsoft.com/Tasks.Read openid profile email")
    }

    @Test
    fun aMicrosoftGrantIsMatchedRegardlessOfCase(): Unit = runBlocking {
        val caldavDao = microsoftAccounts()

        setupMicrosoftAccount(
            microsoftResult(grantedScopes = "https://graph.microsoft.com/tasks.readwrite openid"),
            caldavDao,
            encryption,
            analytics,
        )

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aMicrosoftResponseWithoutScopesIsAccepted(): Unit = runBlocking {
        val caldavDao = microsoftAccounts()

        setupMicrosoftAccount(microsoftResult(grantedScopes = null), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aGoogleTasksGrantWithoutTheTasksScopeIsRejected(): Unit = runBlocking {
        assertGoogleTasksGrantRejected("openid email")
    }

    @Test
    fun aReadOnlyGoogleTasksGrantIsRejected(): Unit = runBlocking {
        assertGoogleTasksGrantRejected("${GOOGLE_TASKS_SCOPE}.readonly openid email")
    }

    @Test
    fun aGoogleTasksResponseWithoutScopesIsAccepted(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }

        setupGoogleTasksAccount(googleTasksResult(grantedScopes = null), caldavDao, encryption, analytics)

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aGoogleTasksTokenThatFailsToEncryptLeavesTheAccountAlone(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking {
                getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "alice@example.com")
            } doReturn CaldavAccount(id = 2, accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        }

        val failure = runCatching {
            setupGoogleTasksAccount(googleTasksResult(), caldavDao, failingEncryption, analytics)
        }.exceptionOrNull()

        assertNotNull(failure)
        verifyBlocking(caldavDao, never()) { setGoogleTasksReauth(any(), any()) }
    }

    private suspend fun assertMicrosoftGrantRejected(grantedScopes: String) {
        val caldavDao = microsoftAccounts(microsoftAccount(2L, "1234"))

        val failure = runCatching {
            setupMicrosoftAccount(
                microsoftResult(grantedScopes = grantedScopes),
                caldavDao,
                encryption,
                analytics,
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        verifyBlocking(caldavDao, never()) { setMicrosoftReauth(any(), any(), any(), any()) }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    private suspend fun assertGoogleTasksGrantRejected(grantedScopes: String) {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
        }

        val failure = runCatching {
            setupGoogleTasksAccount(
                googleTasksResult(grantedScopes = grantedScopes),
                caldavDao,
                encryption,
                analytics,
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    private fun accountAdded(type: String) =
        AnalyticsEvents.SYNC_ADD_ACCOUNT to mapOf<String, Any>(AnalyticsEvents.PARAM_TYPE to type)

    private fun microsoftAccounts(existing: CaldavAccount? = null) = mock<CaldavDao> {
        onBlocking { findMicrosoftAccount(any(), anyOrNull(), anyOrNull()) } doReturn existing
        onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
    }

    private fun microsoftAccount(id: Long, username: String) = CaldavAccount(
        id = id,
        accountType = CaldavAccount.TYPE_MICROSOFT,
        username = username,
    )

    private fun microsoftResult(
        email: String? = null,
        preferredUsername: String? = "alice@example.com",
        subject: String? = "1234",
        grantedScopes: String? = "https://graph.microsoft.com/Tasks.ReadWrite https://graph.microsoft.com/User.Read openid profile email",
    ) = OAuthResult(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        clientId = "client-id",
        grantedScopes = grantedScopes,
        idToken = idToken(
            listOfNotNull(
                subject?.let { "\"sub\":\"$it\"" },
                email?.let { "\"email\":\"$it\"" },
                preferredUsername?.let { "\"preferred_username\":\"$it\"" },
            ).joinToString(",", "{", "}")
        ),
    )

    private fun googleTasksResult(
        grantedScopes: String? = "$GOOGLE_TASKS_SCOPE openid email",
    ) =
        OAuthResult(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            tokenEndpoint = "https://oauth2.googleapis.com/token",
            clientId = "client-id",
            idToken = idToken("""{"sub":"1234","email":"alice@example.com"}"""),
            grantedScopes = grantedScopes,
        )

    @OptIn(ExperimentalEncodingApi::class)
    private fun idToken(claims: String) = IdToken(
        "header." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
            .encode(claims.toByteArray()) + ".signature"
    )

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
