package org.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.tasks.auth.TasksServerEnvironment
import org.tasks.caldav.CaldavClient
import org.tasks.di.coreModule
import org.tasks.di.platformModule
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.service.Upgrader
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification

private var started = false

internal fun ensureStarted() {
    if (started) return
    started = true
    CaldavClient.registerFactories()
    startKoin {
        modules(coreModule, platformModule())
    }
    val koin = KoinPlatform.getKoin()
    runBlocking {
        val versionCode = koin.get<PlatformConfiguration>().versionCode
        koin.get<AppPreferences>().recordInstallIfNeeded(versionCode)
        koin.get<Upgrader>().upgrade(versionCode)
    }
    syncWhenForegrounded()
}

private fun syncWhenForegrounded() {
    val syncAdapters = KoinPlatform.getKoin().get<SyncAdapters>()
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { syncAdapters.sync(SyncSource.APP_RESUME) }
}

fun MainViewController() = ComposeUIViewController {
    ensureStarted()
    val serverEnv = koinInject<TasksServerEnvironment>()
    val scope = rememberCoroutineScope()
    var currentEnv by remember { mutableStateOf(serverEnv.currentEnvironment) }
    App(
        openUrl = { url ->
            NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
        },
        environments = serverEnv.environments,
        currentEnvironment = currentEnv,
        onSelectEnvironment = { env ->
            scope.launch {
                serverEnv.setEnvironment(env)
                currentEnv = env
            }
        },
    )
}
