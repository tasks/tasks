/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;

/**
 * A model that is synchronized to a remote server and has a remote id
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class RemoteModel extends AbstractModel {

    /** remote id property common to all remote models */
    public static final String UUID_PROPERTY_NAME = "remoteId"; //$NON-NLS-1$

    /** remote id property */
    public static final StringProperty UUID_PROPERTY = new StringProperty(null, UUID_PROPERTY_NAME);

    /** user id property common to all remote models */
    protected static final String USER_ID_PROPERTY_NAME = "userId"; //$NON-NLS-1$

    /** user id property */
    public static final StringProperty USER_ID_PROPERTY = new StringProperty(null, USER_ID_PROPERTY_NAME);

    /** user json property common to all remote models */
    protected static final String USER_JSON_PROPERTY_NAME = "user"; //$NON-NLS-1$

    /** user json property */
    @Deprecated public static final StringProperty USER_JSON_PROPERTY = new StringProperty(null, USER_JSON_PROPERTY_NAME);

    /** pushed at date property name */
    public static final String PUSHED_AT_PROPERTY_NAME = "pushedAt"; //$NON-NLS-1$

    /** pushed at date property name */
    public static final LongProperty PUSHED_AT_PROPERTY = new LongProperty(null, PUSHED_AT_PROPERTY_NAME);

    /** constant value for no uuid */
    public static final String NO_UUID = "0"; //$NON-NLS-1$

    /**
     * Utility method to get the identifier of the model, if it exists.
     *
     * @return {@value #NO_UUID} if this model was not added to the database
     */
    abstract public String getUuid();

    protected String getUuidHelper(StringProperty uuid) {
        if(setValues != null && setValues.containsKey(uuid.name))
            return setValues.getAsString(uuid.name);
        else if(values != null && values.containsKey(uuid.name))
            return values.getAsString(uuid.name);
        else
            return NO_UUID;
    }

    public static boolean isUuidEmpty(String uuid) {
        return NO_UUID.equals(uuid) || TextUtils.isEmpty(uuid);
    }

    public static final String PICTURE_THUMB = "thumb"; //$NON-NLS-1$
    public static final String PICTURE_MEDIUM = "medium"; //$NON-NLS-1$
    public static final String PICTURE_LARGE = "large"; //$NON-NLS-1$

    public String getPictureUrl(StringProperty pictureProperty, String size) {
        String value = getValue(pictureProperty);
        try {
            JSONObject pictureJson = new JSONObject(value);
            return pictureJson.optString(size);
        } catch (JSONException e) {
            return value;
        }
    }

    public static String getPictureUrlFromCursor(TodorooCursor<?> cursor, StringProperty pictureProperty, String size) {
        String value = cursor.get(pictureProperty);
        try {
            JSONObject pictureJson = new JSONObject(value);
            return pictureJson.optString(size);
        } catch (JSONException e) {
            return value;
        }
    }
}
