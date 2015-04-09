/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

public class DeadlineControlSet extends PopupControlSet {

    private DateAndTimePicker dateAndTimePicker;

    public DeadlineControlSet(ActivityPreferences preferences, Activity activity) {
        super(preferences, activity, R.layout.control_set_deadline_dialog, R.layout.control_set_deadline, 0);
    }

    @Override
    protected void refreshDisplayView() {
        StringBuilder displayString = new StringBuilder();
        boolean isOverdue;
        if (initialized) {
            isOverdue = !dateAndTimePicker.isAfterNow();
            displayString.append(dateAndTimePicker.getDisplayString(activity));
        } else {
            isOverdue = model.getDueDate() < DateUtilities.now();
            displayString.append(DateAndTimePicker.getDisplayString(activity, model.getDueDate(), false, false));
        }

        TextView dateDisplay = (TextView) getView().findViewById(R.id.display_row_edit);
        if (TextUtils.isEmpty(displayString)) {
            dateDisplay.setText(R.string.TEA_deadline_hint);
            dateDisplay.setTextColor(unsetColor);
        } else {
            dateDisplay.setText(displayString);
            if (isOverdue) {
                dateDisplay.setTextColor(activity.getResources().getColor(R.color.red_theme_color));
            } else {
                dateDisplay.setTextColor(themeColor);
            }
        }
    }

    @Override
    protected void afterInflate() {
        dateAndTimePicker = (DateAndTimePicker) getDialogView().findViewById(R.id.date_and_time);
        LinearLayout body = (LinearLayout) getDialogView().findViewById(R.id.datetime_body);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Button okButton = (Button) LayoutInflater.from(activity).inflate(R.layout.control_dialog_ok, null);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int) (45 * metrics.density));
        body.addView(okButton, params);
    }

    @Override
    protected void setupOkButton(View view) {
        super.setupOkButton(view);
        Button okButton = (Button) view.findViewById(R.id.edit_dlg_ok);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
                DialogUtilities.dismissDialog(DeadlineControlSet.this.activity, DeadlineControlSet.this.dialog);
            }
        });
    }

    @Override
    protected void readFromTaskOnInitialize() {
        long dueDate = model.getDueDate();
        initializeWithDate(dueDate);
        refreshDisplayView();
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        long dueDate = dateAndTimePicker.constructDueDate();
        if (dueDate != task.getDueDate()) // Clear snooze if due date has changed
        {
            task.setReminderSnooze(0L);
        }
        task.setDueDate(dueDate);
    }

    private void initializeWithDate(long dueDate) {
        dateAndTimePicker.initializeWithDate(dueDate);
    }

    @Override
    public int getIcon() {
        return R.attr.ic_action_clock;
    }
}
