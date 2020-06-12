package org.tasks.injection

import android.app.Application
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
    fun getAppDatabase(context: Application): Database {
        return Room.databaseBuilder(context, Database::class.java, Database.NAME)
                .allowMainThreadQueries() // TODO: remove me
                .addMigrations(*Migrations.MIGRATIONS)
                .build()
    }

    @Provides
    fun getPreferences(context: Application): Preferences {
        return Preferences(context)
    }
}