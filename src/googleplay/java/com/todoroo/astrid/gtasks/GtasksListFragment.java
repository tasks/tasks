/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedListFragmentHelperInterface;
import com.todoroo.astrid.subtasks.SubtasksListFragment;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.sync.IndeterminateProgressBarSyncResultCallback;
import org.tasks.sync.SyncThrottle;

import javax.inject.Inject;

public class GtasksListFragment extends SubtasksListFragment {

    public static final String TOKEN_STORE_ID = "storeId"; //$NON-NLS-1$

    @Inject TaskService taskService;
    @Inject MetadataDao metadataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject SyncV2Service syncService;
    @Inject @ForActivity Context context;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject ActivityPreferences preferences;
    @Inject SyncThrottle syncThrottle;
    @Inject DialogBuilder dialogBuilder;

    private GtasksList list;

    @Override
    protected OrderedListFragmentHelperInterface<?> createFragmentHelper() {
        return new OrderedMetadataListFragmentHelper<>(preferences, taskAttachmentDao, taskService, metadataDao, this, gtasksTaskListUpdater, dialogBuilder);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);

        long storeObjectId = extras.getLong(TOKEN_STORE_ID, 0);
        list = storeObjectDao.getGtasksList(storeObjectId);
        ((OrderedMetadataListFragmentHelper<GtasksList>)helper).setList(list);
    }

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment()) {
            return;
        }
        if (list != null && syncThrottle.canSync(list.getId())) {
            refreshData(false);
        }
    }

    private void refreshData(final boolean manual) {
        syncService.synchronizeList(list, new IndeterminateProgressBarSyncResultCallback(gtasksPreferenceService, getActivity(), new Runnable() {
            @Override
            public void run() {
                if (manual) {
                    context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                } else {
                    refresh();
                }
            }
        }));
    }

    @Override
    protected void onTaskDelete(Task task) {
        helper.onDeleteTask(task);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_clear_completed:
                clearCompletedTasks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearCompletedTasks() {
        final TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.getSqlQuery(),
                null, Task.ID, Task.COMPLETION_DATE);

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progressDialog = dialogBuilder.newProgressDialog(R.string.gtasks_GTA_clearing);

            @Override
            protected void onPreExecute() {
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                String listId = null;
                try {
                    for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                        Task t = new Task(tasks);
                        if (t.isCompleted()) {
                            if (listId == null) {
                                listId = gtasksMetadataService.getTaskMetadata(
                                        t.getId()).getValue(GtasksMetadata.LIST_ID);
                            }
                            t.setDeletionDate(DateUtilities.now());
                            taskService.save(t);
                        }
                    }
                } finally {
                    tasks.close();
                }
                if (listId != null) {
                    gtasksTaskListUpdater.correctMetadataForList(listId);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                loadTaskListContent();
            }
        }.execute();
    }

    @Override
    public Property<?>[] taskProperties() {
        return helper.taskProperties();
    }
}
