package org.tasks.feed

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.analytics.CrashReporting
import org.tasks.http.OkHttpClientFactory
import org.tasks.jobs.WorkResult
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences

class BlogFeedCheckerTest {
    private val server = MockWebServer().apply { start() }
    private val reported = mutableListOf<Throwable>()

    private val checker = BlogFeedChecker(
        httpClientFactory = object : OkHttpClientFactory {
            override suspend fun newClient(
                foreground: Boolean,
                cookieKey: String?,
                block: (OkHttpClient.Builder) -> Unit,
            ): OkHttpClient = OkHttpClient.Builder().also(block).build()
        },
        tasksPreferences = TasksPreferences(InMemoryDataStore()),
        appPreferences = mock<AppPreferences> {
            onBlocking { getInstallDate() } doReturn 0L
        },
        crashReporting = object : CrashReporting {
            override fun reportException(t: Throwable, fatal: Boolean) {
                reported.add(t)
            }
        },
        feedUrl = server.url("/blog/rss.xml").toString(),
    )

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun anHtmlPageInPlaceOfTheFeedIsNotReported() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("<html>\n<meta http-equiv='null' content=no-cache>\n</html>")
        )

        assertEquals(WorkResult.Fail, checker.check())
        assertTrue(reported.isEmpty())
    }

    @Test
    fun aMalformedFeedIsReported() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/xml")
                .setBody("<rss>\n<meta http-equiv='null' content=no-cache>\n</rss>")
        )

        assertEquals(WorkResult.Fail, checker.check())
        assertEquals(1, reported.size)
    }

    @Test
    fun theFeedIsParsed() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/xml")
                .setBody("<rss><channel></channel></rss>")
        )

        assertEquals(WorkResult.Success, checker.check())
        assertTrue(reported.isEmpty())
    }
}
