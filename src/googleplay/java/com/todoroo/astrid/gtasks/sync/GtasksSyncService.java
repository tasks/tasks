/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.text.TextUtils;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.MoveRequest;

import org.tasks.analytics.Tracker;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.injection.ApplicationScope;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

@ApplicationScope
public class GtasksSyncService {

    private final MetadataDao metadataDao;
    private final TaskDao taskDao;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksInvoker gtasksInvoker;
    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<>();
    private final SyncAdapterHelper syncAdapterHelper;
    private final Tracker tracker;

    @Inject
    public GtasksSyncService(MetadataDao metadataDao, TaskDao taskDao,
                             GtasksPreferenceService gtasksPreferenceService,
                             GtasksInvoker gtasksInvoker,
                             SyncAdapterHelper syncAdapterHelper, Tracker tracker) {
        this.metadataDao = metadataDao;
        this.taskDao = taskDao;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksInvoker = gtasksInvoker;
        this.syncAdapterHelper = syncAdapterHelper;
        this.tracker = tracker;
        new OperationPushThread(operationQueue).start();
    }

    public interface SyncOnSaveOperation {
        void op(GtasksInvoker invoker) throws IOException;
    }

    private class MoveOp implements SyncOnSaveOperation {
        final Metadata metadata;

        public MoveOp(Metadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            pushMetadataOnSave(metadata, invoker);
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

    public void triggerMoveForMetadata(final Metadata metadata) {
        if (metadata == null) {
            return;
        }
        if (metadata.checkAndClearTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
            return;
        }
        if (!metadata.getKey().equals(GtasksMetadata.METADATA_KEY)) //Don't care about non-gtasks metadata
        {
            return;
        }
        if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
        {
            return;
        }
        if (!syncAdapterHelper.isEnabled()) {
            return;
        }

        operationQueue.offer(new MoveOp(metadata));
    }

    private void pushMetadataOnSave(Metadata model, GtasksInvoker invoker) throws IOException {
        AndroidUtilities.sleepDeep(1000L);

        String taskId = model.getValue(GtasksMetadata.ID);
        String listId = model.getValue(GtasksMetadata.LIST_ID);
        String parent = getRemoteParentId(model);
        String priorSibling = getRemoteSiblingId(listId, model);

        MoveRequest move = new MoveRequest(invoker, taskId, listId, parent, priorSibling);
        com.google.api.services.tasks.model.Task result = move.push();
        // Update order metadata from result
        if (result != null) {
            model.setValue(GtasksMetadata.GTASKS_ORDER, Long.parseLong(result.getPosition()));
            model.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            metadataDao.saveExisting(model);
        }
    }

    public void iterateThroughList(String listId, final GtasksTaskListUpdater.OrderedListIterator iterator, long startAtOrder, boolean reverse) {
        Field orderField = Functions.cast(GtasksMetadata.ORDER, "LONG");
        Order order = reverse ? Order.desc(orderField) : Order.asc(orderField);
        Criterion startAtCriterion = reverse ?  Functions.cast(GtasksMetadata.ORDER, "LONG").lt(startAtOrder) :
                Functions.cast(GtasksMetadata.ORDER, "LONG").gt(startAtOrder - 1);

        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataDao.MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.LIST_ID.eq(listId),
                startAtCriterion)).
                orderBy(order);

        metadataDao.query(query, entry -> {
            long taskId = entry.getValue(Metadata.TASK);
            Metadata metadata = metadataDao.getFirstActiveByTaskAndKey(taskId, GtasksMetadata.METADATA_KEY);
            if(metadata != null) {
                iterator.processTask(taskId, metadata);
            }
        });
    }

    /**
     * Gets the remote id string of the parent task
     */
    public String getRemoteParentId(Metadata gtasksMetadata) {
        String parent = null;
        if (gtasksMetadata.containsNonNullValue(GtasksMetadata.PARENT_TASK)) {
            long parentId = gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK);
            Metadata parentMetadata = metadataDao.getFirstActiveByTaskAndKey(parentId, GtasksMetadata.METADATA_KEY);
            if (parentMetadata != null && parentMetadata.containsNonNullValue(GtasksMetadata.ID)) {
                parent = parentMetadata.getValue(GtasksMetadata.ID);
                if (TextUtils.isEmpty(parent)) {
                    parent = null;
                }
            }
        }
        return parent;
    }

    /**
     * Gets the remote id string of the previous sibling task
     */
    public String getRemoteSiblingId(String listId, Metadata gtasksMetadata) {
        final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getValue(GtasksMetadata.INDENT));
        final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK));
        final AtomicReference<String> sibling = new AtomicReference<>();
        GtasksTaskListUpdater.OrderedListIterator iterator = (taskId, metadata) -> {
            Task t = taskDao.fetch(taskId, Task.TITLE, Task.DELETION_DATE);
            if (t == null || t.isDeleted()) {
                return;
            }
            int currIndent = metadata.getValue(GtasksMetadata.INDENT);
            long currParent = metadata.getValue(GtasksMetadata.PARENT_TASK);

            if (currIndent == indentToMatch.get() && currParent == parentToMatch.get()) {
                if (sibling.get() == null) {
                    sibling.set(metadata.getValue(GtasksMetadata.ID));
                }
            }
        };

        iterateThroughList(listId, iterator, gtasksMetadata.getValue(GtasksMetadata.ORDER), true);
        return sibling.get();
    }
}
