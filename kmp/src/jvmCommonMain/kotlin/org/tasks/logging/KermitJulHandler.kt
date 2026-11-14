package org.tasks.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter

internal class KermitJulHandler : Handler() {

    init {
        formatter = SimpleFormatter()
    }

    override fun publish(record: LogRecord) {
        val severity = record.level.toSeverity()
        if (!isLibraryLoggable(severity)) {
            return
        }
        val loggerName = record.loggerName.orEmpty()
        val tag = loggerName.substringAfterLast('.').ifBlank { loggerName }
        Logger.logBlock(severity, tag, record.thrown) {
            formatter.formatMessage(record)
        }
    }

    override fun flush() = Unit

    override fun close() = Unit

    private fun Level.toSeverity() = when {
        intValue() >= Level.SEVERE.intValue() -> Severity.Error
        intValue() >= Level.WARNING.intValue() -> Severity.Warn
        intValue() >= Level.INFO.intValue() -> Severity.Info
        intValue() >= Level.FINE.intValue() -> Severity.Debug
        else -> Severity.Verbose
    }
}

private val bitfireLogger = java.util.logging.Logger.getLogger("at.bitfire")

fun installKermitJulHandler() {
    val root = LogManager.getLogManager().getLogger("")
    root.handlers.forEach(root::removeHandler)
    root.addHandler(KermitJulHandler())
    bitfireLogger.level = if (Logger.config.minSeverity == Severity.Verbose) Level.ALL else null
}
