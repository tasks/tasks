package org.tasks.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import okio.Path.Companion.toPath
import org.tasks.TasksBuildConfig
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal val fileLogWriter: FileLogWriter by lazy {
    val caches = NSFileManager.defaultManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask).first() as NSURL
    FileLogWriter("${caches.path ?: error("no Caches directory")}/logs".toPath())
}

internal fun setupLogging() {
    Logger.setMinSeverity(if (TasksBuildConfig.DEBUG) Severity.Verbose else Severity.Debug)
    Logger.setLogWriters(
        if (TasksBuildConfig.DEBUG) {
            platformLogWriter()
        } else {
            SeverityFilterLogWriter(platformLogWriter(), Severity.Error)
        },
        fileLogWriter,
    )
    logStartup()
}
