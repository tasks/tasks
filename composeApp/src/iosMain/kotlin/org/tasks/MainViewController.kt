package org.tasks

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.tasks.di.coreModule
import org.tasks.di.platformModule
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.service.Upgrader

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
}
