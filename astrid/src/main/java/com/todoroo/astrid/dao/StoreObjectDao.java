/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.data.StoreObject;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Data Access layer for {@link StoreObject}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class StoreObjectDao extends DatabaseDao<StoreObject> {

    @Inject
	public StoreObjectDao(Database database) {
        super(StoreObject.class);
        setDatabase(database);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class StoreObjectCriteria {

    	/** Returns all store objects with given type */
    	public static Criterion byType(String type) {
    	    return StoreObject.TYPE.eq(type);
    	}
    }

}

