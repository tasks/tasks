/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data;

import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;

/** A data object backed by a database */
@SuppressWarnings("nls")
public abstract class LegacyAbstractModel {

    /* Data Source Ordering:
     *
     * In order to return the best data, we want to check first what the user
     * has explicitly set (setValues), then the values we have read out of
     * the database (values), the database itself (cursor), then defaults
     * (getDefaultValues)
     */

    /** User set values */
    protected ContentValues setValues = new ContentValues();

    /** Cached values from database */
    private final ContentValues values = new ContentValues();

    /** Cursor into the database */
    private Cursor cursor = null;

    // --- constructors

    /** Construct a model from scratch */
    public LegacyAbstractModel() {
        // ...
    }

    /** Construct a model from a database object */
    public LegacyAbstractModel(Cursor cursor) {
        this.cursor = cursor;
    }

    // --- data source getters

    /** Get the user-set values for this object */
    public ContentValues getSetValues() {
        return setValues;
    }

    /** Get the default values for this object */
    abstract public ContentValues getDefaultValues();

    /** Get a list of all field/value pairs merged across data sources */
    public ContentValues getMergedValues() {
        ContentValues mergedValues = new ContentValues();

        mergedValues.putAll(getDefaultValues());
        mergedValues.putAll(values);
        mergedValues.putAll(setValues);

        return mergedValues;
    }

    /** Return the database cursor */
    public Cursor getCursor() {
        return cursor;
    }

    // --- checking against cached values

    protected void putIfChangedFromDatabase(String field, String newValue) {
        if(!setValues.containsKey(field) && values.containsKey(field)) {
            String value = values.getAsString(field);
            if(value == null) {
                if(newValue == null)
                    return;
            } else if(value.equals(newValue))
                return;
        }
        setValues.put(field, newValue);
    }

    protected void putIfChangedFromDatabase(String field, Long newValue) {
        if(!setValues.containsKey(field) && values.containsKey(field)) {
            Long value = values.getAsLong(field);
            if(value == null) {
                if(newValue == null)
                    return;
            } else if(value.equals(newValue))
                return;
        }
        setValues.put(field, newValue);
    }

    protected void putIfChangedFromDatabase(String field, Integer newValue) {
        if(!setValues.containsKey(field) && values.containsKey(field)) {
            Integer value = values.getAsInteger(field);
            if(value == null) {
                if(newValue == null)
                    return;
            } else if(value.equals(newValue))
                return;
        }
        setValues.put(field, newValue);
    }

    protected void putIfChangedFromDatabase(String field, Double newValue) {
        if(!setValues.containsKey(field) && values.containsKey(field)) {
            Double value = values.getAsDouble(field);
            if(value == null) {
                if(newValue == null)
                    return;
            } else if(value.equals(newValue))
                return;
        }
        setValues.put(field, newValue);
    }

    protected static final HashMap<Class<?>, HashMap<String, Integer>>
        columnIndexCache = new HashMap<Class<?>, HashMap<String, Integer>>();
    private int getColumnIndex(String field) {
        HashMap<String, Integer> classCache;
        classCache = columnIndexCache.get(getClass());
        if(classCache == null) {
            classCache = new HashMap<String, Integer>();
            columnIndexCache.put(getClass(), classCache);
        }

        Integer index = classCache.get(field);
        if(index == null) {
            index = cursor.getColumnIndexOrThrow(field);
            classCache.put(field, index);
        }

        return index;
    }

    // --- data retrieval for the different object types

    protected String retrieveString(String field) {
        if(setValues.containsKey(field))
            return setValues.getAsString(field);

        if(values.containsKey(field))
            return values.getAsString(field);

        // if we have a database to hit, do that now
        if(cursor != null) {
            String value = cursor.getString(getColumnIndex(field));
            values.put(field, value);
            return value;
        }

        // do we have defaults?
        ContentValues defaults = getDefaultValues();
        if(defaults != null && defaults.containsKey(field))
            return defaults.getAsString(field);

        throw new UnsupportedOperationException("Could not read field " + field);
    }

    protected Integer retrieveInteger(String field) {
        if(setValues.containsKey(field))
            return setValues.getAsInteger(field);

        if(values.containsKey(field))
            return values.getAsInteger(field);

        // if we have a database to hit, do that now
        if(cursor != null) {
            try {
                Integer value = cursor.getInt(getColumnIndex(field));
                values.put(field, value);
                return value;
            } catch (Exception e) {
                // error reading from cursor, try to continue
            }
        }

        // do we have defaults?
        ContentValues defaults = getDefaultValues();
        if(defaults != null && defaults.containsKey(field))
            return defaults.getAsInteger(field);

        throw new UnsupportedOperationException("Could not read field " + field);
    }

    protected Long retrieveLong(String field) {
        if(setValues.containsKey(field))
            return setValues.getAsLong(field);

        if(values.containsKey(field))
            return values.getAsLong(field);

        // if we have a database to hit, do that now
        if(cursor != null) {
            Long value = cursor.getLong(getColumnIndex(field));
            values.put(field, value);
            return value;
        }

        // do we have defaults?
        ContentValues defaults = getDefaultValues();
        if(defaults != null && defaults.containsKey(field))
            return defaults.getAsLong(field);

        throw new UnsupportedOperationException("Could not read field " + field);
    }

    protected Double retrieveDouble(String field) {
        if(setValues.containsKey(field))
            return setValues.getAsDouble(field);

        if(values.containsKey(field))
            return values.getAsDouble(field);

        // if we have a database to hit, do that now
        if(cursor != null) {
            Double value = cursor.getDouble(getColumnIndex(field));
            values.put(field, value);
            return value;
        }

        // do we have defaults?
        ContentValues defaults = getDefaultValues();
        if(defaults != null && defaults.containsKey(field))
            return defaults.getAsDouble(field);

        throw new UnsupportedOperationException("Could not read field " + field);
    }

    // --- retrieving composite objects

    protected Date retrieveDate(String field) {
        Long time;
        try {
            time = retrieveLong(field);
            if(time == null || time == 0)
                return null;
        } catch (NullPointerException e) {
            return null;
        }

        return new Date(time);
    }
}
