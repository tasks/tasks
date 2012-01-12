/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.EditPeopleControlSet;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.opencrx.OpencrxControlSet;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevControlSet;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.DeadlineControlSet;
import com.todoroo.astrid.ui.EditNotesControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ImportanceControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.voice.VoiceInputAssistant;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button
 * pressed) as long as the task has a title.
 *
 * @author timsu
 *
 */
public final class TaskEditActivity extends Fragment {

    public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment";

    // --- bundle tokens

    /**
     * Task ID
     */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    /**
     * Content Values to set
     */
    public static final String TOKEN_VALUES = "v"; //$NON-NLS-1$

    /**
     * Task in progress (during orientation change)
     */
    private static final String TASK_IN_PROGRESS = "task_in_progress"; //$NON-NLS-1$

    /**
     * Tab to start on
     */
    public static final String TOKEN_TAB = "tab"; //$NON-NLS-1$

    // --- request codes

    private static final int REQUEST_LOG_IN = 0;
    private static final int REQUEST_VOICE_RECOG = 1;

    // --- menu codes

    private static final int MENU_SAVE_ID = R.string.TEA_menu_save;
    private static final int MENU_DISCARD_ID = R.string.TEA_menu_discard;
    private static final int MENU_DELETE_ID = R.string.TEA_menu_delete;

    // --- result codes

    public static final int RESULT_CODE_SAVED = Activity.RESULT_FIRST_USER;
    public static final int RESULT_CODE_DISCARDED = Activity.RESULT_FIRST_USER + 1;
    public static final int RESULT_CODE_DELETED = Activity.RESULT_FIRST_USER + 2;

    public static final String TAB_BASIC = "basic"; //$NON-NLS-1$

    public static final String TAB_SHARE = "share"; //$NON-NLS-1$

    public static final String TAB_ALARMS = "alarms"; //$NON-NLS-1$

    public static final String TAB_MORE = "more"; //$NON-NLS-1$

    public static final String OVERRIDE_FINISH_ANIM = "finishAnim"; //$NON-NLS-1$

    public static final String TOKEN_TASK_WAS_ASSIGNED = "task_assigned"; //$NON-NLS-1$

    public static final String TOKEN_ASSIGNED_TO = "task_assigned_to"; //$NON-NLS-1$

    // --- services

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private Database database;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private AddOnService addOnService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

	// --- UI components

    private ImageButton voiceAddNoteButton;

    private EditPeopleControlSet peopleControlSet = null;
    private EditNotesControlSet notesControlSet = null;
    private HideUntilControlSet hideUntilControls = null;
    private TagsControlSet tagsControlSet = null;
    private EditText title;

    private final List<TaskEditControlSet> controls =
        Collections.synchronizedList(new ArrayList<TaskEditControlSet>());

	// --- other instance variables

    /** true if editing started with a new task */
    boolean isNewTask = false;

	/** task model */
	Task model = null;

	/** whether task should be saved when this activity exits */
	private boolean shouldSaveState = true;

	/** edit control receiver */
	private final ControlReceiver controlReceiver = new ControlReceiver();

    /** voice assistant for notes-creation */
    private VoiceInputAssistant voiceNoteAssistant;

    private EditText notesEditText;

    private Dialog whenDialog;

    private boolean overrideFinishAnim;

    // --- fragment handling variables
    OnTaskEditDetailsClickedListener mListener;
    private boolean mDualFragments = false;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    /** Container Activity must implement this interface and we ensure
     * that it does during the onAttach() callback
     */
    public interface OnTaskEditDetailsClickedListener {
        public void onTaskEditDetailsClicked(int category, int position);
    }


    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback interface
        try {
            mListener = (OnTaskEditDetailsClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTaskEditDetailsClickedListener"); //$NON-NLS-1$
        }
    }

    public TaskEditActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // Tell the framework to try to keep this fragment around
        // during a configuration change.
        setRetainInstance(true);

		new StartupService().onStartupApplication(getActivity());

		// if we were editing a task already, restore it
		if(savedInstanceState != null && savedInstanceState.containsKey(TASK_IN_PROGRESS)) {
		    Task task = savedInstanceState.getParcelable(TASK_IN_PROGRESS);
		    if(task != null) {
		        model = task;
		    }
		}

		getActivity().setResult(Activity.RESULT_OK);
    }

    /* ======================================================================
     * ==================================================== UI initialization
     * ====================================================================== */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.task_edit_activity,
              container, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);
        getSupportActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment tasklistFrame = getFragmentManager().findFragmentByTag(TaskListActivity.TAG_TASKLIST_FRAGMENT);
        mDualFragments = (tasklistFrame != null) && tasklistFrame.isInLayout();

        setUpUIComponents();
        adjustInfoPopovers();

        overrideFinishAnim = getActivity().getIntent().getBooleanExtra(OVERRIDE_FINISH_ANIM, true);

        // disable keyboard until user requests it
        AndroidUtilities.suppressVirtualKeyboard(title);
    }

    /** Initialize UI components */
    private void setUpUIComponents() {
        LinearLayout basicControls = (LinearLayout) getView().findViewById(R.id.basic_controls);
        LinearLayout whenDialogView = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                R.layout.task_edit_when_controls, null);
        LinearLayout moreControls = (LinearLayout) getView().findViewById(R.id.more_controls);

        constructWhenDialog(whenDialogView);

        HashMap<String, TaskEditControlSet> controlSetMap = new HashMap<String, TaskEditControlSet>();

        // populate control set
        EditTitleControlSet editTitle = new EditTitleControlSet(getActivity(),
                R.layout.control_set_title, Task.TITLE, R.id.title);
        title = (EditText) editTitle.getView().findViewById(R.id.title);
        controls.add(editTitle);
        controlSetMap.put(getString(R.string.TEA_ctrl_title_pref), editTitle);

        TimerActionControlSet timerAction = new TimerActionControlSet(getActivity(),
                editTitle.getView());
        controls.add(timerAction);

        controls.add(peopleControlSet = new EditPeopleControlSet(
                getActivity(), R.layout.control_set_assigned,
                R.layout.control_set_assigned_display,
                R.string.actfm_EPA_assign_label, REQUEST_LOG_IN));
        controlSetMap.put(getString(R.string.TEA_ctrl_who_pref),
                peopleControlSet);

        RepeatControlSet repeatControls = new RepeatControlSet(
                getActivity(), R.layout.control_set_repeat,
                R.layout.control_set_repeat_display, R.string.repeat_enabled);
        controls.add(repeatControls);

        GCalControlSet gcalControl = new GCalControlSet(getActivity(),
                R.layout.control_set_gcal, R.layout.control_set_gcal_display,
                R.string.gcal_TEA_addToCalendar_label);

        //The deadline control set contains the repeat controls and the calendar controls.
        //NOTE: we add the gcalControl to the list AFTER the deadline control, because
        //otherwise the correct date may not be written to the calendar event. Order matters!
        DeadlineControlSet deadlineControl = new DeadlineControlSet(
                getActivity(), R.layout.control_set_deadline,
                R.layout.control_set_deadline_display, repeatControls.getDisplayView(), gcalControl.getDisplayView());
        controls.add(deadlineControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_when_pref), deadlineControl);
        controls.add(gcalControl);

        hideUntilControls = new HideUntilControlSet(getActivity(),
                R.layout.control_set_hide, R.layout.control_set_hide_display,
                R.string.hide_until_prompt);
        controls.add(hideUntilControls);

        ImportanceControlSet importanceControl = new ImportanceControlSet(
                getActivity(), R.layout.control_set_importance);
        controls.add(importanceControl);
        importanceControl.addListener(editTitle);
        controlSetMap.put(getString(R.string.TEA_ctrl_importance_pref),
                importanceControl);

        tagsControlSet = new TagsControlSet(getActivity(),
                R.layout.control_set_tags, R.layout.control_set_tags_display,
                R.string.TEA_tags_label);
        controls.add(tagsControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_lists_pref),
                tagsControlSet);

        notesControlSet = new EditNotesControlSet(getActivity(),
                R.layout.control_set_notes, R.layout.control_set_notes_display);
        notesEditText = (EditText) notesControlSet.getView().findViewById(
                R.id.notes);
        controls.add(notesControlSet);
        controlSetMap.put(getString(R.string.TEA_ctrl_notes_pref),
                notesControlSet);

        ReminderControlSet reminderControl = new ReminderControlSet(
                getActivity(), R.layout.control_set_reminders,
                R.layout.control_set_reminders_display);
        controls.add(reminderControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_reminders_pref),
                reminderControl);

        hideUntilControls = new HideUntilControlSet(getActivity(),
                R.layout.control_set_hide, R.layout.control_set_hide_display,
                R.string.hide_until_prompt);
        controls.add(hideUntilControls);
        reminderControl.addViewToBody(hideUntilControls.getDisplayView());

        TimerControlSet timerControl = new TimerControlSet(
                getActivity(), R.layout.control_set_timers,
                R.layout.control_set_timers_extras_display,
                R.string.TEA_timer_controls);
        timerAction.setListener(timerControl);
        controls.add(timerControl);
        controlSetMap.put(getString(R.string.TEA_ctrl_timer_pref), timerControl);

        try {
            if(ProducteevUtilities.INSTANCE.isLoggedIn()) {
                ProducteevControlSet producteevControl = new ProducteevControlSet(getActivity(), R.layout.control_set_producteev, R.layout.control_set_producteev_display, R.string.producteev_TEA_control_set_display);
                controls.add(producteevControl);
                basicControls.addView(producteevControl.getDisplayView());
                notesEditText.setHint(R.string.producteev_TEA_notes);
            }
        } catch (Exception e) {
            Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        try {
            if(OpencrxCoreUtils.INSTANCE.isLoggedIn()) {
                OpencrxControlSet ocrxControl = new OpencrxControlSet(getActivity(), R.layout.control_set_opencrx, R.layout.control_set_opencrx_display, R.string.opencrx_TEA_opencrx_title);
                controls.add(ocrxControl);
                basicControls.addView(ocrxControl.getDisplayView());
                notesEditText.setHint(R.string.opencrx_TEA_notes);
            }
        } catch (Exception e) {
            Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String[] itemOrder;
        String orderPreference = Preferences.getStringValue(BeastModePreferenceActivity.BEAST_MODE_ORDER_PREF);
        if (orderPreference != null)
            itemOrder = orderPreference.split(BeastModePreferenceActivity.BEAST_MODE_PREF_ITEM_SEPARATOR);
        else
            itemOrder = getResources().getStringArray(R.array.TEA_control_sets_prefs);
        String moreSectionTrigger = getString(R.string.TEA_ctrl_more_pref);
        String shareViewDescriptor = getString(R.string.TEA_ctrl_share_pref);
        LinearLayout section = basicControls;
        for (int i = 0; i < itemOrder.length; i++) {
            String item = itemOrder[i];
            if (item.equals(moreSectionTrigger)) {
                section = moreControls;
            } else {
                TaskEditControlSet curr = controlSetMap.get(item);
                if (item.equals(shareViewDescriptor))
                    section.addView(peopleControlSet.getSharedWithRow());
                else if (curr != null)
                    section.addView(curr.getDisplayView());
            }
        }
        if (moreControls.getChildCount() == 0)
            getView().findViewById(R.id.more_header).setVisibility(View.GONE);



        // Load task data in background
        new TaskEditBackgroundLoader().start();
    }

    /** Set up button listeners */
    private void setUpListeners() {
        final View.OnClickListener mSaveListener = new View.OnClickListener() {
            public void onClick(View v) {
                saveButtonClick();
            }
        };
        final View.OnClickListener mDiscardListener = new View.OnClickListener() {
            public void onClick(View v) {
                discardButtonClick();
            }
        };
        final View.OnClickListener mExpandWhenListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (whenDialog != null)
                    whenDialog.show();
                Preferences.setBoolean(R.string.p_showed_when_row, true);
            }
        };
        final View.OnClickListener mExpandMoreListener = new View.OnClickListener() {
            final Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            final Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            @Override
            public void onClick(View v) {
                fadeIn.setDuration(300);
                fadeOut.setDuration(300);
                View moreView = getView().findViewById(R.id.more_controls);
                View moreHeader = getView().findViewById(R.id.more_header);
                if (moreView.getVisibility() == View.GONE) {
                    moreView.setVisibility(View.VISIBLE);
                    moreView.startAnimation(fadeIn);
                    moreHeader.setVisibility(View.GONE);
                    moreHeader.startAnimation(fadeOut);
                }
            }
        };

        // set up save, cancel, and delete buttons
        try {
            Button saveButtonGeneral = (Button) getView().findViewById(R.id.save);
            saveButtonGeneral.setOnClickListener(mSaveListener);

            Button discardButtonGeneral = (Button) getView().findViewById(R.id.discard);
            discardButtonGeneral.setOnClickListener(mDiscardListener);

            getView().findViewById(R.id.more_header).setOnClickListener(mExpandMoreListener);

            getView().findViewById(R.id.activity).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launchIntent = new Intent(getActivity(), EditNoteActivity.class);
                    launchIntent.putExtra(EditNoteActivity.EXTRA_TASK_ID, model.getId());
                    startActivity(launchIntent);
                }
            });
        } catch (Exception e) {
            // error loading the proper activity
        }
    }

    private void constructWhenDialog(View whenDialogView) {
        int theme = ThemeService.getDialogTheme();
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
        whenDialog.addContentView(whenDialogView, new LayoutParams(metrics.widthPixels - (int)(30 * metrics.density), LayoutParams.WRAP_CONTENT));
    }

    /**
     * Initialize task edit page in the background
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    class TaskEditBackgroundLoader extends Thread {

        public void onUiThread() {
            // prepare and set listener for voice-button
            if(addOnService.hasPowerPack()) {
                voiceAddNoteButton = (ImageButton) notesControlSet.getView().findViewById(R.id.voiceAddNoteButton);
                voiceAddNoteButton.setVisibility(View.VISIBLE);
                int prompt = R.string.voice_edit_note_prompt;
                voiceNoteAssistant = new VoiceInputAssistant(getActivity(), voiceAddNoteButton,
                        notesEditText, REQUEST_VOICE_RECOG);
                voiceNoteAssistant.setAppend(true);
                voiceNoteAssistant.setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                voiceNoteAssistant.configureMicrophoneButton(prompt);
            }

            // re-read all
            synchronized(controls) {
                for(TaskEditControlSet controlSet : controls)
                    controlSet.readFromTask(model);
                if (isNewTask) {
                    hideUntilControls.setDefaults();
                }
                autoExpand();
            }
        }

        private void autoExpand() {
            LinearLayout moreControls = (LinearLayout) getView().findViewById(R.id.more_controls);
            LinearLayout moreHeader = (LinearLayout) getView().findViewById(R.id.more_header);

            if (notesControlSet.hasNotes() && notesControlSet.getDisplayView().getParent() == moreControls) {
                moreHeader.performClick();
            } else if (tagsControlSet.hasLists() && tagsControlSet.getDisplayView().getParent() == moreControls) {
                moreHeader.performClick();
            }
        }

        @Override
        public void run() {
            AndroidUtilities.sleepDeep(500L);

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    onUiThread();
                }
            });

            // set up listeners
            setUpListeners();
        }
    }

    /* ======================================================================
     * =============================================== model reading / saving
     * ====================================================================== */

    /**
     * Loads action item from the given intent
     * @param intent
     */
    @SuppressWarnings("nls")
    protected void loadItem(Intent intent) {
        if(model != null) {
            // came from bundle
            isNewTask = (model.getValue(Task.TITLE).length() == 0);
            return;
        }

        long idParam = intent.getLongExtra(TOKEN_ID, -1L);

        database.openForReading();
        if(idParam > -1L) {
            model = taskService.fetchById(idParam, Task.PROPERTIES);
            if (model != null)
                model.clearValue(Task.REMOTE_ID); // Having this can screw up autosync
        }

        // not found by id or was never passed an id
        if(model == null) {
            String valuesAsString = intent.getStringExtra(TOKEN_VALUES);
            ContentValues values = null;
            try {
                if(valuesAsString != null)
                    values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
            } catch (Exception e) {
                // oops, can't serialize
            }
            model = TaskListActivity.createWithValues(values, null, taskService, metadataService);
        }

        if(model.getValue(Task.TITLE).length() == 0) {
            StatisticsService.reportEvent(StatisticsConstants.CREATE_TASK);
            isNewTask = true;

            // set deletion date until task gets a title
            model.setValue(Task.DELETION_DATE, DateUtilities.now());
        } else {
            StatisticsService.reportEvent(StatisticsConstants.EDIT_TASK);
        }

        if(model == null) {
            exceptionService.reportError("task-edit-no-task",
                    new NullPointerException("model"));
            getActivity().finish();
            return;
        }

        if (!isNewTask) {
            if (actFmPreferenceService.isLoggedIn()) {
                getView().findViewById(R.id.activityContainer).setVisibility(View.VISIBLE);
            }
        }

        // clear notification
        Notifications.cancelNotifications(model.getId());
    }

    /** Convenience method to populate fields after setting model to null */
    public void repopulateFromScratch(Intent intent) {
        model = null;
        populateFields(intent);
    }

    /** Populate UI component values from the model */
    public void populateFields(Intent intent) {
        loadItem(intent);

        TextView titleText = (TextView) getView().findViewById(R.id.taskLabel);
        if(isNewTask)
            titleText.setText(R.string.TEA_view_titleNew);
        else
            titleText.setText(model.getValue(Task.TITLE));

        synchronized(controls) {
            for(TaskEditControlSet controlSet : controls)
                controlSet.readFromTask(model);
        }
    }

    /** Populate UI component values from the model */
    private void populateFields() {
        populateFields(getActivity().getIntent());
    }

    /** Save task model from values in UI components */
    public void save(boolean onPause) {
        if(title.getText().length() > 0)
            model.setValue(Task.DELETION_DATE, 0L);

        if(title.getText().length() == 0)
            return;

        StringBuilder toast = new StringBuilder();
        synchronized(controls) {
            for(TaskEditControlSet controlSet : controls) {
                String toastText = controlSet.writeToModel(model);
                if(toastText != null)
                    toast.append('\n').append(toastText);
            }
        }


        String processedToast = addDueTimeToToast(toast.toString());
        boolean cancelFinish = !onPause && peopleControlSet != null &&
            !peopleControlSet.saveSharingSettings(processedToast);

        model.putTransitory("task-edit-save", true); //$NON-NLS-1$
        taskService.save(model);


        if (!onPause && !cancelFinish) {
            if (!peopleControlSet.isAssignedToMe()) {
                Intent data = new Intent();
                data.putExtra(TOKEN_TASK_WAS_ASSIGNED, true);
                data.putExtra(TOKEN_ASSIGNED_TO, peopleControlSet.getAssignedToString());
                getActivity().setResult(Activity.RESULT_OK, data);
            }

            shouldSaveState = false;
            getActivity().finish();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            save(false);
            return true;
        }
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // abandon editing and delete the newly created task if
        // no title was entered
        if (overrideFinishAnim) {
            AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_right_in, R.anim.slide_right_out);
        }

        if(title.getText().length() == 0 && isNewTask && model.isSaved()) {
            taskService.delete(model);
        }
    }

    /* ======================================================================
     * ================================================ edit control handling
     * ====================================================================== */

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class ControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // add a separator
                View separator = new View(getActivity());
                separator.setPadding(5, 5, 5, 5);
                separator.setBackgroundResource(android.R.drawable.divider_horizontal_dark);

            } catch (Exception e) {
                exceptionService.reportError("receive-detail-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON), e);
            }
        }
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    protected void saveButtonClick() {
        save(false);
    }

    /**
     * Displays a Toast reporting that the selected task has been saved and, if
     * it has a due date, that is due in 'x' amount of time, to 1 time-unit of
     * precision
     * @param additionalMessage
     */
    private String addDueTimeToToast(String additionalMessage) {
        int stringResource;

        long due = model.getValue(Task.DUE_DATE);
        String toastMessage;
        if (due != 0) {
            stringResource = R.string.TEA_onTaskSave_due;
            CharSequence formattedDate =
                DateUtilities.getRelativeDay(getActivity(), due);
            toastMessage = getString(stringResource, formattedDate);
        } else {
            toastMessage = getString(R.string.TEA_onTaskSave_notDue);
        }

        return toastMessage + additionalMessage;
    }

    protected void discardButtonClick() {
        shouldSaveState = false;

        // abandon editing in this case
        if(title.getText().length() == 0 || TextUtils.isEmpty(model.getValue(Task.TITLE))) {
            if(isNewTask)
                taskService.delete(model);
        }

        showCancelToast();
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    /**
     * Show toast for task edit canceling
     */
    private void showCancelToast() {
        Toast.makeText(getActivity(), R.string.TEA_onTaskCancel,
                Toast.LENGTH_SHORT).show();
    }

    protected void deleteButtonClick() {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.DLG_confirm_title)
            .setMessage(R.string.DLG_delete_this_task_question)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    taskService.delete(model);
                    shouldSaveState = false;
                    showDeleteToast();
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    /**
     * Show toast for task edit deleting
     */
    private void showDeleteToast() {
        Toast.makeText(getActivity(), R.string.TEA_onTaskDelete,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case MENU_SAVE_ID:
            saveButtonClick();
            return true;
        case MENU_DISCARD_ID:
            discardButtonClick();
            return true;
        case MENU_DELETE_ID:
            deleteButtonClick();
            return true;
        case android.R.id.home:
            saveButtonClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item;

        item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
        item.setIcon(android.R.drawable.ic_menu_save);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, MENU_DELETE_ID, 0, R.string.TEA_menu_delete);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
        getActivity().unregisterReceiver(controlReceiver);

        if(shouldSaveState)
            save(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        StatisticsService.sessionStart(getActivity());
        getActivity().registerReceiver(controlReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_EDIT_CONTROLS));
        populateFields();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_VOICE_RECOG && resultCode == Activity.RESULT_OK) {
            // handle the result of voice recognition, put it into the appropiate textfield
            voiceNoteAssistant.handleActivityResult(requestCode, resultCode, data);

            // write the voicenote into the model, or it will be deleted by onResume.populateFields
            // (due to the activity-change)
            notesControlSet.writeToModel(model);
        }

        // respond to sharing logoin
        peopleControlSet.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // stick our task into the outState
        outState.putParcelable(TASK_IN_PROGRESS, model);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        StatisticsService.sessionStop(getActivity());
    }

    private void adjustInfoPopovers() {
        Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
        if (!Preferences.isSet(getString(R.string.p_showed_lists_help)))
            Preferences.setBoolean(R.string.p_showed_lists_help, false);
    }

    /* ======================================================================
     * ========================================== UI component helper classes
     * ====================================================================== */

}
