package org.tasks.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2
import org.tasks.time.startOfDay
import org.tasks.time.startOfDayUtc
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LogEventOncePerDayTest {
    private class TestAnalytics(
        override val tasksPreferences: TasksPreferences = TasksPreferences(InMemoryDataStore()),
    ) : Analytics, CrashReporting {
        val logged = AtomicInteger()
        val reported = AtomicInteger()

        @Volatile
        var enabled = true

        @Volatile
        var failNextSend = false

        @Volatile
        var cancelNextSend = false

        @Volatile
        var reporterThrows = false

        @Volatile
        var beforeSend: (() -> Unit)? = null

        override val analyticsEnabled: Boolean
            get() = enabled

        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            beforeSend?.invoke()
            if (cancelNextSend) {
                cancelNextSend = false
                throw CancellationException("cancelled")
            }
            if (failNextSend) {
                failNextSend = false
                throw IllegalStateException("send failed")
            }
            logged.incrementAndGet()
        }

        override fun reportException(t: Throwable, fatal: Boolean) {
            reported.incrementAndGet()
            if (reporterThrows) {
                throw IllegalStateException("the reporter is the broken part")
            }
        }

        override fun addTask(source: String) = Unit
        override fun completeTask(source: String) = Unit
        override fun setSku(sku: String?) = Unit
    }

    private class DetachedDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val scope = CoroutineScope(Dispatchers.Default)
        val writing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences = scope
            .async {
                writing.complete(Unit)
                release.await()
                transform(state.value).also { state.value = it }
            }
            .await()

        fun close() = scope.cancel()
    }

    private class BrokenDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences = throw IOException("the preference store is the broken part")
    }

    private val analytics = TestAnalytics()
    private val logged get() = analytics.logged

    private suspend fun claimedDay(): Long = analytics.tasksPreferences.get(KEY, ABSENT)

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

    @Test
    fun theDayIsTheUtcDayNotTheLocalOne() = withTimeZone(AUCKLAND) {
        runBlocking {
            DateTimeUtils2.setCurrentMillisFixed(utc("2026-11-10T10:00:00Z"))

            analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

            assertEquals(utc("2026-11-10T00:00:00Z"), claimedDay())
            assertNotEquals(utc("2026-11-10T10:00:00Z").startOfDay(), claimedDay())
        }
    }

    @Test
    fun aNewLocalDayInsideTheSameUtcDayDoesNotLogAgain() = withTimeZone(AUCKLAND) {
        runBlocking {
            DateTimeUtils2.setCurrentMillisFixed(utc("2026-11-10T10:00:00Z"))
            analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

            DateTimeUtils2.setCurrentMillisFixed(utc("2026-11-10T11:30:00Z"))
            analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

            assertEquals(1, logged.get())
        }
    }

    @Test
    fun aSendThatThrowsKeepsTheDay() = runBlocking {
        val today = DateTimeUtils2.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(today)
        analytics.failNextSend = true

        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(0, logged.get())
        assertEquals(today.startOfDayUtc(), claimedDay())
    }

    @Test
    fun aSendThatThrowsIsNotRetriedTheSameDay() = runBlocking {
        analytics.failNextSend = true
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(0, logged.get())
    }

    @Test
    fun aSendThatThrowsIsReported() = runBlocking {
        analytics.failNextSend = true

        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(1, analytics.reported.get())
    }

    @Test
    fun aSendThatThrowsDoesNotBlockTheNextDay() = runBlocking {
        val firstDay = DateTimeUtils2.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(firstDay)
        analytics.failNextSend = true
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        DateTimeUtils2.setCurrentMillisFixed(firstDay + TimeUnit.DAYS.toMillis(1))
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(1, logged.get())
    }

    @Test
    fun aSendFailsAtMostOncePerDaySoEveryFailureIsWorthReporting() = runBlocking {
        val firstDay = DateTimeUtils2.currentTimeMillis()
        repeat(5) { day ->
            DateTimeUtils2.setCurrentMillisFixed(firstDay + TimeUnit.DAYS.toMillis(day.toLong()))
            analytics.failNextSend = true
            repeat(4) { analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED) }
        }

        assertEquals(0, logged.get())
        assertEquals(5, analytics.reported.get())
    }

    @Test
    fun aCancelledSendReachesTheCallerUnreported() {
        val today = DateTimeUtils2.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(today)
        analytics.cancelNextSend = true

        assertThrows(CancellationException::class.java) {
            runBlocking { analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED) }
        }

        assertEquals(0, analytics.reported.get())
        assertEquals(today.startOfDayUtc(), runBlocking { claimedDay() })
    }

    @Test
    fun aCrashReporterThatThrowsDoesNotReachTheCaller() = runBlocking {
        analytics.failNextSend = true
        analytics.reporterThrows = true

        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(0, logged.get())
    }

    @Test
    fun aCrashReporterThatThrowsStillLeavesTheNextDayOpen() = runBlocking {
        val firstDay = DateTimeUtils2.currentTimeMillis()
        DateTimeUtils2.setCurrentMillisFixed(firstDay)
        analytics.failNextSend = true
        analytics.reporterThrows = true
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        analytics.reporterThrows = false
        DateTimeUtils2.setCurrentMillisFixed(firstDay + TimeUnit.DAYS.toMillis(1))
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(1, logged.get())
    }

    @Test
    fun aClaimThatThrowsSendsNothingAndKeepsNoDay() = runBlocking {
        val broken = TestAnalytics(TasksPreferences(BrokenDataStore()))

        broken.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(0, broken.logged.get())
        assertEquals(ABSENT, broken.tasksPreferences.get(KEY, ABSENT))
    }

    @Test
    fun aClaimThatThrowsIsReported() = runBlocking {
        val broken = TestAnalytics(TasksPreferences(BrokenDataStore()))

        broken.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(1, broken.reported.get())
    }

    @Test
    fun aClaimThatThrowsWithABrokenReporterDoesNotReachTheCaller() = runBlocking {
        val broken = TestAnalytics(TasksPreferences(BrokenDataStore()))
        broken.reporterThrows = true

        broken.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(0, broken.logged.get())
    }

    @Test
    fun aCallerCancelledWhileClaimingTheDayStillSendsTheEvent() = runBlocking {
        val store = DetachedDataStore()
        try {
            val cancelled = TestAnalytics(TasksPreferences(store))
            val job = launch(Dispatchers.Default) {
                cancelled.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)
            }

            store.writing.await()
            job.cancel()
            store.release.complete(Unit)
            job.join()

            assertEquals(1, cancelled.logged.get())
        } finally {
            store.close()
        }
    }

    @Test
    fun aCallerCancelledWhileClaimingStillGetsItsFailureReported() = runBlocking {
        val store = DetachedDataStore()
        try {
            val cancelled = TestAnalytics(TasksPreferences(store))
            cancelled.failNextSend = true
            val job = launch(Dispatchers.Default) {
                cancelled.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)
            }

            store.writing.await()
            job.cancel()
            store.release.complete(Unit)
            job.join()

            assertEquals(0, cancelled.logged.get())
            assertEquals(1, cancelled.reported.get())
        } finally {
            store.close()
        }
    }

    @Test
    fun optedOutLogsNothing() = runBlocking {
        analytics.enabled = false

        repeat(4) { analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED) }

        assertEquals(0, logged.get())
    }

    @Test
    fun optedOutWritesNoDedupeKey() = runBlocking {
        analytics.enabled = false

        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(ABSENT, claimedDay())
    }

    @Test
    fun aDayPassedOverWhileDisabledIsNotBurnt() = runBlocking {
        DateTimeUtils2.setCurrentMillisFixed(DateTimeUtils2.currentTimeMillis())
        analytics.enabled = false
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)
        assertEquals(0, logged.get())

        analytics.enabled = true
        analytics.logEventOncePerDay(AnalyticsEvents.MCP_TOOL_USED)

        assertEquals(1, logged.get())
    }

    companion object {
        private const val AUCKLAND = "Pacific/Auckland"

        private const val ABSENT = -1L

        private val KEY = longPreferencesKey(
            utcDayKey(
                event = AnalyticsEvents.MCP_TOOL_USED,
                dedupeBy = AnalyticsEvents.MCP_TOOL_USED,
            )
        )

        private fun utc(timestamp: String): Long =
            java.time.Instant.parse(timestamp).toEpochMilli()

        private fun withTimeZone(id: String, block: () -> Unit) {
            val default = java.util.TimeZone.getDefault()
            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(id))
                block()
            } finally {
                java.util.TimeZone.setDefault(default)
            }
        }
    }
}
