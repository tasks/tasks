/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import com.todoroo.astrid.dao.Database;

import org.tasks.injection.InjectingTestCase;

import javax.inject.Inject;

public abstract class DatabaseTestCase extends InjectingTestCase {

    @Inject protected Database database;

    @Override
    protected void setUp() {
        super.setUp();

        database.close();
        getContext().deleteDatabase(database.getName());
        database.openForWriting();
    }

    @Override
    protected void tearDown() {
        database.close();
    }
}
