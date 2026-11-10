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
        LoggerFactory
            .getLogger("at.bitfire.dav4jvm.ResponseParser")
            .warn("Ignoring XML response element without valid href")

        assertEquals(
            listOf(
                Entry(
                    severity = Severity.Warn,
                    tag = "ResponseParser",
                    message = "Ignoring XML response element without valid href",
                    throwable = null,
                )
            ),
            entries,
        )
    }

    @Test
    fun `placeholders and throwables survive the crossing`() {
        val cause = IllegalStateException("nope")

        LoggerFactory
            .getLogger("at.bitfire.dav4jvm.ktor.ResponseParser")
            .warn("Couldn't parse href: {}", "/dav/cal/a.ics", cause)

        assertEquals(1, entries.size)
        assertEquals(Severity.Warn, entries.single().severity)
        assertEquals("ResponseParser", entries.single().tag)
        assertEquals("Couldn't parse href: /dav/cal/a.ics", entries.single().message)
        assertEquals(cause, entries.single().throwable)
    }

    @Test
    fun `severities map across`() {
        val logger = LoggerFactory.getLogger("Probe")

        logger.trace("v")
        logger.debug("d")
        logger.info("i")
        logger.warn("w")
        logger.error("e")

        assertEquals(
            listOf(Severity.Verbose, Severity.Debug, Severity.Info, Severity.Warn, Severity.Error),
            entries.map { it.severity },
        )
        assertEquals(listOf("Probe"), entries.map { it.tag }.distinct())
    }

    @Test
    fun `library chatter below info is dropped unless the app wants everything`() {
        Logger.setMinSeverity(Severity.Debug)
        val logger = LoggerFactory.getLogger("at.bitfire.dav4jvm.PropertyRegistry")

        assertTrue(!logger.isDebugEnabled)
        assertTrue(logger.isInfoEnabled)

        logger.trace("Registering Factory for DAV::add-member")
        logger.debug("Registering Factory for DAV::add-member")
        logger.info("Registering DAV property factories")
        logger.warn("Ignoring XML response element without valid href")

        assertEquals(
            listOf("Registering DAV property factories", "Ignoring XML response element without valid href"),
            entries.map { it.message },
        )
    }

    @Test
    fun `kermit min severity gates library logging`() {
        Logger.setMinSeverity(Severity.Warn)
        val logger = LoggerFactory.getLogger("Probe")

        assertTrue(!logger.isDebugEnabled)
        assertTrue(logger.isWarnEnabled)

        logger.debug("dropped")
        logger.warn("kept")

        assertEquals(listOf("kept"), entries.map { it.message })
    }
}
