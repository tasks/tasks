/**
 * See the file "LICENSE" for the full license governing this code.
 */
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

/**
 * Data Model which represents an update (e.g. a comment or data update event)
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Update extends RemoteModel {

    // --- table

    /** table for this model */
    public static final Table TABLE = new Table("updates", Update.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote ID */
    public static final LongProperty REMOTE_ID = new LongProperty(
            TABLE, REMOTE_ID_PROPERTY_NAME);

    /** Associated Task remote-id (if any) */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** Associated Tag remote-ids (comma separated list with leading and trailing commas) */
    public static final StringProperty TAGS = new StringProperty(
            TABLE, "tag");

    /** From user id */
    public static final LongProperty USER_ID = new LongProperty(
            TABLE, USER_ID_PROPERTY_NAME);

    /** From User Object (JSON) */
    public static final StringProperty USER = new StringProperty(
            TABLE, USER_JSON_PROPERTY_NAME);

    /** Action text */
    public static final StringProperty ACTION = new StringProperty(
            TABLE, "action");

    /** Action code */
    public static final StringProperty ACTION_CODE = new StringProperty(
            TABLE, "actionCode");

    /** Message */
    public static final StringProperty MESSAGE = new StringProperty(
            TABLE, "message");

    /** Target Object Name */
    public static final StringProperty TARGET_NAME = new StringProperty(
            TABLE, "targetName");

    /** From User Object (JSON) */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture");

    /** Unixtime Metadata was created */
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Update.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static {
        defaultValues.put(REMOTE_ID.name, 0);
        defaultValues.put(TASK.name, 0);
        defaultValues.put(TAGS.name, "");
        defaultValues.put(USER_ID.name, 0);
        defaultValues.put(USER.name, "");
        defaultValues.put(ACTION.name, "");
        defaultValues.put(ACTION_CODE.name, "");
        defaultValues.put(MESSAGE.name, "");
        defaultValues.put(TARGET_NAME.name, "");
        defaultValues.put(PICTURE.name, "");
    }

    // --- data access boilerplate

    public Update() {
        super();
    }

    public Update(TodorooCursor<Update> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<Update> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<Update> CREATOR = new ModelCreator<Update>(Update.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
