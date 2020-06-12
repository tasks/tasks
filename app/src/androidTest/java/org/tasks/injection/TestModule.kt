package org.tasks.injection

import android.content.Context
import androidx.room.Room
import com.todoroo.astrid.dao.Database
import dagger.Module
import dagger.Provides
import org.tasks.TestUtilities
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissivePermissionChecker
import org.tasks.preferences.Preferences

@Module(includes = [ApplicationModule::class])
class TestModule {
    @Provides
    @ApplicationScope
    fun getDatabase(context: Application): Database {
        return Room.inMemoryDatabaseBuilder(context, Database::class.java)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    fun getPermissionChecker(context: Application): PermissionChecker {
        return PermissivePermissionChecker(context)
    }

    @Provides
    fun getPreferences(context: Application): Preferences {
        return TestUtilities.newPreferences(context)
    }
}