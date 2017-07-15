/**
 * Copyright (c) 2012 Todoroo Inc
 *
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

import org.tasks.BuildConfig;

/**
 * Data Model which represents a piece of data unrelated to a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class StoreObject extends AbstractModel {

    // --- table

    /** table for this model */
    public static final Table TABLE = new Table("store", StoreObject.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Store Type Key */
    public static final StringProperty TYPE = new StringProperty(
            TABLE, "type");

    /** Store Item Key */
    public static final StringProperty ITEM= new StringProperty(
            TABLE, "item");

    /** Store Value Column 1 */
    public static final StringProperty VALUE1 = new StringProperty(
            TABLE, "value");

    /** Store Value Column 2 */
    public static final StringProperty VALUE2 = new StringProperty(
            TABLE, "value2");

    /** Store Value Column 3 */
    public static final StringProperty VALUE3 = new StringProperty(
            TABLE, "value3");

    /** Store Value Column 3 */
    public static final StringProperty VALUE4 = new StringProperty(
            TABLE, "value4");

    /** Unixtime Task was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted", Property.PROP_FLAG_DATE);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(StoreObject.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(DELETION_DATE.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<StoreObject> CREATOR = new ModelCreator<>(StoreObject.class);

    public String getType() {
        return getValue(TYPE);
    }

    public void setType(String type) {
        setValue(TYPE, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreObject)) return false;

        StoreObject that = (StoreObject) o;

        return getMergedValues().equals(that.getMergedValues());
    }
}
