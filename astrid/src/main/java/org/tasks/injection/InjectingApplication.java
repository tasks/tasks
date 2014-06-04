package org.tasks.injection;

import android.app.Application;

import dagger.ObjectGraph;

public class InjectingApplication extends Application implements Injector {

    private final ObjectGraph objectGraph = ObjectGraph.create(new TasksModule());

    @Override
    public void onCreate() {
        super.onCreate();

        inject(this);
    }

    @Override
    public void inject(Object caller) {
        objectGraph.inject(this);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }
}
