package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

@SuppressWarnings("nls")
public class History extends AbstractModel {

    /** table for this model */
    public static final Table TABLE = new Table("history", History.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote ID */
    public static final StringProperty UUID = new StringProperty(
            TABLE, RemoteModel.UUID_PROPERTY_NAME);

    /** Created at */
    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, "created_at", Property.PROP_FLAG_DATE);

    /** User id */
    public static final StringProperty USER_UUID = new StringProperty(
            TABLE, "user_id", Property.PROP_FLAG_USER_ID);

    /** Column name */
    public static final StringProperty COLUMN = new StringProperty(
            TABLE, "columnString");

    /** Old value */
    public static final StringProperty OLD_VALUE = new StringProperty(
            TABLE, "old_value", Property.PROP_FLAG_NULLABLE);

    /** New value */
    public static final StringProperty NEW_VALUE = new StringProperty(
            TABLE, "new_value", Property.PROP_FLAG_NULLABLE);

    /** Table identifier */
    public static final StringProperty TABLE_ID = new StringProperty(
            TABLE, "table_id");

    /** Target identifier */
    public static final StringProperty TARGET_ID = new StringProperty(
            TABLE, "target_id");

    /** Task name and id (JSONArray) */
    public static final StringProperty TASK = new StringProperty(
            TABLE, "task");

    /** Associated tag id */
    public static final StringProperty TAG_ID = new StringProperty(
            TABLE, "tag_id");


    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static {
        defaultValues.put(UUID.name, 0L);
        defaultValues.put(CREATED_AT.name, 0L);
        defaultValues.put(USER_UUID.name, RemoteModel.NO_UUID);
        defaultValues.put(OLD_VALUE.name, "");
        defaultValues.put(NEW_VALUE.name, "");
        defaultValues.put(TAG_ID.name, RemoteModel.NO_UUID);
        defaultValues.put(TASK.name, "");
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public History() {
        super();
    }

    public History(TodorooCursor<History> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<History> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(History.class);

    private static final Creator<History> CREATOR = new ModelCreator<History>(History.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

    // ---- Column ids
    public static final String COL_TAG_ADDED = "tag_added";
    public static final String COL_TAG_REMOVED = "tag_removed";
    public static final String COL_SHARED_WITH = "shared_with";
    public static final String COL_UNSHARED_WITH = "unshared_with";
    public static final String COL_MEMBER_ADDED = "member_added";
    public static final String COL_MEMBER_REMOVED = "member_removed";
    public static final String COL_COMPLETED_AT = "completed_at";
    public static final String COL_DELETED_AT = "deleted_at";
    public static final String COL_IMPORTANCE = "importance";
    public static final String COL_NOTES_LENGTH = "notes_length";
    public static final String COL_PUBLIC = "public";
    public static final String COL_DUE = "due";
    public static final String COL_REPEAT = "repeat";
    public static final String COL_TASK_REPEATED = "task_repeated";
    public static final String COL_TITLE = "title";
    public static final String COL_NAME = "name";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_PICTURE_ID = "picture_id";
    public static final String COL_DEFAULT_LIST_IMAGE_ID = "default_list_image_id";
    public static final String COL_IS_SILENT = "is_silent";
    public static final String COL_IS_FAVORITE = "is_favorite";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_ATTACHMENT_ADDED = "attachment_added";
    public static final String COL_ATTACHMENT_REMOVED = "attachment_removed";
    public static final String COL_ACKNOWLEDGED = "acknowledged";


}
