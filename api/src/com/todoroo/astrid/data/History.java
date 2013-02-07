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
    public static final LongProperty UUID = new LongProperty(
            TABLE, RemoteModel.UUID_PROPERTY_NAME);

    /** Created at */
    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, "created_at", Property.PROP_FLAG_DATE);

    /** User id */
    public static final StringProperty USER_UUID = new StringProperty(
            TABLE, "user_id", Property.PROP_FLAG_USER_ID);

    /** Column name */
    public static final StringProperty COLUMN = new StringProperty(
            TABLE, "column");

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

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static {
        defaultValues.put(UUID.name, 0L);
        defaultValues.put(CREATED_AT.name, 0L);
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

}
