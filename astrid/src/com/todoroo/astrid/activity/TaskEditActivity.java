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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import ru.otdelit.astrid.opencrx.OpencrxControlSet;
import ru.otdelit.astrid.opencrx.OpencrxCoreUtils;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.producteev.ProducteevControlSet;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.CalendarDialog;
import com.todoroo.astrid.ui.DeadlineTimePickerDialog;
import com.todoroo.astrid.ui.DeadlineTimePickerDialog.OnDeadlineTimeSetListener;
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

    // --- request codes

    @SuppressWarnings("unused")
    private static final int REQUEST_CODE_OPERATION = 0;

    // --- menu codes

    private static final int MENU_SAVE_ID = Menu.FIRST;
    private static final int MENU_DISCARD_ID = Menu.FIRST + 1;
    private static final int MENU_DELETE_ID = Menu.FIRST + 2;

    // --- result codes

    public static final int RESULT_CODE_SAVED = RESULT_FIRST_USER;
    public static final int RESULT_CODE_DISCARDED = RESULT_FIRST_USER + 1;
    public static final int RESULT_CODE_DELETED = RESULT_FIRST_USER + 2;

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

    private EditTextControlSet notesControlSet = null;
    private EditText title;

    private final List<TaskEditControlSet> controls =
        Collections.synchronizedList(new ArrayList<TaskEditControlSet>());

	// --- other instance variables

    /** true if editing started with a new task */
    boolean isNewTask = false;

	/** task model */
	private Task model = null;

	/** whether task should be saved when this activity exits */
	private boolean shouldSaveState = true;

	/** edit control receiver */
	private final ControlReceiver controlReceiver = new ControlReceiver();

    /** voice assistant for notes-creation */
    private VoiceInputAssistant voiceNoteAssistant = null;

    private EditText notesEditText;

    private boolean cancelled = false;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public TaskEditActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
        Resources r = getResources();

        // set up tab host
        TabHost tabHost = getTabHost();
        tabHost.setPadding(0, 4, 0, 0);
        LayoutInflater.from(this).inflate(R.layout.task_edit_activity,
                tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.TEA_tab_basic)).
                setIndicator(r.getString(R.string.TEA_tab_basic),
                        r.getDrawable(R.drawable.tab_edit)).setContent(
                                R.id.tab_basic));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.TEA_tab_extra)).
                setIndicator(r.getString(R.string.TEA_tab_extra),
                        r.getDrawable(R.drawable.tab_advanced)).setContent(
                                R.id.tab_extra));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.TEA_tab_addons)).
                setIndicator(r.getString(R.string.TEA_tab_addons),
                        r.getDrawable(R.drawable.tab_addons)).setContent(
                                R.id.tab_addons));
        getTabWidget().setBackgroundColor(Color.BLACK);

        // populate control set
        title = (EditText) findViewById(R.id.title);
        controls.add(new EditTextControlSet(Task.TITLE, R.id.title));
        controls.add(new ImportanceControlSet(R.id.importance_container));
        controls.add(new UrgencyControlSet(R.id.urgency));
        notesEditText = (EditText) findViewById(R.id.notes);

        // prepare and set listener for voice-button
        if(addOnService.hasPowerPack()) {
            voiceAddNoteButton = (ImageButton) findViewById(R.id.voiceAddNoteButton);
            voiceAddNoteButton.setVisibility(View.VISIBLE);
            int prompt = R.string.voice_edit_note_prompt;
            voiceNoteAssistant = new VoiceInputAssistant(this, voiceAddNoteButton,
                    notesEditText);
            voiceNoteAssistant.setAppend(true);
            voiceNoteAssistant.setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            voiceNoteAssistant.configureMicrophoneButton(prompt);
        }

        new Thread() {
            @Override
            public void run() {
                AndroidUtilities.sleepDeep(500L);

                runOnUiThread(new Runnable() {
                    public void run() {
                        // internal add-ins
                        controls.add(new TagsControlSet(TaskEditActivity.this, R.id.tags_container));

                        LinearLayout extrasAddons = (LinearLayout) findViewById(R.id.tab_extra_addons);
                        controls.add(new RepeatControlSet(TaskEditActivity.this, extrasAddons));
                        controls.add(new GCalControlSet(TaskEditActivity.this, extrasAddons));

                        LinearLayout addonsAddons = (LinearLayout) findViewById(R.id.tab_addons_addons);

                        try {
                            if(ProducteevUtilities.INSTANCE.isLoggedIn()) {
                                controls.add(new ProducteevControlSet(TaskEditActivity.this, addonsAddons));
                                notesEditText.setHint(R.string.producteev_TEA_notes);
                                ((TextView)findViewById(R.id.notes_label)).setHint(R.string.producteev_TEA_notes);
                            }
                        } catch (Exception e) {
                            Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
                        }

                        try {
                            if(OpencrxCoreUtils.INSTANCE.isLoggedIn()) {
                                controls.add(new OpencrxControlSet(TaskEditActivity.this, addonsAddons));
                                notesEditText.setHint(R.string.opencrx_TEA_notes);
                                ((TextView)findViewById(R.id.notes_label)).setHint(R.string.opencrx_TEA_notes);
                            }
                        } catch (Exception e) {
                            Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
                        }

                        controls.add(new TimerControlSet(TaskEditActivity.this, addonsAddons));
                        controls.add(new AlarmControlSet(TaskEditActivity.this, addonsAddons));

                        if(!addOnService.hasPowerPack()) {
                            // show add-on help if necessary
                            View addonsEmpty = findViewById(R.id.addons_empty);
                            addonsEmpty.setVisibility(View.VISIBLE);
                            addonsAddons.removeView(addonsEmpty);
                            addonsAddons.addView(addonsEmpty);
                            ((Button)findViewById(R.id.addons_button)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent addOnActivity = new Intent(TaskEditActivity.this, AddOnActivity.class);
                                    addOnActivity.putExtra(AddOnActivity.TOKEN_START_WITH_AVAILABLE, true);
                                    startActivity(addOnActivity);
                                }
                            });
                        }

                        controls.add(new ReminderControlSet(R.id.reminder_due,
                                R.id.reminder_overdue, R.id.reminder_alarm));
                        controls.add(new RandomReminderControlSet(R.id.reminder_random,
                                R.id.reminder_random_interval));
                        controls.add(new HideUntilControlSet(R.id.hideUntil));

                        // re-read all
                        synchronized(controls) {
                            for(TaskEditControlSet controlSet : controls)
                                controlSet.readFromTask(model);
                        }
                    }
                });

                notesControlSet = new EditTextControlSet(Task.NOTES, R.id.notes);
                controls.add(notesControlSet);

                // set up listeners
                setUpListeners();
            }
        }.start();

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
            ImageButton saveButtonGeneral = (ImageButton) findViewById(R.id.save_basic);
            saveButtonGeneral.setOnClickListener(mSaveListener);
            ImageButton saveButtonDates = (ImageButton) findViewById(R.id.save_extra);
            saveButtonDates.setOnClickListener(mSaveListener);
            ImageButton saveButtonNotify = (ImageButton) findViewById(R.id.save_addons);
            saveButtonNotify.setOnClickListener(mSaveListener);

            ImageButton discardButtonGeneral = (ImageButton) findViewById(R.id.discard_basic);
            discardButtonGeneral.setOnClickListener(mDiscardListener);
            ImageButton discardButtonDates = (ImageButton) findViewById(R.id.discard_extra);
            discardButtonDates.setOnClickListener(mDiscardListener);
            ImageButton discardButtonNotify = (ImageButton) findViewById(R.id.discard_addons);
            discardButtonNotify.setOnClickListener(mDiscardListener);
        } catch (Exception e) {
            // error loading the proper activity
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
    private void save() {
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

        if(taskService.save(model) && title.getText().length() > 0)
            showSaveToast(toast.toString());
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

                LinearLayout dest = (LinearLayout)findViewById(R.id.tab_addons_addons);
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
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Displays a Toast reporting that the selected task has been saved and, if
     * it has a due date, that is due in 'x' amount of time, to 1 time-unit of
     * precision
     * @param additionalMessage
     */
    private void showSaveToast(String additionalMessage) {
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

        int length = additionalMessage.length() == 0 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(this,
                toastMessage + additionalMessage,
                length).show();
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
            save();
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
        // handle the result of voice recognition, put it into the appropiate textfield
        voiceNoteAssistant.handleActivityResult(requestCode, resultCode, data);

        // write the voicenote into the model, or it will be deleted by onResume.populateFields
        // (due to the activity-change)
        notesControlSet.writeToModel(model);

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

    // --- EditTextControlSet

    /**
     * Control set for mapping a Property to an EditText
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class EditTextControlSet implements TaskEditControlSet {
        private final EditText editText;
        private final StringProperty property;

        public EditTextControlSet(StringProperty property, int editText) {
            this.property = property;
            this.editText = (EditText)findViewById(editText);
        }

        @Override
        public void readFromTask(Task task) {
            editText.setText(task.getValue(property));
        }

        @Override
        public String writeToModel(Task task) {
            task.setValue(property, editText.getText().toString());
            return null;
        }
    }

    // --- ImportanceControlSet

    /**
     * Control Set for setting task importance
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class ImportanceControlSet implements TaskEditControlSet {
        private final List<CompoundButton> buttons = new LinkedList<CompoundButton>();
        private final int[] colors = Task.getImportanceColors(getResources());

        public ImportanceControlSet(int containerId) {
            LinearLayout layout = (LinearLayout)findViewById(containerId);

            int min = Task.IMPORTANCE_MOST;
            int max = Task.IMPORTANCE_LEAST;
            if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
                max = 5;

            for(int i = min; i <= max; i++) {
                final ToggleButton button = new ToggleButton(TaskEditActivity.this);
                button.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

                StringBuilder label = new StringBuilder();
                if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
                    label.append(5 - i).append("\n\u2605"); //$NON-NLS-1$
                else {
                    for(int j = Task.IMPORTANCE_LEAST; j >= i; j--)
                        label.append('!');
                }

                button.setTextColor(colors[i]);
                button.setTextOff(label);
                button.setTextOn(label);

                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        setImportance((Integer)button.getTag());
                    }
                });
                button.setTag(i);

                buttons.add(button);
                layout.addView(button);
            }
        }

        public void setImportance(Integer i) {
            for(CompoundButton b : buttons) {
                if(b.getTag() == i) {
                    b.setTextSize(24);
                    b.setChecked(true);
                    b.setBackgroundResource(R.drawable.btn_selected);
                } else {
                    b.setTextSize(16);
                    b.setChecked(false);
                    b.setBackgroundResource(android.R.drawable.btn_default);
                }
            }
        }

        public Integer getImportance() {
            for(CompoundButton b : buttons)
                if(b.isChecked())
                    return (Integer) b.getTag();
            return null;
        }

        @Override
        public void readFromTask(Task task) {
            setImportance(task.getValue(Task.IMPORTANCE));
        }

        @Override
        public String writeToModel(Task task) {
            if(getImportance() != null)
                task.setValue(Task.IMPORTANCE, getImportance());
            return null;
        }
    }

    // --- UrgencyControlSet

    private class UrgencyControlSet implements TaskEditControlSet,
            OnItemSelectedListener, OnDeadlineTimeSetListener,
            OnCancelListener {

        private static final int SPECIFIC_DATE = -1;
        private static final int EXISTING_TIME_UNSET = -2;

        private final Spinner spinner;
        private ArrayAdapter<UrgencyValue> urgencyAdapter;
        private int previousSetting = Task.URGENCY_NONE;

        private long existingDate = EXISTING_TIME_UNSET;
        private int existingDateHour = EXISTING_TIME_UNSET;
        private int existingDateMinutes = EXISTING_TIME_UNSET;

        /**
         * Container class for urgencies
         *
         * @author Tim Su <tim@todoroo.com>
         *
         */
        private class UrgencyValue {
            public String label;
            public int setting;
            public long dueDate;

            public UrgencyValue(String label, int setting) {
                this.label = label;
                this.setting = setting;
                dueDate = model.createDueDate(setting, 0);
            }

            public UrgencyValue(String label, int setting, long dueDate) {
                this.label = label;
                this.setting = setting;
                this.dueDate = dueDate;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        public UrgencyControlSet(int urgency) {
            this.spinner = (Spinner)findViewById(urgency);
            this.spinner.setOnItemSelectedListener(this);
        }

        /**
         * set up urgency adapter and picks the right selected item
         * @param dueDate
         */
        private void createUrgencyList(long dueDate) {
            // set up base urgency list
            String[] labels = getResources().getStringArray(R.array.TEA_urgency);
            UrgencyValue[] urgencyValues = new UrgencyValue[labels.length];
            urgencyValues[0] = new UrgencyValue(labels[0],
                    Task.URGENCY_SPECIFIC_DAY_TIME, SPECIFIC_DATE);
            urgencyValues[1] = new UrgencyValue(labels[1],
                    Task.URGENCY_TODAY);
            urgencyValues[2] = new UrgencyValue(labels[2],
                    Task.URGENCY_TOMORROW);
            String dayAfterTomorrow = DateUtils.getDayOfWeekString(
                    new Date(DateUtilities.now() + 2 * DateUtilities.ONE_DAY).getDay() +
                    Calendar.SUNDAY, DateUtils.LENGTH_LONG);
            urgencyValues[3] = new UrgencyValue(dayAfterTomorrow,
                    Task.URGENCY_DAY_AFTER);
            urgencyValues[4] = new UrgencyValue(labels[4],
                    Task.URGENCY_NEXT_WEEK);
            urgencyValues[5] = new UrgencyValue(labels[5],
                    Task.URGENCY_NONE);

            // search for setting
            int selection = -1;
            for(int i = 0; i < urgencyValues.length; i++)
                if(urgencyValues[i].dueDate == dueDate) {
                    selection = i;
                    if(dueDate > 0)
                        existingDate = dueDate;
                    break;
                }

            if(selection == -1) {
                UrgencyValue[] updated = new UrgencyValue[labels.length + 1];
                for(int i = 0; i < labels.length; i++)
                    updated[i+1] = urgencyValues[i];
                if(Task.hasDueTime(dueDate)) {
                    Date dueDateAsDate = new Date(dueDate);
                    updated[0] = new UrgencyValue(DateUtilities.getDateStringWithTime(TaskEditActivity.this, dueDateAsDate),
                            Task.URGENCY_SPECIFIC_DAY_TIME, dueDate);
                    existingDate = dueDate;
                    existingDateHour = dueDateAsDate.getHours();
                    existingDateMinutes = dueDateAsDate.getMinutes();
                } else {
                    updated[0] = new UrgencyValue(DateUtilities.getDateString(TaskEditActivity.this, new Date(dueDate)),
                            Task.URGENCY_SPECIFIC_DAY, dueDate);
                    existingDate = dueDate;
                    existingDateHour = SPECIFIC_DATE;
                }
                selection = 0;
                urgencyValues = updated;
            }

            urgencyAdapter = new ArrayAdapter<UrgencyValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    urgencyValues);
            urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.spinner.setAdapter(urgencyAdapter);
            this.spinner.setSelection(selection);
        }

        // --- listening for events

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // if specific date or date & time selected, show dialog
            // ... at conclusion of dialog, update our list
            UrgencyValue item = urgencyAdapter.getItem(position);
            if(item.dueDate == SPECIFIC_DATE) {
                customSetting = item.setting;
                customDate = new Date(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate);
                customDate.setSeconds(0);
                /***** Calendar Dialog Changes -- Start *****/
                final CalendarDialog calendarDialog = new CalendarDialog(TaskEditActivity.this, customDate);
                calendarDialog.show();
                calendarDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        if (!cancelled) {
                            setDate(calendarDialog);
                        }
                        cancelled = false;
                    }
                });

                calendarDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        cancelled = true;
                    }
                });
                /***** Calendar Dialog Changes -- End *****/

                spinner.setSelection(previousSetting);
            } else {
                previousSetting = position;
                model.setValue(Task.DUE_DATE, item.dueDate);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // ignore
        }

        Date customDate;
        int customSetting;

        private void setDate(CalendarDialog calendarDialog) {
            customDate = calendarDialog.getCalendarDate();
            customDate.setMinutes(0);

            if(customSetting != Task.URGENCY_SPECIFIC_DAY_TIME) {
                customDateFinished();
                return;
            }

            boolean specificTime = existingDateHour != SPECIFIC_DATE;
            if(existingDateHour < 0) {
                existingDateHour = customDate.getHours();
                existingDateMinutes= customDate.getMinutes();
            }

            DeadlineTimePickerDialog timePicker = new DeadlineTimePickerDialog(TaskEditActivity.this, this,
                    existingDateHour, existingDateMinutes,
                    DateUtilities.is24HourFormat(TaskEditActivity.this),
                    specificTime);

            timePicker.setOnCancelListener(this);
            timePicker.show();
        }

        public void onTimeSet(TimePicker view, boolean hasTime, int hourOfDay, int minute) {
            if(!hasTime)
                customSetting = Task.URGENCY_SPECIFIC_DAY;
            else {
                customDate.setHours(hourOfDay);
                customDate.setMinutes(minute);
                existingDateHour = hourOfDay;
                existingDateMinutes = minute;
            }
            customDateFinished();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // user canceled, restore previous choice
            spinner.setSelection(previousSetting);
        }

        private void customDateFinished() {
            long time = model.createDueDate(customSetting, customDate.getTime());
            model.setValue(Task.DUE_DATE, time);
            createUrgencyList(time);
        }

        // --- setting up values

        @Override
        public void readFromTask(Task task) {
            long dueDate = task.getValue(Task.DUE_DATE);
            createUrgencyList(dueDate);
        }

        @Override
        public String writeToModel(Task task) {
            UrgencyValue item = urgencyAdapter.getItem(spinner.getSelectedItemPosition());
            if(item.dueDate != SPECIFIC_DATE) // user canceled specific date
                task.setValue(Task.DUE_DATE, item.dueDate);
            return null;
        }

    }

    /**
     * Control set for specifying when a task should be hidden
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class HideUntilControlSet implements TaskEditControlSet,
            OnItemSelectedListener, OnCancelListener,
            OnDeadlineTimeSetListener {

        private static final int SPECIFIC_DATE = -1;
        private static final int EXISTING_TIME_UNSET = -2;

        private final Spinner spinner;
        private int previousSetting = Task.HIDE_UNTIL_NONE;

        private long existingDate = EXISTING_TIME_UNSET;
        private int existingDateHour = EXISTING_TIME_UNSET;
        private int existingDateMinutes = EXISTING_TIME_UNSET;

        public HideUntilControlSet(int hideUntil) {
            this.spinner = (Spinner) findViewById(hideUntil);
            this.spinner.setOnItemSelectedListener(this);
        }

        private ArrayAdapter<HideUntilValue> adapter;

        /**
         * Container class for urgencies
         *
         * @author Tim Su <tim@todoroo.com>
         *
         */
        private class HideUntilValue {
            public String label;
            public int setting;
            public long date;

            public HideUntilValue(String label, int setting) {
                this(label, setting, 0);
            }

            public HideUntilValue(String label, int setting, long date) {
                this.label = label;
                this.setting = setting;
                this.date = date;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private HideUntilValue[] createHideUntilList(long specificDate) {
            // set up base values
            String[] labels = getResources().getStringArray(R.array.TEA_hideUntil);
            HideUntilValue[] values = new HideUntilValue[labels.length];
            values[0] = new HideUntilValue(labels[0], Task.HIDE_UNTIL_NONE);
            values[1] = new HideUntilValue(labels[1], Task.HIDE_UNTIL_DUE);
            values[2] = new HideUntilValue(labels[2], Task.HIDE_UNTIL_DAY_BEFORE);
            values[3] = new HideUntilValue(labels[3], Task.HIDE_UNTIL_WEEK_BEFORE);
            values[4] = new HideUntilValue(labels[4], Task.HIDE_UNTIL_SPECIFIC_DAY, -1);

            if(specificDate > 0) {
                HideUntilValue[] updated = new HideUntilValue[values.length + 1];
                for(int i = 0; i < values.length; i++)
                    updated[i+1] = values[i];
                Date hideUntilAsDate = new Date(specificDate);
                if(hideUntilAsDate.getHours() == 0 && hideUntilAsDate.getMinutes() == 0 && hideUntilAsDate.getSeconds() == 0) {
                    updated[0] = new HideUntilValue(DateUtilities.getDateString(TaskEditActivity.this, new Date(specificDate)),
                            Task.HIDE_UNTIL_SPECIFIC_DAY, specificDate);
                    existingDate = specificDate;
                    existingDateHour = SPECIFIC_DATE;
                } else {
                    updated[0] = new HideUntilValue(DateUtilities.getDateStringWithTime(TaskEditActivity.this, new Date(specificDate)),
                            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, specificDate);
                    existingDate = specificDate;
                    existingDateHour = hideUntilAsDate.getHours();
                    existingDateMinutes = hideUntilAsDate.getMinutes();
                }
                values = updated;
            }

            return values;
        }

        // --- listening for events

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // if specific date selected, show dialog
            // ... at conclusion of dialog, update our list
            HideUntilValue item = adapter.getItem(position);
            if(item.date == SPECIFIC_DATE) {
                customDate = new Date(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate);
                customDate.setSeconds(0);

                /***** Calendar Dialog Changes -- Start *****/
                final CalendarDialog calendarDialog = new CalendarDialog(TaskEditActivity.this, customDate);
                calendarDialog.show();
                calendarDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        if (!cancelled) {
                            setDate(calendarDialog);
                        }
                        cancelled = false;
                    }
                });

                calendarDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        cancelled = true;
                    }
                });
                /***** Calendar Dialog Changes -- End *****/
                /*DatePickerDialog datePicker = new DatePickerDialog(TaskEditActivity.this,
                        this, 1900 + customDate.getYear(), customDate.getMonth(), customDate.getDate());
                datePicker.setOnCancelListener(this);
                datePicker.show();*/

                spinner.setSelection(previousSetting);
            } else {
                previousSetting = position;
                model.setValue(Task.HIDE_UNTIL, item.date);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // ignore
        }

        Date customDate;

        private void setDate(CalendarDialog calendarDialog) {
            customDate = calendarDialog.getCalendarDate();

            boolean specificTime = existingDateHour != SPECIFIC_DATE;
            if(existingDateHour < 0) {
                existingDateHour = customDate.getHours();
                existingDateMinutes= customDate.getMinutes();
            }

            DeadlineTimePickerDialog timePicker = new DeadlineTimePickerDialog(TaskEditActivity.this, this,
                    existingDateHour, existingDateMinutes,
                    DateUtilities.is24HourFormat(TaskEditActivity.this),
                    specificTime);

            timePicker.setOnCancelListener(this);
            timePicker.show();
        }

        /*public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            customDate.setYear(year - 1900);
            customDate.setMonth(month);
            customDate.setDate(monthDay);

            boolean specificTime = existingDateHour != SPECIFIC_DATE;
            if(existingDateHour < 0) {
                existingDateHour = customDate.getHours();
                existingDateMinutes= customDate.getMinutes();
            }

            DeadlineTimePickerDialog timePicker = new DeadlineTimePickerDialog(TaskEditActivity.this, this,
                    existingDateHour, existingDateMinutes,
                    DateUtilities.is24HourFormat(TaskEditActivity.this),
                    specificTime);

            timePicker.setOnCancelListener(this);
            timePicker.show();
        }*/

        public void onTimeSet(TimePicker view, boolean hasTime, int hourOfDay, int minute) {
            if(!hasTime) {
                customDate.setHours(0);
                customDate.setMinutes(0);
                customDate.setSeconds(0);
            } else {
                customDate.setHours(hourOfDay);
                customDate.setMinutes(minute);
                existingDateHour = hourOfDay;
                existingDateMinutes = minute;
            }
            customDateFinished();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // user canceled, restore previous choice
            spinner.setSelection(previousSetting);
        }

        private void customDateFinished() {
            HideUntilValue[] list = createHideUntilList(customDate.getTime());
            adapter = new ArrayAdapter<HideUntilValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(0);
        }

        // --- setting up values

        @Override
        public void readFromTask(Task task) {
            long date = task.getValue(Task.HIDE_UNTIL);

            Date dueDay = new Date(task.getValue(Task.DUE_DATE)/1000L*1000L);
            dueDay.setHours(0);
            dueDay.setMinutes(0);
            dueDay.setSeconds(0);

            int selection = 0;
            if(date == 0) {
                selection = 0;
                date = 0;
            } else if(date == dueDay.getTime()) {
                selection = 1;
                date = 0;
            } else if(date + DateUtilities.ONE_DAY == dueDay.getTime()) {
                selection = 2;
                date = 0;
            } else if(date + DateUtilities.ONE_WEEK == dueDay.getTime()) {
                selection = 3;
                date = 0;
            }

            HideUntilValue[] list = createHideUntilList(date);
            adapter = new ArrayAdapter<HideUntilValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setSelection(selection);
        }

        @Override
        public String writeToModel(Task task) {
            if(adapter == null || spinner == null)
                return null;
            HideUntilValue item = adapter.getItem(spinner.getSelectedItemPosition());
            if(item == null)
                return null;
            long value = task.createHideUntil(item.setting, item.date);
            task.setValue(Task.HIDE_UNTIL, value);
            return null;
        }

    }

    /**
     * Control set dealing with reminder settings
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class ReminderControlSet implements TaskEditControlSet {
        private final CheckBox during, after;
        private final Spinner mode;

        public ReminderControlSet(int duringId, int afterId, int modeId) {
            during = (CheckBox)findViewById(duringId);
            after = (CheckBox)findViewById(afterId);
            mode = (Spinner)findViewById(modeId);

            String[] list = new String[] {
                    getString(R.string.TEA_reminder_mode_once),
                    getString(R.string.TEA_reminder_mode_five),
                    getString(R.string.TEA_reminder_mode_nonstop),
            };
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mode.setAdapter(adapter);
                }
            });
        }

        public void setValue(int flags) {
            during.setChecked((flags & Task.NOTIFY_AT_DEADLINE) > 0);
            after.setChecked((flags &
                    Task.NOTIFY_AFTER_DEADLINE) > 0);

            if((flags & Task.NOTIFY_MODE_NONSTOP) > 0)
                mode.setSelection(2);
            else if((flags & Task.NOTIFY_MODE_FIVE) > 0)
                mode.setSelection(1);
            else
                mode.setSelection(0);
        }

        public int getValue() {
            int value = 0;
            if(during.isChecked())
                value |= Task.NOTIFY_AT_DEADLINE;
            if(after.isChecked())
                value |= Task.NOTIFY_AFTER_DEADLINE;

            value &= ~(Task.NOTIFY_MODE_FIVE | Task.NOTIFY_MODE_NONSTOP);
            if(mode.getSelectedItemPosition() == 2)
                value |= Task.NOTIFY_MODE_NONSTOP;
            else if(mode.getSelectedItemPosition() == 1)
                value |= Task.NOTIFY_MODE_FIVE;

            return value;
        }

        @Override
        public void readFromTask(Task task) {
            setValue(task.getValue(Task.REMINDER_FLAGS));
        }

        @Override
        public String writeToModel(Task task) {
            task.setValue(Task.REMINDER_FLAGS, getValue());
            // clear snooze if task is being edited
            task.setValue(Task.REMINDER_SNOOZE, 0L);
            return null;
        }
    }

    /**
     * Control set dealing with random reminder settings
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class RandomReminderControlSet implements TaskEditControlSet {
        /** default interval for spinner if date is unselected */
        private final long DEFAULT_INTERVAL = DateUtilities.ONE_WEEK * 2;

        private final CheckBox settingCheckbox;
        private final Spinner periodSpinner;

        private boolean periodSpinnerInitialized = false;
        private final int[] hours;

        public RandomReminderControlSet(int settingCheckboxId, int periodButtonId) {
            settingCheckbox = (CheckBox)findViewById(settingCheckboxId);
            periodSpinner = (Spinner)findViewById(periodButtonId);
            periodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    if(periodSpinnerInitialized)
                        settingCheckbox.setChecked(true);
                    periodSpinnerInitialized = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // ignore
                }
            });

            // create adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    getResources().getStringArray(R.array.TEA_reminder_random));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            periodSpinner.setAdapter(adapter);

            // create hour array
            String[] hourStrings = getResources().getStringArray(R.array.TEA_reminder_random_hours);
            hours = new int[hourStrings.length];
            for(int i = 0; i < hours.length; i++)
                hours[i] = Integer.parseInt(hourStrings[i]);
        }

        @Override
        public void readFromTask(Task task) {
            long time = task.getValue(Task.REMINDER_PERIOD);

            boolean enabled = time > 0;
            if(time <= 0) {
                time = DEFAULT_INTERVAL;
            }

            int i;
            for(i = 0; i < hours.length - 1; i++)
                if(hours[i] * DateUtilities.ONE_HOUR >= time)
                    break;
            periodSpinner.setSelection(i);
            settingCheckbox.setChecked(enabled);
        }

        @Override
        public String writeToModel(Task task) {
            if(settingCheckbox.isChecked()) {
                int hourValue = hours[periodSpinner.getSelectedItemPosition()];
                task.setValue(Task.REMINDER_PERIOD, hourValue * DateUtilities.ONE_HOUR);
            } else
                task.setValue(Task.REMINDER_PERIOD, 0L);
            return null;
        }
    }

}
