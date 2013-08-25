/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import android.content.Context;

import com.todoroo.andlib.data.ContentResolverDao;
import com.todoroo.andlib.sql.Criterion;

/**
 * Data access object for accessing Astrid's {@link StoreObject} table. A
 * StoreObject is an arbitrary piece of data stored inside of Astrid.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class StoreObjectApiDao extends ContentResolverDao<StoreObject> {

    public StoreObjectApiDao(Context context) {
        super(StoreObject.class, context, StoreObject.CONTENT_URI);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class StoreObjectCriteria {

        /** Returns all store objects with given type */
        public static Criterion byType(String type) {
            return StoreObject.TYPE.eq(type);
        }

        /** Returns store object with type and key */
        public static Criterion byTypeAndItem(String type, String item) {
            return Criterion.and(byType(type), StoreObject.ITEM.eq(item));
        }

    }

}
