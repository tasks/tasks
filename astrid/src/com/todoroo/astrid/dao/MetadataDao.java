/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.todoroo.andlib.data.AbstractModel;
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
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.AstridPreferences;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataDao extends DatabaseDao<Metadata> {

    @Autowired
    private Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
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

    @Override
    protected boolean shouldRecordOutstanding(Metadata item) {
        ContentValues cv = item.getSetValues();
        return super.shouldRecordOutstanding(item) && cv != null &&
                ((cv.containsKey(Metadata.KEY.name) &&
                        TaskToTagMetadata.KEY.equals(item.getValue(Metadata.KEY))) ||
                (cv.containsKey(Metadata.DELETION_DATE.name) &&
                        item.getValue(Metadata.DELETION_DATE) > 0));
    }

    @Override
    protected boolean createOutstandingEntries(long modelId, ContentValues modelSetValues) {
        Long taskId = modelSetValues.getAsLong(Metadata.TASK.name);
        String tagUuid = modelSetValues.getAsString(TaskToTagMetadata.TAG_UUID.name);
        Long deletionDate = modelSetValues.getAsLong(Metadata.DELETION_DATE.name);
        if (taskId == null || taskId == AbstractModel.NO_ID || RemoteModel.isUuidEmpty(tagUuid))
            return true;

        TaskOutstanding to = new TaskOutstanding();
        to.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, taskId);
        to.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());

        String addedOrRemoved = NameMaps.TAG_ADDED_COLUMN;
        if (deletionDate != null && deletionDate > 0)
            addedOrRemoved = NameMaps.TAG_REMOVED_COLUMN;

        to.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, addedOrRemoved);
        to.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, tagUuid);
        database.insert(outstandingTable.name, null, to.getSetValues());
        ActFmSyncThread.getInstance().enqueueMessage(new ChangesHappened<Task, TaskOutstanding>(taskId, Task.class,
                PluginServices.getTaskDao(), PluginServices.getTaskOutstandingDao()), null);
        return true;
    }

    @Override
    public boolean persist(Metadata item) {
        if(!item.containsValue(Metadata.CREATION_DATE))
            item.setValue(Metadata.CREATION_DATE, DateUtilities.now());

        boolean state = super.persist(item);
        if(Preferences.getBoolean(AstridPreferences.P_FIRST_LIST, true)) {
            if (state && item.containsNonNullValue(Metadata.KEY) &&
                    item.getValue(Metadata.KEY).equals(TaskToTagMetadata.KEY)) {
                StatisticsService.reportEvent(StatisticsConstants.USER_FIRST_LIST);
                Preferences.setBoolean(AstridPreferences.P_FIRST_LIST, false);
            }
        }
        Astrid2TaskProvider.notifyDatabaseModification();
        return state;
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

