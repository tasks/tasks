package com.todoroo.astrid.timers;

import android.app.Activity;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

public class TimerActionControlSet extends TaskEditControlSet {

    private final Button timerButton;
    private final Chronometer chronometer;
    private final TextView timerLabel;
    private boolean timerActive;
    private final Activity activity;
    private Task task;
    private TimerStoppedListener listener;

    public TimerActionControlSet(Activity activity, View parent) {
        super(activity, -1);
        this.activity = activity;
        timerButton = (Button) parent.findViewById(R.id.timer_button);
        timerButton.setOnClickListener(timerListener);

        chronometer = (Chronometer) parent.findViewById(R.id.timer);
        timerLabel = (TextView) parent.findViewById(R.id.timer_label);
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
                TimerPlugin.updateTimer(activity, task, false);
                if (listener != null)
                    listener.timerStopped(task);
                chronometer.stop();
            } else {
                TimerPlugin.updateTimer(activity, task, true);
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
            timerLabel.setVisibility(View.GONE);
            elapsed += DateUtilities.now() - task.getValue(Task.TIMER_START);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            chronometer.start();
        } else {
            chronometer.setVisibility(View.GONE);
            timerLabel.setVisibility(View.VISIBLE);
            timerLabel.setText(DateUtils.formatElapsedTime(elapsed / 1000L));
            chronometer.stop();
        }
    }

    public interface TimerStoppedListener {
        public void timerStopped(Task task);
    }

    public void setListener(TimerStoppedListener listener) {
        this.listener = listener;
    }
}
