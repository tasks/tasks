package org.tasks.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.logging.Formatter
import java.util.logging.LogRecord

class LogFormatter : Formatter() {

    override fun format(r: LogRecord): String =
        "${DATE_FORMAT.format(Date(r.millis))}Z ${r.message}$LINE_SEPARATOR${r.thrown?.stackTrace?.joinToString(LINE_SEPARATOR) ?: ""}"

    companion object {
        val LINE_SEPARATOR = System.lineSeparator() ?: "\n"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
}
