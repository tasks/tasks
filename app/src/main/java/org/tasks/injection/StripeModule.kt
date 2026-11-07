package org.tasks.injection

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.tasks.analytics.Firebase
import org.tasks.analytics.logCloudOnboarding
import org.tasks.auth.TasksServerEnvironment
import org.tasks.auth.PurchaseClaim
import org.tasks.billing.CompositeSubscriptionProvider
import org.tasks.billing.EntitlementSubscriptionProvider
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorVerifier
import org.tasks.billing.LoopbackGitHubSponsorClient
import org.tasks.billing.PurchaseEntitlement
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeClient
import org.tasks.billing.StripeClientImpl
import org.tasks.billing.StripePurchases
import org.tasks.billing.StoreSubscriptionProvider
import org.tasks.billing.SubscriptionProvider
import org.tasks.billing.debugProFlow
import org.tasks.billing.tasksSessionAuthorization
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.extensions.bringTasksToForeground
import org.tasks.http.KtorClientFactory
import org.tasks.preferences.InstallId
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StripeModule {
    @Provides
    @Singleton
    fun providesStripeClient(
        httpClientFactory: KtorClientFactory,
        environment: TasksServerEnvironment,
        json: Json,
        caldavDao: CaldavDao,
        encryption: KeyStoreEncryption,
        installId: InstallId,
    ): StripeClient = StripeClientImpl(
        httpClientFactory = httpClientFactory,
        caldavUrl = { environment.caldavUrl },
        json = json,
        authorization = { tasksSessionAuthorization(caldavDao, encryption) },
        installId = { installId.get() },
    )

    @Provides
    @Singleton
    fun providesPurchaseEntitlement(
        @ApplicationContext context: Context,
        encryption: KeyStoreEncryption,
        httpClientFactory: KtorClientFactory,
        environment: TasksServerEnvironment,
        @ApplicationScope scope: CoroutineScope,
        json: Json,
        syncAdapters: Lazy<SyncAdapters>,
    ): PurchaseEntitlement = PurchaseEntitlement(
        dataDir = context.filesDir.absolutePath,
        encryption = encryption,
        httpClientFactory = httpClientFactory,
        serverEnvironment = environment,
        scope = scope,
        json = json,
        syncAdapters = { syncAdapters.get() },
    ).also {
        runBlocking { it.restore() }
        it.initialize()
    }

    @Provides
    @Singleton
    fun provideSubscriptionProvider(
        store: StoreSubscriptionProvider?,
        entitlement: PurchaseEntitlement,
        tasksPreferences: TasksPreferences,
    ): SubscriptionProvider {
        val entitlements = EntitlementSubscriptionProvider(
            entitlement = entitlement,
            debugPro = tasksPreferences.debugProFlow,
            verifyBeforeSync = true,
        )
        return when (store) {
            null -> entitlements
            else -> CompositeSubscriptionProvider(listOf(store, entitlements))
        }
    }

    @Provides
    @Singleton
    fun providesStripePurchases(
        client: StripeClient,
        entitlement: PurchaseEntitlement,
        caldavDao: CaldavDao,
        clientProvider: CaldavClientProvider,
        accountData: TasksAccountDataRepository,
        tasksPreferences: TasksPreferences,
        firebase: Firebase,
        @ApplicationScope scope: CoroutineScope,
    ): StripePurchases = StripePurchases(
        client = client,
        entitlement = entitlement,
        caldavDao = caldavDao,
        clientFactory = clientProvider,
        accountData = accountData,
        tasksPreferences = tasksPreferences,
        logOnboarding = { step -> firebase.logCloudOnboarding(step) },
    ).also { it.claimOnSignIn(scope) }

    @Provides
    @Singleton
    fun providesGitHubSponsorVerifier(
        httpClientFactory: KtorClientFactory,
        environment: TasksServerEnvironment,
        entitlement: PurchaseEntitlement,
        json: Json,
        installId: InstallId,
    ): GitHubSponsorVerifier = GitHubSponsorVerifier(
        httpClientFactory = httpClientFactory,
        serverEnvironment = environment,
        purchaseEntitlement = entitlement,
        json = json,
        installId = { installId.get() },
    )

    @Provides
    @Singleton
    fun providesGitHubSponsorClient(
        @ApplicationContext context: Context,
        verifier: Lazy<GitHubSponsorVerifier>,
    ): GitHubSponsorClient = LoopbackGitHubSponsorClient(
        verify = { code, redirectUri -> verifier.get().verify(code, redirectUri) },
        openUrl = { url ->
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        onCallback = { context.bringTasksToForeground() },
    )

    @Provides
    fun providesStripeCheckouts(purchases: StripePurchases): StripeCheckouts = purchases

    @Provides
    fun providesPurchaseClaim(purchases: StripePurchases): PurchaseClaim = purchases
}
