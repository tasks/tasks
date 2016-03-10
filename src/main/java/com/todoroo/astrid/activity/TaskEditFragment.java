/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.notes.CommentsController;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.TaskEditControlFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

import static org.tasks.date.DateTimeUtils.newDateTime;

public final class TaskEditFragment extends InjectingFragment implements Toolbar.OnMenuItemClickListener {

    public interface TaskEditFragmentCallbackHandler {
        void taskEditFinished();
    }

    public static TaskEditFragment newTaskEditFragment(boolean isNewTask, Task task) {
        TaskEditFragment taskEditFragment = new TaskEditFragment();
        taskEditFragment.isNewTask = isNewTask;
        taskEditFragment.model = task;
        return taskEditFragment;
    }

    public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment";
    public static final String TOKEN_VALUES = "v";

    private static final String EXTRA_TASK = "extra_task";
    private static final String EXTRA_IS_NEW_TASK = "extra_is_new_task";

    @Inject TaskService taskService;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskDeleter taskDeleter;
    @Inject NotificationManager notificationManager;
    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject TaskEditControlSetFragmentManager taskEditControlSetFragmentManager;
    @Inject CommentsController commentsController;
    @Inject Preferences preferences;
    @Inject Tracker tracker;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.comments) LinearLayout comments;

    // --- other instance variables

    /** true if editing started with a new task */
    private boolean isNewTask = false;

    /** task model */
    Task model = null;

    private TaskEditFragmentCallbackHandler callback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (TaskEditFragmentCallbackHandler) activity;
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_edit, container, false);
        ButterKnife.bind(this, view);

        if (savedInstanceState != null) {
            model = savedInstanceState.getParcelable(EXTRA_TASK);
            isNewTask = savedInstanceState.getBoolean(EXTRA_IS_NEW_TASK);
        }

        final boolean backButtonSavesTask = preferences.backButtonSavesTask();
        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(
                backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
        DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backButtonSavesTask) {
                    discardButtonClick();
                } else {
                    save();
                }
            }
        });
        toolbar.inflateMenu(R.menu.task_edit_fragment);
        Menu menu = toolbar.getMenu();
        for (int i = 0 ; i < menu.size() ; i++) {
            MenuColorizer.colorMenuItem(menu.getItem(i), getResources().getColor(android.R.color.white));
        }
        toolbar.setOnMenuItemClickListener(this);

        notificationManager.cancel(model.getId());

        commentsController.initialize(model, comments);
        commentsController.reloadView();

        for (int i = 0 ; i < TaskEditControlSetFragmentManager.TASK_EDIT_CONTROL_FRAGMENT_ROWS.length - taskEditControlSetFragmentManager.getNumRows() ; i++) {
            view.findViewById(TaskEditControlSetFragmentManager.TASK_EDIT_DIVIDER_ROWS[i]).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        AndroidUtilities.hideKeyboard(getActivity());

        switch (item.getItemId()) {
            case R.id.menu_delete:
                deleteButtonClick();
                return true;
        }

        return false;
    }

    public Task stopTimer() {
        TimerPlugin.stopTimer(notificationManager, taskService, context, model);
        String elapsedTime = DateUtils.formatElapsedTime(model.getElapsedSeconds());
        addComment(String.format("%s %s\n%s %s", //$NON-NLS-1$
                        getString(R.string.TEA_timer_comment_stopped),
                        DateUtilities.getTimeString(getActivity(), newDateTime()),
                        getString(R.string.TEA_timer_comment_spent),
                        elapsedTime), UserActivity.ACTION_TASK_COMMENT,
                null);
        return model;
    }

    public Task startTimer() {
        TimerPlugin.startTimer(notificationManager, taskService, context, model);
        addComment(String.format("%s %s",
                        getString(R.string.TEA_timer_comment_started),
                        DateUtilities.getTimeString(getActivity(), newDateTime())),
                UserActivity.ACTION_TASK_COMMENT,
                null);
        return model;
    }

    /*
     * ======================================================================
     * =============================================== model reading / saving
     * ======================================================================
     */

    /** Save task model from values in UI components */
    public void save() {
        List<TaskEditControlFragment> fragments = taskEditControlSetFragmentManager.getFragmentsInPersistOrder();
        if (hasChanges(fragments)) {
            for (TaskEditControlFragment fragment : fragments) {
                fragment.apply(model);
            }
            taskService.save(model);

            boolean tagsChanged = Flags.check(Flags.TAGS_CHANGED);

            // Notify task list fragment in multi-column case
            // since the activity isn't actually finishing
            TaskListActivity tla = (TaskListActivity) getActivity();

            if (tagsChanged) {
                tla.tagsChanged();
            }
            if (isNewTask) {
                tla.getTaskListFragment().onTaskCreated(model.getId(), model.getUuid());
            }
            callback.taskEditFinished();
        } else {
            discard();
        }
    }

    private EditTitleControlSet getEditTitleControlSet() {
        return getFragment(EditTitleControlSet.TAG);
    }

    private FilesControlSet getFilesControlSet() {
        return getFragment(FilesControlSet.TAG);
    }

    @SuppressWarnings("unchecked")
    private <T extends TaskEditControlFragment> T getFragment(int tag) {
        return (T) getFragmentManager().findFragmentByTag(getString(tag));
    }

    /*
     * ======================================================================
     * ======================================================= event handlers
     * ======================================================================
     */

    private boolean hasChanges(List<TaskEditControlFragment> fragments) {
        try {
            for (TaskEditControlFragment fragment : fragments) {
                if (fragment.hasChanges(model)) {
                    return true;
                }
            }
        } catch(Exception e) {
            Timber.e(e, e.getMessage());
            tracker.reportException(e);
        }
        return false;
    }

    public void discardButtonClick() {
        if (hasChanges(taskEditControlSetFragmentManager.getFragmentsInPersistOrder())) {
            dialogBuilder.newMessageDialog(R.string.discard_confirmation)
                    .setPositiveButton(R.string.keep_editing, null)
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            discard();
                        }
                    })
                    .show();
        } else {
            discard();
        }
    }

    public void discard() {
        if (isNewTask) {
            TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), model);
            taskDeleter.delete(model);
        }

        callback.taskEditFinished();
    }

    protected void deleteButtonClick() {
        dialogBuilder.newMessageDialog(R.string.DLG_delete_this_task_question)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), model);
                        taskDeleter.delete(model);
                        callback.taskEditFinished();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_TASK, model);
        outState.putBoolean(EXTRA_IS_NEW_TASK, isNewTask);
    }

    /*
     * ======================================================================
     * ========================================== UI component helper classes
     * ======================================================================
     */

    public void onPriorityChange(int priority) {
        getEditTitleControlSet().setPriority(priority);
    }

    public void onRepeatChanged(boolean repeat) {
        getEditTitleControlSet().repeatChanged(repeat);
    }

    public void addComment(String message, String actionCode, String picture) {
        UserActivity userActivity = new UserActivity();
        userActivity.setMessage(message);
        userActivity.setAction(actionCode);
        userActivity.setTargetId(model.getUuid());
        userActivity.setCreatedAt(DateUtilities.now());
        if (picture != null) {
            userActivity.setPicture(picture);
        }
        userActivityDao.createNew(userActivity);
        commentsController.reloadView();
    }
}
