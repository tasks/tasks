/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.core.CustomFilterActivity.CriterionInstance;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import java.util.Map;

/**
 * {@link StoreObject} entries for a saved custom filter
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SavedFilter {

    private static final String TYPE = "filter"; //$NON-NLS-1$

    /**
     * Save a filter
     */
    public static StoreObject persist(StoreObjectDao dao, CustomFilterAdapter adapter, String title,
            String sql, Map<String, Object> values) {

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
        storeObject.setItem(title);
        storeObject.setValue(sql);

        if(values == null) {
            storeObject.setValue2(""); //$NON-NLS-1$
        } else {
            storeObject.setValue2(AndroidUtilities.mapToSerializedString(values));
        }

        String filters = serializeFilters(adapter);
        storeObject.setValue3(filters);

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
