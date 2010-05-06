/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.model;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridContentProvider.AstridTask;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Task extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("tasks", Task.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, AstridTask.ID);

    /** Name of Task */
    public static final StringProperty TITLE = new StringProperty(
            TABLE, AstridTask.TITLE);

    /** Urgency of Task (see urgency flags) */
    public static final IntegerProperty URGENCY = new IntegerProperty(
            TABLE, AstridTask.URGENCY);

    /** Importance of Task (see importance flags) */
    public static final IntegerProperty IMPORTANCE = new IntegerProperty(
            TABLE, AstridTask.IMPORTANCE);

    /** Unixtime Task is due, 0 if not set */
    public static final IntegerProperty DUE_DATE = new IntegerProperty(
            TABLE, AstridTask.DUE_DATE);

    /** Unixtime Task should be hidden until */
    public static final IntegerProperty HIDDEN_UNTIL = new IntegerProperty(
            TABLE, AstridTask.HIDDEN_UNTIL);

    /** Unixtime Task was created */
    public static final IntegerProperty CREATION_DATE = new IntegerProperty(
            TABLE, AstridTask.CREATION_DATE);

    /** Unixtime Task was last touched */
    public static final IntegerProperty MODIFICATION_DATE = new IntegerProperty(
            TABLE, AstridTask.MODIFICATION_DATE);

    /** Unixtime Task was completed. 0 means active */
    public static final IntegerProperty COMPLETION_DATE = new IntegerProperty(
            TABLE, AstridTask.COMPLETION_DATE);

    /** Unixtime Task was deleted. 0 means active */
    public static final IntegerProperty DELETION_DATE = new IntegerProperty(
            TABLE, AstridTask.DELETION_DATE);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = new Property<?>[] {
        ID,
        TITLE,
        URGENCY,
        IMPORTANCE,
        DUE_DATE,
        HIDDEN_UNTIL,
        CREATION_DATE,
        COMPLETION_DATE,
        DELETION_DATE
    };

    static {
        TABLE.setProperties(PROPERTIES);
    }

    // --- urgency flags

    public static final int URGENCY_NONE = AstridTask.URGENCY_NONE;
    public static final int URGENCY_TODAY = AstridTask.URGENCY_TODAY;
    public static final int URGENCY_THIS_WEEK = AstridTask.URGENCY_THIS_WEEK;
    public static final int URGENCY_THIS_MONTH = AstridTask.URGENCY_THIS_MONTH;
    public static final int URGENCY_WITHIN_THREE_MONTHS = AstridTask.URGENCY_WITHIN_THREE_MONTHS;
    public static final int URGENCY_WITHIN_SIX_MONTHS = AstridTask.URGENCY_WITHIN_SIX_MONTHS;
    public static final int URGENCY_WITHIN_A_YEAR = AstridTask.URGENCY_WITHIN_A_YEAR;
    public static final int URGENCY_SPECIFIC_DAY = AstridTask.URGENCY_SPECIFIC_DAY;
    public static final int URGENCY_SPECIFIC_DAY_TIME = AstridTask.URGENCY_SPECIFIC_DAY_TIME;

    // --- importance flags

    public static final int IMPORTANCE_DO_OR_DIE = AstridTask.IMPORTANCE_DO_OR_DIE;
    public static final int IMPORTANCE_MUST_DO = AstridTask.IMPORTANCE_MUST_DO;
    public static final int IMPORTANCE_SHOULD_DO = AstridTask.IMPORTANCE_SHOULD_DO;
    public static final int IMPORTANCE_NONE = AstridTask.IMPORTANCE_NONE;

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(TITLE.name, "");
        defaultValues.put(DUE_DATE.name, 0);
        defaultValues.put(HIDDEN_UNTIL.name, 0);
        defaultValues.put(COMPLETION_DATE.name, 0);
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(URGENCY.name, URGENCY_NONE);
        defaultValues.put(IMPORTANCE.name, IMPORTANCE_NONE);
    }

    private static boolean defaultValuesLoaded = false;

    public static ContentValues getStaticDefaultValues() {
        return defaultValues;
    }

    /**
     * Call to load task default values from preferences.
     */
    public static void refreshDefaultValues() {
        /*defaultValues.put(URGENCY.name,
                Preferences.getIntegerFromString(R.string.EPr_default_urgency_key));
        defaultValues.put(IMPORTANCE.name,
                Preferences.getIntegerFromString(R.string.EPr_default_importance_key));*/
        defaultValuesLoaded = true;
    }

    @Override
    public ContentValues getDefaultValues() {
        // if refreshDefaultValues has never been called, call it
        if(!defaultValuesLoaded) {
            refreshDefaultValues();
        }

        return defaultValues;
    }

    // --- data access boilerplate

    public Task() {
        super();
    }

    public Task(TodorooCursor<Task> cursor, Property<?>[] properties) {
        this();
        readPropertiesFromCursor(cursor, properties);
    }

    public void readFromCursor(TodorooCursor<Task> cursor, Property<?>[] properties) {
        super.readPropertiesFromCursor(cursor, properties);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- data access methods

    /** Checks whether task is done. Requires COMPLETION_DATE */
    public boolean isCompleted() {
        return getValue(COMPLETION_DATE) > 0;
    }

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        try {
            return getValue(DELETION_DATE) > 0;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    /** Checks whether task is hidden. Requires HIDDEN_UNTIL */
    public boolean isHidden() {
    	return getValue(HIDDEN_UNTIL) > DateUtilities.now();
    }

    /** Checks whether task is done. Requires DUE_DATE */
    public boolean hasDueDate() {
        return getValue(DUE_DATE) > 0;
    }

}