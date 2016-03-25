/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.utility.AndroidUtilities;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import timber.log.Timber;

import static com.todoroo.andlib.data.Property.DoubleProperty;

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
public abstract class AbstractModel implements Parcelable, Cloneable {

    private static final ContentValuesSavingVisitor saver = new ContentValuesSavingVisitor();

    /** id property common to all models */
    protected static final String ID_PROPERTY_NAME = "_id"; //$NON-NLS-1$

    /** id field common to all models */
    public static final LongProperty ID_PROPERTY = new LongProperty(null, ID_PROPERTY_NAME);

    /** sentinel for objects without an id */
    public static final long NO_ID = 0;

    /** prefix for transitories retained in content values */
    public static final String RETAIN_TRANSITORY_PREFIX = "retain-trans-"; //$NON-NLS-1$

    // --- abstract methods

    /** Get the default values for this object */
    abstract public ContentValues getDefaultValues();

    // --- data store variables and management

    /* Data Source Ordering:
     *
     * In order to return the best data, we want to check first what the user
     * has explicitly set (setValues), then the values we have read out of
     * the database (values), then defaults (getDefaultValues)
     */

    /** User set values */
    protected ContentValues setValues = null;

    /** Values from database */
    protected ContentValues values = null;

    /** Transitory Metadata (not saved in database) */
    protected HashMap<String, Object> transitoryData = null;

    public AbstractModel() {
    }

    public AbstractModel(TodorooCursor<? extends AbstractModel> cursor) {
        readPropertiesFromCursor(cursor);
    }

    public AbstractModel(AbstractModel abstractModel) {
        if (abstractModel != null) {
            if (abstractModel.setValues != null) {
                setValues = new ContentValues(abstractModel.setValues);
            }
            if (abstractModel.values != null) {
                values = new ContentValues(abstractModel.values);
            }
        }
    }

    /** Get database-read values for this object */
    public ContentValues getDatabaseValues() {
        return values;
    }

    /** Get the user-set values for this object */
    public ContentValues getSetValues() {
        return setValues;
    }

    /** Get a list of all field/value pairs merged across data sources */
    public ContentValues getMergedValues() {
        ContentValues mergedValues = new ContentValues();

        ContentValues defaultValues = getDefaultValues();
        if(defaultValues != null) {
            mergedValues.putAll(defaultValues);
        }
        if(values != null) {
            mergedValues.putAll(values);
        }
        if(setValues != null) {
            mergedValues.putAll(setValues);
        }

        return mergedValues;
    }

    /**
     * Clear all data on this model
     */
    public void clear() {
        values = null;
        setValues = null;
    }

    /**
     * Transfers all set values into values. This occurs when a task is
     * saved - future saves will not need to write all the data as before.
     */
    public void markSaved() {
        if(values == null) {
            values = setValues;
        } else if(setValues != null) {
            values.putAll(setValues);
        }
        setValues = null;
    }

    /**
     * Use merged values to compare two models to each other. Must be of
     * exactly the same class.
     */
    @Override
    public boolean equals(Object other) {
        if(other == null || other.getClass() != getClass()) {
            return false;
        }

        return getMergedValues().equals(((AbstractModel) other).getMergedValues());
    }

    @Override
    public int hashCode() {
        return getMergedValues().hashCode() ^ getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "\n" + "set values:\n" + setValues + "\n" + "values:\n" + values + "\n";
    }

    @Override
    public AbstractModel clone() {
        AbstractModel clone;
        try {
            clone = (AbstractModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        if(setValues != null) {
            clone.setValues = new ContentValues(setValues);
        }
        if(values != null) {
            clone.values = new ContentValues(values);
        }
        return clone;
    }

    /**
     * Reads all properties from the supplied cursor and store
     */
    void readPropertiesFromCursor(TodorooCursor<? extends AbstractModel> cursor) {
        if (values == null) {
            values = new ContentValues();
        }

        // clears user-set values
        setValues = null;
        transitoryData = null;

        for (Property<?> property : cursor.getProperties()) {
            try {
                saver.save(property, values, cursor.get(property));
            } catch (IllegalArgumentException e) {
                // underlying cursor may have changed, suppress
                Timber.e(e, e.getMessage());
            }
        }
    }

    /**
     * Reads the given property. Make sure this model has this property!
     */
    public synchronized <TYPE> TYPE getValue(Property<TYPE> property) {
        Object value;
        String columnName = property.getColumnName();
        if(setValues != null && setValues.containsKey(columnName)) {
            value = setValues.get(columnName);
        } else if(values != null && values.containsKey(columnName)) {
            value = values.get(columnName);
        } else if(getDefaultValues().containsKey(columnName)) {
            value = getDefaultValues().get(columnName);
        } else {
            throw new UnsupportedOperationException(
                    "Model Error: Did not read property " + property.name); //$NON-NLS-1$
        }

        // resolve properties that were retrieved with a different type than accessed
        try {
            if(value instanceof String && property instanceof LongProperty) {
                return (TYPE) Long.valueOf((String) value);
            } else if(value instanceof String && property instanceof IntegerProperty) {
                return (TYPE) Integer.valueOf((String) value);
            } else if(value instanceof Integer && property instanceof LongProperty) {
                return (TYPE) Long.valueOf(((Number) value).longValue());
            } else if(value instanceof String && property instanceof DoubleProperty) {
                return (TYPE) Double.valueOf((String) value);
            }
            return (TYPE) value;
        } catch (NumberFormatException e) {
            Timber.e(e, e.getMessage());
            return (TYPE) getDefaultValues().get(property.name);
        }
    }

    /**
     * Utility method to get the identifier of the model, if it exists.
     *
     * @return {@value #NO_ID} if this model was not added to the database
     */
    abstract public long getId();

    protected long getIdHelper(LongProperty id) {
        if(setValues != null && setValues.containsKey(id.name)) {
            return setValues.getAsLong(id.name);
        } else if(values != null && values.containsKey(id.name)) {
            return values.getAsLong(id.name);
        } else {
            return NO_ID;
        }
    }

    public void setId(long id) {
        if (setValues == null) {
            setValues = new ContentValues();
        }

        if(id == NO_ID) {
            clearValue(ID_PROPERTY);
        } else {
            setValues.put(ID_PROPERTY_NAME, id);
        }
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
    public boolean containsValue(Property<?> property) {
        if(setValues != null && setValues.containsKey(property.getColumnName())) {
            return true;
        }
        if(values != null && values.containsKey(property.getColumnName())) {
            return true;
        }
        return false;
    }

    /**
     * @return true if setValues or values contains this property, and the value
     *         stored is not null
     */
    public boolean containsNonNullValue(Property<?> property) {
        if(setValues != null && setValues.containsKey(property.getColumnName())) {
            return setValues.get(property.getColumnName()) != null;
        }
        if(values != null && values.containsKey(property.getColumnName())) {
            return values.get(property.getColumnName()) != null;
        }
        return false;
    }

    // --- data storage

    /**
     * Check whether the user has changed this property value and it should be
     * stored for saving in the database
     */
    protected synchronized <TYPE> boolean shouldSaveValue(
            Property<TYPE> property, TYPE newValue) {

    	// we've already decided to save it, so overwrite old value
        if (setValues.containsKey(property.getColumnName())) {
            return true;
        }

        // values contains this key, we should check it out
        if(values != null && values.containsKey(property.getColumnName())) {
            TYPE value = getValue(property);
            if (value == null) {
                if (newValue == null) {
                    return false;
                }
            } else if (value.equals(newValue)) {
                return false;
            }
        }

        // otherwise, good to save
        return true;
    }

    /**
     * Sets the given property. Make sure this model has this property!
     */
    public synchronized <TYPE> void setValue(Property<TYPE> property,
            TYPE value) {
        if (setValues == null) {
            setValues = new ContentValues();
        }
        if (!shouldSaveValue(property, value)) {
            return;
        }

        saver.save(property, setValues, value);
    }

    /**
     * Merges content values with those coming from another source
     */
    public synchronized void mergeWith(ContentValues other) {
        if (setValues == null) {
            setValues = new ContentValues();
        }
        setValues.putAll(other);
    }

    /**
     * Merges set values with those coming from another source,
     * keeping the existing value if one already exists
     */
    public synchronized void mergeWithoutReplacement(ContentValues other) {
        if (setValues == null) {
            setValues = new ContentValues();
        }
        for (Entry<String, Object> item : other.valueSet()) {
            if (setValues.containsKey(item.getKey())) {
                continue;
            }
            AndroidUtilities.putInto(setValues, item.getKey(), item.getValue());
        }
    }

    /**
     * Clear the key for the given property
     */
    public synchronized void clearValue(Property<?> property) {
        if(setValues != null && setValues.containsKey(property.getColumnName())) {
            setValues.remove(property.getColumnName());
        }
        if(values != null && values.containsKey(property.getColumnName())) {
            values.remove(property.getColumnName());
        }
    }

    // --- setting and retrieving flags

    public synchronized void putTransitory(String key, Object value) {
        if(transitoryData == null) {
            transitoryData = new HashMap<>();
        }
        transitoryData.put(key, value);
    }

    public Object getTransitory(String key) {
        if(transitoryData == null) {
            return null;
        }
        return transitoryData.get(key);
    }

    public Object clearTransitory(String key) {
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

    // --- property management

    /**
     * Looks inside the given class and finds all declared properties
     */
    protected static Property<?>[] generateProperties(Class<? extends AbstractModel> cls) {
        ArrayList<Property<?>> properties = new ArrayList<>();
        if(cls.getSuperclass() != AbstractModel.class) {
            properties.addAll(Arrays.asList(generateProperties(
                    (Class<? extends AbstractModel>) cls.getSuperclass())));
        }

        // a property is public, static & extends Property
        for(Field field : cls.getFields()) {
            if((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            if(!Property.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                if(((Property<?>) field.get(null)).table == null) {
                    continue;
                }
                properties.add((Property<?>) field.get(null));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return properties.toArray(new Property<?>[properties.size()]);
    }

    /**
     * Visitor that saves a value into a content values store
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class ContentValuesSavingVisitor implements PropertyVisitor<Void, Object> {

        private ContentValues store;

        public synchronized void save(Property<?> property, ContentValues newStore, Object value) {
            this.store = newStore;

            // we don't allow null values, as they indicate unset properties
            // when the database was written

            if(value != null) {
                property.accept(this, value);
            }
        }

        @Override
        public Void visitInteger(Property<Integer> property, Object value) {
            store.put(property.getColumnName(), (Integer) value);
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, Object value) {
            store.put(property.getColumnName(), (Long) value);
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, Object value) {
            store.put(property.getColumnName(), (Double) value);
            return null;
        }

        @Override
        public Void visitString(Property<String> property, Object value) {
            store.put(property.getColumnName(), (String) value);
            return null;
        }
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(setValues, 0);
        dest.writeParcelable(values, 0);
    }

   /**
    * Parcelable creator helper
    */
    protected static final class ModelCreator<TYPE extends AbstractModel>
            implements Parcelable.Creator<TYPE> {

        private final Class<TYPE> cls;

        public ModelCreator(Class<TYPE> cls) {
            super();
            this.cls = cls;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TYPE createFromParcel(Parcel source) {
            TYPE model;
            try {
                model = cls.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
            model.setValues = source.readParcelable(ContentValues.class.getClassLoader());
            model.values = source.readParcelable(ContentValues.class.getClassLoader());
            return model;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TYPE[] newArray(int size) {
            return (TYPE[]) Array.newInstance(cls, size);
        }
   }

}
