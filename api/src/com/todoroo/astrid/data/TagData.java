/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Data Model which represents a collaboration space for users / tasks.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class TagData extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tagdata", TagData.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** User id */
    public static final LongProperty USER_ID = new LongProperty(
            TABLE, USER_ID_PROPERTY_NAME);

    /** User Object (JSON) */
    public static final StringProperty USER = new StringProperty(
            TABLE, USER_JSON_PROPERTY_NAME);

    /** Remote goal id */
    public static final LongProperty REMOTE_ID = new LongProperty(
            TABLE, REMOTE_ID_PROPERTY_NAME);

    /** Name of Tag */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** Project picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture");

    /** Tag team array (JSON) */
    public static final StringProperty MEMBERS = new StringProperty(
            TABLE, "members");

    /** Tag member count */
    public static final IntegerProperty MEMBER_COUNT = new IntegerProperty(
            TABLE, "memberCount");

    /** Flags */
    public static final IntegerProperty FLAGS = new IntegerProperty(
            TABLE, "flags");

    /** Unixtime Project was created */
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created");

    /** Unixtime Project was last touched */
    public static final LongProperty MODIFICATION_DATE = new LongProperty(
            TABLE, "modified");

    /** Unixtime Project was completed. 0 means active */
    public static final LongProperty COMPLETION_DATE = new LongProperty(
            TABLE, "completed");

    /** Unixtime Project was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted");

    /** Project picture thumbnail */
    public static final StringProperty THUMB = new StringProperty(
            TABLE, "thumb");

    /** Project last activity date */
    public static final LongProperty LAST_ACTIVITY_DATE = new LongProperty(
            TABLE, "lastActivityDate");

    /** Whether user is part of Tag team */
    public static final IntegerProperty IS_TEAM = new IntegerProperty(
            TABLE, "isTeam");

    /** Whether Tag has unread activity */
    public static final IntegerProperty IS_UNREAD = new IntegerProperty(
            TABLE, "isUnread");

    /** Task count */
    public static final IntegerProperty TASK_COUNT = new IntegerProperty(
            TABLE, "taskCount");

    /** Tag Desription */
    public static final StringProperty TAG_DESCRIPTION = new StringProperty(
            TABLE, "tagDescription");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TagData.class);

    // --- flags

    /** whether tag is publicly visible */
    public static final int FLAG_PUBLIC = 1 << 0;

    /** whether user should not be notified of tag activity */
    public static final int FLAG_SILENT = 1 << 1;

    /** whether tag is emergent */
    public static final int FLAG_EMERGENT = 1 << 2;

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(USER_ID.name, 0);
        defaultValues.put(USER.name, "{}");
        defaultValues.put(REMOTE_ID.name, 0);
        defaultValues.put(NAME.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(IS_TEAM.name, 1);
        defaultValues.put(MEMBERS.name, "[]");
        defaultValues.put(MEMBER_COUNT.name, 0);
        defaultValues.put(FLAGS.name, 0);
        defaultValues.put(COMPLETION_DATE.name, 0);
        defaultValues.put(DELETION_DATE.name, 0);

        defaultValues.put(THUMB.name, "");
        defaultValues.put(LAST_ACTIVITY_DATE.name, 0);
        defaultValues.put(IS_UNREAD.name, 0);
        defaultValues.put(TASK_COUNT.name, 0);
        defaultValues.put(TAG_DESCRIPTION.name, "");
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

    // --- parcelable helpers

    public static final Creator<TagData> CREATOR = new ModelCreator<TagData>(TagData.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

    // --- data access methods

    /** Checks whether task is done. Requires COMPLETION_DATE */
    public boolean isCompleted() {
        return getValue(COMPLETION_DATE) > 0;
    }

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        // assume false if we didn't load deletion date
        if(!containsValue(DELETION_DATE))
            return false;
        else
            return getValue(DELETION_DATE) > 0;
    }

    public String getPictureHash() {
        String s = getValue(TagData.NAME) + "" + getValue(TagData.CREATION_DATE);
        return s;
    }

}
