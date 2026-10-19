package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.KtorClientFactory
import org.tasks.http.jsonBody
import org.tasks.http.withClient
import okio.Path.Companion.toPath
import org.tasks.security.EncryptedStore
import org.tasks.security.Encryption
import org.tasks.security.rawEcdsaToDer
import org.tasks.security.verifyEs256
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.io.encoding.Base64

class PurchaseEntitlement(
    dataDir: String,
    encryption: Encryption,
    private val httpClientFactory: KtorClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val scope: CoroutineScope,
    private val json: Json,
    private val syncAdapters: SyncAdapters,
    private val publicKeyBase64: String = TASKS_PUBLIC_KEY,
    private val refreshCallTimeoutMillis: Long = REFRESH_CALL_TIMEOUT_MILLIS,
    private val readyTimeoutMillis: Long = READY_TIMEOUT_MILLIS,
) {
    private val storage = EncryptedStore("$dataDir/$FILE_NAME".toPath(), encryption)
    private val logger = Logger.withTag("PurchaseEntitlement")
    private val checkLock = Mutex()
    private var refreshJob: Job? = null

    private var lastRefreshAttempt = 0L

    private val _hasPro = MutableStateFlow(false)
    val hasPro: Flow<Boolean> = _hasPro

    private val _sku = MutableStateFlow<String?>(null)
    val sku: Flow<String?> = _sku

    private val _formattedPrice = MutableStateFlow<String?>(null)
    val formattedPrice: Flow<String?> = _formattedPrice

    private val _terms = MutableStateFlow<PurchaseTerms?>(null)
    val terms: Flow<PurchaseTerms?> = _terms

    private val _provider = MutableStateFlow<EntitlementProvider?>(null)
    val provider: Flow<EntitlementProvider?> = _provider

    private val publicKey by lazy { Base64.Default.decode(publicKeyBase64) }

    @Serializable
    data class StoredEntitlement(
        val jwt: String,
        val refreshToken: String,
        val sku: String? = null,
        val formattedPrice: String? = null,
        val provider: EntitlementProvider,
        val terms: PurchaseTerms? = null,
    )

    @Serializable
    private data class JwtPayload(
        val exp: Long? = null,
    )

    @Serializable
    private data class RefreshRequest(
        val refresh_token: String,
    )

    @Serializable
    private data class CheckRequest(
        val purchase_jwt: String,
    )

    @Serializable
    private data class CheckResponse(
        val sku: String? = null,
        val formatted_price: String? = null,
        val status: String? = null,
        val trial_end: Long? = null,
        val credit_cents: Int? = null,
        val credit_currency: String? = null,
    )

    @Serializable
    private data class RefreshResponse(
        val jwt: String? = null,
        val refresh_token: String? = null,
        val sku: String? = null,
        val formatted_price: String? = null,
    )

    private sealed interface LoadResult {
        data class Loaded(val entitlement: StoredEntitlement) : LoadResult

        object Missing : LoadResult

        object KeyUnavailable : LoadResult
    }

    private enum class StoreResult { REJECTED, GRANTED_NOT_PERSISTED, STORED }

    private sealed interface RefreshResult {
        data class Success(
            val jwt: String,
            val refreshToken: String,
            val sku: String?,
            val formattedPrice: String?,
        ) : RefreshResult

        object Revoked : RefreshResult

        object Failed : RefreshResult
    }

    companion object {
        const val FILE_NAME = "entitlement.json"

        private const val TASKS_PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1ZGBhWUqfRRg78YGyVchzC0y9Ugh" +
            "SXVw/oVv5itVIzZHovcXs8di7X7zeDfNYlHv+nHaGExFI7y6QxjJ/+NasQ=="
        private const val REFRESH_LEAD_SECONDS = 24 * 60 * 60L
        private const val GRACE_PERIOD_SECONDS = 7 * 24 * 60 * 60L
        private const val RETRY_INTERVAL_SECONDS = 15 * 60L
        private const val READY_TIMEOUT_MILLIS = 30_000L
        private const val REFRESH_CALL_TIMEOUT_MILLIS = 20_000L

        private val base64Url =
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    }

    suspend fun getJwt(): String? = (load() as? LoadResult.Loaded)?.entitlement?.jwt

    fun initialize() = scope.launch { checkLock.withLock { check() } }

    suspend fun checkTier(): Boolean = checkLock.withLock {
        val stored = (load() as? LoadResult.Loaded)?.entitlement ?: return@withLock false
        val checked = callCheck(stored) ?: return@withLock false
        val terms = checked.terms(stored.provider)
        val tierMoved = checked.sku != stored.sku ||
            checked.formatted_price != stored.formattedPrice
        logger.i {
            "Check says ${checked.sku}, status=${terms?.status}, " +
                "trialEnd=${terms?.trialEnd}, credit=${terms?.creditCents}"
        }
        if (!tierMoved && terms == stored.terms) {
            return@withLock false
        }
        val updated = stored.copy(
            sku = checked.sku,
            formattedPrice = checked.formatted_price,
            terms = terms,
        )
        if (!storage.write(json.encodeToString(StoredEntitlement.serializer(), updated))) {
            logger.e { "Failed to persist the check, it will read stale after a restart" }
        }
        _sku.value = updated.sku
        _formattedPrice.value = updated.formattedPrice
        _terms.value = updated.terms
        tierMoved
    }

    private fun CheckResponse.terms(provider: EntitlementProvider): PurchaseTerms? {
        if (provider != EntitlementProvider.STRIPE) {
            return null
        }
        return PurchaseTerms(
            status = status,
            trialEnd = trial_end,
            creditCents = credit_cents,
            creditCurrency = credit_currency,
        ).takeIf { !it.isEmpty }
    }

    suspend fun awaitReady(): Boolean {
        val answered = withTimeoutOrNull(readyTimeoutMillis) { checkLock.withLock { check() } }
        if (answered == null) {
            logger.e { "Timed out checking entitlement" }
            return false
        }
        return answered
    }

    private suspend fun check(): Boolean {
        if (_hasPro.value) return true
        val stored = when (val result = load()) {
            is LoadResult.Loaded -> result.entitlement
            LoadResult.KeyUnavailable -> return false
            LoadResult.Missing -> return true
        }
        if (!verifySignature(stored.jwt)) {
            logger.e { "Stored entitlement failed signature verification, ignoring it" }
            return true
        }
        val exp = parsePayload(stored.jwt)?.exp
        if (exp == null) {
            logger.e { "Stored entitlement has no expiration, ignoring it" }
            return true
        }
        _sku.value = stored.sku
        _formattedPrice.value = stored.formattedPrice
        _terms.value = stored.terms
        _provider.value = stored.provider
        val now = currentTimeMillis() / 1000
        if (now < exp + GRACE_PERIOD_SECONDS) {
            _hasPro.value = true
            scheduleRefresh(stored, exp)
            return true
        }
        if (now < lastRefreshAttempt + RETRY_INTERVAL_SECONDS) {
            logger.e { "Grace period exceeded and refresh failed, syncing without pro" }
            return false
        }
        return attemptRefresh(stored)
    }

    suspend fun storeEntitlement(
        jwt: String,
        refreshToken: String,
        sku: String? = null,
        formattedPrice: String? = null,
        provider: EntitlementProvider,
    ): Boolean = checkLock.withLock {
        grantEntitlement(jwt, refreshToken, sku, formattedPrice, provider) == StoreResult.STORED
    }

    private suspend fun grantEntitlement(
        jwt: String,
        refreshToken: String,
        sku: String?,
        formattedPrice: String?,
        provider: EntitlementProvider,
        terms: PurchaseTerms? = null,
    ): StoreResult {
        if (!verifySignature(jwt)) {
            logger.e { "Refusing to store entitlement that failed signature verification" }
            return StoreResult.REJECTED
        }
        val payload = parsePayload(jwt)
        if (payload == null) {
            logger.e { "Refusing to store entitlement with an unreadable payload" }
            return StoreResult.REJECTED
        }
        val entitlement =
            StoredEntitlement(jwt, refreshToken, sku, formattedPrice, provider, terms)
        val persisted =
            storage.write(json.encodeToString(StoredEntitlement.serializer(), entitlement))
        if (!persisted) {
            logger.e { "Failed to persist entitlement, pro will not survive a restart" }
        }
        val wasPro = _hasPro.getAndUpdate { true }
        _sku.value = sku
        _formattedPrice.value = formattedPrice
        _provider.value = provider
        _terms.value = terms
        if (!wasPro) {
            syncAdapters.sync(SyncSource.PURCHASE_COMPLETED)
        }
        payload.exp?.let { scheduleRefresh(entitlement, it) }
        return if (persisted) StoreResult.STORED else StoreResult.GRANTED_NOT_PERSISTED
    }

    private fun scheduleRefresh(entitlement: StoredEntitlement, exp: Long) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (true) {
                val now = currentTimeMillis() / 1000
                val refreshAt = maxOf(
                    exp - REFRESH_LEAD_SECONDS,
                    lastRefreshAttempt + RETRY_INTERVAL_SECONDS,
                )
                if (refreshAt > now) {
                    delay((refreshAt - now) * 1000)
                }
                val answered = checkLock.withLock {
                    attemptRefresh(entitlement).also {
                        if (!it && currentTimeMillis() / 1000 >= exp + GRACE_PERIOD_SECONDS) {
                            logger.i { "Grace period exceeded, dropping to free tier" }
                            _hasPro.value = false
                        }
                    }
                }
                if (answered) break
            }
        }
    }

    private suspend fun attemptRefresh(entitlement: StoredEntitlement): Boolean {
        lastRefreshAttempt = currentTimeMillis() / 1000
        return when (val result = callRefresh(entitlement.refreshToken, entitlement.provider)) {
            is RefreshResult.Success -> {
                val granted = grantEntitlement(
                    jwt = result.jwt,
                    refreshToken = result.refreshToken,
                    sku = result.sku,
                    formattedPrice = result.formattedPrice,
                    provider = entitlement.provider,
                    terms = entitlement.terms,
                )
                if (granted == StoreResult.REJECTED) {
                    logger.e { "Refreshed entitlement was rejected, keeping the stored one" }
                    false
                } else {
                    logger.i { "Purchase entitlement refreshed" }
                    true
                }
            }
            RefreshResult.Revoked -> {
                logger.i { "Subscription no longer active, clearing entitlement" }
                if (!storage.delete()) {
                    logger.e { "Failed to clear revoked entitlement, retrying on next launch" }
                }
                _hasPro.value = false
                _sku.value = null
                _formattedPrice.value = null
                _provider.value = null
                _terms.value = null
                true
            }
            RefreshResult.Failed -> false
        }
    }

    private suspend fun load(): LoadResult {
        val decrypted = try {
            storage.read()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Cannot read entitlement, encryption key unavailable" }
            return LoadResult.KeyUnavailable
        }
        if (decrypted == null) {
            if (storage.exists()) {
                logger.e { "Entitlement is empty or could not be decrypted, ignoring it" }
            }
            return LoadResult.Missing
        }
        return try {
            LoadResult.Loaded(json.decodeFromString(StoredEntitlement.serializer(), decrypted))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse entitlement, ignoring it" }
            LoadResult.Missing
        }
    }

    private fun verifySignature(jwt: String): Boolean {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return false
            val der = rawEcdsaToDer(base64Url.decode(parts[2])) ?: return false
            verifyEs256(
                publicKey = publicKey,
                signed = "${parts[0]}.${parts[1]}".encodeToByteArray(),
                derSignature = der,
            )
        } catch (e: Exception) {
            logger.e(e) { "JWT signature verification failed" }
            false
        }
    }

    private fun parsePayload(jwt: String): JwtPayload? {
        return try {
            val payloadJson = base64Url.decode(jwt.split(".")[1]).decodeToString()
            json.decodeFromString(JwtPayload.serializer(), payloadJson)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse JWT payload" }
            null
        }
    }

    private suspend fun callRefresh(
        refreshToken: String,
        provider: EntitlementProvider,
    ): RefreshResult {
        val url = when (provider) {
            EntitlementProvider.GITHUB_SPONSOR ->
                "${serverEnvironment.caldavUrl}/desktop/github/refresh"
            EntitlementProvider.PLAY, EntitlementProvider.STRIPE ->
                "${serverEnvironment.caldavUrl}/desktop/refresh"
        }
        return try {
            val (code, body) = httpClientFactory.withClient(refreshCallTimeoutMillis) { client ->
                val response = client.post(url) {
                    jsonBody(
                        json,
                        RefreshRequest.serializer(),
                        RefreshRequest(refresh_token = refreshToken),
                    )
                }
                response.status.value to response.bodyAsText()
            }
            if (code !in 200..299) {
                if (code == 402) {
                    RefreshResult.Revoked
                } else {
                    logger.w { "Entitlement refresh failed: HTTP $code" }
                    RefreshResult.Failed
                }
            } else {
                val result = json.decodeFromString(RefreshResponse.serializer(), body)
                if (result.jwt == null || result.refresh_token == null) {
                    logger.w { "Entitlement refresh response has no jwt or refresh token" }
                    RefreshResult.Failed
                } else {
                    RefreshResult.Success(
                        jwt = result.jwt,
                        refreshToken = result.refresh_token,
                        sku = result.sku,
                        formattedPrice = result.formatted_price,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.e(e) { "Failed to refresh purchase entitlement" }
            RefreshResult.Failed
        }
    }

    private suspend fun callCheck(entitlement: StoredEntitlement): CheckResponse? {
        val url = when (entitlement.provider) {
            EntitlementProvider.GITHUB_SPONSOR -> return null
            EntitlementProvider.PLAY, EntitlementProvider.STRIPE ->
                "${serverEnvironment.caldavUrl}/desktop/check"
        }
        return try {
            val (code, body) = httpClientFactory.withClient(refreshCallTimeoutMillis) { client ->
                val response = client.post(url) {
                    jsonBody(
                        json,
                        CheckRequest.serializer(),
                        CheckRequest(purchase_jwt = entitlement.jwt),
                    )
                }
                response.status.value to response.bodyAsText()
            }
            if (code !in 200..299) {
                logger.w { "Entitlement check failed: HTTP $code" }
                return null
            }
            json.decodeFromString(CheckResponse.serializer(), body).takeIf { it.sku != null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.e(e) { "Failed to check purchase entitlement" }
            null
        }
    }
}
