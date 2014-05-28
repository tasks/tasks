package org.tasks.injection;

import android.content.Context;

import dagger.ObjectGraph;

public class TestInjector implements Injector {

    ObjectGraph objectGraph;

    public TestInjector(Context context) {
        objectGraph = ObjectGraph.create(new TasksModule(context));
    }

    @Override
    public void inject(Object caller, Object... modules) {
        objectGraph
                .plus(modules)
                .inject(caller);
    }
}
