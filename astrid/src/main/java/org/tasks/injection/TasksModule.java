package org.tasks.injection;

import android.content.Context;

import org.tasks.Tasks;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = { Tasks.class })
public class TasksModule {
    private Context context;

    public TasksModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getApplicationContext() {
        return context.getApplicationContext();
    }
}
