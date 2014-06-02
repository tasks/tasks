/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import org.tasks.injection.InjectingTestCase;

import com.todoroo.astrid.dao.Database;

import javax.inject.Inject;

public class DatabaseTestCase extends InjectingTestCase {

    @Inject protected Database database;

    @Override
    protected void setUp() {
        super.setUp();

        database.clear();
        database.openForWriting();
    }

    @Override
    protected void tearDown() {
        database.close();
        super.tearDown();
    }
}
