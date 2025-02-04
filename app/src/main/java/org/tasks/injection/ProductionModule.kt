package org.tasks.injection

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.todoroo.andlib.utility.AndroidUtilities.atLeastR
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
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
import timber.log.Timber
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
        val builder = Room
            .databaseBuilder<Database>(
                context = context,
                name = databaseFile.absolutePath
            )
            .addMigrations(*Migrations.migrations(context, fileStorage))
            .setDriver()
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

fun <T : RoomDatabase> RoomDatabase.Builder<T>.setDriver() =
    if (atLeastR()) {
        if (BuildConfig.DEBUG) {
            setQueryCallback(
                queryCallback = { sql, args -> Timber.tag("SQL").d("[sql=${sql.replace(Regex("\\s+"), " ").trim()}] [args=$args]") },
                executor = { it.run() },
            )
        } else {
            this
        }
    } else {
        // need bundled sqlite for window functions
        this
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(connection: SQLiteConnection) {
                    super.onOpen(connection)

                    connection.execSQL("PRAGMA busy_timeout = 60000")
                }
            })
    }
