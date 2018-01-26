/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.arch.persistence.room.Ignore;
import android.content.ContentValues;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Task;

import org.tasks.data.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * <code>AbstractModel</code> represents a row in a database.
 * <p>
 * A single database can be represented by multiple <code>AbstractModel</code>s
 * corresponding to different queries that return a different set of columns.
 * Each model exposes a set of properties that it contains.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public abstract class AbstractModel {

    /** id property common to all models */
    protected static final String ID_PROPERTY_NAME = "_id"; //$NON-NLS-1$

    /** id field common to all models */
    public static final LongProperty ID_PROPERTY = new LongProperty(null, ID_PROPERTY_NAME);

    /** sentinel for objects without an id */
    public static final long NO_ID = 0;

    // --- abstract methods

    public interface ValueWriter<TYPE> {
        void setValue(Task instance, TYPE value);
    }

    // --- data store variables and management

    /* Data Source Ordering:
     *
     * In order to return the best data, we want to check first what the user
     * has explicitly set (setValues), then the values we have read out of
     * the database (values), then defaults (getRoomGetters)
     */

    /** User set values */
    @Ignore
    protected final Set<String> setValues = new HashSet<>();

    /** Transitory Metadata (not saved in database) */
    @Ignore
    protected HashMap<String, Object> transitoryData = null;

    /** Get the user-set values for this object */
    public Set<String> getSetValues() {
        return newHashSet(setValues);
    }

    /**
     * Transfers all set values into values. This occurs when a task is
     * saved - future saves will not need to write all the data as before.
     */
    public void markSaved() {
        setValues.clear();
    }

    private void setValue(String columnName, Object value) {
        ValueWriter<Object> writer = Task.roomSetters.get(columnName);
        if (writer == null) {
            throw new RuntimeException();
        }
        writer.setValue((Task) this, value);
        setValues.add(columnName);
    }


    /**
     * Utility method to get the identifier of the model, if it exists.
     *
     * @return {@value #NO_ID} if this model was not added to the database
     */
    abstract public long getId();

    public void setId(long id) {
        setValue(ID_PROPERTY, id);
    }

    /**
     * @return true if this model has found Jesus (i.e. the database)
     */
    public boolean isSaved() {
        return getId() != NO_ID;
    }

    /**
     * @return true if setValues or values contains this property
     */
    public boolean isModified(Property<?> property) {
        return setValues.contains(property.getColumnName());
    }

    // --- data storage

    /**
     * Sets the given property. Make sure this model has this property!
     */
    public synchronized <TYPE> void setValue(Property<TYPE> property, TYPE value) {
        setValue(property.getColumnName(), value);
    }

    /**
     * Merges set values with those coming from another source,
     * keeping the existing value if one already exists
     */
    public synchronized void mergeWithoutReplacement(ContentValues other) {
        for (Entry<String, Object> item : other.valueSet()) {
            String columnName = item.getKey();
            if (setValues.add(columnName)) {
                setValue(columnName, item.getValue());
            }
        }
    }

    // --- setting and retrieving flags

    public synchronized void putTransitory(String key, Object value) {
        if(transitoryData == null) {
            transitoryData = new HashMap<>();
        }
        transitoryData.put(key, value);
    }

    public void setTags(ArrayList<String> tags) {
        if (transitoryData == null) {
            transitoryData = new HashMap<>();
        }
        transitoryData.put(Tag.KEY, tags);
    }

    public ArrayList<String> getTags() {
        Object tags = getTransitory(Tag.KEY);
        return tags == null ? new ArrayList<>() : (ArrayList<String>) tags;
    }

    public <T> T getTransitory(String key) {
        if(transitoryData == null) {
            return null;
        }
        return (T) transitoryData.get(key);
    }

    private Object clearTransitory(String key) {
        if (transitoryData == null) {
            return null;
        }
        return transitoryData.remove(key);
    }

    // --- Convenience wrappers for using transitories as flags
    public boolean checkTransitory(String flag) {
        Object trans = getTransitory(flag);
        return trans != null;
    }

    public boolean checkAndClearTransitory(String flag) {
        Object trans = clearTransitory(flag);
        return trans != null;
    }
}
