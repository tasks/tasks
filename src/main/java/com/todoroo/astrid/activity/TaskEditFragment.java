/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
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
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.CheckableImageView;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.DeadlineControlSet;
import com.todoroo.astrid.ui.DescriptionControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ImportanceControlSet;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.utility.Flags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.dialogs.DateAndTimePickerDialog;
import org.tasks.injection.InjectingFragment;
import org.tasks.location.GeofenceService;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.DeviceInfo;
import org.tasks.preferences.ResourceResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.todoroo.andlib.utility.AndroidUtilities.preGingerbreadMR1;
import static org.tasks.files.FileHelper.getPathFromUri;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button pressed)
 * as long as the task has a title.
 *
 * @author timsu
 *
 */
public final class TaskEditFragment extends InjectingFragment implements
ViewPager.OnPageChangeListener, EditNoteActivity.UpdatesChangedListener {

    private static final Logger log = LoggerFactory.getLogger(TaskEditFragment.class);

    public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment"; //$NON-NLS-1$

    // --- bundle tokens

    /**
     * Task ID
     */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    /**
     * Content Values to set
     */
    public static final String TOKEN_VALUES = "v"; //$NON-NLS-1$

    public static final String TOKEN_OPEN_CONTROL = "open_control"; //$NON-NLS-1$

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

    public static final int REQUEST_CODE_RECORD = 30;
    public static final int REQUEST_CODE_ATTACH_FILE = 40;
    public static final int REQUEST_CODE_BEAST_MODE = 50;

    // --- result codes

    public static final String OVERRIDE_FINISH_ANIM = "finishAnim"; //$NON-NLS-1$

    public static final String TOKEN_TAGS_CHANGED = "tags_changed";  //$NON-NLS-1$
    public static final String TOKEN_NEW_REPEATING_TASK = "new_repeating"; //$NON-NLS-1$

    // --- services

    public static final int TAB_VIEW_UPDATES = 0;

    @Inject TaskService taskService;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TagService tagService;
    @Inject MetadataDao metadataDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskDeleter taskDeleter;
    @Inject NotificationManager notificationManager;
    @Inject AlarmService alarmService;
    @Inject GCalHelper gcalHelper;
    @Inject ActivityPreferences preferences;
    @Inject DateChangedAlerts dateChangedAlerts;
    @Inject TagDataDao tagDataDao;
    @Inject ActFmCameraModule actFmCameraModule;
    @Inject GeofenceService geofenceService;
    @Inject ResourceResolver resourceResolver;
    @Inject DeviceInfo deviceInfo;

    // --- UI components

    private final HashMap<String, TaskEditControlSet> controlSetMap = new HashMap<>();
    private FilesControlSet filesControlSet;
    private TimerActionControlSet timerAction;
    private EditNoteActivity editNotes;
    private HideUntilControlSet hideUntilControls;

    @InjectView(R.id.title) EditText title;
    @InjectView(R.id.pager) ViewPager mPager;
    @InjectView(R.id.updatesFooter) View commentsBar;
    @InjectView(R.id.completeBox) CheckableImageView checkbox;
    @InjectView(R.id.timer_container) LinearLayout timerShortcut;
    @InjectView(R.id.basic_controls) LinearLayout basicControls;
    @InjectView(R.id.edit_scroll) ScrollView scrollView;
    @InjectView(R.id.commentField) EditText commentField;

    private final List<TaskEditControlSet> controls = Collections.synchronizedList(new ArrayList<TaskEditControlSet>());

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
    private boolean showTimerShortcut;

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
        showTimerShortcut = preferences.getBoolean(R.string.p_show_timer_shortcut, false);

        getActivity().setResult(Activity.RESULT_OK);
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
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        AstridActivity activity = (AstridActivity) getActivity();

        setUpUIComponents();

        overrideFinishAnim = false;
        if (activity != null) {
            if (activity.getIntent() != null) {
                overrideFinishAnim = activity.getIntent().getBooleanExtra(
                        OVERRIDE_FINISH_ANIM, true);
            }
        }
    }

    private void instantiateEditNotes() {
        if (showEditComments) {
            long idParam = getActivity().getIntent().getLongExtra(TOKEN_ID, -1L);
            editNotes = new EditNoteActivity(actFmCameraModule, preferences, metadataDao, userActivityDao,
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

        if (timerAction != null && editNotes != null) {
            timerAction.removeListener(editNotes);
            timerAction.addListener(editNotes);
        }

        if (editNotes != null) {
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

    /** Initialize UI components */
    private void setUpUIComponents() {
        // populate control set
        EditTitleControlSet editTitle = new EditTitleControlSet(
                taskService,
                getActivity(),
                title,
                checkbox);
        controls.add(editTitle);

        timerAction = new TimerActionControlSet(notificationManager, taskService, getActivity(), getView());
        controls.add(timerAction);

        TagsControlSet tagsControlSet = new TagsControlSet(metadataDao, tagDataDao, preferences, tagService, getActivity());
        controls.add(tagsControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_lists_pref), tagsControlSet);

        RepeatControlSet repeatControls = new RepeatControlSet(preferences, getActivity());
        controlSetMap.put(getString(R.string.TEA_ctrl_repeat_pref), repeatControls);

        GCalControlSet gcalControl = new GCalControlSet(gcalHelper, getActivity());
        controlSetMap.put(getString(R.string.TEA_ctrl_gcal), gcalControl);

        // The deadline control set contains the repeat controls and the
        // calendar controls.
        // NOTE: we add the gcalControl AFTER the
        // deadline control, because
        // otherwise the correct date may not be written to the calendar event.
        // Order matters!
        DeadlineControlSet deadlineControl = new DeadlineControlSet(preferences, getActivity());
        controlSetMap.put(getString(R.string.TEA_ctrl_when_pref), deadlineControl);
        controls.add(repeatControls);

        repeatControls.addListener(editTitle);
        controls.add(deadlineControl);
        controls.add(gcalControl);

        ImportanceControlSet importanceControl = new ImportanceControlSet(getActivity());
        controls.add(importanceControl);
        importanceControl.addListener(editTitle);
        controlSetMap.put(getString(R.string.TEA_ctrl_importance_pref),
                importanceControl);

        DescriptionControlSet notesControlSet = new DescriptionControlSet(getActivity());
        controls.add(notesControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_notes_pref),
                notesControlSet);

        ReminderControlSet reminderControlSet = new ReminderControlSet(alarmService, geofenceService, this);
        controls.add(reminderControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_reminders_pref), reminderControlSet);

        hideUntilControls = new HideUntilControlSet(this);
        controls.add(hideUntilControls);
        controlSetMap.put(getString(R.string.TEA_ctrl_hide_until_pref), hideUntilControls);

        // TODO: Fix the fact that hideUntil doesn't update accordingly with date changes when lazy loaded. Until then, don't lazy load.
        hideUntilControls.getView();

        TimerControlSet timerControl = new TimerControlSet(preferences, getActivity());
        timerAction.addListener(timerControl);
        controls.add(timerControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_timer_pref), timerControl);

        filesControlSet = new FilesControlSet(preferences, taskAttachmentDao, getActivity());
        controls.add(filesControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_files_pref), filesControlSet);

        loadEditPageOrder(false);

        if (!showEditComments) {
            commentsBar.setVisibility(View.GONE);
        }
        if (!showTimerShortcut) {
            timerShortcut.setVisibility(View.GONE);
        }

        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    private void loadEditPageOrder(boolean removeViews) {
        if (removeViews) {
            basicControls.removeAllViews();
        }

        ArrayList<String> controlOrder = BeastModePreferences.constructOrderedControlList(preferences, getActivity());
        String[] itemOrder = controlOrder.toArray(new String[controlOrder.size()]);

        String hideAlwaysTrigger = getString(R.string.TEA_ctrl_hide_section_pref);

        Class<?> openControl = (Class<?>) getActivity().getIntent().getSerializableExtra(TOKEN_OPEN_CONTROL);

        for (String item : itemOrder) {
            if (item.equals(hideAlwaysTrigger)) {
                break; // As soon as we hit the hide section, we're done
            } else {
                View controlSet = null;
                TaskEditControlSet curr = controlSetMap.get(item);

                if (curr != null) {
                    controlSet = curr.getView();
                }

                if (controlSet != null) {
                    ImageView icon = (ImageView) controlSet.findViewById(R.id.icon);
                    if (icon != null) {
                        icon.setImageResource(resourceResolver.getResource(curr.getIcon()));
                    }
                    basicControls.addView(controlSet);
                }

                if (curr != null && curr.getClass().equals(openControl) && curr instanceof PopupControlSet) {
                    curr.getView().performClick();
                }
            }
        }

        getActivity().getIntent().removeExtra(TOKEN_OPEN_CONTROL);
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
                log.error(e.getMessage(), e);
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
            log.error("task-edit-no-task", new NullPointerException("model"));
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
        populateFields(intent);
        loadMoreContainer();
    }

    /** Populate UI component values from the model */
    public void populateFields(Intent intent) {
        loadItem(intent);

        synchronized (controls) {
            if (!taskAttachmentDao.taskHasAttachments(model.getUuid())) {
                filesControlSet.getView().setVisibility(View.GONE);
            }
            for (TaskEditControlSet controlSet : controls) {
                controlSet.readFromTask(model);
            }
        }
    }

    /** Save task model from values in UI components */
    public void save(boolean onPause) {
        if (title == null) {
            return;
        }

        if (title.getText().length() > 0) {
            model.setDeletionDate(0L);
        }

        if (title.getText().length() == 0) {
            return;
        }

        synchronized (controls) {
            for (TaskEditControlSet controlSet : controls) {
                if (controlSet instanceof PopupControlSet) { // Save open control set
                    PopupControlSet popup = (PopupControlSet) controlSet;
                    Dialog d = popup.getDialog();
                    if (d != null && d.isShowing()) {
                        getActivity().getIntent().putExtra(TOKEN_OPEN_CONTROL, popup.getClass());
                    }
                }
                controlSet.writeToModel(model);
            }
        }

        boolean tagsChanged = Flags.check(Flags.TAGS_CHANGED);
        model.putTransitory(TaskService.TRANS_EDIT_SAVE, true); // TODO: not used?
        taskService.save(model);

        if (!onPause) {
            boolean taskEditActivity = (getActivity() instanceof TaskEditActivity);
            boolean showRepeatAlert = model.getTransitory(TaskService.TRANS_REPEAT_CHANGED) != null
                    && !TextUtils.isEmpty(model.getRecurrence());

            if (taskEditActivity) {
                Intent data = new Intent();
                if (showRepeatAlert) {
                    data.putExtra(TOKEN_NEW_REPEATING_TASK, model);
                }
                data.putExtra(TOKEN_TAGS_CHANGED, tagsChanged);
                getActivity().setResult(Activity.RESULT_OK, data);

            } else {
                // Notify task list fragment in multi-column case
                // since the activity isn't actually finishing
                TaskListActivity tla = (TaskListActivity) getActivity();
                if (showRepeatAlert) {
                    dateChangedAlerts.showRepeatChangedDialog(tla, model);
                }

                if (tagsChanged) {
                    tla.tagsChanged();
                }
                tla.refreshTaskList();
            }

            removeExtrasFromIntent(getActivity().getIntent());
            shouldSaveState = false;
            getActivity().onBackPressed();

        }
    }

    public boolean onKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(title.getText().length() == 0) {
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
            if (title.getText().length() == 0 && isNewTask && model != null && model.isSaved()) {
                taskDeleter.delete(model);
            }
        } else if (activity instanceof TaskEditActivity) {
            if (title.getText().length() == 0 && isNewTask && model != null && model.isSaved()) {
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
        if (title.getText().length() == 0 || TextUtils.isEmpty(model.getTitle())) {
            if (isNewTask) {
                TimerPlugin.updateTimer(notificationManager, taskService, getActivity(), model, false);
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
        new AlertDialog.Builder(getActivity()).setTitle(
                R.string.DLG_confirm_title).setMessage(
                        R.string.DLG_delete_this_task_question).setIcon(
                                android.R.drawable.ic_dialog_alert).setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TimerPlugin.updateTimer(notificationManager, taskService, getActivity(), model, false);
                        taskDeleter.delete(model);
                        shouldSaveState = false;

                        Activity a = getActivity();
                        if (a instanceof TaskEditActivity) {
                            getActivity().setResult(Activity.RESULT_OK);
                            getActivity().onBackPressed();
                        } else if (a instanceof TaskListActivity) {
                            discardButtonClick();
                            TaskListFragment tlf = ((TaskListActivity) a).getTaskListFragment();
                            if (tlf != null) {
                                tlf.refresh();
                            }
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void startAttachFile() {
        final List<Runnable> runnables = new ArrayList<>();
        List<String> options = new ArrayList<>();

        if (deviceInfo.hasCamera() || deviceInfo.hasGallery()) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    actFmCameraModule.showPictureLauncher(null);
                }
            });
            options.add(getString(R.string.file_add_picture));
        }
        runnables.add(new Runnable() {
            @Override
            public void run() {
                Intent attachFile = new Intent(getActivity(), FileExplore.class);
                startActivityForResult(attachFile, REQUEST_CODE_ATTACH_FILE);
            }
        });
        options.add(getString(R.string.file_add_sdcard));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                runnables.get(which).run();
            }
        };

        // show a menu of available options
        new AlertDialog.Builder(getActivity())
        .setAdapter(adapter, listener)
        .show().setOwnerActivity(getActivity());
    }

    private void startRecordingAudio() {
        Intent recordAudio = new Intent(getActivity(), AACRecordingActivity.class);
        startActivityForResult(recordAudio, REQUEST_CODE_RECORD);
    }

    private void attachFile(String file) {
        File src = new File(file);
        if (!src.exists()) {
            Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return;
        }

        File dst = new File(preferences.getAttachmentsDirectory() + File.separator + src.getName());
        try {
            AndroidUtilities.copyFile(src, dst);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return;
        }

        String path = dst.getAbsolutePath();
        String name = dst.getName();
        String extension = AndroidUtilities.getFileExtension(name);

        String type = TaskAttachment.FILE_TYPE_OTHER;
        if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap map = MimeTypeMap.getSingleton();
            String guessedType = map.getMimeTypeFromExtension(extension);
            if (!TextUtils.isEmpty(guessedType)) {
                type = guessedType;
            }
        }

        createNewFileAttachment(path, name, type);
    }

    private void attachImage(Uri uri) {
        try {
            String path = getPathFromUri(getActivity(), uri);
            File file = new File(path);
            String extension = path.substring(path.lastIndexOf('.') + 1);
            createNewFileAttachment(path, file.getName(), TaskAttachment.FILE_TYPE_IMAGE + extension);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
        }
    }

    private void createNewFileAttachment(String path, String fileName, String fileType) {
        TaskAttachment attachment = TaskAttachment.createNewAttachment(model.getUuid(), path, fileName, fileType);
        taskAttachmentDao.createNew(attachment);
        filesControlSet.refreshMetadata();
        filesControlSet.getView().setVisibility(View.VISIBLE);
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
        case R.id.menu_attach:
            startAttachFile();
            return true;
        case R.id.menu_record_note:
            startRecordingAudio();
            return true;
        case R.id.menu_delete:
            deleteButtonClick();
            return true;
        case android.R.id.home:
            if (title.getText().length() == 0) {
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
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity && ((TaskListActivity)activity).isDrawerOpen()) {
            return;
        }

        menu.clear();
        inflater.inflate(R.menu.task_edit_fragment, menu);
        if (preGingerbreadMR1()) {
            // media recorder aac support requires api level 10
            // approximately 1% of current installs are using api level 7-9
            menu.findItem(R.id.menu_record_note).setVisible(false);
        }
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
    public void onStart() {
        super.onStart();
        populateFields(getActivity().getIntent());
        if (isNewTask) {
            title.requestFocus();
            title.setCursorVisible(true);
            getActivity().getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (editNotes == null) {
            instantiateEditNotes();
        }

        if (requestCode == HideUntilControlSet.REQUEST_HIDE_UNTIL && resultCode == Activity.RESULT_OK) {
            long timestamp = data.getLongExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, 0L);
            if (timestamp > 0) {
                hideUntilControls.setCustomDate(timestamp);
            } else {
                log.error("Invalid timestamp");
            }
            return;
        } else if (editNotes != null && editNotes.activityResult(requestCode, resultCode, data)) {
            return;
        } else if (requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK) {
            String recordedAudioPath = data.getStringExtra(AACRecordingActivity.RESULT_OUTFILE);
            String recordedAudioName = data.getStringExtra(AACRecordingActivity.RESULT_FILENAME);
            createNewFileAttachment(recordedAudioPath, recordedAudioName, TaskAttachment.FILE_TYPE_AUDIO + "m4a"); //$NON-NLS-1$
        } else if (requestCode == REQUEST_CODE_ATTACH_FILE && resultCode == Activity.RESULT_OK) {
            attachFile(data.getStringExtra(FileExplore.RESULT_FILE_SELECTED));
        } else if (requestCode == REQUEST_CODE_BEAST_MODE) {
            loadEditPageOrder(true);
            new TaskEditBackgroundLoader().start();
            return;
        }

        actFmCameraModule.activityResult(requestCode, resultCode, data, new CameraResultCallback() {
            @Override
            public void handleCameraResult(Uri uri) {
                attachImage(uri);
            }
        });

        super.onActivityResult(requestCode, resultCode, data);
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

    // Tab Page listener when page/tab changes
    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int position) {
        setPagerHeightForPosition();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
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
        AndroidUtilities.hideSoftInputForViews(getActivity(), title, commentField);
        title.setCursorVisible(false);
        commentField.setCursorVisible(false);
    }
}
