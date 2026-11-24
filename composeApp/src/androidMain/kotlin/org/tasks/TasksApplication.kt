package org.tasks

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.jobs.BackgroundWork
import org.tasks.logging.FileLogWriter
import org.tasks.logging.SeverityFilterLogWriter
import org.tasks.logging.installKermitJulHandler
import org.tasks.logging.logStartup
import org.tasks.opentasks.OpenTaskContentObserver
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.service.UpgradeState
import org.tasks.service.Upgrader
import org.tasks.sync.SyncAdapters
import java.io.File

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val logDir = File(cacheDir, "logs").apply { mkdirs() }
        val logcat = if (TasksBuildConfig.DEBUG) {
            platformLogWriter()
        } else {
            SeverityFilterLogWriter(platformLogWriter(), Severity.Error)
        }
        Logger.setMinSeverity(if (TasksBuildConfig.DEBUG) Severity.Verbose else Severity.Debug)
        Logger.setLogWriters(logcat, FileLogWriter(logDir))
        installKermitJulHandler()
        logStartup()
        org.tasks.caldav.CaldavClient.registerFactories()
        startKoin {
            androidContext(this@TasksApplication)
            modules(commonModule, platformModule())
        }
        val koin = getKoin()
        val versionCode = koin.get<PlatformConfiguration>().versionCode
        runBlocking {
            koin.get<AppPreferences>().recordInstallIfNeeded(versionCode)
        }
        val upgradeState = koin.get<UpgradeState>()
        val scope = koin.get<CoroutineScope>()
        upgradeState.launch(scope) {
            koin.get<Upgrader>().upgrade(versionCode)
        }
        scope.launch(Dispatchers.Main) {
            upgradeState.awaitReady("TasksApplication startup")
            koin.getOrNull<org.tasks.fcm.PushTokenManager>()
                ?.registerTokenForAllAccounts()
            koin.get<BackgroundWork>().updatePurchases()
            val observer = OpenTaskContentObserver(
                context = this@TasksApplication,
                syncAdapters = koin.get<SyncAdapters>(),
                tasksPreferences = koin.get<TasksPreferences>(),
            )
            OpenTaskContentObserver.registerObserver(this@TasksApplication, observer)
        }
    }
}
