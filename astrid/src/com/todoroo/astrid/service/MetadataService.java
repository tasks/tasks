/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;

/**
 * Service layer for {@link Metadata}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataService {

    public static interface SynchronizeMetadataCallback {
        public void beforeDeleteMetadata(Metadata m);
    }

    @Autowired
    private MetadataDao metadataDao;

    public MetadataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Clean up metadata. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Metadata> cursor = metadataDao.fetchDangling(Metadata.ID);
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                metadataDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<Metadata> query(Query query) {
        return metadataDao.query(query);
    }

    /**
     * Delete from metadata table where rows match a certain condition
     * @param where
     */
    public int deleteWhere(Criterion where) {
        return metadataDao.deleteWhere(where);
    }

    /**
     * Delete from metadata table where rows match a certain condition
     * @param where predicate for which rows to update
     * @param metadata values to set
     */
    public int update(Criterion where, Metadata metadata) {
        return metadataDao.update(where, metadata);
    }

    /**
     * Save a single piece of metadata
     * @param metadata
     */
    public boolean save(Metadata metadata) {
        if(!metadata.containsNonNullValue(Metadata.TASK))
            throw new IllegalArgumentException("metadata needs to be attached to a task: " + metadata.getMergedValues()); //$NON-NLS-1$

        return metadataDao.persist(metadata);
    }

    /**
     * Synchronize metadata for given task id
     * @param id
     * @param metadata
     * @param metadataKeys
     * @return true if there were changes
     */
    public boolean synchronizeMetadata(long taskId, ArrayList<Metadata> metadata,
            Criterion metadataCriterion, SynchronizeMetadataCallback callback, boolean hardDelete) {
        boolean dirty = false;
        HashSet<ContentValues> newMetadataValues = new HashSet<ContentValues>();
        for(Metadata metadatum : metadata) {
            metadatum.setValue(Metadata.TASK, taskId);
            metadatum.clearValue(Metadata.CREATION_DATE);
            metadatum.clearValue(Metadata.ID);

            ContentValues values = metadatum.getMergedValues();
            for(Entry<String, Object> entry : values.valueSet()) {
                if(entry.getKey().startsWith("value")) //$NON-NLS-1$
                    values.put(entry.getKey(), entry.getValue().toString());
            }
            newMetadataValues.add(values);
        }

        Metadata item = new Metadata();
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).where(Criterion.and(MetadataCriteria.byTask(taskId),
                metadataCriterion)));
        try {
            // try to find matches within our metadata list
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                item.readFromCursor(cursor);
                long id = item.getId();

                // clear item id when matching with incoming values
                item.clearValue(Metadata.ID);
                item.clearValue(Metadata.CREATION_DATE);
                ContentValues itemMergedValues = item.getMergedValues();

                if(newMetadataValues.contains(itemMergedValues)) {
                    newMetadataValues.remove(itemMergedValues);
                    continue;
                }

                // not matched. cut it
                item.setId(id);
                if (callback != null) {
                    callback.beforeDeleteMetadata(item);
                }
                if (hardDelete)
                    metadataDao.delete(id);
                else {
                    item.setValue(Metadata.DELETION_DATE, DateUtilities.now());
                    metadataDao.persist(item);
                }
                dirty = true;
            }
        } finally {
            cursor.close();
        }

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            item.clear();
            item.setValue(Metadata.CREATION_DATE, DateUtilities.now());
            item.mergeWith(values);
            metadataDao.persist(item);
            dirty = true;
        }

        return dirty;
    }

    public boolean synchronizeMetadata(long taskId, ArrayList<Metadata> metadata,
            Criterion metadataCriterion, boolean hardDelete) {
        return synchronizeMetadata(taskId, metadata, metadataCriterion, null, hardDelete);
    }

    /**
     * Does metadata with this key and task exist?
     */
    public boolean hasMetadata(long id, String key) {
        CountProperty count = new CountProperty();
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(
                count).where(MetadataCriteria.byTaskAndwithKey(id, key)));
        try {
            cursor.moveToFirst();
            return cursor.get(count) > 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * Deletes the given metadata
     * @param metadata
     */
    public void delete(Metadata metadata) {
        metadataDao.delete(metadata.getId());
    }
}
