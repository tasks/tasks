package com.todoroo.astrid.timers;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.TimeDurationControlSet;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet implements TaskEditControlSet {

    private final Activity activity;

    TaskEditControlSet estimated, elapsed;

    public TimerControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.timer_control, parent, true);

        estimated = new TimeDurationTaskEditControlSet(Task.ESTIMATED_SECONDS,
                R.id.estimatedDuration, 0, R.string.DLG_hour_minutes
                );
        elapsed = new TimeDurationTaskEditControlSet(Task.ELAPSED_SECONDS, R.id.elapsedDuration,
                0, R.string.DLG_hour_minutes
                );
    }

    @Override
    public void readFromTask(Task task) {
        estimated.readFromTask(task);
        elapsed.readFromTask(task);
    }

    @Override
    public String writeToModel(Task task) {
        estimated.writeToModel(task);
        elapsed.writeToModel(task);
        return null;
    }

    // --- TimeDurationTaskEditControlSet

    /**
     * Control set for mapping a Property to a TimeDurationControlSet
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TimeDurationTaskEditControlSet implements TaskEditControlSet {
        private final TimeDurationControlSet controlSet;
        private final IntegerProperty property;

        public TimeDurationTaskEditControlSet(IntegerProperty property, int timeButtonId,
                int prefixResource, int titleResource) {
            this.property = property;
            this.controlSet = new TimeDurationControlSet(activity,
                    timeButtonId, prefixResource, titleResource);
        }

        @Override
        public void readFromTask(Task task) {
            controlSet.setTimeDuration(task.getValue(property));
        }

        @Override
        public String writeToModel(Task task) {
            task.setValue(property, controlSet.getTimeDurationInSeconds());
            return null;
        }
    }

}