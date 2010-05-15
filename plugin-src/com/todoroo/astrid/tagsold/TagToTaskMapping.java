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

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class TagToTaskMapping extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("tagTaskMap", TagToTaskMapping.class);

    // --- properties

    /** Tag */
    public static final LongProperty TAG = new LongProperty(
            TABLE, "tag");

    /** Task */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TagToTaskMapping.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public TagToTaskMapping() {
        super();
    }

    public TagToTaskMapping(TodorooCursor<TagToTaskMapping> cursor, Property<?>[] properties) {
        this();
        readPropertiesFromCursor(cursor, properties);
    }

    public void readFromCursor(TodorooCursor<TagToTaskMapping> cursor, Property<?>[] properties) {
        super.readPropertiesFromCursor(cursor, properties);
    }

    @Override
    public long getId() {
        return NO_ID;
    }

}