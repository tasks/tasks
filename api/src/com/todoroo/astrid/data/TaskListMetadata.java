/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Data Model which represents a user.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class TaskListMetadata extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("task_list_metadata", TaskListMetadata.class);

    /** model class for entries in the outstanding table */
    public static final Class<? extends OutstandingEntry<TaskListMetadata>> OUTSTANDING_MODEL = TaskListMetadataOutstanding.class;

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME);

    /** Tag UUID */
    public static final StringProperty TAG_UUID = new StringProperty(
            TABLE, "tag_uuid");

    /** Filter id (one of below) */
    public static final StringProperty FILTER = new StringProperty(
            TABLE, "filter");

    /** Tree of task ids (serialized to json array) */
    public static final StringProperty TASK_IDS = new StringProperty(
            TABLE, "task_ids", Property.PROP_FLAG_JSON);

    /** Sort setting (one of below) */
    public static final StringProperty SORT = new StringProperty(
            TABLE, "sort");

    /** Settings hash */
    public static final StringProperty SETTINGS = new StringProperty(
            TABLE, "settings", Property.PROP_FLAG_JSON);

    /** Array of child tags (for folders) */
    public static final StringProperty CHILD_TAG_IDS = new StringProperty(
            TABLE, "child_tags", Property.PROP_FLAG_JSON);

    /** If the folder is collapsed */
    public static final IntegerProperty IS_COLLAPSED = new IntegerProperty(
            TABLE, "is_collapsed");



    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TaskListMetadata.class);

    public static final String FILTER_ID_ALL = "all";
    public static final String FILTER_ID_TODAY = "today";
    public static final String FILTER_ID_NO_LIST = "nolist";
    public static final String FILTER_ID_ASSIGNED_BY_ME = "assigned";

    public static final String SORT_AUTO = "auto";
    public static final String SORT_MANUAL = "manual";
    public static final String SORT_DUE = "due";
    public static final String SORT_CREATED = "created";
    public static final String SORT_COMPLETED = "completed";
    public static final String SORT_UPDATED = "updated";
    public static final String SORT_TITLE = "title";

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(PUSHED_AT.name, 0);
        defaultValues.put(TAG_UUID.name, NO_UUID);
        defaultValues.put(FILTER.name, "");
        defaultValues.put(TASK_IDS.name, "[]");
        defaultValues.put(SORT.name, "");
        defaultValues.put(SETTINGS.name, "{}");
        defaultValues.put(CHILD_TAG_IDS.name, "[]");
        defaultValues.put(IS_COLLAPSED.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public TaskListMetadata() {
        super();
    }

    public TaskListMetadata(TodorooCursor<TaskListMetadata> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<TaskListMetadata> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
    }

    public static boolean taskIdsIsEmpty(String taskIds) {
        return TextUtils.isEmpty(taskIds) || "[]".equals(taskIds) || "[\"-1\"]".equals(taskIds) || "[-1]".equals(taskIds);
    }

    // --- parcelable helpers

    public static final Creator<TaskListMetadata> CREATOR = new ModelCreator<TaskListMetadata>(TaskListMetadata.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
