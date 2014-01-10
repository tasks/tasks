package com.todoroo.andlib.test;

import com.todoroo.andlib.service.RobolectricTestDependencyInjector;

import org.tasks.Broadcaster;

import static org.mockito.Mockito.mock;

abstract public class TodorooRobolectricTestCaseWithInjector extends TodorooRobolectricTestCase {

    protected RobolectricTestDependencyInjector testInjector;

    protected void addInjectables() {
    }

	@Override
	public void setUp() throws Exception {
	    testInjector = RobolectricTestDependencyInjector.initialize("test");
        testInjector.addInjectable("broadcaster", mock(Broadcaster.class));
	    addInjectables();

	    super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
	    super.tearDown();

	    RobolectricTestDependencyInjector.deinitialize(testInjector);
	}

}
