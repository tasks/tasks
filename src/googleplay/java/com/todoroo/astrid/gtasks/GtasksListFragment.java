/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedListFragmentHelperInterface;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import org.tasks.sync.IndeterminateProgressBarSyncResultCallback;
import org.tasks.sync.SyncThrottle;

import javax.inject.Inject;

public class GtasksListFragment extends SubtasksListFragment {

    public static TaskListFragment newGtasksListFragment(GtasksFilter filter, GtasksList list) {
        GtasksListFragment fragment = new GtasksListFragment();
        fragment.filter = filter;
        fragment.list = list;
        return fragment;
    }

    private static final String EXTRA_STORE_OBJECT = "extra_store_object";

    @Inject TaskService taskService;
    @Inject MetadataDao metadataDao;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject SyncV2Service syncService;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject Preferences preferences;
    @Inject SyncThrottle syncThrottle;
    @Inject DialogBuilder dialogBuilder;
    @Inject Broadcaster broadcaster;

    private GtasksList list;

    @Override
    protected OrderedListFragmentHelperInterface createFragmentHelper() {
        return new OrderedMetadataListFragmentHelper<>(preferences, taskAttachmentDao, taskService, metadataDao, this, gtasksTaskListUpdater, dialogBuilder);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            StoreObject storeObject = savedInstanceState.getParcelable(EXTRA_STORE_OBJECT);
            list = new GtasksList(storeObject);
        }

        ((OrderedMetadataListFragmentHelper<GtasksList>)helper).setList(list);
    }

    @Override
    protected void inflateMenu(Toolbar toolbar) {
        super.inflateMenu(toolbar);
        toolbar.inflateMenu(R.menu.menu_gtasks_list_fragment);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_STORE_OBJECT, list.getStoreObject());
    }

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (list != null && syncThrottle.canSync(list.getId())) {
            syncData();
        }
    }

    private void syncData() {
        syncService.synchronizeList(list, new IndeterminateProgressBarSyncResultCallback(this, gtasksPreferenceService, broadcaster));
    }

    @Override
    protected void onTaskDelete(Task task) {
        helper.onDeleteTask(task);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_clear_completed:
                clearCompletedTasks();
                return true;
            default:
                return super.onMenuItemClick(item);
        }
    }

    private void clearCompletedTasks() {
        syncService.clearCompleted(list, new SyncResultCallback() {
            @Override
            public void started() {
                setSyncOngoing(true);
            }

            @Override
            public void finished() {
                setSyncOngoing(false);

                syncData();
            }
        });
    }

    @Override
    public Property<?>[] taskProperties() {
        return helper.taskProperties();
    }
}
