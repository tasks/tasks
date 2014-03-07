/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.test.TodorooTestCaseWithInjector;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Test case that automatically sets up and tears down a test database
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseTestCase extends TodorooTestCaseWithInjector {

    static {
        AstridDependencyInjector.initialize();
    }

    public static Database database = new TestDatabase();

    @Override
    protected void addInjectables() {
        testInjector.addInjectable("database", database);
    }

	@Override
	protected void setUp() throws Exception {
	    // call upstream setup, which invokes dependency injector
	    super.setUp();

		// empty out test databases
	    assertNotNull(ContextManager.getContext());
	    database.clear();
		database.openForWriting();

        Astrid3ContentProvider.setDatabaseOverride(database);
	}

    @Override
	protected void tearDown() throws Exception {
		database.close();
		super.tearDown();
	}

	public static class TestDatabase extends Database {
        @Override
	    public String getName() {
	        return "databasetest";
	    }
	}
}
