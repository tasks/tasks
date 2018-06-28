/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.content.Context;
import android.text.TextUtils;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.MoveRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class GtasksSyncService {

  private final Context context;
  private final TaskDao taskDao;
  private final Preferences preferences;
  private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue =
      new LinkedBlockingQueue<>();
  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final Tracker tracker;
  private final GoogleTaskDao googleTaskDao;
  private final PlayServices playServices;

  @Inject
  public GtasksSyncService(
      @ForApplication Context context,
      TaskDao taskDao,
      Preferences preferences,
      GtaskSyncAdapterHelper gtaskSyncAdapterHelper,
      Tracker tracker,
      GoogleTaskDao googleTaskDao,
      GoogleTaskListDao googleTaskListDao,
      PlayServices playServices) {
    this.context = context;
    this.taskDao = taskDao;
    this.preferences = preferences;
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.tracker = tracker;
    this.googleTaskDao = googleTaskDao;
    this.playServices = playServices;
    new OperationPushThread(operationQueue).start();
  }

  public void clearCompleted(GoogleTaskList googleTaskList) {
    operationQueue.offer(new ClearOp(googleTaskList));
  }

  public void triggerMoveForMetadata(GoogleTaskList googleTaskList, GoogleTask googleTask) {
    if (googleTask == null) {
      return;
    }
    if (googleTask.isSuppressSync()) {
      googleTask.setSuppressSync(false);
      return;
    }
    if (preferences.isSyncOngoing()) {
      return;
    }
    if (!gtaskSyncAdapterHelper.isEnabled()) {
      return;
    }

    operationQueue.offer(new MoveOp(googleTaskList, googleTask));
  }

  private void pushMetadataOnSave(GoogleTask model, GtasksInvoker invoker) throws IOException {
    AndroidUtilities.sleepDeep(1000L);

    String taskId = model.getRemoteId();
    String listId = model.getListId();
    String parent = getRemoteParentId(model);
    String priorSibling = getRemoteSiblingId(listId, model);

    MoveRequest move = new MoveRequest(invoker, taskId, listId, parent, priorSibling);
    com.google.api.services.tasks.model.Task result = move.push();
    // Update order googleTask from result
    if (result != null) {
      model.setRemoteOrder(Long.parseLong(result.getPosition()));
      model.setSuppressSync(true);
      googleTaskDao.update(model);
    }
  }

  public void iterateThroughList(
      String listId,
      final GtasksTaskListUpdater.OrderedListIterator iterator,
      long startAtOrder,
      boolean reverse) {
    List<GoogleTask> tasks =
        reverse
            ? googleTaskDao.getTasksFromReverse(listId, startAtOrder)
            : googleTaskDao.getTasksFrom(listId, startAtOrder);
    for (GoogleTask entry : tasks) {
      iterator.processTask(entry.getTask(), entry);
    }
  }

  /** Gets the remote id string of the parent task */
  public String getRemoteParentId(GoogleTask googleTask) {
    String parent = null;
    long parentId = googleTask.getParent();
    GoogleTask parentTask = googleTaskDao.getByTaskId(parentId);
    if (parentTask != null) {
      parent = parentTask.getRemoteId();
      if (TextUtils.isEmpty(parent)) {
        parent = null;
      }
    }
    return parent;
  }

  /** Gets the remote id string of the previous sibling task */
  public String getRemoteSiblingId(String listId, GoogleTask gtasksMetadata) {
    final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getIndent());
    final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getParent());
    final AtomicReference<String> sibling = new AtomicReference<>();
    GtasksTaskListUpdater.OrderedListIterator iterator =
        (taskId, googleTask) -> {
          Task t = taskDao.fetch(taskId);
          if (t == null || t.isDeleted()) {
            return;
          }
          int currIndent = googleTask.getIndent();
          long currParent = googleTask.getParent();

          if (currIndent == indentToMatch.get() && currParent == parentToMatch.get()) {
            if (sibling.get() == null) {
              sibling.set(googleTask.getRemoteId());
            }
          }
        };

    iterateThroughList(listId, iterator, gtasksMetadata.getOrder(), true);
    return sibling.get();
  }

  interface SyncOnSaveOperation {

    void op() throws IOException;
  }

  private class MoveOp implements SyncOnSaveOperation {

    final GoogleTask googleTask;
    private final GoogleTaskList googleTaskList;

    MoveOp(GoogleTaskList googleTaskList, GoogleTask googleTask) {
      this.googleTaskList = googleTaskList;
      this.googleTask = googleTask;
    }

    @Override
    public void op() throws IOException {
      GtasksInvoker invoker = new GtasksInvoker(context, playServices, googleTaskList.getAccount());
      pushMetadataOnSave(googleTask, invoker);
    }
  }

  private class ClearOp implements SyncOnSaveOperation {

    private GoogleTaskList googleTaskList;

    ClearOp(GoogleTaskList googleTaskList) {
      this.googleTaskList = googleTaskList;
    }

    @Override
    public void op() throws IOException {
      GtasksInvoker invoker = new GtasksInvoker(context, playServices, googleTaskList.getAccount());
      invoker.clearCompleted(googleTaskList.getRemoteId());
    }
  }

  private class OperationPushThread extends Thread {

    private final LinkedBlockingQueue<SyncOnSaveOperation> queue;

    OperationPushThread(LinkedBlockingQueue<SyncOnSaveOperation> queue) {
      this.queue = queue;
    }

    @Override
    public void run() {
      //noinspection InfiniteLoopStatement
      while (true) {
        SyncOnSaveOperation op;
        try {
          op = queue.take();
        } catch (InterruptedException e) {
          Timber.e(e);
          continue;
        }
        try {
          op.op();
        } catch (UserRecoverableAuthIOException ignored) {

        } catch (IOException e) {
          tracker.reportException(e);
        }
      }
    }
  }
}
