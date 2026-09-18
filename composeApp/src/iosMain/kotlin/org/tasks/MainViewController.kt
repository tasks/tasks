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
import org.tasks.sse.SseClient
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState

private var foregroundSyncInstalled = false

internal fun ensureStarted() {
    if (KoinPlatform.getKoinOrNull() == null) {
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
    }
    if (!foregroundSyncInstalled) {
        foregroundSyncInstalled = true
        syncWhenForegrounded()
    }
}

private fun syncWhenForegrounded() {
    val koin = KoinPlatform.getKoin()
    val syncAdapters = koin.get<SyncAdapters>()
    val sseClient = koin.get<SseClient>()
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) {
        sseClient.start()
        syncAdapters.sync(SyncSource.APP_RESUME)
    }
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidEnterBackgroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { sseClient.stop() }
    if (UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive) {
        sseClient.start()
    }
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
