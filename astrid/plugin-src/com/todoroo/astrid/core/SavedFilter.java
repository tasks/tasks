/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CustomFilterActivity.CriterionInstance;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

/**
 * {@link StoreObject} entries for a saved custom filter
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SavedFilter {

    /** type */
    public static final String TYPE = "filter"; //$NON-NLS-1$

    /** saved filter name */
    public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** perma-sql */
    public static final StringProperty SQL = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /** serialized new task content values */
    public static final StringProperty VALUES = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE2.name);

    /** serialized list of filters applied */
    public static final StringProperty FILTERS = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE3.name);

    // --- data storage and retrieval methods

    /**
     * Save a filter
     *
     * @param adapter
     * @param title
     * @param sql2
     * @param values2
     */
    public static void persist(CustomFilterAdapter adapter, String title,
            String sql, ContentValues values) {

        if(title == null || title.length() == 0)
            return;

        // if filter of this name exists, edit it
        StoreObjectDao dao = PluginServices.getStoreObjectDao();
        StoreObject storeObject = new StoreObject();
        TodorooCursor<StoreObject> cursor = dao.query(Query.select(StoreObject.ID).where(NAME.eq(title)));
        try {
            if(!cursor.isAfterLast()) {
                cursor.moveToNext();
                storeObject.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        // populate saved filter properties
        storeObject.setValue(StoreObject.TYPE, TYPE);
        storeObject.setValue(NAME, title);
        storeObject.setValue(SQL, sql);

        if(values == null)
            storeObject.setValue(VALUES, ""); //$NON-NLS-1$
        else
            storeObject.setValue(VALUES, AndroidUtilities.contentValuesToSerializedString(values));

        String filters = serializeFilters(adapter);
        storeObject.setValue(FILTERS, filters);

        dao.persist(storeObject);
    }

    /**
     * Turn a series of CriterionInstance objects into a string
     * @param adapter
     * @return
     */
    private static String serializeFilters(CustomFilterAdapter adapter) {
        StringBuilder values = new StringBuilder();
        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance item = adapter.getItem(i);

            // criterion|entry|text|type|sql
            values.append(escape(item.criterion.identifier)).append(AndroidUtilities.SERIALIZATION_SEPARATOR);
            values.append(escape(item.getValueFromCriterion())).append(AndroidUtilities.SERIALIZATION_SEPARATOR);
            values.append(escape(item.criterion.text)).append(AndroidUtilities.SERIALIZATION_SEPARATOR);
            values.append(item.type).append(AndroidUtilities.SERIALIZATION_SEPARATOR);
            if(item.criterion.sql != null)
                values.append(item.criterion.sql);
            values.append('\n');
        }

        return values.toString();
    }

    private static String escape(String item) {
        if(item == null)
            return ""; //$NON-NLS-1$
        return item.replace(AndroidUtilities.SERIALIZATION_SEPARATOR,
                AndroidUtilities.SEPARATOR_ESCAPE);
    }

    /**
     * Read filter from store
     * @param savedFilter
     * @return
     */
    public static Filter load(StoreObject savedFilter) {
        String title = savedFilter.getValue(NAME);
        String sql = savedFilter.getValue(SQL);
        String values = savedFilter.getValue(VALUES);

        ContentValues contentValues = null;
        if(!TextUtils.isEmpty(values))
            contentValues = AndroidUtilities.contentValuesFromSerializedString(values);

        return new Filter(title, title, sql, contentValues);
    }

}
