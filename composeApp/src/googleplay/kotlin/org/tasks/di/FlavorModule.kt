package org.tasks.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.DesktopLinkServiceImpl
import org.tasks.billing.GooglePlayQrScanner
import org.tasks.billing.QrScanner
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.GooglePlayFcmTokenProvider
import org.tasks.fcm.PushTokenManager

val flavorModule = module {
    single {
        PlatformConfiguration(
            supportsCaldav = true,
            supportsEteSync = true,
            supportsOpenTasks = true,
            supportsDesktopLinking = true,
        )
    }
    single<FcmTokenProvider> { GooglePlayFcmTokenProvider() }
    single {
        PushTokenManager(
            tokenProvider = get(),
            caldavDao = get(),
            caldavClientProvider = get(),
            scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
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
