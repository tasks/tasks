/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.database.Cursor;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataDao extends GenericDao<Metadata> {

    @Autowired
    Database database;

	public MetadataDao() {
        super(Metadata.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class MetadataCriteria {

    	/** Returns all metadata associated with a given task */
    	public static Criterion byTask(long taskId) {
    	    return Metadata.TASK.eq(taskId);
    	}

    	/** Returns all metadata associated with a given key */
    	public static Criterion withKey(String key) {
    	    return Metadata.KEY.eq(key);
    	}

    	/** Returns all metadata associated with a given key */
    	public static Criterion byTaskAndwithKey(long taskId, String key) {
    	    return Criterion.and(withKey(key), byTask(taskId));
    	}

    }



    /**
     * Fetch all metadata that are unattached to the task
     * @param database
     * @param properties
     * @return
     */
    public TodorooCursor<Metadata> fetchDangling(Property<?>... properties) {
        Query sql = Query.select(properties).from(Metadata.TABLE).join(Join.left(Task.TABLE,
                Metadata.TASK.eq(Task.ID))).where(Task.TITLE.isNull());
        Cursor cursor = database.rawQuery(sql.toString(), null);
        return new TodorooCursor<Metadata>(cursor, properties);
    }

}

