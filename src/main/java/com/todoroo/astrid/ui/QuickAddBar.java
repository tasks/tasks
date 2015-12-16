/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.Injector;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Quick Add Bar lets you add tasks.
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class QuickAddBar {

    @Inject TaskService taskService;
    @Inject TaskCreator taskCreator;
    @Inject DialogBuilder dialogBuilder;

    private TaskListActivity activity;
    private TaskListFragment fragment;

    public void initialize(Injector injector, TaskListActivity myActivity, TaskListFragment myFragment) {
        injector.inject(this); // TODO: get rid of this
        activity = myActivity;
        fragment = myFragment;
    }

    // --- quick add task logic

    public Task quickAddTask() {
        return quickAddTask("");
    }

    /**
     * Quick-add a new task
     */
    public Task quickAddTask(String title) {
        TagData tagData = fragment.getActiveTagData();
        if(tagData != null && (!tagData.containsNonNullValue(TagData.NAME) ||
                tagData.getName().length() == 0)) {
            dialogBuilder.newMessageDialog(R.string.tag_no_title_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return null;
        }

        try {
            if (title != null) {
                title = title.trim();
            }

            Task task = new Task();
            if (title != null) {
                task.setTitle(title); // need this for calendar
            }

            taskService.createWithValues(task, fragment.getFilter().valuesForNewTasks, title);

            taskCreator.addToCalendar(task, title);

            fragment.loadTaskListContent();
            fragment.selectCustomId(task.getId());
            activity.onTaskListItemClicked(task.getId());

            fragment.onTaskCreated(task.getId(), task.getUUID());
            return task;
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        return null;
    }
}
