/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.astrid.core.SavedFilter;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;

import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.sql.Criterion.and;
import static com.todoroo.andlib.sql.Query.select;

public class StoreObjectDao {

    private final DatabaseDao<StoreObject> dao;

    private static final Criterion isSavedFilter = StoreObject.TYPE.eq(SavedFilter.TYPE);

    @Inject
    public StoreObjectDao(Database database) {
        dao = new DatabaseDao<>(database, StoreObject.class);
    }

    public List<StoreObject> getSavedFilters() {
        return dao.toList(select(StoreObject.PROPERTIES)
                .where(isSavedFilter)
                .orderBy(Order.asc(SavedFilter.NAME)));
    }

    public StoreObject getSavedFilterById(long id) {
        return dao.getFirst(select(StoreObject.PROPERTIES)
                .where(and(isSavedFilter, StoreObject.ID.eq(id))));
    }

    public GtasksList getGtasksList(long id) {
        StoreObject result = dao.fetch(id, StoreObject.PROPERTIES);
        if (result == null) {
            throw new RuntimeException(String.format("No store object found [id=%s]", id));
        } else if (!result.getType().equals(GtasksList.TYPE)) {
            throw new RuntimeException("Not a google task list");
        }
        return new GtasksList(result);
    }

    public List<StoreObject> getGtasksLists() {
        return dao.toList(select(StoreObject.PROPERTIES)
                .where(and(StoreObject.DELETION_DATE.eq(0), StoreObject.TYPE.eq(GtasksList.TYPE)))
                .orderBy(Order.asc(StoreObject.VALUE1)));
    }

    public boolean persist(StoreObject storeObject) {
        return dao.persist(storeObject);
    }

    public void persist(GtasksList list) {
        persist(list.getStoreObject());
    }

    public void update(StoreObject storeObject) {
        dao.saveExisting(storeObject);
    }

    public StoreObject getSavedFilterByName(String title) {
        return dao.getFirst(select(StoreObject.ID)
                .where(and(isSavedFilter, SavedFilter.NAME.eq(title))));
    }

    public void delete(long id) {
        dao.delete(id);
    }
}

