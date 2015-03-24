/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Activity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskTimeLog;
import com.todoroo.astrid.helper.TaskEditControlSetBase;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.timelog.TimeLogService;

import java.util.ArrayList;
import java.util.List;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet extends PopupControlSet implements TimerActionListener, TimeLogControlSet.OnTimeSpentChangeListener {

    TimeDurationTaskEditControlSet estimated, remaining;
    private final TextView displayEdit;

    private TimeLogControlSet timeLogControlSet;
    private TimeLogService timeLogService;

    public TimerControlSet(ActivityPreferences preferences, final Activity activity, int viewLayout, int displayViewLayout, int title, TimeLogService timeLogService) {
        super(preferences, activity, viewLayout, displayViewLayout, title);
        this.timeLogService = timeLogService;

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(R.string.TEA_timer_controls);
        displayEdit.setTextColor(unsetColor);

        estimated = new TimeDurationTaskEditControlSet(activity, getView(), Task.ESTIMATED_SECONDS, R.id.estimatedDurationLayout, R.string.TEA_estimatedDuration_label);
        remaining = new TimeDurationTaskEditControlSet(activity, getView(), Task.REMAINING_SECONDS, R.id.remainingDurationLayout, R.string.TEA_remainingDuration_label);
        estimated.setTimeDurationChangeListener(new EstimatedTimeListener());
    }

    @Override
    protected void readFromTaskOnInitialize() {
        estimated.readFromTask(model);
        remaining.readFromTask(model);
        timeLogControlSet.readFromTask(model);
    }

    @Override
    protected void afterInflate() {
        timeLogControlSet = new TimeLogControlSet(preferences, timeLogService, activity, this);
        LinearLayout mainLayout = (LinearLayout) getView().findViewById(R.id.timers_mainLayout);
        mainLayout.addView(timeLogControlSet.getView());
        // Nothing to do here
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if (initialized) {
            estimated.writeToModel(task);
//            elapsed.writeToModel(task);
            remaining.writeToModel(task);
            timeLogControlSet.writeToModel(task);
        }
    }

    @Override
    public void timeSpentChanged(int fromInSeconds, int toInSeconds) {
        model.setElapsedSeconds((int) toInSeconds);
        model.lowerRemainingSeconds(toInSeconds - fromInSeconds);
        remaining.readFromTaskOnInitialize();
    }

    private void setRemainingTime(int remainingSeconds) {
        model.setRemainingSeconds(remainingSeconds);
        remaining.readFromTaskOnInitialize();
    }

    // --- TimeDurationTaskEditControlSet

    /**
     * Control set for mapping a Property to a TimeDurationControlSet
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TimeDurationTaskEditControlSet extends TaskEditControlSetBase {
        private final TimeDurationControlSet controlSet;
        private final IntegerProperty property;

        public TimeDurationTaskEditControlSet(Activity activity, View v, IntegerProperty property, int timeButtonId, int labelId) {
            super(activity, -1);
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity, v, property, timeButtonId, labelId);
        }

        @Override
        public void readFromTaskOnInitialize() {
            controlSet.setModel(model);
            controlSet.setTimeDuration(model.getValue(property));
        }

        @Override
        protected void afterInflate() {
            // Nothing
        }

        @Override
        protected void writeToModelAfterInitialized(Task task) {
            task.setValue(property, getTimeDurationInSeconds());
        }

        public int getTimeDurationInSeconds() {
            return controlSet.getTimeDurationInSeconds();
        }

        public void setTimeDurationChangeListener(TimeDurationControlSet.TimeDurationChangeListener timeDurationChangeListener) {
            controlSet.setTimeDurationChangeListener(timeDurationChangeListener);
        }
    }

    @Override
    protected void refreshDisplayView() {
        int spent = timeLogControlSet.getTimeSpent();
        int remaining = this.remaining.getTimeDurationInSeconds();

        List<String> toJoin = new ArrayList<>();
        if (spent > 0) {
            toJoin.add(activity.getString(R.string.TEA_timer_elap, DateUtils.formatElapsedTime(spent)));
        }
        if (remaining > 0) {
            toJoin.add(activity.getString(R.string.TEA_timer_remain, DateUtils.formatElapsedTime(remaining)));
        }

        String toDisplay = TextUtils.join(", ", toJoin);

        if (!TextUtils.isEmpty(toDisplay)) {
            displayEdit.setText(toDisplay);
            displayEdit.setTextColor(themeColor);
        } else {
            displayEdit.setText(R.string.TEA_timer_controls);
            displayEdit.setTextColor(unsetColor);
        }
    }

    @Override
    public void timerStopped(Task task, TaskTimeLog timeLog) {
        timeLogControlSet.timerStopped(task, timeLog);
        remaining.readFromTaskOnInitialize();
        refreshDisplayView();
//        elapsed.readFromTask(task);
    }

    @Override
    public void timerStarted(Task task) {
    }

    private class EstimatedTimeListener implements TimeDurationControlSet.TimeDurationChangeListener {
        @Override
        public void timeDurationChanged(int newDurationInSeconds) {
            int elapsedSeconds = model.getElapsedSeconds();
            int timeLeft = newDurationInSeconds - elapsedSeconds;
            setRemainingTime(timeLeft);
        }
    }
}
