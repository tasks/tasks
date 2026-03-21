package org.tasks.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import org.tasks.data.db.Database
import org.tasks.auth.TasksServerEnvironment
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import java.io.File

actual fun platformModule(): Module = module {
    single { TasksServerEnvironment(get()) }
    single<Database> {
        val dbDir = File(System.getProperty("user.home"), ".tasks")
        dbDir.mkdirs()
        val dbFile = File(dbDir, Database.NAME)
        Room.databaseBuilder<Database>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single {
        val configDir = File(System.getProperty("user.home"), ".tasks")
        configDir.mkdirs()
        val dataStoreFile = File(configDir, dataStoreFileName)
        TasksPreferences(createDataStore { dataStoreFile.absolutePath })
    }
}
