package org.tasks.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.GooglePlayFcmTokenProvider
import org.tasks.fcm.PushTokenManager

val flavorModule = module {
    single { PlatformConfiguration() }
    single<FcmTokenProvider> { GooglePlayFcmTokenProvider() }
    single {
        PushTokenManager(
            tokenProvider = get(),
            caldavDao = get(),
            caldavClientProvider = get(),
            scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}
