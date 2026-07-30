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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.serializer
import org.tasks.security.KeyStoreEncryption
import java.io.File

class DesktopCookieStorage internal constructor(
    private val storage: EncryptedFile,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : CookiesStorage {
    private val logger = Logger.withTag("DesktopCookieStorage")
    private val mutex = Mutex()

    private val persistMutex = Mutex()
    private val cookies = mutableListOf<Cookie>()
    private var loaded = false
    private var lastPersisted: List<String>? = null

    private val writeSignal = Channel<Unit>(Channel.CONFLATED)
    private val writer = scope.launch {
        for (ignored in writeSignal) {
            runCatching { persistLatest() }
                .onFailure { logger.e(it) { "Failed to persist cookies" } }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val (result, evicted) = mutex.withLock {
            ensureLoaded()
            val removed = removeExpired()
            cookies.filter { it.matches(requestUrl) } to removed
        }
        if (evicted) writeSignal.trySend(Unit)
        return result
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            ensureLoaded()
            if (cookie.name.isBlank()) return
            cookies.removeAll { it.name == cookie.name && it.matches(requestUrl) }
            cookies.add(cookie.fillDefaults(requestUrl).withAbsoluteExpiry(getTimeMillis()))
        }
        writeSignal.trySend(Unit)
    }

    override fun close() {
        writeSignal.close()
        runBlocking {
            writer.join()
            persistLatest()
        }
        scope.cancel()
    }

    suspend fun flush() = persistLatest()

    private suspend fun ensureLoaded() {
        if (loaded) return
        storage.readList(String.serializer()).forEach {
            runCatching { cookies.add(parseServerSetCookieHeader(it)) }
        }
        loaded = true
    }

    private fun removeExpired(): Boolean {
        val now = getTimeMillis()
        return cookies.removeAll { cookie ->
            cookie.expires?.timestamp?.let { it < now } ?: false
        }
    }

    private suspend fun persistLatest() = persistMutex.withLock {
        val rendered = mutex.withLock {
            if (loaded) cookies.map { renderSetCookieHeader(it) } else null
        } ?: return@withLock
        if (rendered == lastPersisted) return@withLock
        if (storage.writeList(String.serializer(), rendered)) {
            lastPersisted = rendered
        }
    }

    companion object {
        fun forKey(cookieDir: File, key: String?, encryption: KeyStoreEncryption) =
            DesktopCookieStorage(EncryptedFile(cookieFile(cookieDir, key, "msgraph"), encryption))
    }
}

private fun Cookie.withAbsoluteExpiry(now: Long): Cookie {
    val seconds = maxAge ?: return this
    return copy(maxAge = null, expires = GMTDate(now + seconds.toLong() * 1000L))
}
