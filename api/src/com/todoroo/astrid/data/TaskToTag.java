package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.api.AstridApiConstants;

@SuppressWarnings("nls")
public class TaskToTag extends AbstractModel {

    /** table for this model */
    public static final Table TABLE = new Table("tasks_to_tags", TaskToTag.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    public static final LongProperty TASK_ID = new LongProperty(
            TABLE, "taskId");

    public static final LongProperty TASK_UUID = new LongProperty(
            TABLE, "taskUuid");

    public static final LongProperty TAG_ID = new LongProperty(
            TABLE, "tagId");

    public static final LongProperty TAG_UUID = new LongProperty(
            TABLE, "tagUuid");

    public static final LongProperty DELETED_AT = new LongProperty(
            TABLE, "deletedAt");

    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, "pushedAt");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TaskToTag.class);

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(DELETED_AT.name, 0L);
        defaultValues.put(PUSHED_AT.name, 0L);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<TaskToTag> CREATOR = new ModelCreator<TaskToTag>(TaskToTag.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
