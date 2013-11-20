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

    /** User id */
    public static final StringProperty USER_ID = new StringProperty(
            TABLE, USER_ID_PROPERTY_NAME, Property.PROP_FLAG_USER_ID);

    /** User Object (JSON) */
    @Deprecated public static final StringProperty USER = new StringProperty(
            TABLE, USER_JSON_PROPERTY_NAME);

    /** Remote goal id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Name of Tag */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** Project picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture", Property.PROP_FLAG_JSON | Property.PROP_FLAG_PICTURE);

    /** Tag team array (JSON) */
    @Deprecated public static final StringProperty MEMBERS = new StringProperty(
            TABLE, "members");

    /** Tag member count */
    public static final IntegerProperty MEMBER_COUNT = new IntegerProperty(
            TABLE, "memberCount");

    /** Flags */
    public static final IntegerProperty FLAGS = new IntegerProperty(
            TABLE, "flags");

    /** Unixtime Project was created */
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created", Property.PROP_FLAG_DATE);

    /** Unixtime Project was completed. 0 means active */
    public static final LongProperty COMPLETION_DATE = new LongProperty(
            TABLE, "completed", Property.PROP_FLAG_DATE);

    /** Unixtime Project was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted", Property.PROP_FLAG_DATE);

    /** Project picture thumbnail */
    public static final StringProperty THUMB = new StringProperty(
            TABLE, "thumb");

    /** Project last activity date */
    public static final LongProperty LAST_ACTIVITY_DATE = new LongProperty(
            TABLE, "lastActivityDate", Property.PROP_FLAG_DATE);

    /** Whether user is part of Tag team */
    public static final IntegerProperty IS_TEAM = new IntegerProperty(
            TABLE, "isTeam");

    /** Whether Tag has unread activity */
    public static final IntegerProperty IS_UNREAD = new IntegerProperty(
            TABLE, "isUnread");

    /** Whether tag is a folder */
    public static final IntegerProperty IS_FOLDER = new IntegerProperty(
            TABLE, "isFolder", Property.PROP_FLAG_BOOLEAN);

    /** Task count */
    public static final IntegerProperty TASK_COUNT = new IntegerProperty(
            TABLE, "taskCount");

    /** Tag Desription */
    public static final StringProperty TAG_DESCRIPTION = new StringProperty(
            TABLE, "tagDescription");

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME, Property.PROP_FLAG_DATE);

    /** Tasks pushed at date */
    public static final LongProperty TASKS_PUSHED_AT = new LongProperty(
            TABLE, "tasks_pushed_at", Property.PROP_FLAG_DATE);

    /** Metadata pushed at date */
    public static final LongProperty METADATA_PUSHED_AT = new LongProperty(
            TABLE, "metadata_pushed_at", Property.PROP_FLAG_DATE);

    /** User activities pushed at date */
    public static final LongProperty USER_ACTIVITIES_PUSHED_AT = new LongProperty(
            TABLE, "activities_pushed_at", Property.PROP_FLAG_DATE);

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
        defaultValues.put(USER_ID.name, "0");
        defaultValues.put(USER.name, "");
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(NAME.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(IS_TEAM.name, 1);
        defaultValues.put(MEMBERS.name, "");
        defaultValues.put(MEMBER_COUNT.name, 0);
        defaultValues.put(FLAGS.name, 0);
        defaultValues.put(COMPLETION_DATE.name, 0);
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(LAST_AUTOSYNC.name, 0);

        defaultValues.put(THUMB.name, "");
        defaultValues.put(LAST_ACTIVITY_DATE.name, 0);
        defaultValues.put(IS_UNREAD.name, 0);
        defaultValues.put(TASK_COUNT.name, 0);
        defaultValues.put(TAG_DESCRIPTION.name, "");
        defaultValues.put(PUSHED_AT.name, 0L);
        defaultValues.put(TASKS_PUSHED_AT.name, 0L);
        defaultValues.put(METADATA_PUSHED_AT.name, 0L);
        defaultValues.put(USER_ACTIVITIES_PUSHED_AT.name, 0L);
        defaultValues.put(TAG_ORDERING.name, "[]");
        defaultValues.put(IS_FOLDER.name, 0);
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
        if(!containsValue(DELETION_DATE)) {
            return false;
        } else {
            return getValue(DELETION_DATE) > 0;
        }
    }

}
