/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

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

    public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment"; //$NON-NLS-1$

    // --- bundle tokens

    /**
     * Task ID
     */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$
    public static final String TOKEN_UUID = "uuid";

    /**
     * Content Values to set
     */
    public static final String TOKEN_VALUES = "v"; //$NON-NLS-1$

    /**
     * Task in progress (during orientation change)
     */
    private static final String TASK_IN_PROGRESS = "task_in_progress"; //$NON-NLS-1$

    /**
     * Task remote id (during orientation change)
     */
    private static final String TASK_UUID = "task_uuid"; //$NON-NLS-1$

    /**
     * Token for saving a bitmap in the intent before it has been added with a comment
     */
    public static final String TOKEN_PICTURE_IN_PROGRESS = "picture_in_progress"; //$NON-NLS-1$

    // --- request codes

    public static final int REQUEST_CODE_RECORD = 30; // TODO: move this to file control set
    public static final int REQUEST_CODE_CAMERA = 60;

    // --- result codes

    public static final String OVERRIDE_FINISH_ANIM = "finishAnim"; //$NON-NLS-1$

    public static final String TOKEN_TAGS_CHANGED = "tags_changed";  //$NON-NLS-1$

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

    private final Map<String, Integer> controlSetFragments = new HashMap<>();
    private final List<Integer> displayedFragments = new ArrayList<>();
    private EditNoteActivity editNotes;

    @Bind(R.id.pager) ViewPager mPager;
    @Bind(R.id.updatesFooter) View commentsBar;
    @Bind(R.id.basic_controls) LinearLayout basicControls;
    @Bind(R.id.edit_scroll) ScrollView scrollView;
    @Bind(R.id.commentField) EditText commentField;

    // --- other instance variables

    /** true if editing started with a new task */
    private boolean isNewTask = false;

    /** task model */
    Task model = null;

    /** whether task should be saved when this activity exits */
    private boolean shouldSaveState = true;

    private boolean overrideFinishAnim;

    private String uuid = RemoteModel.NO_UUID;

    private boolean showEditComments;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if we were editing a task already, restore it
        if (savedInstanceState != null
                && savedInstanceState.containsKey(TASK_IN_PROGRESS)) {
            Task task = savedInstanceState.getParcelable(TASK_IN_PROGRESS);
            if (task != null) {
                model = task;
            }
            if (savedInstanceState.containsKey(TASK_UUID)) {
                uuid = savedInstanceState.getString(TASK_UUID);
            }
        }

        showEditComments = preferences.getBoolean(R.string.p_show_task_edit_comments, true);

        getActivity().setResult(RESULT_OK);
    }

    /*
     * ======================================================================
     * ==================================================== UI initialization
     * ======================================================================
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.task_edit_activity, container, false);
        ButterKnife.bind(this, view);

        loadItem(getActivity().getIntent());

        registerFragment(R.string.TEA_ctrl_title_pref);
        registerFragment(R.string.TEA_ctrl_when_pref);
        registerFragment(R.string.TEA_ctrl_gcal);
        registerFragment(R.string.TEA_ctrl_importance_pref);
        registerFragment(R.string.TEA_ctrl_notes_pref);
        registerFragment(R.string.TEA_ctrl_hide_until_pref);
        registerFragment(R.string.TEA_ctrl_reminders_pref);
        registerFragment(R.string.TEA_ctrl_files_pref);
        registerFragment(R.string.TEA_ctrl_timer_pref);
        registerFragment(R.string.TEA_ctrl_lists_pref);
        registerFragment(R.string.TEA_ctrl_repeat_pref);
        
        ArrayList<String> controlOrder = BeastModePreferences.constructOrderedControlList(preferences, getActivity());
        controlOrder.add(0, getString(R.string.TEA_ctrl_title_pref));

        String hideAlwaysTrigger = getString(R.string.TEA_ctrl_hide_section_pref);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        for (String item : controlOrder) {
            if (item.equals(hideAlwaysTrigger)) {
                break;
            }
            Integer fragmentId = controlSetFragments.get(item);
            if (fragmentId == null) {
                Timber.e("Unknown task edit control %s", item);
                continue;
            }
            displayedFragments.add(fragmentId);
            if (fragmentManager.findFragmentByTag(item) == null) {
                TaskEditControlFragment fragment = createFragment(controlSetFragments.get(item));
                if (fragment != null) {
                    fragment.initialize(isNewTask, model);
                    fragmentTransaction.add(basicControls.getId(), fragment, item);
                }
            }
        }

        fragmentTransaction.commit();

        if (!showEditComments) {
            commentsBar.setVisibility(View.GONE);
        }

        return view;
    }

    private void registerFragment(int resId) {
        controlSetFragments.put(getString(resId), resId);
    }

    private TaskEditControlFragment createFragment(int fragmentId) {
        switch (fragmentId) {
            case R.string.TEA_ctrl_title_pref:
                return new EditTitleControlSet();
            case R.string.TEA_ctrl_when_pref:
                return new DeadlineControlSet();
            case R.string.TEA_ctrl_importance_pref:
                return new PriorityControlSet();
            case R.string.TEA_ctrl_notes_pref:
                return new DescriptionControlSet();
            case R.string.TEA_ctrl_gcal:
                return new CalendarControlSet();
            case R.string.TEA_ctrl_hide_until_pref:
                return new HideUntilControlSet();
            case R.string.TEA_ctrl_reminders_pref:
                return new ReminderControlSet();
            case R.string.TEA_ctrl_files_pref:
                return new FilesControlSet();
            case R.string.TEA_ctrl_timer_pref:
                return new TimerControlSet();
            case R.string.TEA_ctrl_lists_pref:
                return new TagsControlSet();
            case R.string.TEA_ctrl_repeat_pref:
                return new RepeatControlSet();
            default:
                throw new RuntimeException("Unsupported fragment");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        AstridActivity activity = (AstridActivity) getActivity();

        overrideFinishAnim = false;
        if (activity != null) {
            if (activity.getIntent() != null) {
                overrideFinishAnim = activity.getIntent().getBooleanExtra(
                        OVERRIDE_FINISH_ANIM, true);
            }
        }

        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    private void instantiateEditNotes() {
        if (showEditComments) {
            long idParam = getActivity().getIntent().getLongExtra(TOKEN_ID, -1L);
            editNotes = new EditNoteActivity(actFmCameraModule, metadataDao, userActivityDao,
                    taskService, this, getView(), idParam);
            editNotes.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));

            editNotes.addListener(this);
        }
    }

    private void loadMoreContainer() {
        long idParam = getActivity().getIntent().getLongExtra(TOKEN_ID, -1L);

        int tabStyle = TaskEditViewPager.TAB_SHOW_ACTIVITY;

        if (!showEditComments) {
            tabStyle &= ~TaskEditViewPager.TAB_SHOW_ACTIVITY;
        }

        if (editNotes == null) {
            instantiateEditNotes();
        } else {
            editNotes.loadViewForTaskID(idParam);
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

    /**
     * Loads action item from the given intent
     */
    private void loadItem(Intent intent) {
        if (model != null) {
            // came from bundle
            setIsNewTask(model.getTitle().length() == 0);
            return;
        }

        long idParam = intent.getLongExtra(TOKEN_ID, -1L);
        if (idParam > -1L) {
            model = taskService.fetchById(idParam, Task.PROPERTIES);

            if (model != null && model.containsNonNullValue(Task.UUID)) {
                uuid = model.getUUID();
            }
        }

        // not found by id or was never passed an id
        if (model == null) {
            String valuesAsString = intent.getStringExtra(TOKEN_VALUES);
            ContentValues values = null;
            try {
                if (valuesAsString != null) {
                    valuesAsString = PermaSql.replacePlaceholders(valuesAsString);
                    values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
                }
            } catch (Exception e) {
                // oops, can't serialize
                Timber.e(e, e.getMessage());
            }
            model = taskService.createWithValues(values, null);
            getActivity().getIntent().putExtra(TOKEN_ID, model.getId());
        }

        if (model.getTitle().length() == 0) {

            // set deletion date until task gets a title
            model.setDeletionDate(DateUtilities.now());
        }

        setIsNewTask(model.getTitle().length() == 0);

        if (model == null) {
            Timber.e(new NullPointerException("model"), "task-edit-no-task");
            getActivity().onBackPressed();
            return;
        }

        notificationManager.cancel(model.getId());
    }

    private void setIsNewTask(boolean isNewTask) {
        this.isNewTask = isNewTask;
    }

    /** Convenience method to populate fields after setting model to null */
    public void repopulateFromScratch(Intent intent) {
        model = null;
        uuid = RemoteModel.NO_UUID;
        loadMoreContainer();
    }

    private String getTitle() {
        return getEditTitleControlSet().getTitle();
    }

    /** Save task model from values in UI components */
    public void save(boolean onPause) {
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

        if (!onPause) {
            for (Integer fragmentId : displayedFragments) {
                getFragment(fragmentId).apply(model);
            }
            taskService.save(model);

            boolean taskEditActivity = (getActivity() instanceof TaskEditActivity);

            boolean tagsChanged = Flags.check(Flags.TAGS_CHANGED);

            if (taskEditActivity) {
                Intent data = new Intent();
                data.putExtra(TOKEN_TAGS_CHANGED, tagsChanged);
                data.putExtra(TOKEN_ID, model.getId());
                data.putExtra(TOKEN_UUID, model.getUuid());
                getActivity().setResult(RESULT_OK, data);

            } else {
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
            }

            removeExtrasFromIntent(getActivity().getIntent());
            shouldSaveState = false;
            getActivity().onBackPressed();
        }
    }

    private EditTitleControlSet getEditTitleControlSet() {
        return getFragment(R.string.TEA_ctrl_title_pref);
    }

    private FilesControlSet getFilesControlSet() {
        return getFragment(R.string.TEA_ctrl_files_pref);
    }

    private TimerControlSet getTimerControl() {
        return getFragment(R.string.TEA_ctrl_timer_pref);
    }

    @SuppressWarnings("unchecked")
    private <T extends TaskEditControlFragment> T getFragment(int tag) {
        return (T) getFragmentManager().findFragmentByTag(getString(tag));
    }

    public boolean onKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(getTitle().length() == 0) {
                discardButtonClick();
            } else {
                saveButtonClick();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // abandon editing and delete the newly created task if
        // no title was entered
        Activity activity = getActivity();
        if (overrideFinishAnim) {
            AndroidUtilities.callOverridePendingTransition(activity,
                    R.anim.slide_right_in, R.anim.slide_right_out);
        }

        if (activity instanceof TaskListActivity) {
            if (getTitle().length() == 0 && isNewTask && model != null && model.isSaved()) {
                taskDeleter.delete(model);
            }
        } else if (activity instanceof TaskEditActivity) {
            if (getTitle().length() == 0 && isNewTask && model != null && model.isSaved()) {
                taskDeleter.delete(model);
            }
        }
    }

    /**
     * Helper to remove task edit specific info from activity intent
     */
    public static void removeExtrasFromIntent(Intent intent) {
        if (intent != null) {
            intent.removeExtra(TaskListActivity.OPEN_TASK);
            intent.removeExtra(TOKEN_PICTURE_IN_PROGRESS);
        }
    }

    /*
     * ======================================================================
     * ======================================================= event handlers
     * ======================================================================
     */

    protected void saveButtonClick() {
        save(false);
    }

    protected void discardButtonClick() {
        shouldSaveState = false;

        // abandon editing in this case
        if (getTitle().trim().length() == 0 || TextUtils.isEmpty(model.getTitle())) {
            if (isNewTask) {
                TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), model);
                taskDeleter.delete(model);
                if (getActivity() instanceof TaskListActivity) {
                    TaskListActivity tla = (TaskListActivity) getActivity();
                    tla.refreshTaskList();
                }
            }
        }

        removeExtrasFromIntent(getActivity().getIntent());
        getActivity().onBackPressed();
    }

    protected void deleteButtonClick() {
        dialogBuilder.newMessageDialog(R.string.DLG_delete_this_task_question)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), model);
                        taskDeleter.delete(model);
                        shouldSaveState = false;

                        Activity a = getActivity();
                        if (a instanceof TaskEditActivity) {
                            getActivity().setResult(RESULT_OK);
                            getActivity().onBackPressed();
                        } else if (a instanceof TaskListActivity) {
                            discardButtonClick();
                            TaskListFragment tlf = ((TaskListActivity) a).getTaskListFragment();
                            if (tlf != null) {
                                tlf.refresh();
                            }
                        }
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
            saveButtonClick();
            return true;
        case R.id.menu_discard:
            discardButtonClick();
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
                saveButtonClick();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.task_edit_fragment, menu);
        MenuColorizer.colorMenu(getActivity(), menu, getResources().getColor(android.R.color.white));
        if (preferences.useTabletLayout()) {
            menu.findItem(R.id.menu_save).setVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (shouldSaveState) {
            save(true);
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

        // stick our task into the outState
        outState.putParcelable(TASK_IN_PROGRESS, model);
        outState.putString(TASK_UUID, uuid);
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
