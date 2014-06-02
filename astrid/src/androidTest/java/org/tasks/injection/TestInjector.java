package org.tasks.injection;

import android.content.Context;

import dagger.ObjectGraph;

public class TestInjector implements Injector {

    ObjectGraph objectGraph;

    public TestInjector(Context context) {
        objectGraph = ObjectGraph.create(new TestModule(context));
    }

    @Override
    public void inject(Object caller) {
        objectGraph.inject(caller);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }
}
