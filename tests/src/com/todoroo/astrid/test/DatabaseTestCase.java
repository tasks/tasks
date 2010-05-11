package com.todoroo.astrid.test;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Test case that automatically sets up and tears down a test database
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseTestCase extends TodorooTestCase {

    @Autowired
	public Database database;

    static {
        AstridDependencyInjector.initialize();
    }

	@Override
	protected void setUp() throws Exception {
	    super.setUp();

		// create new test database
        database.clear();
		database.openForWriting();
	}

	@Override
	protected void tearDown() throws Exception {
		database.close();
	}
}
