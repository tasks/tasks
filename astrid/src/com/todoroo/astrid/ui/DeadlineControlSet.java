package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;

public class DeadlineControlSet extends PopupControlSet {

    private final DateAndTimePicker dateAndTimePicker;

    public DeadlineControlSet(Activity activity, int viewLayout, int displayViewLayout, View...extraViews) {
        super(activity, viewLayout, displayViewLayout, 0);

        dateAndTimePicker = (DateAndTimePicker) getView().findViewById(R.id.date_and_time);
        LinearLayout extras = (LinearLayout) getView().findViewById(R.id.datetime_extras);
        for (View v : extraViews) {
            LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
            extras.addView(v, lp);
        }

        Button okButton = (Button) LayoutInflater.from(activity).inflate(R.layout.control_dialog_ok, null);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
                DialogUtilities.dismissDialog(DeadlineControlSet.this.activity, DeadlineControlSet.this.dialog);
            }
        });
        LinearLayout body = (LinearLayout) getView().findViewById(R.id.datetime_body);
        body.addView(okButton);
    }

    @Override
    protected void refreshDisplayView() {
        TextView dateDisplay = (TextView) getDisplayView().findViewById(R.id.deadline_display);
        String toDisplay = dateAndTimePicker.getDisplayString(activity);
        dateDisplay.setText(toDisplay);
    }

    @Override
    public void readFromTask(Task task) {
        long dueDate = task.getValue(Task.DUE_DATE);
        dateAndTimePicker.initializeWithDate(dueDate);
        refreshDisplayView();
    }

    @Override
    public String writeToModel(Task task) {
        long dueDate = dateAndTimePicker.constructDueDate();
        task.setValue(Task.DUE_DATE, dueDate);
        return null;
    }
}
