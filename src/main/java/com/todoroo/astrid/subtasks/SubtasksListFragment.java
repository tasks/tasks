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
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;

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

    protected OrderedListFragmentHelperInterface helper;

    private int lastVisibleIndex = -1;

    @Inject TaskService taskService;
    @Inject SubtasksFilterUpdater subtasksFilterUpdater;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject DialogBuilder dialogBuilder;
    @Inject TaskListMetadataDao taskListMetadataDao;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        helper = createFragmentHelper();
    }

    protected OrderedListFragmentHelperInterface createFragmentHelper() {
        return new AstridOrderedListFragmentHelper<>(preferences, taskAttachmentDao, taskService, this, subtasksFilterUpdater, dialogBuilder);
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
        if (helper instanceof AstridOrderedListFragmentHelper) {
            ((AstridOrderedListFragmentHelper<TaskListMetadata>) helper).setList(initializeTaskListMetadata());
        }
        helper.beforeSetUpTaskList(filter);

        super.setTaskAdapter();
    }

    private TaskListMetadata initializeTaskListMetadata() {
        String filterId = null;
        String prefId = null;
        if (BuiltInFilterExposer.isInbox(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_ALL;
            prefId = SubtasksUpdater.ACTIVE_TASKS_ORDER;
        } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_TODAY;
            prefId = SubtasksUpdater.TODAY_TASKS_ORDER;
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
            defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskService, defaultOrder);
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
    public void onTaskCreated(long id, String uuid) {
        helper.onCreateTask(id, uuid);
    }

    @Override
    protected void onTaskDelete(Task task) {
        super.onTaskDelete(task);
        helper.onDeleteTask(task);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(context, cursor, sqlQueryTemplate);
    }

    @Override
    public void inject(FragmentComponent component) {
        if (this instanceof GtasksListFragment) {
            component.inject((GtasksListFragment) this);
        } else {
            component.inject(this);
        }
    }
}
