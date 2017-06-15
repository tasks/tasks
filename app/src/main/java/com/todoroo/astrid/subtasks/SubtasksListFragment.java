/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;

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

    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject Theme theme;
    @Inject TaskDao taskDao;
    @Inject AstridOrderedListFragmentHelper helper;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        helper.setTaskListFragment(this);
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
        return helper.createTaskAdapter(theme.wrap(context), cursor);
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
