/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.TaskEditControlFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button pressed)
 * as long as the task has a title.
 *
 * @author timsu
 *
 */
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

    public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment"; //$NON-NLS-1$

    // --- bundle tokens

    /**
     * Content Values to set
     */
    public static final String TOKEN_VALUES = "v"; //$NON-NLS-1$

    /**
     * Task in progress (during orientation change)
     */
    private static final String EXTRA_TASK = "extra_task"; //$NON-NLS-1$
    private static final String EXTRA_IS_NEW_TASK = "extra_is_new_task";

    /**
     * Token for saving a bitmap in the intent before it has been added with a comment
     */
    public static final String TOKEN_PICTURE_IN_PROGRESS = "picture_in_progress"; //$NON-NLS-1$

    // --- request codes

    public static final int REQUEST_CODE_RECORD = 30; // TODO: move this to file control set
    public static final int REQUEST_CODE_CAMERA = 60;

    @Inject TaskService taskService;
    @Inject MetadataDao metadataDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskDeleter taskDeleter;
    @Inject NotificationManager notificationManager;
    @Inject ActivityPreferences preferences;
    @Inject ActFmCameraModule actFmCameraModule;
    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject TaskEditControlSetFragmentManager taskEditControlSetFragmentManager;

    // --- UI components

    private EditNoteActivity editNotes;

    @Bind(R.id.updatesFooter) View commentsBar;
    @Bind(R.id.edit_body) LinearLayout body;
    @Bind(R.id.commentField) EditText commentField;
    @Bind(R.id.toolbar) Toolbar toolbar;

    // --- other instance variables

    /** true if editing started with a new task */
    private boolean isNewTask = false;

    /** task model */
    Task model = null;

    private boolean showEditComments;
    private TaskEditFragmentCallbackHandler callback;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if we were editing a task already, restore it
        if (savedInstanceState != null) {
            model = savedInstanceState.getParcelable(EXTRA_TASK);
            isNewTask = savedInstanceState.getBoolean(EXTRA_IS_NEW_TASK);
       }

        showEditComments = preferences.getBoolean(R.string.p_show_task_edit_comments, true);

        getActivity().setResult(RESULT_OK);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (TaskEditFragmentCallbackHandler) activity;
    }

    /*
     * ======================================================================
     * ==================================================== UI initialization
     * ======================================================================
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_edit, container, false);
        ButterKnife.bind(this, view);

        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_save_24dp));
        DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        toolbar.inflateMenu(R.menu.task_edit_fragment);
        Menu menu = toolbar.getMenu();
        for (int i = 0 ; i < menu.size() ; i++) {
            MenuColorizer.colorMenuItem(menu.getItem(i), getResources().getColor(android.R.color.white));
        }
        toolbar.setOnMenuItemClickListener(this);

        notificationManager.cancel(model.getId());

        if (!showEditComments) {
            commentsBar.setVisibility(View.GONE);
        }

        return view;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        hideKeyboard();

        switch (item.getItemId()) {
            case R.id.menu_record_note:
                startRecordingAudio();
                return true;
            case R.id.menu_delete:
                deleteButtonClick();
                return true;
        }

        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    private void instantiateEditNotes() {
        if (showEditComments) {
            editNotes = new EditNoteActivity(actFmCameraModule, metadataDao, userActivityDao,
                    taskService, this, getView(), model.getId());
            editNotes.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            body.addView(editNotes);
        }
    }

    private void loadMoreContainer() {
        if (editNotes == null) {
            instantiateEditNotes();
        } else {
            editNotes.loadViewForTaskID(model.getId());
        }

        if (editNotes != null) {
            TimerControlSet timerControl = getTimerControl();
            if (timerControl != null) {
                timerControl.setEditNotes(editNotes);
            }
        }
    }

    public Task stopTimer() {
        TimerPlugin.stopTimer(notificationManager, taskService, context, model);
        return model;
    }

    public Task startTimer() {
        TimerPlugin.startTimer(notificationManager, taskService, context, model);
        return model;
    }

    /**
     * Initialize task edit page in the background
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class TaskEditBackgroundLoader extends Thread {

        @Override
        public void run() {
            AndroidUtilities.sleepDeep(500L);

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        // todo: is this necessary?
                        loadMoreContainer();
                    }
                }
            });
        }
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
            removeExtrasFromIntent(getActivity().getIntent());
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

    private TimerControlSet getTimerControl() {
        return getFragment(TimerControlSet.TAG );
    }

    @SuppressWarnings("unchecked")
    private <T extends TaskEditControlFragment> T getFragment(int tag) {
        return (T) getFragmentManager().findFragmentByTag(getString(tag));
    }

    /**
     * Helper to remove task edit specific info from activity intent
     */
    public static void removeExtrasFromIntent(Intent intent) {
        if (intent != null) {
            intent.removeExtra(TOKEN_PICTURE_IN_PROGRESS);
        }
    }

    /*
     * ======================================================================
     * ======================================================= event handlers
     * ======================================================================
     */

    private boolean hasChanges(List<TaskEditControlFragment> fragments) {
        for (TaskEditControlFragment fragment : fragments) {
            if (fragment.hasChanges(model)) {
                return true;
            }
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

        removeExtrasFromIntent(getActivity().getIntent());
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

    private void startRecordingAudio() {
        Intent recordAudio = new Intent(getActivity(), AACRecordingActivity.class);
        startActivityForResult(recordAudio, REQUEST_CODE_RECORD);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (editNotes == null) {
            instantiateEditNotes();
        }

        if (requestCode == REQUEST_CODE_RECORD && resultCode == RESULT_OK) {
            String recordedAudioPath = data.getStringExtra(AACRecordingActivity.RESULT_OUTFILE);
            String recordedAudioName = data.getStringExtra(AACRecordingActivity.RESULT_FILENAME);
            getFilesControlSet().createNewFileAttachment(recordedAudioPath, recordedAudioName, TaskAttachment.FILE_TYPE_AUDIO + "m4a"); //$NON-NLS-1$
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            if (editNotes != null && resultCode == RESULT_OK) {
                Uri uri = data.getParcelableExtra(CameraActivity.EXTRA_URI);
                editNotes.setPictureUri(uri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    private void hideKeyboard() {
        getEditTitleControlSet().hideKeyboard();
        AndroidUtilities.hideSoftInputForViews(getActivity(), commentField);
        commentField.setCursorVisible(false);
    }

    public void onPriorityChange(int priority) {
        getEditTitleControlSet().setPriority(priority);
    }

    public void onRepeatChanged(boolean repeat) {
        getEditTitleControlSet().repeatChanged(repeat);
    }
}
