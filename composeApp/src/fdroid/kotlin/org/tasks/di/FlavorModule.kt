package org.tasks.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopLinkService
import org.tasks.analytics.Reporting
import org.tasks.auth.MicrosoftSignIn
import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.TasksOAuthSignIn
import org.tasks.billing.QrScanner
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.AndroidMicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.asMicrosoftTokenSource

val flavorModule = module {
    single {
        PlatformConfiguration(
            appStore = AppStore.FDROID,
            billingProvider = BillingProvider.STRIPE,
            isAndroid = true,
            isLibre = true,
            supportsCaldav = true,
            supportsEteSync = true,
            supportsOpenTasks = true,
            supportsMicrosoft = true,
            supportsNotifications = false,
            supportsNotificationTroubleshooting = false,
            supportsSystemNotificationSettings = false,
        )
    }
    single<QrScanner> { object : QrScanner { override suspend fun scan(): String? = null } }
    single<DesktopLinkService> { object : DesktopLinkService { override suspend fun confirmLink(code: String) = false } }
    single<MicrosoftSignIn> {
        MicrosoftSignIn { loginHint ->
            get<TasksOAuthSignIn>().signInToMicrosoft(openUrl = {}, analytics = get<Reporting>(), loginHint = loginHint)
        }
    }
    single<MicrosoftClientProvider> {
        AndroidMicrosoftClientProvider(
            context = androidContext(),
            okHttpClientFactory = get(),
            caldavDao = get(),
            tokens = OAuthTokenStore(get<KeyStoreEncryption>(), get(), get()).asMicrosoftTokenSource(),
        )
    }
}
