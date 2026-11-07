package org.tasks.di

import kotlinx.coroutines.flow.map
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.CurrentActivity
import org.tasks.R
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.auth.MicrosoftSignIn
import org.tasks.sync.microsoft.AndroidMicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.MsalClient
import org.tasks.sync.microsoft.setupMsalMicrosoftAccount
import org.tasks.analytics.Reporting
import org.tasks.analytics.logCloudOnboarding
import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.DesktopLinkServiceImpl
import org.tasks.billing.GooglePlayQrScanner
import org.tasks.billing.PlayBilling
import org.tasks.billing.JsonPlayPurchaseStore
import org.tasks.billing.PlayBillingClient
import org.tasks.billing.PlayCheckoutViewModel
import org.tasks.billing.PlayPurchases
import org.tasks.billing.PlayPurchaseVerifier
import org.tasks.billing.PlaySignatureVerifier
import org.tasks.billing.PlayStoreBillingProvider
import org.tasks.billing.PlaySubscriptionProvider
import org.tasks.billing.PurchaseEntitlement
import org.tasks.billing.QrScanner
import org.tasks.billing.StoreBillingProvider
import org.tasks.billing.StoreSubscriptionProvider
import org.tasks.billing.unlockSource
import org.tasks.billing.maybeTriggerCloudOnboarding
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.GooglePlayFcmTokenProvider
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

val flavorModule = module {
    single {
        PlatformConfiguration(
            appStore = AppStore.GOOGLE_PLAY,
            billingProvider = BillingProvider.GOOGLE_PLAY,
            isAndroid = true,
            supportsCaldav = true,
            supportsEteSync = true,
            supportsOpenTasks = true,
            supportsMicrosoft = true,
            supportsDesktopLinking = true,
            supportsNotifications = false,
            supportsNotificationTroubleshooting = false,
            supportsSystemNotificationSettings = false,
        )
    }
    single(createdAtStart = true) {
        CurrentActivity().also { androidApplication().registerActivityLifecycleCallbacks(it) }
    }
    single { MsalClient(androidContext(), R.raw.microsoft_config) }
    single<MicrosoftSignIn> {
        MicrosoftSignIn {
            val activity = get<CurrentActivity>().activity
                ?: throw IllegalStateException("Microsoft sign-in needs a foreground activity")
            setupMsalMicrosoftAccount(
                email = get<MsalClient>().signIn(activity, null),
                caldavDao = get(),
                onAccountAdded = {
                    get<Reporting>().logEvent(
                        AnalyticsEvents.SYNC_ADD_ACCOUNT,
                        AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_MICROSOFT,
                    )
                },
            )
        }
    }
    single<MicrosoftClientProvider> {
        AndroidMicrosoftClientProvider(
            context = androidContext(),
            okHttpClientFactory = get(),
            caldavDao = get(),
            tokens = get<MsalClient>(),
        )
    }
    single<FcmTokenProvider> { GooglePlayFcmTokenProvider() }
    single<QrScanner> { GooglePlayQrScanner(androidContext()) }
    single<DesktopLinkService> {
        DesktopLinkServiceImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            subscriptionProvider = get<StoreSubscriptionProvider>(),
            json = get(),
        )
    }
    single<PlayPurchaseVerifier> { PlaySignatureVerifier() }
    single {
        PlayPurchases(
            store = JsonPlayPurchaseStore(tasksPreferences = get(), json = get()),
            verifier = get(),
        )
    }
    single<PlayBilling> {
        PlayBillingClient(
            context = androidContext(),
            purchases = get(),
            reporting = get(),
            scope = get(),
            onPurchasesAdded = { purchased ->
                maybeTriggerCloudOnboarding(
                    purchased = purchased,
                    caldavDao = get(),
                    tasksPreferences = get(),
                    logStep = { step -> get<Reporting>().logCloudOnboarding(step) },
                )
                get<SyncAdapters>().sync(SyncSource.PURCHASE_COMPLETED)
            },
        )
    }
    single<StoreSubscriptionProvider> {
        PlaySubscriptionProvider(purchases = get(), billing = get())
    }
    single<StoreBillingProvider> { PlayStoreBillingProvider() }
    viewModel {
        PlayCheckoutViewModel(
            billing = get(),
            purchases = get(),
            reporting = get(),
            coveredBy = get<PurchaseEntitlement>().coveredBy.map { it?.unlockSource },
        )
    }
}
