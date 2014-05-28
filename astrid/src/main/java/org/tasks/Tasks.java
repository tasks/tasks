package org.tasks;

import android.app.Application;

import com.todoroo.andlib.service.ContextManager;

import org.tasks.injection.Injector;

import dagger.ObjectGraph;

import static org.tasks.injection.TasksModule.newTasksModule;

public class Tasks extends Application implements Injector {

    Injector injector;

    @Override
    public void onCreate() {
        super.onCreate();

        ContextManager.setContext(this);

        getInjector();
    }

    @Override
    public void inject(Object caller, Object... modules) {
        getInjector().inject(caller, modules);
    }

    private Injector getInjector() {
        if (injector == null) {
            injector = new Injector() {
                ObjectGraph objectGraph = ObjectGraph.create(newTasksModule(Tasks.this));

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
