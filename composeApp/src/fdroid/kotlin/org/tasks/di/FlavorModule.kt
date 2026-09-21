package org.tasks.di

import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.QrScanner

val flavorModule = module {
    single {
        PlatformConfiguration(
            appStore = AppStore.FDROID,
            isAndroid = true,
            isLibre = true,
            supportsCaldav = true,
            supportsEteSync = true,
            supportsOpenTasks = true,
            supportsNotifications = false,
            supportsNotificationTroubleshooting = false,
            supportsSystemNotificationSettings = false,
        )
    }
    single<QrScanner> { object : QrScanner { override suspend fun scan(): String? = null } }
    single<DesktopLinkService> { object : DesktopLinkService { override suspend fun confirmLink(code: String) = false } }
}
