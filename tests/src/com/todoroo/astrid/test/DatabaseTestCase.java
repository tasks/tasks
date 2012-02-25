package com.todoroo.astrid.test;

import java.io.File;

import com.todoroo.andlib.test.TodorooTestCaseWithInjector;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.legacy.AlarmDatabase;
import com.todoroo.astrid.provider.ProviderTestUtilities;
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
	    database.clear();
		database.openForWriting();

		ProviderTestUtilities.setDatabaseOverride(database);
	}

	/**
	 * Helper to delete a database by name
	 * @param toDelete
	 */
	protected void deleteDatabase(String toDelete) {
	    File db = getContext().getDatabasePath(toDelete);
	    if(db.exists())
	        db.delete();
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

	public static class TestAlarmsDatabase extends AlarmDatabase {
	    @Override
        public String getName() {
	        return "alarmstest";
	    }
	}
}
