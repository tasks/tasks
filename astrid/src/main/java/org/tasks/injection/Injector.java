package org.tasks.injection;

public interface Injector {

    public void inject(Object caller, Object... modules);

}
