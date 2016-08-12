/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import com.todoroo.astrid.dao.Database;

import org.tasks.injection.InjectingTestCase;

public abstract class DatabaseTestCase extends InjectingTestCase {

    protected Database database;

    @Override
    protected void setUp() {
        super.setUp();

        database = component.getDatabase();

        database.close();
        getContext().deleteDatabase(database.getName());
        database.openForWriting();
    }

    @Override
    protected void tearDown() {
        database.close();
    }
}
