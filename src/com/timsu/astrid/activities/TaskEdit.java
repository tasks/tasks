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
package com.timsu.astrid.activities;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.enums.RepeatInterval;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.utilities.Notifications;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.widget.DateControlSet;
import com.timsu.astrid.widget.DateWithNullControlSet;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.TimeDurationControlSet;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;
import com.timsu.astrid.widget.TimeDurationControlSet.TimeDurationType;

/**
 * This activity is responsible for creating new tasks and editing existing
 * ones. It saves a task when it is paused (screen rotated, back button pressed)
 * as long as the task has a title.
 *
 * @author timsu
 *
 */
public class TaskEdit extends TaskModificationTabbedActivity<TaskModelForEdit> {

	// bundle arguments
	public static final String TAG_NAME_TOKEN = "t";
	public static final String START_CHAR_TOKEN = "s";

	// menu items
	private static final int SAVE_ID = Menu.FIRST;
	private static final int DISCARD_ID = Menu.FIRST + 1;
	private static final int DELETE_ID = Menu.FIRST + 2;

	// other constants
	private static final int MAX_TAGS = 5;
	private static final int MAX_ALERTS = 5;
	private static final String TAB_BASIC = "basic";
	private static final String TAB_DATES = "dates";
	private static final String TAB_ALERTS = "alerts";
	private static final int DEFAULT_CAL_TIME = 3600;

	// UI components
	private EditText name;
	private ImportanceControlSet importance;
	private TimeDurationControlSet estimatedDuration;
	private TimeDurationControlSet elapsedDuration;
	private TimeDurationControlSet notification;
	private DateControlSet definiteDueDate;
	private DateControlSet preferredDueDate;
	private DateControlSet hiddenUntil;
	private EditText notes;
	private LinearLayout tagsContainer;
	private NotifyFlagControlSet flags;
	private LinearLayout alertsContainer;
	private Button repeatValue;
	private Spinner repeatInterval;
	private CheckBox addToCalendar;

	// other instance variables
	private boolean shouldSaveState = true;
	private boolean repeatHelpShown = false;
	private TagController tagController;
	private AlertController alertController;
	private List<TagModelForView> tags;
	private List<TagIdentifier> taskTags;

	// OnClickListeners for save, discard and delete
	private View.OnClickListener mSaveListener = new View.OnClickListener() {
		public void onClick(View v) {
			saveButtonClick();
		}
	};
	private View.OnClickListener mDiscardListener = new View.OnClickListener() {
		public void onClick(View v) {
			discardButtonClick();
		}
	};
	private View.OnClickListener mDeleteListener = new View.OnClickListener() {
		public void onClick(View v) {
			deleteButtonClick();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		long taskId = 0;
		try {
			taskId = intent.getExtras().getLong("id");
		} catch (Exception e) {
			e.printStackTrace();
		}
//		Log.d("astrid", "id = " + taskId);

		tagController = new TagController(this);
		tagController.open();
		alertController = new AlertController(this);
		alertController.open();

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

		setUpUIComponents();
		setUpListeners();

		// disable name input box until user requests it
		AstridUtilities.suppressVirtualKeyboard(name);
	}

	@Override
	protected TaskModelForEdit getModel(TaskIdentifier identifier) {
		if (identifier != null)
			return controller.fetchTaskForEdit(this, identifier);
		else
			return controller.createNewTaskForEdit();
	}

	/*
	 * ======================================================================
	 * =============================================== model reading / saving
	 * ======================================================================
	 */

	/** Populate UI component values from the model */
	private void populateFields() {
		Resources r = getResources();

		// set UI components based on model variables
		if (model.getCursor() != null)
			startManagingCursor(model.getCursor());
		if (model.getTaskIdentifier() == null) {
			FlurryAgent.onEvent("create-task");
			Bundle extras = getIntent().getExtras();
			if (extras != null && extras.containsKey(START_CHAR_TOKEN))
				name.setText("" + extras.getChar(START_CHAR_TOKEN));
		} else {
			FlurryAgent.onEvent("edit-task");
			name.setText(model.getName());
		}

		if (model.getName().length() > 0)
			setTitle(new StringBuilder().append(r.getString(R.string.taskEdit_titlePrefix)).append(" ").append(
					model.getName()));
		estimatedDuration.setTimeDuration(model.getEstimatedSeconds());
		elapsedDuration.setTimeDuration(model.getElapsedSeconds());
		importance.setImportance(model.getImportance());
		definiteDueDate.setDate(model.getDefiniteDueDate());
		preferredDueDate.setDate(model.getPreferredDueDate());
		hiddenUntil.setDate(model.getHiddenUntil());
		notification.setTimeDuration(model.getNotificationIntervalSeconds());
		flags.setValue(model.getNotificationFlags());
		notes.setText(model.getNotes());
		if (model.getTaskIdentifier() == null) {
			Integer reminder = Preferences.getDefaultReminder(this);
			if (reminder != null)
				notification.setTimeDuration(24 * 3600 * reminder);
		}
		if (model.getCalendarUri() != null)
			addToCalendar.setText(r.getString(R.string.showCalendar_label));

		// tags (only configure if not already set)
		if (tagsContainer.getChildCount() == 0) {
			tags = tagController.getAllTags();
			if (model.getTaskIdentifier() != null) {
				taskTags = tagController.getTaskTags(model.getTaskIdentifier());
				if (taskTags.size() > 0) {
					Map<TagIdentifier, TagModelForView> tagsMap = new HashMap<TagIdentifier, TagModelForView>();
					for (TagModelForView tag : tags)
						tagsMap.put(tag.getTagIdentifier(), tag);
					for (TagIdentifier id : taskTags) {
						if (!tagsMap.containsKey(id))
							continue;

						TagModelForView tag = tagsMap.get(id);
						addTag(tag.getName());
					}
				}
			} else {
				taskTags = new LinkedList<TagIdentifier>();

				Bundle extras = getIntent().getExtras();
				if (extras != null && extras.containsKey(TAG_NAME_TOKEN)) {
					addTag(extras.getString(TAG_NAME_TOKEN));
				}
			}
			addTag("");
		}

		// alerts
		if (model.getTaskIdentifier() != null) {
			List<Date> alerts = alertController.getTaskAlerts(model.getTaskIdentifier());
			for (Date alert : alerts) {
				addAlert(alert);
			}
		}

		// repeats
		RepeatInfo repeatInfo = model.getRepeat();
		if (repeatInfo != null) {
			setRepeatValue(repeatInfo.getValue());
			repeatInterval.setSelection(repeatInfo.getInterval().ordinal());
		} else
			setRepeatValue(0);

	}

	/** Save task model from values in UI components */
	private void save() {
		// don't save if user accidentally created a new task
		if (name.getText().length() == 0)
			return;

		// tell the task list to update itself
		TaskListSubActivity.shouldRefreshTaskList = true;

		model.setName(name.getText().toString());
		model.setEstimatedSeconds(estimatedDuration.getTimeDurationInSeconds());
		model.setElapsedSeconds(elapsedDuration.getTimeDurationInSeconds());
		model.setImportance(importance.getImportance());
		model.setDefiniteDueDate(definiteDueDate.getDate());
		model.setPreferredDueDate(preferredDueDate.getDate());
		model.setHiddenUntil(hiddenUntil.getDate());
		model.setNotificationFlags(flags.getValue());
		model.setNotes(notes.getText().toString());
		model.setNotificationIntervalSeconds(notification.getTimeDurationInSeconds());
		model.setRepeat(getRepeatValue());

		try {
			// write out to database
			controller.saveTask(model, false);
			saveTags();
			saveAlerts();
			Notifications.updateAlarm(this, controller, alertController, model);

			Date dueDate = model.getPreferredDueDate();
			if (dueDate == null) {
				dueDate = model.getDefiniteDueDate();
			}
			if (dueDate != null && model.getProgressPercentage() != TaskModelForEdit.COMPLETE_PERCENTAGE) {
				showSaveToast(dueDate);
			} else {
				showSaveToast();
			}

		} catch (Exception e) {
			Log.e("astrid", "Error saving", e);
		}
	}

	/**
	 * Displays a Toast reporting that the selected task has been saved and is
	 * due in 'x' amount of time, to 2 time-units of precision (e.g. Days +
	 * Hours).
	 *
	 * @param dueDate
	 *            the Date when the task is due
	 */
	private void showSaveToast(Date dueDate) {
		int stringResource;

		int timeInSeconds = (int) ((dueDate.getTime() - System.currentTimeMillis()) / 1000L);

		if (timeInSeconds < 0) {
			timeInSeconds *= -1; // DateUtilities.getDurationString() requires
			// positive integer
			stringResource = R.string.taskEdit_onTaskSave_Overdue;
		} else {
			stringResource = R.string.taskEdit_onTaskSave_Due;
		}
		String formattedDate = DateUtilities.getDurationString(getResources(), timeInSeconds, 2);
		Toast.makeText(this, getResources().getString(stringResource, formattedDate), Toast.LENGTH_SHORT).show();
	}

	/**
	 * Displays a Toast reporting that the selected task has been saved. Use
	 * this version when no due Date has been set.
	 */
	private void showSaveToast() {
		Toast.makeText(this, R.string.taskEdit_onTaskSave_notDue, Toast.LENGTH_SHORT).show();
	}

	/** Save task tags. Must be called after task already has an ID */
	private void saveTags() {
		Set<TagIdentifier> tagsToDelete;
		Set<TagIdentifier> tagsToAdd;

		HashSet<String> tagNames = new HashSet<String>();
		for (int i = 0; i < tagsContainer.getChildCount(); i++) {
			TextView tagName = (TextView) tagsContainer.getChildAt(i).findViewById(R.id.text1);
			if (tagName.getText().length() == 0)
				continue;
			tagNames.add(tagName.getText().toString());
		}

		// map names to tag identifiers, creating them if necessary
		HashSet<TagIdentifier> tagIds = new HashSet<TagIdentifier>();
		HashMap<String, TagIdentifier> tagsByName = new HashMap<String, TagIdentifier>();
		for (TagModelForView tag : tags)
			tagsByName.put(tag.getName(), tag.getTagIdentifier());
		for (String tagName : tagNames) {
			if (tagsByName.containsKey(tagName))
				tagIds.add(tagsByName.get(tagName));
			else {
				TagIdentifier newTagId = tagController.createTag(tagName);
				tagIds.add(newTagId);
			}
		}

		// intersect tags to figure out which we need to add / remove
		tagsToDelete = new HashSet<TagIdentifier>(taskTags);
		tagsToDelete.removeAll(tagIds);
		tagsToAdd = tagIds;
		tagsToAdd.removeAll(taskTags);

		// perform the database updates
		for (TagIdentifier tagId : tagsToDelete)
			tagController.removeTag(model.getTaskIdentifier(), tagId);
		for (TagIdentifier tagId : tagsToAdd)
			tagController.addTag(model.getTaskIdentifier(), tagId);

		if (tagsToDelete.size() > 0 || tagsToAdd.size() > 0)
			SyncDataController.taskUpdated(this, model);
	}

	/** Helper method to save alerts for this task */
	private void saveAlerts() {
		alertController.removeAlerts(model.getTaskIdentifier());

		for (int i = 0; i < alertsContainer.getChildCount(); i++) {
			DateControlSet dateControlSet = (DateControlSet) alertsContainer.getChildAt(i).getTag();
			Date date = dateControlSet.getDate();
			alertController.addAlert(model.getTaskIdentifier(), date);
		}
	}

	/*
	 * ======================================================================
	 * ==================================================== UI initialization
	 * ======================================================================
	 */

	/** Initialize UI components */
	private void setUpUIComponents() {
		Resources r = getResources();
		setTitle(new StringBuilder().append(r.getString(R.string.taskEdit_titleGeneric)));

		// populate instance variables
		name = (EditText) findViewById(R.id.name);
		importance = new ImportanceControlSet(R.id.importance_container);
		tagsContainer = (LinearLayout) findViewById(R.id.tags_container);
		estimatedDuration = new TimeDurationControlSet(this, R.id.estimatedDuration, 0, R.string.hour_minutes_dialog,
				TimeDurationType.HOURS_MINUTES);
		elapsedDuration = new TimeDurationControlSet(this, R.id.elapsedDuration, 0, R.string.hour_minutes_dialog,
				TimeDurationType.HOURS_MINUTES);
		notification = new TimeDurationControlSet(this, R.id.notification, R.string.notification_prefix,
				R.string.notification_dialog, TimeDurationType.DAYS_HOURS);
		definiteDueDate = new DateWithNullControlSet(this, R.id.definiteDueDate_notnull, R.id.definiteDueDate_date,
				R.id.definiteDueDate_time);
		preferredDueDate = new DateWithNullControlSet(this, R.id.preferredDueDate_notnull, R.id.preferredDueDate_date,
				R.id.preferredDueDate_time);
		hiddenUntil = new DateWithNullControlSet(this, R.id.hiddenUntil_notnull, R.id.hiddenUntil_date,
				R.id.hiddenUntil_time);
		notes = (EditText) findViewById(R.id.notes);
		flags = new NotifyFlagControlSet(R.id.flag_before, R.id.flag_during, R.id.flag_after, R.id.flag_nonstop);
		alertsContainer = (LinearLayout) findViewById(R.id.alert_container);
		repeatInterval = (Spinner) findViewById(R.id.repeat_interval);
		repeatValue = (Button) findViewById(R.id.repeat_value);
		addToCalendar = (CheckBox) findViewById(R.id.add_to_calendar);

		// individual ui component initialization
		ArrayAdapter<String> repeatAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				RepeatInterval.getLabels(getResources()));
		repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		repeatInterval.setAdapter(repeatAdapter);
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
		if (model.getTaskIdentifier() == null) {
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

	/** Set up the repeat value button */
	private void setRepeatValue(int value) {
		if (value == 0)
			repeatValue.setText(R.string.repeat_value_unset);
		else
			repeatValue.setText(Integer.toString(value));
		repeatValue.setTag(value);
	}

	private RepeatInfo getRepeatValue() {
		if (repeatValue.getTag().equals(0))
			return null;
		return new RepeatInfo(RepeatInterval.values()[repeatInterval.getSelectedItemPosition()], (Integer) repeatValue
				.getTag());
	}

	/** Adds an alert to the alert field */
	private boolean addAlert(Date alert) {
		if (alertsContainer.getChildCount() >= MAX_ALERTS)
			return false;

		LayoutInflater inflater = getLayoutInflater();
		final View alertItem = inflater.inflate(R.layout.edit_alert_item, null);
		alertsContainer.addView(alertItem);

		DateControlSet dcs = new DateControlSet(this, (Button) alertItem.findViewById(R.id.date), (Button) alertItem
				.findViewById(R.id.time));
		dcs.setDate(alert);
		alertItem.setTag(dcs);

		ImageButton reminderRemoveButton;
		reminderRemoveButton = (ImageButton) alertItem.findViewById(R.id.button1);
		reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				alertsContainer.removeView(alertItem);
			}
		});

		return true;
	}

	/** Adds a tag to the tag field */
	private boolean addTag(String tagName) {
		if (tagsContainer.getChildCount() >= MAX_TAGS) {
			return false;
		}

		LayoutInflater inflater = getLayoutInflater();
		final View tagItem = inflater.inflate(R.layout.edit_tag_item, null);
		tagsContainer.addView(tagItem);

		AutoCompleteTextView textView = (AutoCompleteTextView) tagItem.findViewById(R.id.text1);
		textView.setText(tagName);
		ArrayAdapter<TagModelForView> tagsAdapter = new ArrayAdapter<TagModelForView>(this,
				android.R.layout.simple_dropdown_item_1line, tags);
		textView.setAdapter(tagsAdapter);
		textView.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (start == 0 && tagsContainer.getChildAt(tagsContainer.getChildCount() - 1) == tagItem) {
					addTag("");
				}
			}

			public void afterTextChanged(Editable s) {
				//
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				//
			}
		});

		ImageButton reminderRemoveButton;
		reminderRemoveButton = (ImageButton) tagItem.findViewById(R.id.button1);
		reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tagsContainer.removeView(tagItem);
			}
		});

		return true;
	}

	/*
	 * ======================================================================
	 * ======================================================= event handlers
	 * ======================================================================
	 */

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus && TaskList.shouldCloseInstance) { // user wants to quit
			finish();
		}
	}

	private void saveButtonClick() {
		setResult(RESULT_OK);
		finish();
	}

	private void discardButtonClick() {
		shouldSaveState = false;
		setResult(Constants.RESULT_DISCARD);
		finish();
	}

	private void deleteButtonClick() {
		new AlertDialog.Builder(this).setTitle(R.string.delete_title).setMessage(R.string.delete_this_task_title)
				.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// tell the task list to update itself
								TaskListSubActivity.shouldRefreshTaskList = true;

								controller.deleteTask(model.getTaskIdentifier());
								shouldSaveState = false;
								setResult(Constants.RESULT_GO_HOME);
								finish();
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
	}

	private void repeatValueClick() {
		final int tagValue = (Integer) repeatValue.getTag();
		if (tagValue > 0)
			repeatHelpShown = true;

		final Runnable openDialogRunnable = new Runnable() {
			public void run() {
				repeatHelpShown = true;

				int dialogValue = tagValue;
				if (dialogValue == 0)
					dialogValue = 1;

				new NumberPickerDialog(TaskEdit.this, new OnNumberPickedListener() {
					public void onNumberPicked(NumberPicker view, int number) {
						setRepeatValue(number);
					}
				}, getResources().getString(R.string.repeat_picker_title), dialogValue, 1, 0, 31).show();
			}
		};

		if (repeatHelpShown || !Preferences.shouldShowRepeatHelp(this)) {
			openDialogRunnable.run();
			return;
		}

		new AlertDialog.Builder(this).setTitle(R.string.repeat_help_dialog_title).setMessage(
				R.string.repeat_help_dialog).setIcon(android.R.drawable.ic_dialog_info).setPositiveButton(
				android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						openDialogRunnable.run();
					}
				}).setNeutralButton(R.string.repeat_help_hide, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Preferences.setShowRepeatHelp(TaskEdit.this, false);
				openDialogRunnable.run();
			}
		}).show();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_ID:
			saveButtonClick();
			return true;
		case DISCARD_ID:
			discardButtonClick();
			return true;
		case DELETE_ID:
			deleteButtonClick();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item;

		item = menu.add(Menu.NONE, SAVE_ID, 0, R.string.save_label);
		item.setIcon(android.R.drawable.ic_menu_save);
		item.setAlphabeticShortcut('s');

		item = menu.add(Menu.NONE, DISCARD_ID, 0, R.string.discard_label);
		item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		item.setAlphabeticShortcut('c');

		if (model.getTaskIdentifier() != null) {
			item = menu.add(Menu.NONE, DELETE_ID, 0, R.string.delete_label);
			item.setIcon(android.R.drawable.ic_menu_delete);
			item.setAlphabeticShortcut('d');
		}

		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// save the tag name token for when we rotate the screen
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(TAG_NAME_TOKEN))
			outState.putString(TAG_NAME_TOKEN, extras.getString(TAG_NAME_TOKEN));
	}

	/**
	 * Take the values from the model and set the calendar start and end times
	 * based on these. Sets keys 'dtstart' and 'dtend'.
	 *
	 * @param preferred
	 *            preferred due date or null
	 * @param definite
	 *            definite due date or null
	 * @param estimatedSeconds
	 *            estimated duration or null
	 * @param values
	 */
	public static void createCalendarStartEndTimes(Date preferred, Date definite, Integer estimatedSeconds,
			ContentValues values) {
		FlurryAgent.onEvent("create-calendar-event");

		Long deadlineDate = null;
		if (preferred != null && preferred.after(new Date()))
			deadlineDate = preferred.getTime();
		else if (definite != null)
			deadlineDate = definite.getTime();
		else
			deadlineDate = System.currentTimeMillis() + 24 * 3600 * 1000L;

		int estimatedTime = DEFAULT_CAL_TIME;
		if (estimatedSeconds != null && estimatedSeconds > 0) {
			estimatedTime = estimatedSeconds;
		}
		values.put("dtstart", deadlineDate - estimatedTime * 1000L);
		values.put("dtend", deadlineDate);
	}

	@Override
	protected void onPause() {
		// create calendar event
		if (addToCalendar.isChecked() && model.getCalendarUri() == null) {
			Uri uri = Uri.parse("content://calendar/events");
			ContentResolver cr = getContentResolver();

			ContentValues values = new ContentValues();
			values.put("title", name.getText().toString());
			values.put("calendar_id", 1);
			values.put("description", notes.getText().toString());
			values.put("hasAlarm", 0);
			values.put("transparency", 0);
			values.put("visibility", 0);

			createCalendarStartEndTimes(model.getPreferredDueDate(), model.getDefiniteDueDate(), model
					.getEstimatedSeconds(), values);

			Uri result = cr.insert(uri, values);
			if (result != null)
				model.setCalendarUri(result.toString());
			else
				Log.e("astrid", "Error creating calendar event!");
		}

		if (shouldSaveState)
			save();

		if (addToCalendar.isChecked() && model.getCalendarUri() != null) {
			Uri result = Uri.parse(model.getCalendarUri());
			Intent intent = new Intent(Intent.ACTION_EDIT, result);

			ContentValues values = new ContentValues();
			createCalendarStartEndTimes(model.getPreferredDueDate(), model.getDefiniteDueDate(), model
					.getEstimatedSeconds(), values);

			intent.putExtra("beginTime", values.getAsLong("dtstart"));
			intent.putExtra("endTime", values.getAsLong("dtend"));

			startActivity(intent);
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tagController.close();
		alertController.close();
	}

	/*
	 * ======================================================================
	 * ========================================== UI component helper classes
	 * ======================================================================
	 */

	/** Control set dealing with notification flags */
	public class NotifyFlagControlSet {
		private CheckBox before, during, after, nonstop;

		public NotifyFlagControlSet(int beforeId, int duringId, int afterId, int nonstopId) {
			before = (CheckBox) findViewById(beforeId);
			during = (CheckBox) findViewById(duringId);
			after = (CheckBox) findViewById(afterId);
			nonstop = (CheckBox) findViewById(nonstopId);
		}

		public void setValue(int flags) {
			before.setChecked((flags & TaskModelForEdit.NOTIFY_BEFORE_DEADLINE) > 0);
			during.setChecked((flags & TaskModelForEdit.NOTIFY_AT_DEADLINE) > 0);
			after.setChecked((flags & TaskModelForEdit.NOTIFY_AFTER_DEADLINE) > 0);
			nonstop.setChecked((flags & TaskModelForEdit.NOTIFY_NONSTOP) > 0);
		}

		public int getValue() {
			int value = 0;
			if (before.isChecked())
				value |= TaskModelForEdit.NOTIFY_BEFORE_DEADLINE;
			if (during.isChecked())
				value |= TaskModelForEdit.NOTIFY_AT_DEADLINE;
			if (after.isChecked())
				value |= TaskModelForEdit.NOTIFY_AFTER_DEADLINE;
			if (nonstop.isChecked())
				value |= TaskModelForEdit.NOTIFY_NONSTOP;
			return value;
		}
	}

	/** Control set dealing with importance */
	public class ImportanceControlSet {
		private List<CompoundButton> buttons = new LinkedList<CompoundButton>();

		public ImportanceControlSet(int containerId) {
			LinearLayout layout = (LinearLayout) findViewById(containerId);
			Resources r = getResources();

			for (Importance i : Importance.values()) {
				final ToggleButton button = new ToggleButton(TaskEdit.this);
				button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.WRAP_CONTENT, 1));
				button.setTextColor(r.getColor(i.getColorResource()));
				button.setTextOff(r.getString(i.getLabelResource()));
				button.setTextOn(r.getString(i.getLabelResource()));
				button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						setImportance((Importance) button.getTag());
					}
				});
				button.setTag(i);

				buttons.add(button);
				layout.addView(button);
			}
		}

		public void setImportance(Importance i) {
			for (CompoundButton b : buttons) {
				if (b.getTag() == i) {
					b.setTextSize(24);
					b.setChecked(true);
				} else {
					b.setTextSize(16);
					b.setChecked(false);
				}
			}
		}

		public Importance getImportance() {
			for (CompoundButton b : buttons)
				if (b.isChecked())
					return (Importance) b.getTag();
			return Importance.DEFAULT;
		}
	}

	/** Control set dealing with "blocking on" */
	public class BlockingOnControlSet {

		private CheckBox activatedCheckBox;
		private Spinner taskBox;

		public BlockingOnControlSet(int checkBoxId, int taskBoxId) {
			activatedCheckBox = (CheckBox) findViewById(checkBoxId);
			taskBox = (Spinner) findViewById(taskBoxId);

			Cursor tasks = controller.getActiveTaskListCursor();
			startManagingCursor(tasks);
			SimpleCursorAdapter tasksAdapter = new SimpleCursorAdapter(TaskEdit.this,
					android.R.layout.simple_list_item_1, tasks, new String[] { TaskModelForList.getNameField() },
					new int[] { android.R.id.text1 });
			taskBox.setAdapter(tasksAdapter);

			activatedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					taskBox.setEnabled(isChecked);
				}
			});

		}

		public void setBlockingOn(TaskIdentifier value) {
			activatedCheckBox.setChecked(value != null);
			if (value == null) {
				return;
			}

			for (int i = 0; i < taskBox.getCount(); i++)
				if (taskBox.getItemIdAtPosition(i) == value.getId()) {
					taskBox.setSelection(i);
					return;
				}

			// not found
			activatedCheckBox.setChecked(false);
		}

		public TaskIdentifier getBlockingOn() {
			if (!activatedCheckBox.isChecked())
				return null;

			return new TaskIdentifier(taskBox.getSelectedItemId());
		}
	}
}
