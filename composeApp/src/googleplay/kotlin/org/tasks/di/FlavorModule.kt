package org.tasks.di

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.tasks.AppStore
import org.tasks.analytics.Reporting
import org.tasks.analytics.logCloudOnboarding
import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.DesktopLinkServiceImpl
import org.tasks.billing.EntitlementProvider
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
import org.tasks.billing.UnlockSource
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
    single<PlayPurchaseVerifier> { PlaySignatureVerifier() }
    single {
        PlayPurchases(
            store = JsonPlayPurchaseStore(tasksPreferences = get(), json = get()),
            verifier = get(),
        )
    }
    single<PlayBilling> {
        val scope = get<CoroutineScope>()
        PlayBillingClient(
            context = androidContext(),
            purchases = get(),
            reporting = get(),
            scope = scope,
            onPurchasesUpdated = { purchased ->
                maybeTriggerCloudOnboarding(
                    purchased = purchased,
                    caldavDao = get(),
                    tasksPreferences = get(),
                    logStep = { step -> get<Reporting>().logCloudOnboarding(step) },
                )
                scope.launch { get<SyncAdapters>().sync(SyncSource.PURCHASE_COMPLETED) }
            },
        )
    }
    single<StoreSubscriptionProvider> {
        val purchases: PlayPurchases = get()
        val billing: PlayBilling = get()
        get<CoroutineScope>().launch {
            purchases.load()
            try {
                billing.queryPurchases()
            } catch (e: Exception) {
                Logger.w(e) { "Failed to refresh Play purchases" }
            }
        }
        PlaySubscriptionProvider(purchases = purchases, billing = billing)
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

private val EntitlementProvider.unlockSource: UnlockSource?
    get() = when (this) {
        EntitlementProvider.GITHUB_SPONSOR -> UnlockSource.GITHUB
        EntitlementProvider.STRIPE -> UnlockSource.STRIPE
        EntitlementProvider.PLAY -> null
    }
