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
import com.todoroo.astrid.helper.TaskEditControlSetBase;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import static org.tasks.preferences.ResourceResolver.getResource;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet extends PopupControlSet implements TimerActionListener {

    TimeDurationTaskEditControlSet estimated, elapsed;
    private final TextView displayEdit;
    private final ImageView image;

    public TimerControlSet(ActivityPreferences preferences, final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(preferences, activity, viewLayout, displayViewLayout, title);

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(R.string.TEA_timer_controls);
        displayEdit.setTextColor(unsetColor);

        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);

        estimated = new TimeDurationTaskEditControlSet(activity, getView(), Task.ESTIMATED_SECONDS,R.id.estimatedDuration);
        elapsed = new TimeDurationTaskEditControlSet(activity, getView(), Task.ELAPSED_SECONDS, R.id.elapsedDuration);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        estimated.readFromTask(model);
        elapsed.readFromTask(model);
    }

    @Override
    protected void afterInflate() {
        // Nothing to do here
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if (initialized) {
            estimated.writeToModel(task);
            elapsed.writeToModel(task);
        }
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

        public TimeDurationTaskEditControlSet(Activity activity, View v, IntegerProperty property, int timeButtonId) {
            super(activity, -1);
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity, v, property, timeButtonId);
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
        String est = estimated.getDisplayString();
        if (!TextUtils.isEmpty(est)) {
            est = activity.getString(R.string.TEA_timer_est, est);
        }
        String elap = elapsed.getDisplayString();
        if (!TextUtils.isEmpty(elap)) {
            elap = activity.getString(R.string.TEA_timer_elap, elap);
        }

        String toDisplay;

        if (!TextUtils.isEmpty(est) && !TextUtils.isEmpty(elap)) {
            toDisplay = est + ", " + elap; //$NON-NLS-1$
        } else if (!TextUtils.isEmpty(est)) {
            toDisplay = est;
        } else if (!TextUtils.isEmpty(elap)) {
            toDisplay = elap;
        } else {
            toDisplay = null;
        }

        if (!TextUtils.isEmpty(toDisplay)) {
            displayEdit.setText(toDisplay);
            displayEdit.setTextColor(themeColor);
            image.setImageResource(getResource(activity, R.attr.tea_icn_timer));
        } else {
            displayEdit.setText(R.string.TEA_timer_controls);
            displayEdit.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_timer_gray);
        }
    }

    @Override
    public void timerStopped(Task task) {
        elapsed.readFromTask(task);
    }

    @Override
    public void timerStarted(Task task) {
    }

}
