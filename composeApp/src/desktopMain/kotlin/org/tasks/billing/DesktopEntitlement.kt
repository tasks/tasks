package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.EncryptedFile
import org.tasks.http.OkHttpClientFactory
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit

class DesktopEntitlement(
    dataDir: File,
    private val httpClientFactory: OkHttpClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val scope: CoroutineScope,
    private val json: Json,
    private val encryption: KeyStoreEncryption,
    private val syncAdapters: SyncAdapters,
    private val publicKeyBase64: String = TASKS_PUBLIC_KEY,
    private val refreshCallTimeoutMillis: Long = REFRESH_CALL_TIMEOUT_MILLIS,
    private val readyTimeoutMillis: Long = READY_TIMEOUT_MILLIS,
) {
    private val storage = EncryptedFile(File(dataDir, FILE_NAME), encryption)
    private val logger = Logger.withTag("DesktopEntitlement")
    private val checkLock = Mutex()
    private var refreshJob: Job? = null

    @Volatile
    private var lastRefreshAttempt = 0L

    private val _hasPro = MutableStateFlow(false)
    val hasPro: Flow<Boolean> = _hasPro

    private val _sku = MutableStateFlow<String?>(null)
    val sku: Flow<String?> = _sku

    private val _formattedPrice = MutableStateFlow<String?>(null)
    val formattedPrice: Flow<String?> = _formattedPrice

    private val _provider = MutableStateFlow<EntitlementProvider?>(null)
    val provider: Flow<EntitlementProvider?> = _provider

    private val publicKey by lazy {
        val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val keySpec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("EC").generatePublic(keySpec)
    }

    @Serializable
    data class StoredEntitlement(
        val jwt: String,
        val refreshToken: String,
        val sku: String? = null,
        val formattedPrice: String? = null,
        val provider: EntitlementProvider,
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
    }

    suspend fun getJwt(): String? = (load() as? LoadResult.Loaded)?.entitlement?.jwt

    fun initialize() = scope.launch { checkLock.withLock { check() } }

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
        val entitlement = StoredEntitlement(jwt, refreshToken, sku, formattedPrice, provider)
        val persisted =
            storage.write(json.encodeToString(StoredEntitlement.serializer(), entitlement))
        if (!persisted) {
            logger.e { "Failed to persist entitlement, pro will not survive a restart" }
        }
        val wasPro = _hasPro.getAndUpdate { true }
        _sku.value = sku
        _formattedPrice.value = formattedPrice
        _provider.value = provider
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
                )
                if (granted == StoreResult.REJECTED) {
                    logger.e { "Refreshed entitlement was rejected, keeping the stored one" }
                    false
                } else {
                    logger.i { "Desktop entitlement refreshed" }
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

            val headerAndPayload = "${parts[0]}.${parts[1]}"
            val signatureBytes = Base64.getUrlDecoder().decode(parts[2])

            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(headerAndPayload.toByteArray(Charsets.US_ASCII))
            sig.verify(derEncode(signatureBytes))
        } catch (e: Exception) {
            logger.e(e) { "JWT signature verification failed" }
            false
        }
    }

    private fun parsePayload(jwt: String): JwtPayload? {
        return try {
            val payloadJson = String(Base64.getUrlDecoder().decode(jwt.split(".")[1]))
            json.decodeFromString(JwtPayload.serializer(), payloadJson)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse JWT payload" }
            null
        }
    }

    // ES256 JWTs use raw R||S format (64 bytes), but Java needs DER encoding
    private fun derEncode(raw: ByteArray): ByteArray {
        if (raw.size != 64) {
            logger.w { "Unexpected signature size: ${raw.size}, expected 64" }
            return raw
        }
        val r = raw.sliceArray(0 until 32).trimLeadingZeros()
        val s = raw.sliceArray(32 until 64).trimLeadingZeros()
        if (r.isEmpty() || s.isEmpty()) {
            logger.w { "Invalid signature component after trimming" }
            return raw
        }
        val rLen = if (r[0].toInt() and 0x80 != 0) r.size + 1 else r.size
        val sLen = if (s[0].toInt() and 0x80 != 0) s.size + 1 else s.size
        val totalLen = 2 + rLen + 2 + sLen
        val der = ByteArray(2 + totalLen)
        var i = 0
        der[i++] = 0x30
        der[i++] = totalLen.toByte()
        der[i++] = 0x02
        der[i++] = rLen.toByte()
        if (rLen > r.size) der[i++] = 0x00
        r.copyInto(der, i); i += r.size
        der[i++] = 0x02
        der[i++] = sLen.toByte()
        if (sLen > s.size) der[i++] = 0x00
        s.copyInto(der, i)
        return der
    }

    private fun ByteArray.trimLeadingZeros(): ByteArray {
        var start = 0
        while (start < size - 1 && this[start] == 0.toByte()) start++
        return if (start == 0) this else sliceArray(start until size)
    }

    private suspend fun callRefresh(refreshToken: String, provider: EntitlementProvider): RefreshResult =
        withContext(Dispatchers.IO) {
            try {
                val client = httpClientFactory.newClient {
                    it.callTimeout(refreshCallTimeoutMillis, TimeUnit.MILLISECONDS)
                }
                val url = when (provider) {
                    EntitlementProvider.GITHUB_SPONSOR -> "${serverEnvironment.caldavUrl}/desktop/github/refresh"
                    EntitlementProvider.PLAY -> "${serverEnvironment.caldavUrl}/desktop/refresh"
                }
                val body = json.encodeToString(
                    RefreshRequest.serializer(),
                    RefreshRequest(refresh_token = refreshToken)
                ).toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        return@withContext if (it.code == 402) {
                            RefreshResult.Revoked
                        } else {
                            logger.w { "Entitlement refresh failed: HTTP ${it.code}" }
                            RefreshResult.Failed
                        }
                    }
                    val responseBody = it.body.string()
                    val result = json.decodeFromString(RefreshResponse.serializer(), responseBody)
                    if (result.jwt == null || result.refresh_token == null) {
                        logger.w { "Entitlement refresh response has no jwt or refresh token" }
                        return@withContext RefreshResult.Failed
                    }
                    RefreshResult.Success(
                        jwt = result.jwt,
                        refreshToken = result.refresh_token,
                        sku = result.sku,
                        formattedPrice = result.formatted_price,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ensureActive()
                logger.e(e) { "Failed to refresh desktop entitlement" }
                RefreshResult.Failed
            }
        }
}
