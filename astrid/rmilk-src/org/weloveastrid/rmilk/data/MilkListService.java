/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;

import java.util.Map;

import org.weloveastrid.rmilk.MilkDependencyInjector;
import org.weloveastrid.rmilk.api.data.RtmList;
import org.weloveastrid.rmilk.api.data.RtmLists;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.StoreObjectApiDao;
import com.todoroo.astrid.data.StoreObjectApiDao.StoreObjectCriteria;

/**
 * Service for reading and writing Milk lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkListService {

    static {
        MilkDependencyInjector.initialize();
    }

    private StoreObject[] lists = null;

    private final StoreObjectApiDao storeObjectDao;

    public MilkListService() {
        storeObjectDao = new StoreObjectApiDao(ContextManager.getContext());
    }

    /**
     * Reads lists
     */
    private void readLists() {
        if(lists != null)
            return;

        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(MilkListFields.TYPE)).orderBy(Order.asc(MilkListFields.POSITION)));
        try {
            lists = new StoreObject[cursor.getCount()];
            for(int i = 0; i < lists.length; i++) {
                cursor.moveToNext();
                StoreObject list = new StoreObject(cursor);
                lists[i] = list;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * @return a list of lists
     */
    public StoreObject[] getLists() {
        readLists();
        return lists;
    }

    /**
     * Clears current cache of RTM lists and loads data from RTM into
     * database. Returns the inbox list.
     *
     * @param remoteLists
     * @return list with the name "inbox"
     */
    public StoreObject setLists(RtmLists remoteLists) {
        readLists();

        StoreObject inbox = null;
        for(Map.Entry<String, RtmList> remote : remoteLists.getLists().entrySet()) {
            if(remote.getValue().isSmart() || "All Tasks".equals(remote.getValue().getName())) //$NON-NLS-1$
                continue;

            long id = Long.parseLong(remote.getValue().getId());
            StoreObject local = null;
            for(StoreObject list : lists) {
                if(list.getValue(MilkListFields.REMOTE_ID).equals(id)) {
                    local = list;
                    break;
                }
            }

            if(local == null)
                local = new StoreObject();

            local.setValue(StoreObject.TYPE, MilkListFields.TYPE);
            local.setValue(MilkListFields.REMOTE_ID, id);
            local.setValue(MilkListFields.NAME, remote.getValue().getName());
            local.setValue(MilkListFields.POSITION, remote.getValue().getPosition());
            local.setValue(MilkListFields.ARCHIVED, remote.getValue().isArchived() ? 1 : 0);
            storeObjectDao.save(local);

            if(remote.getValue().isInbox()) {
                inbox = local;
            }
        }

        // clear list cache
        lists = null;
        return inbox;
    }

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getListName(long listId) {
        readLists();
        for(StoreObject list : lists)
            if(list.getValue(MilkListFields.REMOTE_ID).equals(listId))
                return list.getValue(MilkListFields.NAME);
        return null;
    }
}
