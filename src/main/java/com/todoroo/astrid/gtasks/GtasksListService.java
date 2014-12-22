/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.astrid.dao.StoreObjectDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class GtasksListService {

    private static final Logger log = LoggerFactory.getLogger(GtasksListService.class);

    private final StoreObjectDao storeObjectDao;

    @Inject
    public GtasksListService(StoreObjectDao storeObjectDao) {
        this.storeObjectDao = storeObjectDao;
    }

    public List<GtasksList> getLists() {
        return storeObjectDao.getGtasksLists();
    }

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    public synchronized void updateLists(TaskLists remoteLists) {
        List<GtasksList> lists = getLists();

        Set<Long> previousLists = new HashSet<>();
        for(GtasksList list : lists) {
            previousLists.add(list.getId());
        }

        List<TaskList> items = remoteLists.getItems();
        for(int i = 0; i < items.size(); i++) {
            com.google.api.services.tasks.model.TaskList remote = items.get(i);

            String id = remote.getId();
            GtasksList local = null;
            for(GtasksList list : lists) {
                if(list.getRemoteId().equals(id)) {
                    local = list;
                    break;
                }
            }

            String title = remote.getTitle();
            if(local == null) {
                log.debug("Adding new gtask list {}", title);
                local = new GtasksList(id);
            }

            local.setName(title);
            local.setOrder(i);
            storeObjectDao.persist(local);
            previousLists.remove(local.getId());
        }

        // check for lists that aren't on remote server
        for(Long listId : previousLists) {
            storeObjectDao.delete(listId);
        }
    }

    public List<GtasksList> getListsToUpdate(TaskLists remoteLists) {
        List<GtasksList> listsToUpdate = newArrayList();
        for (TaskList remoteList : remoteLists.getItems()) {
            GtasksList localList = getList(remoteList.getId());
            String listName = localList.getName();
            Long lastSync = localList.getLastSync();
            long lastUpdate = remoteList.getUpdated().getValue();
            if (lastSync < lastUpdate) {
                listsToUpdate.add(localList);
                log.debug("{} out of date [local={}] [remote={}]", listName, newDateTime(lastSync), newDateTime(lastUpdate));
            } else {
                log.debug("{} up to date", listName);
            }
        }
        return listsToUpdate;
    }

    public GtasksList getList(String listId) {
        for(GtasksList list : getLists()) {
            if (list != null && list.getRemoteId().equals(listId)) {
                return list;
            }
        }
        return null;
    }
}
