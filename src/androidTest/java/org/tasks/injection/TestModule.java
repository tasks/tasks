package org.tasks.injection;

import android.content.Context;

import com.todoroo.astrid.dao.Database;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class TestModule {
    private Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    public Database getDatabase() {
        return new Database(context) {
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
