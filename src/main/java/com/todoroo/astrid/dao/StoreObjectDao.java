/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.astrid.core.SavedFilter;
import com.todoroo.astrid.data.StoreObject;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.sql.Criterion.and;
import static com.todoroo.andlib.sql.Query.select;

@Singleton
public class StoreObjectDao extends DatabaseDao<StoreObject> {

    private static Criterion isSavedFilter = StoreObject.TYPE.eq(SavedFilter.TYPE);

    @Inject
    public StoreObjectDao(Database database) {
        super(StoreObject.class);
        setDatabase(database);
    }

    public void getSavedFilters(Callback<StoreObject> callback) {
        query(callback, select(StoreObject.PROPERTIES)
                .where(isSavedFilter)
                .orderBy(Order.asc(SavedFilter.NAME)));
    }

    public List<StoreObject> getByType(String type) {
        return toList(select(StoreObject.PROPERTIES)
                .where(StoreObject.TYPE.eq(type)));
    }

    public StoreObject getSavedFilterByName(String title) {
        return getFirst(select(StoreObject.ID)
                .where(and(isSavedFilter, SavedFilter.NAME.eq(title))));
    }
}

