package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.api.AstridApiConstants;

@SuppressWarnings("nls")
public class UserActivity extends RemoteModel {

 // --- table

    /** table for this model */
    public static final Table TABLE = new Table("userActivity", UserActivity.class);

    /** model class for entries in the outstanding table */
    public static final Class<? extends OutstandingEntry<UserActivity>> OUTSTANDING_MODEL = UserActivityOutstanding.class;

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

    /** Pushed at */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, "pushed_at", Property.PROP_FLAG_DATE);

    /** User ID (activity initiator) */
    public static final StringProperty USER_UUID = new StringProperty(
            TABLE, "user_uuid");

    /** Action */
    public static final StringProperty ACTION = new StringProperty(
            TABLE, "action");

    /** Message */
    public static final StringProperty MESSAGE = new StringProperty(
            TABLE, "message");

    /** Picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture");

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
            TABLE, "created_at", Property.PROP_FLAG_DATE);


    // --- helpers

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
    }

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

    private static final Creator<UserActivity> CREATOR = new ModelCreator<UserActivity>(UserActivity.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
