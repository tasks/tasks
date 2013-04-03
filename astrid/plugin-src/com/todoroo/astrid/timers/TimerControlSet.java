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

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.ui.TimeDurationControlSet;

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

    public TimerControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(R.string.TEA_timer_controls);
        displayEdit.setTextColor(unsetColor);

        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);

        estimated = new TimeDurationTaskEditControlSet(activity, getView(), Task.ESTIMATED_SECONDS,
                R.id.estimatedDuration, 0, R.string.DLG_hour_minutes
                );
        elapsed = new TimeDurationTaskEditControlSet(activity, getView(), Task.ELAPSED_SECONDS, R.id.elapsedDuration,
                0, R.string.DLG_hour_minutes
                );
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
    protected String writeToModelAfterInitialized(Task task) {
        if (initialized) {
            estimated.writeToModel(task);
            elapsed.writeToModel(task);
        }
        return null;
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

        public TimeDurationTaskEditControlSet(Activity activity, View v, IntegerProperty property, int timeButtonId,
                int prefixResource, int titleResource) {
            super(activity, -1);
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity, v, property,
                    timeButtonId, prefixResource, titleResource);
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
        protected String writeToModelAfterInitialized(Task task) {
            task.setValue(property, controlSet.getTimeDurationInSeconds());
            return null;
        }

        public String getDisplayString() {
            int seconds = controlSet.getTimeDurationInSeconds();
            if (seconds > 0)
                return DateUtils.formatElapsedTime(controlSet.getTimeDurationInSeconds());
            return null;
        }
    }

    @Override
    protected void refreshDisplayView() {
        String est = estimated.getDisplayString();
        if (!TextUtils.isEmpty(est))
            est = activity.getString(R.string.TEA_timer_est, est);
        String elap = elapsed.getDisplayString();
        if (!TextUtils.isEmpty(elap))
            elap = activity.getString(R.string.TEA_timer_elap, elap);

        String toDisplay;

        if (!TextUtils.isEmpty(est) && !TextUtils.isEmpty(elap))
            toDisplay = est + ", " + elap; //$NON-NLS-1$
        else if (!TextUtils.isEmpty(est))
            toDisplay = est;
        else if (!TextUtils.isEmpty(elap))
            toDisplay = elap;
        else
            toDisplay = null;

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
        elapsed.readFromTask(task);
    }

    @Override
    public void timerStarted(Task task) {
        return;
    }

}
