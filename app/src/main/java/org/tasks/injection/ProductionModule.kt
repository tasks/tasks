package org.tasks.injection

import android.content.Context
import androidx.room.Room
import com.todoroo.astrid.dao.Database
import dagger.Module
import dagger.Provides
import org.tasks.db.Migrations
import org.tasks.preferences.Preferences

@Module(includes = [ApplicationModule::class])
internal class ProductionModule {
    @Provides
    @ApplicationScope
    fun getAppDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, Database.NAME)
                .allowMainThreadQueries() // TODO: remove me
                .addMigrations(*Migrations.MIGRATIONS)
                .build()
    }

    @Provides
    fun getPreferences(@ApplicationContext context: Context): Preferences {
        return Preferences(context)
    }
}