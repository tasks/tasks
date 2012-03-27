package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.CreateRequest;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.MoveRequest;
import com.todoroo.astrid.service.MetadataService;

public final class GtasksSyncService {

    private static final String DEFAULT_LIST = "@default"; //$NON-NLS-1$
    @Autowired MetadataService metadataService;
    @Autowired MetadataDao metadataDao;
    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired TaskDao taskDao;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    public GtasksSyncService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<SyncOnSaveOperation>();

    private abstract class SyncOnSaveOperation { /**/ }

    private class TaskPushOp extends SyncOnSaveOperation {
        protected Task model;
        protected long creationDate = DateUtilities.now();

        public TaskPushOp(Task model) {
            this.model = model;
        }
    }

    class MoveOp extends SyncOnSaveOperation {
        protected Metadata metadata;

        public MoveOp(Metadata metadata) {
            this.metadata = metadata;
        }
    }


    @SuppressWarnings("nls")
    public void initialize() {
        new Thread(new Runnable() {
        public void run() {
               while (true) {
                   SyncOnSaveOperation op;
                   try {
                       op = operationQueue.take();
                   } catch (InterruptedException e) {
                       continue;
                   }
                   try {
                       if (!gtasksPreferenceService.isOngoing()) {
                           GtasksInvoker invoker = new GtasksInvoker(gtasksPreferenceService.getToken());
                           if (op instanceof TaskPushOp) {
                               TaskPushOp taskPush = (TaskPushOp)op;
                               if(DateUtilities.now() - taskPush.creationDate < 1000)
                                   AndroidUtilities.sleepDeep(1000 - (DateUtilities.now() - taskPush.creationDate));
                               pushTaskOnSave(taskPush.model, taskPush.model.getMergedValues(), invoker, false);
                           } else if (op instanceof MoveOp) {
                               MoveOp move = (MoveOp)op;
                               pushMetadataOnSave(move.metadata, invoker);
                           }
                       }
                   } catch (IOException e) {
                       Log.w("gtasks-sync-error", "Sync on save failed", e);
                   }
               }
           }
        }).start();

        taskDao.addListener(new ModelUpdateListener<Task>() {
            public void onModelUpdated(final Task model) {
                if(model.checkAndClearTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC))
                    return;
                if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
                    return;
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken())
                    return;
                if (!checkValuesForProperties(setValues, TASK_PROPERTIES)) //None of the properties we sync were updated
                    return;

                Task toPush = taskDao.fetch(model.getId(), TASK_PROPERTIES);
                if (toPush.getValue(Task.USER_ID) != Task.USER_ID_SELF)
                    return;

                operationQueue.offer(new TaskPushOp(toPush));
            }
        });
    }

    private static final Property<?>[] TASK_PROPERTIES = { Task.ID, Task.TITLE,
            Task.NOTES, Task.DUE_DATE, Task.COMPLETION_DATE, Task.DELETION_DATE };

    /**
     * Checks to see if any of the values changed are among the properties we sync
     * @param values
     * @param properties
     * @return false if none of the properties we sync were changed, true otherwise
     */
    private boolean checkValuesForProperties(ContentValues values, Property<?>[] properties) {
        for (Property<?> property : properties) {
            if (property != Task.ID && values.containsKey(property.name))
                return true;
        }
        return false;
    }


    public void triggerMoveForMetadata(final Metadata metadata) {
        if (metadata == null)
            return;
        if (metadata.checkAndClearTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC))
            return;
        if (!metadata.getValue(Metadata.KEY).equals(GtasksMetadata.METADATA_KEY)) //Don't care about non-gtasks metadata
            return;
        if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
            return;
        if (!checkForToken())
            return;

        operationQueue.offer(new MoveOp(metadata));
    }

    /**
     * Synchronize with server when data changes
     */
    public void pushTaskOnSave(Task task, ContentValues values, GtasksInvoker invoker, boolean sleep) throws IOException {
        if (sleep)
            AndroidUtilities.sleepDeep(1000L); //Wait for metadata to be saved

        Metadata gtasksMetadata = gtasksMetadataService.getTaskMetadata(task.getId());
        com.google.api.services.tasks.model.Task remoteModel = null;
        boolean newlyCreated = false;

        String remoteId = null;
        String listId = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        if (listId == null) {
            com.google.api.services.tasks.model.TaskList defaultList = invoker.getGtaskList(DEFAULT_LIST);
            if (defaultList != null) {
                listId = defaultList.getId();
                Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, listId);
            } else {
                listId = DEFAULT_LIST;
            }
        }

        if (gtasksMetadata == null || !gtasksMetadata.containsNonNullValue(GtasksMetadata.ID) ||
                TextUtils.isEmpty(gtasksMetadata.getValue(GtasksMetadata.ID))) { //Create case
            if (gtasksMetadata == null) {
                gtasksMetadata = GtasksMetadata.createEmptyMetadata(task.getId());
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
                (!values.containsKey(Task.TITLE.name) || TextUtils.isEmpty(task.getValue(Task.TITLE)) || task.getValue(Task.DELETION_DATE) > 0)) {
            return;
        }

        //Update the remote model's changed properties
        if (values.containsKey(Task.DELETION_DATE.name) && task.isDeleted()) {
            remoteModel.setDeleted(true);
        }

        if (values.containsKey(Task.TITLE.name)) {
            remoteModel.setTitle(task.getValue(Task.TITLE));
        }
        if (values.containsKey(Task.NOTES.name)) {
            remoteModel.setNotes(task.getValue(Task.NOTES));
        }
        if (values.containsKey(Task.DUE_DATE.name) && task.hasDueDate()) {
            remoteModel.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(task.getValue(Task.DUE_DATE)));
        }
        if (values.containsKey(Task.COMPLETION_DATE.name)) {
            if (task.isCompleted()) {
                remoteModel.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.getValue(Task.COMPLETION_DATE)));
                remoteModel.setStatus("completed"); //$NON-NLS-1$
            } else {
                remoteModel.setCompleted(null);
                remoteModel.setStatus("needsAction"); //$NON-NLS-1$
            }
        }

        if (!newlyCreated) {
            invoker.updateGtask(listId, remoteModel);
        } else {
            String parent = gtasksMetadataService.getRemoteParentId(gtasksMetadata);
            String priorSibling = gtasksMetadataService.getRemoteSiblingId(listId, gtasksMetadata);

            CreateRequest create = new CreateRequest(invoker, listId, remoteModel, parent, priorSibling);
            com.google.api.services.tasks.model.Task created = create.executePush();

            if (created != null) {
                //Update the metadata for the newly created task
                gtasksMetadata.setValue(GtasksMetadata.ID, created.getId());
                gtasksMetadata.setValue(GtasksMetadata.LIST_ID, listId);
            } else return;
        }

        task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        gtasksMetadata.setValue(GtasksMetadata.LAST_SYNC, DateUtilities.now() + 1000L);
        metadataService.save(gtasksMetadata);
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
        if (!gtasksPreferenceService.isLoggedIn())
            return false;
        return true;
    }
}
