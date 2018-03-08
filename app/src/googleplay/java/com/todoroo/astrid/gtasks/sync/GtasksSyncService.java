/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.text.TextUtils;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.MoveRequest;

import org.tasks.analytics.Tracker;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.injection.ApplicationScope;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

@ApplicationScope
public class GtasksSyncService {

    private final TaskDao taskDao;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksInvoker gtasksInvoker;
    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<>();
    private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
    private final Tracker tracker;
    private final GoogleTaskDao googleTaskDao;

    @Inject
    public GtasksSyncService(TaskDao taskDao,
                             GtasksPreferenceService gtasksPreferenceService,
                             GtasksInvoker gtasksInvoker,
                             GtaskSyncAdapterHelper gtaskSyncAdapterHelper, Tracker tracker,
                             GoogleTaskDao googleTaskDao) {
        this.taskDao = taskDao;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksInvoker = gtasksInvoker;
        this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
        this.tracker = tracker;
        this.googleTaskDao = googleTaskDao;
        new OperationPushThread(operationQueue).start();
    }

    public interface SyncOnSaveOperation {
        void op(GtasksInvoker invoker) throws IOException;
    }

    private class MoveOp implements SyncOnSaveOperation {
        final GoogleTask googleTask;

        public MoveOp(GoogleTask googleTask) {
            this.googleTask = googleTask;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            pushMetadataOnSave(googleTask, invoker);
        }
    }

    private class ClearOp implements SyncOnSaveOperation {
        private final String listId;

        public ClearOp(String listId) {
            this.listId = listId;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            invoker.clearCompleted(listId);
        }
    }

    private class OperationPushThread extends Thread {
        private final LinkedBlockingQueue<SyncOnSaveOperation> queue;

        public OperationPushThread(LinkedBlockingQueue<SyncOnSaveOperation> queue) {
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
                    Timber.e(e, e.getMessage());
                    continue;
                }
                try {
                    op.op(gtasksInvoker);
                } catch (UserRecoverableAuthIOException ignored) {

                } catch (IOException e) {
                    tracker.reportException(e);
                }
            }
        }
    }

    public void clearCompleted(String listId) {
        operationQueue.offer(new ClearOp(listId));
    }

    public void triggerMoveForMetadata(final GoogleTask googleTask) {
        if (googleTask == null) {
            return;
        }
        if (googleTask.isSuppressSync()) {
            googleTask.setSuppressSync(false);
            return;
        }
        if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
        {
            return;
        }
        if (!gtaskSyncAdapterHelper.isEnabled()) {
            return;
        }

        operationQueue.offer(new MoveOp(googleTask));
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

    public void iterateThroughList(String listId, final GtasksTaskListUpdater.OrderedListIterator iterator, long startAtOrder, boolean reverse) {
        List<GoogleTask> tasks = reverse
                ? googleTaskDao.getTasksFromReverse(listId, startAtOrder)
                : googleTaskDao.getTasksFrom(listId, startAtOrder);
        for (GoogleTask entry : tasks) {
            iterator.processTask(entry.getTask(), entry);
        }
    }

    /**
     * Gets the remote id string of the parent task
     */
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

    /**
     * Gets the remote id string of the previous sibling task
     */
    public String getRemoteSiblingId(String listId, GoogleTask gtasksMetadata) {
        final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getIndent());
        final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getParent());
        final AtomicReference<String> sibling = new AtomicReference<>();
        GtasksTaskListUpdater.OrderedListIterator iterator = (taskId, googleTask) -> {
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
}
