package org.tasks.injection

import android.content.Context
import androidx.room.Room
import com.todoroo.astrid.dao.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mockito.Mockito.mock
import org.tasks.TestUtilities
import org.tasks.jobs.WorkManager
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissivePermissionChecker
import org.tasks.preferences.Preferences
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
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
    fun getWorkManager(): WorkManager = mock(WorkManager::class.java)
}