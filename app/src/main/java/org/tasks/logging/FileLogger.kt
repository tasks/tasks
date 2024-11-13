package org.tasks.logging

import android.annotation.SuppressLint
import android.app.Application
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.logging.LogFormatter.Companion.LINE_SEPARATOR
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileLogger @Inject constructor(
    private val context: Application,
) : Timber.DebugTree() {
    private val logDirectory = File(context.cacheDir, "logs").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileHandler: FileHandler = FileHandler(
        "${logDirectory.absolutePath}/log.%g.txt",
        20 * 1024 * 1024,
        10
    )
    @SuppressLint("LogNotTimber")
    private val logger = Logger.getLogger(context.packageName).apply {
        try {
            useParentHandlers = false
        } catch (e: SecurityException) {
            Log.e("FileLogger", "Failed to disable parent handlers", e)
        }
    }

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

    suspend fun getZipFile(): File = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "logs.zip")
        val buffer = ByteArray(1024)
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                try {
                    Runtime
                        .getRuntime()
                        .exec(arrayOf("logcat", "-d", "-v", "threadtime", "*:*"))
                        ?.inputStream
                        ?.use { logcat ->
                            zos.putNextEntry(ZipEntry("logcat.txt"))
                            var len: Int
                            while (logcat.read(buffer).also { len = it } > 0) {
                                zos.write(buffer, 0, len)
                            }
                            zos.closeEntry()
                        }
                } catch (e: IOException) {
                    Timber.e(e, "Failed to save logcat")
                }
                fileHandler.flush()
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
        zipFile
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
