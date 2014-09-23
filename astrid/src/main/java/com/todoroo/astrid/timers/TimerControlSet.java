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
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

import org.tasks.R;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet extends PopupControlSet implements TimerActionListener {

    TimeDurationTaskEditControlSet estimated, elapsed, remaining;
    private final TextView displayEdit;
    private final ImageView image;

    private TimeLogControlSet timeLogControlSet;

    public TimerControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(R.string.TEA_timer_controls);
        displayEdit.setTextColor(unsetColor);

        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);

        estimated = new TimeDurationTaskEditControlSet(activity, getView(), Task.ESTIMATED_SECONDS,
                R.id.estimatedDurationLayout, 0, R.string.DLG_hour_minutes, R.string.TEA_estimatedDuration_label
                );
//        elapsed = new TimeDurationTaskEditControlSet(activity, getView(), Task.ELAPSED_SECONDS, R.id.elapsedDurationLayout,
//                0, R.string.DLG_hour_minutes,
//                R.string.TEA_elapsedDuration_label);
        remaining = new TimeDurationTaskEditControlSet(activity, getView(), Task.REMAINING_SECONDS, R.id.remainingDurationLayout,
                0, R.string.DLG_hour_minutes,
                R.string.TEA_remainingDuration_label);//TODO zmienic kolumne z Task.ELAPSED_SECONDS na remaining

        timeLogControlSet = new TimeLogControlSet(activity);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        estimated.readFromTask(model);
//        elapsed.readFromTask(model);
        remaining.readFromTask(model);
    }

    @Override
    protected void afterInflate() {
        // Nothing to do here
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if (initialized) {
            estimated.writeToModel(task);
//            elapsed.writeToModel(task);
            remaining.writeToModel(task);
        }
    }

    // --- TimeDurationTaskEditControlSet

    /**
     * Control set for mapping a Property to a TimeDurationControlSet
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TimeDurationTaskEditControlSet extends TaskEditControlSet {
        private final TimeDurationControlSet controlSet;
        private final IntegerProperty property;

        public TimeDurationTaskEditControlSet(Activity activity, View v, IntegerProperty property, int layoutId,
                                              int prefixResource, int titleResource, int labelId) {
            super(activity, -1);
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity, v, property,
                    layoutId, prefixResource, titleResource, labelId);
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
            task.setValue(property, controlSet.getTimeDurationInSeconds());
        }

        public String getDisplayString() {
            int seconds = controlSet.getTimeDurationInSeconds();
            if (seconds > 0) {
                return DateUtils.formatElapsedTime(controlSet.getTimeDurationInSeconds());
            }
            return null;
        }
    }

    @Override
    protected void refreshDisplayView() {
//        String est = estimated.getDisplayString();
//        if (!TextUtils.isEmpty(est)) {
//            est = activity.getString(R.string.TEA_timer_est, est);
//            est = est.substring(0,est.length()-3);
//        }
        String elaps = "";//elapsed.getDisplayString();
        if (!TextUtils.isEmpty(elaps)) {
            elaps = activity.getString(R.string.TEA_timer_elap, elaps);
            elaps = elaps.substring(0,elaps.length()-3);
        }

        String remain = remaining.getDisplayString();
        if (!TextUtils.isEmpty(remain)) {
            remain = activity.getString(R.string.TEA_timer_remain, remain);
            remain = remain.substring(0,remain.length()-3);
        }

        String toDisplay="";
        if (!TextUtils.isEmpty(elaps))
            toDisplay += elaps;
        if (!TextUtils.isEmpty(toDisplay))
            toDisplay += ", ";
        if (!TextUtils.isEmpty(remain))
            toDisplay += remain;

        if (!TextUtils.isEmpty(toDisplay)) {
            displayEdit.setText(toDisplay);
            displayEdit.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_timer, R.drawable.tea_icn_timer_lightblue));
        } else {
            displayEdit.setText(R.string.TEA_timer_controls);
            displayEdit.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_timer_gray);
        }
    }

    @Override
    public void timerStopped(Task task) {
//        elapsed.readFromTask(task);
    }

    @Override
    public void timerStarted(Task task) {
    }

}
