package com.todoroo.astrid.timers;


import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

import org.tasks.R;
import org.tasks.timelog.TimeLogService;

public class TimeLogControlSet extends TaskEditControlSet{

    private final TimeLogService timeLogService;

    private LinearLayout timeLogContainer;

    public TimeLogControlSet(TimeLogService timeLogService, Activity activity) {
        super(activity, R.layout.control_set_time_log);
        this.timeLogService = timeLogService;

    }

    @Override
    protected void readFromTaskOnInitialize() {
        //TODO read list of time logs from database

    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {

        //TODO write the list of time logs to model
    }

    @Override
    protected void afterInflate() {
        timeLogContainer = (LinearLayout) getView().findViewById(R.id.time_log_container);
        getView().findViewById(R.id.time_log_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO show popup
            }
        });

    }

    private void addTask(){

    }

    public static class AddTimeLogControlSet extends TimeDurationControlSet {

        public AddTimeLogControlSet(Activity activity, View view, Property.IntegerProperty property, int layoutId, int prefixResource, int titleResource, int labelId) {
            super(activity, view, property, layoutId, labelId);
        }
    }
}
