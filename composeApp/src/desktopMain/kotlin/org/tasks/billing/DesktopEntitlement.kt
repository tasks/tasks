package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory
import org.tasks.security.KeyStoreEncryption
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class DesktopEntitlement(
    private val dataDir: File,
    private val httpClientFactory: OkHttpClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val scope: CoroutineScope,
    private val json: Json,
    private val encryption: KeyStoreEncryption,
) {
    private val entitlementFile = File(dataDir, "entitlement.json")
    private val fileLock = Any()
    private val logger = Logger.withTag("DesktopEntitlement")
    private var refreshJob: Job? = null

    private val _hasPro = MutableStateFlow(false)
    val hasPro: Flow<Boolean> = _hasPro

    private val _sku = MutableStateFlow<String?>(null)
    val sku: Flow<String?> = _sku

    private val _formattedPrice = MutableStateFlow<String?>(null)
    val formattedPrice: Flow<String?> = _formattedPrice

    private val publicKey by lazy {
        val keyBase64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1ZGBhWUqfRRg78YGyVchzC0y9Ugh" +
            "SXVw/oVv5itVIzZHovcXs8di7X7zeDfNYlHv+nHaGExFI7y6QxjJ/+NasQ=="
        val keyBytes = Base64.getDecoder().decode(keyBase64)
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
        val iss: String? = null,
        val iat: Long? = null,
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
        val error: String? = null,
    )

    companion object {
        private const val REFRESH_LEAD_SECONDS = 24 * 60 * 60L
        private const val GRACE_PERIOD_SECONDS = 7 * 24 * 60 * 60L
        private const val RETRY_INTERVAL_SECONDS = 15 * 60L
    }

    fun initialize() {
        val stored = load() ?: return
        if (!verifySignature(stored.jwt)) return
        val payload = parsePayload(stored.jwt) ?: return
        val exp = payload.exp ?: return
        val now = currentTimeMillis() / 1000
        if (now < exp + GRACE_PERIOD_SECONDS) {
            _hasPro.value = true
            _sku.value = stored.sku
            _formattedPrice.value = stored.formattedPrice
            scheduleRefresh(stored, exp)
        }
    }

    suspend fun storeEntitlement(jwt: String, refreshToken: String, sku: String? = null, formattedPrice: String? = null, provider: EntitlementProvider) {
        if (!verifySignature(jwt)) return
        val payload = parsePayload(jwt) ?: return
        val entitlement = StoredEntitlement(jwt, refreshToken, sku, formattedPrice, provider)
        val plainText = json.encodeToString(StoredEntitlement.serializer(), entitlement)
        val encrypted = encryption.encrypt(plainText) ?: return
        withContext(Dispatchers.IO) {
            synchronized(fileLock) { entitlementFile.writeText(encrypted) }
        }
        _hasPro.value = true
        _sku.value = sku
        _formattedPrice.value = formattedPrice
        val exp = payload.exp
        if (exp != null) {
            scheduleRefresh(entitlement, exp)
        }
    }

    private fun scheduleRefresh(entitlement: StoredEntitlement, exp: Long) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            // Wait until 24h before expiry
            val refreshAt = exp - REFRESH_LEAD_SECONDS
            val now = currentTimeMillis() / 1000
            if (refreshAt > now) {
                delay((refreshAt - now) * 1000)
            }
            // Retry until refreshed, 402, or grace period exceeded
            while (true) {
                val currentTime = currentTimeMillis() / 1000
                if (currentTime >= exp + GRACE_PERIOD_SECONDS) {
                    logger.i { "Grace period exceeded, revoking pro" }
                    _hasPro.value = false
                    synchronized(fileLock) { entitlementFile.delete() }
                    return@launch
                }
                try {
                    val result = callRefresh(entitlement.refreshToken, entitlement.provider)
                    if (result != null) {
                        storeEntitlement(result.jwt!!, result.refresh_token!!, result.sku, result.formatted_price, entitlement.provider)
                        logger.i { "Desktop entitlement refreshed" }
                        return@launch
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to refresh desktop entitlement" }
                }
                // callRefresh sets hasPro=false on 402
                if (!_hasPro.value) return@launch
                delay(RETRY_INTERVAL_SECONDS * 1000)
            }
        }
    }

    private fun load(): StoredEntitlement? {
        return try {
            val encrypted = synchronized(fileLock) {
                if (!entitlementFile.exists()) return null
                entitlementFile.readText()
            }
            val decrypted = encryption.decrypt(encrypted) ?: return null
            json.decodeFromString(StoredEntitlement.serializer(), decrypted)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load entitlement" }
            null
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

    private suspend fun callRefresh(refreshToken: String, provider: EntitlementProvider): RefreshResponse? =
        withContext(Dispatchers.IO) {
            val client = httpClientFactory.newClient()
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
                    if (it.code == 402) {
                        synchronized(fileLock) { entitlementFile.delete() }
                        _hasPro.value = false
                    }
                    return@withContext null
                }
                val responseBody = it.body?.string() ?: return@withContext null
                val result = json.decodeFromString(RefreshResponse.serializer(), responseBody)
                if (result.jwt != null && result.refresh_token != null) result else null
            }
        }
}
