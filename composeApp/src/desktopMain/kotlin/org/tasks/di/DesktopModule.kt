package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.auth.DesktopOAuthFlow
import org.tasks.auth.DesktopSignInHandler
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopEntitlement
import org.tasks.billing.DesktopLinkClient
import org.tasks.billing.DesktopLinkClientImpl
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorClientImpl
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.data.db.Database
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.opentasks.OpenTasksSyncer
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.PushTokenManager
import org.tasks.http.DefaultOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.kmp.JvmBuildConfig
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import org.tasks.security.DesktopKeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sse.SseClient
import org.tasks.sse.SseTokenProvider
import java.io.File

fun dataDir(): File {
    val override = System.getProperty("tasks.dataDir")?.takeIf { it.isNotBlank() }
    val home = System.getProperty("user.home")
    val file = override?.let { File(it) }
        ?: File(home, if (JvmBuildConfig.DEBUG) ".tasks.org.debug" else ".tasks.org")
    return file.also { it.mkdirs() }
}

actual fun platformModule(): Module = module {
    singleOf(::TasksServerEnvironment)
    single {
        PlatformConfiguration(
            versionCode = JvmBuildConfig.VERSION_CODE,
            billingProvider = BillingProvider.PADDLE,
            supportsCaldav = true,
            supportsEteSync = true,
        )
    }
    single<Reporting> {
        PostHogReporting(
            apiKey = JvmBuildConfig.POSTHOG_KEY,
            dataDir = dataDir(),
        )
    }
    factory<OkHttpClientFactory> { DefaultOkHttpClientFactory() }
    factory { DesktopOAuthFlow(serverEnvironment = get()) }
    factoryOf(::DesktopSignInHandler) bind SignInHandler::class
    single {
        KeyStoreEncryption(
            DesktopKeyProvider(
                serviceName = "Tasks.org",
                accountName = "encryption-key",
                fallbackKeyFile = File(dataDir(), ".key"),
            )
        )
    }
    single<Database> {
        val dbFile = File(dataDir(), Database.NAME)
        Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single {
        val dataStoreFile = File(dataDir(), dataStoreFileName)
        TasksPreferences(createDataStore { dataStoreFile.absolutePath })
    }
    factory {
        FileStorage(dataDir().absolutePath)
    }
    factory {
        EtebaseClientProvider(
            filesDir = dataDir().absolutePath,
            encryption = get(),
            caldavDao = get(),
            httpClientFactory = get(),
        )
    }
    factory<OpenTasksSyncer> {
        val caldavDao = get<org.tasks.data.dao.CaldavDao>()
        val refreshBroadcaster = get<org.tasks.broadcast.RefreshBroadcaster>()
        object : OpenTasksSyncer {
            override suspend fun sync(hasPro: Boolean) {
                caldavDao.getAccounts(org.tasks.data.entity.CaldavAccount.TYPE_OPENTASKS).forEach { account ->
                    account.error = "OpenTasks sync is not supported on desktop"
                    caldavDao.update(account)
                    refreshBroadcaster.broadcastRefresh()
                }
            }
        }
    }
    factoryOf(::VtodoCache)
    single {
        DesktopEntitlement(
            dataDir = dataDir(),
            httpClientFactory = get(),
            serverEnvironment = get(),
            scope = get(),
            json = get(),
            encryption = get(),
        ).also { it.initialize() }
    }
    single<SubscriptionProvider> {
        val entitlement: DesktopEntitlement = get()
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
                kotlinx.coroutines.flow.combine(entitlement.hasPro, entitlement.sku) { hasPro, sku ->
                    if (hasPro) {
                        SubscriptionProvider.SubscriptionInfo(
                            sku = sku ?: "desktop_play",
                            isMonthly = sku?.startsWith("monthly") == true,
                            isTasksSubscription = false,
                        )
                    } else {
                        null
                    }
                }
            override suspend fun getFormattedPrice(sku: String): String? =
                entitlement.formattedPrice.first()
        }
    }
    single<DesktopLinkClient> {
        DesktopLinkClientImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            desktopEntitlement = get(),
            json = get(),
        )
    }
    single<GitHubSponsorClient> {
        GitHubSponsorClientImpl(
            httpClientFactory = get(),
            serverEnvironment = get(),
            desktopEntitlement = get(),
            json = get(),
        )
    }
    single { SseTokenProvider() } bind FcmTokenProvider::class
    single {
        PushTokenManager(
            tokenProvider = get(),
            caldavDao = get(),
            caldavClientProvider = get(),
            scope = get(),
        )
    }
    single {
        SseClient(
            scope = get(),
            backgroundWork = get(),
            caldavDao = get(),
            encryption = get(),
            environment = get(),
            httpClientFactory = get(),
            tokenProvider = get(),
        )
    }
}
