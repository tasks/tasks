/**
 * Copyright (c) 2012 Todoroo Inc
 * <p/>
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.TimeDurationControlSet;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.ThemeManager;
import org.tasks.ui.TaskEditControlFragment;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_timer_pref;

    public interface TimerControlSetCallback {
        Task stopTimer();
        Task startTimer();
    }

    private static final String EXTRA_STARTED = "extra_started";
    private static final String EXTRA_ESTIMATED = "extra_estimated";
    private static final String EXTRA_ELAPSED = "extra_elapsed";

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject ThemeManager themeManager;

    @Bind(R.id.display_row_edit) TextView displayEdit;
    @Bind(R.id.timer) Chronometer chronometer;
    @Bind(R.id.timer_button) ImageView timerButton;

    private TimeDurationControlSet estimated;
    private TimeDurationControlSet elapsed;
    private long timerStarted;
    protected AlertDialog dialog;
    private View dialogView;
    private int elapsedSeconds;
    private int estimatedSeconds;
    private TimerControlSetCallback callback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            timerStarted = savedInstanceState.getLong(EXTRA_STARTED);
            elapsedSeconds = savedInstanceState.getInt(EXTRA_ELAPSED);
            estimatedSeconds = savedInstanceState.getInt(EXTRA_ESTIMATED);
        }

        dialogView = inflater.inflate(R.layout.control_set_timers_dialog, null);
        estimated = new TimeDurationControlSet(context, dialogView, R.id.estimatedDuration, themeManager);
        elapsed = new TimeDurationControlSet(context, dialogView, R.id.elapsedDuration, themeManager);
        estimated.setTimeDuration(estimatedSeconds);
        elapsed.setTimeDuration(elapsedSeconds);
        refresh();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (TimerControlSetCallback) activity;
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_ELAPSED, elapsed.getTimeDurationInSeconds());
        outState.putInt(EXTRA_ESTIMATED, estimated.getTimeDurationInSeconds());
        outState.putLong(EXTRA_STARTED, timerStarted);
    }

    @OnClick(R.id.display_row_edit)
    void openPopup(View view) {
        if (dialog == null) {
            buildDialog();
        }
        dialog.show();
    }

    protected Dialog buildDialog() {
        AlertDialog.Builder builder = dialogBuilder.newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        refreshDisplayView();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        refreshDisplayView();
                    }
                });
        dialog = builder.show();
        return dialog;
    }

    @OnClick(R.id.timer_container)
    void timerClicked(View view) {
        if (timerActive()) {
            Task task = callback.stopTimer();
            elapsed.setTimeDuration(task.getElapsedSeconds());
            timerStarted = 0;
            chronometer.stop();
            refreshDisplayView();
        } else {
            Task task = callback.startTimer();
            timerStarted = task.getTimerStart();
            chronometer.start();
        }
        updateChronometer();
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_timers;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_timer_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        timerStarted = task.getTimerStart();
        elapsedSeconds = task.getElapsedSeconds();
        estimatedSeconds = task.getEstimatedSeconds();
    }

    @Override
    public boolean hasChanges(Task original) {
        return elapsed.getTimeDurationInSeconds() != original.getElapsedSeconds() ||
                estimated.getTimeDurationInSeconds() != original.getEstimatedSeconds();
    }

    @Override
    public void apply(Task task) {
        task.setElapsedSeconds(elapsed.getTimeDurationInSeconds());
        task.setEstimatedSeconds(estimated.getTimeDurationInSeconds());
    }

    private void refresh() {
        refreshDisplayView();
        updateChronometer();
    }

    private void refreshDisplayView() {
        String est = null;
        int estimatedSeconds = estimated.getTimeDurationInSeconds();
        if (estimatedSeconds > 0) {
            est = getString(R.string.TEA_timer_est, DateUtils.formatElapsedTime(estimatedSeconds));
        }
        String elap = null;
        int elapsedSeconds = elapsed.getTimeDurationInSeconds();
        if (elapsedSeconds > 0) {
            elap = getString(R.string.TEA_timer_elap, DateUtils.formatElapsedTime(elapsedSeconds));
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
            displayEdit.setAlpha(1.0f);
        } else {
            displayEdit.setText(R.string.TEA_timer_controls);
            displayEdit.setAlpha(0.5f);
        }
    }

    private void updateChronometer() {
        timerButton.setImageResource(timerActive()
                ? R.drawable.ic_pause_24dp
                : R.drawable.ic_play_arrow_24dp);

        long elapsed = this.elapsed.getTimeDurationInSeconds() * 1000L;
        if (timerActive()) {
            chronometer.setVisibility(View.VISIBLE);
            elapsed += DateUtilities.now() - timerStarted;
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            if (elapsed > DateUtilities.ONE_DAY) {
                chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
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

    private boolean timerActive() {
        return timerStarted > 0;
    }
}
