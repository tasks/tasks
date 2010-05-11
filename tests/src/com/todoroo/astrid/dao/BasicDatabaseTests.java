package com.todoroo.astrid.dao;

import android.database.sqlite.SQLiteDatabase;

import com.todoroo.astrid.test.DatabaseTestCase;


public class BasicDatabaseTests extends DatabaseTestCase {

    /**
     * Test that it's possible to open the database multiple times, to no effect
     */
	public void testOpenMultipleTimes() {
	    SQLiteDatabase sqlDatabase = database.getDatabase();
	    database.openForReading();
	    assertEquals(sqlDatabase, database.getDatabase());
	    database.openForWriting();
		assertEquals(sqlDatabase, database.getDatabase());
		database.openForReading();
		assertEquals(sqlDatabase, database.getDatabase());
	}


	public void testCloseAndReopen() {
	    SQLiteDatabase sqlDatabase = database.getDatabase();
	    database.close();
	    database.openForReading();
	    assertNotSame(sqlDatabase, database.getDatabase());
	}
}
