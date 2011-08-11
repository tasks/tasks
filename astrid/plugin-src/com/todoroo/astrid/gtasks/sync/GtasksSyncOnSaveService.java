package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.ContentValues;
import android.text.TextUtils;

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
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GoogleTasksException;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.gtasks.api.MoveRequest;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.utility.Flags;

public final class GtasksSyncOnSaveService {

    @Autowired MetadataService metadataService;
    @Autowired MetadataDao metadataDao;
    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired TaskDao taskDao;
    @Autowired GtasksPreferenceService gtasksPreferenceService;
    @Autowired GtasksTaskListUpdater gtasksTaskListUpdater;

    public GtasksSyncOnSaveService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private final LinkedBlockingQueue<SyncOnSaveOperation> operationQueue = new LinkedBlockingQueue<SyncOnSaveOperation>();

    private abstract class SyncOnSaveOperation { /**/ }

    private class TaskPushOp extends SyncOnSaveOperation {
        protected Task model;

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
                       if (syncOnSaveEnabled() && !gtasksPreferenceService.isOngoing()) {
                           if (op instanceof TaskPushOp) {
                               TaskPushOp taskPush = (TaskPushOp)op;
                               pushTaskOnSave(taskPush.model, taskPush.model.getSetValues());
                           } else if (op instanceof MoveOp) {
                               MoveOp move = (MoveOp)op;
                               pushMetadataOnSave(move.metadata);
                           }
                       }
                   } catch (IOException e){
                       System.err.println("Sync on save failed"); //$NON-NLS-1$
                   }
               }
           }
        }).start();

        taskDao.addListener(new ModelUpdateListener<Task>() {
            public void onModelUpdated(final Task model) {
                if (!syncOnSaveEnabled())
                    return;
                if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
                    return;
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken())
                    return;
                if (!checkValuesForProperties(setValues, TASK_PROPERTIES)) //None of the properties we sync were updated
                    return;
                if(Flags.checkAndClear(Flags.GTASKS_SUPPRESS_SYNC))
                    return;

                operationQueue.offer(new TaskPushOp((Task)model.clone()));
            }
        });
    }

    private static final Property<?>[] TASK_PROPERTIES =
        { Task.TITLE,
          Task.NOTES,
          Task.DUE_DATE,
          Task.COMPLETION_DATE,
          Task.DELETION_DATE };

    /**
     * Checks to see if any of the values changed are among the properties we sync
     * @param values
     * @param properties
     * @return false if none of the properties we sync were changed, true otherwise
     */
    private boolean checkValuesForProperties(ContentValues values, Property<?>[] properties) {
        for (Property<?> property : properties) {
            if (values.containsKey(property.name))
                return true;
        }
        return false;
    }


    public void triggerMoveForMetadata(final Metadata metadata) {
        if (!syncOnSaveEnabled())
            return;
        if (!metadata.getValue(Metadata.KEY).equals(GtasksMetadata.METADATA_KEY)) //Don't care about non-gtasks metadata
            return;
        if (gtasksPreferenceService.isOngoing()) //Don't try and sync changes that occur during a normal sync
            return;
        if (!checkForToken())
            return;
        if (Flags.checkAndClear(Flags.GTASKS_SUPPRESS_SYNC))
            return;

        operationQueue.offer(new MoveOp(metadata));
    }

    /**
     * Synchronize with server when data changes
     */
    private void pushTaskOnSave(Task task, ContentValues values) throws IOException {
        AndroidUtilities.sleepDeep(1000L); //Wait for metadata to be saved

        Metadata gtasksMetadata = gtasksMetadataService.getTaskMetadata(task.getId());
        com.google.api.services.tasks.v1.model.Task remoteModel = null;
        boolean newlyCreated = false;

        //Initialize the gtasks api service
        String token = gtasksPreferenceService.getToken();
        token = GtasksTokenValidator.validateAuthToken(token);
        if (token == null) {
            throw new GoogleTasksException("Failed to establish connection for sync on save"); //$NON-NLS-1$
        }
        gtasksPreferenceService.setToken(token);
        GtasksService gtasksService = new GtasksService(token);

        String remoteId = null;
        String listId = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        if (listId == null) {
            listId = gtasksService.getGtaskList("@default").id; //$NON-NLS-1$
            Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, listId);
        }

        if (gtasksMetadata == null || !gtasksMetadata.containsNonNullValue(GtasksMetadata.ID) ||
                TextUtils.isEmpty(gtasksMetadata.getValue(GtasksMetadata.ID))) { //Create case
            if (gtasksMetadata == null) {
                gtasksMetadata = GtasksMetadata.createEmptyMetadata(task.getId());
            }
            if (gtasksMetadata.containsNonNullValue(GtasksMetadata.LIST_ID)) {
                listId = gtasksMetadata.getValue(GtasksMetadata.LIST_ID);
            }

            remoteModel = new com.google.api.services.tasks.v1.model.Task();
            newlyCreated = true;
        } else { //update case
            remoteId = gtasksMetadata.getValue(GtasksMetadata.ID);
            listId = gtasksMetadata.getValue(GtasksMetadata.LIST_ID);
            remoteModel = gtasksService.getGtask(listId, remoteId);
        }

        //If task was newly created but without a title, don't sync--we're in the middle of
        //creating a task which may end up being cancelled
        if (newlyCreated &&
                (!values.containsKey(Task.TITLE.name) || TextUtils.isEmpty(task.getValue(Task.TITLE)))) {
            return;
        }

        //Update the remote model's changed properties
        if (values.containsKey(Task.DELETION_DATE.name) && task.isDeleted()) {
            remoteModel.deleted = true;
        }

        if (values.containsKey(Task.TITLE.name)) {
            remoteModel.title = task.getValue(Task.TITLE);
        }
        if (values.containsKey(Task.NOTES.name)) {
            remoteModel.notes = task.getValue(Task.NOTES);
        }
        if (values.containsKey(Task.DUE_DATE.name)) {
            remoteModel.due = GtasksApiUtilities.unixTimeToGtasksDate(task.getValue(Task.DUE_DATE));
        }
        if (values.containsKey(Task.COMPLETION_DATE.name)) {
            if (task.isCompleted()) {
                remoteModel.completed = GtasksApiUtilities.unixTimeToGtasksDate(task.getValue(Task.COMPLETION_DATE));
                remoteModel.status = "completed"; //$NON-NLS-1$
            } else {
                remoteModel.completed = null;
                remoteModel.status = "needsAction"; //$NON-NLS-1$
            }
        }

        if (!newlyCreated) {
            gtasksService.updateGtask(listId, remoteModel);
        } else {
            String parent = gtasksMetadataService.getRemoteParentId(gtasksMetadata);
            String priorSibling = gtasksMetadataService.getRemoteSiblingId(listId, gtasksMetadata);

            try { //Make sure the parent task exists on the target list
                if (parent != null) {
                    com.google.api.services.tasks.v1.model.Task remoteParent = gtasksService.getGtask(listId, parent);
                    if (remoteParent == null || (remoteParent.deleted != null && remoteParent.deleted))
                        parent = null;
                }
            } catch (IOException e) {
                parent = null;
            }

            try {
                if (priorSibling != null) {
                    com.google.api.services.tasks.v1.model.Task remoteSibling = gtasksService.getGtask(listId, priorSibling);
                    if (remoteSibling == null || (remoteSibling.deleted != null && remoteSibling.deleted))
                        priorSibling = null;
                }
            } catch (IOException e) {
                priorSibling = null;
            }


            com.google.api.services.tasks.v1.model.Task created = gtasksService.createGtask(listId, remoteModel, parent, priorSibling);

            //Update the metadata for the newly created task
            gtasksMetadata.setValue(GtasksMetadata.ID, created.id);
            gtasksMetadata.setValue(GtasksMetadata.LIST_ID, listId);
            metadataService.save(gtasksMetadata);
        }

        task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        task.setValue(Task.LAST_SYNC, DateUtilities.now());
        Flags.set(Flags.GTASKS_SUPPRESS_SYNC);
        taskDao.saveExisting(task);
    }

    private void pushMetadataOnSave(Metadata model) throws IOException {
        AndroidUtilities.sleepDeep(1000L);
        //Initialize the gtasks api service
        String token = gtasksPreferenceService.getToken();
        token = GtasksTokenValidator.validateAuthToken(token);
        if (token == null) {
            throw new GoogleTasksException("Failed to establish connection for sync on save"); //$NON-NLS-1$
        }
        gtasksPreferenceService.setToken(token);
        GtasksService gtasksService = new GtasksService(token);

        String taskId = model.getValue(GtasksMetadata.ID);
        String listId = model.getValue(GtasksMetadata.LIST_ID);
        String parent = gtasksMetadataService.getRemoteParentId(model);
        String priorSibling = gtasksMetadataService.getRemoteSiblingId(listId, model);

        try { //Make sure the parent task exists on the target list
            if (parent != null) {
                com.google.api.services.tasks.v1.model.Task remoteParent = gtasksService.getGtask(listId, parent);
                if (remoteParent == null || (remoteParent.deleted != null && remoteParent.deleted))
                    parent = null;
            }
        } catch (IOException e) {
            parent = null;
        }

        try {
            if (priorSibling != null) {
                com.google.api.services.tasks.v1.model.Task remoteSibling = gtasksService.getGtask(listId, priorSibling);
                if (remoteSibling == null || (remoteSibling.deleted != null && remoteSibling.deleted))
                    priorSibling = null;
            }
        } catch (IOException e) {
            priorSibling = null;
        }

        MoveRequest move = new MoveRequest(gtasksService, taskId, listId, parent, priorSibling);
        move.executePush();
    }

    private boolean syncOnSaveEnabled() {
        return Preferences.getBoolean(gtasksPreferenceService.getSyncOnSaveKey(), false);
    }

    private boolean checkForToken() {
        if (!gtasksPreferenceService.isLoggedIn())
            return false;
        return true;
    }
}
