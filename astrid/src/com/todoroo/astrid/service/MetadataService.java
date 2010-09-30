package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.ContentValues;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
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
     * @return number of items saved
     */
    public int synchronizeMetadata(long taskId, ArrayList<Metadata> metadata,
            Criterion metadataCriterion) {
        HashSet<ContentValues> newMetadataValues = new HashSet<ContentValues>();
        for(Metadata metadatum : metadata) {
            metadatum.setValue(Metadata.TASK, taskId);
            metadatum.clearValue(Metadata.ID);
            newMetadataValues.add(metadatum.getMergedValues());
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
                ContentValues itemMergedValues = item.getMergedValues();
                if(newMetadataValues.contains(itemMergedValues)) {
                    newMetadataValues.remove(itemMergedValues);
                    continue;
                }

                // not matched. cut it
                metadataDao.delete(id);
            }
        } finally {
            cursor.close();
        }

        // everything that remains shall be written
        int written = 0;
        for(ContentValues values : newMetadataValues) {
            item.clear();
            item.mergeWith(values);
            metadataDao.persist(item);
            ++written;
        }

        return written;
    }
}
