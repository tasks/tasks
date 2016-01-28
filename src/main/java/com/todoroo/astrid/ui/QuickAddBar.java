/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

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
    @Inject DialogBuilder dialogBuilder;

    private TaskListFragment fragment;

    public void initialize(Injector injector, TaskListFragment myFragment) {
        injector.inject(this); // TODO: get rid of this
        fragment = myFragment;
    }

    /**
     * Quick-add a new task
     */
    public Task quickAddTask(String title) {
        TagData tagData = fragment.getActiveTagData();
        if(tagData != null && (!tagData.containsNonNullValue(TagData.NAME) || tagData.getName().length() == 0)) {
            dialogBuilder.newMessageDialog(R.string.tag_no_title_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return null;
        }

        try {
            return taskService.createWithValues(fragment.getFilter().valuesForNewTasks, title);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        return null;
    }
}
