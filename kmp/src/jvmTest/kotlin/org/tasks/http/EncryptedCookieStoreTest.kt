package org.tasks.http

import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptedCookieStoreTest {

    private lateinit var dir: File
    private val encryption = KeyStoreEncryption(object : KeyProvider {
        private val key: SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun getKey() = key
    })

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("cookie-store-test").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store(key: String? = null) =
        EncryptedCookieStore.createForTest(EncryptedCookieStore.cookieFile(dir, key), encryption)

    private fun cookie(
        name: String = "session",
        value: String = "abc123",
        domain: String = "example.com",
        expiresAt: Long = System.currentTimeMillis() + 60_000,
    ) = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(domain)
        .path("/")
        .expiresAt(expiresAt)
        .build()

    @Test
    fun persistsCookiesAcrossInstances() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        store().initialize().apply {
            saveFromResponse(url, listOf(cookie()))
            flush()
        }

        val loaded = store().initialize().loadForRequest(url)

        assertEquals(1, loaded.size)
        assertEquals("session", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun evictsExpiredCookies() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        store()
            .initialize()
            .saveFromResponse(url, listOf(cookie(expiresAt = System.currentTimeMillis() - 1)))

        val loaded = store().initialize().loadForRequest(url)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun doesNotLeakCookiesAcrossKeys() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        store(key = "alice").initialize().saveFromResponse(url, listOf(cookie()))

        val other = store(key = "bob").initialize().loadForRequest(url)

        assertTrue(other.isEmpty())
    }

    @Test
    fun onlyReturnsCookiesMatchingUrl() = runBlocking {
        store().initialize().apply {
            saveFromResponse(
                "https://example.com/".toHttpUrl(),
                listOf(cookie(domain = "example.com")),
            )
            flush()
        }

        val loaded = store().initialize().loadForRequest("https://other.com/".toHttpUrl())

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun replacesCookieWhenSecureFlagChanges() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        val store = store().initialize()
        store.saveFromResponse(
            url,
            listOf(
                Cookie.Builder().name("session").value("a").domain("example.com").path("/").build()
            ),
        )
        store.saveFromResponse(
            url,
            listOf(
                Cookie.Builder().name("session").value("b").domain("example.com").path("/").secure()
                    .build()
            ),
        )

        val loaded = store.loadForRequest(url)

        assertEquals(1, loaded.size)
        assertEquals("b", loaded[0].value)
    }

    @Test
    fun sharesInMemoryStateForSharedFile() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        val file = EncryptedCookieStore.cookieFile(dir, null)
        val first = EncryptedCookieStore.getOrCreate(file, encryption)
        val second = EncryptedCookieStore.getOrCreate(file, encryption)

        first.saveFromResponse(url, listOf(cookie(name = "one")))
        second.saveFromResponse(url, listOf(cookie(name = "two")))
        second.flush()

        val reloaded = store().initialize().loadForRequest(url)
        assertEquals(setOf("one", "two"), reloaded.map { it.name }.toSet())
    }

    @Test
    fun encryptsCookiesAtRest() = runBlocking {
        val url = "https://example.com/".toHttpUrl()
        store().initialize().apply {
            saveFromResponse(url, listOf(cookie()))
            flush()
        }

        val file = EncryptedCookieStore.cookieFile(dir, null)
        assertTrue(file.exists())
        val contents = file.readText()
        assertFalse(contents.contains("session"))
        assertFalse(contents.contains("abc123"))
    }

    @Test
    fun handlesMissingFile() = runBlocking {
        val loaded = store().initialize().loadForRequest("https://example.com/".toHttpUrl())

        assertTrue(loaded.isEmpty())
        assertNull(EncryptedCookieStore.cookieFile(dir, null).takeIf { it.exists() })
    }
}
