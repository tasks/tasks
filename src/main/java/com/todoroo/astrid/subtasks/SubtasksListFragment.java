/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.tags.TagService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.CheckBoxes;

import javax.inject.Inject;

/**
 * Fragment for subtasks
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SubtasksListFragment extends TaskListFragment {

    public static TaskListFragment newSubtasksListFragment(Filter filter) {
        SubtasksListFragment fragment = new SubtasksListFragment();
        fragment.filter = filter;
        return fragment;
    }

    protected AstridOrderedListFragmentHelper helper;

    private int lastVisibleIndex = -1;

    @Inject SubtasksFilterUpdater subtasksFilterUpdater;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject DialogBuilder dialogBuilder;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject CheckBoxes checkBoxes;
    @Inject TagService tagService;
    @Inject ThemeCache themeCache;
    @Inject Theme theme;
    @Inject TaskDao taskDao;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        helper = new AstridOrderedListFragmentHelper(preferences, taskAttachmentDao,
                this, subtasksFilterUpdater, dialogBuilder, checkBoxes, tagService, themeCache, taskDao);
    }

    @Override
    protected int getListBody() {
        return R.layout.task_list_body_subtasks;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        helper.setUpUiComponents();
    }

    @Override
    public void setTaskAdapter() {
        helper.setList(initializeTaskListMetadata());
        helper.beforeSetUpTaskList(filter);

        super.setTaskAdapter();
    }

    private TaskListMetadata initializeTaskListMetadata() {
        String filterId = null;
        String prefId = null;
        if (BuiltInFilterExposer.isInbox(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_ALL;
            prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER;
        } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_TODAY;
            prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER;
        }
        if (TextUtils.isEmpty(filterId)) {
            return null;
        }
        TaskListMetadata taskListMetadata = taskListMetadataDao.fetchByTagId(filterId, TaskListMetadata.PROPERTIES);
        if (taskListMetadata == null) {
            String defaultOrder = preferences.getStringValue(prefId);
            if (TextUtils.isEmpty(defaultOrder)) {
                defaultOrder = "[]"; //$NON-NLS-1$
            }
            defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskDao, defaultOrder);
            taskListMetadata = new TaskListMetadata();
            taskListMetadata.setFilter(filterId);
            taskListMetadata.setTaskIDs(defaultOrder);
            taskListMetadataDao.createNew(taskListMetadata);
        }
        return taskListMetadata;
    }

    @Override
    public void onPause() {
        super.onPause();
        lastVisibleIndex = getListView().getFirstVisiblePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        ListView listView = getListView();
        if (lastVisibleIndex >= 0) {
            listView.setSelection(lastVisibleIndex);
        }
        unregisterForContextMenu(listView);
    }

    @Override
    public void onTaskCreated(String uuid) {
        helper.onCreateTask(uuid);
    }

    @Override
    protected void onTaskDelete(Task task) {
        super.onTaskDelete(task);
        helper.onDeleteTask(task);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(theme.wrap(context), cursor, taskListDataProvider.getSqlQueryTemplate());
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
