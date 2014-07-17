/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class MetadataDao extends DatabaseDao<Metadata> {

    @Inject
	public MetadataDao(Database database) {
        super(Metadata.class);
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

    @Override
    public boolean persist(Metadata item) {
        if(!item.containsNonNullValue(Metadata.TASK)) {
            throw new IllegalArgumentException("metadata needs to be attached to a task: " + item.getMergedValues()); //$NON-NLS-1$
        }
        if(!item.containsValue(Metadata.CREATION_DATE)) {
            item.setCreationDate(DateUtilities.now());
        }

        return super.persist(item);
    }

    /**
     * Clean up metadata. Typically called on startup
     */
    public void removeDanglingMetadata() {
        deleteWhere(Metadata.ID.in(Query.select(Metadata.ID).from(Metadata.TABLE).join(Join.left(Task.TABLE,
                Metadata.TASK.eq(Task.ID))).where(Task.TITLE.isNull())));
    }

    public void byTaskAndKey(long taskId, String key, Callback<Metadata> callback) {
        query(callback, Query.select(Metadata.PROPERTIES).where(
                Criterion.and(Metadata.TASK.eq(taskId), Metadata.KEY.eq(key))));
    }
}

