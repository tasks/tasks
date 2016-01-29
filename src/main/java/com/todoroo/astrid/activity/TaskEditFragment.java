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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;

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
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.TaskEditControlFragment;

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
public final class TaskEditFragment extends InjectingFragment implements EditNoteActivity.UpdatesChangedListener {

    public interface TaskEditFragmentCallbackHandler {
        void taskEditFinished();
    }

    public static TaskEditFragment newTaskEditFragment(boolean isNewTask, Task task) {
        TaskEditFragment taskEditFragment = new TaskEditFragment();
        taskEditFragment.isNewTask = isNewTask;
        taskEditFragment.model = task;
        taskEditFragment.applyModel = true;
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
    private static final String EXTRA_APPLY_MODEL = "extra_apply_model";
    private static final String EXTRA_IS_NEW_TASK = "extra_is_new_task";

    /**
     * Token for saving a bitmap in the intent before it has been added with a comment
     */
    public static final String TOKEN_PICTURE_IN_PROGRESS = "picture_in_progress"; //$NON-NLS-1$

    // --- request codes

    public static final int REQUEST_CODE_RECORD = 30; // TODO: move this to file control set
    public static final int REQUEST_CODE_CAMERA = 60;

    // --- services

    public static final int TAB_VIEW_UPDATES = 0;

    @Inject TaskService taskService;
    @Inject MetadataDao metadataDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskDeleter taskDeleter;
    @Inject NotificationManager notificationManager;
    @Inject ActivityPreferences preferences;
    @Inject ActFmCameraModule actFmCameraModule;
    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;

    // --- UI components

    private EditNoteActivity editNotes;

    @Bind(R.id.pager) ViewPager mPager;
    @Bind(R.id.updatesFooter) View commentsBar;
    @Bind(R.id.edit_scroll) ScrollView scrollView;
    @Bind(R.id.commentField) EditText commentField;

    public static final int[] rowIds = new int[] {
        R.id.row_1,
        R.id.row_2,
        R.id.row_3,
        R.id.row_4,
        R.id.row_5,
        R.id.row_6,
        R.id.row_7,
        R.id.row_8,
        R.id.row_9,
        R.id.row_10,
        R.id.row_11,
    };

    // --- other instance variables

    /** true if editing started with a new task */
    private boolean isNewTask = false;

    /** task model */
    Task model = null;
    private boolean applyModel = false;

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
            applyModel = savedInstanceState.getBoolean(EXTRA_APPLY_MODEL);
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
        View view = inflater.inflate(R.layout.task_edit_fragment, container, false);
        ButterKnife.bind(this, view);

        notificationManager.cancel(model.getId());

        applyModel = false;

        if (!showEditComments) {
            commentsBar.setVisibility(View.GONE);
        }

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    private void instantiateEditNotes() {
        if (showEditComments) {
            editNotes = new EditNoteActivity(actFmCameraModule, metadataDao, userActivityDao,
                    taskService, this, getView(), model.getId());
            editNotes.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));

            editNotes.addListener(this);
        }
    }

    private void loadMoreContainer() {
        int tabStyle = TaskEditViewPager.TAB_SHOW_ACTIVITY;

        if (!showEditComments) {
            tabStyle &= ~TaskEditViewPager.TAB_SHOW_ACTIVITY;
        }

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
            editNotes.addListener(this);
        }

        if (tabStyle == 0) {
            return;
        }

        TaskEditViewPager adapter = new TaskEditViewPager(getActivity(), tabStyle);
        adapter.parent = this;

        mPager.setAdapter(adapter);

        setCurrentTab(TAB_VIEW_UPDATES);
        setPagerHeightForPosition();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updatesChanged();
            }
        }, 500L);
    }

    private void setCurrentTab(int position) {
        mPager.setCurrentItem(position);
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

    private String getTitle() {
        return getEditTitleControlSet().getTitle();
    }

    /** Save task model from values in UI components */
    public void save() {
        String title = getTitle();
        if (title == null) {
            return;
        }

        if (title.length() > 0) {
            model.setDeletionDate(0L);
        }

        if (title.length() == 0) {
            return;
        }

        for (int fragmentId : rowIds) {
            TaskEditControlFragment fragment = (TaskEditControlFragment) getFragmentManager().findFragmentById(fragmentId);
            if (fragment == null) {
                break;
            }
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
        tla.refreshTaskList();
        if (isNewTask) {
            tla.getTaskListFragment().onTaskCreated(model.getId(), model.getUuid());
        }

        removeExtrasFromIntent(getActivity().getIntent());
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

    public void onBackPressed() {
        if(getTitle().length() == 0) {
            discardButtonClick();
        } else {
            save();
        }
        callback.taskEditFinished();
    }

    /**
     * Helper to remove task edit specific info from activity intent
     */
    public static void removeExtrasFromIntent(Intent intent) {
        if (intent != null) {
            intent.removeExtra(TaskListActivity.TOKEN_SWITCH_TO_FILTER);
            intent.removeExtra(TaskListActivity.OPEN_TASK);
            intent.removeExtra(TOKEN_PICTURE_IN_PROGRESS);
        }
    }

    /*
     * ======================================================================
     * ======================================================= event handlers
     * ======================================================================
     */

    protected void discardButtonClick() {
        if (isNewTask) {
            TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), model);
            taskDeleter.delete(model);
        }

        removeExtrasFromIntent(getActivity().getIntent());
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
    public boolean onOptionsItemSelected(MenuItem item) {
        hideKeyboard();

        switch (item.getItemId()) {
        case R.id.menu_save:
            save();
            callback.taskEditFinished();
            return true;
        case R.id.menu_discard:
            discardButtonClick();
            callback.taskEditFinished();
            return true;
        case R.id.menu_record_note:
            startRecordingAudio();
            return true;
        case R.id.menu_delete:
            deleteButtonClick();
            return true;
        case android.R.id.home:
            if (getTitle().trim().length() == 0) {
                discardButtonClick();
            } else {
                save();
            }
            callback.taskEditFinished();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.task_edit_fragment, menu);
        for (int i = 0 ; i < menu.size() ; i++) {
            MenuColorizer.colorMenuItem(menu.getItem(i), getResources().getColor(android.R.color.white));
        }
        if (getResources().getBoolean(R.bool.two_pane_layout)) {
            menu.findItem(R.id.menu_save).setVisible(true);
        }
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
        outState.putBoolean(EXTRA_APPLY_MODEL, applyModel);
        outState.putBoolean(EXTRA_IS_NEW_TASK, isNewTask);
    }

    /*
     * ======================================================================
     * ========================================== UI component helper classes
     * ======================================================================
     */

    public View getPageView() {
        return editNotes;
    }

    private void setPagerHeightForPosition() {
        int height = 0;

        View view = editNotes;
        if (mPager == null) {
            return;
        }

        int desiredWidth = MeasureSpec.makeMeasureSpec(view.getWidth(),
                MeasureSpec.AT_MOST);
        view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
        height = Math.max(view.getMeasuredHeight(), height);
        LayoutParams pagerParams = mPager.getLayoutParams();
        if (height > 0 && height != pagerParams.height) {
            pagerParams.height = height;
            mPager.setLayoutParams(pagerParams);
        }
    }

    // EditNoteActivity Listener when there are new updates/comments
    @Override
    public void updatesChanged()  {
        if (mPager != null && mPager.getCurrentItem() == TAB_VIEW_UPDATES) {
            setPagerHeightForPosition();
        }
    }

    // EditNoteActivity Lisener when there are new updates/comments
    @Override
    public void commentAdded() {
        setCurrentTab(TAB_VIEW_UPDATES);
        setPagerHeightForPosition();
        scrollToView(editNotes);
    }

    // Scroll to view in edit task
    public void scrollToView(View v) {
        View child = v;
        int top = v.getTop();
        while (!child.equals(scrollView) ) {
            top += child.getTop();
            ViewParent parentView = child.getParent();
            if (parentView != null && View.class.isInstance(parentView)) {
                child = (View) parentView;
            }
            else {
                break;
            }
        }
        scrollView.smoothScrollTo(0, top);
    }

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
