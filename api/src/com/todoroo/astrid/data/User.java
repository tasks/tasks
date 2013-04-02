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
    @Deprecated public static final StringProperty EMAIL = new StringProperty(
            TABLE, "email");

    /** User picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture", Property.PROP_FLAG_JSON);

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME);

    /** Pushed at date */
    public static final LongProperty TASKS_PUSHED_AT = new LongProperty(
            TABLE, "tasks_pushed_at");

    /** Friendship status. One of the STATUS constants below */
    public static final StringProperty STATUS = new StringProperty(
            TABLE, "status");

    /** Last autosync */
    public static final LongProperty LAST_AUTOSYNC = new LongProperty(
            TABLE, "lastAutosync");

    /** Friendship tatus that needs to be reported to the server.
     * One of the PENDING constants below */
    @Deprecated public static final StringProperty PENDING_STATUS = new StringProperty(
            TABLE, "pendingStatus");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(User.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(NAME.name, "");
        defaultValues.put(FIRST_NAME.name, "");
        defaultValues.put(LAST_NAME.name, "");
        defaultValues.put(EMAIL.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(PUSHED_AT.name, 0L);
        defaultValues.put(TASKS_PUSHED_AT.name, 0L);
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(STATUS.name, "");
        defaultValues.put(LAST_AUTOSYNC.name, 0L);
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

    public static final String STATUS_REQUEST = "request";
    public static final String STATUS_CONFIRM = "confirm";
    public static final String STATUS_IGNORE = "ignore";
    public static final String STATUS_RENOUNCE = "renounce";

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
        return getDisplayName(NAME, FIRST_NAME, LAST_NAME);
    }

    private String getCheckedString(StringProperty stringProperty) {
        return containsNonNullValue(stringProperty) ? getValue(stringProperty) : null;
    }

    public String getDisplayName(StringProperty nameProperty, StringProperty firstNameProperty, StringProperty lastNameProperty) {
        String name = getCheckedString(nameProperty);
        if (!(TextUtils.isEmpty(name) || "null".equals(name)))
            return name;
        String firstName = getCheckedString(firstNameProperty);
        boolean firstNameEmpty = TextUtils.isEmpty(firstName) || "null".equals(firstName);
        String lastName = getCheckedString(lastNameProperty);
        boolean lastNameEmpty = TextUtils.isEmpty(lastName) || "null".equals(lastName);
        if (firstNameEmpty && lastNameEmpty)
            return getCheckedString(EMAIL);
        StringBuilder nameBuilder = new StringBuilder();
        if (!firstNameEmpty)
            nameBuilder.append(firstName).append(" ");
        if (!lastNameEmpty)
            nameBuilder.append(lastName);
        return nameBuilder.toString().trim();
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
