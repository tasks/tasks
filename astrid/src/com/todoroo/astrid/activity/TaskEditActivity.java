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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.enums.RepeatInterval;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.widget.DateControlSet;
import com.timsu.astrid.widget.DateWithNullControlSet;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.TimeDurationControlSet;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;
import com.timsu.astrid.widget.TimeDurationControlSet.TimeDurationType;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.alarms.Alarm;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button
 * pressed) as long as the task has a title.
 *
 * @author timsu
 *
 */
public final class TaskEditActivity extends AbstractModelTabActivity<Task> {

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

	private static final int MAX_TAGS = 5;
	private static final int MAX_ALERTS = 5;
	private static final String TAB_BASIC = "basic"; //$NON-NLS-1$
	private static final String TAB_DATES = "dates"; //$NON-NLS-1$
	private static final String TAB_ALERTS = "alerts"; //$NON-NLS-1$
	private static final int DEFAULT_CAL_TIME = 3600;

    // --- services

    @Autowired
    TaskService taskService;

    @Autowired
    DateUtilities dateUtilities;

    TagService tagService = new TagService();

    AlarmService alarmService = new AlarmService();

	// --- UI components
    EditText title;
    ImportanceControlSet importance;
	TimeDurationControlSet estimatedDuration;
	TimeDurationControlSet elapsedDuration;
	TimeDurationControlSet notification;
	DateControlSet dueDate;
	DateControlSet preferredDueDate;
	DateControlSet hiddenUntil;
	EditText notes;
	LinearLayout tagsContainer;
	NotifyFlagControlSet flags;
	LinearLayout alertsContainer;
	Button repeatValue;
	Spinner repeatInterval;
	CheckBox addToCalendar;

	// --- other instance variables

	/** whether task should be saved when this activity exits */
	boolean shouldSaveState = true;

	/** whether help should be shown when setting repeat */
	boolean repeatHelpShown = false;

	/** list of all tags */
	Tag[] allTags;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    @Override
    protected Task fetchModel(long id) {
        database.openForWriting();

        if(id == Task.NO_ID) {
            FlurryAgent.onEvent("create-task"); //$NON-NLS-1$
            Task task = new Task();
            taskService.save(task, false);
            return task;
        }

        FlurryAgent.onEvent("edit-task"); //$NON-NLS-1$
        return taskService.fetchById(id, Task.PROPERTIES);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TabHost tabHost = getTabHost();
		tabHost.setPadding(0, 4, 0, 0);
		Resources r = getResources();

		LayoutInflater.from(this).inflate(R.layout.task_edit, tabHost.getTabContentView(), true);

		tabHost.addTab(tabHost.newTabSpec(TAB_BASIC).setIndicator(r.getString(R.string.taskEdit_tab_basic),
				r.getDrawable(R.drawable.ic_dialog_info_c)).setContent(R.id.tab_basic));
		tabHost.addTab(tabHost.newTabSpec(TAB_DATES).setIndicator(r.getString(R.string.taskEdit_tab_dates),
				r.getDrawable(R.drawable.ic_dialog_time_c)).setContent(R.id.tab_dates));
		tabHost.addTab(tabHost.newTabSpec(TAB_ALERTS).setIndicator(r.getString(R.string.taskEdit_tab_alerts),
				r.getDrawable(R.drawable.ic_dialog_alert_c)).setContent(R.id.tab_notification));

		// weird case that has been hit before.
		if(model == null)
		    model = new Task();

		setUpUIComponents();
		setUpListeners();

		// disable name input box until user requests it
		AstridUtilities.suppressVirtualKeyboard(title);
    }

    /* ======================================================================
     * ==================================================== UI initialization
     * ====================================================================== */

    /** Initialize UI components */
    private void setUpUIComponents() {
        Resources r = getResources();
        setTitle(new StringBuilder().append(r.getString(R.string.taskEdit_titleGeneric)));

        // populate instance variables
        title = (EditText)findViewById(R.id.name);
        importance = new ImportanceControlSet(R.id.importance_container);
        tagsContainer = (LinearLayout)findViewById(R.id.tags_container);
        estimatedDuration = new TimeDurationControlSet(this,
                R.id.estimatedDuration, 0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES);
        elapsedDuration = new TimeDurationControlSet(this, R.id.elapsedDuration,
                0, R.string.hour_minutes_dialog,
                TimeDurationType.HOURS_MINUTES);
        notification = new TimeDurationControlSet(this, R.id.notification,
                R.string.notification_prefix, R.string.notification_dialog,
                TimeDurationType.DAYS_HOURS);
        dueDate = new DateWithNullControlSet(this, R.id.definiteDueDate_notnull,
                R.id.definiteDueDate_date, R.id.definiteDueDate_time);
        preferredDueDate = new DateWithNullControlSet(this, R.id.preferredDueDate_notnull,
                R.id.preferredDueDate_date, R.id.preferredDueDate_time);
        hiddenUntil = new DateWithNullControlSet(this, R.id.hiddenUntil_notnull,
                R.id.hiddenUntil_date, R.id.hiddenUntil_time);
        notes = (EditText)findViewById(R.id.notes);
        flags = new NotifyFlagControlSet(R.id.flag_before,
                R.id.flag_during, R.id.flag_after, R.id.flag_nonstop);
        alertsContainer = (LinearLayout)findViewById(R.id.alert_container);
        repeatInterval = (Spinner)findViewById(R.id.repeat_interval);
        repeatValue = (Button)findViewById(R.id.repeat_value);
        addToCalendar = (CheckBox)findViewById(R.id.add_to_calendar);

        // individual ui component initialization
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,
                RepeatInterval.getLabels(getResources()));
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatInterval.setAdapter(repeatAdapter);

        // load tags
        allTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE);

        // read data
        populateFields();
    }

    /** Set up button listeners */
    private void setUpListeners() {
        Button saveButtonGeneral = (Button) findViewById(R.id.save_general);
        saveButtonGeneral.setOnClickListener(mSaveListener);

        Button saveButtonDates = (Button) findViewById(R.id.save_dates);
        saveButtonDates.setOnClickListener(mSaveListener);

        Button saveButtonNotify = (Button) findViewById(R.id.save_notify);
        saveButtonNotify.setOnClickListener(mSaveListener);

        Button discardButtonGeneral = (Button) findViewById(R.id.discard_general);
        discardButtonGeneral.setOnClickListener(mDiscardListener);

        Button discardButtonDates = (Button) findViewById(R.id.discard_dates);
        discardButtonDates.setOnClickListener(mDiscardListener);

        Button discardButtonNotify = (Button) findViewById(R.id.discard_notify);
        discardButtonNotify.setOnClickListener(mDiscardListener);

        Button deleteButtonGeneral = (Button) findViewById(R.id.delete_general);
        Button deleteButtonDates = (Button) findViewById(R.id.delete_dates);
        Button deleteButtonNotify = (Button) findViewById(R.id.delete_notify);
        if(model.getId() == Task.NO_ID) {
            deleteButtonGeneral.setVisibility(View.GONE);
            deleteButtonDates.setVisibility(View.GONE);
            deleteButtonNotify.setVisibility(View.GONE);
        } else {
            deleteButtonGeneral.setOnClickListener(mDeleteListener);
            deleteButtonDates.setOnClickListener(mDeleteListener);
            deleteButtonNotify.setOnClickListener(mDeleteListener);
        }

        Button addAlertButton = (Button) findViewById(R.id.addAlert);
        addAlertButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                addAlert(null);
            }
        });

        repeatValue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                repeatValueClick();
            }
        });
    }

    private final View.OnClickListener mSaveListener = new View.OnClickListener() {
        public void onClick(View v) {
            saveButtonClick();
        }
    };
    private final View.OnClickListener mDiscardListener = new View.OnClickListener() {
        public void onClick(View v) {
            discardButtonClick();
        }
    };
    private final View.OnClickListener mDeleteListener = new View.OnClickListener() {
        public void onClick(View v) {
            deleteButtonClick();
        }
    };

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

    /** Adds an alert to the alert field */
    protected boolean addAlert(Date alert) {
        if(alertsContainer.getChildCount() >= MAX_ALERTS)
            return false;

        LayoutInflater inflater = getLayoutInflater();
        final View alertItem = inflater.inflate(R.layout.edit_alert_item, null);
        alertsContainer.addView(alertItem);

        DateControlSet dcs = new DateControlSet(this,
                (Button)alertItem.findViewById(R.id.date),
                (Button)alertItem.findViewById(R.id.time));
        dcs.setDate(alert);
        alertItem.setTag(dcs);

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)alertItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alertsContainer.removeView(alertItem);
            }
        });

        return true;
    }

    /* ======================================================================
     * =============================================== model reading / saving
     * ====================================================================== */

    /** Populate UI component values from the model */
    private void populateFields() {
        Resources r = getResources();

    	title.setText(model.getValue(Task.TITLE));
        if(title.getText().length() > 0) {
            setTitle(new StringBuilder().
                append(r.getString(R.string.taskEdit_titlePrefix)).
                append(" "). //$NON-NLS-1$
                append(title.getText()));
        }
        estimatedDuration.setTimeDuration(model.getValue(Task.ESTIMATED_SECONDS));
        elapsedDuration.setTimeDuration(model.getValue(Task.ELAPSED_SECONDS));
        importance.setImportance(model.getValue(Task.IMPORTANCE));
        dueDate.setDate(model.getValue(Task.DUE_DATE));
        hiddenUntil.setDate(model.getValue(Task.HIDE_UNTIL));
        notification.setTimeDuration(model.getValue(Task.NOTIFICATIONS));
        flags.setValue(model.getValue(Task.NOTIFICATION_FLAGS));
        notes.setText(model.getValue(Task.NOTES));
        if(model.getValue(Task.CALENDAR_URI).length() > 0)
            addToCalendar.setText(r.getString(R.string.showCalendar_label));

        // tags (only configure if not already set)
        if(tagsContainer.getChildCount() == 0) {
            TodorooCursor<Metadata> cursor = tagService.getTags(model.getId());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                    addTag(cursor.get(Metadata.VALUE));
            } finally {
                cursor.close();
            }
            addTag(""); //$NON-NLS-1$
        }

        /// alarms
        if(alertsContainer.getChildCount() == 0) {
            TodorooCursor<Alarm> cursor = alarmService.getAlarms(model.getId());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                    addAlert(new Date(cursor.get(Alarm.TIME)));
            } finally {
                cursor.close();
            }
        }

        // repeats
        RepeatInfo repeatInfo = RepeatInfo.fromSingleField(model.getValue(Task.REPEAT));
        if(repeatInfo != null) {
            setRepeatValue(repeatInfo.getValue());
            repeatInterval.setSelection(repeatInfo.getInterval().ordinal());
        } else
            setRepeatValue(0);

    }

    /** Save task model from values in UI components */
    private void save() {
        // don't save if user accidentally created a new task
        if(title.getText().length() == 0) {
            if(model.isSaved())
                taskService.delete(model);
            return;
        }

        model.setValue(Task.TITLE, title.getText().toString());
        model.setValue(Task.ESTIMATED_SECONDS, estimatedDuration.getTimeDurationInSeconds());
        model.setValue(Task.ELAPSED_SECONDS, elapsedDuration.getTimeDurationInSeconds());
        model.setValue(Task.IMPORTANCE, importance.getImportance());
        model.setValue(Task.DUE_DATE, dueDate.getMillis());
        model.setValue(Task.HIDE_UNTIL, hiddenUntil.getMillis());
        model.setValue(Task.NOTIFICATION_FLAGS, flags.getValue());
        model.setValue(Task.NOTES, notes.getText().toString());
        model.setValue(Task.NOTIFICATIONS, notification.getTimeDurationInSeconds());
        model.setValue(Task.REPEAT, RepeatInfo.toSingleField(getRepeatValue()));

        taskService.save(model, false);
        saveTags();
        saveAlerts();

        long due = model.getValue(Task.DUE_DATE);
        if (due != 0) {
        	showSaveToast(due);
        } else {
        	showSaveToast();
        }
    }

    /**
     * Displays a Toast reporting that the selected task has been saved and is
     * due in 'x' amount of time, to 2 time-units of precision (e.g. Days + Hours).
     * @param due the Date when the task is due
     */
    private void showSaveToast(long due) {
    	int stringResource;

    	long dueFromNow = due - System.currentTimeMillis();

    	if (dueFromNow < 0) {
    		stringResource = R.string.taskEdit_onTaskSave_Overdue;
    	} else {
    		stringResource = R.string.taskEdit_onTaskSave_Due;
    	}
    	String formattedDate = dateUtilities.getDurationString(dueFromNow, 2);
    	Toast.makeText(this,
    			getResources().getString(stringResource, formattedDate),
    			Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a Toast reporting that the selected task has been saved.
     * Use this version when no due Date has been set.
     */
    private void showSaveToast() {
    	Toast.makeText(this, R.string.taskEdit_onTaskSave_notDue, Toast.LENGTH_SHORT).show();
    }

    /** Save task tags. Must be called after task already has an ID */
    private void saveTags() {
        ArrayList<String> tags = new ArrayList<String>();

        for(int i = 0; i < tagsContainer.getChildCount(); i++) {
            TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
            if(tagName.getText().length() == 0)
                continue;
            tags.add(tagName.getText().toString());
        }

        tagService.synchronizeTags(model.getId(), tags);
    }

    /** Helper method to save alerts for this task */
    private void saveAlerts() {
        ArrayList<Alarm> alarms = new ArrayList<Alarm>();

        for(int i = 0; i < alertsContainer.getChildCount(); i++) {
            DateControlSet dateControlSet = (DateControlSet)alertsContainer.
                getChildAt(i).getTag();
            Date date = dateControlSet.getDate();
            Alarm alarm = new Alarm();
            alarm.setValue(Alarm.TIME, date.getTime());
        }

        alarmService.synchronizeAlarms(model.getId(), alarms);
    }


    /** Adds a tag to the tag field */
    boolean addTag(String tagName) {
        if (tagsContainer.getChildCount() >= MAX_TAGS) {
            return false;
        }

        LayoutInflater inflater = getLayoutInflater();
        final View tagItem = inflater.inflate(R.layout.edit_tag_item, null);
        tagsContainer.addView(tagItem);

        AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
            findViewById(R.id.text1);
        textView.setText(tagName);
        ArrayAdapter<Tag> tagsAdapter =
            new ArrayAdapter<Tag>(this,
                    android.R.layout.simple_dropdown_item_1line, allTags);
        textView.setAdapter(tagsAdapter);
        textView.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(start == 0 && tagsContainer.getChildAt(
                        tagsContainer.getChildCount()-1) == tagItem) {
                    addTag(""); //$NON-NLS-1$
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

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    protected void saveButtonClick() {
        setResult(RESULT_OK);
        finish();
    }

    protected void discardButtonClick() {
        shouldSaveState = false;
        setResult(Constants.RESULT_DISCARD);
        finish();
    }

    protected void deleteButtonClick() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_task_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    taskService.delete(model);
                    shouldSaveState = false;
                    setResult(Constants.RESULT_GO_HOME);
                    finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    protected void repeatValueClick() {
        final int tagValue = (Integer)repeatValue.getTag();
        if(tagValue > 0)
            repeatHelpShown = true;

        final Runnable openDialogRunnable = new Runnable() {
            public void run() {
                repeatHelpShown = true;

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

        if(repeatHelpShown || !Preferences.shouldShowRepeatHelp(this)) {
            openDialogRunnable.run();
            return;
        }

        new AlertDialog.Builder(this)
        .setTitle(R.string.repeat_help_dialog_title)
        .setMessage(R.string.repeat_help_dialog)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                openDialogRunnable.run();
            }
        })
        .setNeutralButton(R.string.repeat_help_hide,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Preferences.setShowRepeatHelp(TaskEditActivity.this, false);
                openDialogRunnable.run();
            }
        })
        .show();
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

        item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.save_label);
        item.setIcon(android.R.drawable.ic_menu_save);
        item.setAlphabeticShortcut('s');

        item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.discard_label);
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        item.setAlphabeticShortcut('c');

        item = menu.add(Menu.NONE, MENU_DELETE_ID, 0, R.string.delete_label);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setAlphabeticShortcut('d');

        return true;
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
    public static void createCalendarStartEndTimes(Date preferred, Date definite,
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

    @Override
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

        if(shouldSaveState)
            save();

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

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // populateFields();
    }

    /* ======================================================================
     * ========================================== UI component helper classes
     * ====================================================================== */

    /** Control set dealing with notification flags */
    public class NotifyFlagControlSet {
        private final CheckBox before, during, after, nonstop;

        public NotifyFlagControlSet(int beforeId, int duringId,
                int afterId, int nonstopId) {
            before = (CheckBox)findViewById(beforeId);
            during = (CheckBox)findViewById(duringId);
            after = (CheckBox)findViewById(afterId);
            nonstop = (CheckBox)findViewById(nonstopId);
        }

        public void setValue(int flags) {
            before.setChecked((flags &
                    TaskModelForEdit.NOTIFY_BEFORE_DEADLINE) > 0);
            during.setChecked((flags &
                    TaskModelForEdit.NOTIFY_AT_DEADLINE) > 0);
            after.setChecked((flags &
                    TaskModelForEdit.NOTIFY_AFTER_DEADLINE) > 0);
            nonstop.setChecked((flags &
                    TaskModelForEdit.NOTIFY_NONSTOP) > 0);
        }

        public int getValue() {
            int value = 0;
            if(before.isChecked())
                value |= TaskModelForEdit.NOTIFY_BEFORE_DEADLINE;
            if(during.isChecked())
                value |= TaskModelForEdit.NOTIFY_AT_DEADLINE;
            if(after.isChecked())
                value |= TaskModelForEdit.NOTIFY_AFTER_DEADLINE;
            if(nonstop.isChecked())
                value |= TaskModelForEdit.NOTIFY_NONSTOP;
            return value;
        }
    }

    /** Control set dealing with importance */
    public class ImportanceControlSet {
        private final List<CompoundButton> buttons = new LinkedList<CompoundButton>();

        private final int[] colors = Task.getImportanceColors(getResources());

        public ImportanceControlSet(int containerId) {
            LinearLayout layout = (LinearLayout)findViewById(containerId);

            for(int i = Task.IMPORTANCE_MOST; i <= Task.IMPORTANCE_LEAST; i++) {
                final ToggleButton button = new ToggleButton(TaskEditActivity.this);
                button.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

                StringBuilder label = new StringBuilder();
                for(int j = Task.IMPORTANCE_LEAST; j >= 0; j--)
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
                } else {
                    b.setTextSize(16);
                    b.setChecked(false);
                }
            }
        }

        public int getImportance() {
            for(CompoundButton b : buttons)
                if(b.isChecked())
                    return (Integer) b.getTag();
            return Task.getStaticDefaultValues().getAsInteger(Task.IMPORTANCE.name);
        }
    }

}
