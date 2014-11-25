/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GtasksListService {

    public static final StoreObject LIST_NOT_FOUND_OBJECT = null;

    private final StoreObjectDao storeObjectDao;

    private List<StoreObject> lists;

    @Inject
    public GtasksListService(StoreObjectDao storeObjectDao) {
        this.storeObjectDao = storeObjectDao;
    }

    private void readLists() {
        if(lists != null) {
            return;
        }

        lists = storeObjectDao.getByType(GtasksList.TYPE);
    }

    public List<StoreObject> getLists() {
        readLists();
        return lists;
    }

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    public synchronized void updateLists(TaskLists remoteLists) {
        readLists();

        Set<Long> previousLists = new HashSet<>();
        for(StoreObject list : lists) {
            previousLists.add(list.getId());
        }

        List<TaskList> items = remoteLists.getItems();
        List<StoreObject> newLists = new ArrayList<>();
        for(int i = 0; i < items.size(); i++) {
            com.google.api.services.tasks.model.TaskList remote = items.get(i);

            String id = remote.getId();
            StoreObject local = null;
            for(StoreObject list : lists) {
                if(list.getValue(GtasksList.REMOTE_ID).equals(id)) {
                    local = list;
                    break;
                }
            }

            if(local == null) {
                local = new StoreObject();
            }

            local.setType(GtasksList.TYPE);
            local.setValue(GtasksList.REMOTE_ID, id);
            local.setValue(GtasksList.NAME, remote.getTitle());
            local.setValue(GtasksList.ORDER, i);
            storeObjectDao.persist(local);
            previousLists.remove(local.getId());
            newLists.add(local);
        }
        lists = newLists;

        // check for lists that aren't on remote server
        for(Long listId : previousLists) {
            storeObjectDao.delete(listId);
        }
    }

    public StoreObject getList(String listId) {
        readLists();
        for(StoreObject list : lists) {
            if (list != null && list.getValue(GtasksList.REMOTE_ID).equals(listId)) {
                return list;
            }
        }
        return LIST_NOT_FOUND_OBJECT;
    }
}
