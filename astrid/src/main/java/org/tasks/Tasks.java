package org.tasks;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.dao.Database;

import org.tasks.injection.InjectingApplication;

import javax.inject.Inject;

public class Tasks extends InjectingApplication {

    @SuppressWarnings("UnusedDeclaration")
    @Inject Database database;

    @Override
    public void onCreate() {
        super.onCreate();

        ContextManager.setContext(this);
    }
}
