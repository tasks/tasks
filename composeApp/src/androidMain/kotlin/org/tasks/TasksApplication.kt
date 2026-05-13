package org.tasks

import android.app.Application
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.logging.FileLogWriter
import org.tasks.opentasks.OpenTaskContentObserver
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.sync.SyncAdapters
import java.io.File

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val logDir = File(cacheDir, "logs").apply { mkdirs() }
        val logcat = if (TasksBuildConfig.DEBUG) {
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
        org.tasks.caldav.CaldavSynchronizer.registerFactories()
        startKoin {
            androidContext(this@TasksApplication)
            modules(commonModule, platformModule())
        }
        val koin = getKoin()
        runBlocking {
            koin.get<AppPreferences>()
                .recordInstallIfNeeded(koin.get<PlatformConfiguration>().versionCode)
        }
        koin.getOrNull<org.tasks.fcm.PushTokenManager>()
            ?.registerTokenForAllAccounts()
        val observer = OpenTaskContentObserver(
            context = this,
            syncAdapters = koin.get<SyncAdapters>(),
            tasksPreferences = koin.get<TasksPreferences>(),
        )
        OpenTaskContentObserver.registerObserver(this, observer)
    }
}
