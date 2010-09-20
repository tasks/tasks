package com.todoroo.andlib.test;

import android.test.AndroidTestCase;

import com.todoroo.andlib.service.TestDependencyInjector;

/**
 * Base test case for Astrid tests that need a separate injection context
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TodorooTestCaseWithInjector extends AndroidTestCase {

    protected TestDependencyInjector testInjector;

    abstract protected void addInjectables();

	@Override
	protected void setUp() throws Exception {
	    testInjector = TestDependencyInjector.initialize("test");
	    addInjectables();

	    super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
	    super.tearDown();

	    TestDependencyInjector.deinitialize(testInjector);
	}

}
