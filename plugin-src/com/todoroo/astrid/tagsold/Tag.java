/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.tagsold;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Tag extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("tags", Tag.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name of Task */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Tag.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(NAME.name, "");
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public Tag() {
        super();
    }

    public Tag(TodorooCursor<Tag> cursor, Property<?>[] properties) {
        this();
        readPropertiesFromCursor(cursor, properties);
    }

    public void readFromCursor(TodorooCursor<Tag> cursor, Property<?>[] properties) {
        super.readPropertiesFromCursor(cursor, properties);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

}