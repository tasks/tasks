/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Activity;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.notifications.NotificationManager;

import java.util.LinkedList;
import java.util.List;

public class TimerActionControlSet extends TaskEditControlSetBase {

    private final ImageView timerButton;
    private final Chronometer chronometer;
    private boolean timerActive;
    private final List<TimerActionListener> listeners = new LinkedList<>();

    public TimerActionControlSet(final NotificationManager notificationManager, final TaskService taskService, final Activity activity, View parent) {
        super(activity, -1);

        LinearLayout timerContainer = (LinearLayout) parent.findViewById(R.id.timer_container);
        timerButton = (ImageView) parent.findViewById(R.id.timer_button);
        OnClickListener timerListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerActive) {
                    TimerPlugin.updateTimer(notificationManager, taskService, activity, model, false);

                    for (TimerActionListener listener : listeners) {
                        listener.timerStopped(model);
                    }
                    chronometer.stop();
                } else {
                    TimerPlugin.updateTimer(notificationManager, taskService, activity, model, true);
                    for (TimerActionListener listener : listeners) {
                        listener.timerStarted(model);
                    }
                    chronometer.start();
                }
                timerActive = !timerActive;
                updateDisplay();
            }
        };
        timerContainer.setOnClickListener(timerListener);
        chronometer = (Chronometer) parent.findViewById(R.id.timer);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        timerActive = model.getTimerStart() != 0;

        updateDisplay();
    }

    @Override
    protected void afterInflate() {
        // Do nothing
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // Nothing to do here
    }

    private void updateDisplay() {
        final int drawable;
        if(timerActive) {
            drawable = R.drawable.icn_timer_stop;
        } else {
            drawable = R.drawable.icn_edit_timer;
        }
        timerButton.setImageResource(drawable);


        long elapsed = model.getElapsedSeconds() * 1000L;
        if (timerActive) {
            chronometer.setVisibility(View.VISIBLE);
            elapsed += DateUtilities.now() - model.getTimerStart();
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            if (elapsed > DateUtilities.ONE_DAY) {
                chronometer.setOnChronometerTickListener(new OnChronometerTickListener() {
                    @Override
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

    @Override
    public int getIcon() {
        return -1;
    }

    public interface TimerActionListener {
        void timerStopped(Task task);
        void timerStarted(Task task);
    }

    public void addListener(TimerActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(TimerActionListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
}
