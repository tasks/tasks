/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;

/**
 * Data Model which represents a user.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TaskListMetadata extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("task_list_metadata", TaskListMetadata.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Tag UUID */
    public static final StringProperty TAG_UUID = new StringProperty(
            TABLE, "tag_uuid");

    /** Filter id (one of below) */
    public static final StringProperty FILTER = new StringProperty(
            TABLE, "filter");

    /** Tree of task ids (serialized to json array) */
    public static final StringProperty TASK_IDS = new StringProperty(
            TABLE, "task_ids", Property.PROP_FLAG_JSON);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TaskListMetadata.class);

    public static final String FILTER_ID_ALL = "all";
    public static final String FILTER_ID_TODAY = "today";

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(TAG_UUID.name, NO_UUID);
        defaultValues.put(FILTER.name, "");
        defaultValues.put(TASK_IDS.name, "[]");
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<TaskListMetadata> CREATOR = new ModelCreator<>(TaskListMetadata.class);

    public String getTaskIDs() {
        return getValue(TASK_IDS);
    }

    public void setTaskIDs(String taskIds) {
        setValue(TASK_IDS, taskIds);
    }

    public void setTagUUID(String tagUuid) {
        setValue(TAG_UUID, tagUuid);
    }

    public void setFilter(String filter) {
        setValue(FILTER, filter);
    }
}
