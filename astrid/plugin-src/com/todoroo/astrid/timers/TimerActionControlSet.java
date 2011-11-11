package com.todoroo.astrid.timers;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

public class TimerActionControlSet extends TaskEditControlSet {

    private final Button timerButton;
    private boolean timerActive;
    private final Activity activity;
    private Task task;
    private TimerStoppedListener listener;

    public TimerActionControlSet(Activity activity, View buttonParent) {
        super(activity, -1);
        this.activity = activity;
        timerButton = (Button) buttonParent.findViewById(R.id.timer_button);
        timerButton.setOnClickListener(timerListener);
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
            } else {
                TimerPlugin.updateTimer(activity, task, true);
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
    }

    public interface TimerStoppedListener {
        public void timerStopped(Task task);
    }

    public void setListener(TimerStoppedListener listener) {
        this.listener = listener;
    }
}
