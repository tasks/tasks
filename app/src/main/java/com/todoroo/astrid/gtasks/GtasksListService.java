/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.service.TaskDeleter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import timber.log.Timber;

public class GtasksListService {

  private final GoogleTaskListDao googleTaskListDao;
  private final TaskDeleter taskDeleter;
  private final LocalBroadcastManager localBroadcastManager;
  private final GoogleTaskDao googleTaskDao;
  private final TaskDao taskDao;

  @Inject
  public GtasksListService(
      GoogleTaskListDao googleTaskListDao,
      TaskDeleter taskDeleter,
      LocalBroadcastManager localBroadcastManager,
      GoogleTaskDao googleTaskDao,
      TaskDao taskDao) {
    this.googleTaskListDao = googleTaskListDao;
    this.taskDeleter = taskDeleter;
    this.localBroadcastManager = localBroadcastManager;
    this.googleTaskDao = googleTaskDao;
    this.taskDao = taskDao;
  }

  public GoogleTaskList getList(long id) {
    return googleTaskListDao.getById(id);
  }

  /**
   * Reads in remote list information and updates local list objects.
   *
   * @param remoteLists remote information about your lists
   */
  public synchronized void updateLists(GoogleTaskAccount account, List<TaskList> remoteLists) {
    List<GoogleTaskList> lists = googleTaskListDao.getLists(account.getAccount());

    Set<Long> previousLists = new HashSet<>();
    for (GoogleTaskList list : lists) {
      previousLists.add(list.getId());
    }

    for (int i = 0; i < remoteLists.size(); i++) {
      com.google.api.services.tasks.model.TaskList remote = remoteLists.get(i);

      String id = remote.getId();
      GoogleTaskList local = null;
      for (GoogleTaskList list : lists) {
        if (list.getRemoteId().equals(id)) {
          local = list;
          break;
        }
      }

      String title = remote.getTitle();
      if (local == null) {
        GoogleTaskList byRemoteId = googleTaskListDao.findExistingList(id);
        if (byRemoteId != null) {
          byRemoteId.setAccount(account.getAccount());
          local = byRemoteId;
        } else {
          Timber.d("Adding new gtask list %s", title);
          local = new GoogleTaskList();
          local.setAccount(account.getAccount());
          local.setRemoteId(id);
        }
      }

      local.setTitle(title);
      local.setRemoteOrder(i);
      googleTaskListDao.insertOrReplace(local);
      previousLists.remove(local.getId());
    }

    // check for lists that aren't on remote server
    for (Long listId : previousLists) {
      taskDeleter.delete(googleTaskListDao.getById(listId));
    }

    localBroadcastManager.broadcastRefreshList();
  }

  public List<GoogleTaskList> getListsToUpdate(List<TaskList> remoteLists) {
    List<GoogleTaskList> listsToUpdate = newArrayList();
    for (TaskList remoteList : remoteLists) {
      GoogleTaskList localList = getList(remoteList.getId());
      String listName = localList.getTitle();
      Long lastSync = localList.getLastSync();
      long lastUpdate = remoteList.getUpdated().getValue();
      if (lastSync < lastUpdate) {
        listsToUpdate.add(localList);
        Timber.d(
            "%s out of date [local=%s] [remote=%s]",
            listName, printTimestamp(lastSync), printTimestamp(lastUpdate));
      } else {
        Timber.d("%s up to date", listName);
      }
    }
    return listsToUpdate;
  }

  public GoogleTaskList getList(String listId) {
    return googleTaskListDao.getByRemoteId(listId);
  }
}
