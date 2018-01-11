/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;

import org.tasks.LocalBroadcastManager;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.TaskListDataProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.tasks.time.DateTimeUtils.printTimestamp;

public class GtasksListService {

    private final StoreObjectDao storeObjectDao;
    private final TaskListDataProvider taskListDataProvider;
    private final TaskDeleter taskDeleter;
    private LocalBroadcastManager localBroadcastManager;
    private final GoogleTaskDao googleTaskDao;

    @Inject
    public GtasksListService(StoreObjectDao storeObjectDao, TaskListDataProvider taskListDataProvider,
                             TaskDeleter taskDeleter, LocalBroadcastManager localBroadcastManager,
                             GoogleTaskDao googleTaskDao) {
        this.storeObjectDao = storeObjectDao;
        this.taskListDataProvider = taskListDataProvider;
        this.taskDeleter = taskDeleter;
        this.localBroadcastManager = localBroadcastManager;
        this.googleTaskDao = googleTaskDao;
    }

    public List<GtasksList> getLists() {
        return transform(storeObjectDao.getGtasksLists(), GtasksList::new);
    }

    public GtasksList getList(long id) {
        return storeObjectDao.getGtasksList(id);
    }

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    public synchronized void updateLists(List<TaskList> remoteLists) {
        List<GtasksList> lists = getLists();

        Set<Long> previousLists = new HashSet<>();
        for(GtasksList list : lists) {
            previousLists.add(list.getId());
        }

        for(int i = 0; i < remoteLists.size(); i++) {
            com.google.api.services.tasks.model.TaskList remote = remoteLists.get(i);

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
                Timber.d("Adding new gtask list %s", title);
                local = new GtasksList(id);
            }

            local.setName(title);
            local.setOrder(i);
            storeObjectDao.persist(local);
            previousLists.remove(local.getId());
        }

        // check for lists that aren't on remote server
        for(Long listId : previousLists) {
            deleteList(storeObjectDao.getGtasksList(listId));
        }

        localBroadcastManager.broadcastRefreshList();
    }

    public void deleteList(GtasksList gtasksList) {
        List<Task> tasks = taskListDataProvider
                .constructCursor(new GtasksFilter(gtasksList), Task.PROPERTIES)
                .toList();
        for (Task task : tasks) {
            taskDeleter.delete(task);
        }
        googleTaskDao.deleteList(gtasksList.getRemoteId());
        storeObjectDao.delete(gtasksList.getId());
    }

    public List<GtasksList> getListsToUpdate(List<TaskList> remoteLists) {
        List<GtasksList> listsToUpdate = newArrayList();
        for (TaskList remoteList : remoteLists) {
            GtasksList localList = getList(remoteList.getId());
            String listName = localList.getName();
            Long lastSync = localList.getLastSync();
            long lastUpdate = remoteList.getUpdated().getValue();
            if (lastSync < lastUpdate) {
                listsToUpdate.add(localList);
                Timber.d("%s out of date [local=%s] [remote=%s]", listName, printTimestamp(lastSync), printTimestamp(lastUpdate));
            } else {
                Timber.d("%s up to date", listName);
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
