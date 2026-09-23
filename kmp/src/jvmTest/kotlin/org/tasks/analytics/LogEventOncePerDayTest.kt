package org.tasks.analytics

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LogEventOncePerDayTest {
    private val logged = AtomicInteger()
    private val analytics = object : Analytics {
        override val tasksPreferences = TasksPreferences(InMemoryDataStore())
        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            logged.incrementAndGet()
        }
        override fun addTask(source: String) = Unit
        override fun completeTask(source: String) = Unit
        override fun identify(distinctId: String) = Unit
    }

    @After
    fun tearDown() = DateTimeUtils2.setCurrentMillisSystem()

    @Test
    fun repeatedCallsLogOneEvent() = runBlocking {
        repeat(4) { analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED) }

        assertEquals(1, logged.get())
    }

    @Test
    fun toolCallsArrivingTogetherLogOneEvent() = runBlocking(Dispatchers.Default) {
        val start = CompletableDeferred<Unit>()
        val calls = (1..16).map {
            async {
                start.await()
                analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)
            }
        }
        start.complete(Unit)
        calls.awaitAll()

        assertEquals(1, logged.get())
    }

    @Test
    fun separateDedupeKeysEachLogOneEvent() = runBlocking(Dispatchers.Default) {
        val start = CompletableDeferred<Unit>()
        val calls = listOf("read_only", "read_write").flatMap { mode ->
            (1..8).map {
                async {
                    start.await()
                    analytics.logEventOncePerDay(
                        event = AnalyticsEvents.MCP_TOOL_USED,
                        dedupeBy = mode,
                    )
                }
            }
        }
        start.complete(Unit)
        calls.awaitAll()

        assertEquals(2, logged.get())
    }

    @Test
    fun theNextDayLogsAgain() = runBlocking {
        val today = DateTimeUtils2.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(today)
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        DateTimeUtils2.setCurrentMillisFixed(today + TimeUnit.DAYS.toMillis(2))
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(2, logged.get())
    }
}
