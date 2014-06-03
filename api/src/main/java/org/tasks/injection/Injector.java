package org.tasks.injection;

import dagger.ObjectGraph;

public interface Injector {

    void inject(Object caller);

    ObjectGraph getObjectGraph();
}
