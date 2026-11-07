package org.tasks.http

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentCookiesStorageTest {

    private class MemoryBackend(var saved: List<String>? = emptyList()) : CookieBackend {
        override suspend fun read() = saved ?: emptyList()

        override suspend fun write(cookies: List<String>): Boolean {
            saved = cookies
            return true
        }

        override suspend fun delete() {
            saved = null
        }
    }

    private val backend = MemoryBackend()
    private val lists = Url("https://graph.microsoft.com/v1.0/me/todo/lists")
    private val beta = Url("https://graph.microsoft.com/beta/me/todo/lists")
    private val login = Url("https://login.microsoftonline.com/common")

    private fun storage() = PersistentCookiesStorage(backend)

    @Test
    fun discardingDeletesTheSavedCookies() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1"))
        storage.flush()

        storage.discard()

        assertEquals(null, backend.saved)
    }

    @Test
    fun aDiscardedStorageNeverWritesAgain() = runTest {
        val storage = storage()
        storage.discard()

        storage.addCookie(lists, Cookie("session", "1"))
        storage.flush()
        storage.close()

        assertEquals(null, backend.saved)
    }

    @Test
    fun aCookieWithMaxAgeZeroDeletesTheSavedCookie() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1"))

        storage.addCookie(lists, Cookie("session", "", maxAge = 0))
        storage.flush()

        assertTrue(storage.get(lists).isEmpty())
        assertTrue(storage().get(lists).isEmpty())
    }

    @Test
    fun aCookieThatExpiredInThePastDeletesTheSavedCookie() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1"))

        storage.addCookie(lists, Cookie("session", "", expires = GMTDate(getTimeMillis() - 60_000)))

        assertTrue(storage.get(lists).isEmpty())
    }

    @Test
    fun maxAgeIsSavedAsAnAbsoluteExpiry() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1", maxAge = 600))
        storage.flush()

        val restored = storage().get(lists).single()

        assertEquals(null, restored.maxAge)
        assertTrue(restored.expires!!.timestamp > getTimeMillis())
    }

    @Test
    fun aSavedCookiePastItsExpiryIsNotRestored() = runTest {
        backend.saved = listOf("session=1; Expires=Thu, 01 Jan 2015 00:00:00 GMT; Domain=graph.microsoft.com; Path=/")

        assertTrue(storage().get(lists).isEmpty())
    }

    @Test
    fun cookiesWithTheSameNameOnDifferentPathsAreKeptApart() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "v1", path = "/v1.0"))
        storage.addCookie(beta, Cookie("session", "beta", path = "/beta"))
        storage.flush()

        val restored = storage()
        assertEquals(listOf("v1"), restored.get(lists).map { it.value })
        assertEquals(listOf("beta"), restored.get(beta).map { it.value })
    }

    @Test
    fun cookiesWithTheSameNameOnDifferentHostsAreKeptApart() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "graph"))
        storage.addCookie(login, Cookie("session", "login"))

        assertEquals(listOf("graph"), storage.get(lists).map { it.value })
        assertEquals(listOf("login"), storage.get(login).map { it.value })
    }

    @Test
    fun aCookieForTheSameHostAndPathReplacesTheOldOne() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1", path = "/"))
        storage.addCookie(beta, Cookie("session", "2", path = "/", domain = ".Graph.Microsoft.com"))

        assertEquals(listOf("2"), storage.get(lists).map { it.value })
    }

    @Test
    fun closingDoesNotWaitForAPendingWrite() = runTest {
        val gate = CompletableDeferred<Unit>()
        val blocked = object : CookieBackend {
            override suspend fun read() = emptyList<String>()

            override suspend fun write(cookies: List<String>): Boolean {
                gate.await()
                return backend.write(cookies)
            }

            override suspend fun delete() = backend.delete()
        }
        val storage = PersistentCookiesStorage(blocked)
        storage.addCookie(lists, Cookie("session", "1"))

        storage.close()
        gate.complete(Unit)
        storage.closeAndFlush()

        assertEquals(listOf("1"), storage().get(lists).map { it.value })
    }

    @Test
    fun closeAndFlushSavesPendingCookies() = runTest {
        val storage = storage()
        storage.addCookie(lists, Cookie("session", "1"))

        storage.closeAndFlush()

        assertEquals(listOf("1"), storage().get(lists).map { it.value })
    }

    @Test
    fun aCookieAddedAfterCloseIsStillSaved() = runTest {
        val storage = storage()
        storage.close()

        storage.addCookie(lists, Cookie("session", "1"))

        assertEquals(listOf("1"), storage().get(lists).map { it.value })
    }

    @Test
    fun savedCookiesWithoutADomainAreSkipped() = runTest {
        backend.saved = listOf("session=1")

        assertTrue(storage().get(lists).isEmpty())
    }

    @Test
    fun aCookieWithoutANameIsIgnored() = runTest {
        val storage = storage()

        storage.addCookie(lists, Cookie("", "1"))
        storage.flush()

        assertTrue(storage.get(lists).isEmpty())
        assertFalse(backend.saved.orEmpty().isNotEmpty())
    }
}
