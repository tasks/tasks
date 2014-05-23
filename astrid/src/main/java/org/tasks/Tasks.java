package org.tasks;

import android.app.Application;

import com.todoroo.astrid.service.AstridDependencyInjector;

import org.tasks.injection.Injector;

public class Tasks extends Application implements Injector {

    Injector injector;

    @Override
    public void onCreate() {
        super.onCreate();

        injector = AstridDependencyInjector.getInjector();
    }

    @Override
    public void inject(Object caller, Object... modules) {
        injector.inject(caller, modules);
    }
}
