/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.ContentValues;
import android.content.Context;

import com.todoroo.andlib.data.ContentResolverDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;

/**
 * Data access object for accessing Astrid's {@link Metadata} table. A
 * piece of Metadata is information about a task, for example a tag or a
 * note. It operates in a one-to-many relation with tasks.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataApiDao extends ContentResolverDao<Metadata> {

    public MetadataApiDao(Context context) {
        super(Metadata.class, context, Metadata.CONTENT_URI);
    }

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
     * Synchronize metadata for given task id. Deletes rows in database that
     * are not identical to those in the metadata list, creates rows that
     * have no match.
     *
     * @param taskId id of task to perform synchronization on
     * @param metadata list of new metadata items to save
     * @param metadataCriteria criteria to load data for comparison from metadata
     */
    public void synchronizeMetadata(long taskId, ArrayList<Metadata> metadata,
            Criterion metadataCriteria) {
        HashSet<ContentValues> newMetadataValues = new HashSet<ContentValues>();
        for(Metadata metadatum : metadata) {
            metadatum.setValue(Metadata.TASK, taskId);
            metadatum.clearValue(Metadata.ID);
            newMetadataValues.add(metadatum.getMergedValues());
        }

        Metadata item = new Metadata();
        TodorooCursor<Metadata> cursor = query(Query.select(Metadata.PROPERTIES).where(Criterion.and(MetadataCriteria.byTask(taskId),
                metadataCriteria)));
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
                delete(id);
            }
        } finally {
            cursor.close();
        }

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            item.clear();
            item.mergeWith(values);
            save(item);
        }
    }


}
