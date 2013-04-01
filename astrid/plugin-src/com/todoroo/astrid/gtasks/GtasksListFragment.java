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
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.subtasks.OrderedListFragmentHelperInterface;
import com.todoroo.astrid.subtasks.OrderedMetadataListFragmentHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;

public class GtasksListFragment extends SubtasksListFragment {

    protected static final int MENU_CLEAR_COMPLETED_ID = MENU_ADDON_INTENT_ID + 1;

    public static final String TOKEN_STORE_ID = "storeId"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SUPPORT_ID + 1;

    @Autowired private StoreObjectDao storeObjectDao;

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    @Autowired private GtasksMetadataService gtasksMetadataService;

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Autowired private SyncV2Service syncService;

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
        return new OrderedMetadataListFragmentHelper<StoreObject>(this, gtasksTaskListUpdater);
    }

    @Override
    protected boolean allowResorting() {
        return false;
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
        if (!isCurrentTaskListFragment())
            return;
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
                if (manual)
                    ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                else
                    refresh();
                ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
            }
        }));
    }

    @Override
    protected void onTaskDelete(Task task) {
        helper.onDeleteTask(task);
    }

    @Override
    protected void addMenuItems(Menu menu, Activity activity) {
        super.addMenuItems(menu, activity);
        addMenuItem(menu, R.string.gtasks_GTA_clear_completed, android.R.drawable.ic_input_delete, MENU_CLEAR_COMPLETED_ID, false);
    }

    @Override
    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
     // handle my own menus
        switch (id) {
        case MENU_REFRESH_ID:
            refreshData(true);
            return true;
        case MENU_CLEAR_COMPLETED_ID:
            clearCompletedTasks();
            return true;
        }

        return super.handleOptionsMenuItemSelected(id, intent);
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
                            t.setValue(Task.DELETION_DATE, DateUtilities.now());
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

    @Override
    protected void addSyncRefreshMenuItem(Menu menu, int themeFlags) {
        if(gtasksPreferenceService.isLoggedIn()) {
            addMenuItem(menu, R.string.actfm_TVA_menu_refresh,
                    ThemeService.getDrawable(R.drawable.icn_menu_refresh, themeFlags), MENU_REFRESH_ID, true);
        } else {
            super.addSyncRefreshMenuItem(menu, themeFlags);
        }
    }

}
