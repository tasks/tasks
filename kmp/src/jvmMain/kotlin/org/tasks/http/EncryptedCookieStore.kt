package org.tasks.http

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.tasks.security.KeyStoreEncryption
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class EncryptedCookieStore private constructor(
    file: File,
    encryption: KeyStoreEncryption,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : CookieJar {
    private val logger = Logger.withTag("EncryptedCookieStore")
    private val storage = EncryptedFile(file, encryption)
    private val lock = Any()
    private val cookies = LinkedHashMap<CookieKey, Cookie>()

    private val initMutex = Mutex()
    @Volatile private var initialized = false

    private val persistMutex = Mutex()
    private var lastPersisted: List<StoredCookie>? = null

    private val writeSignal = Channel<Unit>(Channel.CONFLATED)
    private val writer = scope.launch {
        for (ignored in writeSignal) {
            runCatching { persistLatest() }
                .onFailure { logger.e(it) { "Failed to persist cookies" } }
        }
    }

    suspend fun initialize(): EncryptedCookieStore {
        if (initialized) return this
        initMutex.withLock {
            if (initialized) return this
            val loaded = storage.readList(StoredCookie.serializer())
                .mapNotNull { runCatching { it.toCookie() }.getOrNull() }
            val now = System.currentTimeMillis()
            synchronized(lock) {
                loaded.forEach { if (it.expiresAt > now) cookies[it.key()] = it }
            }
            initialized = true
        }
        return this
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            cookies.forEach { this.cookies[it.key()] = it }
        }
        writeSignal.trySend(Unit)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        var evicted = false
        val valid = synchronized(lock) {
            val result = ArrayList<Cookie>()
            val iterator = cookies.values.iterator()
            while (iterator.hasNext()) {
                val cookie = iterator.next()
                if (cookie.expiresAt < now) {
                    iterator.remove()
                    evicted = true
                } else if (cookie.matches(url)) {
                    result.add(cookie)
                }
            }
            result
        }
        if (evicted) writeSignal.trySend(Unit)
        return valid
    }

    suspend fun flush() = persistLatest()

    private suspend fun persistLatest() = persistMutex.withLock {
        val snapshot = synchronized(lock) { cookies.values.map { it.toStored() } }
        if (snapshot == lastPersisted) return@withLock
        if (storage.writeList(StoredCookie.serializer(), snapshot)) {
            lastPersisted = snapshot
        }
    }

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
        val persistent: Boolean,
    ) {
        fun toCookie(): Cookie {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .path(path)
            if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
            if (persistent) builder.expiresAt(expiresAt)
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            return builder.build()
        }
    }

    private data class CookieKey(
        val name: String,
        val domain: String,
        val path: String,
    )

    private fun Cookie.key() = CookieKey(name, domain, path)

    private fun Cookie.toStored() = StoredCookie(
        name = name,
        value = value,
        expiresAt = expiresAt,
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httpOnly,
        hostOnly = hostOnly,
        persistent = persistent,
    )

    companion object {
        private val stores = ConcurrentHashMap<String, EncryptedCookieStore>()

        suspend fun getOrCreate(file: File, encryption: KeyStoreEncryption): EncryptedCookieStore {
            val key = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            return stores
                .computeIfAbsent(key) { EncryptedCookieStore(file, encryption) }
                .initialize()
        }

        suspend fun flushAll() = stores.values.forEach { runCatching { it.flush() } }

        fun cookieFile(dir: File, key: String?): File = cookieFile(dir, key, "cookies")

        internal fun createForTest(file: File, encryption: KeyStoreEncryption) =
            EncryptedCookieStore(file, encryption)
    }
}
