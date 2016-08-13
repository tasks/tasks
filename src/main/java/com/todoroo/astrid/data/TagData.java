/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;

/**
 * Data Model which represents a collaboration space for users / tasks.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagData extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tagdata", TagData.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote goal id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Name of Tag */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    public static final IntegerProperty COLOR = new IntegerProperty(
            TABLE, "color");

    /** Unixtime Project was deleted. 0 means not deleted */
    @Deprecated
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted", Property.PROP_FLAG_DATE);

    /** Tag ordering */
    @Deprecated
    public static final StringProperty TAG_ORDERING = new StringProperty(
            TABLE, "tagOrdering");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TagData.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(NAME.name, "");
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(TAG_ORDERING.name, "[]");
        defaultValues.put(COLOR.name, -1);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public String getUuid() {
        return getUuidHelper(UUID);
    }

    // --- parcelable helpers

    public static final Creator<TagData> CREATOR = new ModelCreator<>(TagData.class);

    // --- data access methods

    public String getName() {
        return getValue(NAME);
    }

    public void setName(String name) {
        setValue(NAME, name);
    }

    public String getTagOrdering() {
        return getValue(TAG_ORDERING);
    }

    public void setColor(int color) {
        setValue(COLOR, color);
    }

    public int getColor() {
        return getValue(COLOR);
    }

    // TODO: remove?
    public String getUUID() {
        return getValue(UUID);
    }

    public void setUUID(String uuid) {
        setValue(UUID, uuid);
    }
}
