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
import com.todoroo.astrid.helper.TaskEditControlSetBase;
import com.todoroo.astrid.ui.DateAndTimePicker;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.timelog.TimeLogService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimeLogControlSet extends TaskEditControlSetBase implements TimerActionControlSet.TimerActionListener {

    private final View view;

    @Override
    public int getIcon() {
        return -1;
    }

    public interface OnTimeSpentChangeListener {
        void timeSpentChanged(int fromInSeconds, int toInSeconds);
    }


    private ActivityPreferences preferences;

    private final TimeLogService timeLogService;
    private OnTimeSpentChangeListener timeSpentChangeListener;

    private LinearLayout timeLogContainer;

    private TextView timeSpentSumTextView;

    private int timeSpent = 0;

    public TimeLogControlSet(Activity activity, View view, ActivityPreferences preferences, TimeLogService timeLogService, OnTimeSpentChangeListener timeSpentChangeListener) {
        super(activity, -1);
        this.preferences = preferences;
        this.timeLogService = timeLogService;
        this.timeSpentChangeListener = timeSpentChangeListener;
        this.view = view;
        loadViewFields();
    }


    private void loadViewFields() {
        timeLogContainer = (LinearLayout) view.findViewById(R.id.time_log_container);
        timeSpentSumTextView = (TextView) view.findViewById(R.id.taskTimeLog_sum_value);
        View timeLogAddButton = view.findViewById(R.id.taskTimeLog_add);
        if (timeLogAddButton != null) {
            timeLogAddButton.setOnClickListener(new AddTimeLogOnClickListener());
        }
    }

    @Override
    protected void readFromTaskOnInitialize() {
        reloadTimeLogs();
    }

    private void reloadTimeLogs() {
        timeLogContainer.removeAllViews();
        timeSpent = 0;
        timeLogService.getTimeLogs(model.getId(), new Callback<TaskTimeLog>() {
            @Override
            public void apply(TaskTimeLog timeLog) {
                addTimeLog(timeLog, false);
                changeTimeSpent(timeLog.getTimeSpent(), false);
            }
        });
    }

    /**
     * @param timeLog
     * @return added row with provided time log
     */
    private LinearLayout addTimeLog(final TaskTimeLog timeLog, boolean asFirst) {
        final LinearLayout timeLogRow = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.control_set_time_log_row, null);
        if (asFirst) {
            timeLogContainer.addView(timeLogRow, 0);
        } else {
            timeLogContainer.addView(timeLogRow);
        }

        fillView(timeLog, timeLogRow);

        timeLogRow.setTag(timeLog);

        timeLogRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddTimeLogTimePickerDialog dialog = new AddTimeLogTimePickerDialog(preferences, activity, new MyTimeLogPickedListener(timeLogRow, timeLog), activity.getString(R.string.TEA_timer_title_timeSpentHoursMinutes), timeLog.getTimeSpent(), timeLog.getTime(), timeLog.getDescription());
                dialog.show();
            }
        });
        return timeLogRow;
    }

    private void fillView(TaskTimeLog timeLog, LinearLayout timeLogRow) {
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
        if (changeInSeconds == 0)
            return;

        int oldTimeSpent = timeSpent;
        timeSpent += changeInSeconds;
        timeSpentSumTextView.setText(DateUtils.formatElapsedTime(timeSpent));
        notifyIfNeeded(notify, oldTimeSpent);
    }

    private void notifyIfNeeded(boolean notify, int oldTimeSpent) {
        if (notify && timeSpentChangeListener != null) {
            timeSpentChangeListener.timeSpentChanged(oldTimeSpent, timeSpent);
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

    private void sortTimeLogs() {
        List<View> children = new ArrayList<>();
        for (int i = 0; i < timeLogContainer.getChildCount(); i++) {
            children.add(timeLogContainer.getChildAt(i));
        }
        timeLogContainer.removeAllViews();
        Collections.sort(children, new TimeLogRowsByTimeReverseComparator());
        for (View child : children) {
            timeLogContainer.addView(child);
        }

    }

    @Override
    protected void afterInflate() {
        reloadTimeLogs();
    }

    @Override
    public View getView() {
        return view;
    }

    public int getTimeSpent() {
        return timeSpent;
    }

    @Override
    public void timerStopped(Task task, TaskTimeLog timeLog) {
        addTimeLog(timeLog, true);
        changeTimeSpent(timeLog.getTimeSpent(), false);
    }

    @Override
    public void timerStarted(Task task) {

    }

    private static class TimeLogRowsByTimeReverseComparator implements Comparator<View> {
        @Override
        public int compare(View lhs, View rhs) {
            return -(getTime(lhs).compareTo(getTime(rhs)));
        }

        public Long getTime(View lhs) {
            return ((TaskTimeLog) lhs.getTag()).getTime();
        }
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
            Long oldTime = timeLog.getTime();
            changeTimeSpent(timeSpentInSeconds - timeLog.getTimeSpent(), true);
            timeLog.setTimeSpent(timeSpentInSeconds);
            timeLog.setDescription(description);
            if (oldTime != time.getTime()) {
                timeLog.setTime(time.getTime());
                sortTimeLogs();
            }
            fillView(timeLog, timeLogRow);
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

    private class AddTimeLogOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            TaskTimeLog timeLog = createTimeLog();
            LinearLayout timeLogRow = addTimeLog(timeLog, true);
            timeLogRow.performClick();
        }
    }

    private TaskTimeLog createTimeLog() {
        TaskTimeLog timeLog = new TaskTimeLog();
        timeLog.setTime(DateUtilities.now());
        timeLog.setTimeSpent(0);
        timeLog.setDescription("");
        timeLog.setTaskId(model.getId());
        return timeLog;
    }
}
