package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.tasks.auth.OAuthTokenRefresh.withRefreshResult
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class OAuthTokenStore(
    private val encryption: Encryption,
    private val caldavDao: CaldavDao,
    private val oauthClient: TasksOAuthClient,
    private val refreshWhenExpiryUnknown: Boolean = true,
    private val authHeader: suspend () -> String? = { null },
) {
    private val logger = Logger.withTag(TAG)
    private val decodedLock = Mutex()
    private val decoded = mutableMapOf<String, Pair<String, OAuthTokenData>>()
    private val lastForced = mutableMapOf<String, Pair<String, Long>>()
    private val locksLock = Mutex()
    private val locks = mutableMapOf<String, KeyLock>()

    fun isExpired(data: OAuthTokenData): Boolean =
        OAuthTokenRefresh.isExpired(data, refreshWhenExpiryUnknown)

    suspend fun getToken(account: CaldavAccount, rejected: String? = null): String =
        getTokenData(account, rejected).accessToken

    suspend fun getTokenData(account: CaldavAccount, rejected: String? = null): OAuthTokenData {
        val key = account.lockKey()
        return withKeyLock(key) {
            val id = account.id.takeIf { it != 0L }
            val fromDb = id?.let { caldavDao.getAccount(it) }?.password
            val encrypted = fromDb
                ?: account.password
                ?: throw signInAgain("missing credentials")
            val data = read(key, encrypted)
            val forced = rejected != null && data.accessToken == rejected && mayForceRefresh(key, rejected)
            if (!forced && !isExpired(data)) {
                account.password = encrypted
                return@withKeyLock data
            }
            if (data.refreshToken.isBlank()) {
                throw signInAgain("no refresh token")
            }
            logger.d { if (forced) "Token rejected, refreshing" else "Token expired, refreshing" }
            val header = authHeader()
            withContext(NonCancellable) {
                val result = OAuthTokenRefresh.refreshOrThrowIO {
                    oauthClient.refreshToken(
                        tokenEndpoint = data.tokenEndpoint,
                        clientId = data.clientId,
                        refreshToken = data.refreshToken,
                        authHeader = header,
                    )
                }
                val refreshed = data.withRefreshResult(result)
                persist(
                    key = key,
                    previous = data,
                    data = refreshed,
                    id = id,
                    replacing = encrypted.takeIf { fromDb != null },
                    account = account,
                ).also { remembered ->
                    decodedLock.withLock {
                        when {
                            remembered.accessToken != refreshed.accessToken -> lastForced.remove(key)
                            forced -> lastForced[key] = refreshed.accessToken to currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    private suspend fun mayForceRefresh(key: String, rejected: String): Boolean {
        val (issued, at) = decodedLock.withLock { lastForced[key] } ?: return true
        if (issued != rejected) {
            return true
        }
        val allowed = currentTimeMillis() - at >= FORCED_REFRESH_INTERVAL_MS
        if (!allowed) {
            logger.w { "Token rejected again within ${FORCED_REFRESH_INTERVAL_MS / 60_000} minutes of a refresh, not refreshing" }
        }
        return allowed
    }

    private suspend fun read(key: String, encrypted: String): OAuthTokenData {
        decodedLock.withLock { decoded[key] }
            ?.takeIf { (cached, _) -> cached == encrypted }
            ?.let { (_, data) -> return data }
        val decrypted = encryption.decrypt(encrypted)
            ?: run {
                logger.e { "Failed to decrypt credentials" }
                throw IllegalStateException("${TokenError.REFRESH_FAILED}: failed to decrypt credentials")
            }
        val data = try {
            OAuthTokenData.deserialize(decrypted)
        } catch (e: Exception) {
            logger.e { "Unreadable credentials: ${e::class.qualifiedName}" }
            throw IllegalStateException(
                "${TokenError.REFRESH_FAILED}: unreadable credentials",
                IllegalStateException(e::class.qualifiedName),
            )
        }
        remember(key, encrypted, data)
        return data
    }

    private suspend fun remember(key: String, encrypted: String, data: OAuthTokenData) {
        decodedLock.withLock { decoded[key] = encrypted to data }
    }

    private suspend fun persist(
        key: String,
        previous: OAuthTokenData,
        data: OAuthTokenData,
        id: Long?,
        replacing: String?,
        account: CaldavAccount,
    ): OAuthTokenData {
        val encrypted = encryption.encrypt(data.serialize())
            ?: throw IllegalStateException("Failed to encrypt credentials")
        var saved = when {
            id == null -> {
                logger.w {
                    "Credentials for a detached account (id=0) cannot be persisted; the next sync may " +
                        "replay a spent refresh token and log the account out"
                }
                true
            }
            replacing == null -> {
                caldavDao.setPassword(id, encrypted)
                true
            }
            else -> caldavDao.replacePassword(id, expected = replacing, password = encrypted) > 0
        }
        while (!saved) {
            val newer = caldavDao.getAccount(id!!)?.password
                ?: throw signInAgain("missing credentials")
            val newerData = try {
                read(key, newer)
            } catch (_: IllegalStateException) {
                null
            }
            if (newerData != null && newerData.refreshToken != previous.refreshToken) {
                logger.i { "Signed in again during refresh, using the new credentials" }
                account.password = newer
                return newerData
            }
            logger.i { "Credentials were rewritten during refresh, keeping the refreshed ones" }
            saved = caldavDao.replacePassword(id, expected = newer, password = encrypted) > 0
        }
        account.password = encrypted
        remember(key, encrypted, data)
        return data
    }

    private class KeyLock {
        val mutex = Mutex()
        var holders = 0
    }

    internal suspend fun lockCount(): Int = locksLock.withLock { locks.size }

    private suspend fun <T> withKeyLock(key: String, block: suspend () -> T): T {
        val lock = locksLock.withLock { locks.getOrPut(key) { KeyLock() }.also { it.holders++ } }
        try {
            return lock.mutex.withLock { block() }
        } finally {
            withContext(NonCancellable) {
                locksLock.withLock {
                    if (--lock.holders == 0) {
                        locks.remove(key)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "OAuthTokenStore"
        const val FORCED_REFRESH_INTERVAL_MS = 5 * 60_000L

        private fun signInAgain(reason: String) = IOException("${TokenError.REFRESH_FAILED}: $reason")
    }
}

internal fun CaldavAccount.clientKey(): String =
    username?.takeIf { it.isNotBlank() } ?: uuid ?: ""

private fun CaldavAccount.lockKey(): String =
    id.takeIf { it != 0L }?.let { "id:$it" } ?: "detached:$accountType:${clientKey()}"
