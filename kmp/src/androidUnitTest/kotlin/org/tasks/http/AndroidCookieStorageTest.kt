package org.tasks.http

import android.app.Application
import android.content.Context
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidCookieStorageTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val url = Url("https://graph.microsoft.com/v1.0/me/todo/lists")

    @Test
    fun aCookieWithoutADomainIsSentBackToItsHost() = runTest {
        val storage = AndroidCookieStorage(context, "user")

        storage.addCookie(url, Cookie("session", "1"))

        assertEquals(listOf("1"), storage.get(url).map { it.value })
    }

    @Test
    fun cookiesSurviveANewInstance() = runTest {
        AndroidCookieStorage(context, "user").apply {
            addCookie(url, Cookie("session", "1"))
            flush()
        }

        val restored = AndroidCookieStorage(context, "user").get(url).single()

        assertEquals("session", restored.name)
        assertEquals("1", restored.value)
        assertEquals("graph.microsoft.com", restored.domain)
    }

    @Test
    fun cookiesSavedByOlderVersionsAreDropped() = runTest {
        val prefs = context.getSharedPreferences("cookies_user", Context.MODE_PRIVATE)
        prefs.edit().putString("session", Cookie("session", "1").toString()).commit()

        val storage = AndroidCookieStorage(context, "user")

        assertTrue(storage.get(url).isEmpty())
        assertFalse(prefs.contains("session"))
    }

    @Test
    fun accountsKeepSeparateCookies() = runTest {
        AndroidCookieStorage(context, "a").apply {
            addCookie(url, Cookie("session", "1"))
            flush()
        }

        assertTrue(AndroidCookieStorage(context, "b").get(url).isEmpty())
    }

    @Test
    fun cookiesWithTheSameNameOnDifferentPathsSurviveANewInstance() = runTest {
        val beta = Url("https://graph.microsoft.com/beta/me/todo/lists")
        AndroidCookieStorage(context, "user").apply {
            addCookie(url, Cookie("session", "v1", path = "/v1.0"))
            addCookie(beta, Cookie("session", "beta", path = "/beta"))
            flush()
        }

        val restored = AndroidCookieStorage(context, "user")

        assertEquals(listOf("v1"), restored.get(url).map { it.value })
        assertEquals(listOf("beta"), restored.get(beta).map { it.value })
    }
}
