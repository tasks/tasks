package com.timsu.astrid.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForSync;
import com.timsu.astrid.utilities.DialogUtilities;

/** A service that synchronizes with Astrid
 *
 * @author timsu
 *
 */
public abstract class SynchronizationService {

    private int id;

    public SynchronizationService(int id) {
        this.id = id;
    }

    /** Synchronize with the service */
    abstract void synchronize(Activity activity);

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
    static void showError(Context context, Throwable e) {
        Log.e("astrid", "Synchronization Error", e);

        Resources r = context.getResources();
        DialogUtilities.okDialog(context,
                r.getString(R.string.sync_error) + " " +
                e.getLocalizedMessage(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing?
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
         * @return remote id
         */
        String createTask() throws IOException;

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
    protected void synchronizeTasks(Activity activity, List<TaskProxy> remoteTasks,
            SynchronizeHelper helper) throws IOException {
        SyncStats stats = new SyncStats();

        // get data out of the database
        Set<SyncMapping> mappings = Synchronizer.getSyncController().getSyncMapping(getId());
        Set<TaskIdentifier> localTasks = Synchronizer.getTaskController().getAllTaskIdentifiers();

        //  build local maps / lists
        Map<String, SyncMapping> remoteIdToSyncMapping =
            new HashMap<String, SyncMapping>();
        Map<TaskIdentifier, SyncMapping> localIdToSyncMapping =
            new HashMap<TaskIdentifier, SyncMapping>();
        Set<SyncMapping> localChanges = new HashSet<SyncMapping>();
        Set<TaskIdentifier> mappedTasks = new HashSet<TaskIdentifier>();
        for(SyncMapping mapping : mappings) {
            if(mapping.isUpdated())
                localChanges.add(mapping);
            remoteIdToSyncMapping.put(mapping.getRemoteId(), mapping);
            localIdToSyncMapping.put(mapping.getTask(), mapping);
            mappedTasks.add(mapping.getTask());
        }

        // build remote map
        Map<TaskIdentifier, TaskProxy> remoteChangeMap =
            new HashMap<TaskIdentifier, TaskProxy>();
        for(TaskProxy remoteTask : remoteTasks) {
            if(remoteIdToSyncMapping.containsKey(remoteTask.getRemoteId())) {
                SyncMapping mapping = remoteIdToSyncMapping.get(remoteTask.getRemoteId());
                remoteChangeMap.put(mapping.getTask(), remoteTask);
            }
        }

        // grab tasks without a sync mapping and create them remotely
        Set<TaskIdentifier> newlyCreatedTasks = new HashSet<TaskIdentifier>(localTasks);
        newlyCreatedTasks.removeAll(mappedTasks);
        for(TaskIdentifier taskId : newlyCreatedTasks) {
            String remoteId = helper.createTask();
            stats.remoteCreatedTasks++;
            SyncMapping mapping = new SyncMapping(taskId, getId(), remoteId);
            Synchronizer.getSyncController().saveSyncMapping(mapping);

            // add it to data structures
            localChanges.add(mapping);
        }

        // find deleted tasks and remove them from the list
        Set<TaskIdentifier> deletedTasks = new HashSet<TaskIdentifier>(mappedTasks);
        deletedTasks.removeAll(localTasks);
        for(TaskIdentifier taskId : deletedTasks) {
            stats.remoteDeletedTasks++;
            SyncMapping mapping = localIdToSyncMapping.get(taskId);
            Synchronizer.getSyncController().deleteSyncMapping(mapping);
            helper.deleteTask(mapping);

            // remove it from data structures
            localChanges.remove(mapping);
            remoteIdToSyncMapping.remove(mapping);
            remoteChangeMap.remove(taskId);
        }

        // for each updated local task
        for(SyncMapping mapping : localChanges) {
            TaskProxy localTask = new TaskProxy(getId(), mapping.getRemoteId(), false);
            TaskModelForSync task = Synchronizer.getTaskController().fetchTaskForSync(
                    mapping.getTask());
            localTask.readFromTaskModel(task);

            // if there is a conflict, merge
            TaskProxy remoteConflict = null;
            if(remoteChangeMap.containsKey(mapping.getTask())) {
                remoteConflict = remoteChangeMap.get(mapping.getTask());
                localTask.mergeWithOther(remoteConflict);
                stats.mergedTasks++;
            }

            helper.pushTask(localTask, mapping);

            // re-fetch remote task
            if(remoteConflict != null) {
                TaskProxy newTask = helper.refetchTask(remoteConflict);
                remoteTasks.remove(remoteConflict);
                remoteTasks.add(newTask);
            }
            stats.remoteUpdatedTasks++;
        }
        stats.remoteUpdatedTasks -= stats.remoteCreatedTasks;

        // load remote information
        for(TaskProxy remoteTask : remoteTasks) {
            SyncMapping mapping = null;
            TaskModelForSync task = null;

            // if it's new, create a new task model
            if(!remoteIdToSyncMapping.containsKey(remoteTask.getRemoteId())) {
                // if it's new & deleted, forget about it
                if(remoteTask.isDeleted()) {
                    continue;
                }

                task = new TaskModelForSync();
                stats.localCreatedTasks++;
            } else {
                mapping = remoteIdToSyncMapping.get(remoteTask.getRemoteId());
                if(remoteTask.isDeleted()) {
                    Synchronizer.getTaskController().deleteTask(mapping.getTask());
                    Synchronizer.getSyncController().deleteSyncMapping(mapping);
                    stats.localDeletedTasks++;
                    continue;
                }

                task = Synchronizer.getTaskController().fetchTaskForSync(
                        mapping.getTask());
            }

            // save the data
            remoteTask.writeToTaskModel(task);
            Synchronizer.getTaskController().saveTask(task);
            stats.localUpdatedTasks++;

            if(mapping == null) {
                mapping = new SyncMapping(task.getTaskIdentifier(), remoteTask);
                Synchronizer.getSyncController().saveSyncMapping(mapping);
            }
        }
        stats.localUpdatedTasks -= stats.localCreatedTasks;

        Synchronizer.getSyncController().clearUpdatedTaskList(getId());
        stats.showDialog(activity);
    }

    // --- helper classes

    private class SyncStats {
        int localCreatedTasks = 0;
        int localUpdatedTasks = 0;
        int localDeletedTasks = 0;

        int mergedTasks = 0;

        int remoteCreatedTasks = 0;
        int remoteUpdatedTasks = 0;
        int remoteDeletedTasks = 0;

        /** Display a dialog with statistics */
        public void showDialog(Context context) {
            if(equals(new SyncStats())) // i.e. no change
                return;

            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append(" Sync Results:"); // TODO i18n
            sb.append("\n\nLocal ---");
            if(localCreatedTasks > 0)
                sb.append("\nCreated: " + localCreatedTasks);
            if(localUpdatedTasks > 0)
                sb.append("\nUpdated: " + localUpdatedTasks);
            if(localDeletedTasks > 0)
                sb.append("\nDeleted: " + localDeletedTasks);

            if(mergedTasks > 0)
                sb.append("\n\nMerged: " + localCreatedTasks);

            sb.append("\n\nRemote ---");
            if(remoteCreatedTasks > 0)
                sb.append("\nCreated: " + remoteCreatedTasks);
            if(remoteUpdatedTasks > 0)
                sb.append("\nUpdated: " + remoteUpdatedTasks);
            if(remoteDeletedTasks > 0)
                sb.append("\nDeleted: " + remoteDeletedTasks);

            DialogUtilities.okDialog(context, sb.toString(), null);
        }
    }
}
