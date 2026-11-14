package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.logging.Level
import java.util.logging.LogManager

class KermitJulHandlerTest {
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
    private val root = LogManager.getLogManager().getLogger("")
    private val originalHandlers = root.handlers.toList()

    @Before
    fun setUp() {
        Logger.setMinSeverity(Severity.Verbose)
        Logger.setLogWriters(writer)
        installKermitJulHandler()
    }

    @After
    fun tearDown() {
        root.handlers.forEach(root::removeHandler)
        originalHandlers.forEach(root::addHandler)
        Logger.setLogWriters(originalWriters)
        Logger.setMinSeverity(originalSeverity)
        installKermitJulHandler()
        root.handlers.forEach(root::removeHandler)
        originalHandlers.forEach(root::addHandler)
    }

    @Test
    fun `a dav4kmp warning lands in the app log`() {
        val cause = IllegalStateException("nope")

        java.util.logging.Logger
            .getLogger("at.bitfire.dav4jvm.ktor.ResponseParser")
            .log(Level.WARNING, "Unresolvable <href> in <response>: /a", cause)

        assertEquals(
            listOf(Entry(Severity.Warn, "ResponseParser", "Unresolvable <href> in <response>: /a", cause)),
            entries,
        )
    }

    @Test
    fun `parameters are formatted`() {
        java.util.logging.Logger.getLogger("at.bitfire.cert4android.CertUtils")
            .log(Level.INFO, "Trusted {0}", "example.com")

        assertEquals(listOf(Entry(Severity.Info, "CertUtils", "Trusted example.com", null)), entries)
    }

    @Test
    fun `severities map across`() {
        val logger = java.util.logging.Logger.getLogger("at.bitfire.dav4jvm.Probe")

        logger.finest("v")
        logger.fine("d")
        logger.info("i")
        logger.warning("w")
        logger.severe("e")

        assertEquals(
            listOf(Severity.Verbose, Severity.Debug, Severity.Info, Severity.Warn, Severity.Error),
            entries.map { it.severity },
        )
    }

    @Test
    fun `library chatter below info is dropped unless the app wants everything`() {
        Logger.setMinSeverity(Severity.Debug)
        installKermitJulHandler()
        val logger = java.util.logging.Logger.getLogger("at.bitfire.dav4jvm.PropertyRegistry")

        logger.fine("Registering Factory for DAV::add-member")
        logger.info("Registering DAV property factories")

        assertEquals(listOf("Registering DAV property factories"), entries.map { it.message })
    }
}
