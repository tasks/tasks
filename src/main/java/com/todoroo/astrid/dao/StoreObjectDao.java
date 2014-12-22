/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.google.common.base.Function;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.astrid.core.SavedFilter;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
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

    public GtasksList getGtasksList(long id) {
        StoreObject result = fetch(id, StoreObject.PROPERTIES);
        if (!result.getType().equals(GtasksList.TYPE)) {
            throw new RuntimeException("Not a google task list");
        }
        return new GtasksList(result);
    }

    public List<GtasksList> getGtasksLists() {
        return newArrayList(transform(getByType(GtasksList.TYPE), new Function<StoreObject, GtasksList>() {
            @Override
            public GtasksList apply(StoreObject input) {
                return new GtasksList(input);
            }
        }));
    }

    public void persist(GtasksList list) {
        persist(list.getStoreObject());
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

