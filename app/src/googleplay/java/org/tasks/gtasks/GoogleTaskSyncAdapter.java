/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tasks.gtasks;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.utility.Constants;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingAbstractThreadedSyncAdapter;
import org.tasks.injection.SyncAdapterComponent;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.sync.RecordSyncStatusCallback;
import org.tasks.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link GoogleTaskSyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
public class GoogleTaskSyncAdapter extends InjectingAbstractThreadedSyncAdapter {

    private static final String DEFAULT_LIST = "@default"; //$NON-NLS-1$

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject Broadcaster broadcaster;
    @Inject StoreObjectDao storeObjectDao;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject Preferences preferences;
    @Inject GtasksInvoker gtasksInvoker;
    @Inject TaskDao taskDao;
    @Inject MetadataDao metadataDao;
    @Inject GtasksMetadata gtasksMetadataFactory;
    @Inject Tracker tracker;
    @Inject NotificationManager notificationManager;

    public GoogleTaskSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     .
     *
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link android.content.AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     *
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (!account.name.equals(gtasksPreferenceService.getUserName())) {
            Timber.d("Sync not enabled for %s", account);
            syncResult.stats.numAuthExceptions++;
            return;
        }
        Timber.d("%s: start sync", account);
        RecordSyncStatusCallback callback = new RecordSyncStatusCallback(gtasksPreferenceService, broadcaster);
        try {
            callback.started();
            synchronize();
            gtasksPreferenceService.recordSuccessfulSync();
        } catch (UserRecoverableAuthIOException e) {
            Timber.e(e, e.getMessage());
            sendNotification(getContext(), e.getIntent());
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
        } catch (Exception e) {
            tracker.reportException(e);
        } finally {
            callback.finished();
            Timber.d("%s: end sync", account);
        }
    }

    private void sendNotification(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);

        PendingIntent resolve = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setAutoCancel(true)
                .setContentIntent(resolve)
                .setContentTitle(context.getString(R.string.sync_error_permissions))
                .setContentText(context.getString(R.string.common_google_play_services_notification_ticker))
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setTicker(context.getString(R.string.common_google_play_services_notification_ticker));
        notificationManager.notify(Constants.NOTIFICATION_SYNC_ERROR, builder.build());
    }

    private void synchronize() throws IOException {
        pushLocalChanges();

        List<TaskList> gtaskLists = new ArrayList<>();
        String nextPageToken = null;
        do {
            TaskLists remoteLists = gtasksInvoker.allGtaskLists(nextPageToken);
            if (remoteLists == null) {
                break;
            }
            List<TaskList> items = remoteLists.getItems();
            if (items != null) {
                gtaskLists.addAll(items);
            }
            nextPageToken = remoteLists.getNextPageToken();
        } while (nextPageToken != null);
        gtasksListService.updateLists(gtaskLists);
        if (gtasksListService.getList(gtasksPreferenceService.getDefaultList()) == null) {
            gtasksPreferenceService.setDefaultList(null);
        }
        for (final GtasksList list : gtasksListService.getListsToUpdate(gtaskLists)) {
            fetchAndApplyRemoteChanges(list);
        }
    }

    private void pushLocalChanges() throws UserRecoverableAuthIOException {
        List<Task> tasks = taskDao.toList(Query.select(Task.PROPERTIES)
                .join(Join.left(Metadata.TABLE, Criterion.and(MetadataDao.MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY), Task.ID.eq(Metadata.TASK))))
                .where(Criterion.or(Task.MODIFICATION_DATE.gt(GtasksMetadata.LAST_SYNC), GtasksMetadata.ID.eq(""))));
        for (Task task : tasks) {
            try {
                pushTask(task, task.getMergedValues(), gtasksInvoker);
            } catch (UserRecoverableAuthIOException e) {
                throw e;
            } catch (IOException e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    /**
     * Synchronize with server when data changes
     */
    private void pushTask(Task task, ContentValues values, GtasksInvoker invoker) throws IOException {
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
                Timber.e(e, e.getMessage());
                metadataDao.delete(gtasksMetadata.getId());
                return;
            }
        } else {
            String parent = gtasksSyncService.getRemoteParentId(gtasksMetadata);
            String priorSibling = gtasksSyncService.getRemoteSiblingId(listId, gtasksMetadata);

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

    private List<Metadata> getDeleted(long taskId) {
        return metadataDao.toList(Criterion.and(
                MetadataDao.MetadataCriteria.byTaskAndwithKey(taskId, GtasksMetadata.METADATA_KEY),
                MetadataDao.MetadataCriteria.isDeleted()));
    }

    private synchronized void fetchAndApplyRemoteChanges(GtasksList list) throws UserRecoverableAuthIOException {
        String listId = list.getRemoteId();
        long lastSyncDate = list.getLastSync();

        boolean includeDeletedAndHidden = lastSyncDate != 0;
        try {
            List<com.google.api.services.tasks.model.Task> tasks = new ArrayList<>();
            String nextPageToken = null;
            do {
                Tasks taskList = gtasksInvoker.getAllGtasksFromListId(listId, includeDeletedAndHidden,
                        includeDeletedAndHidden, lastSyncDate + 1000L, nextPageToken);
                if (taskList == null) {
                    break;
                }
                List<com.google.api.services.tasks.model.Task> items = taskList.getItems();
                if (items != null) {
                    tasks.addAll(items);
                }
                nextPageToken = taskList.getNextPageToken();
            } while (nextPageToken != null);

            if (!tasks.isEmpty()) {
                for (com.google.api.services.tasks.model.Task t : tasks) {
                    GtasksTaskContainer container = new GtasksTaskContainer(t, listId, GtasksMetadata.createEmptyMetadataWithoutList(AbstractModel.NO_ID));
                    findLocalMatch(container);
                    container.gtaskMetadata.setValue(GtasksMetadata.GTASKS_ORDER, Long.parseLong(t.getPosition()));
                    container.gtaskMetadata.setValue(GtasksMetadata.PARENT_TASK, localIdForGtasksId(t.getParent()));
                    container.gtaskMetadata.setValue(GtasksMetadata.LAST_SYNC, DateUtilities.now() + 1000L);
                    write(container);
                    lastSyncDate = Math.max(lastSyncDate, container.getUpdateTime());
                }
                list.setLastSync(lastSyncDate);
                storeObjectDao.persist(list);
                gtasksTaskListUpdater.correctOrderAndIndentForList(listId);
            }
        } catch (UserRecoverableAuthIOException e) {
            throw e;
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
        }
    }

    private long localIdForGtasksId(String gtasksId) {
        Metadata metadata = getMetadataByGtaskId(gtasksId);
        return metadata == null ? AbstractModel.NO_ID : metadata.getTask();
    }

    private void findLocalMatch(GtasksTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID) {
            return;
        }
        Metadata metadata = getMetadataByGtaskId(remoteTask.gtaskMetadata.getValue(GtasksMetadata.ID));
        if (metadata != null) {
            remoteTask.task.setId(metadata.getValue(Metadata.TASK));
            remoteTask.task.setUuid(taskDao.uuidFromLocalId(remoteTask.task.getId()));
            remoteTask.gtaskMetadata = metadata;
        }
    }

    private Metadata getMetadataByGtaskId(String gtaskId) {
        return metadataDao.getFirst(Query.select(Metadata.PROPERTIES).where(Criterion.and(
                Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.ID.eq(gtaskId))));
    }

    private void write(GtasksTaskContainer task) {
        //  merge astrid dates with google dates

        if(task.task.isSaved()) {
            Task local = taskDao.fetch(task.task.getId(), Task.PROPERTIES);
            if (local == null) {
                task.task.clearValue(Task.ID);
                task.task.clearValue(Task.UUID);
            } else {
                mergeDates(task.task, local);
            }
        } else { // Set default importance and reminders for remotely created tasks
            task.task.setImportance(preferences.getIntegerFromString(
                    R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
            TaskDao.setDefaultReminders(preferences, task.task);
        }
        if (!TextUtils.isEmpty(task.task.getTitle())) {
            task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            task.task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
            saveTaskAndMetadata(task);
        }
    }

    /**
     * Saves a task and its metadata
     */
    private void saveTaskAndMetadata(GtasksTaskContainer task) {
        task.prepareForSaving();
        taskDao.save(task.task);
        synchronizeMetadata(task.task.getId(), task.metadata, GtasksMetadata.METADATA_KEY);
    }

    /**
     * Synchronize metadata for given task id. Deletes rows in database that
     * are not identical to those in the metadata list, creates rows that
     * have no match.
     *
     * @param taskId id of task to perform synchronization on
     * @param metadata list of new metadata items to save
     * @param metadataKey metadata key
     */
    private void synchronizeMetadata(long taskId, ArrayList<Metadata> metadata, String metadataKey) {
        final Set<ContentValues> newMetadataValues = new HashSet<>();
        for(Metadata metadatum : metadata) {
            metadatum.setTask(taskId);
            metadatum.clearValue(Metadata.ID);
            newMetadataValues.add(metadatum.getMergedValues());
        }

        metadataDao.byTaskAndKey(taskId, metadataKey, item -> {
            long id = item.getId();

            // clear item id when matching with incoming values
            item.clearValue(Metadata.ID);
            ContentValues itemMergedValues = item.getMergedValues();
            if(newMetadataValues.contains(itemMergedValues)) {
                newMetadataValues.remove(itemMergedValues);
            } else {
                // not matched. cut it
                metadataDao.delete(id);
            }
        });

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            Metadata item = new Metadata();
            item.mergeWith(values);
            metadataDao.persist(item);
        }
    }


    static void mergeDates(Task remote, Task local) {
        if (remote.hasDueDate() && local.hasDueTime()) {
            DateTime oldDate = newDateTime(local.getDueDate());
            DateTime newDate = newDateTime(remote.getDueDate())
                    .withHourOfDay(oldDate.getHourOfDay())
                    .withMinuteOfHour(oldDate.getMinuteOfHour())
                    .withSecondOfMinute(oldDate.getSecondOfMinute());
            local.setDueDateAdjustingHideUntil(
                    Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDate.getMillis()));
        } else {
            local.setDueDateAdjustingHideUntil(remote.getDueDate());
        }

        remote.setHideUntil(local.getHideUntil());
        remote.setDueDate(local.getDueDate());
    }

    @Override
    protected void inject(SyncAdapterComponent component) {
        component.inject(this);
    }
}
