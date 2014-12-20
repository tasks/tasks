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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GtasksListService {

    private static final Logger log = LoggerFactory.getLogger(GtasksListService.class);

    public static final StoreObject LIST_NOT_FOUND_OBJECT = null;

    private final StoreObjectDao storeObjectDao;

    @Inject
    public GtasksListService(StoreObjectDao storeObjectDao) {
        this.storeObjectDao = storeObjectDao;
    }

    public List<StoreObject> getLists() {
        return storeObjectDao.getByType(GtasksList.TYPE);
    }

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    public synchronized void updateLists(TaskLists remoteLists) {
        List<StoreObject> lists = getLists();

        Set<Long> previousLists = new HashSet<>();
        for(StoreObject list : lists) {
            previousLists.add(list.getId());
        }

        List<TaskList> items = remoteLists.getItems();
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

            String title = remote.getTitle();
            if(local == null) {
                log.debug("Adding new gtask list {}", title);
                local = new StoreObject();
                local.setValue(GtasksList.LAST_SYNC, 0L);
            }

            local.setType(GtasksList.TYPE);
            local.setValue(GtasksList.REMOTE_ID, id);
            local.setValue(GtasksList.NAME, title);
            local.setValue(GtasksList.ORDER, i);
            storeObjectDao.persist(local);
            previousLists.remove(local.getId());
        }

        // check for lists that aren't on remote server
        for(Long listId : previousLists) {
            storeObjectDao.delete(listId);
        }
    }

    public StoreObject getList(String listId) {
        for(StoreObject list : getLists()) {
            if (list != null && list.getValue(GtasksList.REMOTE_ID).equals(listId)) {
                return list;
            }
        }
        return LIST_NOT_FOUND_OBJECT;
    }
}
