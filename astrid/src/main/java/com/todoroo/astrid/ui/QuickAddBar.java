/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.text.TextUtils;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.Injector;
import org.tasks.preferences.ActivityPreferences;

import java.util.HashSet;

import javax.inject.Inject;

/**
 * Quick Add Bar lets you add tasks.
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class QuickAddBar {

    private static final Logger log = LoggerFactory.getLogger(QuickAddBar.class);

    @Inject TaskService taskService;
    @Inject TaskCreator taskCreator;
    @Inject ActivityPreferences preferences;
    @Inject DateChangedAlerts dateChangedAlerts;

    private TaskListActivity activity;
    private TaskListFragment fragment;

    public void initialize(Injector injector, TaskListActivity myActivity, TaskListFragment myFragment) {
        injector.inject(this); // TODO: get rid of this
        activity = myActivity;
        fragment = myFragment;
        resetControlSets();
    }

    private void resetControlSets() {
        Task empty = new Task();
        TagData tagData = fragment.getActiveTagData();
        if (tagData != null) {
            HashSet<String> tagsTransitory = new HashSet<>();
            tagsTransitory.add(tagData.getName());
            empty.putTransitory(TaskService.TRANS_TAGS, tagsTransitory);
        }
    }

    // --- quick add task logic

    /**
     * Quick-add a new task
     */
    public Task quickAddTask(String title, boolean selectNewTask) {
        TagData tagData = fragment.getActiveTagData();
        if(tagData != null && (!tagData.containsNonNullValue(TagData.NAME) ||
                tagData.getName().length() == 0)) {
            DialogUtilities.okDialog(activity, activity.getString(R.string.tag_no_title_error), null);
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

            resetControlSets();

            taskCreator.addToCalendar(task, title);

            if (selectNewTask) {
                fragment.loadTaskListContent();
                fragment.selectCustomId(task.getId());
                if (task.getTransitory(TaskService.TRANS_QUICK_ADD_MARKUP) != null) {
                    showAlertForMarkupTask(activity, task, title);
                } else if (!TextUtils.isEmpty(task.getRecurrence())) {
                    showAlertForRepeatingTask(activity, task);
                }
                activity.onTaskListItemClicked(task.getId());
            }

            fragment.onTaskCreated(task);

            return task;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new Task();
        }
    }

    private void showAlertForMarkupTask(AstridActivity activity, Task task, String originalText) {
        dateChangedAlerts.showQuickAddMarkupDialog(activity, task, originalText);
    }

    private void showAlertForRepeatingTask(AstridActivity activity, Task task) {
        dateChangedAlerts.showRepeatChangedDialog(activity, task);
    }
}
