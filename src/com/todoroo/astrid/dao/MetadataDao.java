/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.database.Cursor;
import android.database.DatabaseUtils;

import com.todoroo.android.data.AbstractDao;
import com.todoroo.android.data.AbstractDatabase;
import com.todoroo.android.data.Property;
import com.todoroo.android.data.TodorooCursor;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataDao extends AbstractDao<Metadata> {

	public MetadataDao() {
        super(Metadata.class);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class MetadataSql {

    	/** Returns all metadata associated with a given task */
    	public static String byTask(long taskId) {
    	    return String.format("(%s = %d)", Metadata.TASK, //$NON-NLS-1$
    	            taskId);
    	}

    	/** Returns all metadata associated with a given key */
    	public static String withKey(String key) {
    	    return String.format("(%s = %s)", Metadata.KEY, //$NON-NLS-1$
    	            DatabaseUtils.sqlEscapeString(key));
    	}

    }

    /**
     * Delete all matching a clause
     * @param database
     * @param where
     * @return # of deleted items
     */
    public int deleteWhere(AbstractDatabase database, String where) {
        return database.getDatabase().delete(Database.METADATA_TABLE, where, null);
    }

    /**
     * Fetch all metadata that are unattached to the task
     * @param database
     * @param properties
     * @return
     */
    public TodorooCursor<Metadata> fetchDangling(AbstractDatabase database, Property<?>[] properties) {
        String sql = String.format("SELECT %s FROM %s LEFT JOIN %s ON %s.%s = %s.%s WHERE %s.%s ISNULL", //$NON-NLS-1$
                propertiesForSelect(properties, true),
                Database.METADATA_TABLE, Database.TASK_TABLE,
                Database.METADATA_TABLE, Metadata.TASK, Database.TASK_TABLE,
                    Task.ID,
                Database.TASK_TABLE, Task.TITLE);
        Cursor cursor = database.getDatabase().rawQuery(sql, null);
        return new TodorooCursor<Metadata>(cursor);
    }

}

