package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.auth.DesktopOAuthFlow
import org.tasks.fcm.FcmTokenProvider
import org.tasks.http.DefaultOkHttpClientFactory
import org.tasks.http.OkHttpClientFactory
import org.tasks.auth.DesktopSignInHandler
import org.tasks.auth.SignInHandler
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.db.Database
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import org.tasks.security.DesktopKeyProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.sse.SseClient
import org.tasks.sse.SseTokenProvider
import java.io.File
import java.util.Properties

private fun dataDir(): File {
    val path = System.getProperty("tasks.dataDir")
        ?: File(System.getProperty("user.home"), ".tasks.org").absolutePath
    return File(path).also { it.mkdirs() }
}

private val config: Properties by lazy {
    Properties().apply {
        DesktopModule::class.java.getResourceAsStream("/tasks.properties")?.use { load(it) }
    }
}

private object DesktopModule

actual fun platformModule(): Module = module {
    singleOf(::TasksServerEnvironment)
    single { PlatformConfiguration() }
    single<Reporting> { PostHogReporting(config.getProperty("posthog.key", "")) }
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
    single { SseTokenProvider() } bind FcmTokenProvider::class
    single {
        SseClient(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            backgroundWork = get(),
            caldavDao = get(),
            encryption = get(),
            environment = get(),
            httpClientFactory = get(),
            tokenProvider = get(),
        ).also { it.start() }
    }
}
