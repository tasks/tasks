/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.files.FileUtilities;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.DeadlineControlSet;
import com.todoroo.astrid.ui.EditNotesControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ImportanceControlSet;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.voice.VoiceRecognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.ResourceResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import static android.support.v4.view.MenuItemCompat.setShowAsAction;

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

    private static final int REQUEST_VOICE_RECOG = 10;
    public static final int REQUEST_CODE_CONTACT = 20;
    public static final int REQUEST_CODE_RECORD = 30;
    public static final int REQUEST_CODE_ATTACH_FILE = 40;
    public static final int REQUEST_CODE_BEAST_MODE = 50;

    // --- menu codes

    private static final int MENU_SAVE_ID = R.string.TEA_menu_save;
    private static final int MENU_DISCARD_ID = R.string.TEA_menu_discard;
    private static final int MENU_ATTACH_ID = R.string.premium_attach_file;
    private static final int MENU_RECORD_ID = R.string.premium_record_audio;
    private static final int MENU_DELETE_TASK_ID = R.string.delete_task;

    // --- result codes

    public static final String OVERRIDE_FINISH_ANIM = "finishAnim"; //$NON-NLS-1$

    public static final String TOKEN_TAGS_CHANGED = "tags_changed";  //$NON-NLS-1$
    public static final String TOKEN_NEW_REPEATING_TASK = "new_repeating"; //$NON-NLS-1$

    // --- services

    public static final int TAB_VIEW_UPDATES = 0;

    @Inject TaskService taskService;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TagService tagService;
    @Inject MetadataService metadataService;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskDeleter taskDeleter;
    @Inject NotificationManager notificationManager;
    @Inject AlarmService alarmService;
    @Inject GCalHelper gcalHelper;
    @Inject ActivityPreferences preferences;
    @Inject DateChangedAlerts dateChangedAlerts;
    @Inject ResourceResolver resourceResolver;

    // --- UI components

    private EditNotesControlSet notesControlSet = null;
    private FilesControlSet filesControlSet = null;
    private TimerActionControlSet timerAction;
    private EditText title;
    private EditNoteActivity editNotes;
    private ViewPager mPager;
    private HashMap<String, TaskEditControlSet> controlSetMap = new HashMap<>();

    private final List<TaskEditControlSet> controls = Collections.synchronizedList(new ArrayList<TaskEditControlSet>());

    // --- other instance variables

    /** true if editing started with a new task */
    private boolean isNewTask = false;

    /** task model */
    Task model = null;

    /** whether task should be saved when this activity exits */
    private boolean shouldSaveState = true;

    /** voice assistant for notes-creation */
    private VoiceInputAssistant voiceNoteAssistant;

    private EditText notesEditText;

    private Dialog whenDialog;

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
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.task_edit_activity, container, false);
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
            editNotes = new EditNoteActivity(metadataService, userActivityDao, taskService, this, getView(),
                    idParam);
            editNotes.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));

            editNotes.addListener(this);
        }
    }

    private void loadMoreContainer() {
        View commentsBar = getView().findViewById(R.id.updatesFooter);

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

        mPager = (ViewPager) getView().findViewById(R.id.pager);
        mPager.setAdapter(adapter);

        if (showEditComments) {
            commentsBar.setVisibility(View.VISIBLE);
        }
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

        LinearLayout titleControls = (LinearLayout) getView().findViewById(
                R.id.title_controls);
        LinearLayout whenDialogView = (LinearLayout) LayoutInflater.from(
                getActivity()).inflate(R.layout.task_edit_when_controls, null);

        constructWhenDialog(whenDialogView);

        controlSetMap = new HashMap<>();

        // populate control set
        EditTitleControlSet editTitle = new EditTitleControlSet(taskService, getActivity(),
                R.layout.control_set_title, R.id.title);
        title = (EditText) editTitle.getView().findViewById(R.id.title);
        controls.add(editTitle);
        titleControls.addView(editTitle.getDisplayView(), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));

        timerAction = new TimerActionControlSet(taskService, getActivity(), getView());
        controls.add(timerAction);

        TagsControlSet tagsControlSet = new TagsControlSet(tagService, getActivity(),
                R.layout.control_set_tags, R.layout.control_set_default_display, R.string.TEA_tags_label_long);
        controls.add(tagsControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_lists_pref),
                tagsControlSet);

        RepeatControlSet repeatControls = new RepeatControlSet(getActivity(),
                R.layout.control_set_repeat,
                R.layout.control_set_repeat_display, R.string.repeat_enabled);

        GCalControlSet gcalControl = new GCalControlSet(gcalHelper, getActivity(),
                R.layout.control_set_gcal, R.layout.control_set_gcal_display,
                R.string.gcal_TEA_addToCalendar_label);

        // The deadline control set contains the repeat controls and the
        // calendar controls.
        // NOTE: we add the gcalControl AFTER the
        // deadline control, because
        // otherwise the correct date may not be written to the calendar event.
        // Order matters!
        DeadlineControlSet deadlineControl = new DeadlineControlSet(
                getActivity(), R.layout.control_set_deadline,
                R.layout.control_set_deadline_display, repeatControls,
                repeatControls.getDisplayView(), gcalControl.getDisplayView());
        controlSetMap.put(getString(R.string.TEA_ctrl_when_pref),
                deadlineControl);
        controls.add(repeatControls);
        repeatControls.addListener(editTitle);
        controls.add(deadlineControl);
        controls.add(gcalControl);

        ImportanceControlSet importanceControl = new ImportanceControlSet(
                getActivity(), R.layout.control_set_importance);
        controls.add(importanceControl);
        importanceControl.addListener(editTitle);
        controlSetMap.put(getString(R.string.TEA_ctrl_importance_pref),
                importanceControl);

        notesControlSet = new EditNotesControlSet(getActivity(),
                R.layout.control_set_notes, R.layout.control_set_notes_display);
        notesEditText = (EditText) notesControlSet.getView().findViewById(
                R.id.notes);
        controls.add(notesControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_notes_pref),
                notesControlSet);

        ReminderControlSet reminderControl = new ReminderControlSet(
                alarmService, getActivity(), R.layout.control_set_reminders,
                R.layout.control_set_default_display);
        controls.add(reminderControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_reminders_pref),
                reminderControl);

        HideUntilControlSet hideUntilControls = new HideUntilControlSet(getActivity(),
                R.layout.control_set_hide,
                R.layout.control_set_default_display,
                R.string.hide_until_prompt);
        controls.add(hideUntilControls);
        reminderControl.addViewToBody(hideUntilControls.getDisplayView());

        // TODO: Fix the fact that hideUntil doesn't update accordingly with date changes when lazy loaded. Until then, don't lazy load.
        hideUntilControls.getView();

        TimerControlSet timerControl = new TimerControlSet(getActivity(),
                R.layout.control_set_timers,
                R.layout.control_set_default_display,
                R.string.TEA_timer_controls);
        timerAction.addListener(timerControl);
        controls.add(timerControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_timer_pref), timerControl);

        filesControlSet = new FilesControlSet(taskAttachmentDao, getActivity(),
                R.layout.control_set_files, R.layout.control_set_files_display, R.string.TEA_control_files);
        controls.add(filesControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_files_pref), filesControlSet);

        loadEditPageOrder(false);

        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    private void loadEditPageOrder(boolean removeViews) {
        LinearLayout basicControls = (LinearLayout) getView().findViewById(
                R.id.basic_controls);
        if (removeViews) {
            basicControls.removeAllViews();
        }

        ArrayList<String> controlOrder = BeastModePreferences.constructOrderedControlList(preferences, getActivity());
        String[] itemOrder = controlOrder.toArray(new String[controlOrder.size()]);

        String hideAlwaysTrigger = getString(R.string.TEA_ctrl_hide_section_pref);

        Class<?> openControl = (Class<?>) getActivity().getIntent().getSerializableExtra(TOKEN_OPEN_CONTROL);

        for (int i = 0; i < itemOrder.length; i++) {
            String item = itemOrder[i];
            if (item.equals(hideAlwaysTrigger)) {
                break; // As soon as we hit the hide section, we're done
            } else {
                View controlSet = null;
                TaskEditControlSet curr = controlSetMap.get(item);

                if (curr != null) {
                    controlSet = curr.getDisplayView();
                }

                if (controlSet != null) {
                    if ((i + 1 >= itemOrder.length)) {
                        removeTeaSeparator(controlSet);
                    }
                    basicControls.addView(controlSet);
                }

                if (curr != null && curr.getClass().equals(openControl) && curr instanceof PopupControlSet) {
                    curr.getDisplayView().performClick();
                }
            }
        }

        getActivity().getIntent().removeExtra(TOKEN_OPEN_CONTROL);
    }

    private void removeTeaSeparator(View view) {

        View teaSeparator = view.findViewById(R.id.TEA_Separator);

        if (teaSeparator != null) {
            teaSeparator.setVisibility(View.GONE);
        }
    }

    private void constructWhenDialog(View whenDialogView) {
        int theme = ThemeService.getEditDialogTheme();
        whenDialog = new Dialog(getActivity(), theme);

        Button dismissDialogButton = (Button) whenDialogView.findViewById(R.id.when_dismiss);
        dismissDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtilities.dismissDialog(getActivity(), whenDialog);
            }
        });

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        whenDialog.setTitle(R.string.TEA_when_dialog_title);
        whenDialog.addContentView(whenDialogView, new LayoutParams(
                metrics.widthPixels - (int) (30 * metrics.density),
                LayoutParams.WRAP_CONTENT));
    }

    /**
     * Initialize task edit page in the background
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class TaskEditBackgroundLoader extends Thread {

        public void onUiThread() {
            // prepare and set listener for voice-button
            if (getActivity() != null) {
                if (VoiceRecognizer.voiceInputAvailable(getActivity())) {
                    ImageButton voiceAddNoteButton = (ImageButton) notesControlSet.getView().findViewById(
                            R.id.voiceAddNoteButton);
                    voiceAddNoteButton.setVisibility(View.VISIBLE);
                    int prompt = R.string.voice_edit_note_prompt;
                    voiceNoteAssistant = new VoiceInputAssistant(voiceAddNoteButton, REQUEST_VOICE_RECOG);
                    voiceNoteAssistant.setAppend();
                    voiceNoteAssistant.setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    voiceNoteAssistant.configureMicrophoneButton(TaskEditFragment.this, prompt);
                }
                loadMoreContainer();
            }
        }

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
                    onUiThread();
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
    protected void loadItem(Intent intent) {
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
                    values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
                }
            } catch (Exception e) {
                // oops, can't serialize
            }
            model = TaskService.createWithValues(taskService, metadataService, tagService, values, null);
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
        Activity activity = getActivity();
        if (activity instanceof TaskEditActivity) {
            ((TaskEditActivity) activity).updateTitle(isNewTask);
        }
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
                filesControlSet.getDisplayView().setVisibility(View.GONE);
            }
            for (TaskEditControlSet controlSet : controls) {
                controlSet.readFromTask(model);
            }
        }

    }

    /** Populate UI component values from the model */
    private void populateFields() {
        populateFields(getActivity().getIntent());
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
        model.putTransitory(TaskService.TRANS_EDIT_SAVE, true);
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
        ArrayList<String> options = new ArrayList<>();
        options.add(getString(R.string.file_add_picture));
        options.add(getString(R.string.file_add_sdcard));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if(which == 0) {
                    ActFmCameraModule.showPictureLauncher(TaskEditFragment.this, null);
                } else if (which == 1) {
                    Intent attachFile = new Intent(getActivity(), FileExplore.class);
                    startActivityForResult(attachFile, REQUEST_CODE_ATTACH_FILE);
                }
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

        File dst = new File(FileUtilities.getAttachmentsDirectory(preferences, getActivity()) + File.separator + src.getName());
        try {
            AndroidUtilities.copyFile(src, dst);
        } catch (Exception e) {
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

    private void attachImage(Bitmap bitmap) {

        AtomicReference<String> nameRef = new AtomicReference<>();
        String path = FileUtilities.getNewImageAttachmentPath(preferences, getActivity(), nameRef);

        try {
            FileOutputStream fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            createNewFileAttachment(path, nameRef.get(), TaskAttachment.FILE_TYPE_IMAGE + "png");
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
        }
    }

    private void createNewFileAttachment(String path, String fileName, String fileType) {
        TaskAttachment attachment = TaskAttachment.createNewAttachment(model.getUuid(), path, fileName, fileType);
        taskAttachmentDao.createNew(attachment);
        filesControlSet.refreshMetadata();
        filesControlSet.getDisplayView().setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SAVE_ID:
            saveButtonClick();
            return true;
        case MENU_DISCARD_ID:
            discardButtonClick();
            return true;
        case MENU_ATTACH_ID:
            startAttachFile();
            return true;
        case MENU_RECORD_ID:
            startRecordingAudio();
            return true;
        case MENU_DELETE_TASK_ID:
            deleteButtonClick();
            return true;
        case android.R.id.home:
            if (title.getText().length() == 0) {
                discardButtonClick();
            } else {
                saveButtonClick();
            }
            hideKeyboard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item;

        item = menu.add(Menu.NONE, MENU_ATTACH_ID, 0, R.string.premium_attach_file);
        item.setIcon(resourceResolver.getResource(R.attr.ic_action_new_attachment));

        setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // media recorder aac support requires api level 10
            // approximately 1% of current installs are using api level 7-9
            item = menu.add(Menu.NONE, MENU_RECORD_ID, 0, R.string.premium_record_audio);
            item.setIcon(resourceResolver.getResource(R.attr.ic_action_mic));
            setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        item = menu.add(Menu.NONE, MENU_DELETE_TASK_ID, 0, R.string.delete_task);
        item.setIcon(ThemeService.getDrawable(resourceResolver.getResource(R.attr.ic_action_discard)));
        setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);

        boolean useSaveAndCancel = preferences.getBoolean(R.string.p_save_and_cancel, false);

        if (useSaveAndCancel || preferences.useTabletLayout()) {
            if (useSaveAndCancel) {
                item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
                item.setIcon(resourceResolver.getResource(R.attr.ic_action_cancel));
                setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            if (!(getActivity() instanceof TaskEditActivity)) {
                item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
                item.setIcon(resourceResolver.getResource(R.attr.ic_action_save));
                setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
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
    public void onResume() {
        super.onResume();
        populateFields();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (editNotes == null) {
            instantiateEditNotes();
        }

        if (editNotes != null && editNotes.activityResult(requestCode, resultCode, data)) {
            return;
        } else if (requestCode == REQUEST_VOICE_RECOG
                && resultCode == Activity.RESULT_OK) {
            // handle the result of voice recognition, put it into the
            // appropiate textfield
            voiceNoteAssistant.handleActivityResult(requestCode, resultCode, data, notesEditText);

            // write the voicenote into the model, or it will be deleted by
            // onResume.populateFields
            // (due to the activity-change)
            notesControlSet.writeToModel(model);
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

        ActFmCameraModule.activityResult(getActivity(), requestCode, resultCode, data, new CameraResultCallback() {
            @Override
            public void handleCameraResult(Bitmap bitmap) {
                attachImage(bitmap);
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
        ScrollView scrollView = (ScrollView) getView().findViewById(R.id.edit_scroll);
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
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(title.getWindowToken(), 0);
    }
}
