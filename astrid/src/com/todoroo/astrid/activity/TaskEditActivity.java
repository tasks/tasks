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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TabActivity;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.enums.RepeatInterval;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.TimeDurationControlSet;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;
import com.timsu.astrid.widget.TimeDurationControlSet.TimeDurationType;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.utility.Constants;

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
    public static final String ID_TOKEN = "i"; //$NON-NLS-1$

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

	// --- other constants

    /** Number of tags a task can have */
	private static final int MAX_TAGS = 5;

    // --- services

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private Database database;

    @Autowired
    private TaskService taskService;

    @Autowired
    private DateUtilities dateUtilities;

	// --- UI components

    private EditText title;

    private final ArrayList<TaskEditControlSet> controls = new ArrayList<TaskEditControlSet>();

	// --- other instance variables

	/** task model */
	private Task model = null;

	/** whether task should be saved when this activity exits */
	private boolean shouldSaveState = true;

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
		AstridUtilities.suppressVirtualKeyboard(title);
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
                        r.getDrawable(R.drawable.tea_tab_basic)).setContent(
                                R.id.tab_basic));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.TEA_tab_extra)).
                setIndicator(r.getString(R.string.TEA_tab_extra),
                        r.getDrawable(R.drawable.tea_tab_extra)).setContent(
                                R.id.tab_extra));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.TEA_tab_addons)).
                setIndicator(r.getString(R.string.TEA_tab_addons),
                        r.getDrawable(R.drawable.tea_tab_extensions)).setContent(
                                R.id.tab_addons));

        // populate control set
        title = (EditText) findViewById(R.id.title);

        controls.add(new EditTextControlSet(Task.TITLE, R.id.title));
        controls.add(new ImportanceControlSet(R.id.importance_container));
        controls.add(new UrgencyControlSet(R.id.urgency));
        controls.add(new HideUntilControlSet(R.id.hideUntil));
        controls.add(new EditTextControlSet(Task.NOTES, R.id.notes));

        controls.add( new ReminderControlSet(R.id.reminder_due,
                R.id.reminder_overdue, R.id.reminder_alarm));
        controls.add( new RandomReminderControlSet(R.id.reminder_random,
                R.id.reminder_random_interval));
        controls.add(new TagsControlSet(R.id.tags_container));
        controls.add(new RepeatControlSet(R.id.repeat_value, R.id.repeat_interval));

        controls.add(new CalendarControlSet(R.id.add_to_calendar, R.id.view_calendar_event));
        controls.add(new TimeDurationTaskEditControlSet(Task.ESTIMATED_SECONDS,
                R.id.estimatedDuration, 0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES));
        controls.add(new TimeDurationTaskEditControlSet(Task.ELAPSED_SECONDS, R.id.elapsedDuration,
                0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES));

        // read data
        populateFields();

        // set up listeners
        setUpListeners();
    }

    /**
     * @return true if task is newly created
     */
    private boolean isNewTask() {
        return model == null ? true : model.getValue(Task.TITLE).length() == 0;
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
        /*final View.OnClickListener mDeleteListener = new View.OnClickListener() {
            public void onClick(View v) {
                deleteButtonClick();
            }
        };*/

        // set up save, cancel, and delete buttons
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

        ImageButton deleteButtonGeneral = (ImageButton) findViewById(R.id.delete_basic);
        ImageButton deleteButtonDates = (ImageButton) findViewById(R.id.delete_extra);
        ImageButton deleteButtonNotify = (ImageButton) findViewById(R.id.delete_addons);

        // hide the delete button always for now
        deleteButtonGeneral.setVisibility(View.GONE);
        deleteButtonDates.setVisibility(View.GONE);
        deleteButtonNotify.setVisibility(View.GONE);
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
        long idParam = intent.getLongExtra(ID_TOKEN, -1L);

        database.openForReading();
        if(idParam == -1L) {
            model = new Task();
            taskService.save(model, false);
        } else {
            model = taskService.fetchById(idParam, Task.PROPERTIES);
        }

        if(model.getValue(Task.TITLE).length() == 0)
            FlurryAgent.onEvent("create-task");
        FlurryAgent.onEvent("edit-task");

        if(model == null) {
            exceptionService.reportError("task-edit-no-task",
                    new NullPointerException("model"));
            finish();
            return;
        }
    }

    /** Populate UI component values from the model */
    private void populateFields() {
        Resources r = getResources();
        loadItem(getIntent());

        if(isNewTask())
            setTitle(R.string.TEA_view_titleNew);
        else
            setTitle(r.getString(R.string.TEA_view_title, model.getValue(Task.TITLE)));

        for(TaskEditControlSet controlSet : controls)
            controlSet.readFromModel();
    }

    /** Save task model from values in UI components */
    private void save() {
        // abandon editing in this case
        if(title.getText().length() == 0) {
            if(isNewTask())
                taskService.delete(model);
            discardButtonClick();
            return;
        }

        for(TaskEditControlSet controlSet : controls)
            controlSet.writeToModel();

        taskService.save(model, false);
        showSaveToast();
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
     */
    private void showSaveToast() {
        // if we have no title, don't show a message
        if(isNewTask())
            return;

        int stringResource;

        long due = model.getValue(Task.DUE_DATE);
        if (due != 0) {
            long dueFromNow = due - System.currentTimeMillis();

            if (dueFromNow < 0) {
                stringResource = R.string.TEA_onTaskSave_overdue;
            } else {
                stringResource = R.string.TEA_onTaskSave_due;
            }
            String formattedDate = dateUtilities.getDurationString(dueFromNow, 1);
            Toast.makeText(this,
                    getResources().getString(stringResource, formattedDate),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.TEA_onTaskSave_notDue,
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void discardButtonClick() {
        shouldSaveState = false;
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

        item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_save_label);
        item.setIcon(android.R.drawable.ic_menu_save);
        item.setAlphabeticShortcut('s');

        item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_cancel_label);
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        item.setAlphabeticShortcut('c');

        item = menu.add(Menu.NONE, MENU_DELETE_ID, 0, R.string.TEA_delete_label);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setAlphabeticShortcut('d');

        return true;
    }

    @Override
    protected void onPause() {
        if(shouldSaveState)
            save();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);

        // don't save if user accidentally created a new task
        if(title.getText().length() == 0) {
            if(model.isSaved())
                taskService.delete(model);
            showCancelToast();
            return;
        }
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
        public void readFromModel();

        /**
         * Write data from control set to model
         */
        public void writeToModel();
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
        public void readFromModel() {
            editText.setText(model.getValue(property));
        }

        @Override
        public void writeToModel() {
            model.setValue(property, editText.getText().toString());
        }
    }

    // --- TimeDurationTaskEditControlSet

    /**
     * Control set for mapping a Property to a TimeDurationControlSet
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TimeDurationTaskEditControlSet implements TaskEditControlSet {
        private final TimeDurationControlSet controlSet;
        private final IntegerProperty property;

        public TimeDurationTaskEditControlSet(IntegerProperty property, int timeButtonId,
                int prefixResource, int titleResource, TimeDurationType type) {
            this.property = property;
            this.controlSet = new TimeDurationControlSet(TaskEditActivity.this,
                    timeButtonId, prefixResource, titleResource, type);
        }

        @Override
        public void readFromModel() {
            controlSet.setTimeDuration(model.getValue(property));
        }

        @Override
        public void writeToModel() {
            model.setValue(property, controlSet.getTimeDurationInSeconds());
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

            for(int i = Task.IMPORTANCE_MOST; i <= Task.IMPORTANCE_LEAST; i++) {
                final ToggleButton button = new ToggleButton(TaskEditActivity.this);
                button.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

                StringBuilder label = new StringBuilder();
                for(int j = Task.IMPORTANCE_LEAST; j >= i; j--)
                    label.append('!');

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

        public int getImportance() {
            for(CompoundButton b : buttons)
                if(b.isChecked())
                    return (Integer) b.getTag();
            return Task.IMPORTANCE_LEAST;
        }

        @Override
        public void readFromModel() {
            setImportance(model.getValue(Task.IMPORTANCE));
        }

        @Override
        public void writeToModel() {
            model.setValue(Task.IMPORTANCE, getImportance());
        }
    }

    // --- UrgencyControlSet

    private class UrgencyControlSet implements TaskEditControlSet,
            OnItemSelectedListener, OnTimeSetListener, OnDateSetListener {

        private static final long SPECIFIC_DATE = -1;

        private final Spinner urgency;
        private ArrayAdapter<UrgencyValue> urgencyAdapter;

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
            this.urgency = (Spinner)findViewById(urgency);
            this.urgency.setOnItemSelectedListener(this);
        }

        /**
         * set up urgency adapter and picks the right selected item
         * @param dueDate
         */
        private void createUrgencyList(long dueDate) {
            // set up base urgency list
            String[] labels = getResources().getStringArray(R.array.TEA_urgency);
            UrgencyValue[] urgencyValues = new UrgencyValue[labels.length];
            urgencyValues[0] = new UrgencyValue(labels[Task.URGENCY_NONE],
                    Task.URGENCY_NONE);
            urgencyValues[1] = new UrgencyValue(labels[Task.URGENCY_TODAY],
                    Task.URGENCY_TODAY);
            urgencyValues[2] = new UrgencyValue(labels[Task.URGENCY_TOMORROW],
                    Task.URGENCY_TOMORROW);
            String dayAfterTomorrow = new SimpleDateFormat("EEEE").format( //$NON-NLS-1$
                    new Date(DateUtilities.now() + 2 * DateUtilities.ONE_DAY));
            urgencyValues[3] = new UrgencyValue(dayAfterTomorrow,
                    Task.URGENCY_DAY_AFTER);
            urgencyValues[4] = new UrgencyValue(labels[Task.URGENCY_NEXT_WEEK],
                    Task.URGENCY_NEXT_WEEK);
            urgencyValues[5] = new UrgencyValue(labels[Task.URGENCY_NEXT_MONTH],
                    Task.URGENCY_NEXT_MONTH);
            urgencyValues[6] = new UrgencyValue(labels[Task.URGENCY_SPECIFIC_DAY],
                    Task.URGENCY_SPECIFIC_DAY, SPECIFIC_DATE);
            urgencyValues[7] = new UrgencyValue(labels[Task.URGENCY_SPECIFIC_DAY_TIME],
                    Task.URGENCY_SPECIFIC_DAY_TIME, SPECIFIC_DATE);

            // search for setting
            int selection = -1;
            for(int i = 0; i < urgencyValues.length; i++)
                if(urgencyValues[i].dueDate == dueDate) {
                    selection = i;
                    break;
                }

            if(selection == -1) {
                UrgencyValue[] updated = new UrgencyValue[labels.length + 1];
                for(int i = 0; i < labels.length; i++)
                    updated[i+1] = urgencyValues[i];
                if(Task.hasDueTime(dueDate)) {
                    SimpleDateFormat format = DateUtilities.getDateWithTimeFormat(TaskEditActivity.this);
                    updated[0] = new UrgencyValue(format.format(new Date(dueDate)),
                            Task.URGENCY_SPECIFIC_DAY_TIME, dueDate);
                } else {
                    SimpleDateFormat format = DateUtilities.getDateFormat(TaskEditActivity.this);
                    updated[0] = new UrgencyValue(format.format(new Date(dueDate)),
                            Task.URGENCY_SPECIFIC_DAY, dueDate);
                }
                selection = 0;
                urgencyValues = updated;
            }

            urgencyAdapter = new ArrayAdapter<UrgencyValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    urgencyValues);
            urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.urgency.setAdapter(urgencyAdapter);
            this.urgency.setSelection(selection);
        }

        // --- listening for events

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // if specific date or date & time selected, show dialog
            // ... at conclusion of dialog, update our list
            UrgencyValue item = urgencyAdapter.getItem(position);
            if(item.dueDate == SPECIFIC_DATE) {
                customSetting = item.setting;
                customDate = new Date();
                customDate.setSeconds(0);
                DatePickerDialog datePicker = new DatePickerDialog(TaskEditActivity.this,
                        this, 1900 + customDate.getYear(), customDate.getMonth(), customDate.getDate());
                datePicker.show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // ignore
        }

        Date customDate;
        int customSetting;

        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            customDate.setYear(year - 1900);
            customDate.setMonth(month);
            customDate.setDate(monthDay);
            customDate.setMinutes(0);

            if(customSetting != Task.URGENCY_SPECIFIC_DAY_TIME) {
                customDateFinished();
                return;
            }

            new TimePickerDialog(TaskEditActivity.this, this,
                    customDate.getHours(), customDate.getMinutes(),
                    DateUtilities.is24HourFormat(TaskEditActivity.this)).show();
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            customDate.setHours(hourOfDay);
            customDate.setMinutes(minute);
            customDateFinished();
        }

        private void customDateFinished() {
            long time = model.createDueDate(customSetting, customDate.getTime());
            createUrgencyList(time);
        }

        // --- setting up values

        @Override
        public void readFromModel() {
            long dueDate = model.getValue(Task.DUE_DATE);
            createUrgencyList(dueDate);
        }

        @Override
        public void writeToModel() {
            UrgencyValue item = urgencyAdapter.getItem(urgency.getSelectedItemPosition());
            model.setValue(Task.DUE_DATE, item.dueDate);
        }
    }

    /**
     * Control set for specifying when a task should be hidden
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class HideUntilControlSet implements TaskEditControlSet,
            OnItemSelectedListener, OnDateSetListener {

        private static final int SPECIFIC_DATE = -1;
        private final Spinner hideUntil;

        public HideUntilControlSet(int hideUntil) {
            this.hideUntil = (Spinner) findViewById(hideUntil);
            this.hideUntil.setOnItemSelectedListener(this);
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
                HideUntilValue[] updated = new HideUntilValue[labels.length + 1];
                for(int i = 0; i < labels.length; i++)
                    updated[i+1] = values[i];
                SimpleDateFormat format = DateUtilities.getDateFormat(TaskEditActivity.this);
                updated[0] = new HideUntilValue(format.format(new Date(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY, specificDate);
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
                intermediateDate = new Date();
                intermediateDate.setSeconds(0);
                DatePickerDialog datePicker = new DatePickerDialog(TaskEditActivity.this,
                        this, 1900 + intermediateDate.getYear(), intermediateDate.getMonth(), intermediateDate.getDate());
                datePicker.show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // ignore
        }

        Date intermediateDate;

        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            intermediateDate.setYear(year - 1900);
            intermediateDate.setMonth(month);
            intermediateDate.setDate(monthDay);
            intermediateDate.setHours(0);
            intermediateDate.setMinutes(0);
            customDateFinished();
        }

        private void customDateFinished() {
            HideUntilValue[] list = createHideUntilList(intermediateDate.getTime());
            adapter = new ArrayAdapter<HideUntilValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            hideUntil.setAdapter(adapter);
            hideUntil.setSelection(0);
        }

        // --- setting up values

        @Override
        public void readFromModel() {
            long date = model.getValue(Task.HIDE_UNTIL);
            long dueDate = model.getValue(Task.DUE_DATE);

            int selection = 0;
            if(Math.abs(date - dueDate) < DateUtilities.ONE_DAY) {
                selection = 1;
                date = 0;
            } else if(Math.abs(date - dueDate) < 2 * DateUtilities.ONE_DAY) {
                selection = 2;
                date = 0;
            } else if(Math.abs(date - dueDate) > DateUtilities.ONE_WEEK &&
                    Math.abs(date - dueDate) < (DateUtilities.ONE_WEEK + DateUtilities.ONE_DAY)) {
                selection = 3;
                date = 0;
            }

            HideUntilValue[] list = createHideUntilList(date);
            adapter = new ArrayAdapter<HideUntilValue>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            hideUntil.setAdapter(adapter);

            hideUntil.setSelection(selection);
        }

        @Override
        public void writeToModel() {
            HideUntilValue item = adapter.getItem(hideUntil.getSelectedItemPosition());
            long value = model.createHideUntil(item.setting, item.date);
            model.setValue(Task.HIDE_UNTIL, value);
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
                    getString(R.string.TEA_reminder_alarm_off),
                    getString(R.string.TEA_reminder_alarm_on),
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mode.setAdapter(adapter);
        }

        public void setValue(int flags) {
            during.setChecked((flags & Task.NOTIFY_AT_DEADLINE) > 0);
            after.setChecked((flags &
                    Task.NOTIFY_AFTER_DEADLINE) > 0);
            mode.setSelection((flags &
                    TaskModelForEdit.NOTIFY_NONSTOP) > 0 ? 1 : 0);
        }

        public int getValue() {
            int value = 0;
            if(during.isChecked())
                value |= Task.NOTIFY_AT_DEADLINE;
            if(after.isChecked())
                value |= Task.NOTIFY_AFTER_DEADLINE;
            if(mode.getSelectedItemPosition() == 1)
                value |= Task.NOTIFY_NONSTOP;
            return value;
        }

        @Override
        public void readFromModel() {
            setValue(model.getValue(Task.REMINDER_FLAGS));
        }

        @Override
        public void writeToModel() {
            model.setValue(Task.REMINDER_FLAGS, getValue());
        }
    }

    /**
     * Control set dealing with random reminder settings
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class RandomReminderControlSet implements TaskEditControlSet {
        private final CheckBox settingCheckbox;
        private final Spinner periodSpinner;

        private final int[] hours;

        public RandomReminderControlSet(int settingCheckboxId, int periodButtonId) {
            settingCheckbox = (CheckBox)findViewById(settingCheckboxId);
            periodSpinner = (Spinner)findViewById(periodButtonId);
            periodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    settingCheckbox.setChecked(true);
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
        public void readFromModel() {
            long time = model.getValue(Task.REMINDER_PERIOD);

            boolean shouldDisable = time <= 0;
            if(time <= 0) {
                time = DateUtilities.ONE_WEEK;
            }

            int i;
            for(i = 0; i < hours.length - 1; i++)
                if(hours[i] * DateUtilities.ONE_HOUR >= time)
                    break;
            periodSpinner.setSelection(i);
            settingCheckbox.setChecked(shouldDisable);
        }

        @Override
        public void writeToModel() {
            if(settingCheckbox.isChecked()) {
                int hourValue = hours[periodSpinner.getSelectedItemPosition()];
                model.setValue(Task.REMINDER_PERIOD, hourValue * DateUtilities.ONE_HOUR);
            } else
                model.setValue(Task.REMINDER_PERIOD, 0L);
        }
    }

    /**
     * Control set to manage adding and removing tags
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class TagsControlSet implements TaskEditControlSet {

        private final TagService tagService = new TagService();
        private final Tag[] allTags;
        private final LinearLayout tagsContainer;

        public TagsControlSet(int tagsContainer) {
            allTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE);
            this.tagsContainer = (LinearLayout) findViewById(tagsContainer);
        }

        @SuppressWarnings("nls")
        @Override
        public void readFromModel() {
            // tags (only configure if not already set)
            if(tagsContainer.getChildCount() == 0) {
                TodorooCursor<Metadata> cursor = tagService.getTags(model.getId());
                try {
                    for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                        addTag(cursor.get(Metadata.VALUE));
                } finally {
                    cursor.close();
                }
                addTag("");
            }
        }

        @Override
        public void writeToModel() {
            ArrayList<String> tags = new ArrayList<String>();

            for(int i = 0; i < tagsContainer.getChildCount(); i++) {
                TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
                if(tagName.getText().length() == 0)
                    continue;
                tags.add(tagName.getText().toString());
            }

            tagService.synchronizeTags(model.getId(), tags);
        }

        /** Adds a tag to the tag field */
        boolean addTag(String tagName) {
            if (tagsContainer.getChildCount() >= MAX_TAGS) {
                return false;
            }

            LayoutInflater inflater = getLayoutInflater();
            final View tagItem = inflater.inflate(R.layout.tag_edit_row, null);
            tagsContainer.addView(tagItem);

            AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
                findViewById(R.id.text1);
            textView.setText(tagName);
            ArrayAdapter<Tag> tagsAdapter =
                new ArrayAdapter<Tag>(TaskEditActivity.this,
                        android.R.layout.simple_dropdown_item_1line, allTags);
            textView.setAdapter(tagsAdapter);
            textView.addTextChangedListener(new TextWatcher() {
                @SuppressWarnings("nls")
                public void onTextChanged(CharSequence s, int start, int before,
                        int count) {
                    if(start == 0 && tagsContainer.getChildAt(
                            tagsContainer.getChildCount()-1) == tagItem) {
                        addTag("");
                    }
                }

                public void afterTextChanged(Editable s) {
                    //
                }

                public void beforeTextChanged(CharSequence s, int start, int count,
                        int after) {
                    //
                }
            });

            ImageButton reminderRemoveButton;
            reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
            reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    tagsContainer.removeView(tagItem);
                }
            });

            return true;
        }
    }

    public class RepeatControlSet implements TaskEditControlSet {

        private final Button repeatValue;
        private final Spinner repeatInterval;

        public RepeatControlSet(int repeatValue, int repeatInterval) {
            this.repeatValue = (Button) findViewById(repeatValue);
            this.repeatInterval = (Spinner) findViewById(repeatInterval);
            ArrayAdapter<String> repeatAdapter = new ArrayAdapter<String>(
                    TaskEditActivity.this, android.R.layout.simple_spinner_item,
                    RepeatInterval.getLabels(getResources()));
            repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.repeatInterval.setAdapter(repeatAdapter);

            this.repeatValue.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    repeatValueClick();
                }
            });
        }

        /** Set up the repeat value button */
        void setRepeatValue(int value) {
            if(value == 0)
                repeatValue.setText(R.string.repeat_value_unset);
            else
                repeatValue.setText(Integer.toString(value));
            repeatValue.setTag(value);
        }

        private RepeatInfo getRepeatValue() {
            if(repeatValue.getTag() == null || repeatValue.getTag().equals(0))
                return null;
            return new RepeatInfo(RepeatInterval.values()
                        [repeatInterval.getSelectedItemPosition()],
                    (Integer)repeatValue.getTag());
        }

        protected void repeatValueClick() {
            final int tagValue = (Integer)repeatValue.getTag();

            final Runnable openDialogRunnable = new Runnable() {
                public void run() {
                    int dialogValue = tagValue;
                    if(dialogValue == 0)
                        dialogValue = 1;

                    new NumberPickerDialog(TaskEditActivity.this, new OnNumberPickedListener() {
                        public void onNumberPicked(NumberPicker view, int number) {
                            setRepeatValue(number);
                        }
                    }, getResources().getString(R.string.repeat_picker_title),
                    dialogValue, 1, 0, 31).show();
                }
            };

            openDialogRunnable.run();
        }


        @Override
        public void readFromModel() {
            // repeats
            RepeatInfo repeatInfo = RepeatInfo.fromSingleField(model.getValue(Task.REPEAT));
            if(repeatInfo != null) {
                setRepeatValue(repeatInfo.getValue());
                repeatInterval.setSelection(repeatInfo.getInterval().ordinal());
            } else
                setRepeatValue(0);
        }


        @Override
        public void writeToModel() {
            RepeatInfo repeatInfo = getRepeatValue();
            model.setValue(Task.REPEAT, RepeatInfo.toSingleField(repeatInfo));
        }
    }

    /**
     * Calendar Control Set
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class CalendarControlSet implements TaskEditControlSet {

        /** If task has no estimated time, how early to set a task in calendar */
        private static final int DEFAULT_CAL_TIME = 3600;

        private final CheckBox addToCalendar;
        private final Button viewCalendarEvent;

        public CalendarControlSet(int addToCalendar,
                int viewCalendarEvent) {
            this.addToCalendar = (CheckBox) findViewById(addToCalendar);
            this.viewCalendarEvent = (Button) findViewById(viewCalendarEvent);
        }

        /** Take the values from the model and set the calendar start and end times
         * based on these. Sets keys 'dtstart' and 'dtend'.
         *
         * @param preferred preferred due date or null
         * @param definite definite due date or null
         * @param estimatedSeconds estimated duration or null
         * @param values
         */
        @SuppressWarnings("nls")
        public void createCalendarStartEndTimes(Date preferred, Date definite,
                Integer estimatedSeconds, ContentValues values) {
            FlurryAgent.onEvent("create-calendar-event");

            Long deadlineDate = null;
            if (preferred != null && preferred.after(new Date()))
                deadlineDate = preferred.getTime();
            else if (definite != null)
                deadlineDate = definite.getTime();
            else
                deadlineDate = System.currentTimeMillis() + 24*3600*1000L;

            int estimatedTime = DEFAULT_CAL_TIME;
            if(estimatedSeconds != null && estimatedSeconds > 0) {
                estimatedTime = estimatedSeconds;
            }
            values.put("dtstart", deadlineDate - estimatedTime * 1000L);
            values.put("dtend", deadlineDate);
        }

        protected void onPause() {
            // create calendar event
            /*if(addToCalendar.isChecked() && model.getCalendarUri() == null) {

                Uri uri = Uri.parse("content://calendar/events");
                ContentResolver cr = getContentResolver();

                ContentValues values = new ContentValues();
                values.put("title", title.getText().toString());
                values.put("calendar_id", Preferences.getDefaultCalendarIDSafe(this));
                values.put("description", notes.getText().toString());
                values.put("hasAlarm", 0);
                values.put("transparency", 0);
                values.put("visibility", 0);

                createCalendarStartEndTimes(model.getPreferredDueDate(),
                        model.getDefiniteDueDate(), model.getEstimatedSeconds(),
                        values);

                Uri result = null;
                try{
                    result = cr.insert(uri, values);
                    model.setCalendarUri(result.toString());
                } catch (IllegalArgumentException e) {
                    Log.e("astrid", "Error creating calendar event!", e);
                }
            } */

            // save save save

            /* if(addToCalendar.isChecked() && model.getCalendarUri() != null) {
                Uri result = Uri.parse(model.getCalendarUri());
                Intent intent = new Intent(Intent.ACTION_EDIT, result);

                ContentValues values = new ContentValues();
                createCalendarStartEndTimes(model.getPreferredDueDate(),
                        model.getDefiniteDueDate(), model.getEstimatedSeconds(),
                        values);

                intent.putExtra("beginTime", values.getAsLong("dtstart"));
                intent.putExtra("endTime", values.getAsLong("dtend"));

                startActivity(intent);
            } */

        }

        @Override
        public void readFromModel() {
        }

        @Override
        public void writeToModel() {
        }
    }

}
