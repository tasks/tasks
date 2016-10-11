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
import com.todoroo.astrid.adapter.TaskAdapter;
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
import com.todoroo.astrid.tags.TagService;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.CheckBoxes;

import java.util.ArrayList;
import java.util.Arrays;

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
    @Inject DialogBuilder dialogBuilder;
    @Inject Broadcaster broadcaster;
    @Inject CheckBoxes checkBoxes;
    @Inject TagService tagService;
    @Inject ThemeCache themeCache;

    private GtasksList list;

    @Override
    protected OrderedListFragmentHelperInterface createFragmentHelper() {
        return new OrderedMetadataListFragmentHelper<>(preferences, taskAttachmentDao, taskService,
                metadataDao, this, gtasksTaskListUpdater, dialogBuilder, checkBoxes, tagService, themeCache);
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

                onRefresh();
            }
        });
    }

    @Override
    public Property<?>[] taskProperties() {
        Property<?>[] baseProperties = TaskAdapter.PROPERTIES;
        ArrayList<Property<?>> properties = new ArrayList<>(Arrays.asList(baseProperties));
        properties.add(gtasksTaskListUpdater.indentProperty());
        properties.add(gtasksTaskListUpdater.orderProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }
}
