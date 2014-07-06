/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;

/**
 * Data Model which represents a collaboration space for users / tasks.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagData extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tagdata", TagData.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote goal id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Name of Tag */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** Unixtime Project was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted", Property.PROP_FLAG_DATE);

    /** Task count */
    public static final IntegerProperty TASK_COUNT = new IntegerProperty(
            TABLE, "taskCount");

    /** Tag ordering */
    @Deprecated
    public static final StringProperty TAG_ORDERING = new StringProperty(
            TABLE, "tagOrdering");

    /** Last autosync */
    public static final LongProperty LAST_AUTOSYNC = new LongProperty(
            TABLE, "lastAutosync");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TagData.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(NAME.name, "");
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(LAST_AUTOSYNC.name, 0);
        defaultValues.put(TASK_COUNT.name, 0);
        defaultValues.put(TAG_ORDERING.name, "[]");
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public TagData() {
        super();
    }

    public TagData(TodorooCursor<TagData> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<TagData> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public String getUuid() {
        return getUuidHelper(UUID);
    }

    // --- parcelable helpers

    public static final Creator<TagData> CREATOR = new ModelCreator<>(TagData.class);

    // --- data access methods

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        // assume false if we didn't load deletion date
        return containsValue(DELETION_DATE) && getValue(DELETION_DATE) > 0;
    }

    public String getName() {
        return getValue(NAME);
    }

    public void setName(String name) {
        setValue(NAME, name);
    }

    public String getTagOrdering() {
        return getValue(TAG_ORDERING);
    }

    public Long getLastAutosync() {
        return getValue(LAST_AUTOSYNC);
    }

    public void setLastAutosync(Long lastAutosync) {
        setValue(LAST_AUTOSYNC, lastAutosync);
    }

    public String getUUID() {
        return getValue(UUID);
    }

    public void setUUID(String uuid) {
        setValue(UUID, uuid);
    }

    public Integer getTaskCount() {
        return getValue(TASK_COUNT);
    }

    public void setTaskCount(Integer taskCount) {
        setValue(TASK_COUNT, taskCount);
    }

    public void setDeletionDate(Long deletionDate) {
        setValue(DELETION_DATE, deletionDate);
    }
}
