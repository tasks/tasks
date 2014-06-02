package org.tasks;

import android.app.Application;

import com.todoroo.andlib.service.ContextManager;

import org.tasks.injection.Injector;

import dagger.ObjectGraph;

public class Tasks extends Application implements Injector {

    ObjectGraph objectGraph = ObjectGraph.create();

    @Override
    public void onCreate() {
        super.onCreate();

        ContextManager.setContext(this);
    }

    @Override
    public void inject(Object caller) {
        getObjectGraph().inject(caller);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }
}
