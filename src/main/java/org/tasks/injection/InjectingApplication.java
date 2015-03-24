package org.tasks.injection;

import android.app.Application;

import dagger.ObjectGraph;

public class InjectingApplication extends Application implements Injector {

    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        objectGraph = Dagger.getObjectGraph(this);

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
