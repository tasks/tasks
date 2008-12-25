/*
 * ASTRID: Android's Simple Task Recording Dame
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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.timsu.astrid.R;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;

public class TaskEdit extends TaskModificationActivity<TaskModelForEdit> {
    private static final int       SAVE_ID    = Menu.FIRST;
    private static final int       DISCARD_ID = Menu.FIRST + 1;
    private static final int       DELETE_ID  = Menu.FIRST + 2;

    private EditText               name;
    private Spinner                importance;
    private TimeDurationControlSet estimatedDuration;
    private TimeDurationControlSet elapsedDuration;
    private DateControlSet         definiteDueDate;
    private DateControlSet         preferredDueDate;
    private DateControlSet         hiddenUntil;
    private EditText               notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_edit);

        setUpUIComponents();
		setUpListeners();
        populateFields();
    }

    @Override
    protected TaskModelForEdit getModel(TaskIdentifier identifier) {
        if (identifier != null)
            return controller.fetchTaskForEdit(identifier);
        else
            return controller.createNewTaskForEdit();
    }

    // --- data saving and retrieving

    private void populateFields() {
        Resources r = getResources();
        if(model.getCursor() != null)
            startManagingCursor(model.getCursor());

        name.setText(model.getName());
        if(model.getName().length() > 0)
            setTitle(new StringBuilder().
                append(r.getString(R.string.taskEdit_titlePrefix)).
                append(" ").
                append(model.getName()));

        estimatedDuration.setTimeElapsed(model.getEstimatedSeconds());
        elapsedDuration.setTimeElapsed(model.getElapsedSeconds());
        importance.setSelection(model.getImportance().ordinal());

        definiteDueDate.setDate(model.getDefiniteDueDate());
        preferredDueDate.setDate(model.getPreferredDueDate());
        hiddenUntil.setDate(model.getHiddenUntil());

        notes.setText(model.getNotes());
    }

    private void save() {
        model.setName(name.getText().toString());
        model.setEstimatedSeconds(estimatedDuration.getTimeDurationInSeconds());
        model.setElapsedSeconds(elapsedDuration.getTimeDurationInSeconds());
        model.setImportance(Importance.values()[importance.getSelectedItemPosition()]);

        model.setDefiniteDueDate(definiteDueDate.getDate());
        model.setPreferredDueDate(preferredDueDate.getDate());
        model.setHiddenUntil(hiddenUntil.getDate());

        model.setNotes(notes.getText().toString());

        try {
            if(!controller.saveTask(model))
                throw new RuntimeException("Unable to save task: false");
        } catch (RuntimeException e) {
            Log.e(getClass().getSimpleName(), "Error saving task!", e);
        }
    }

    // --- user interface components

    private void setUpUIComponents() {
        Resources r = getResources();
        setTitle(new StringBuilder()
            .append(r.getString(R.string.app_name))
            .append(": ")
            .append(r.getString(R.string.taskEdit_titleGeneric)));

        name = (EditText)findViewById(R.id.name);
        importance = (Spinner)findViewById(R.id.importance);

        estimatedDuration = new TimeDurationControlSet(R.id.estimatedDuration);
        elapsedDuration = new TimeDurationControlSet(R.id.elapsedDuration);
        definiteDueDate = new DateControlSet(R.id.definiteDueDate_notnull,
                R.id.definiteDueDate_date, R.id.definiteDueDate_time);
        preferredDueDate = new DateControlSet(R.id.preferredDueDate_notnull,
                R.id.preferredDueDate_date, R.id.preferredDueDate_time);
        hiddenUntil = new DateControlSet(R.id.hiddenUntil_notnull,
                R.id.hiddenUntil_date, R.id.hiddenUntil_time);

        notes = (EditText)findViewById(R.id.notes);

        // set up for each field

        ImportanceAdapter importanceAdapter = new ImportanceAdapter(this,
                    android.R.layout.simple_spinner_item,
                    android.R.layout.simple_spinner_dropdown_item,
                    Importance.values());
        importance.setAdapter(importanceAdapter);
    }

    /** Display importance with proper formatting */
    private class ImportanceAdapter extends ArrayAdapter<Importance> {
        private int textViewResourceId, dropDownResourceId;
        private LayoutInflater inflater;

        public ImportanceAdapter(Context context, int textViewResourceId,
                int dropDownResourceId, Importance[] objects) {
            super(context, textViewResourceId, objects);

            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.textViewResourceId = textViewResourceId;
            this.dropDownResourceId = dropDownResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, textViewResourceId, true);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent, dropDownResourceId, true);
        }

        public View getView(int position, View convertView, ViewGroup parent,
                int resource, boolean setColors) {
            View view;
            TextView text;
            Resources r = getResources();

            if (convertView == null) {
                view = inflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            try {
                text = (TextView) view;
            } catch (ClassCastException e) {
                Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
                throw new IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView", e);
            }

            text.setText(r.getString(getItem(position).getLabelResource()));
            if(setColors)
                text.setBackgroundColor(r.getColor(getItem(position).getColorResource()));

            return view;
        }
    }

    /** Set up button listeners */
    private void setUpListeners() {

        Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                saveButtonClick();
            }
        });

        Button discardButton = (Button) findViewById(R.id.discard);
        discardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                discardButtonClick();
            }
        });

        Button deleteButton = (Button) findViewById(R.id.delete);
        if(model.getTaskIdentifier() == null)
            deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                deleteButtonClick();
            }
        });
    }

    private void saveButtonClick() {
        save();
        setResult(RESULT_OK);
        finish();
    }

    private void discardButtonClick() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void deleteButtonClick() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_task_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    controller.deleteTask(model.getTaskIdentifier());
                    setResult(RESULT_OK);
                    finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
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

        item = menu.add(Menu.NONE, DISCARD_ID, 0, R.string.delete_label);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setAlphabeticShortcut('d');

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    // --- date/time methods and helper classes

    private class TimeDurationControlSet implements OnNumberPickedListener,
            View.OnClickListener {
        private Button timeButton;
        private int timeDuration;
        private final NumberPickerDialog dialog =
            new NumberPickerDialog(TaskEdit.this, this,
                    getResources().getString(R.string.minutes_dialog),
                    0, 5, 0, 999);

        public TimeDurationControlSet(int timeButtonId) {
            timeButton = (Button)findViewById(timeButtonId);
            timeButton.setOnClickListener(this);
        }

        public int getTimeDurationInSeconds() {
            return timeDuration;
        }

        public void setTimeElapsed(Integer timeDurationInSeconds) {
            if(timeDurationInSeconds == null)
                timeDurationInSeconds = 0;

            timeDuration = timeDurationInSeconds;

            Resources r = getResources();
            if(timeDurationInSeconds == 0) {
                timeButton.setText(r.getString(R.string.blank_button_title));
                return;
            }

            timeButton.setText(DateUtilities.getDurationString(r,
                    timeDurationInSeconds, 2));
            dialog.setInitialValue(timeDuration/60);
        }

        @Override
        /** Called when NumberPicker activity is completed */
        public void onNumberPicked(NumberPicker view, int value) {
            setTimeElapsed(value * 60);
        }

        /** Called when time button is clicked */
        public void onClick(View v) {
            dialog.show();
        }


    }

    private static final Format dateFormatter = new SimpleDateFormat("EEE, MMM d, yyyy");
    private static final Format timeFormatter = new SimpleDateFormat("h:mm a");

    private class DateControlSet implements OnTimeSetListener,
            OnDateSetListener, View.OnClickListener {
        private CheckBox activatedCheckBox;
        private Button dateButton;
        private Button timeButton;
        private Date date;

        public DateControlSet(int checkBoxId, int dateButtonId, int timeButtonId) {
            activatedCheckBox = (CheckBox)findViewById(checkBoxId);
            dateButton = (Button)findViewById(dateButtonId);
            timeButton = (Button)findViewById(timeButtonId);

            activatedCheckBox.setOnCheckedChangeListener(
                    new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    dateButton.setEnabled(isChecked);
                    timeButton.setEnabled(isChecked);
                }
            });
            dateButton.setOnClickListener(this);
            timeButton.setOnClickListener(this);
        }

        public Date getDate() {
            if(!activatedCheckBox.isChecked())
                return null;
            return date;
        }

        /** Initialize the components for the given date field */
        public void setDate(Date newDate) {
            this.date = newDate;
            if(newDate == null) {
                date = new Date();
                date.setMinutes(0);
            }

            activatedCheckBox.setChecked(newDate != null);
            dateButton.setEnabled(newDate != null);
            timeButton.setEnabled(newDate != null);

            updateDate();
            updateTime();
        }

        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            date.setYear(year - 1900);
            date.setMonth(month);
            date.setDate(monthDay);
            updateDate();
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            date.setHours(hourOfDay);
            date.setMinutes(minute);
            updateTime();
        }

        public void updateDate() {
            dateButton.setText(dateFormatter.format(date));

        }

        public void updateTime() {
            timeButton.setText(timeFormatter.format(date));
        }

        public void onClick(View v) {
            if(v == timeButton)
                new TimePickerDialog(TaskEdit.this, this, date.getHours(),
                    date.getMinutes(), false).show();
            else
                new DatePickerDialog(TaskEdit.this, this, 1900 +
                        date.getYear(), date.getMonth(), date.getDate()).show();
        }
    }
}
