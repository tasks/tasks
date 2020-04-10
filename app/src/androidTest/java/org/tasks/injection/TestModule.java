package org.tasks.injection;

import static org.tasks.TestUtilities.newPreferences;

import android.content.Context;
import androidx.room.Room;
import com.todoroo.astrid.dao.Database;
import dagger.Module;
import dagger.Provides;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissivePermissionChecker;
import org.tasks.preferences.Preferences;

@Module(includes = ApplicationModule.class)
class TestModule {

  @Provides
  @ApplicationScope
  Database getDatabase(@ForApplication Context context) {
    return Room.inMemoryDatabaseBuilder(context, Database.class)
        .fallbackToDestructiveMigration()
        .build();
  }

  @Provides
  PermissionChecker getPermissionChecker(@ForApplication Context context) {
    return new PermissivePermissionChecker(context);
  }

  @Provides
  Preferences getPreferences(@ForApplication Context context) {
    return newPreferences(context);
  }
}
