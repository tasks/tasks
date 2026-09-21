package org.tasks.logging

import android.app.Application
import co.touchlab.kermit.Severity
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.backup.TasksJsonExporter
import org.tasks.preferences.DiagnosticInfo
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileLogger @Inject constructor(
    private val context: Application,
    private val diagnosticInfo: Lazy<DiagnosticInfo>,
    private val tasksJsonExporter: Lazy<TasksJsonExporter>,
) : Timber.DebugTree() {
    val fileLogWriter = FileLogWriter(
        File(context.cacheDir, "logs").apply { mkdirs() }
    )

    private val severityMap = mapOf(
        2 to Severity.Verbose,  // Log.VERBOSE
        3 to Severity.Debug,    // Log.DEBUG
        4 to Severity.Info,     // Log.INFO
        5 to Severity.Warn,     // Log.WARN
        6 to Severity.Error,    // Log.ERROR
        7 to Severity.Assert,   // Log.ASSERT
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val severity = severityMap[priority] ?: Severity.Debug
        fileLogWriter.log(severity, message, tag ?: "", t)
    }

    suspend fun getZipFile(): File = withContext(Dispatchers.IO) {
        fileLogWriter.zipLogFiles(
            zipFile = File(context.cacheDir, "logs.zip"),
        ) { zos ->
            try {
                Runtime
                    .getRuntime()
                    .exec(arrayOf("logcat", "-d", "-v", "threadtime", "*:*"))
                    ?.inputStream
                    ?.use { logcat ->
                        zos.putNextEntry(ZipEntry("logcat.txt"))
                        logcat.copyTo(zos)
                        zos.closeEntry()
                    }
            } catch (e: IOException) {
                Timber.e(e, "Failed to save logcat")
            }
            zos.putNextEntry(ZipEntry("device.txt"))
            zos.write(diagnosticInfo.get().debugInfo.toByteArray(Charsets.UTF_8))
            try {
                zos.write("\n".toByteArray(Charsets.UTF_8))
                zos.write(diagnosticInfo.get().getDiagnosticInfo().toByteArray(Charsets.UTF_8))
            } catch (e: Exception) {
                Timber.e(e, "Failed to save diagnostics")
            }
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("settings.json"))
            tasksJsonExporter.get().doSettingsExport(zos)
            zos.closeEntry()
        }
    }
}
