/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.data.StoreObject;

/**
 * Data Access layer for {@link StoreObject}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class StoreObjectDao extends DatabaseDao<StoreObject> {

    @Autowired
    private Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public StoreObjectDao() {
        super(StoreObject.class);
        DependencyInjectionService.getInstance().inject(this);
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

    	/** Returns store object with type and key */
    	public static Criterion byTypeAndItem(String type, String item) {
    	    return Criterion.and(byType(type), StoreObject.ITEM.eq(item));
    	}

    }

}

