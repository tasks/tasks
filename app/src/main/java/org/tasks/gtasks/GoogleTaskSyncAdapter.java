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
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.utility.Constants;

import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.InjectingAbstractThreadedSyncAdapter;
import org.tasks.injection.SyncAdapterComponent;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.sync.RecordSyncStatusCallback;
import org.tasks.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    @Inject LocalBroadcastManager localBroadcastManager;
    @Inject GoogleTaskListDao googleTaskListDao;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject Preferences preferences;
    @Inject GtasksInvoker gtasksInvoker;
    @Inject TaskDao taskDao;
    @Inject Tracker tracker;
    @Inject NotificationManager notificationManager;
    @Inject GoogleTaskDao googleTaskDao;
    @Inject TaskCreator taskCreator;
    @Inject DefaultFilterProvider defaultFilterProvider;

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
        RecordSyncStatusCallback callback = new RecordSyncStatusCallback(gtasksPreferenceService, localBroadcastManager);
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT).setAutoCancel(true)
                .setContentIntent(resolve)
                .setContentTitle(context.getString(R.string.sync_error_permissions))
                .setContentText(context.getString(R.string.common_google_play_services_notification_ticker))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_warning_white_24dp)
                .setTicker(context.getString(R.string.common_google_play_services_notification_ticker));
        notificationManager.notify(Constants.NOTIFICATION_SYNC_ERROR, builder, true, false, false);
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
        Filter defaultRemoteList = defaultFilterProvider.getDefaultRemoteList();
        if (defaultRemoteList instanceof GtasksFilter) {
            GoogleTaskList list = gtasksListService.getList(((GtasksFilter) defaultRemoteList).getRemoteId());
            if (list == null) {
                preferences.setString(R.string.p_default_remote_list, null);
            }
        }
        for (final GoogleTaskList list : gtasksListService.getListsToUpdate(gtaskLists)) {
            fetchAndApplyRemoteChanges(list);
        }
    }

    private void pushLocalChanges() throws UserRecoverableAuthIOException {
        List<Task> tasks = taskDao.getGoogleTasksToPush();
        for (Task task : tasks) {
            try {
                pushTask(task, gtasksInvoker);
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
    private void pushTask(Task task, GtasksInvoker invoker) throws IOException {
        for (GoogleTask deleted : googleTaskDao.getDeletedByTaskId(task.getId())) {
            gtasksInvoker.deleteGtask(deleted.getListId(), deleted.getRemoteId());
            googleTaskDao.delete(deleted);
        }

        GoogleTask gtasksMetadata = googleTaskDao.getByTaskId(task.getId());

        if (gtasksMetadata == null) {
            return;
        }

        com.google.api.services.tasks.model.Task remoteModel;
        boolean newlyCreated = false;

        String remoteId;
        Filter defaultRemoteList = defaultFilterProvider.getDefaultRemoteList();
        String listId = defaultRemoteList instanceof GtasksFilter
                ? ((GtasksFilter) defaultRemoteList).getRemoteId()
                : DEFAULT_LIST;

        if (Strings.isNullOrEmpty(gtasksMetadata.getRemoteId())) { //Create case
            String selectedList = gtasksMetadata.getListId();
            if (!Strings.isNullOrEmpty(selectedList)) {
                listId = selectedList;
            }
            remoteModel = new com.google.api.services.tasks.model.Task();
            newlyCreated = true;
        } else { //update case
            remoteId = gtasksMetadata.getRemoteId();
            listId = gtasksMetadata.getListId();
            remoteModel = new com.google.api.services.tasks.model.Task();
            remoteModel.setId(remoteId);
        }

        //If task was newly created but without a title, don't sync--we're in the middle of
        //creating a task which may end up being cancelled. Also don't sync new but already
        //deleted tasks
        if (newlyCreated && (TextUtils.isEmpty(task.getTitle()) || task.getDeletionDate() > 0)) {
            return;
        }

        //Update the remote model's changed properties
        if (task.isDeleted()) {
            remoteModel.setDeleted(true);
        }

        remoteModel.setTitle(task.getTitle());
        remoteModel.setNotes(task.getNotes());
        if (task.hasDueDate()) {
            remoteModel.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(task.getDueDate()));
        }
        if (task.isCompleted()) {
            remoteModel.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.getCompletionDate()));
            remoteModel.setStatus("completed"); //$NON-NLS-1$
        } else {
            remoteModel.setCompleted(null);
            remoteModel.setStatus("needsAction"); //$NON-NLS-1$
        }

        if (!newlyCreated) {
            try {
                invoker.updateGtask(listId, remoteModel);
            } catch(HttpNotFoundException e) {
                Timber.e(e, e.getMessage());
                googleTaskDao.delete(gtasksMetadata);
                return;
            }
        } else {
            String parent = gtasksSyncService.getRemoteParentId(gtasksMetadata);
            String priorSibling = gtasksSyncService.getRemoteSiblingId(listId, gtasksMetadata);

            com.google.api.services.tasks.model.Task created = invoker.createGtask(listId, remoteModel, parent, priorSibling);

            if (created != null) {
                //Update the metadata for the newly created task
                gtasksMetadata.setRemoteId(created.getId());
                gtasksMetadata.setListId(listId);
            } else {
                return;
            }
        }

        task.setModificationDate(DateUtilities.now());
        gtasksMetadata.setLastSync(DateUtilities.now() + 1000L);
        if (gtasksMetadata.getId() == Task.NO_ID) {
            googleTaskDao.insert(gtasksMetadata);
        } else {
            googleTaskDao.update(gtasksMetadata);
        }
        task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
        taskDao.save(task);
    }

    private synchronized void fetchAndApplyRemoteChanges(GoogleTaskList list) throws UserRecoverableAuthIOException {
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
                for (com.google.api.services.tasks.model.Task gtask : tasks) {
                    String remoteId = gtask.getId();
                    GoogleTask googleTask = getMetadataByGtaskId(remoteId);
                    Task task = null;
                    if (googleTask == null) {
                        googleTask = new GoogleTask(0, "");
                    } else if (googleTask.getTask() > 0){
                        task = taskDao.fetch(googleTask.getTask());
                    }
                    if (task == null) {
                        task = taskCreator.createWithValues(null, "");
                    }
                    GtasksTaskContainer container = new GtasksTaskContainer(gtask, task, listId, googleTask);
                    container.gtaskMetadata.setRemoteOrder(Long.parseLong(gtask.getPosition()));
                    container.gtaskMetadata.setParent(localIdForGtasksId(gtask.getParent()));
                    container.gtaskMetadata.setLastSync(DateUtilities.now() + 1000L);
                    write(container);
                    lastSyncDate = Math.max(lastSyncDate, container.getUpdateTime());
                }
                list.setLastSync(lastSyncDate);
                googleTaskListDao.insertOrReplace(list);
                gtasksTaskListUpdater.correctOrderAndIndentForList(listId);
            }
        } catch (UserRecoverableAuthIOException e) {
            throw e;
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
        }
    }

    private long localIdForGtasksId(String gtasksId) {
        GoogleTask metadata = getMetadataByGtaskId(gtasksId);
        return metadata == null ? Task.NO_ID : metadata.getTask();
    }

    private GoogleTask getMetadataByGtaskId(String gtaskId) {
        return googleTaskDao.getByRemoteId(gtaskId);
    }

    private void write(GtasksTaskContainer task) {
        if (!TextUtils.isEmpty(task.task.getTitle())) {
            task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            task.task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
            task.prepareForSaving();
            if (task.task.isNew()) {
                taskDao.createNew(task.task);
            }
            taskDao.save(task.task);
            synchronizeMetadata(task.task.getId(), task.metadata);
        }
    }

    /**
     * Synchronize metadata for given task id. Deletes rows in database that
     * are not identical to those in the metadata list, creates rows that
     * have no match.
     *
     * @param taskId id of task to perform synchronization on
     * @param metadata list of new metadata items to save
     */
    private void synchronizeMetadata(long taskId, ArrayList<GoogleTask> metadata) {
        for(GoogleTask metadatum : metadata) {
            metadatum.setTask(taskId);
            metadatum.setId(0);
        }

        for (GoogleTask item : googleTaskDao.getAllByTaskId(taskId)) {
            long id = item.getId();

            // clear item id when matching with incoming values
            item.setId(0);
            if(metadata.contains(item)) {
                metadata.remove(item);
            } else {
                // not matched. cut it
                item.setId(id);
                googleTaskDao.delete(item);
            }
        }

        // everything that remains shall be written
        for(GoogleTask values : metadata) {
            googleTaskDao.insert(values);
        }
    }


    public static void mergeDates(long remoteDueDate, Task local) {
        if (remoteDueDate > 0 && local.hasDueTime()) {
            DateTime oldDate = newDateTime(local.getDueDate());
            DateTime newDate = newDateTime(remoteDueDate)
                    .withHourOfDay(oldDate.getHourOfDay())
                    .withMinuteOfHour(oldDate.getMinuteOfHour())
                    .withSecondOfMinute(oldDate.getSecondOfMinute());
            local.setDueDateAdjustingHideUntil(
                    Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDate.getMillis()));
        } else {
            local.setDueDateAdjustingHideUntil(remoteDueDate);
        }
    }

    @Override
    protected void inject(SyncAdapterComponent component) {
        component.inject(this);
    }
}
