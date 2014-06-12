package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

import java.io.File;

public class UserActivity extends RemoteModel {

 // --- table

    /** table for this model */
    public static final Table TABLE = new Table("userActivity", UserActivity.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote ID */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** User ID (activity initiator) */
    public static final StringProperty USER_UUID = new StringProperty(
            TABLE, "user_uuid", Property.PROP_FLAG_USER_ID);

    /** Action */
    public static final StringProperty ACTION = new StringProperty(
            TABLE, "action");

    /** Message */
    public static final StringProperty MESSAGE = new StringProperty(
            TABLE, "message");

    /** Picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture", Property.PROP_FLAG_JSON | Property.PROP_FLAG_PICTURE);

    /** Target id */
    public static final StringProperty TARGET_ID = new StringProperty(
            TABLE, "target_id");

    /** Target name */
    public static final StringProperty TARGET_NAME = new StringProperty(
            TABLE, "target_name");

    /** Created at */
    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, "created_at", Property.PROP_FLAG_DATE);

    /** Deleted at */
    public static final LongProperty DELETED_AT = new LongProperty(
            TABLE, "deleted_at", Property.PROP_FLAG_DATE);

    public UserActivity() {
        super();
    }

    // --- Action codes
    public static final String ACTION_TASK_COMMENT = "task_comment";
    public static final String ACTION_TAG_COMMENT = "tag_comment";

    public UserActivity(TodorooCursor<UserActivity> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    // --- helpers

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(USER_UUID.name, NO_UUID);
        defaultValues.put(ACTION.name, "");
        defaultValues.put(MESSAGE.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(TARGET_ID.name, NO_UUID);
        defaultValues.put(TARGET_NAME.name, "");
        defaultValues.put(CREATED_AT.name, 0L);
        defaultValues.put(DELETED_AT.name, 0L);
    }

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(UserActivity.class);

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    private static final Creator<UserActivity> CREATOR = new ModelCreator<>(UserActivity.class);

    public Long getCreatedAt() {
        return getValue(CREATED_AT);
    }

    public void setCreatedAt(Long createdAt) {
        setValue(CREATED_AT, createdAt);
    }

    public String getMessage() {
        return getValue(MESSAGE);
    }

    public void setMessage(String message) {
        setValue(MESSAGE, message);
    }

    public void setUserUUID(String userUuid) {
        setValue(USER_UUID, userUuid);
    }

    public void setTargetName(String targetName) {
        setValue(TARGET_NAME, targetName);
    }

    public void setTargetId(String targetId) {
        setValue(TARGET_ID, targetId);
    }

    public void setAction(String action) {
        setValue(ACTION, action);
    }

    public void setUUID(String uuid) {
        setValue(UUID, uuid);
    }

    public void setPicture(String picture) {
        setValue(PICTURE, picture);
    }

    public Uri getPictureUri() {
        return PictureHelper.getPictureUri(getValue(PICTURE));
    }
}
