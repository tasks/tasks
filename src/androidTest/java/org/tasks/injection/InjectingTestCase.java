package org.tasks.injection;

import android.test.AndroidTestCase;

import com.todoroo.andlib.service.ContextManager;

import dagger.ObjectGraph;

import static org.tasks.TestUtilities.initializeMockito;

public abstract class InjectingTestCase extends AndroidTestCase {

    @Override
    protected void setUp() {
        ContextManager.setContext(getContext());

        initializeMockito(getContext());

        ObjectGraph objectGraph = ObjectGraph.create(new TestModule(getContext()));
        Object extension = getModule();
        if (extension != null) {
            objectGraph = objectGraph.plus(extension);
        }
        objectGraph.inject(this);
    }

    protected Object getModule() {
        return null;
    }
}
