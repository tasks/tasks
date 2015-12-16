/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.api.MoveRequest;
import com.todoroo.astrid.service.TaskService;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class GtasksSyncService {

    private static final String DEFAULT_LIST = "@default"; //$NON-NLS-1$

    private final MetadataDao metadataDao;
    private final GtasksMetadataService gtasksMetadataService;
    private final TaskDao taskDao;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksMetadata gtasksMetadataFactory;
    private GtasksInvoker gtasksInvoker;
    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<>();

    @Inject
    public GtasksSyncService(MetadataDao metadataDao, GtasksMetadataService gtasksMetadataService,
                             TaskDao taskDao, GtasksPreferenceService gtasksPreferenceService,
                             GtasksMetadata gtasksMetadataFactory, GtasksInvoker gtasksInvoker) {
        this.metadataDao = metadataDao;
        this.gtasksMetadataService = gtasksMetadataService;
        this.taskDao = taskDao;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksMetadataFactory = gtasksMetadataFactory;
        this.gtasksInvoker = gtasksInvoker;
    }

    private abstract class SyncOnSaveOperation {
        abstract public void op(GtasksInvoker invoker) throws IOException;
    }

    private class TaskPushOp extends SyncOnSaveOperation {
        protected Task model;
        protected long creationDate = DateUtilities.now();

        public TaskPushOp(Task model) {
            this.model = model;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            if(DateUtilities.now() - creationDate < 1000) {
                AndroidUtilities.sleepDeep(1000 - (DateUtilities.now() - creationDate));
            }
            pushTaskOnSave(model, model.getMergedValues(), invoker);
        }
    }

    private class MoveOp extends SyncOnSaveOperation {
        protected Metadata metadata;

        public MoveOp(Metadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            pushMetadataOnSave(metadata, invoker);
        }
    }

    private class NotifyOp extends SyncOnSaveOperation {
        private final Semaphore sema;

        public NotifyOp(Semaphore sema) {
            this.sema = sema;
        }

        @Override
        public void op(GtasksInvoker invoker) throws IOException {
            sema.release();
        }
    }

    public void initialize() {
        new OperationPushThread(operationQueue).start();

        taskDao.addListener(new ModelUpdateListener<Task>() {
            @Override
            public void onModelUpdated(final Task model) {
                if(model.checkAndClearTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
                    return;
                }
                if (gtasksPreferenceService.isOngoing() && !model.checkTransitory(TaskService.TRANS_REPEAT_COMPLETE)) { //Don't try and sync changes that occur during a normal sync
                    return;
                }
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken()) {
                    return;
                }
                if (!checkValuesForProperties(setValues, TASK_PROPERTIES)) { //None of the properties we sync were updated
                    return;
                }

                Task toPush = taskDao.fetch(model.getId(), TASK_PROPERTIES);
                operationQueue.offer(new TaskPushOp(toPush));
            }
        });
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

    public void waitUntilEmpty() {
        Semaphore sema = new Semaphore(0);
        operationQueue.offer(new NotifyOp(sema));
        try {
            sema.acquire();
        } catch (InterruptedException e) {
            // Ignored
            Timber.e(e, e.getMessage());
        }
    }

    private static final Property<?>[] TASK_PROPERTIES = { Task.ID, Task.TITLE,
            Task.NOTES, Task.DUE_DATE, Task.COMPLETION_DATE, Task.DELETION_DATE };

    /**
     * Checks to see if any of the values changed are among the properties we sync
     * @return false if none of the properties we sync were changed, true otherwise
     */
    private boolean checkValuesForProperties(ContentValues values, Property<?>[] properties) {
        for (Property<?> property : properties) {
            if (property != Task.ID && values.containsKey(property.name)) {
                return true;
            }
        }
        return false;
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
        if (!checkForToken()) {
            return;
        }

        operationQueue.offer(new MoveOp(metadata));
    }

    /**
     * Synchronize with server when data changes
     */
    public void pushTaskOnSave(Task task, ContentValues values, GtasksInvoker invoker) throws IOException {
        Metadata gtasksMetadata = gtasksMetadataService.getTaskMetadata(task.getId());
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
            String parent = gtasksMetadataService.getRemoteParentId(gtasksMetadata);
            String priorSibling = gtasksMetadataService.getRemoteSiblingId(listId, gtasksMetadata);

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
        String parent = gtasksMetadataService.getRemoteParentId(model);
        String priorSibling = gtasksMetadataService.getRemoteSiblingId(listId, model);

        MoveRequest move = new MoveRequest(invoker, taskId, listId, parent, priorSibling);
        com.google.api.services.tasks.model.Task result = move.push();
        // Update order metadata from result
        if (result != null) {
            model.setValue(GtasksMetadata.GTASKS_ORDER, Long.parseLong(result.getPosition()));
            model.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            metadataDao.saveExisting(model);
        }
    }

    private boolean checkForToken() {
        return gtasksPreferenceService.isLoggedIn();
    }
}
