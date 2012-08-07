/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.data.Task;

/**
 * Data Model which represents an alarm. This is a transitional class -
 * Alarms are moved over to metadata
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class TransitionalAlarm extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("alarm", TransitionalAlarm.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Associated Task */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** Alarm Time */
    public static final LongProperty TIME = new LongProperty(
            TABLE, "time");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TransitionalAlarm.class);

    // --- constants

    /** this alarm was already triggered */
    public static final int TYPE_TRIGGERED = 0;

    /** this alarm is single-shot */
    public static final int TYPE_SINGLE = 1;

    /** this alarm repeats itself until turned off */
    public static final int TYPE_REPEATING = 2;

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        //
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    @Deprecated
    public TransitionalAlarm() {
        super();
    }

    public TransitionalAlarm(TodorooCursor<TransitionalAlarm> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<TransitionalAlarm> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
