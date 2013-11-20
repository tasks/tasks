/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.activity.TaskListFragment.OnTaskListItemClickedListener;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FileUtilities;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceRecognizer;

import org.tasks.R;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Quick Add Bar lets you add tasks.
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class QuickAddBar extends LinearLayout {

    private ImageButton voiceAddButton;
    private ImageButton quickAddButton;
    private EditText quickAddBox;
    private LinearLayout quickAddControls;
    private View quickAddControlsContainer;

    private DeadlineControlSet deadlineControl;
    private RepeatControlSet repeatControl;
    private GCalControlSet gcalControl;

    private String currentVoiceFile = null;

    @Autowired ExceptionService exceptionService;
    @Autowired
    private TaskAttachmentDao taskAttachmentDao;

    private VoiceRecognizer voiceRecognizer;

    private AstridActivity activity;
    private TaskListFragment fragment;

    public QuickAddBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public QuickAddBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QuickAddBar(Context context) {
        super(context);
    }

    public void initialize(AstridActivity myActivity, TaskListFragment myFragment,
            final OnTaskListItemClickedListener mListener) {
        activity = myActivity;
        fragment = myFragment;

        DependencyInjectionService.getInstance().inject(this);
        LayoutInflater.from(activity).inflate(R.layout.quick_add_bar, this);

        quickAddControls = (LinearLayout) findViewById(R.id.taskListQuickaddControls);
        quickAddControlsContainer = findViewById(R.id.taskListQuickaddControlsContainer);

        // set listener for pressing enter in quick-add box
        quickAddBox = (EditText) findViewById(R.id.quickAddText);
        quickAddBox.setOnEditorActionListener(new OnEditorActionListener() {
            /**
             * When user presses enter, quick-add the task
             */
            @Override
            public boolean onEditorAction(TextView view, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && !TextUtils.isEmpty(quickAddBox.getText().toString().trim())) {
                    quickAddTask(quickAddBox.getText().toString(), true);
                    return true;
                }
                return false;
            }
        });

        quickAddBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final boolean controlsVisible = !TextUtils.isEmpty(s) && quickAddBox.hasFocus();
                final boolean showControls = Preferences.getBoolean(R.string.p_show_quickadd_controls, true);

                final boolean plusVisible = !TextUtils.isEmpty(s);
                final boolean hidePlus = Preferences.getBoolean(R.string.p_hide_plus_button, false);
                quickAddControlsContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        quickAddButton.setVisibility((plusVisible || !hidePlus) ? View.VISIBLE : View.GONE);
                        quickAddControlsContainer.setVisibility((showControls && controlsVisible) ? View.VISIBLE : View.GONE);
                    }
                }, 10);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/**/}

            @Override
            public void afterTextChanged(Editable s) {/**/}
        });

        int fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 18);
        quickAddBox.setTextSize(Math.min(fontSize, 22));

        quickAddButton = ((ImageButton) findViewById(
                R.id.quickAddButton));
        quickAddButton.setVisibility(Preferences.getBoolean(R.string.p_hide_plus_button, false) ? View.GONE : View.VISIBLE);

        // set listener for quick add button
        quickAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(), true);
                if (task != null && task.getValue(Task.TITLE).length() == 0) {
                    mListener.onTaskListItemClicked(task.getId(), true);
                }
            }
        });

        // prepare and set listener for voice add button
        voiceAddButton = (ImageButton) findViewById(
                R.id.voiceAddButton);

        voiceAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognition();
            }
        });

        // set listener for extended addbutton
        quickAddButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(),
                        false);
                if (task == null) {
                    return true;
                }

                mListener.onTaskListItemClicked(task.getId(), true);
                return true;
            }
        });

        if (Preferences.getBoolean(R.string.p_voiceInputEnabled, true)
                && VoiceRecognizer.voiceInputAvailable(activity)) {
            voiceAddButton.setVisibility(View.VISIBLE);
        } else {
            voiceAddButton.setVisibility(View.GONE);
        }

        setUpQuickAddControlSets();
    }

    private void setUpQuickAddControlSets() {

        repeatControl = new RepeatControlSet(activity,
                R.layout.control_set_repeat,
                R.layout.control_set_repeat_display, R.string.repeat_enabled);

        gcalControl = new GCalControlSet(activity,
                R.layout.control_set_gcal, R.layout.control_set_gcal_display,
                R.string.gcal_TEA_addToCalendar_label);

        deadlineControl = new DeadlineControlSet(activity,
                R.layout.control_set_deadline,
                R.layout.control_set_default_display, null,
                repeatControl.getDisplayView(), gcalControl.getDisplayView());
        deadlineControl.setIsQuickadd();

        resetControlSets();

        LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f);
        View deadlineDisplay = deadlineControl.getDisplayView();
        quickAddControls.addView(deadlineDisplay, 0, lp);
        TextView tv = (TextView) deadlineDisplay.findViewById(R.id.display_row_edit);
        tv.setGravity(Gravity.LEFT);
    }

    private void resetControlSets() {
        Task empty = new Task();
        TagData tagData = fragment.getActiveTagData();
        if (tagData != null) {
            HashSet<String> tagsTransitory = new HashSet<>();
            tagsTransitory.add(tagData.getValue(TagData.NAME));
            empty.putTransitory(TaskService.TRANS_TAGS, tagsTransitory);
        }
        repeatControl.readFromTask(empty);
        gcalControl.readFromTask(empty);
        gcalControl.resetCalendarSelector();
        deadlineControl.readFromTask(empty);
    }


    // --- quick add task logic

    /**
     * Quick-add a new task
     */
    public Task quickAddTask(String title, boolean selectNewTask) {
        TagData tagData = fragment.getActiveTagData();
        if(tagData != null && (!tagData.containsNonNullValue(TagData.NAME) ||
                tagData.getValue(TagData.NAME).length() == 0)) {
            DialogUtilities.okDialog(activity, activity.getString(R.string.tag_no_title_error), null);
            return null;
        }

        try {
            if (title != null) {
                title = title.trim();
            }

            Task task = new Task();
            if (title != null) {
                task.setValue(Task.TITLE, title); // need this for calendar
            }

            if (repeatControl.isRecurrenceSet()) {
                repeatControl.writeToModel(task);
            }
            if (deadlineControl.isDeadlineSet()) {
                task.clearValue(Task.HIDE_UNTIL);
                deadlineControl.writeToModel(task);
                TaskDao.createDefaultHideUntil(task);
            }
            gcalControl.writeToModel(task);

            TaskService.createWithValues(task, fragment.getFilter().valuesForNewTasks, title);

            resetControlSets();

            addToCalendar(task, title);

            TextView quickAdd = (TextView) findViewById(R.id.quickAddText);
            quickAdd.setText(""); //$NON-NLS-1$

            if (selectNewTask) {
                fragment.loadTaskListContent(true);
                fragment.selectCustomId(task.getId());
                if (task.getTransitory(TaskService.TRANS_QUICK_ADD_MARKUP) != null) {
                    showAlertForMarkupTask(activity, task, title);
                } else if (!TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
                    showAlertForRepeatingTask(activity, task);
                }
            }

            if (currentVoiceFile != null) {

                AtomicReference<String> nameRef = new AtomicReference<>();
                String path = FileUtilities.getNewAudioAttachmentPath(activity, nameRef);

                voiceRecognizer.convert(path);
                currentVoiceFile = null;

                TaskAttachment attachment = TaskAttachment.createNewAttachment(task.getUuid(), path, nameRef.get(), TaskAttachment.FILE_TYPE_AUDIO + "m4a");
                taskAttachmentDao.createNew(attachment);
            }

            fragment.onTaskCreated(task);

            return task;
        } catch (Exception e) {
            exceptionService.displayAndReportError(activity,
                    "quick-add-task", e);
            return new Task();
        }
    }

    private static void addToCalendar(Task task, String title) {
        boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null
                && !Preferences.getStringValue(R.string.gcal_p_default).equals("-1") && task.hasDueDate(); //$NON-NLS-1$

        if (!TextUtils.isEmpty(title) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getValue(Task.CALENDAR_URI))) {
            Uri calendarUri = GCalHelper.createTaskEvent(task,
                    ContextManager.getContext().getContentResolver(), new ContentValues());
            task.setValue(Task.CALENDAR_URI, calendarUri.toString());
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            PluginServices.getTaskService().save(task);
        }
    }

    /**
     * Static method to quickly add tasks without all the control set nonsense.
     * Used from the share link activity.
     */
    public static Task basicQuickAddTask(String title) {
        if (TextUtils.isEmpty(title)) {
            return null;
        }

        title = title.trim();

        Task task = TaskService.createWithValues(null, title);
        addToCalendar(task, title);

        return task;
    }

    private static void showAlertForMarkupTask(AstridActivity activity, Task task, String originalText) {
        DateChangedAlerts.showQuickAddMarkupDialog(activity, task, originalText);
    }

    private static void showAlertForRepeatingTask(AstridActivity activity, Task task) {
        DateChangedAlerts.showRepeatChangedDialog(activity, task);
    }

    // --- instance methods

    public EditText getQuickAddBox() {
        return quickAddBox;
    }

    @Override
    public void clearFocus() {
        super.clearFocus();
        quickAddBox.clearFocus();
    }

    public void performButtonClick() {
        quickAddButton.performClick();
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle the result of voice recognition, put it into the textfield
        if (voiceRecognizer.handleActivityResult(requestCode, resultCode, data, quickAddBox)) {
            // if user wants, create the task directly (with defaultvalues)
            // after saying it
            Flags.set(Flags.TLA_RESUMED_FROM_VOICE_ADD);
            if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false)) {
                quickAddTask(quickAddBox.getText().toString(), true);
            }

            // the rest of onActivityResult is totally unrelated to
            // voicerecognition, so bail out
            return true;
        } else if (requestCode == TaskEditFragment.REQUEST_CODE_CONTACT) {
            return true;
        }


        return false;
    }


    public VoiceRecognizer getVoiceRecognizer() {
        return voiceRecognizer;
    }
    public void startVoiceRecognition() {
        if (VoiceRecognizer.speechRecordingAvailable(activity) && currentVoiceFile == null) {
            currentVoiceFile = Long.toString(DateUtilities.now());
        }
        voiceRecognizer.startVoiceRecognition(activity, fragment, currentVoiceFile);
    }

    public void setupRecognizerApi() {
        voiceRecognizer = VoiceRecognizer.instantiateVoiceRecognizer(activity, activity, voiceAddButton);
    }

    public void destroyRecognizerApi() {
        voiceRecognizer.destroyRecognizerApi();
    }
}
