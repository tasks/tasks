package org.tasks.injection;

import com.todoroo.andlib.test.TodorooTestCase;

import dagger.ObjectGraph;

public abstract class InjectingTestCase extends TodorooTestCase {

    @Override
    protected void setUp() {
        super.setUp();

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
