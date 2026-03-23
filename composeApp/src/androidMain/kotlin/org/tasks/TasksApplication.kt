package org.tasks

import android.app.Application
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.kmp.IS_DEBUG
import org.tasks.logging.FileLogWriter
import java.io.File

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val logDir = File(cacheDir, "logs").apply { mkdirs() }
        val logcat = if (IS_DEBUG) {
            platformLogWriter()
        } else {
            object : LogWriter() {
                private val delegate = platformLogWriter()
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    if (severity >= Severity.Error) {
                        delegate.log(severity, message, tag, throwable)
                    }
                }
            }
        }
        Logger.setLogWriters(logcat, FileLogWriter(logDir))
        startKoin {
            androidContext(this@TasksApplication)
            modules(commonModule, platformModule())
        }
    }
}
