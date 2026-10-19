package org.tasks.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.OkHttpKtorClientFactory
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.time.DateTimeUtils2
import java.io.File
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class PurchaseEntitlementTest {

    private lateinit var dir: File
    private lateinit var server: MockWebServer
    private lateinit var scope: CoroutineScope

    private val json = Json { ignoreUnknownKeys = true }
    private val keyProvider = FixedKeyProvider()
    private val encryption = KeyStoreEncryption(keyProvider)
    private val syncAdapters = mock<SyncAdapters>()

    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

    private val imposterKey = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
        .private

    private val entitlementFile: File
        get() = File(dir, "entitlement.json")

    private class FixedKeyProvider : KeyProvider {
        private val key: SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        @Volatile
        var locked = false

        override fun getKey(): SecretKey =
            if (locked) throw IllegalStateException("Encryption key unavailable") else key
    }

    private val httpClient = OkHttpClient()

    private val clientFactory = OkHttpKtorClientFactory(object : OkHttpClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: (OkHttpClient.Builder) -> Unit,
        ) = httpClient.newBuilder().apply(block).build()
    })

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("entitlement-test").toFile()
        server = MockWebServer().apply { start() }
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    @After
    fun tearDown() {
        DateTimeUtils2.setCurrentMillisSystem()
        scope.cancel()
        httpClient.dispatcher.cancelAll()
        server.shutdown()
        runBlocking { withTimeout(5_000) { scope.coroutineContext.job.join() } }
        httpClient.connectionPool.evictAll()
        httpClient.dispatcher.executorService.shutdown()
        dir.deleteRecursively()
    }

    @Test
    fun syncsWithoutProWhenNoEntitlementStored() = runBlocking {
        entitlement().apply {
            initialize()

            assertTrue(awaitReady())
            assertFalse(hasPro.first())
        }
        assertEquals(0, server.requestCount)
        verifyNoInteractions(syncAdapters)
    }

    @Test
    fun ignoresEntitlementThatFailsSignatureVerification() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS, key = imposterKey))

        entitlement().apply {
            initialize()

            assertTrue(awaitReady())
            assertFalse(hasPro.first())
        }
        assertEquals(0, server.requestCount)
        verifyNoInteractions(syncAdapters)
    }

    @Test
    fun grantsProFromUnexpiredEntitlement() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS))

        entitlement().apply {
            initialize()

            assertTrue(awaitReady())
            assertTrue(hasPro.first())
            assertEquals("annual", sku.first())
            assertEquals(EntitlementProvider.PLAY, provider.first())
        }
        assertEquals(0, server.requestCount)
        verifyNoInteractions(syncAdapters)
    }

    @Test
    fun refreshesEntitlementPastGracePeriod() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        val renewed = jwt(exp = now() + 30 * DAY_SECONDS)
        server.enqueue(refreshResponse(renewed))

        entitlement().apply {
            initialize()

            assertTrue(awaitReady())
            assertTrue(hasPro.first())
            assertEquals(renewed, getJwt())
        }
        assertEquals("stored-token", refreshTokenSent())
        assertTrue(entitlementFile.exists())
    }

    @Test
    fun storesRotatedRefreshToken() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS)))

        entitlement().apply {
            initialize()
            awaitReady()
        }
        assertEquals("rotated-token", readFromDisk().refreshToken)
    }

    @Test
    fun neverGrantsProPastGraceBeforeRefreshAnswers() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(MockResponse().setResponseCode(500).setHeadersDelay(10, TimeUnit.SECONDS))

        val entitlement = entitlement()
        entitlement.initialize()
        withTimeout(5000) { entitlement.provider.first { it != null } }

        assertFalse(entitlement.hasPro.first())
    }

    @Test
    fun syncsWithoutProWhenPastGraceRefreshFails() = runBlocking {
        val stored = jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS)
        storeOnDisk(stored)
        server.enqueue(MockResponse().setResponseCode(500))

        val entitlement = entitlement()
        entitlement.initialize()

        assertFalse(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertEquals(stored, entitlement.getJwt())
        assertEquals("stored-token", refreshTokenSent())
        assertTrue(entitlementFile.exists())
    }

    @Test
    fun answersLaterSyncsImmediatelyAfterPastGraceRefreshFails() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(MockResponse().setResponseCode(500))

        val entitlement = entitlement()
        entitlement.initialize()
        assertFalse(entitlement.awaitReady())

        repeat(3) {
            assertFalse(withTimeout(2_000) { entitlement.awaitReady() })
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun syncsWithoutProWhenRefreshCallTimesOut() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(
            refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS))
                .setHeadersDelay(10, TimeUnit.SECONDS)
        )

        val entitlement = entitlement(refreshCallTimeoutMillis = 500)
        entitlement.initialize()

        assertFalse(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertTrue(entitlementFile.exists())
    }

    @Test
    fun dropsToFreeTierWhenGracePeriodLapsesMidSession() = runBlocking {
        val start = System.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(start)
        storeOnDisk(jwt(exp = start / 1000 - GRACE_SECONDS + 60))
        server.enqueue(MockResponse().setResponseCode(500).setHeadersDelay(2, TimeUnit.SECONDS))

        val entitlement = entitlement()

        assertTrue(entitlement.awaitReady())
        assertTrue(entitlement.hasPro.first())

        DateTimeUtils2.setCurrentMillisFixed(start + 61_000)

        withTimeout(10_000) { entitlement.hasPro.first { !it } }

        assertFalse(withTimeout(2_000) { entitlement.awaitReady() })
    }

    @Test
    fun syncsWhenPastGraceRefreshRestoresPro() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS)))

        entitlement().apply {
            initialize()

            assertTrue(awaitReady())
            assertTrue(hasPro.first())
        }
        verify(syncAdapters).sync(SyncSource.PURCHASE_COMPLETED)
    }

    @Test
    fun deletesEntitlementWhenSubscriptionRevoked() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(MockResponse().setResponseCode(402))

        val entitlement = entitlement()
        entitlement.initialize()

        assertTrue(entitlement.awaitReady())
        assertNull(entitlement.getJwt())
        assertFalse(entitlementFile.exists())
    }

    @Test
    fun clearsSubscriptionDetailsWhenRevoked() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(MockResponse().setResponseCode(402))

        val entitlement = entitlement()
        entitlement.initialize()
        entitlement.awaitReady()

        assertFalse(entitlement.hasPro.first())
        assertNull(entitlement.sku.first())
        assertNull(entitlement.formattedPrice.first())
        assertNull(entitlement.provider.first())
    }

    @Test
    fun keepsStoredEntitlementWhenRenewedEntitlementIsRejected() = runBlocking {
        val stored = jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS)
        storeOnDisk(stored)
        server.enqueue(refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS, key = imposterKey)))

        val entitlement = entitlement()
        entitlement.initialize()

        assertFalse(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertEquals(stored, entitlement.getJwt())
        assertEquals("stored-token", readFromDisk().refreshToken)
    }

    @Test
    fun retriesLockedKeychainOnNextSync() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS))
        keyProvider.locked = true

        val entitlement = entitlement()
        entitlement.initialize()

        assertFalse(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())

        keyProvider.locked = false

        assertTrue(entitlement.awaitReady())
        assertTrue(entitlement.hasPro.first())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun answersImmediatelyWhileKeychainStaysLocked() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS))
        keyProvider.locked = true

        val entitlement = entitlement()
        entitlement.initialize()

        repeat(3) {
            assertFalse(withTimeout(2_000) { entitlement.awaitReady() })
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun ignoresEmptyEntitlementFile() = runBlocking {
        entitlementFile.writeText("")

        val entitlement = entitlement()
        entitlement.initialize()

        assertTrue(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
    }

    @Test
    fun ignoresUnparseableEntitlement() = runBlocking {
        entitlementFile.writeText(encryption.encrypt("not an entitlement")!!)

        val entitlement = entitlement()
        entitlement.initialize()

        assertTrue(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
    }

    @Test
    fun syncsWhenProIsGranted() = runBlocking {
        val granted = entitlement().storeEntitlement(
            jwt = jwt(exp = now() + 30 * DAY_SECONDS),
            refreshToken = "stored-token",
            provider = EntitlementProvider.PLAY,
        )

        assertTrue(granted)
        verify(syncAdapters).sync(SyncSource.PURCHASE_COMPLETED)
    }

    @Test
    fun doesNotSyncWhenProWasAlreadyActive() = runBlocking {
        val entitlement = entitlement()
        val granted = entitlement.storeEntitlement(
            jwt = jwt(exp = now() + 30 * DAY_SECONDS),
            refreshToken = "stored-token",
            provider = EntitlementProvider.PLAY,
        )

        val renewed = entitlement.storeEntitlement(
            jwt = jwt(exp = now() + 60 * DAY_SECONDS),
            refreshToken = "rotated-token",
            provider = EntitlementProvider.PLAY,
        )

        assertTrue(granted)
        assertTrue(renewed)
        assertEquals("rotated-token", readFromDisk().refreshToken)
        verify(syncAdapters, times(1)).sync(SyncSource.PURCHASE_COMPLETED)
    }

    @Test
    fun rejectsEntitlementThatFailsSignatureVerification() = runBlocking {
        val granted = entitlement().storeEntitlement(
            jwt = jwt(exp = now() + 30 * DAY_SECONDS, key = imposterKey),
            refreshToken = "stored-token",
            provider = EntitlementProvider.PLAY,
        )

        assertFalse(granted)
        assertFalse(entitlementFile.exists())
        verifyNoInteractions(syncAdapters)
    }

    @Test
    fun ignoresEntitlementWithUnreadablePayload() = runBlocking {
        storeOnDisk(signedJwt("not json"))

        val entitlement = entitlement()
        entitlement.initialize()

        assertTrue(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun ignoresEntitlementWithoutExpiration() = runBlocking {
        storeOnDisk(signedJwt("""{"iss":"tasks.org"}"""))

        val entitlement = entitlement()
        entitlement.initialize()

        assertTrue(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun syncsWithoutProWhenRefreshResponseHasNoJwt() = runBlocking {
        val stored = jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS)
        storeOnDisk(stored)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )

        val entitlement = entitlement()
        entitlement.initialize()

        assertFalse(entitlement.awaitReady())
        assertFalse(entitlement.hasPro.first())
        assertEquals(stored, entitlement.getJwt())
        assertTrue(entitlementFile.exists())
    }

    @Test
    fun keepsNewlyLinkedEntitlementWhenInFlightRefreshIsRevoked() = runBlocking {
        storeOnDisk(jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS))
        server.enqueue(MockResponse().setResponseCode(402).setHeadersDelay(2, TimeUnit.SECONDS))

        val entitlement = entitlement()
        entitlement.initialize()
        assertEquals("stored-token", refreshTokenSent())

        val linked = jwt(exp = now() + 30 * DAY_SECONDS)
        val granted = entitlement.storeEntitlement(
            jwt = linked,
            refreshToken = "linked-token",
            provider = EntitlementProvider.PLAY,
        )

        assertTrue(granted)
        assertTrue(entitlement.awaitReady())
        assertTrue(entitlement.hasPro.first())
        assertEquals(linked, entitlement.getJwt())
        assertEquals("linked-token", readFromDisk().refreshToken)
    }

    @Test
    fun grantsProWhenEntitlementCannotBePersisted() = runBlocking {
        val blocked = File(dir, "blocked").apply { writeText("not a directory") }

        val entitlement = entitlement(dataDir = blocked)
        val granted = entitlement.storeEntitlement(
            jwt = jwt(exp = now() + 30 * DAY_SECONDS),
            refreshToken = "stored-token",
            provider = EntitlementProvider.PLAY,
        )

        assertFalse(granted)
        assertTrue(entitlement.hasPro.first())
    }

    @Test
    fun aCardSubscriptionRefreshesWhereAPlayOneDoes() = runBlocking {
        storeOnDisk(
            jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS),
            EntitlementProvider.STRIPE,
        )
        server.enqueue(refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS)))

        entitlement().apply {
            initialize()
            assertTrue(awaitReady())
            assertTrue(hasPro.first())
        }

        assertEquals("/desktop/refresh", server.takeRequest(5, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun aTierChangedOnAnotherDeviceIsPickedUpByACheck() = runBlocking {
        val stored = jwt(exp = now() + 30 * DAY_SECONDS)
        storeOnDisk(stored, EntitlementProvider.STRIPE)
        server.enqueue(checkResponse("annual_30", "USD 29.99"))

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())
        assertEquals("annual", entitlement.sku.first())

        assertTrue(entitlement.checkTier())

        assertEquals("annual_30", entitlement.sku.first())
        assertEquals("USD 29.99", entitlement.formattedPrice.first())
        assertEquals("annual_30", readFromDisk().sku)
        assertEquals("/desktop/check", server.takeRequest(5, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun aCheckSpendsNoCredentialAndLooksLikeNoPurchase() = runBlocking {
        val stored = jwt(exp = now() + 30 * DAY_SECONDS)
        storeOnDisk(stored, EntitlementProvider.STRIPE)
        server.enqueue(checkResponse("annual_30", "USD 29.99"))

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())

        assertTrue(entitlement.checkTier())

        assertEquals(stored, entitlement.getJwt())
        assertEquals("stored-token", readFromDisk().refreshToken)
        verifyNoInteractions(syncAdapters)
    }

    @Test
    fun aTierThatHasNotMovedIsNoNews() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(checkResponse("annual", "USD 10.00"))

        assertFalse(entitlement().checkTier())
    }

    @Test
    fun aRefreshKeepsWhatTheCheckLearned() = runBlocking {
        storeOnDisk(
            jwt = jwt(exp = now() - GRACE_SECONDS - DAY_SECONDS),
            provider = EntitlementProvider.STRIPE,
            terms = PurchaseTerms(status = "trialing", trialEnd = 1760000000000L),
        )
        server.enqueue(refreshResponse(jwt(exp = now() + 30 * DAY_SECONDS)))

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())

        assertEquals("trialing", entitlement.terms.first()?.status)
        assertEquals(1760000000000L, entitlement.terms.first()?.trialEnd)
        assertEquals("trialing", readFromDisk().terms?.status)
    }

    @Test
    fun aCheckRecordsTheTermsItWasToldAbout() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(
            checkResponse(
                sku = "annual_30",
                formattedPrice = "USD 29.99",
                status = "trialing",
                trialEnd = 1760000000000L,
                creditCents = 900,
                creditCurrency = "EUR",
            )
        )

        val entitlement = entitlement()
        assertTrue(entitlement.checkTier())

        val terms = entitlement.terms.first()
        assertEquals("trialing", terms?.status)
        assertEquals(1760000000000L, terms?.trialEnd)
        assertEquals(900, terms?.creditCents)
        assertEquals("EUR", terms?.creditCurrency)
        assertEquals(terms, readFromDisk().terms)
    }

    @Test
    fun termsThatMovedOnTheirOwnAreNotATierChange() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(
            checkResponse("annual", "USD 10.00", creditCents = 900, creditCurrency = "USD")
        )

        val entitlement = entitlement()
        assertFalse(entitlement.checkTier())

        assertEquals(900, entitlement.terms.first()?.creditCents)
        assertEquals(900, readFromDisk().terms?.creditCents)
    }

    @Test
    fun aCheckThatSaysNothingAboutTermsRecordsNothing() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(checkResponse("annual_30", "USD 29.99"))

        val entitlement = entitlement()
        assertTrue(entitlement.checkTier())

        assertNull(entitlement.terms.first())
    }

    @Test
    fun aPlayRowRecordsNoTerms() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.PLAY)
        server.enqueue(
            checkResponse("annual", "USD 10.00", status = "active", creditCents = 900)
        )

        val entitlement = entitlement()
        assertFalse(entitlement.checkTier())

        assertNull(entitlement.terms.first())
        assertNull(readFromDisk().terms)
    }

    @Test
    fun termsRecordedBeforeARestartAreStillKnown() = runBlocking {
        storeOnDisk(
            jwt = jwt(exp = now() + 30 * DAY_SECONDS),
            provider = EntitlementProvider.STRIPE,
            terms = PurchaseTerms(status = "active", creditCents = 900, creditCurrency = "USD"),
        )

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())

        assertEquals("active", entitlement.terms.first()?.status)
        assertEquals(900, entitlement.terms.first()?.creditCents)
    }

    @Test
    fun aCheckThatCannotBeAnsweredLeavesTheTierAlone() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(MockResponse().setResponseCode(503))

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())

        assertFalse(entitlement.checkTier())

        assertEquals("annual", entitlement.sku.first())
    }

    @Test
    fun aRowThatIsGoneRevokesNothing() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.STRIPE)
        server.enqueue(MockResponse().setResponseCode(404))

        val entitlement = entitlement()
        entitlement.initialize()
        assertTrue(entitlement.awaitReady())

        assertFalse(entitlement.checkTier())

        assertTrue(entitlement.hasPro.first())
        assertTrue(entitlementFile.exists())
    }

    @Test
    fun aSponsorHasNoTierToCheck() = runBlocking {
        storeOnDisk(jwt(exp = now() + 30 * DAY_SECONDS), EntitlementProvider.GITHUB_SPONSOR)

        assertFalse(entitlement().checkTier())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun thereIsNothingToCheckWithNothingStored() = runBlocking {
        assertFalse(entitlement().checkTier())
        assertEquals(0, server.requestCount)
    }

    private fun entitlement(
        refreshCallTimeoutMillis: Long = 20_000L,
        readyTimeoutMillis: Long = 30_000L,
        dataDir: File = dir,
    ) = PurchaseEntitlement(
        dataDir = dataDir.path,
        encryption = encryption,
        httpClientFactory = clientFactory,
        serverEnvironment = mock<TasksServerEnvironment> {
            on { caldavUrl } doReturn server.url("/").toString().trimEnd('/')
        },
        scope = scope,
        json = json,
        syncAdapters = syncAdapters,
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded),
        refreshCallTimeoutMillis = refreshCallTimeoutMillis,
        readyTimeoutMillis = readyTimeoutMillis,
    )

    private fun now() = System.currentTimeMillis() / 1000

    private fun storeOnDisk(
        jwt: String,
        provider: EntitlementProvider = EntitlementProvider.PLAY,
        terms: PurchaseTerms? = null,
    ) = runBlocking {
        val stored = PurchaseEntitlement.StoredEntitlement(
            jwt = jwt,
            refreshToken = "stored-token",
            sku = "annual",
            formattedPrice = "USD 10.00",
            provider = provider,
            terms = terms,
        )
        val plain = json.encodeToString(PurchaseEntitlement.StoredEntitlement.serializer(), stored)
        entitlementFile.writeText(encryption.encrypt(plain)!!)
    }

    private fun refreshResponse(jwt: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(
            """{"jwt":"$jwt","refresh_token":"rotated-token","sku":"annual",""" +
                """"formatted_price":"USD 10.00"}"""
        )

    private fun checkResponse(
        sku: String,
        formattedPrice: String,
        status: String? = null,
        trialEnd: Long? = null,
        creditCents: Int? = null,
        creditCurrency: String? = null,
    ): MockResponse {
        val fields = buildList {
            add(""""sku":"$sku"""")
            add(""""formatted_price":"$formattedPrice"""")
            status?.let { add(""""status":"$it"""") }
            trialEnd?.let { add(""""trial_end":$it""") }
            creditCents?.let { add(""""credit_cents":$it""") }
            creditCurrency?.let { add(""""credit_currency":"$it"""") }
        }
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(fields.joinToString(",", "{", "}"))
    }

    private fun refreshTokenSent(): String? {
        val body = server.takeRequest(5, TimeUnit.SECONDS)?.body?.readUtf8() ?: return null
        return json.decodeFromString(JsonObject.serializer(), body)["refresh_token"]
            ?.jsonPrimitive
            ?.content
    }

    private suspend fun readFromDisk(): PurchaseEntitlement.StoredEntitlement {
        val decrypted = encryption.decrypt(entitlementFile.readText())!!
        return json.decodeFromString(PurchaseEntitlement.StoredEntitlement.serializer(), decrypted)
    }

    private fun jwt(exp: Long, key: PrivateKey = keyPair.private): String =
        signedJwt("""{"iss":"tasks.org","exp":$exp}""", key)

    private fun signedJwt(payload: String, key: PrivateKey = keyPair.private): String {
        val header = encode("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
        val signed = "$header.${encode(payload.toByteArray())}"
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

    companion object {
        private const val DAY_SECONDS = 24 * 60 * 60L
        private const val GRACE_SECONDS = 7 * DAY_SECONDS
    }
}
