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
import java.util.List;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.EditPeopleControlSet;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.opencrx.OpencrxControlSet;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevControlSet;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTextControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ImportanceControlSet;
import com.todoroo.astrid.ui.RandomReminderControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.ui.UrgencyControlSet;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.voice.VoiceInputAssistant;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button
 * pressed) as long as the task has a title.
 *
 * @author timsu
 *
 */
public final class TaskEditActivity extends TabActivity {

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

    private static final int MENU_SAVE_ID = Menu.FIRST;
    private static final int MENU_DISCARD_ID = Menu.FIRST + 1;
    private static final int MENU_DELETE_ID = Menu.FIRST + 2;

    // --- result codes

    public static final int RESULT_CODE_SAVED = RESULT_FIRST_USER;
    public static final int RESULT_CODE_DISCARDED = RESULT_FIRST_USER + 1;
    public static final int RESULT_CODE_DELETED = RESULT_FIRST_USER + 2;

    public static final String TAB_BASIC = "basic"; //$NON-NLS-1$

    public static final String TAB_SHARE = "share"; //$NON-NLS-1$

    public static final String TAB_ALARMS = "alarms"; //$NON-NLS-1$

    public static final String TAB_MORE = "more"; //$NON-NLS-1$

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

	// --- UI components

    private ImageButton voiceAddNoteButton;

    private EditPeopleControlSet peopleControlSet = null;
    private EditTextControlSet notesControlSet = null;
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
    private VoiceInputAssistant voiceNoteAssistant = null;

    private EditText notesEditText;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public TaskEditActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    ThemeService.applyTheme(this);
		super.onCreate(savedInstanceState);
		new StartupService().onStartupApplication(this);

        setUpUIComponents();

		// disable keyboard until user requests it
		AndroidUtilities.suppressVirtualKeyboard(title);

		// if we were editing a task already, restore it
		if(savedInstanceState != null && savedInstanceState.containsKey(TASK_IN_PROGRESS)) {
		    Task task = savedInstanceState.getParcelable(TASK_IN_PROGRESS);
		    if(task != null) {
		        model = task;
		    }
		}

		setResult(RESULT_OK);
    }

    /* ======================================================================
     * ==================================================== UI initialization
     * ====================================================================== */

    /** Initialize UI components */
    private void setUpUIComponents() {
        final Resources r = getResources();

        // set up tab host
        final TabHost tabHost = getTabHost();
        tabHost.setPadding(0, 4, 0, 0);
        LayoutInflater.from(this).inflate(R.layout.task_edit_activity,
                tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec(TAB_BASIC).
                setIndicator(r.getString(R.string.TEA_tab_basic),
                        r.getDrawable(R.drawable.gl_pencil)).setContent(
                                R.id.tab_basic));
        tabHost.addTab(tabHost.newTabSpec(TAB_SHARE).
                setIndicator(r.getString(R.string.TEA_tab_share),
                        r.getDrawable(R.drawable.gl_group)).setContent(
                                R.id.tab_share));
        tabHost.addTab(tabHost.newTabSpec(TAB_ALARMS).
                setIndicator(r.getString(R.string.TEA_tab_alarms),
                        r.getDrawable(R.drawable.gl_alarm)).setContent(
                                R.id.tab_alarms));
        tabHost.addTab(tabHost.newTabSpec(TAB_MORE).
                setIndicator(r.getString(R.string.TEA_tab_more),
                        r.getDrawable(R.drawable.gl_more)).setContent(
                                R.id.tab_more));
        tabHost.setBackgroundDrawable(findViewById(R.id.taskEditParent).getBackground());
        //getTabWidget().setBackgroundColor(Color.WHITE);
        AndroidUtilities.callApiMethod(8, getTabWidget(), "setStripEnabled", //$NON-NLS-1$
                new Class<?>[] { boolean.class }, false);
        if(getIntent().hasExtra(TOKEN_TAB))
            tabHost.setCurrentTabByTag(getIntent().getStringExtra(TOKEN_TAB));
        OnTabChangeListener tabChange = new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++)
                    tabHost.getTabWidget().getChildAt(i).setBackgroundResource(0);
                View child = tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab());
                if(child != null)
                    child.setBackgroundColor(r.getColor(R.color.task_edit_selected));
            }
        };
        tabChange.onTabChanged(null);
        tabHost.setOnTabChangedListener(tabChange);

        // populate control set
        title = (EditText) findViewById(R.id.title);
        controls.add(new EditTextControlSet(this, Task.TITLE, R.id.title));
        controls.add(new ImportanceControlSet(this, R.id.importance_container));
        controls.add(new UrgencyControlSet(this, R.id.urgency_date, R.id.urgency_time));
        notesEditText = (EditText) findViewById(R.id.notes);

        // prepare and set listener for voice-button
        if(addOnService.hasPowerPack()) {
            voiceAddNoteButton = (ImageButton) findViewById(R.id.voiceAddNoteButton);
            voiceAddNoteButton.setVisibility(View.VISIBLE);
            int prompt = R.string.voice_edit_note_prompt;
            voiceNoteAssistant = new VoiceInputAssistant(this, voiceAddNoteButton,
                    notesEditText, REQUEST_VOICE_RECOG);
            voiceNoteAssistant.setAppend(true);
            voiceNoteAssistant.setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            voiceNoteAssistant.configureMicrophoneButton(prompt);
        }

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

        // set up save, cancel, and delete buttons
        try {
            Button saveButtonGeneral = (Button) findViewById(R.id.save);
            saveButtonGeneral.setOnClickListener(mSaveListener);

            Button discardButtonGeneral = (Button) findViewById(R.id.discard);
            discardButtonGeneral.setOnClickListener(mDiscardListener);
        } catch (Exception e) {
            // error loading the proper activity
        }
    }

    /**
     * Initialize task edit page in the background
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    class TaskEditBackgroundLoader extends Thread {

        public void onUiThread() {
            // internal add-ins
            controls.add(new TagsControlSet(TaskEditActivity.this, R.id.tags_container));

            controls.add(new RepeatControlSet(TaskEditActivity.this,
                    (LinearLayout) findViewById(R.id.addons_urgency)));
            LinearLayout alarmsAddons = (LinearLayout) findViewById(R.id.addons_alarms);
            LinearLayout moreAddons = (LinearLayout) findViewById(R.id.addons_more);
            controls.add(new GCalControlSet(TaskEditActivity.this, moreAddons));


            try {
                if(ProducteevUtilities.INSTANCE.isLoggedIn()) {
                    controls.add(new ProducteevControlSet(TaskEditActivity.this, moreAddons));
                    notesEditText.setHint(R.string.producteev_TEA_notes);
                    ((TextView)findViewById(R.id.notes_label)).setHint(R.string.producteev_TEA_notes);
                }
            } catch (Exception e) {
                Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            try {
                if(OpencrxCoreUtils.INSTANCE.isLoggedIn()) {
                    controls.add(new OpencrxControlSet(TaskEditActivity.this, moreAddons));
                    notesEditText.setHint(R.string.opencrx_TEA_notes);
                    ((TextView)findViewById(R.id.notes_label)).setHint(R.string.opencrx_TEA_notes);
                }
            } catch (Exception e) {
                Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            controls.add(new TimerControlSet(TaskEditActivity.this, moreAddons));
            controls.add(new AlarmControlSet(TaskEditActivity.this, alarmsAddons));

            if(!Constants.MARKET_DISABLED && !addOnService.hasPowerPack()) {
                // show add-on help if necessary
                View addonsEmpty = findViewById(R.id.addons_empty);
                addonsEmpty.setVisibility(View.VISIBLE);
                moreAddons.removeView(addonsEmpty);
                moreAddons.addView(addonsEmpty);
                ((Button)findViewById(R.id.addons_button)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent addOnActivity = new Intent(TaskEditActivity.this, AddOnActivity.class);
                        addOnActivity.putExtra(AddOnActivity.TOKEN_START_WITH_AVAILABLE, true);
                        startActivity(addOnActivity);
                    }
                });
            }

            controls.add(new ReminderControlSet(TaskEditActivity.this,
                    R.id.reminder_due, R.id.reminder_overdue, R.id.reminder_alarm));
            controls.add(new RandomReminderControlSet(TaskEditActivity.this,
                    R.id.reminder_random, R.id.reminder_random_interval));
            HideUntilControlSet hideUntilControls = new HideUntilControlSet(TaskEditActivity.this, R.id.hideUntil);
            controls.add(hideUntilControls);
            controls.add(peopleControlSet = new EditPeopleControlSet(
                    TaskEditActivity.this, REQUEST_LOG_IN));

            // re-read all
            synchronized(controls) {
                for(TaskEditControlSet controlSet : controls)
                    controlSet.readFromTask(model);
                if (isNewTask) {
                    hideUntilControls.setDefaults();
                }
            }
        }

        @Override
        public void run() {
            AndroidUtilities.sleepDeep(500L);

            runOnUiThread(new Runnable() {
                public void run() {
                    onUiThread();
                }
            });

            notesControlSet = new EditTextControlSet(TaskEditActivity.this, Task.NOTES, R.id.notes);
            controls.add(notesControlSet);

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
            StatisticsService.reportEvent("create-task");
            isNewTask = true;

            // set deletion date until task gets a title
            model.setValue(Task.DELETION_DATE, DateUtilities.now());
        } else {
            StatisticsService.reportEvent("edit-task");
        }

        if(model == null) {
            exceptionService.reportError("task-edit-no-task",
                    new NullPointerException("model"));
            finish();
            return;
        }

        // clear notification
        Notifications.cancelNotifications(model.getId());
    }

    /** Populate UI component values from the model */
    private void populateFields() {
        Resources r = getResources();
        loadItem(getIntent());

        if(isNewTask)
            setTitle(R.string.TEA_view_titleNew);
        else
            setTitle(r.getString(R.string.TEA_view_title, model.getValue(Task.TITLE)));

        synchronized(controls) {
            for(TaskEditControlSet controlSet : controls)
                controlSet.readFromTask(model);
        }
    }

    /** Save task model from values in UI components */
    private void save(boolean onPause) {
        StringBuilder toast = new StringBuilder();
        synchronized(controls) {
            for(TaskEditControlSet controlSet : controls) {
                String toastText = controlSet.writeToModel(model);
                if(toastText != null)
                    toast.append('\n').append(toastText);
            }
        }

        if(title.getText().length() > 0)
            model.setValue(Task.DELETION_DATE, 0L);

        taskService.save(model);
        if(title.getText().length() == 0)
            return;

        String processedToast = addDueTimeToToast(toast.toString());
        if(!onPause && peopleControlSet != null && !peopleControlSet.saveSharingSettings(processedToast))
            return;

        if (!onPause) // Saving during on pause could cause a double finish
            finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            save(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();

        // abandon editing and delete the newly created task if
        // no title was entered

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
                Bundle extras = intent.getExtras();
                RemoteViews view = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);

                // add a separator
                View separator = new View(TaskEditActivity.this);
                separator.setPadding(5, 5, 5, 5);
                separator.setBackgroundResource(android.R.drawable.divider_horizontal_dark);

                LinearLayout dest = (LinearLayout)findViewById(R.id.addons_more);
                dest.addView(separator);
                view.apply(TaskEditActivity.this, dest);

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
                DateUtils.getRelativeTimeSpanString(due);
            toastMessage = getString(stringResource, formattedDate);
        } else {
            toastMessage = getString(R.string.TEA_onTaskSave_notDue);
        }

        return toastMessage + additionalMessage;
    }

    protected void discardButtonClick() {
        shouldSaveState = false;

        // abandon editing in this case
        if(title.getText().length() == 0) {
            if(isNewTask)
                taskService.delete(model);
        }

        showCancelToast();
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Show toast for task edit canceling
     */
    private void showCancelToast() {
        Toast.makeText(this, R.string.TEA_onTaskCancel,
                Toast.LENGTH_SHORT).show();
    }

    protected void deleteButtonClick() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.DLG_confirm_title)
            .setMessage(R.string.DLG_delete_this_task_question)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    taskService.delete(model);
                    shouldSaveState = false;
                    showDeleteToast();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    /**
     * Show toast for task edit deleting
     */
    private void showDeleteToast() {
        Toast.makeText(this, R.string.TEA_onTaskDelete,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
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
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;

        item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
        item.setIcon(android.R.drawable.ic_menu_save);

        item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        item = menu.add(Menu.NONE, MENU_DELETE_ID, 0, R.string.TEA_menu_delete);
        item.setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(controlReceiver);

        if(shouldSaveState)
            save(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(controlReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_EDIT_CONTROLS));
        populateFields();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_VOICE_RECOG && resultCode == RESULT_OK) {
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // stick our task into the outState
        outState.putParcelable(TASK_IN_PROGRESS, model);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    /* ======================================================================
     * ========================================== UI component helper classes
     * ====================================================================== */

    // --- interface

    /**
     * Interface for working with controls that alter task data
     */
    public interface TaskEditControlSet {
        /**
         * Read data from model to update the control set
         */
        public void readFromTask(Task task);

        /**
         * Write data from control set to model
         * @return text appended to the toast
         */
        public String writeToModel(Task task);
    }

}
