package org.tasks.injection;

import android.content.Context;
import androidx.room.Room;
import com.todoroo.astrid.dao.Database;
import dagger.Module;
import dagger.Provides;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissivePermissionChecker;
import org.tasks.preferences.Preferences;

@Module(includes = ApplicationModule.class)
public class TestModule {

  public static Preferences newPreferences(Context context) {
    return new Preferences(context, "test_preferences");
  }

  @Provides
  @ApplicationScope
  public Database getDatabase(@ForApplication Context context) {
    return Room.inMemoryDatabaseBuilder(context, Database.class)
        .fallbackToDestructiveMigration()
        .build();
  }

  @Provides
  public PermissionChecker getPermissionChecker(@ForApplication Context context) {
    return new PermissivePermissionChecker(context);
  }

  @Provides
  public Preferences getPreferences(@ForApplication Context context) {
    return newPreferences(context);
  }
}
