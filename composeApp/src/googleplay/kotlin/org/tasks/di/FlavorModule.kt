package org.tasks.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.DesktopLinkServiceImpl
import org.tasks.billing.GooglePlayQrScanner
import org.tasks.billing.QrScanner
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.GooglePlayFcmTokenProvider

val flavorModule = module {
    single {
        PlatformConfiguration(
            appStore = AppStore.GOOGLE_PLAY,
            isAndroid = true,
            supportsCaldav = true,
            supportsEteSync = true,
            supportsOpenTasks = true,
            supportsDesktopLinking = true,
            supportsNotifications = false,
            supportsNotificationTroubleshooting = false,
            supportsSystemNotificationSettings = false,
        )
    }
    single<FcmTokenProvider> { GooglePlayFcmTokenProvider() }
    single<QrScanner> { GooglePlayQrScanner(androidContext()) }
    single<DesktopLinkService> {
        DesktopLinkServiceImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            subscriptionProvider = get(),
            json = get(),
        )
    }
}
