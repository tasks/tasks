/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.os.Bundle;
import android.view.MenuItem;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedListFragmentHelperInterface;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
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
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject SyncV2Service syncService;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject ActivityPreferences preferences;
    @Inject SyncThrottle syncThrottle;
    @Inject DialogBuilder dialogBuilder;
    @Inject Broadcaster broadcaster;

    private GtasksList list;

    @Override
    protected OrderedListFragmentHelperInterface createFragmentHelper() {
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
