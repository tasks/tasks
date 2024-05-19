package org.tasks.injection

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.caldav.FileStorage
import org.tasks.data.OpenTaskDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.db.Database
import org.tasks.db.Migrations
import org.tasks.jobs.WorkManager
import org.tasks.jobs.WorkManagerImpl
import org.tasks.location.AndroidLocationManager
import org.tasks.location.LocationManager
import org.tasks.preferences.Preferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class ProductionModule {
    @Provides
    @Singleton
    fun getAppDatabase(
        @ApplicationContext context: Context,
        preferences: Preferences,
        fileStorage: FileStorage,
    ): Database {
        val databaseFile = context.getDatabasePath(Database.NAME)
        val builder = Room.databaseBuilder<Database>(
            context = context,
            name = databaseFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*Migrations.migrations(context, fileStorage))
        if (!BuildConfig.DEBUG || !preferences.getBoolean(R.string.p_crash_main_queries, false)) {
            builder.allowMainThreadQueries()
        }
        return builder.build()
    }

    @Provides
    fun getPreferences(@ApplicationContext context: Context): Preferences = Preferences(context)

    @Provides
    fun locationManager(locationManager: AndroidLocationManager): LocationManager = locationManager

    @Provides
    @Singleton
    fun getWorkManager(
        @ApplicationContext context: Context,
        preferences: Preferences,
        caldavDao: CaldavDao,
        openTaskDao: OpenTaskDao,
    ): WorkManager = WorkManagerImpl(context, preferences, caldavDao, openTaskDao)
}