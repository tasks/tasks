package org.tasks.injection;

import dagger.ObjectGraph;

public interface Injector {

    public void inject(Object caller);

    public ObjectGraph getObjectGraph();

}
