package org.tasks.injection;

import android.content.Context;
import androidx.room.Room;
import com.todoroo.astrid.dao.Database;
import dagger.Module;
import dagger.Provides;
import org.tasks.db.Migrations;
import org.tasks.preferences.Preferences;

@Module(includes = ApplicationModule.class)
public class ProductionModule {
  @Provides
  @ApplicationScope
  public Database getAppDatabase(@ForApplication Context context) {
    return Room.databaseBuilder(context, Database.class, Database.NAME)
        .allowMainThreadQueries() // TODO: remove me
        .addMigrations(Migrations.MIGRATIONS)
        .build();
  }

  @Provides
  public Preferences getPreferences(@ForApplication Context context) {
    return new Preferences(context);
  }
}
