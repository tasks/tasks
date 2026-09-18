package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.utf8Size
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Instant

class FileLogWriter(
    val logDirectory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val fileSizeLimit: Long = FILE_SIZE_LIMIT,
) : LogWriter() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val files = List(FILE_COUNT) { logDirectory / "log.$it.txt" }
    private var written = 0L
    private var sink: BufferedSink = rotate()

    @Volatile
    private var shuttingDown = false

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val entry = formatEntry(Clock.System.now(), severity, tag, message, throwable)
        val write = scope.launch { write(entry) }
        if (shuttingDown) {
            runBlocking { write.join() }
        }
    }

    suspend fun flush() {
        scope.launch { }.join()
    }

    fun beginShutdown() {
        shuttingDown = true
        runBlocking { withTimeoutOrNull(DRAIN_TIMEOUT_MS) { flush() } }
    }

    fun logFiles(): List<Path> = files.filter { fileSystem.exists(it) }

    private fun write(entry: String) {
        sink.writeUtf8(entry)
        sink.flush()
        written += entry.utf8Size()
        if (written >= fileSizeLimit) {
            sink.close()
            sink = rotate()
        }
    }

    private fun rotate(): BufferedSink {
        fileSystem.createDirectories(logDirectory)
        for (i in FILE_COUNT - 2 downTo 0) {
            if (fileSystem.exists(files[i])) {
                fileSystem.delete(files[i + 1])
                fileSystem.atomicMove(files[i], files[i + 1])
            }
        }
        written = 0
        return fileSystem.sink(files[0]).buffer()
    }

    companion object {
        private const val FILE_SIZE_LIMIT = 20L * 1024 * 1024
        private const val FILE_COUNT = 10
        private const val DRAIN_TIMEOUT_MS = 2_000L

        private const val MAX_LENGTH = 23
        private const val TAG_PART = (MAX_LENGTH - 3) / 2

        private val TIMESTAMP = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char('T')
            hour()
            char(':')
            minute()
            char(':')
            second()
            char('.')
            secondFraction(3)
            char('Z')
        }

        internal fun formatEntry(
            timestamp: Instant,
            severity: Severity,
            tag: String,
            message: String,
            throwable: Throwable?,
        ): String = buildString {
            append(TIMESTAMP.format(timestamp.toLocalDateTime(TimeZone.UTC)))
            append(' ')
            append(tag.truncateOrPad())
            append(' ')
            append(severity.name[0])
            append(' ')
            append(message)
            append('\n')
            throwable?.let {
                append(it.stackTraceToString())
                append('\n')
            }
        }

        private fun String.truncateOrPad(): String = when {
            length == MAX_LENGTH -> this
            length < MAX_LENGTH -> padEnd(MAX_LENGTH, ' ')
            else -> "${substring(0, TAG_PART)}...${substring(length - TAG_PART)}"
        }
    }
}
