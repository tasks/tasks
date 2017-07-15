/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.AndroidUtilities;
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
    private static final StringProperty FILTERS = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE3.name);

    // --- data storage and retrieval methods

    /**
     * Save a filter
     */
    public static StoreObject persist(StoreObjectDao dao, CustomFilterAdapter adapter, String title,
            String sql, ContentValues values) {

        if(title == null || title.length() == 0) {
            return null;
        }

        // if filter of this name exists, edit it
        StoreObject storeObject = dao.getSavedFilterByName(title);
        if (storeObject == null) {
            storeObject = new StoreObject();
        }

        // populate saved filter properties
        storeObject.setType(TYPE);
        storeObject.setValue(NAME, title);
        storeObject.setValue(SQL, sql);

        if(values == null) {
            storeObject.setValue(VALUES, ""); //$NON-NLS-1$
        } else {
            storeObject.setValue(VALUES, AndroidUtilities.contentValuesToSerializedString(values));
        }

        String filters = serializeFilters(adapter);
        storeObject.setValue(FILTERS, filters);

        if (dao.persist(storeObject)) {
            return storeObject;
        }

        return null;
    }

    /**
     * Turn a series of CriterionInstance objects into a string
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
            if(item.criterion.sql != null) {
                values.append(item.criterion.sql);
            }
            values.append('\n');
        }

        return values.toString();
    }

    private static String escape(String item) {
        if(item == null) {
            return ""; //$NON-NLS-1$
        }
        return item.replace(AndroidUtilities.SERIALIZATION_SEPARATOR,
                AndroidUtilities.SEPARATOR_ESCAPE);
    }
}
