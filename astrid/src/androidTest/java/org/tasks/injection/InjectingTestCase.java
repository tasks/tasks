package org.tasks.injection;

import com.todoroo.andlib.test.TodorooTestCase;

import java.util.List;

import static java.util.Collections.emptyList;

public abstract class InjectingTestCase extends TodorooTestCase {

	@Override
	protected void setUp() {
        super.setUp();

        new TestInjector(getContext()).inject(this, getModules().toArray());
	}

    protected List<Object> getModules() {
        return emptyList();
    }
}
