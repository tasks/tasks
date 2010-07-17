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
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.widget.TimeDurationControlSet;
import com.timsu.astrid.widget.TimeDurationControlSet.TimeDurationType;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.DeadlineTimePickerDialog;
import com.todoroo.astrid.widget.DeadlineTimePickerDialog.OnDeadlineTimeSetListener;

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

	/** edit control receiver */
	private final ControlReceiver controlReceiver = new ControlReceiver();

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
        controls.add(new TagsControlSet(this, R.id.tags_container));

        // internal add-ins
        LinearLayout extrasAddons = (LinearLayout) findViewById(R.id.tab_extra_addons);
        controls.add(new RepeatControlSet(this, extrasAddons));

        LinearLayout addonsAddons = (LinearLayout) findViewById(R.id.tab_addons_addons);
        controls.add(new GCalControlSet(this, addonsAddons));
        controls.add(new TimeDurationTaskEditControlSet(Task.ESTIMATED_SECONDS,
                R.id.estimatedDuration, 0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES));
        controls.add(new TimeDurationTaskEditControlSet(Task.ELAPSED_SECONDS, R.id.elapsedDuration,
                0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES));

        // read data
        populateFields();

        // request add-on controls
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_EDIT_CONTROLS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, model.getId());
        sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

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
        if(idParam > -1L) {
            model = taskService.fetchById(idParam, Task.PROPERTIES);
        }

        // not found by id or was never passed an id
        if(model == null) {
            model = new Task();
            taskService.save(model, false);
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
            controlSet.readFromTask(model);
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
            controlSet.writeToModel(model);

        taskService.save(model, false);
        showSaveToast();
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

        // abandon editing in this case
        if(title.getText().length() == 0) {
            if(isNewTask())
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
        unregisterReceiver(controlReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(controlReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_EDIT_CONTROLS));
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
        public void readFromTask(Task task);

        /**
         * Write data from control set to model
         */
        public void writeToModel(Task task);
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
        public void writeToModel(Task task) {
            task.setValue(property, editText.getText().toString());
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
        public void readFromTask(Task task) {
            controlSet.setTimeDuration(task.getValue(property));
        }

        @Override
        public void writeToModel(Task task) {
            task.setValue(property, controlSet.getTimeDurationInSeconds());
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
        public void readFromTask(Task task) {
            setImportance(task.getValue(Task.IMPORTANCE));
        }

        @Override
        public void writeToModel(Task task) {
            task.setValue(Task.IMPORTANCE, getImportance());
        }
    }

    // --- UrgencyControlSet

    private class UrgencyControlSet implements TaskEditControlSet,
            OnItemSelectedListener, OnDeadlineTimeSetListener, OnDateSetListener {

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
            urgencyValues[0] = new UrgencyValue(labels[0],
                    Task.URGENCY_SPECIFIC_DAY_TIME, SPECIFIC_DATE);
            urgencyValues[1] = new UrgencyValue(labels[1],
                    Task.URGENCY_TODAY);
            urgencyValues[2] = new UrgencyValue(labels[2],
                    Task.URGENCY_TOMORROW);
            String dayAfterTomorrow = new SimpleDateFormat("EEEE").format( //$NON-NLS-1$
                    new Date(DateUtilities.now() + 2 * DateUtilities.ONE_DAY));
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

            new DeadlineTimePickerDialog(TaskEditActivity.this, this,
                    customDate.getHours(), customDate.getMinutes(),
                    DateUtilities.is24HourFormat(TaskEditActivity.this)).show();
        }

        public void onTimeSet(TimePicker view, boolean hasTime, int hourOfDay, int minute) {
            if(!hasTime)
                customSetting = Task.URGENCY_SPECIFIC_DAY;
            else {
                customDate.setHours(hourOfDay);
                customDate.setMinutes(minute);
            }
            customDateFinished();
        }

        private void customDateFinished() {
            long time = model.createDueDate(customSetting, customDate.getTime());
            createUrgencyList(time);
        }

        // --- setting up values

        @Override
        public void readFromTask(Task task) {
            long dueDate = task.getValue(Task.DUE_DATE);
            createUrgencyList(dueDate);
        }

        @Override
        public void writeToModel(Task task) {
            UrgencyValue item = urgencyAdapter.getItem(urgency.getSelectedItemPosition());
            if(item.dueDate != SPECIFIC_DATE) // user cancelled specific date
                task.setValue(Task.DUE_DATE, item.dueDate);
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
                HideUntilValue[] updated = new HideUntilValue[values.length + 1];
                for(int i = 0; i < values.length; i++)
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
        public void readFromTask(Task task) {
            long date = task.getValue(Task.HIDE_UNTIL);
            long dueDate = task.getValue(Task.DUE_DATE);

            int selection = 0;
            if(date == 0) {
                selection = 0;
                date = 0;
            } else if(Math.abs(date - dueDate) < DateUtilities.ONE_DAY) {
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
        public void writeToModel(Task task) {
            HideUntilValue item = adapter.getItem(hideUntil.getSelectedItemPosition());
            long value = task.createHideUntil(item.setting, item.date);
            task.setValue(Task.HIDE_UNTIL, value);
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
        public void readFromTask(Task task) {
            setValue(task.getValue(Task.REMINDER_FLAGS));
        }

        @Override
        public void writeToModel(Task task) {
            task.setValue(Task.REMINDER_FLAGS, getValue());
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
        public void readFromTask(Task task) {
            long time = task.getValue(Task.REMINDER_PERIOD);

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
        public void writeToModel(Task task) {
            if(settingCheckbox.isChecked()) {
                int hourValue = hours[periodSpinner.getSelectedItemPosition()];
                task.setValue(Task.REMINDER_PERIOD, hourValue * DateUtilities.ONE_HOUR);
            } else
                task.setValue(Task.REMINDER_PERIOD, 0L);
        }
    }

}
