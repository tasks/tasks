package org.tasks.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class KermitSlf4jProvider : SLF4JServiceProvider {
    private val loggerFactory = ILoggerFactory { name -> KermitSlf4jLogger(name) }
    private val markerFactory = BasicMarkerFactory()
    private val mdcAdapter = NOPMDCAdapter()

    override fun getLoggerFactory(): ILoggerFactory = loggerFactory

    override fun getMarkerFactory(): IMarkerFactory = markerFactory

    override fun getMDCAdapter(): MDCAdapter = mdcAdapter

    override fun getRequestedApiVersion(): String = REQUESTED_API_VERSION

    override fun initialize() = Unit

    private companion object {
        private const val REQUESTED_API_VERSION = "2.0.99"
    }
}

internal class KermitSlf4jLogger(private val loggerName: String) : LegacyAbstractLogger() {
    private val kermitTag = loggerName.substringAfterLast('.').ifBlank { loggerName }

    override fun getName(): String = loggerName

    override fun getFullyQualifiedCallerName(): String? = null

    override fun isTraceEnabled(): Boolean = isLoggable(Severity.Verbose)

    override fun isDebugEnabled(): Boolean = isLoggable(Severity.Debug)

    override fun isInfoEnabled(): Boolean = isLoggable(Severity.Info)

    override fun isWarnEnabled(): Boolean = isLoggable(Severity.Warn)

    override fun isErrorEnabled(): Boolean = isLoggable(Severity.Error)

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val severity = level.toSeverity()
        if (!isLoggable(severity)) {
            return
        }
        Logger.logBlock(severity, kermitTag, throwable) {
            MessageFormatter.basicArrayFormat(messagePattern, arguments)
        }
    }

    private fun isLoggable(severity: Severity): Boolean {
        val appMinSeverity = Logger.config.minSeverity
        if (severity < appMinSeverity) {
            return false
        }
        return appMinSeverity == Severity.Verbose || severity >= LIBRARY_MIN_SEVERITY
    }

    private fun Level?.toSeverity() = when (this) {
        Level.TRACE -> Severity.Verbose
        Level.DEBUG -> Severity.Debug
        Level.INFO -> Severity.Info
        Level.WARN -> Severity.Warn
        Level.ERROR, null -> Severity.Error
    }

    private companion object {
        private val LIBRARY_MIN_SEVERITY = Severity.Info
    }
}
