package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import timber.log.Timber

class TimberLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val priority = severity.toTimberPriority()
        if (throwable != null) {
            Timber.tag(tag).log(priority, throwable, message)
        } else {
            Timber.tag(tag).log(priority, message)
        }
    }

    private fun Severity.toTimberPriority(): Int = when (this) {
        Severity.Verbose -> android.util.Log.VERBOSE
        Severity.Debug -> android.util.Log.DEBUG
        Severity.Info -> android.util.Log.INFO
        Severity.Warn -> android.util.Log.WARN
        Severity.Error -> android.util.Log.ERROR
        Severity.Assert -> android.util.Log.ASSERT
    }
}
