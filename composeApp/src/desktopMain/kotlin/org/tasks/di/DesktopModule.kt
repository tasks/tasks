package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import org.tasks.PlatformConfiguration
import org.tasks.auth.DesktopOAuthFlow
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
    single { TasksServerEnvironment(get()) }
    single { PlatformConfiguration() }
    single { DesktopOAuthFlow(caldavUrl = get<TasksServerEnvironment>().caldavUrl) }
    single<SignInHandler> { DesktopSignInHandler(get(), get(), get(), get()) }
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
}
