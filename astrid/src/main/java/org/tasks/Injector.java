package org.tasks;

import dagger.ObjectGraph;

public class Injector {

    ObjectGraph objectGraph;

    public Injector() {
        objectGraph = ObjectGraph.create(new TasksModule());
    }

    public void inject(Object caller) {
        objectGraph.inject(caller);
    }
}
