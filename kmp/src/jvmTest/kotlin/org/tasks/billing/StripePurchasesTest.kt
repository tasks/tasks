package org.tasks.billing

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.analytics.AnalyticsEvents
import org.tasks.auth.TasksServerEnvironment
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.StripeSubscriptionResult
import org.tasks.caldav.TasksAccountClient
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.OkHttpKtorClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class StripePurchasesTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope
    private lateinit var server: MockWebServer

    private var checkedSku: String? = null
    private var checkedCreditCents: Int? = null

    private val json = Json { ignoreUnknownKeys = true }
    private val encryption = KeyStoreEncryption(object : KeyProvider {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun getKey(): SecretKey = key
    })
    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    private val imposterKey = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
        .private

    private val tasksAccount = CaldavAccount(
        uuid = "tasks-1",
        username = "alice",
        password = "s3cret",
        url = "https://caldav.example.com/caldav/alice/",
        accountType = CaldavAccount.TYPE_TASKS,
    )

    private var accounts = listOf(tasksAccount)
    private var claimResult: StripeSubscriptionResult = StripeSubscriptionResult.Claimed
    private var claims = 0
    private val credentialClaims = mutableListOf<Pair<String, String>>()
    private var failCredentialClaims = false
    private val changeReceipts = mutableListOf<String?>()
    private val portalReceipts = mutableListOf<String?>()
    private val cloudAnnual = StripePrice(
        priceId = "pri_30",
        sku = "annual_30",
        amountCents = 2999,
        entitles = true,
    )
    private var restore: StripeRestore = StripeRestore.Failed
    private var checkoutStatus: StripeCheckoutStatus = StripeCheckoutStatus.Pending
    private var change: StripeChange = StripeChange.Failed
    private val onboardingSteps = mutableListOf<String>()

    private val accountFlow = MutableSharedFlow<List<CaldavAccount>>(replay = 1)
    private var watchThrows = false
    private var signInAttempts = 0
    private var failAccountLookup = false

    private val caldavDao = mock<CaldavDao> {
        on { watchAccounts() } doAnswer {
            if (watchThrows) throw IllegalStateException("database closed") else accountFlow
        }
        onBlocking { getAccounts() } doAnswer {
            signInAttempts++
            if (failAccountLookup) throw IllegalStateException("database busy") else accounts
        }
        onBlocking { getAccounts(any<List<Int>>()) } doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val types = invocation.getArgument<List<Int>>(0)
            accounts.filter { it.accountType in types }
        }
    }

    private val accountClient = object : TasksAccountClient {
        override suspend fun generateNewPassword(description: String?) = null
        override suspend fun deletePassword(id: Int) = Unit
        override suspend fun registerPushToken(token: String, provider: String?) = Unit
        override suspend fun unregisterPushToken(token: String) = Unit
        override suspend fun getAccount(): String? = null
        override suspend fun regenerateInboundEmail() = null
        override suspend fun setInboundCalendar(calendar: String?) = null
        override suspend fun claimStripeSubscription(purchaseJwt: String): StripeSubscriptionResult {
            claims++
            return claimResult
        }
        override fun close() = Unit
    }

    private val clientFactory = object : CaldavClientFactory {
        override suspend fun forUrl(url: String?, username: String?, password: String?) =
            throw UnsupportedOperationException()
        override suspend fun forAccount(account: CaldavAccount, url: String?) =
            throw UnsupportedOperationException()
        override suspend fun forTasksAccount(account: CaldavAccount) = accountClient
        override suspend fun forTasksCredentials(username: String, token: String): TasksAccountClient {
            if (failCredentialClaims) {
                throw IOException("no route to host")
            }
            credentialClaims.add(username to token)
            return accountClient
        }
    }

    private val stripeClient = object : StripeClient {
        override suspend fun prices(): List<StripePrice>? = null
        override suspend fun createCheckout(priceId: String?) = StripeCheckout.Failed
        override suspend fun changePlan(priceId: String, purchaseJwt: String?): StripeChange {
            changeReceipts.add(purchaseJwt)
            return change
        }
        override suspend fun pollCheckout(code: String) = checkoutStatus
        override suspend fun requestRestoreCode(email: String) = StripeRestoreRequest.Sent
        override suspend fun verifyRestoreCode(email: String, code: String) = restore
        override suspend fun portalUrl(purchaseJwt: String?): String? {
            portalReceipts.add(purchaseJwt)
            return null
        }
    }

    private val tasksPreferences = TasksPreferences(InMemoryDataStore())

    private val httpClient = OkHttpClient()

    private val ktorClientFactory = OkHttpKtorClientFactory(object : OkHttpClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: (OkHttpClient.Builder) -> Unit,
        ) = httpClient.newBuilder().apply(block).build()
    })

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("stripe-purchases-test").toFile()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    checkedSku
                        ?.let {
                            MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody(
                                    """{"sku":"$it","formatted_price":"USD 29.99"""" +
                                        (checkedCreditCents?.let { cents ->
                                            ""","credit_cents":$cents,"credit_currency":"EUR""""
                                        } ?: "") + "}"
                                )
                        }
                        ?: MockResponse().setResponseCode(404)
            }
            start()
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        httpClient.dispatcher.cancelAll()
        server.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.dispatcher.executorService.shutdown()
        dir.deleteRecursively()
    }

    private fun entitlement() = PurchaseEntitlement(
        dataDir = dir.path,
        encryption = encryption,
        httpClientFactory = ktorClientFactory,
        serverEnvironment = mock<TasksServerEnvironment> {
            on { caldavUrl } doReturn server.url("/").toString().trimEnd('/')
        },
        scope = scope,
        json = json,
        syncAdapters = mock<SyncAdapters>(),
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded),
    )

    private fun purchases(entitlement: PurchaseEntitlement = entitlement()) = StripePurchases(
        client = stripeClient,
        entitlement = entitlement,
        caldavDao = caldavDao,
        clientFactory = clientFactory,
        accountData = TasksAccountDataRepository(null, caldavDao, tasksPreferences),
        tasksPreferences = tasksPreferences,
        logOnboarding = { onboardingSteps.add(it) },
    )

    private fun purchase(
        key: PrivateKey = keyPair.private,
        sku: String = "monthly_03",
    ) = StripePurchase(
        purchaseJwt = jwt(key),
        refreshToken = "refresh-1",
        sku = sku,
        formattedPrice = "USD 3.00",
    )

    @Test
    fun aKeptPurchaseUnlocksThisDevice() = runBlocking {
        val entitlement = entitlement()

        val outcome = purchases(entitlement).keep(purchase())

        assertEquals(StripeRestoreOutcome.Restored, outcome)
        assertTrue(entitlement.hasPro.first())
        assertEquals("monthly_03", entitlement.sku.first())
        assertEquals(EntitlementProvider.STRIPE, entitlement.provider.first())
    }

    @Test
    fun aKeptPurchaseFundsTheSignedInAccount() = runBlocking {
        purchases().keep(purchase())

        assertEquals(1, claims)
    }

    @Test
    fun aPurchaseThatCannotBeStoredIsNeverClaimed() = runBlocking {
        val entitlement = entitlement()

        val outcome = purchases(entitlement).keep(purchase(key = imposterKey))

        assertEquals(StripeRestoreOutcome.StorageFailed, outcome)
        assertEquals(0, claims)
        assertFalse(entitlement.hasPro.first())
    }

    @Test
    fun aSubscriptionBelowTheCloudFloorAsksForAnUpgrade() = runBlocking {
        claimResult = StripeSubscriptionResult.InsufficientSubscription

        assertEquals(StripeRestoreOutcome.NeedsUpgrade, purchases().keep(purchase()))
    }

    @Test
    fun aSubscriptionFundingAnotherAccountNamesIt() = runBlocking {
        claimResult = StripeSubscriptionResult.TokenInUse("b**@e**.com")

        assertEquals(
            StripeRestoreOutcome.AccountInUse("b**@e**.com"),
            purchases().keep(purchase()),
        )
    }

    @Test
    fun aClaimThatFailsStillLeavesTheAppUnlocked() = runBlocking {
        claimResult = StripeSubscriptionResult.Unavailable
        val entitlement = entitlement()

        assertEquals(StripeRestoreOutcome.Restored, purchases(entitlement).keep(purchase()))
        assertTrue(entitlement.hasPro.first())
    }

    @Test
    fun withNoAccountThereIsNothingToClaimOnto() = runBlocking {
        accounts = emptyList()
        val entitlement = entitlement()

        assertEquals(StripeRestoreOutcome.Restored, purchases(entitlement).keep(purchase()))
        assertEquals(0, claims)
        assertTrue(entitlement.hasPro.first())
    }

    @Test
    fun aPlanChangeClaimsTheHeldPurchaseFirst() = runBlocking {
        val purchases = purchases()
        purchases.keep(purchase(sku = "annual_04"))
        claims = 0
        change = StripeChange.Changed(entitles = true)

        purchases.changePlan(cloudAnnual)

        assertEquals(1, claims)
    }

    @Test
    fun aPlanChangeSendsTheReceiptItHolds() = runBlocking {
        val entitlement = entitlement()
        val purchases = purchases(entitlement)
        purchases.keep(purchase(sku = "annual_04"))
        changeReceipts.clear()
        change = StripeChange.Changed(entitles = true)

        purchases.changePlan(cloudAnnual)

        assertEquals(listOf(entitlement.getJwt()), changeReceipts)
    }

    @Test
    fun aPlanChangeWithNothingHeldSendsNoReceipt() = runBlocking {
        changeReceipts.clear()
        change = StripeChange.Changed(entitles = true)

        purchases().changePlan(cloudAnnual)

        assertEquals(listOf<String?>(null), changeReceipts)
    }

    @Test
    fun aRedeemedCodeIsKept() = runBlocking {
        restore = StripeRestore.Restored(purchase())
        val entitlement = entitlement()

        val result = purchases(entitlement).redeemRestoreCode("alice@example.com", "123456")

        assertTrue(result is StripeRestore.Restored)
        assertTrue(entitlement.hasPro.first())
    }

    @Test
    fun aRejectedCodeKeepsNothing() = runBlocking {
        restore = StripeRestore.InvalidCode
        val entitlement = entitlement()

        val result = purchases(entitlement).redeemRestoreCode("alice@example.com", "000000")

        assertEquals(StripeRestore.InvalidCode, result)
        assertFalse(entitlement.hasPro.first())
    }

    @Test
    fun aCollectedCheckoutIsKept() = runBlocking {
        checkoutStatus = StripeCheckoutStatus.Complete(purchase())
        val entitlement = entitlement()

        val result = purchases(entitlement).collectCheckout("c0de")

        assertEquals(StripeCollection.Collected(StripeRestoreOutcome.Restored), result)
        assertTrue(entitlement.hasPro.first())
    }

    @Test
    fun aServerThatCannotAnswerIsWorthPollingAgain() = runBlocking {
        val purchases = purchases()

        checkoutStatus = StripeCheckoutStatus.Unavailable
        assertEquals(StripeCollection.Pending, purchases.collectCheckout("c0de"))

        checkoutStatus = StripeCheckoutStatus.Failed
        assertEquals(StripeCollection.Pending, purchases.collectCheckout("c0de"))
    }

    @Test
    fun aRateLimitedPollCarriesTheWaitBack() = runBlocking {
        checkoutStatus = StripeCheckoutStatus.RateLimited(retryAfterMillis = 20_000L)

        assertEquals(
            StripeCollection.RateLimited(retryAfterMillis = 20_000L),
            purchases().collectCheckout("c0de"),
        )
    }

    @Test
    fun aCloudPurchaseWithNoAccountHandsOverToTheCelebration() = runBlocking {
        accounts = emptyList()
        checkoutStatus = StripeCheckoutStatus.Complete(purchase(sku = "monthly_03"))

        purchases().collectCheckout("c0de")

        assertTrue(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
        assertEquals(listOf(AnalyticsEvents.CloudOnboarding.TRIGGERED), onboardingSteps)
    }

    @Test
    fun anUpgradeToCloudWithNoAccountHandsOverToTheCelebration() = runBlocking {
        accounts = emptyList()
        change = StripeChange.Changed(entitles = true)

        purchases().changePlan(cloudAnnual)

        assertTrue(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
    }

    @Test
    fun aTierThatMovedWhileWeWereAwayStartsNoCelebration() = runBlocking {
        accounts = emptyList()
        val entitlement = entitlement()
        val purchases = purchases(entitlement)
        purchases.keep(purchase(sku = "annual_04"))
        checkedSku = "annual_30"

        assertEquals(StripePlan("annual_30"), purchases.currentPlan())

        assertFalse(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
        assertTrue(onboardingSteps.isEmpty())
    }

    @Test
    fun theCurrentPlanCarriesWhatTheCheckSaidAboutIt() = runBlocking {
        accounts = emptyList()
        val entitlement = entitlement()
        val purchases = purchases(entitlement)
        purchases.keep(purchase(sku = "annual_04"))
        checkedSku = "annual_30"
        checkedCreditCents = 900

        val plan = purchases.currentPlan()

        assertEquals("annual_30", plan?.sku)
        assertEquals(900, plan?.terms?.creditCents)
        assertEquals("EUR", plan?.terms?.creditCurrency)
    }

    @Test
    fun asignedInPlanCarriesWhatTheAccountSaidAboutIt() = runBlocking {
        tasksPreferences.set(
            TasksPreferences.cachedAccountData,
            """{"subscriptions":[{"provider":"stripe","isActive":true,"product":"annual_30",""" +
                """"trial_end":1760000000000}],"credit_cents":900,"credit_currency":"USD"}""",
        )

        val plan = purchases().currentPlan()

        assertEquals("annual_30", plan?.sku)
        assertEquals(1760000000000L, plan?.terms?.trialEnd)
        assertEquals(900, plan?.terms?.creditCents)
        assertEquals("USD", plan?.terms?.creditCurrency)
    }

    @Test
    fun aCloudTrialIsNotHiddenByASupporterRowsStatus() = runBlocking {
        val trialEnd = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        tasksPreferences.set(
            TasksPreferences.cachedAccountData,
            """{"subscriptions":[{"provider":"stripe","isActive":true,"product":"annual_30",""" +
                """"trial_end":$trialEnd}],"supporter":[{"provider":"stripe",""" +
                """"product":"annual_01","status":"active"}]}""",
        )

        val plan = purchases().currentPlan()

        assertEquals(trialEnd, plan?.trialEnd)
    }

    @Test
    fun aChangeReadsTheNewTierBackWithoutSpendingTheToken() = runBlocking {
        val entitlement = entitlement()
        val purchases = purchases(entitlement)
        purchases.keep(purchase(sku = "annual_04"))
        checkedSku = "annual_30"
        change = StripeChange.Changed(entitles = true)

        purchases.changePlan(cloudAnnual)

        assertEquals("annual_30", entitlement.sku.first())
        assertEquals("/desktop/check", server.takeRequest(5, TimeUnit.SECONDS)?.path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun theCustomerPortalIsOpenedOnTheReceiptWeHold() = runBlocking {
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )

        purchases(entitlement).portalUrl()

        assertEquals(listOf(entitlement.getJwt()), portalReceipts)
    }

    @Test
    fun aPortalForSomeoneHoldingNothingFallsBackToTheSession() = runBlocking {
        purchases().portalUrl()

        assertEquals(listOf(null), portalReceipts)
    }

    @Test
    fun aDowngradeBelowTheCloudFloorCelebratesNothing() = runBlocking {
        accounts = emptyList()
        change = StripeChange.Changed(entitles = false)

        purchases().changePlan(cloudAnnual)

        assertFalse(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
    }

    @Test
    fun someoneAlreadySignedInNeedsNoCelebration() = runBlocking {
        checkoutStatus = StripeCheckoutStatus.Complete(purchase(sku = "monthly_03"))

        purchases().collectCheckout("c0de")

        assertFalse(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
        assertTrue(onboardingSteps.isEmpty())
    }

    @Test
    fun aSupporterTierBuysNoCloudSoThereIsNothingToSignInFor() = runBlocking {
        accounts = emptyList()
        checkoutStatus = StripeCheckoutStatus.Complete(purchase(sku = "annual_01"))

        purchases().collectCheckout("c0de")

        assertFalse(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
    }

    @Test
    fun aRestoreIsNotAPurchaseAndStartsNoCelebration() = runBlocking {
        accounts = emptyList()
        restore = StripeRestore.Restored(purchase(sku = "monthly_03"))

        purchases().redeemRestoreCode("alice@example.com", "123456")

        assertFalse(tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false))
    }

    @Test
    fun anExpiredCodeIsNotWorthPollingAgain() = runBlocking {
        checkoutStatus = StripeCheckoutStatus.Gone

        assertEquals(StripeCollection.Gone, purchases().collectCheckout("c0de"))
    }

    @Test
    fun withNothingStoredThereIsNothingToClaimAtSignIn() = runBlocking {
        assertNull(purchases().claimStoredPurchase())
        assertEquals(0, claims)
    }

    @Test
    fun aHeldPurchaseIsClaimedAtSignIn() = runBlocking {
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )
        claims = 0

        assertEquals(StripeSubscriptionResult.Claimed, purchases(entitlement).claimStoredPurchase())
    }

    @Test
    fun aSignInWhoseEmailDidNotMatchStillGetsTheJwt() = runBlocking {
        tasksPreferences.set(TasksPreferences.cachedAccountData, """{"subscriptions":[]}""")
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )
        claims = 0

        assertEquals(StripeSubscriptionResult.Claimed, purchases(entitlement).claimStoredPurchase())
        assertEquals(1, claims)
    }

    @Test
    fun anAccountThatAlreadyHasOneIsLeftAlone() = runBlocking {
        tasksPreferences.set(
            TasksPreferences.cachedAccountData,
            """{"subscriptions":[{"provider":"stripe","isActive":true}]}""",
        )
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )
        claims = 0

        assertNull(purchases(entitlement).claimStoredPurchase())
        assertEquals(0, claims)
    }

    @Test
    fun aHeldPurchaseIsClaimedBeforeTheSignInAsksForAHomeSet() = runBlocking {
        accounts = emptyList()
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )
        claims = 0

        purchases(entitlement).claimHeldPurchase("google_1234", "access-token")

        assertEquals(1, claims)
        assertEquals(listOf("google_1234" to "access-token"), credentialClaims)
    }

    @Test
    fun aSignInWithNothingHeldClaimsNothing() = runBlocking {
        purchases().claimHeldPurchase("google_1234", "access-token")

        assertEquals(0, claims)
        assertTrue(credentialClaims.isEmpty())
    }

    @Test
    fun aFailedSignInClaimIsNotFatal() = runBlocking {
        val entitlement = entitlement()
        entitlement.storeEntitlement(
            jwt = jwt(),
            refreshToken = "refresh-1",
            provider = EntitlementProvider.STRIPE,
        )
        failCredentialClaims = true

        purchases(entitlement).claimHeldPurchase("google_1234", "access-token")
    }

    @Test
    fun aSignedInAccountIsClaimedOntoAsSoonAsItAppears() = runBlocking {
        purchases(storedPurchase()).claimOnSignIn(scope)

        accountFlow.emit(listOf(tasksAccount))

        assertEquals(1, await { claims >= 1 }.let { claims })
    }

    @Test
    fun aFailedClaimDoesNotStopUsWatchingForTheNextSignIn() = runBlocking {
        failAccountLookup = true
        purchases(storedPurchase()).claimOnSignIn(scope)

        accountFlow.emit(listOf(tasksAccount))
        await { signInAttempts >= 1 }
        failAccountLookup = false
        accountFlow.emit(listOf(tasksAccount.copy(uuid = "tasks-2")))

        await { claims >= 1 }
        assertEquals(1, claims)
    }

    @Test
    fun aDatabaseThatIsGoneTakesTheWatcherDownQuietly() = runBlocking {
        watchThrows = true
        val uncaught = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, e -> uncaught.add(e) }
        val watched = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)

        purchases().claimOnSignIn(watched)
        withTimeout(5_000) { watched.coroutineContext.job.children.toList().joinAll() }

        assertTrue(uncaught.isEmpty())
        watched.cancel()
    }

    private suspend fun storedPurchase(): PurchaseEntitlement {
        tasksPreferences.set(TasksPreferences.cachedAccountData, """{"subscriptions":[]}""")
        claims = 0
        return entitlement().also {
            it.storeEntitlement(
                jwt = jwt(),
                refreshToken = "refresh-1",
                provider = EntitlementProvider.STRIPE,
            )
        }
    }

    private suspend fun await(done: () -> Boolean) {
        withTimeout(5_000) {
            while (!done()) {
                delay(10)
            }
        }
    }

    private fun jwt(key: PrivateKey = keyPair.private): String {
        val exp = System.currentTimeMillis() / 1000 + 7 * 24 * 60 * 60
        val header = encode("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
        val payload = encode("""{"iss":"tasks.org","exp":$exp}""".toByteArray())
        val signed = "$header.$payload"
        return "$signed.${encode(rawSignature(signed, key))}"
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun rawSignature(signed: String, key: PrivateKey): ByteArray {
        val der = Signature.getInstance("SHA256withECDSA").run {
            initSign(key)
            update(signed.toByteArray(Charsets.US_ASCII))
            sign()
        }
        var offset = 2
        val rLength = der[offset + 1].toInt()
        val r = der.copyOfRange(offset + 2, offset + 2 + rLength).trimmed()
        offset += 2 + rLength
        val sLength = der[offset + 1].toInt()
        val s = der.copyOfRange(offset + 2, offset + 2 + sLength).trimmed()
        return ByteArray(64).also {
            r.copyInto(it, 32 - r.size)
            s.copyInto(it, 64 - s.size)
        }
    }

    private fun ByteArray.trimmed(): ByteArray {
        var start = 0
        while (start < size - 1 && this[start] == 0.toByte()) start++
        return copyOfRange(start, size)
    }
}
