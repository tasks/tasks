package org.tasks.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.analytics.AnalyticsEvents
import org.tasks.auth.TasksServerEnvironment
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.StripeSubscriptionResult
import org.tasks.caldav.TasksAccountClient
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.KtorClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import java.io.File
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class StripePurchasesTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope

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
    private var checkoutStatus: StripeCheckoutStatus = StripeCheckoutStatus.Pending
    private val onboardingSteps = mutableListOf<String>()

    private val caldavDao = mock<CaldavDao> {
        onBlocking { getAccounts() } doAnswer { accounts }
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
    }

    private val stripeClient = object : StripeClient {
        override suspend fun prices(): List<StripePrice>? = null
        override suspend fun createCheckout(priceId: String?) = StripeCheckout.Failed
        override suspend fun pollCheckout(code: String) = checkoutStatus
    }

    private val tasksPreferences = TasksPreferences(InMemoryDataStore())

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("stripe-purchases-test").toFile()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    private fun entitlement() = PurchaseEntitlement(
        dataDir = dir.path,
        encryption = encryption,
        httpClientFactory = mock<KtorClientFactory>(),
        serverEnvironment = mock<TasksServerEnvironment>(),
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
    fun anExpiredCodeIsNotWorthPollingAgain() = runBlocking {
        checkoutStatus = StripeCheckoutStatus.Gone

        assertEquals(StripeCollection.Gone, purchases().collectCheckout("c0de"))
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
