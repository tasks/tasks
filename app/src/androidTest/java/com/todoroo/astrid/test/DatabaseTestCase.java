/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import com.todoroo.astrid.dao.Database;

import org.junit.After;
import org.tasks.injection.InjectingTestCase;

import static android.support.test.InstrumentationRegistry.getTargetContext;

public abstract class DatabaseTestCase extends InjectingTestCase {

    protected Database database;

    @Override
    public void setUp() {
        super.setUp();

        database = component.getDatabase();

        database.close();
        getTargetContext().deleteDatabase(database.getName());
        database.openForWriting();
    }

    @After
    public void tearDown() {
        database.close();
    }
}
