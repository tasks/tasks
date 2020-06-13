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
}