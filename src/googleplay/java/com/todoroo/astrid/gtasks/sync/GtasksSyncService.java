/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.OrderedMetadataListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.api.MoveRequest;

import org.tasks.gtasks.SyncAdapterHelper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class GtasksSyncService {

    private static final String DEFAULT_LIST = "@default"; //$NON-NLS-1$

    private final MetadataDao metadataDao;
    private final TaskDao taskDao;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksMetadata gtasksMetadataFactory;
    private final GtasksInvoker gtasksInvoker;
    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<>();
    private final SyncAdapterHelper syncAdapterHelper;

    @Inject
    public GtasksSyncService(MetadataDao metadataDao, TaskDao taskDao,
                             GtasksPreferenceService gtasksPreferenceService,
                             GtasksMetadata gtasksMetadataFactory, GtasksInvoker gtasksInvoker,
                             SyncAdapterHelper syncAdapterHelper) {
        this.metadataDao = metadataDao;
        this.taskDao = taskDao;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksMetadataFactory = gtasksMetadataFactory;
        this.gtasksInvoker = gtasksInvoker;
        this.syncAdapterHelper = syncAdapterHelper;
        new OperationPushThread(operationQueue).start();
    }

    public interface SyncOnSaveOperation {
        void op(GtasksInvoker invoker) throws IOException;
    }

    private class MoveOp implements SyncOnSaveOperation {
        protected Metadata metadata;

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

    public void enqueue(SyncOnSaveOperation operation) {
        operationQueue.offer(operation);
    }

    private class OperationPushThread extends Thread {
        private final LinkedBlockingQueue<SyncOnSaveOperation> queue;

        public OperationPushThread(LinkedBlockingQueue<SyncOnSaveOperation> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
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
                } catch (IOException e) {
                    Timber.e(e, e.getMessage());
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

    /**
     * Synchronize with server when data changes
     */
    public void pushTaskOnSave(Task task, ContentValues values, GtasksInvoker invoker) throws IOException {
        for (Metadata deleted : getDeleted(task.getId())) {
            gtasksInvoker.deleteGtask(deleted.getValue(GtasksMetadata.LIST_ID), deleted.getValue(GtasksMetadata.ID));
            metadataDao.delete(deleted.getId());
        }

        Metadata gtasksMetadata = metadataDao.getFirstActiveByTaskAndKey(task.getId(), GtasksMetadata.METADATA_KEY);
        com.google.api.services.tasks.model.Task remoteModel;
        boolean newlyCreated = false;

        String remoteId;
        String listId = gtasksPreferenceService.getDefaultList();
        if (listId == null) {
            com.google.api.services.tasks.model.TaskList defaultList = invoker.getGtaskList(DEFAULT_LIST);
            if (defaultList != null) {
                listId = defaultList.getId();
                gtasksPreferenceService.setDefaultList(listId);
            } else {
                listId = DEFAULT_LIST;
            }
        }

        if (gtasksMetadata == null || !gtasksMetadata.containsNonNullValue(GtasksMetadata.ID) ||
                TextUtils.isEmpty(gtasksMetadata.getValue(GtasksMetadata.ID))) { //Create case
            if (gtasksMetadata == null) {
                gtasksMetadata = gtasksMetadataFactory.createEmptyMetadata(task.getId());
            }
            if (gtasksMetadata.containsNonNullValue(GtasksMetadata.LIST_ID)) {
                listId = gtasksMetadata.getValue(GtasksMetadata.LIST_ID);
            }

            remoteModel = new com.google.api.services.tasks.model.Task();
            newlyCreated = true;
        } else { //update case
            remoteId = gtasksMetadata.getValue(GtasksMetadata.ID);
            listId = gtasksMetadata.getValue(GtasksMetadata.LIST_ID);
            remoteModel = new com.google.api.services.tasks.model.Task();
            remoteModel.setId(remoteId);
        }

        //If task was newly created but without a title, don't sync--we're in the middle of
        //creating a task which may end up being cancelled. Also don't sync new but already
        //deleted tasks
        if (newlyCreated &&
                (!values.containsKey(Task.TITLE.name) || TextUtils.isEmpty(task.getTitle()) || task.getDeletionDate() > 0)) {
            return;
        }

        //Update the remote model's changed properties
        if (values.containsKey(Task.DELETION_DATE.name) && task.isDeleted()) {
            remoteModel.setDeleted(true);
        }

        if (values.containsKey(Task.TITLE.name)) {
            remoteModel.setTitle(task.getTitle());
        }
        if (values.containsKey(Task.NOTES.name)) {
            remoteModel.setNotes(task.getNotes());
        }
        if (values.containsKey(Task.DUE_DATE.name) && task.hasDueDate()) {
            remoteModel.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(task.getDueDate()));
        }
        if (values.containsKey(Task.COMPLETION_DATE.name)) {
            if (task.isCompleted()) {
                remoteModel.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.getCompletionDate()));
                remoteModel.setStatus("completed"); //$NON-NLS-1$
            } else {
                remoteModel.setCompleted(null);
                remoteModel.setStatus("needsAction"); //$NON-NLS-1$
            }
        }

        if (!newlyCreated) {
            try {
                invoker.updateGtask(listId, remoteModel);
            } catch(HttpNotFoundException e) {
                Timber.e("Received 404 response, deleting %s", gtasksMetadata);
                metadataDao.delete(gtasksMetadata.getId());
                return;
            }
        } else {
            String parent = getRemoteParentId(gtasksMetadata);
            String priorSibling = getRemoteSiblingId(listId, gtasksMetadata);

            com.google.api.services.tasks.model.Task created = invoker.createGtask(listId, remoteModel, parent, priorSibling);

            if (created != null) {
                //Update the metadata for the newly created task
                gtasksMetadata.setValue(GtasksMetadata.ID, created.getId());
                gtasksMetadata.setValue(GtasksMetadata.LIST_ID, listId);
            } else {
                return;
            }
        }

        task.setModificationDate(DateUtilities.now());
        gtasksMetadata.setValue(GtasksMetadata.LAST_SYNC, DateUtilities.now() + 1000L);
        metadataDao.persist(gtasksMetadata);
        task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
        taskDao.saveExistingWithSqlConstraintCheck(task);
    }

    public void pushMetadataOnSave(Metadata model, GtasksInvoker invoker) throws IOException {
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

    public void iterateThroughList(String listId, final OrderedMetadataListUpdater.OrderedListIterator iterator, long startAtOrder, boolean reverse) {
        Field orderField = Functions.cast(GtasksMetadata.ORDER, "LONG");
        Order order = reverse ? Order.desc(orderField) : Order.asc(orderField);
        Criterion startAtCriterion = reverse ?  Functions.cast(GtasksMetadata.ORDER, "LONG").lt(startAtOrder) :
                Functions.cast(GtasksMetadata.ORDER, "LONG").gt(startAtOrder - 1);

        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataDao.MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.LIST_ID.eq(listId),
                startAtCriterion)).
                orderBy(order);

        metadataDao.query(query, new Callback<Metadata>() {
            @Override
            public void apply(Metadata entry) {
                long taskId = entry.getValue(Metadata.TASK);
                Metadata metadata = metadataDao.getFirstActiveByTaskAndKey(taskId, GtasksMetadata.METADATA_KEY);
                if(metadata != null) {
                    iterator.processTask(taskId, metadata);
                }
            }
        });
    }

    private List<Metadata> getDeleted(long taskId) {
        return metadataDao.toList(Criterion.and(
                MetadataDao.MetadataCriteria.byTaskAndwithKey(taskId, GtasksMetadata.METADATA_KEY),
                MetadataDao.MetadataCriteria.isDeleted()));
    }

    /**
     * Gets the remote id string of the parent task
     */
    private String getRemoteParentId(Metadata gtasksMetadata) {
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
    private String getRemoteSiblingId(String listId, Metadata gtasksMetadata) {
        final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getValue(GtasksMetadata.INDENT));
        final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK));
        final AtomicReference<String> sibling = new AtomicReference<>();
        OrderedMetadataListUpdater.OrderedListIterator iterator = new OrderedMetadataListUpdater.OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
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
            }
        };

        iterateThroughList(listId, iterator, gtasksMetadata.getValue(GtasksMetadata.ORDER), true);
        return sibling.get();
    }
}
