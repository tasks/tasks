package org.tasks.injection;

import android.test.AndroidTestCase;

import dagger.ObjectGraph;

import static org.tasks.TestUtilities.initializeMockito;

public abstract class InjectingTestCase extends AndroidTestCase {

    @Override
    protected void setUp() {
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
