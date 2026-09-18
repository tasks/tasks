package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

class SeverityFilterLogWriter(
    private val delegate: LogWriter,
    private val minSeverity: Severity,
) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= minSeverity && delegate.isLoggable(tag, severity)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) =
        delegate.log(severity, message, tag, throwable)
}
