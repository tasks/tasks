/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedListFragmentHelperInterface;
import com.todoroo.astrid.subtasks.OrderedMetadataListFragmentHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;

import org.tasks.R;

import javax.inject.Inject;

public class GtasksListFragment extends SubtasksListFragment {

    public static final String TOKEN_STORE_ID = "storeId"; //$NON-NLS-1$

    @Inject TaskService taskService;
    @Inject StoreObjectDao storeObjectDao;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject SyncV2Service syncService;

    private StoreObject list;

    private static final Property<?>[] LIST_PROPERTIES = new Property<?>[] {
        StoreObject.ID,
        StoreObject.TYPE,
        GtasksList.REMOTE_ID,
        GtasksList.ORDER,
        GtasksList.NAME,
        GtasksList.LAST_SYNC
    };

    @Override
    protected OrderedListFragmentHelperInterface<?> createFragmentHelper() {
        return new OrderedMetadataListFragmentHelper<>(this, gtasksTaskListUpdater);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);

        long storeObjectId = extras.getLong(TOKEN_STORE_ID, 0);
        list = storeObjectDao.fetch(storeObjectId, LIST_PROPERTIES);
        ((OrderedMetadataListFragmentHelper<StoreObject>)helper).setList(list);
    }

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment()) {
            return;
        }
        if (list != null && DateUtilities.now() - list.getValue(GtasksList.LAST_SYNC) > DateUtilities.ONE_MINUTE * 10) {
            refreshData(false);
        }
    }

    private void refreshData(final boolean manual) {
        ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.DLG_loading);

        syncService.synchronizeList(list, manual, new ProgressBarSyncResultCallback(getActivity(), this,
                R.id.progressBar, new Runnable() {
            @Override
            public void run() {
                if (manual) {
                    ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                } else {
                    refresh();
                }
                ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
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
            case R.id.menu_sync:
                refreshData(true);
                return true;
            case R.id.menu_clear_completed:
                clearCompletedTasks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearCompletedTasks() {
        final ProgressDialog pd = new ProgressDialog(getActivity());
        final TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.getSqlQuery(),
                null, Task.ID, Task.COMPLETION_DATE);
        pd.setMessage(this.getString(R.string.gtasks_GTA_clearing));
        pd.show();

        new Thread() {
            @Override
            public void run() {
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
                    DialogUtilities.dismissDialog(getActivity(), pd);
                }
                if (listId != null) {
                    gtasksTaskListUpdater.correctMetadataForList(listId);
                }
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadTaskListContent(true);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public Property<?>[] taskProperties() {
        return helper.taskProperties();
    }
}
