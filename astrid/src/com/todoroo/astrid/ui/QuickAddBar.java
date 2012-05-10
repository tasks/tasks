package com.todoroo.astrid.ui;

import java.util.HashSet;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.EditPeopleControlSet;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.activity.TaskListFragment.OnTaskListItemClickedListener;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;

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
    private EditPeopleControlSet peopleControl;
    private boolean usePeopleControl = true;

    @Autowired AddOnService addOnService;
    @Autowired ExceptionService exceptionService;
    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired ActFmPreferenceService actFmPreferenceService;

    private VoiceInputAssistant voiceInputAssistant;

    private Activity activity;
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

    public void initialize(Activity myActivity, TaskListFragment myFragment,
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

        quickAddBox.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                quickAddControlsContainer.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            }
        });

        quickAddButton = ((ImageButton) findViewById(
                R.id.quickAddButton));

        // set listener for quick add button
        quickAddButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(), true);
                if (task != null && task.getValue(Task.TITLE).length() == 0) {
                    mListener.onTaskListItemClicked(task.getId());
                }
            }
        });

        // prepare and set listener for voice add button
        voiceAddButton = (ImageButton) findViewById(
                R.id.voiceAddButton);
        int prompt = R.string.voice_edit_title_prompt;
        if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
            prompt = R.string.voice_create_prompt;
        voiceInputAssistant = new VoiceInputAssistant(fragment,
                voiceAddButton, quickAddBox);
        voiceInputAssistant.configureMicrophoneButton(prompt);

        // set listener for extended addbutton
        quickAddButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(),
                        false);
                if (task == null)
                    return true;

                mListener.onTaskListItemClicked(task.getId());
                return true;
            }
        });

        if (addOnService.hasPowerPack()
                && Preferences.getBoolean(R.string.p_voiceInputEnabled, true)
                && voiceInputAssistant.isVoiceInputAvailable()) {
            voiceAddButton.setVisibility(View.VISIBLE);
        } else {
            voiceAddButton.setVisibility(View.GONE);
        }

        setUpQuickAddControlSets();
    }

    public void setUsePeopleControl(boolean usePeopleControl) {
        this.usePeopleControl = usePeopleControl;
        peopleControl.getDisplayView().setVisibility(usePeopleControl ? View.VISIBLE : View.GONE);
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
        deadlineControl.setIsQuickadd(true);

        peopleControl = new EditPeopleControlSet(activity, fragment,
                R.layout.control_set_assigned,
                R.layout.control_set_default_display,
                R.string.actfm_EPA_assign_label,
                TaskEditFragment.REQUEST_LOG_IN);

        resetControlSets();

        LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f);
        quickAddControls.addView(peopleControl.getDisplayView(), 0, lp);
        quickAddControls.addView(deadlineControl.getDisplayView(), 2, lp);
    }

    private void resetControlSets() {
        Task empty = new Task();
        TagData tagData = fragment.getActiveTagData();
        if (tagData != null) {
            HashSet<String> tagsTransitory = new HashSet<String>();
            tagsTransitory.add(tagData.getValue(TagData.NAME));
            empty.putTransitory(TaskService.TRANS_TAGS, tagsTransitory);
        }
        repeatControl.readFromTask(empty);
        gcalControl.readFromTask(empty);
        deadlineControl.readFromTask(empty);
        peopleControl.setUpData(empty, fragment.getActiveTagData());
        peopleControl.assignToMe();
        peopleControl.setTask(null);
    }


    // --- quick add task logic

    /**
     * Quick-add a new task
     *
     * @param title
     * @return
     */
    @SuppressWarnings("nls")
    public Task quickAddTask(String title, boolean selectNewTask) {
        TagData tagData = fragment.getActiveTagData();
        if(tagData != null && (!tagData.containsNonNullValue(TagData.NAME) ||
                tagData.getValue(TagData.NAME).length() == 0)) {
            DialogUtilities.okDialog(activity, activity.getString(R.string.tag_no_title_error), null);
            return null;
        }

        try {
            if (title != null)
                title = title.trim();
            boolean assignedToMe = usePeopleControl ? peopleControl.willBeAssignedToMe() : true;
            if (!assignedToMe && !actFmPreferenceService.isLoggedIn()) {
                DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        activity.startActivity(new Intent(activity, ActFmLoginActivity.class));
                    }
                };

                DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        // Reset people control
                        peopleControl.assignToMe();
                    }
                };
                DialogUtilities.okCancelCustomDialog(activity, activity.getString(R.string.actfm_EPA_login_button),
                        activity.getString(R.string.actfm_EPA_login_to_share), R.string.actfm_EPA_login_button,
                        R.string.actfm_EPA_dont_share_button, android.R.drawable.ic_dialog_alert,
                        okListener, cancelListener);
                return null;
            }

            Task task = new Task();
            if (title != null)
                task.setValue(Task.TITLE, title); // need this for calendar

            if (repeatControl.isRecurrenceSet())
                repeatControl.writeToModel(task);
            if (deadlineControl.isDeadlineSet()) {
                task.clearValue(Task.HIDE_UNTIL);
                deadlineControl.writeToModel(task);
                TaskDao.createDefaultHideUntil(task);
            }
            gcalControl.writeToModel(task);
            if (!assignedToMe) {
                peopleControl.setTask(task);
                peopleControl.saveSharingSettings(null);
            }

            TaskService.createWithValues(task, fragment.getFilter().valuesForNewTasks, title,
                    taskService, metadataService);

            String assignedTo = peopleControl.getAssignedToString();

            resetControlSets();

            addToCalendar(task, title, taskService);

            if(!TextUtils.isEmpty(title))
                fragment.showTaskEditHelpPopover();

            if (activity instanceof TaskListActivity && !assignedToMe)
                ((TaskListActivity) activity).switchToAssignedFilter(assignedTo);

            TextView quickAdd = (TextView) findViewById(R.id.quickAddText);
            quickAdd.setText(""); //$NON-NLS-1$

            if (selectNewTask) {
                fragment.loadTaskListContent(true);
                fragment.selectCustomId(task.getId());
                if (task.getTransitory(TaskService.TRANS_QUICK_ADD_MARKUP) != null) {
                    showAlertForMarkupTask((AstridActivity) activity, task, title);
                } else if (!TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
                    showAlertForRepeatingTask((AstridActivity) activity, task);
                }
            }

            fragment.incrementFilterCount();

            StatisticsService.reportEvent(StatisticsConstants.TASK_CREATED_TASKLIST);
            return task;
        } catch (Exception e) {
            exceptionService.displayAndReportError(activity,
                    "quick-add-task", e);
            return new Task();
        }
    }

    private static void addToCalendar(Task task, String title, TaskService taskService) {
        boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null
                && !Preferences.getStringValue(R.string.gcal_p_default).equals("-1"); //$NON-NLS-1$

        if (!TextUtils.isEmpty(title) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getValue(Task.CALENDAR_URI))) {
            Uri calendarUri = GCalHelper.createTaskEvent(task,
                    ContextManager.getContext().getContentResolver(), new ContentValues());
            task.setValue(Task.CALENDAR_URI, calendarUri.toString());
            task.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskService.save(task);
        }
    }

    /**
     * Static method to quickly add tasks without all the control set nonsense.
     * Used from the share link activity.
     * @param title
     * @return
     */
    public static Task basicQuickAddTask(String title) {
        if (TextUtils.isEmpty(title))
            return null;

        TaskService taskService = PluginServices.getTaskService();
        MetadataService metadataService = PluginServices.getMetadataService();
        title = title.trim();

        Task task = TaskService.createWithValues(null, title, taskService, metadataService);
        addToCalendar(task, title, taskService);

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
        if (voiceInputAssistant.handleActivityResult(requestCode, resultCode,
                data)) {
            // if user wants, create the task directly (with defaultvalues)
            // after saying it
            Flags.set(Flags.TLA_RESUMED_FROM_VOICE_ADD);
            if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
                quickAddTask(quickAddBox.getText().toString(), true);

            // the rest of onActivityResult is totally unrelated to
            // voicerecognition, so bail out
            return true;
        } else if (requestCode == TaskEditFragment.REQUEST_CODE_CONTACT) {
            if (resultCode == Activity.RESULT_OK)
                peopleControl.onActivityResult(requestCode, resultCode, data);
            else
                peopleControl.assignToMe();
            return true;
        }


        return false;
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(quickAddBox.getWindowToken(), 0);
    }

}
