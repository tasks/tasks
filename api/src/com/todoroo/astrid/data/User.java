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
public final class User extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("users", User.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** User Name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** User first name */
    public static final StringProperty FIRST_NAME = new StringProperty(
            TABLE, "first_name");

    /** User last name */
    public static final StringProperty LAST_NAME = new StringProperty(
            TABLE, "last_name");

    /** User Email */
    public static final StringProperty EMAIL = new StringProperty(
            TABLE, "email");

    /** User picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture");

    /** Remote id */
    public static final LongProperty REMOTE_ID = new LongProperty(
            TABLE, REMOTE_ID_PROPERTY_NAME);

    /** UUID */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME);

    /** Friendship status. One of the STATUS constants below */
    public static final StringProperty STATUS = new StringProperty(
            TABLE, "status");

    /** Friendship tatus that needs to be reported to the server.
     * One of the PENDING constants below */
    public static final StringProperty PENDING_STATUS = new StringProperty(
            TABLE, "pendingStatus");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(User.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(NAME.name, "");
        defaultValues.put(EMAIL.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(PUSHED_AT.name, 0L);
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(STATUS.name, "");
        defaultValues.put(PENDING_STATUS.name, "");
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_OTHER_PENDING = "other_pending";
    public static final String STATUS_FRIENDS = "friends";
    public static final String STATUS_IGNORED = "ignored";
    public static final String STATUS_BLOCKED = "blocked";

    public static final String PENDING_REQUEST = "request";
    public static final String PENDING_APPROVE = "approve";
    public static final String PENDING_IGNORE = "ignore";
    public static final String PENDING_UNFRIEND = "unfriend";

    // --- data access boilerplate

    public User() {
        super();
    }

    public User(TodorooCursor<User> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<User> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    public String getDisplayName() {
        String name = getValue(NAME);
        if (!(TextUtils.isEmpty(name) || "null".equals(name)))
            return name;
        return getValue(EMAIL);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
    }

    // --- parcelable helpers

    public static final Creator<User> CREATOR = new ModelCreator<User>(User.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
