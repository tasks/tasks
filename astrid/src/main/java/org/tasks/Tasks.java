package org.tasks;

import android.app.Application;

import org.tasks.injection.Injector;
import org.tasks.injection.TasksModule;

import dagger.ObjectGraph;

public class Tasks extends Application implements Injector {

    Injector injector;

    @Override
    public void onCreate() {
        super.onCreate();

        getInjector();
    }

    @Override
    public void inject(Object caller, Object... modules) {
        getInjector().inject(caller, modules);
    }

    private Injector getInjector() {
        if (injector == null) {
            injector = new Injector() {
                ObjectGraph objectGraph = ObjectGraph.create(new TasksModule());

                @Override
                public void inject(Object caller, Object... modules) {
                    objectGraph
                            .plus(modules)
                            .inject(caller);
                }
            };
        }

        return injector;
    }
}
