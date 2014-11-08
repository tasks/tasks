package com.todoroo.astrid.timers;


import android.app.Activity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskTimeLog;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.ui.DateAndTimePicker;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.timelog.TimeLogService;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class TimeLogControlSet extends TaskEditControlSet implements TimerActionControlSet.TimerActionListener{

    public interface OnTimeSpentChangeListener {
        void timeSpentChanged(int fromInSeconds, int toInSeconds);
    }


    private ActivityPreferences preferences;

    private final TimeLogService timeLogService;
    private OnTimeSpentChangeListener timeSpentChangeListener;

    private LinearLayout timeLogContainer;

    private TextView timeSpentSumTextView;

    private int timeSpent = 0;

    public TimeLogControlSet(ActivityPreferences preferences, TimeLogService timeLogService, Activity activity, OnTimeSpentChangeListener timeSpentChangeListener) {
        super(activity, R.layout.control_set_time_log);
        this.preferences = preferences;
        this.timeLogService = timeLogService;
        this.timeSpentChangeListener = timeSpentChangeListener;
    }


    @Override
    protected void readFromTaskOnInitialize() {
        timeLogContainer.removeAllViews();
        timeSpent = 0;
        timeLogService.getTimeLogs(model.getId(), new Callback<TaskTimeLog>() {
            @Override
            public void apply(TaskTimeLog timeLog) {
                addTimeLog(timeLog);
                changeTimeSpent(timeLog.getTimeSpent(), false);
            }
        });

    }

    /**
     * @param timeLog
     * @return added row with provided time log
     */
    private LinearLayout addTimeLog(final TaskTimeLog timeLog) {
        final LinearLayout timeLogRow = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.control_set_time_log_row, null);
        timeLogContainer.addView(timeLogRow, 0);

        showInView(timeLog, timeLogRow);

        timeLogRow.setTag(timeLog);

        timeLogRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO change string
                AddTimeLogTimePickerDialog dialog = new AddTimeLogTimePickerDialog(preferences, activity, new MyTimeLogPickedListener(timeLogRow, timeLog), activity.getString(R.string.TEA_timer_title_timeSpentHoursMinutes), timeLog.getTimeSpent(), timeLog.getTime(), timeLog.getDescription());
                dialog.show();
            }
        });
        return timeLogRow;
    }

    private void showInView(TaskTimeLog timeLog, LinearLayout timeLogRow) {
        TextView descriptionView = (TextView) timeLogRow.findViewById(R.id.timeLogRow_description);
        String description = timeLog.getDescription();
        if (TextUtils.isEmpty(description)) {
            description = DateAndTimePicker.getDisplayString(activity, timeLog.getTime());
        }
        descriptionView.setText(description);
        TextView timeSpentView = (TextView) timeLogRow.findViewById(R.id.timeLogRow_timeSpent);
        timeSpentView.setText(DateUtils.formatElapsedTime(timeLog.getTimeSpent()));


    }

    /**
     * @param changeInSeconds positive value if time spent increased, negative otherwise
     */
    private void changeTimeSpent(int changeInSeconds, boolean notify) {
        if (changeInSeconds != 0) {
            int oldTimeSpent = timeSpent;
            timeSpent += changeInSeconds;
            timeSpentSumTextView.setText(DateUtils.formatElapsedTime(timeSpent));
            if (notify && timeSpentChangeListener != null) {
                timeSpentChangeListener.timeSpentChanged(oldTimeSpent, timeSpent);
            }
        }
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        Set<TaskTimeLog> timeLogs = getTaskTimeLogs();
        if (timeLogService.synchronizeTimeLogs(model.getId(), timeLogs)) {
            task.setModificationDate(DateUtilities.now());
        }
    }

    private Set<TaskTimeLog> getTaskTimeLogs() {
        Set<TaskTimeLog> timeLogs = new HashSet<>();
        for (int i = 0; i < timeLogContainer.getChildCount(); i++) {
            TaskTimeLog timeLog = (TaskTimeLog) timeLogContainer.getChildAt(i).getTag();
            timeLogs.add(timeLog);
        }
        return timeLogs;
    }

    @Override
    protected void afterInflate() {
        timeLogContainer = (LinearLayout) getView().findViewById(R.id.time_log_container);
        timeSpentSumTextView = (TextView) getView().findViewById(R.id.taskTimeLog_sum_value);
        View timeLogAddButton = getView().findViewById(R.id.taskTimeLog_add);
        timeLogAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskTimeLog timeLog = new TaskTimeLog();
                timeLog.setTime(DateUtilities.now());
                timeLog.setTimeSpent(0);
                timeLog.setDescription("");
                timeLog.setTaskId(model.getId());
                LinearLayout timeLogRow = addTimeLog(timeLog);
                timeLogRow.performClick();
            }
        });

    }

    public int getTimeSpent() {
        return timeSpent;
    }

    @Override
    public void timerStopped(Task task, TaskTimeLog timeLog) {
        addTimeLog(timeLog);
        changeTimeSpent(timeLog.getTimeSpent(), false);
    }

    @Override
    public void timerStarted(Task task) {

    }


    private class MyTimeLogPickedListener implements AddTimeLogTimePickerDialog.OnTimeLogPickedListener {

        private final LinearLayout timeLogRow;
        private final TaskTimeLog timeLog;

        public MyTimeLogPickedListener(LinearLayout timeLogRow, TaskTimeLog timeLog) {
            this.timeLogRow = timeLogRow;
            this.timeLog = timeLog;
        }

        @Override
        public void onTimeLogChanged(int timeSpentInSeconds, Date time, String description) {
            changeTimeSpent(timeSpentInSeconds - timeLog.getTimeSpent(), true);
            timeLog.setTimeSpent(timeSpentInSeconds);
            timeLog.setDescription(description);
            timeLog.setTime(time.getTime());
            showInView(timeLog, timeLogRow);
        }

        @Override
        public void onTimeLogDeleted() {
            removeTimeLog();
            changeTimeSpent(-timeLog.getTimeSpent(), true);
        }

        @Override
        public void onCancel() {
            //TODO jak traktować wpisy z zerowym czasem
            if (timeLog.getTimeSpent() == 0) {
                removeTimeLog();
            } else {
                //do nothing
            }
        }

        private void removeTimeLog() {
            timeLogContainer.removeView(timeLogRow);
        }
    }
}
