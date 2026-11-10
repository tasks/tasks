package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory

class KermitSlf4jProviderTest {
    private data class Entry(
        val severity: Severity,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )

    private val entries = mutableListOf<Entry>()
    private val writer = object : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            entries += Entry(severity, tag, message, throwable)
        }
    }
    private val originalWriters = Logger.config.logWriterList
    private val originalSeverity = Logger.config.minSeverity

    @Before
    fun setUp() {
        Logger.setMinSeverity(Severity.Verbose)
        Logger.setLogWriters(writer)
    }

    @After
    fun tearDown() {
        Logger.setLogWriters(originalWriters)
        Logger.setMinSeverity(originalSeverity)
    }

    @Test
    fun `slf4j resolves to the kermit bridge`() {
        assertTrue(
            LoggerFactory.getLogger("at.bitfire.dav4jvm.ResponseParser") is KermitSlf4jLogger
        )
    }

    @Test
    fun `a library warning lands in the app log`() {
        val cause = IllegalStateException("nope")

        LoggerFactory
            .getLogger("at.bitfire.dav4jvm.ktor.ResponseParser")
            .warn("Couldn't parse href: {}", "/dav/cal/a.ics", cause)

        assertEquals(
            listOf(
                Entry(
                    severity = Severity.Warn,
                    tag = "ResponseParser",
                    message = "Couldn't parse href: /dav/cal/a.ics",
                    throwable = cause,
                )
            ),
            entries,
        )
    }
}
