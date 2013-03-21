/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;

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

    public static final boolean isValidUuid(String uuid) {
        try {
            long value = Long.parseLong(uuid);
            return value > 0;
        } catch (NumberFormatException e) {
            return isUuidEmpty(uuid);
        }
    }

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

    public void setUuid(String uuid) {
        if (setValues == null)
            setValues = new ContentValues();

        if(NO_UUID.equals(uuid))
            clearValue(UUID_PROPERTY);
        else
            setValues.put(UUID_PROPERTY_NAME, uuid);
    }

    public static boolean isUuidEmpty(String uuid) {
        return NO_UUID.equals(uuid) || TextUtils.isEmpty(uuid);
    }

    public static final String PICTURE_THUMB = "thumb"; //$NON-NLS-1$
    public static final String PICTURE_MEDIUM = "medium"; //$NON-NLS-1$
    public static final String PICTURE_LARGE = "large"; //$NON-NLS-1$

    public String getPictureUrl(StringProperty pictureProperty, String size) {
        String value = getValue(pictureProperty);
        return PictureHelper.getPictureUrl(value, size);
    }

    public Bitmap getPictureBitmap(StringProperty pictureProperty) {
        String value = getValue(pictureProperty);
        return PictureHelper.getPictureBitmap(value);
    }

    public static class PictureHelper {

        public static final String PICTURES_DIRECTORY = "pictures"; //$NON-NLS-1$

        public static String getPictureHash(UserActivity update) {
            return String.format("cached::%s%s", update.getValue(UserActivity.TARGET_ID), update.getValue(UserActivity.CREATED_AT));
        }


        public static String getPictureHash(TagData tagData) {
            long tag_date = 0;
            if (tagData.containsValue(TagData.CREATION_DATE)) {
                tag_date = tagData.getValue(TagData.CREATION_DATE);
            }
            if (tag_date == 0) {
                tag_date = DateUtilities.dateToUnixtime(new Date());
            }
            return String.format("cached::%s%s", tagData.getValue(TagData.NAME), tag_date);
        }

        @SuppressWarnings("nls")
        public static JSONObject savePictureJson(Context context, Bitmap bitmap) {
            try {
                String name = DateUtilities.now() + ".jpg";
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", name);
                jsonObject.put("type", "image/jpeg");

                File dir = context.getExternalFilesDir(PICTURES_DIRECTORY);
                if (dir != null) {
                    File file = new File(dir + File.separator + DateUtilities.now() + ".jpg");
                    if (file.exists())
                        return null;

                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        fos.close();
                        jsonObject.put("path", file.getAbsolutePath());
                    } catch (FileNotFoundException e) {
                        //
                    } catch (IOException e) {
                        //
                    }
                    return jsonObject;
                } else {
                    return null;
                }
            } catch (JSONException e) {
                //
            }
            return null;
        }

        public static String getPictureUrl(String value, String size) {
            try {
                if (value == null)
                    return null;
                JSONObject pictureJson = new JSONObject(value);
                if (pictureJson.has("path")) // Unpushed encoded bitmap //$NON-NLS-1$
                    return null;
                return pictureJson.optString(size);
            } catch (JSONException e) {
                return value;
            }
        }

        @SuppressWarnings("nls")
        public static Bitmap getPictureBitmap(String value) {
            try {
                if (value == null)
                    return null;
                if (value.contains("path")) {
                    JSONObject pictureJson = new JSONObject(value);
                    if (pictureJson.has("path")) {
                        String path = pictureJson.getString("path");
                        return BitmapFactory.decodeFile(path);
                    }
                }
                return null;
            } catch (JSONException e) {
                return null;
            }

        }

        public static String getPictureUrlFromCursor(TodorooCursor<?> cursor, StringProperty pictureProperty, String size) {
            String value = cursor.get(pictureProperty);
            return getPictureUrl(value, size);
        }
    }
}
