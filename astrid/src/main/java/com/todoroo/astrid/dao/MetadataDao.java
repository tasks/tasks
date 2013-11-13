/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.AstridPreferences;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataDao extends DatabaseDao<Metadata> {

    @Autowired
    private Database database;

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
            persist(item);
        }
    }

    @Override
    public boolean persist(Metadata item) {
        if(!item.containsValue(Metadata.CREATION_DATE)) {
            item.setValue(Metadata.CREATION_DATE, DateUtilities.now());
        }

        boolean state = super.persist(item);
        if(Preferences.getBoolean(AstridPreferences.P_FIRST_LIST, true)) {
            if (state && item.containsNonNullValue(Metadata.KEY) &&
                    item.getValue(Metadata.KEY).equals(TaskToTagMetadata.KEY)) {
                Preferences.setBoolean(AstridPreferences.P_FIRST_LIST, false);
            }
        }
        Astrid2TaskProvider.notifyDatabaseModification();
        return state;
    }

    /**
     * Fetch all metadata that are unattached to the task
     */
    public TodorooCursor<Metadata> fetchDangling(Property<?>... properties) {
        Query sql = Query.select(properties).from(Metadata.TABLE).join(Join.left(Task.TABLE,
                Metadata.TASK.eq(Task.ID))).where(Task.TITLE.isNull());
        Cursor cursor = database.rawQuery(sql.toString());
        return new TodorooCursor<Metadata>(cursor, properties);
    }
}

