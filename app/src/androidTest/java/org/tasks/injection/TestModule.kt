package org.tasks.injection

import android.content.Context
import androidx.room.Room
import org.tasks.data.db.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mockito.Mockito.mock
import org.tasks.TestUtilities
import org.tasks.jobs.WorkManager
import org.tasks.location.LocationManager
import org.tasks.location.MockLocationManager
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissivePermissionChecker
import org.tasks.preferences.Preferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TestModule {
    @Provides
    @Singleton
    fun getDatabase(@ApplicationContext context: Context): Database {
        return Room.inMemoryDatabaseBuilder(context, Database::class.java)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    fun getPermissionChecker(@ApplicationContext context: Context): PermissionChecker {
        return PermissivePermissionChecker(context)
    }

    @Provides
    fun getPreferences(@ApplicationContext context: Context): Preferences {
        return TestUtilities.newPreferences(context)
    }

    @Provides
    @Singleton
    fun getMockLocationManager(): MockLocationManager = MockLocationManager()

    @Provides
    fun getLocationManager(locationManager: MockLocationManager): LocationManager = locationManager

    @Provides
    fun getWorkManager(): WorkManager = mock(WorkManager::class.java)
}