package com.todoroo.astrid.test;

import java.io.File;

import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.TestDependencyInjector;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.alarms.AlarmDatabase;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Test case that automatically sets up and tears down a test database
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseTestCase extends TodorooTestCase {

	public static Database database = new TestDatabase();

    public AlarmDatabase alarmsDatabase;

    static {
        AstridDependencyInjector.initialize();

        // initialize test dependency injector
        TestDependencyInjector injector = TestDependencyInjector.initialize("db");
        injector.addInjectable("database", database);
    }

	@Override
	protected void setUp() throws Exception {
	    super.setUp();

	    DependencyInjectionService.getInstance().inject(this);

		// empty out test databases
	    database.clear();

		database.openForWriting();
	}

	/**
	 * Helper to delete a database by name
	 * @param database
	 */
	protected void deleteDatabase(String database) {
	    File db = getContext().getDatabasePath(database);
	    if(db.exists())
	        db.delete();
    }

    @Override
	protected void tearDown() throws Exception {
		database.close();
	}

	public static class TestDatabase extends Database {
        @Override
	    protected String getName() {
	        return "databasetest";
	    }
	}

	public static class TestAlarmsDatabase extends AlarmDatabase {
	    @Override
        protected String getName() {
	        return "alarmstest";
	    }
	}
}
