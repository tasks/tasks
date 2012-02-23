package com.todoroo.astrid.taskrabbit;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.TaskRabbitSetListener;
import com.todoroo.astrid.ui.DateAndTimePicker;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitDeadlineControlSet extends PopupControlSet implements TaskRabbitSetListener{

    private final DateAndTimePicker dateAndTimePicker;

    public TaskRabbitDeadlineControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, 0);

        dateAndTimePicker = (DateAndTimePicker) getView().findViewById(R.id.date_and_time);
        this.displayText.setText(activity.getString(R.string.TEA_when_header_label));

        Button okButton = (Button) LayoutInflater.from(activity).inflate(R.layout.control_dialog_ok, null);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
                DialogUtilities.dismissDialog(TaskRabbitDeadlineControlSet.this.activity, TaskRabbitDeadlineControlSet.this.dialog);
            }
        });
        LinearLayout body = (LinearLayout) getView().findViewById(R.id.datetime_body);
        body.addView(okButton);
    }

    @Override
    protected void refreshDisplayView() {
        TextView dateDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        String toDisplay = dateAndTimePicker.getDisplayString(activity, false, false);
        if (dateDisplay != null)
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
        return null;
    }

    @Override
    public void readFromModel(JSONObject json, String key, int mode) {

        long dateValue = json.optLong(key);
        Date currentDate = null;
        if (dateValue < 24) {
            if(dateAndTimePicker.constructDueDate() > 0) return;
            currentDate = new Date();
            currentDate.setHours((int)dateValue);
            currentDate.setMinutes(0);
            currentDate.setSeconds(0);
        }
        else {
            currentDate = new Date(dateValue);
        }

        dateAndTimePicker.initializeWithDate(DateUtilities.dateToUnixtime(currentDate)+ DateUtilities.ONE_DAY);
        refreshDisplayView();
    }

    @Override
    protected void afterInflate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected void readFromTaskOnInitialize() {
     // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing, we don't lazy load this control set yet
        return null;
    }


    @Override
    public void saveToDatabase(JSONObject json, String key) throws JSONException {
        json.put(key, dateAndTimePicker.constructDueDate()/1000);

    }

    @Override
    public void postToTaskRabbit(JSONObject json, String key) throws JSONException {
        long dueDate = dateAndTimePicker.constructDueDate()/1000;
        json.put(key, dueDate);
    }

}
