package org.tasks.injection;

import android.content.Context;

import com.todoroo.astrid.dao.Database;

import org.tasks.analytics.Tracker;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissivePermissionChecker;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class TestModule {
    private Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @ApplicationScope
    @Provides
    public Database getDatabase() {
        return new Database(context, mock(Tracker.class)) {
            @Override
            public String getName() {
                return "databasetest";
            }
        };
    }

    @ApplicationScope
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }

    @Provides
    public PermissionChecker getPermissionChecker() {
        return new PermissivePermissionChecker(context);
    }
}
