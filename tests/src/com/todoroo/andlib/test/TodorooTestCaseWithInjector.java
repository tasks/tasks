/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;

import com.todoroo.andlib.service.TestDependencyInjector;

/**
 * Base test case for Astrid tests that need a separate injection context
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TodorooTestCaseWithInjector extends TodorooTestCase {

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
