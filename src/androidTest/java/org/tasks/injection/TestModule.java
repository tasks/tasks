package org.tasks.injection;

import android.content.Context;

import com.todoroo.astrid.dao.Database;

import org.tasks.analytics.Tracker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class TestModule {
    private Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    public Database getDatabase() {
        return new Database(context, mock(Tracker.class)) {
            @Override
            public String getName() {
                return "databasetest";
            }
        };
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }
}
