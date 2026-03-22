package org.tasks.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.db.Database
import org.tasks.kmp.createDataStore
import org.tasks.preferences.TasksPreferences

actual fun platformModule(): Module = module {
    includes(flavorModule)
    single { TasksServerEnvironment(get()) }
    single<Database> {
        val context = androidContext()
        val dbFile = context.getDatabasePath(Database.NAME)
        Room.databaseBuilder<Database>(
            context = context,
            name = dbFile.absolutePath,
        ).build()
    }
    single {
        val context = androidContext()
        TasksPreferences(createDataStore(context))
    }
}
