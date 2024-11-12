package org.tasks.logging

import android.content.Context
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tasks.logging.LogFormatter.Companion.LINE_SEPARATOR
import timber.log.Timber
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Logger

class FileLogger(
    context: Context,
) : Timber.DebugTree() {
    val logDirectory = File(context.cacheDir, "logs").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileHandler: FileHandler = FileHandler(
        "${logDirectory.absolutePath}/log.%g.txt",
        20 * 1024 * 1024,
        10
    )
    private val logger = Logger.getLogger(context.packageName)

    init {
        fileHandler.formatter = LogFormatter()
        logger.addHandler(fileHandler)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.DEBUG) {
            return
        }
        val threadId = Process.myTid()
        scope.launch {
            t?.let { logger.info(it.stackTrace.joinToString(LINE_SEPARATOR)) }
            logger.info("${Process.myPid()}-${threadId} ${(tag ?: "").truncateOrPad()} ${levels[priority] ?: priority} $message")
        }
    }

    fun flush() {
        fileHandler.flush()
    }

    companion object {
        private const val MAX_LENGTH = 23
        private const val TAG_PART = (MAX_LENGTH - 3) / 2

        private val levels = mapOf(
            Log.VERBOSE to "V",
            Log.DEBUG to "D",
            Log.INFO to "I",
            Log.WARN to "W",
            Log.ERROR to "E",
        )

        private fun String.truncateOrPad(): String =
            when {
                length == MAX_LENGTH -> this
                length < MAX_LENGTH -> padEnd(MAX_LENGTH, ' ')
                else -> "${substring(0, TAG_PART)}...${substring(length - TAG_PART)}"
            }
    }
}
