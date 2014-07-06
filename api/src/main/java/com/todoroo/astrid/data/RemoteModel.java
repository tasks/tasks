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
import com.todoroo.andlib.data.Property.StringProperty;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A model that is synchronized to a remote server and has a remote id
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class RemoteModel extends AbstractModel {

    private static final Logger log = LoggerFactory.getLogger(RemoteModel.class);

    /** remote id property common to all remote models */
    public static final String UUID_PROPERTY_NAME = "remoteId"; //$NON-NLS-1$

    /** remote id property */
    public static final StringProperty UUID_PROPERTY = new StringProperty(null, UUID_PROPERTY_NAME);

    /** pushed at date property name */
    public static final String PUSHED_AT_PROPERTY_NAME = "pushedAt"; //$NON-NLS-1$

    /** constant value for no uuid */
    public static final String NO_UUID = "0"; //$NON-NLS-1$

    public static boolean isValidUuid(String uuid) {
        try {
            long value = Long.parseLong(uuid);
            return value > 0;
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return isUuidEmpty(uuid);
        }
    }

    protected String getUuidHelper(StringProperty uuid) {
        if(setValues != null && setValues.containsKey(uuid.name)) {
            return setValues.getAsString(uuid.name);
        } else if(values != null && values.containsKey(uuid.name)) {
            return values.getAsString(uuid.name);
        } else {
            return NO_UUID;
        }
    }

    public void setUuid(String uuid) {
        if (setValues == null) {
            setValues = new ContentValues();
        }

        if(NO_UUID.equals(uuid)) {
            clearValue(UUID_PROPERTY);
        } else {
            setValues.put(UUID_PROPERTY_NAME, uuid);
        }
    }

    public static boolean isUuidEmpty(String uuid) {
        return NO_UUID.equals(uuid) || TextUtils.isEmpty(uuid);
    }

    public static class PictureHelper {

        public static JSONObject savePictureJson(final Uri uri) {
            try {
                return new JSONObject() {{
                    put("uri", uri.toString());
                }};
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }

        public static Uri getPictureUri(String value) {
            try {
                if (value == null) {
                    return null;
                }
                if (value.contains("uri") || value.contains("path")) {
                    JSONObject json = new JSONObject(value);
                    if (json.has("uri")) {
                        return Uri.parse(json.getString("uri"));
                    }
                    if (json.has("path")) {
                        String path = json.getString("path");
                        return Uri.fromFile(new File(path));
                    }
                }
                return null;
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    public String getUuidProperty() {
        return getValue(UUID_PROPERTY);
    }

    public void setUuidProperty(String uuidProperty) {
        setValue(UUID_PROPERTY, uuidProperty);
    }
}
