package com.todoroo.astrid.dao;

import com.todoroo.astrid.test.DatabaseTestCase;

import android.database.sqlite.SQLiteDatabase;


public class BasicDatabaseTests extends DatabaseTestCase {

    /**
     * Test that it's possible to open the database multiple times, to no effect
     */
	public void testOpenMultipleTimes() {
	    SQLiteDatabase sqlDatabase = database.getDatabase();
	    database.openForReading(getContext());
	    assertEquals(sqlDatabase, database.getDatabase());
	    database.openForWriting(getContext());
		assertEquals(sqlDatabase, database.getDatabase());
		database.openForReading(getContext());
		assertEquals(sqlDatabase, database.getDatabase());
	}

	
	public void testCloseAndReopen() {
	    SQLiteDatabase sqlDatabase = database.getDatabase();
	    database.close();
	    database.openForReading(getContext());
	    assertNotSame(sqlDatabase, database.getDatabase());
	}
}
