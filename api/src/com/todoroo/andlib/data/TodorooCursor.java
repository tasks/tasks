/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import java.util.WeakHashMap;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.todoroo.andlib.data.Property.PropertyVisitor;

/**
 * AstridCursor wraps a cursor and allows users to query for individual
 * {@link Property} types or read an entire {@link AbstractModel} from
 * a database row.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 * @param <TYPE> a model type that is returned by this cursor
 */
public class TodorooCursor<TYPE extends AbstractModel> extends CursorWrapper {

    /** Properties read by this cursor */
    private final Property<?>[] properties;

    /** Weakly cache field name to column id references for this cursor.
     * Because it's a weak hash map, entire keys can be discarded by GC */
    private final WeakHashMap<String, Integer> columnIndexCache;

    /** Property reading visitor */
    private static final CursorReadingVisitor reader = new CursorReadingVisitor();

    /** Wrapped cursor */
    private final Cursor cursor;

    /**
     * Create an <code>AstridCursor</code> from the supplied {@link Cursor}
     * object.
     *
     * @param cursor
     * @param properties properties read from this cursor
     */
    public TodorooCursor(Cursor cursor, Property<?>[] properties) {
        super(cursor);

        this.cursor = cursor;
        this.properties = properties;
        columnIndexCache = new WeakHashMap<String, Integer>();
    }

    /**
     * Get the value for the given property on the underlying {@link Cursor}
     *
     * @param <PROPERTY_TYPE> type to return
     * @param property to retrieve
     * @return
     */
    public <PROPERTY_TYPE> PROPERTY_TYPE get(Property<PROPERTY_TYPE> property) {
        return (PROPERTY_TYPE)property.accept(reader, this);
    }

    /**
     * @return underlying cursor
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Gets entire property list
     * @return
     */
    public Property<?>[] getProperties() {
        return properties;
    }

    /**
     * Use cache to get the column index for the given field name
     */
    public synchronized int getColumnIndexFromCache(String field) {
        Integer index = columnIndexCache.get(field);
        if(index == null) {
            index = getColumnIndexOrThrow(field);
            columnIndexCache.put(field, index);
        }

        return index;
    }

    /**
     * Visitor that reads the given property from a cursor
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class CursorReadingVisitor implements PropertyVisitor<Object, TodorooCursor<?>> {

        public Object visitDouble(Property<Double> property,
                TodorooCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if(property.checkFlag(Property.PROP_FLAG_NULLABLE) && cursor.isNull(column))
                return null;
            return cursor.getDouble(column);
        }

        public Object visitInteger(Property<Integer> property,
                TodorooCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if(property.checkFlag(Property.PROP_FLAG_NULLABLE) && cursor.isNull(column))
                return null;
            return cursor.getInt(column);
        }

        public Object visitLong(Property<Long> property, TodorooCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if(property.checkFlag(Property.PROP_FLAG_NULLABLE) && cursor.isNull(column))
                return null;
            return cursor.getLong(column);
        }

        public Object visitString(Property<String> property,
                TodorooCursor<?> cursor) {
            int column = columnIndex(property, cursor);
            if(property.checkFlag(Property.PROP_FLAG_NULLABLE) && cursor.isNull(column))
                return null;
            return cursor.getString(column);
        }

        private int columnIndex(Property<?> property, TodorooCursor<?> cursor) {
            return cursor.getColumnIndexFromCache(property.getColumnName());
        }

    }

}
