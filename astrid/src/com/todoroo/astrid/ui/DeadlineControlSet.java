package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.DateAndTimePicker.OnDateChangedListener;

public class DeadlineControlSet extends PopupControlSet {

    private final DateAndTimePicker dateAndTimePicker;
    private final TextView auxDisplay;

    public DeadlineControlSet(Activity activity, int viewLayout, int displayViewLayout, View extensionView, int auxDisplayId, int...dateShortcutViews) {
        super(activity, viewLayout, displayViewLayout, 0);

        dateAndTimePicker = (DateAndTimePicker) getView().findViewById(R.id.date_and_time);
        auxDisplay = (TextView) extensionView.findViewById(auxDisplayId);
        setUpListeners(dateShortcutViews);
    }

    private void setUpListeners(int[] dateShortcutViews) {

        dateAndTimePicker.setOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged() {
                refreshDisplayView();
            }
        });

        View.OnClickListener dateShortcutListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        };

        for (int i : dateShortcutViews) {
            View v = activity.findViewById(i);
            if (v != null)
                v.setOnClickListener(dateShortcutListener);
        }
    }

    @Override
    protected void refreshDisplayView() {
        TextView dateDisplay = (TextView) getDisplayView().findViewById(R.id.deadline_display);
        String toDisplay = dateAndTimePicker.getDisplayString(activity);
        dateDisplay.setText(toDisplay);
        auxDisplay.setText(toDisplay);
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
