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
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForView;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;


/** Task Properties view for the Astrid Application.
 *
 * @author Tim Su (timsu@stanfordalumni.org)
 *
 */
public class TaskView extends TaskModificationActivity<TaskModelForView> {

    // activities
    private static final int   ACTIVITY_EDIT           = 0;

    // menu codes
    private static final int   EDIT_ID                 = Menu.FIRST;
    private static final int   DELETE_ID               = Menu.FIRST + 1;

    // UI components
    private TextView           name;
    private TextView           elapsed;
    private TextView           estimated;
    private TextView           definiteDueDate;
    private TextView           preferredDueDate;
    private TextView           creationDate;
    private TextView           notes;
    private Button             timerButton;
    private Button             progress;
    private NumberPickerDialog progressDialog;

    // other instance variables
    private Handler            handler;
    private Timer              updateTimer             = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.task_view);
        handler = new Handler();

        setUpUIComponents();
        setUpListeners();
        populateFields();
    }

    @Override
    protected TaskModelForView getModel(TaskIdentifier identifier) {
        if(identifier == null)
            throw new IllegalArgumentException("Can't view null task!");
        return controller.fetchTaskForView(this, identifier);
    }

    /* ======================================================================
     * =================================================== reading from model
     * ====================================================================== */

    private void populateFields() {
        final Resources r = getResources();
        name.setText(model.getName());
        estimated.setText(DateUtilities.getDurationString(r,
                model.getEstimatedSeconds(), 2));
        elapsed.setText(DateUtilities.getDurationString(r,
                model.getElapsedSeconds(), 2));

        formatDate(model.getCreationDate(), creationDate);

        updateTimerButtonText();
        updateProgressComponents();

        if(model.getNotes().length() == 0)
            ((View)notes.getParent()).setVisibility(View.GONE);
        else
            notes.setText(model.getNotes());
    }

    private void formatDeadline(Date deadline, TextView view) {
        Resources r = getResources();
        if(deadline == null || model.isTaskCompleted()) {
            ((View)view.getParent()).setVisibility(View.GONE);
            return;
        }

        int secondsToDeadline = (int) ((deadline.getTime() -
                System.currentTimeMillis())/1000);
        String text = DateUtilities.getDurationString(r,
                Math.abs(secondsToDeadline), 2) + " ";
        if(secondsToDeadline < 0) {
            view.setTextColor(r.getColor(R.color.view_table_overdue));
            view.setText(text + r.getString(R.string.overdue_suffix));
        } else
            view.setText(text);
    }

    private void formatDate(Date date, TextView view) {
        Resources r = getResources();
        if(date == null || model.isTaskCompleted()) {
            ((View)view.getParent()).setVisibility(View.GONE);
            return;
        }

        int secondsAgo = (int) ((System.currentTimeMillis() - date.getTime())/1000);
        String text = DateUtilities.getDurationString(r,
                Math.abs(secondsAgo), 2) + " ";
        view.setText(text + r.getString(R.string.ago_suffix));
    }

    /* ======================================================================
     * ==================================================== UI initialization
     * ====================================================================== */

    private void setUpUIComponents() {
        Resources r = getResources();

        name = (TextView)findViewById(R.id.name);
        elapsed = (TextView)findViewById(R.id.cell_elapsed);
        estimated = (TextView)findViewById(R.id.cell_estimated);
        definiteDueDate = (TextView)findViewById(R.id.cell_definiteDueDate);
        preferredDueDate = (TextView)findViewById(R.id.cell_preferredDueDate);
        creationDate = (TextView)findViewById(R.id.cell_creationDate);
        notes = (TextView)findViewById(R.id.cell_notes);
        timerButton = (Button)findViewById(R.id.timerButton);
        progress = (Button)findViewById(R.id.progress);

        progressDialog = new NumberPickerDialog(this,
                new NumberPickerDialog.OnNumberPickedListener() {
            @Override
            public void onNumberPicked(NumberPicker view, int number) {
                model.setProgressPercentage(number);
                controller.saveTask(model);
                updateProgressComponents();
            }
        }, r.getString(R.string.progress_dialog), 0, 25, 0, 100);

        name.setTextSize(36);
    }

    private void setUpListeners() {
        Button edit = (Button)findViewById(R.id.edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editButtonClick();
            }
        });

        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model.getTimerStart() == null) {
                    model.setTimerStart(new Date());
                    controller.saveTask(model);
                } else {
                    model.stopTimerAndUpdateElapsedTime();
                    controller.saveTask(model);
                }

                updateTimerButtonText();
            }
        });
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;

        item = menu.add(Menu.NONE, EDIT_ID, 0, R.string.edit_label);
        item.setIcon(android.R.drawable.ic_menu_edit);
        item.setAlphabeticShortcut('s');

        item = menu.add(Menu.NONE, DELETE_ID, 0, R.string.delete_label);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setAlphabeticShortcut('d');

        return true;
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus && MainActivity.shouldCloseInstance) { // user wants to quit
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        switch(resultCode) {
        case Constants.RESULT_GO_HOME:
        case RESULT_CANCELED:
            setResult(resultCode);
            finish();
            break;

        default:
            populateFields();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateTimer.cancel(); // stop the timer
        updateTimer = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();

        if(updateTimer != null)
            return;

        updateTimer = new Timer(); // start timer
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateElapsedTimeText();
                    }
                });
            }
        }, 0, 1000);
    }

    private void editButtonClick() {
        Intent intent = new Intent(TaskView.this, TaskEdit.class);
        intent.putExtra(TaskEdit.LOAD_INSTANCE_TOKEN,
                model.getTaskIdentifier().getId());
        startActivityForResult(intent, ACTIVITY_EDIT);
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
        case EDIT_ID:
            editButtonClick();
            return true;
        case DELETE_ID:
            deleteButtonClick();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    /* ======================================================================
     * ================================================ UI component updating
     * ====================================================================== */

    /** Update components that depend on elapsed time */
    private void updateElapsedTimeText() {
        Resources r = getResources();
        int timeElapsed = model.getElapsedSeconds();
        if(model.getTimerStart() != null) {
            timeElapsed += (int) (System.currentTimeMillis() -
                    model.getTimerStart().getTime())/1000;
        }

        elapsed.setText(DateUtilities.getDurationString(r,
                timeElapsed, Integer.MAX_VALUE));
    }

    /** Update components that depend on timer status */
    private void updateTimerButtonText() {
        Resources r = getResources();
        if(model.getTimerStart() == null)
            timerButton.setText(r.getString(R.string.startTimer_label));
        else
            timerButton.setText(r.getString(R.string.stopTimer_label));
    }

    /** Update components that depend on task progress */
    private void updateProgressComponents() {
        Resources r = getResources();
        progress.setText(model.getProgressPercentage() +
                r.getString(R.string.progress_suffix));

        if(model.isTaskCompleted())
            name.setBackgroundColor(r.getColor(R.color.view_header_done));
        else
            name.setBackgroundColor(r.getColor(model.getImportance().getColorResource()));

        progressDialog.setInitialValue(model.getProgressPercentage());

        formatDeadline(model.getDefiniteDueDate(), definiteDueDate);
        formatDeadline(model.getPreferredDueDate(), preferredDueDate);
    }
}

