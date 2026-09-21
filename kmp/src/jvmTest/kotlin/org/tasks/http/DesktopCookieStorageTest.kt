package org.tasks.http

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class DesktopCookieStorageTest {

    private lateinit var dir: File
    private val encryption = KeyStoreEncryption(object : KeyProvider {
        private val key: SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun getKey() = key
    })

    private val url = Url("https://graph.microsoft.com/v1.0/me")

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("ktor-cookie-store-test").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun storage(key: String?) =
        DesktopCookieStorage.forKey(dir, key, encryption)

    private fun cookie(
        name: String = "session",
        value: String = "abc123",
        expiresAt: Long = System.currentTimeMillis() + 60_000,
    ) = Cookie(
        name = name,
        value = value,
        expires = GMTDate(expiresAt),
        domain = "graph.microsoft.com",
        path = "/",
    )

    @Test
    fun persistsCookiesAcrossInstances() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(url, cookie())
        alice.flush()

        val loaded = storage(key = "alice").get(url)

        assertEquals(1, loaded.size)
        assertEquals("session", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun persistsEveryCookieFromABurst() = runBlocking {
        val alice = storage(key = "alice")
        repeat(5) { i -> alice.addCookie(url, cookie(name = "c$i", value = "v$i")) }
        alice.flush()

        val loaded = storage(key = "alice").get(url).associate { it.name to it.value }

        assertEquals(5, loaded.size)
        repeat(5) { i -> assertEquals("v$i", loaded["c$i"]) }
    }

    @Test
    fun isolatesCookiesPerAccount() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(url, cookie())
        alice.flush()

        val bob = storage(key = "bob").get(url)

        assertTrue(bob.isEmpty())
    }

    @Test
    fun evictsExpiredCookies() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(url, cookie(expiresAt = System.currentTimeMillis() - 60_000))
        alice.flush()

        val loaded = storage(key = "alice").get(url)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun normalizesMaxAgeToAbsoluteExpiry() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(
            url,
            Cookie(name = "x", value = "1", maxAge = 600, domain = "graph.microsoft.com", path = "/"),
        )
        alice.flush()

        val loaded = storage(key = "alice").get(url)

        assertEquals(1, loaded.size)
        assertNotNull(loaded[0].expires)
    }

    @Test
    fun evictsExpiredMaxAgeCookiesAcrossInstances() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(
            url,
            Cookie(name = "x", value = "1", maxAge = -1, domain = "graph.microsoft.com", path = "/"),
        )
        alice.flush()

        val loaded = storage(key = "alice").get(url)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun encryptsCookiesAtRest() = runBlocking {
        val alice = storage(key = "alice")
        alice.addCookie(url, cookie())
        alice.flush()

        val file = cookieFile(dir, "alice", "msgraph")
        assertTrue(file.exists())
        val contents = file.readText()
        assertFalse(contents.contains("session"))
        assertFalse(contents.contains("abc123"))
    }
}
