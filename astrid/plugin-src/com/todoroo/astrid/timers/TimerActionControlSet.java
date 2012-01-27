package com.todoroo.astrid.timers;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.ImageButton;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

public class TimerActionControlSet extends TaskEditControlSet {

    private final ImageButton timerButton;
    private final Chronometer chronometer;
    private boolean timerActive;
    private final Activity activity;
    private Task task;
    private final List<TimerActionListener> listeners = new LinkedList<TimerActionListener>();

    public TimerActionControlSet(Activity activity, View parent) {
        super(activity, -1);
        this.activity = activity;
        timerButton = (ImageButton) parent.findViewById(R.id.timer_button);
        timerButton.setOnClickListener(timerListener);

        chronometer = new Chronometer(activity);
    }

    @Override
    @SuppressWarnings("hiding")
    public void readFromTask(Task task) {
        if (task.getValue(Task.TIMER_START) == 0)
            timerActive = false;
        else
            timerActive = true;

        this.task = task;
        updateDisplay();
    }

    @Override
    @SuppressWarnings("hiding")
    public String writeToModel(Task task) {
        // Nothing to do here
        return null;
    }

    private final OnClickListener timerListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (timerActive) {
                // TimerPlugin.updateTimer(activity, task, false);

                for(TimerActionListener listener : listeners)
                    listener.timerStopped(task);
                chronometer.stop();
            } else {
                // TimerPlugin.updateTimer(activity, task, true);
                for(TimerActionListener listener : listeners)
                    listener.timerStarted(task);
                chronometer.start();
            }
            timerActive = !timerActive;
            updateDisplay();
        }
    };

    private void updateDisplay() {
        final int drawable;
        if(timerActive) {
            drawable = R.drawable.icn_timer_stop;
        } else {
            if (task.getValue(Task.ELAPSED_SECONDS) == 0)
                drawable = R.drawable.icn_edit_timer;
            else
                drawable = R.drawable.icn_timer_start;
        }
        timerButton.setBackgroundResource(drawable);


        long elapsed = task.getValue(Task.ELAPSED_SECONDS) * 1000L;
        if (timerActive) {
            chronometer.setVisibility(View.VISIBLE);
            elapsed += DateUtilities.now() - task.getValue(Task.TIMER_START);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            chronometer.start();
        } else {
            chronometer.setVisibility(View.GONE);
            chronometer.stop();
        }
    }

    public interface TimerActionListener {
        public void timerStopped(Task task);
        public void timerStarted(Task task);
    }

    public void addListener(TimerActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(TimerActionListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }
}
