package com.timsu.astrid.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForSync;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Notifications;
import com.timsu.astrid.utilities.Preferences;

/** A service that synchronizes with Astrid
 *
 * @author timsu
 *
 */
public abstract class SynchronizationService {

    private int id;
    static ProgressDialog progressDialog;
    protected Handler syncHandler;
    public SynchronizationService(int id) {
        this.id = id;
    }

    // called off the UI thread. does some setup
    void synchronizeService(final Activity activity) {
        syncHandler = new Handler();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
        progressDialog.setTitle("Synchronization");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setMessage("Checking Authorization...");
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.show();

        synchronize(activity);
    }

    /** Synchronize with the service */
    protected abstract void synchronize(Activity activity);

    /** Called when user requests a data clear */
    abstract void clearPersonalData(Activity activity);

    /** Get this service's id */
    public int getId() {
        return id;
    }

    /** Gets this service's name */
    abstract String getName();

    // --- utilities

    /** Utility class for showing synchronization errors */
    void showError(final Context context, final Throwable e) {
        Log.e("astrid", "Synchronization Error", e);
        syncHandler.post(new Runnable() {
            @Override
            public void run() {
                if(progressDialog != null)
                    progressDialog.dismiss();

                Resources r = context.getResources();
                DialogUtilities.okDialog(context,
                        r.getString(R.string.sync_error) + " " +
                        e.toString() + " - " + e.getStackTrace()[0], null);
            }
        });
    }

    // --- synchronization logic

    /** interface to assist with synchronization */
    protected interface SynchronizeHelper {
        /** Push the given task to the remote server.
         *
         * @param task task proxy to push
         * @param mapping local/remote mapping.
         */
        void pushTask(TaskProxy task, SyncMapping mapping) throws IOException;

        /** Create a task on the remote server
         *
         * @return primaryTag primary tag of this task. null if no tags exist.
         * @return remote id
         */
        String createTask(String primaryTag) throws IOException;

        /** Fetch remote task. Used to re-read merged tasks
         *
         * @param task TaskProxy of the original task
         * @return new TaskProxy
         */
        TaskProxy refetchTask(TaskProxy task) throws IOException;

        /** Delete the task from the remote server
         *
         * @param mapping mapping to delete
         */
        void deleteTask(SyncMapping mapping) throws IOException;
    }

    /** Helper to synchronize remote tasks with our local database.
     *
     * This initiates the following process:
     * 1. local changes are read
     * 2. remote changes are read
     * 3. local tasks are merged with remote changes and pushed across
     * 4. remote changes are then read in
     *
     * @param remoteTasks remote tasks that have been updated
     * @return local tasks that need to be pushed across
     */
    protected void synchronizeTasks(final Activity activity, List<TaskProxy> remoteTasks,
            SynchronizeHelper helper) throws IOException {
        final SyncStats stats = new SyncStats();
        final StringBuilder log = new StringBuilder();

        syncHandler.post(new Runnable() {
            @Override
            public void run() {
                if(!progressDialog.isShowing())
                    progressDialog.show();
            }
        });

        SyncDataController syncController = Synchronizer.getSyncController(activity);
        TaskController taskController = Synchronizer.getTaskController(activity);
        TagController tagController = Synchronizer.getTagController(activity);
        AlertController alertController = Synchronizer.getAlertController(activity);

        // 1. get data out of the database
        HashSet<SyncMapping> mappings = syncController.getSyncMapping(getId());
        HashSet<TaskIdentifier> activeTasks = taskController.
            getActiveTaskIdentifiers();
        HashSet<TaskIdentifier> allTasks = taskController.
            getAllTaskIdentifiers();
        HashMap<TagIdentifier, TagModelForView> tags =
            tagController.getAllTagsAsMap(activity);

        //  2. build helper data structures
        HashMap<String, SyncMapping> remoteIdToSyncMapping =
            new HashMap<String, SyncMapping>();
        HashMap<TaskIdentifier, SyncMapping> localIdToSyncMapping =
            new HashMap<TaskIdentifier, SyncMapping>();
        HashSet<SyncMapping> localChanges = new HashSet<SyncMapping>();
        HashSet<TaskIdentifier> mappedTasks = new HashSet<TaskIdentifier>();
        for(SyncMapping mapping : mappings) {
            if(mapping.isUpdated())
                localChanges.add(mapping);
            remoteIdToSyncMapping.put(mapping.getRemoteId(), mapping);
            localIdToSyncMapping.put(mapping.getTask(), mapping);
            mappedTasks.add(mapping.getTask());
        }

        // 3. build map of remote tasks
        HashMap<TaskIdentifier, TaskProxy> remoteChangeMap =
            new HashMap<TaskIdentifier, TaskProxy>();
        HashMap<String, TaskProxy> newRemoteTasks = new HashMap<String, TaskProxy>();
        for(TaskProxy remoteTask : remoteTasks) {
            if(remoteIdToSyncMapping.containsKey(remoteTask.getRemoteId())) {
                SyncMapping mapping = remoteIdToSyncMapping.get(remoteTask.getRemoteId());
                remoteChangeMap.put(mapping.getTask(), remoteTask);
            } else if(remoteTask.name != null){
                newRemoteTasks.put(remoteTask.name, remoteTask);
            }
        }

        // 4. CREATE: grab tasks without a sync mapping and create them remotely
        log.append(">> on remote server:\n");
        syncHandler.post(new ProgressLabelUpdater("Sending locally created tasks"));
        HashSet<TaskIdentifier> newlyCreatedTasks = new HashSet<TaskIdentifier>(activeTasks);
        newlyCreatedTasks.removeAll(mappedTasks);
        for(TaskIdentifier taskId : newlyCreatedTasks) {
            TaskModelForSync task = taskController.fetchTaskForSync(taskId);

            /* If there exists an incoming remote task with the same name and
             * no mapping, we don't want to create this on the remote server.
             * Instead, we create a mapping and do an update. */
            if(newRemoteTasks.containsKey(task.getName())) {
                TaskProxy remoteTask = newRemoteTasks.get(task.getName());
                SyncMapping mapping = new SyncMapping(taskId, getId(),
                        remoteTask.getRemoteId());
                syncController.saveSyncMapping(mapping);
                localChanges.add(mapping);
                remoteChangeMap.put(taskId, remoteTask);
                localIdToSyncMapping.put(taskId, mapping);
                continue;
            }

            // grab the primary tag for this task
            LinkedList<TagIdentifier> taskTags =
                tagController.getTaskTags(activity, taskId);
            String listName = null;
            if(taskTags.size() > 0) {
                listName = tags.get(taskTags.get(0)).getName();
                if(listName.startsWith(TagModelForView.HIDDEN_FROM_MAIN_LIST_PREFIX))
                    listName = listName.substring(1);
            }
            String remoteId = helper.createTask(listName);
            SyncMapping mapping = new SyncMapping(taskId, getId(), remoteId);
            syncController.saveSyncMapping(mapping);

            TaskProxy localTask = new TaskProxy(getId(), remoteId, false);
            localTask.readFromTaskModel(task);
            helper.pushTask(localTask, mapping);

            // update stats
            log.append("added " + task.getName() + "\n");
            stats.remoteCreatedTasks++;
            syncHandler.post(new ProgressUpdater(stats.remoteCreatedTasks,
                    newlyCreatedTasks.size()));
        }

        // 5. DELETE: find deleted tasks and remove them from the list
        syncHandler.post(new ProgressLabelUpdater("Sending locally deleted tasks"));
        HashSet<TaskIdentifier> deletedTasks = new HashSet<TaskIdentifier>(
                mappedTasks);
        deletedTasks.removeAll(allTasks);
        for(TaskIdentifier taskId : deletedTasks) {
            SyncMapping mapping = localIdToSyncMapping.get(taskId);
            syncController.deleteSyncMapping(mapping);
            helper.deleteTask(mapping);

            // remove it from data structures
            localChanges.remove(mapping);
            remoteIdToSyncMapping.remove(mapping);
            remoteChangeMap.remove(taskId);

            // update stats
            log.append("deleted id #" + taskId.getId() + "\n");
            stats.remoteDeletedTasks++;
            syncHandler.post(new ProgressUpdater(stats.remoteDeletedTasks,
                    deletedTasks.size()));
        }

        // 6. UPDATE: for each updated local task
        syncHandler.post(new ProgressLabelUpdater("Sending locally edited tasks"));
        for(SyncMapping mapping : localChanges) {
            TaskProxy localTask = new TaskProxy(getId(), mapping.getRemoteId(),
                    false);
            TaskModelForSync task = taskController.fetchTaskForSync(
                    mapping.getTask());
            localTask.readFromTaskModel(task);

            // if there is a conflict, merge
            TaskProxy remoteConflict = null;
            if(remoteChangeMap.containsKey(mapping.getTask())) {
                remoteConflict = remoteChangeMap.get(mapping.getTask());
                localTask.mergeWithOther(remoteConflict);
                stats.mergedTasks++;
                log.append("merged " + task.getName() + "\n");
            } else {
                log.append("updated " + task.getName() + "\n");
            }

            try {
                helper.pushTask(localTask, mapping);
            } catch (Exception e) {
                Log.e("astrid", "Exception pushing task", e);
                continue;
            }

            // re-fetch remote task
            if(remoteConflict != null) {
                TaskProxy newTask = helper.refetchTask(remoteConflict);
                remoteTasks.remove(remoteConflict);
                remoteTasks.add(newTask);
            } else
                stats.remoteUpdatedTasks++;
            syncHandler.post(new ProgressUpdater(stats.remoteUpdatedTasks,
                    localChanges.size()));
        }

        // 7. REMOTE SYNC load remote information
        log.append(">> on astrid:\n");
        syncHandler.post(new ProgressLabelUpdater("Updating local tasks"));
        for(TaskProxy remoteTask : remoteTasks) {
            SyncMapping mapping = null;
            TaskModelForSync task = null;

            // if it's new, create a new task model
            if(!remoteIdToSyncMapping.containsKey(remoteTask.getRemoteId())) {
                // if it's new & deleted, forget about it
                if(remoteTask.isDeleted()) {
                    continue;
                }

                task = taskController.searchForTaskForSync(remoteTask.name);
                if(task == null) {
                    task = new TaskModelForSync();
                    setupTaskDefaults(activity, task);
                    log.append("added " + remoteTask.name + "\n");
                } else {
                    mapping = localIdToSyncMapping.get(task.getTaskIdentifier());
                    log.append("merged " + remoteTask.name + "\n");
                }
            } else {
                mapping = remoteIdToSyncMapping.get(remoteTask.getRemoteId());
                if(remoteTask.isDeleted()) {
                    taskController.deleteTask(mapping.getTask());
                    syncController.deleteSyncMapping(mapping);
                    log.append("deleted " + remoteTask.name + "\n");
                    stats.localDeletedTasks++;
                    continue;
                }

                log.append("updated " + remoteTask.name + "\n");
                task = taskController.fetchTaskForSync(
                        mapping.getTask());
            }

            // save the data
            remoteTask.writeToTaskModel(task);
            taskController.saveTask(task);

            // save tag
            if(remoteTask.tags != null && remoteTask.tags.length > 0) {
                String tag = remoteTask.tags[0];
                TagIdentifier tagIdentifier = null;
                for(TagModelForView tagModel : tags.values()) {
                    String tagName = tagModel.getName();
                    if(tagName.startsWith(TagModelForView.HIDDEN_FROM_MAIN_LIST_PREFIX))
                        tagName = tagName.substring(1);
                    if(tagName.equalsIgnoreCase(tag)) {
                        tagIdentifier = tagModel.getTagIdentifier();
                        break;
                    }
                }
                try {
                    if(tagIdentifier == null)
                        tagIdentifier = tagController.createTag(tag);
                    tagController.addTag(task.getTaskIdentifier(),
                            tagIdentifier);
                } catch (Exception e) {
                    // tag already exists or something
                }
            }
            stats.localUpdatedTasks++;

            if(mapping == null) {
                mapping = new SyncMapping(task.getTaskIdentifier(), remoteTask);
                syncController.saveSyncMapping(mapping);
                stats.localCreatedTasks++;
            }

            Notifications.updateAlarm(activity, taskController, alertController,
                    task);
        }
        stats.localUpdatedTasks -= stats.localCreatedTasks;

        syncController.clearUpdatedTaskList(getId());
        syncHandler.post(new Runnable() {
            @Override
            public void run() {
                stats.showDialog(activity, log.toString());
            }
        });
    }

    /** Set up defaults from preferences for this task */
    private void setupTaskDefaults(Activity activity, TaskModelForSync task) {
        Integer reminder = Preferences.getDefaultReminder(activity);
        if(reminder != null)
            task.setNotificationIntervalSeconds(24*3600*reminder);
    }

    // --- helper classes

    protected class SyncStats {
        int localCreatedTasks = 0;
        int localUpdatedTasks = 0;
        int localDeletedTasks = 0;

        int mergedTasks = 0;

        int remoteCreatedTasks = 0;
        int remoteUpdatedTasks = 0;
        int remoteDeletedTasks = 0;

        /** Display a dialog with statistics */
        public void showDialog(final Activity activity, String log) {
            progressDialog.hide();
            Dialog.OnClickListener finishListener = new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,
                        int which) {
                    Synchronizer.continueSynchronization(activity);
                }
            };

            // nothing updated
            if(localCreatedTasks + localUpdatedTasks + localDeletedTasks +
                    mergedTasks + remoteCreatedTasks + remoteDeletedTasks +
                    remoteUpdatedTasks == 0) {
                if(!Synchronizer.isAutoSync())
                    DialogUtilities.okDialog(activity, "Sync: Up to date!", finishListener);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append(" Sync Results:"); // TODO i18n
            sb.append("\n\n");
            sb.append(log);
            sb.append("\n--- Summary: Astrid Tasks ---");
            if(localCreatedTasks > 0)
                sb.append("\nCreated: " + localCreatedTasks);
            if(localUpdatedTasks > 0)
                sb.append("\nUpdated: " + localUpdatedTasks);
            if(localDeletedTasks > 0)
                sb.append("\nDeleted: " + localDeletedTasks);

            if(mergedTasks > 0)
                sb.append("\n\nMerged: " + localCreatedTasks);

            sb.append("\n\n--- Summary: Remote Server ---");
            if(remoteCreatedTasks > 0)
                sb.append("\nCreated: " + remoteCreatedTasks);
            if(remoteUpdatedTasks > 0)
                sb.append("\nUpdated: " + remoteUpdatedTasks);
            if(remoteDeletedTasks > 0)
                sb.append("\nDeleted: " + remoteDeletedTasks);

            sb.append("\n");

            DialogUtilities.okDialog(activity, sb.toString(), finishListener);
        }
    }

    protected class ProgressUpdater implements Runnable {
        int step, outOf;
        public ProgressUpdater(int step, int outOf) {
            this.step = step;
            this.outOf = outOf;
        }
        public void run() {
            progressDialog.setProgress(100*step/outOf);
        }
    }

    protected class ProgressLabelUpdater implements Runnable {
        String label;
        public ProgressLabelUpdater(String label) {
            this.label = label;
        }
        public void run() {
            progressDialog.setMessage(label);
            progressDialog.setProgress(0);
        }
    }
}
