package org.tasks.http

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import io.ktor.util.date.GMTDate
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CookieBackend {
    suspend fun read(): List<String>

    suspend fun write(cookies: List<String>): Boolean

    suspend fun delete()
}

class PersistentCookiesStorage(
    private val backend: CookieBackend,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : CookiesStorage {
    private val logger = Logger.withTag("PersistentCookiesStorage")
    private val mutex = Mutex()
    private val persistMutex = Mutex()
    private val cookies = mutableListOf<Cookie>()
    private var loaded = false
    private var discarded = false
    private var lastPersisted: List<String>? = null

    private val writeSignal = Channel<Unit>(Channel.CONFLATED)
    private val writer = scope.launch {
        for (ignored in writeSignal) {
            persistSafely()
        }
    }.apply { invokeOnCompletion { scope.cancel() } }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val (result, evicted) = mutex.withLock {
            ensureLoaded()
            val removed = removeExpired(getTimeMillis())
            cookies.filter { it.matches(requestUrl) } to removed
        }
        if (evicted) schedulePersist()
        return result
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name.isBlank()) return
        val filled = cookie.fillDefaults(requestUrl).withAbsoluteExpiry(getTimeMillis())
        mutex.withLock {
            ensureLoaded()
            cookies.removeAll { it.sameIdentityAs(filled) }
            cookies.add(filled)
        }
        schedulePersist()
    }

    override fun close() {
        writeSignal.close()
    }

    suspend fun closeAndFlush() {
        writeSignal.close()
        writer.join()
        persistSafely()
    }

    suspend fun discard() {
        writeSignal.close()
        writer.join()
        persistMutex.withLock {
            mutex.withLock {
                discarded = true
                cookies.clear()
                loaded = true
            }
            runCatching { backend.delete() }
                .onFailure { logger.e(it) { "Failed to delete cookies" } }
        }
    }

    suspend fun flush() = persistLatest()

    private suspend fun schedulePersist() {
        if (writeSignal.trySend(Unit).isClosed) {
            persistSafely()
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        backend.read().forEach { header ->
            runCatching { parseServerSetCookieHeader(header) }
                .getOrNull()
                ?.takeIf { it.name.isNotBlank() && it.domain != null && it.path != null }
                ?.let { cookie ->
                    cookies.removeAll { it.sameIdentityAs(cookie) }
                    cookies.add(cookie)
                }
        }
        loaded = true
    }

    private fun removeExpired(now: Long): Boolean = cookies.removeAll { it.isExpired(now) }

    private suspend fun persistSafely() {
        runCatching { persistLatest() }
            .onFailure { logger.e(it) { "Failed to persist cookies" } }
    }

    private suspend fun persistLatest() = persistMutex.withLock {
        val rendered = mutex.withLock {
            if (loaded && !discarded) cookies.map { renderSetCookieHeader(it) } else null
        } ?: return@withLock
        if (rendered == lastPersisted) return@withLock
        if (backend.write(rendered)) {
            lastPersisted = rendered
        }
    }
}

private fun Cookie.withAbsoluteExpiry(now: Long): Cookie {
    val seconds = maxAge ?: return this
    return copy(maxAge = null, expires = GMTDate(now + seconds.toLong() * 1000L))
}

private fun Cookie.isExpired(now: Long): Boolean = expires?.timestamp?.let { it <= now } ?: false

private fun Cookie.sameIdentityAs(other: Cookie): Boolean =
    name == other.name && normalizedDomain == other.normalizedDomain && path == other.path

private val Cookie.normalizedDomain: String?
    get() = domain?.lowercase()?.trimStart('.')
