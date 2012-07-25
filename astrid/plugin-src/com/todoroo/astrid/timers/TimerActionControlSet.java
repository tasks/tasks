/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

public class TimerActionControlSet extends TaskEditControlSet {

    private final ImageView timerButton;
    private final Chronometer chronometer;
    private final LinearLayout timerContainer;
    private boolean timerActive;
    private final List<TimerActionListener> listeners = new LinkedList<TimerActionListener>();

    public TimerActionControlSet(Activity activity, View parent) {
        super(activity, -1);
        timerContainer = (LinearLayout) parent.findViewById(R.id.timer_container);
        timerButton = (ImageView) parent.findViewById(R.id.timer_button);
        timerContainer.setOnClickListener(timerListener);
        chronometer = (Chronometer) parent.findViewById(R.id.timer);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        if (model.getValue(Task.TIMER_START) == 0)
            timerActive = false;
        else
            timerActive = true;

        updateDisplay();
    }

    @Override
    protected void afterInflate() {
        // Do nothing
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing to do here
        return null;
    }

    private final OnClickListener timerListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (timerActive) {
                TimerPlugin.updateTimer(activity, model, false);

                for(TimerActionListener listener : listeners)
                    listener.timerStopped(model);
                chronometer.stop();
            } else {
                TimerPlugin.updateTimer(activity, model, true);
                for(TimerActionListener listener : listeners)
                    listener.timerStarted(model);
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
            drawable = R.drawable.icn_edit_timer;
        }
        timerButton.setImageResource(drawable);


        long elapsed = model.getValue(Task.ELAPSED_SECONDS) * 1000L;
        if (timerActive) {
            chronometer.setVisibility(View.VISIBLE);
            elapsed += DateUtilities.now() - model.getValue(Task.TIMER_START);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            if (elapsed > DateUtilities.ONE_DAY) {
                chronometer.setOnChronometerTickListener(new OnChronometerTickListener() {
                    public void onChronometerTick(Chronometer cArg) {
                        long t = SystemClock.elapsedRealtime() - cArg.getBase();
                        cArg.setText(DateFormat.format("d'd' h:mm", t)); //$NON-NLS-1$
                    }
                });

            }
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
