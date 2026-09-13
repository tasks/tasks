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
import org.tasks.di.coreModule
import org.tasks.di.platformModule
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.service.Upgrader
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private var started = false

private fun ensureStarted() {
    if (started) return
    started = true
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
