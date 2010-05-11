package com.todoroo.astrid.test;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.TestDependencyInjector;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.Database.AstridSQLiteOpenHelper;
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
        TestDependencyInjector.initialize("db").addInjectable("database",
                new AstridTestDatabase());
    }

	@Override
	protected void setUp() throws Exception {
		// create new test database
		AstridTestDatabase.dropTables(getContext());
		database.openForWriting();

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		database.close();
	}

	public static class AstridTestDatabase extends Database {

	    private static final String NAME = "todoroo-test";

        @Override
        public synchronized void openForWriting() {
            if(helper == null)
                helper = new AstridSQLiteOpenHelper(context, NAME, null, VERSION);
            super.openForWriting();
        }

        @Override
        public synchronized void openForReading() {
            if(helper == null)
                helper = new AstridSQLiteOpenHelper(context, NAME, null, VERSION);
            super.openForWriting();
        }

        public static void dropTables(Context context) {
            // force drop database
            SQLiteOpenHelper helper = new AstridSQLiteOpenHelper(context, NAME, null, VERSION);
            helper.onUpgrade(helper.getWritableDatabase(),
                    0, VERSION);
            helper.close();
        }
	}
}
