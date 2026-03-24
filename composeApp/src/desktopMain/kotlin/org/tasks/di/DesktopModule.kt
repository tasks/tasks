package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.caldav.FileStorage
import org.tasks.caldav.VtodoCache
import org.tasks.auth.DesktopOAuthFlow
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
import java.io.File

actual fun platformModule(): Module = module {
    singleOf(::TasksServerEnvironment)
    single { PlatformConfiguration() }
    factory<OkHttpClientFactory> { DefaultOkHttpClientFactory() }
    factory { DesktopOAuthFlow(serverEnvironment = get()) }
    factoryOf(::DesktopSignInHandler) bind SignInHandler::class
    single {
        val configDir = File(System.getProperty("user.home"), ".tasks.org")
        KeyStoreEncryption(DesktopKeyProvider(File(configDir, ".key")))
    }
    single<Database> {
        val dbDir = File(System.getProperty("user.home"), ".tasks.org")
        dbDir.mkdirs()
        val dbFile = File(dbDir, Database.NAME)
        Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single {
        val configDir = File(System.getProperty("user.home"), ".tasks.org")
        configDir.mkdirs()
        val dataStoreFile = File(configDir, dataStoreFileName)
        TasksPreferences(createDataStore { dataStoreFile.absolutePath })
    }
    factory {
        val configDir = File(System.getProperty("user.home"), ".tasks.org")
        FileStorage(configDir.absolutePath)
    }
    factoryOf(::VtodoCache)
}
