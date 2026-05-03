package org.tasks.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

class FileLogWriter(
    val logDirectory: File,
) : LogWriter() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileHandler = FileHandler(
        "${logDirectory.absolutePath}/log.%g.txt",
        20 * 1024 * 1024,
        10,
    ).apply {
        formatter = object : Formatter() {
            private val dateFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }

            override fun format(record: LogRecord): String = buildString {
                append(dateFormat.format(Date(record.millis)))
                append("Z ")
                append(record.message)
                append(System.lineSeparator())
            }
        }
    }
    private val logger = Logger.getLogger("tasks").apply {
        useParentHandlers = false
        addHandler(fileHandler)
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        scope.launch {
            logger.info("${tag.truncateOrPad()} ${severity.name[0]} $message")
            throwable?.let {
                logger.info(it.stackTraceToString())
            }
        }
    }

    fun flush() {
        fileHandler.flush()
    }

    suspend fun zipLogFiles(
        zipFile: File,
        extras: suspend (ZipOutputStream) -> Unit = {},
    ): File {
        val buffer = ByteArray(1024)
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                extras(zos)
                flush()
                logDirectory
                    .listFiles { _, name -> name?.endsWith(".txt") == true }
                    ?.forEach { file ->
                        FileInputStream(file).use { fis ->
                            zos.putNextEntry(ZipEntry(file.name))
                            var len: Int
                            while (fis.read(buffer).also { len = it } > 0) {
                                zos.write(buffer, 0, len)
                            }
                            zos.closeEntry()
                        }
                    }
            }
        }
        return zipFile
    }

    companion object {
        private const val MAX_LENGTH = 23
        private const val TAG_PART = (MAX_LENGTH - 3) / 2

        private fun String.truncateOrPad(): String = when {
            length == MAX_LENGTH -> this
            length < MAX_LENGTH -> padEnd(MAX_LENGTH, ' ')
            else -> "${substring(0, TAG_PART)}...${substring(length - TAG_PART)}"
        }
    }
}
