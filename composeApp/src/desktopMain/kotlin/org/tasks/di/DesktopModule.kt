package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import org.tasks.billing.SubscriptionProvider
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.auth.DesktopOAuthFlow
import org.tasks.fcm.FcmTokenProvider
import org.tasks.fcm.PushTokenManager
import org.tasks.http.DefaultOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.auth.DesktopSignInHandler
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.db.Database
import org.tasks.kmp.JvmBuildConfig
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import org.tasks.security.DesktopKeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sse.SseClient
import org.tasks.sse.SseTokenProvider
import java.io.File

private fun dataDir(): File {
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
    factoryOf(::VtodoCache)
    single<SubscriptionProvider> {
        object : SubscriptionProvider {
            override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> = flowOf(null)
            override suspend fun getFormattedPrice(sku: String): String? = null
        }
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
