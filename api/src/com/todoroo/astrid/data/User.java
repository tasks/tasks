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
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** User Name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** User Email */
    public static final StringProperty EMAIL = new StringProperty(
            TABLE, "email");

    /** User picture */
    public static final StringProperty PICTURE = new StringProperty(
            TABLE, "picture");

    /** User picture thumbnail */
    public static final StringProperty THUMB = new StringProperty(
            TABLE, "thumb");

    /** User last activity string */
    public static final StringProperty LAST_ACTIVITY = new StringProperty(
            TABLE, "lastActivity");

    /** User last activity date */
    public static final LongProperty LAST_ACTIVITY_DATE = new LongProperty(
            TABLE, "lastActivityDate");

    /** Remote id */
    public static final LongProperty REMOTE_ID = new LongProperty(
            TABLE, REMOTE_ID_PROPERTY_NAME);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(User.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(NAME.name, "");
        defaultValues.put(EMAIL.name, "");
        defaultValues.put(PICTURE.name, "");
        defaultValues.put(THUMB.name, "");
        defaultValues.put(LAST_ACTIVITY.name, "");
        defaultValues.put(LAST_ACTIVITY_DATE.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

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

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<User> CREATOR = new ModelCreator<User>(User.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
