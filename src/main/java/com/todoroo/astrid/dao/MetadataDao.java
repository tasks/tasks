/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class MetadataDao {

    private final DatabaseDao<Metadata> dao;

    @Inject
	public MetadataDao(Database database) {
        dao = new DatabaseDao<>(database, Metadata.class);
    }

    public void query(Callback<Metadata> callback, Query query) {
        query(query, callback);
    }

    public void query(Query query, Callback<Metadata> callback) {
        dao.query(query, callback);
    }

    public Metadata getFirst(Query query) {
        return dao.getFirst(query);
    }

    public int update(Criterion where, Metadata template) {
        return dao.update(where, template);
    }

    public void createNew(Metadata metadata) {
        dao.createNew(metadata);
    }

    public List<Metadata> toList(Criterion criterion) {
        return toList(Query.select(Metadata.PROPERTIES).where(criterion));
    }

    public List<Metadata> toList(Query where) {
        return dao.toList(where);
    }

    public int deleteWhere(Criterion criterion) {
        return dao.deleteWhere(criterion);
    }

    public boolean delete(long id) {
        return dao.delete(id);
    }

    public void saveExisting(Metadata metadata) {
        dao.saveExisting(metadata);
    }

    public Metadata fetch(long id, Property<?>... properties) {
        return dao.fetch(id, properties);
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

    public boolean persist(Metadata item) {
        if(!item.containsNonNullValue(Metadata.TASK)) {
            throw new IllegalArgumentException("metadata needs to be attached to a task: " + item.getMergedValues()); //$NON-NLS-1$
        }
        if(!item.containsValue(Metadata.CREATION_DATE)) {
            item.setCreationDate(DateUtilities.now());
        }

        return dao.persist(item);
    }

    /**
     * Clean up metadata. Typically called on startup
     */
    public void removeDanglingMetadata() {
        dao.deleteWhere(Metadata.ID.in(Query.select(Metadata.ID).from(Metadata.TABLE).join(Join.left(Task.TABLE,
                Metadata.TASK.eq(Task.ID))).where(Task.TITLE.isNull())));
    }

    public List<Metadata> byTask(long taskId) {
        return toList(MetadataCriteria.byTask(taskId));
    }

    public void byTask(long taskId, Callback<Metadata> callback) {
        dao.query(callback, Query.select(Metadata.PROPERTIES).where(Metadata.TASK.eq(taskId)));
    }

    public void byTaskAndKey(long taskId, String key, Callback<Metadata> callback) {
        dao.query(callback, Query.select(Metadata.PROPERTIES).where(
                Criterion.and(Metadata.TASK.eq(taskId), Metadata.KEY.eq(key))));
    }
}

