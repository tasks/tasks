package org.tasks.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
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
import org.tasks.fcm.PushTokenManager
import org.tasks.http.HttpException
import org.tasks.preferences.TasksPreferences
import org.tasks.security.Encryption
import java.io.IOException
import java.util.Collections
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AccountSetupTest {

    private val calls: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private var probeFailure: Exception? = null

    private var accessFailure: Exception? = null

    private fun discoveryClient(url: String?) = object : CaldavDiscoveryClient {
        override suspend fun homeSet(): String {
            calls.add("homeSet $url")
            probeFailure?.let { throw it }
            return HOME_SET
        }

        override suspend fun checkAccess() {
            calls.add("checkAccess $url")
            accessFailure?.let { throw it }
            probeFailure?.let { throw it }
        }

        override fun close() = Unit
    }

    private val clientFactory = object : CaldavClientFactory {
        override suspend fun forUrl(url: String?, username: String?, password: String?) =
            discoveryClient(url)

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

    private val pushTokenManager = mock<PushTokenManager>()

    private val analytics = object : Analytics {
        override val tasksPreferences: TasksPreferences
            get() = throw UnsupportedOperationException()
        override val analyticsEnabled = true

        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
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
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
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
            pushTokenManager = pushTokenManager,
            analytics = analytics,
        )

        assertEquals(listOf("claim google_1234 access-token", "homeSet https://caldav.example.com"), calls)
    }

    @Test
    fun aClaimThatFoundNothingToMoveStillSignsIn() = runBlocking {
        claim = HeldPurchaseClaim.NotClaimed
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
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
            pushTokenManager = pushTokenManager,
            analytics = analytics,
        )

        assertEquals(listOf("claim google_1234 access-token", "homeSet https://caldav.example.com"), calls)
    }

    @Test
    fun aSubscriptionCoveringAnotherAccountStopsBeforeTheAccountIsCreated() = runBlocking {
        claim = HeldPurchaseClaim.CoveringAnotherAccount("b**@e**.com")
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
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
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }.exceptionOrNull()

        verify(caldavDao, never()).insert(any<CaldavAccount>())
        assertEquals("b**@e**.com", (thrown as PurchaseTokenInUseException).existingAccount)
        assertEquals(listOf("claim google_1234 access-token"), calls)
    }

    @Test
    fun aReturningAccountChecksItsStoredHomeSetWithOneRequest() = runBlocking {
        val caldavDao = returningAccount()

        val account = signInAgain(caldavDao)

        assertEquals(1L, account.id)
        assertEquals(listOf("claim google_1234 access-token", "checkAccess $STORED_URL"), calls)
    }

    @Test
    fun aReturningAccountTakesTheNewTokenAndKeepsItsHomeSet() = runBlocking {
        val caldavDao = returningAccount()

        val account = signInAgain(caldavDao)

        verifyBlocking(caldavDao) { setVerified(1L, STORED_URL, "encrypted(access-token)") }
        verifyBlocking(caldavDao, never()) { update(any<CaldavAccount>()) }
        assertEquals("", account.error)
        assertEquals("encrypted(access-token)", account.password)
        assertEquals(STORED_URL, account.url)
    }

    @Test
    fun aReturningAccountWhoseHomeSetMovedFindsItAgain() = runBlocking {
        val caldavDao = returningAccount()
        accessFailure = HttpException(404, "Not Found")

        val account = signInAgain(caldavDao)

        assertEquals(
            listOf(
                "claim google_1234 access-token",
                "checkAccess $STORED_URL",
                "homeSet https://caldav.example.com",
            ),
            calls,
        )
        verifyBlocking(caldavDao) { setVerified(1L, HOME_SET, "encrypted(access-token)") }
        assertEquals(HOME_SET, account.url)
        assertEquals("", account.error)
    }

    @Test
    fun aReturningAccountWithoutAHomeSetFindsOne() = runBlocking {
        val caldavDao = returningAccount(url = null)

        val account = signInAgain(caldavDao)

        assertEquals(
            listOf("claim google_1234 access-token", "homeSet https://caldav.example.com"),
            calls,
        )
        assertEquals(HOME_SET, account.url)
    }

    @Test
    fun aReturningAccountThatLapsedWhileItsHomeSetMovedSurfacesThe402() = runBlocking {
        val caldavDao = returningAccount()
        accessFailure = HttpException(404, "Not Found")
        probeFailure = HttpException(402, "Payment Required")

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertTrue(failure is PaymentRequiredException)
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", CaldavAccount.ERROR_PAYMENT_REQUIRED)
        }
    }

    @Test
    fun aReturningSubscriptionCoveringAnotherAccountStillSignsIn() = runBlocking {
        claim = HeldPurchaseClaim.CoveringAnotherAccount("b**@e**.com")
        val caldavDao = returningAccount()

        val account = signInAgain(caldavDao)

        assertEquals("", account.error)
        verifyBlocking(caldavDao) { setVerified(1L, STORED_URL, "encrypted(access-token)") }
        verifyBlocking(caldavDao, never()) { setPasswordAndError(any(), any(), any()) }
        assertEquals(listOf("claim google_1234 access-token", "checkAccess $STORED_URL"), calls)
    }

    @Test
    fun aLapsedReturningAccountKeepsTheNewTokenAndSurfacesThe402() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = HttpException(402, "Payment Required")

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertTrue(failure is PaymentRequiredException)
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", CaldavAccount.ERROR_PAYMENT_REQUIRED)
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun aReturningAccountWhosePurchaseFundsAnotherAccountIsRejected() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = PurchaseTokenInUseException("b**@e**.com")

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertEquals("b**@e**.com", (failure as PurchaseTokenInUseException).existingAccount)
        verifyBlocking(caldavDao) {
            setPasswordAndError(
                1L,
                "encrypted(access-token)",
                "${CaldavAccount.ERROR_PURCHASE_TOKEN_IN_USE}b**@e**.com",
            )
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun aReturningAccountTheServerRejectsKeepsItsOldToken() = runBlocking {
        assertRejectedWithoutChange(HttpException(401, "Unauthorized"))
    }

    @Test
    fun aReturningAccountTheServerNoLongerKnowsKeepsItsOldToken() = runBlocking {
        assertRejectedWithoutChange(HttpException(404, "Not Found"))
    }

    @Test
    fun aReturningAccountTheServerForbidsKeepsItsOldToken() = runBlocking {
        assertRejectedWithoutChange(HttpException(403, "Forbidden"))
    }

    @Test
    fun aReturningAccountWithAnUnexpectedFailureSignsInAndRecordsIt() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = IllegalStateException("home set not found")

        val account = signInAgain(caldavDao)

        assertEquals("encrypted(access-token)", account.password)
        assertEquals("home set not found", account.error)
        assertEquals(STORED_URL, account.url)
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", "home set not found")
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
        verify(pushTokenManager, never()).registerTokenForAccount(any())
    }

    @Test
    fun aReturningAccountInterruptedFromInsideTheSignInFailsButKeepsTheNewToken(): Unit = runBlocking {
        val caldavDao = returningAccount()
        val cancellation = CancellationException("Parent job is Cancelled")
        probeFailure = cancellation

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertTrue(failure is SignInInterruptedException)
        assertSame(cancellation, failure?.cause)
        verifyBlocking(caldavDao) { setPasswordAndError(1L, "encrypted(access-token)", "") }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun aNewAccountInterruptedFromInsideTheSignInFailsWithoutBeingAdded(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
        }
        probeFailure = CancellationException("Parent job is Cancelled")

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertTrue(failure is SignInInterruptedException)
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
    }

    @Test
    fun aReturningAccountWithAnUnexpectedHttpFailureSignsInAndRecordsIt() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = HttpException(400, "Bad Request")

        val account = signInAgain(caldavDao)

        assertEquals("HTTP 400 - Bad Request", account.error)
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", "HTTP 400 - Bad Request")
        }
    }

    @Test
    fun aReturningAccountThatMustAcceptTheTermsSignsInAndAsksForThem() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = HttpException(451, "Unavailable For Legal Reasons")

        val account = signInAgain(caldavDao)

        assertEquals("encrypted(access-token)", account.password)
        assertEquals("HTTP 451", account.error)
        assertTrue(account.isTosRequired())
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", "HTTP 451")
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun aDifferentAccountIsRejectedWhenAlreadySignedIn() = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn listOf(
                CaldavAccount(
                    id = 2,
                    accountType = CaldavAccount.TYPE_TASKS,
                    username = "github_5678",
                    name = "bob@example.com",
                )
            )
        }

        val failure = runCatching {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = encryption,
                provider = clientFactory,
                purchaseClaim = purchaseClaim,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }.exceptionOrNull()

        failure as WrongAccountException
        assertEquals("bob@example.com", failure.existingAccount)
        assertEquals(OAuthProvider.GITHUB, failure.provider)
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
        verifyBlocking(caldavDao, never()) { setPassword(any(), anyOrNull()) }
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun aRejectionForAnUnnamedAccountNamesNoAccount() = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn listOf(
                CaldavAccount(
                    id = 2,
                    accountType = CaldavAccount.TYPE_TASKS,
                    username = "github_5678",
                    name = " ",
                )
            )
        }

        val failure = runCatching {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = encryption,
                provider = clientFactory,
                purchaseClaim = purchaseClaim,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }.exceptionOrNull()

        assertNull((failure as WrongAccountException).existingAccount)
    }

    @Test
    fun aReturningAccountSignsInWhenTheServerIsDown() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = HttpException(503, "Service Unavailable")

        val account = signInAgain(caldavDao)

        assertEquals("encrypted(access-token)", account.password)
        assertEquals("HTTP 503 - Service Unavailable", account.error)
        assertEquals(STORED_URL, account.url)
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", "HTTP 503 - Service Unavailable")
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun aReturningAccountSignsInOfflineAndRecordsWhyItCouldNotCheck() = runBlocking {
        val caldavDao = returningAccount()
        probeFailure = IOException("Unable to resolve host")

        val account = signInAgain(caldavDao)

        assertEquals("encrypted(access-token)", account.password)
        assertEquals("Unable to resolve host", account.error)
        assertFalse(account.isLoggedOut())
        verifyBlocking(caldavDao) {
            setPasswordAndError(1L, "encrypted(access-token)", "Unable to resolve host")
        }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    @Test
    fun overlappingReturningSignInsTakeTurns(): Unit = runBlocking {
        val checkReached = CompletableDeferred<Unit>()
        val releaseCheck = CompletableDeferred<Unit>()
        val blockedFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient by discoveryClient(url) {
                    override suspend fun checkAccess() {
                        checkReached.complete(Unit)
                        releaseCheck.await()
                    }
                }
        }
        val first = async(Dispatchers.Default) {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = returningAccount(),
                encryption = encryption,
                provider = blockedFactory,
                purchaseClaim = null,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }
        val second = try {
            withTimeout(5_000) { checkReached.await() }
            val second = async(Dispatchers.Default) { signInAgain(returningAccount()) }
            delay(200)

            assertFalse(second.isCompleted)
            assertEquals(emptyList<String>(), calls)
            second
        } finally {
            releaseCheck.complete(Unit)
        }
        first.await()

        assertEquals("", withTimeout(5_000) { second.await() }.error)
        assertEquals(listOf("claim google_1234 access-token", "checkAccess $STORED_URL"), calls)
    }

    @Test
    fun aSignInCancelledWhileWaitingItsTurnLeavesTheAccountAlone(): Unit = runBlocking {
        val checkReached = CompletableDeferred<Unit>()
        val releaseCheck = CompletableDeferred<Unit>()
        val blockedFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient by discoveryClient(url) {
                    override suspend fun checkAccess() {
                        checkReached.complete(Unit)
                        releaseCheck.await()
                    }
                }
        }
        val first = async(Dispatchers.Default) {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = returningAccount(),
                encryption = encryption,
                provider = blockedFactory,
                purchaseClaim = null,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }
        val waitingDao = returningAccount()
        try {
            withTimeout(5_000) { checkReached.await() }
            val waiting = launch(start = CoroutineStart.UNDISPATCHED) { signInAgain(waitingDao) }

            withTimeout(5_000) { waiting.cancelAndJoin() }
        } finally {
            releaseCheck.complete(Unit)
        }
        first.await()

        verifyBlocking(waitingDao, never()) { getAccount(any<Int>(), any()) }
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun aReturningAccountWithABlankHomeSetFindsOne() = runBlocking {
        val caldavDao = returningAccount(url = "")

        val account = signInAgain(caldavDao)

        assertEquals(
            listOf("claim google_1234 access-token", "homeSet https://caldav.example.com"),
            calls,
        )
        verifyBlocking(caldavDao) { setVerified(1L, HOME_SET, "encrypted(access-token)") }
        assertEquals(HOME_SET, account.url)
    }

    private suspend fun assertRejectedWithoutChange(rejection: Exception) {
        val caldavDao = returningAccount()
        probeFailure = rejection

        val failure = runCatching { signInAgain(caldavDao) }.exceptionOrNull()

        assertEquals(rejection, failure)
        verifyBlocking(caldavDao, never()) { setPasswordAndError(any(), any(), any()) }
        verifyBlocking(caldavDao, never()) { setVerified(any(), any(), any()) }
    }

    private suspend fun signInAgain(caldavDao: CaldavDao) = setupTasksAccount(
        oauthResult = oauthResult(),
        issuer = "google",
        caldavUrl = "https://caldav.example.com",
        caldavDao = caldavDao,
        encryption = encryption,
        provider = clientFactory,
        purchaseClaim = purchaseClaim,
        pushTokenManager = pushTokenManager,
        analytics = analytics,
    )

    private fun returningAccount(url: String? = STORED_URL) = mock<CaldavDao> {
        onBlocking { getAccount(any<Int>(), any()) } doReturn CaldavAccount(
            id = 1,
            accountType = CaldavAccount.TYPE_TASKS,
            username = "google_1234",
            url = url,
            error = CaldavAccount.ERROR_UNAUTHORIZED,
        )
    }

    @Test
    fun cancellingAReturningSignInMidCheckStillStoresTheTokenAndRegistersForPush() = runBlocking {
        val caldavDao = returningAccount()
        val probing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val gatedFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient by discoveryClient(url) {
                    override suspend fun checkAccess() {
                        probing.complete(Unit)
                        release.await()
                    }
                }
        }

        val attempt = launch(Dispatchers.Default) {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = encryption,
                provider = gatedFactory,
                purchaseClaim = purchaseClaim,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }
        probing.await()
        attempt.cancel()
        release.complete(Unit)
        attempt.join()

        verifyBlocking(caldavDao) { setVerified(1L, STORED_URL, "encrypted(access-token)") }
        verify(pushTokenManager).registerTokenForAccount(any())
        assertEquals(emptyList<Pair<String, Map<String, Any>>>(), events)
    }

    @Test
    fun cancellingANewSignInMidSetupStillAddsRegistersAndReportsTheAccount() = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doReturn null
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }
        val probing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val gatedFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient by discoveryClient(url) {
                    override suspend fun homeSet(): String {
                        probing.complete(Unit)
                        release.await()
                        return HOME_SET
                    }
                }
        }

        val attempt = launch(Dispatchers.Default) {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = encryption,
                provider = gatedFactory,
                purchaseClaim = purchaseClaim,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }
        probing.await()
        attempt.cancel()
        release.complete(Unit)
        attempt.join()

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
        verify(pushTokenManager).registerTokenForAccount(any())
        assertEquals(listOf(accountAdded(Constants.SYNC_TYPE_TASKS_ORG)), events)
    }

    @Test
    fun cancellingAMicrosoftSignInMidWriteStillAddsAndReportsTheAccount(): Unit = runBlocking {
        val writing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val caldavDao = mock<CaldavDao> {
            onBlocking { findMicrosoftAccount(any(), anyOrNull(), anyOrNull()) } doSuspendableAnswer {
                writing.complete(Unit)
                release.await()
                null
            }
            onBlocking { insert(any<CaldavAccount>()) } doReturn 1L
        }

        val attempt = launch(Dispatchers.Default) {
            setupMicrosoftAccount(microsoftResult(), caldavDao, encryption, analytics)
        }
        writing.await()
        attempt.cancel()
        release.complete(Unit)
        attempt.join()

        verifyBlocking(caldavDao) { insert(any<CaldavAccount>()) }
        assertEquals(listOf(AnalyticsEvents.SYNC_ADD_ACCOUNT), events.map { it.first })
    }

    @Test
    fun overlappingSignInsCreateOneAccount() = runBlocking {
        var stored: CaldavAccount? = null
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccount(any<Int>(), any()) } doAnswer { stored }
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
            onBlocking { insert(any<CaldavAccount>()) } doAnswer {
                stored = it.getArgument<CaldavAccount>(0).copy(id = 1L)
                1L
            }
        }
        val slowFactory = object : CaldavClientFactory by clientFactory {
            override suspend fun forUrl(url: String?, username: String?, password: String?) =
                object : CaldavDiscoveryClient by discoveryClient(url) {
                    override suspend fun homeSet(): String {
                        delay(100)
                        return discoveryClient(url).homeSet()
                    }
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
                    pushTokenManager = pushTokenManager,
                    analytics = analytics,
                )
            }
        }.awaitAll()

        verify(caldavDao, times(1)).insert(any<CaldavAccount>())
        assertEquals(listOf(1L, 1L), accounts.map { it.id })
        assertEquals(
            listOf(
                "checkAccess $HOME_SET",
                "claim google_1234 access-token",
                "claim google_1234 access-token",
                "homeSet https://caldav.example.com",
            ),
            calls.sorted(),
        )
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
    fun aTasksTokenThatFailsToEncryptLeavesTheAccountAlone(): Unit = runBlocking {
        val caldavDao = returningAccount()

        val failure = runCatching {
            setupTasksAccount(
                oauthResult = oauthResult(),
                issuer = "google",
                caldavUrl = "https://caldav.example.com",
                caldavDao = caldavDao,
                encryption = failingEncryption,
                provider = clientFactory,
                purchaseClaim = purchaseClaim,
                pushTokenManager = pushTokenManager,
                analytics = analytics,
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        verifyBlocking(caldavDao, never()) { getAccount(any<Int>(), any()) }
        verifyBlocking(caldavDao, never()) { setPasswordAndError(any(), any(), any()) }
        assertEquals(emptyList<String>(), calls)
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

        verifyBlocking(caldavDao) { setPasswordAndError(eq(2L), any(), eq("")) }
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
        verifyBlocking(caldavDao, never()) { setPasswordAndError(any(), any(), any()) }
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

    @Test
    fun aNewDeviceGoogleAccountIsAddedAndLogged() = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccounts(any<List<Int>>()) } doReturn emptyList()
        }

        setupGoogleTasksAccount("someone@gmail.com", caldavDao, analytics)

        verifyBlocking(caldavDao) {
            insert(
                CaldavAccount(
                    accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
                    uuid = "someone@gmail.com",
                    name = "someone@gmail.com",
                    username = "someone@gmail.com",
                )
            )
        }
        assertEquals(
            listOf(
                AnalyticsEvents.SYNC_ADD_ACCOUNT to
                    mapOf(AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_GOOGLE_TASKS)
            ),
            events,
        )
    }

    @Test
    fun aReturningDeviceGoogleAccountClearsItsErrorAndResyncs() = runBlocking {
        val existing = CaldavAccount(
            id = 7,
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
            uuid = "Someone@gmail.com",
            username = "Someone@gmail.com",
            error = "401 Unauthorized",
        )
        val caldavDao = mock<CaldavDao> {
            onBlocking { getAccounts(listOf(CaldavAccount.TYPE_GOOGLE_TASKS)) } doReturn listOf(existing)
        }

        setupGoogleTasksAccount("someone@gmail.com", caldavDao, analytics)

        verifyBlocking(caldavDao) { update(existing.copy(error = "")) }
        verifyBlocking(caldavDao) { resetLastSync("Someone@gmail.com") }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
        assertTrue(events.isEmpty())
    }
}

private const val HOME_SET = "https://caldav.example.com/caldav/google_1234/"

private const val STORED_URL = "https://caldav.example.com/caldav/stored_google_1234/"
