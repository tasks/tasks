/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Activity;
import android.content.Context;
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
import butterknife.BindView;
import butterknife.OnClick;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.TimeDurationControlSet;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.themes.Theme;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TimerControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_timer_pref;
  private static final String EXTRA_STARTED = "extra_started";
  private static final String EXTRA_ESTIMATED = "extra_estimated";
  private static final String EXTRA_ELAPSED = "extra_elapsed";
  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject Theme theme;

  @BindView(R.id.display_row_edit)
  TextView displayEdit;

  @BindView(R.id.timer)
  Chronometer chronometer;

  @BindView(R.id.timer_button)
  ImageView timerButton;

  private TimeDurationControlSet estimated;
  private TimeDurationControlSet elapsed;
  private long timerStarted;
  private AlertDialog dialog;
  private View dialogView;
  private TimerControlSetCallback callback;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    int elapsedSeconds;
    int estimatedSeconds;
    if (savedInstanceState == null) {
      timerStarted = task.getTimerStart();
      elapsedSeconds = task.getElapsedSeconds();
      estimatedSeconds = task.getEstimatedSeconds();
    } else {
      timerStarted = savedInstanceState.getLong(EXTRA_STARTED);
      elapsedSeconds = savedInstanceState.getInt(EXTRA_ELAPSED);
      estimatedSeconds = savedInstanceState.getInt(EXTRA_ESTIMATED);
    }

    dialogView = inflater.inflate(R.layout.control_set_timers_dialog, null);
    estimated = new TimeDurationControlSet(context, dialogView, R.id.estimatedDuration, theme);
    elapsed = new TimeDurationControlSet(context, dialogView, R.id.elapsedDuration, theme);
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
      dialog = buildDialog();
    }
    dialog.show();
  }

  private AlertDialog buildDialog() {
    return dialogBuilder
        .newDialog()
        .setView(dialogView)
        .setPositiveButton(android.R.string.ok, (dialog12, which) -> refreshDisplayView())
        .setOnCancelListener(dialog1 -> refreshDisplayView())
        .create();
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
  public boolean hasChanges(Task original) {
    return elapsed.getTimeDurationInSeconds() != original.getElapsedSeconds()
        || estimated.getTimeDurationInSeconds() != original.getEstimatedSeconds();
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
      toDisplay = est + ", " + elap; // $NON-NLS-1$
    } else if (!TextUtils.isEmpty(est)) {
      toDisplay = est;
    } else if (!TextUtils.isEmpty(elap)) {
      toDisplay = elap;
    } else {
      toDisplay = null;
    }

    displayEdit.setText(toDisplay);
  }

  private void updateChronometer() {
    timerButton.setImageResource(
        timerActive() ? R.drawable.ic_pause_24dp : R.drawable.ic_play_arrow_24dp);

    long elapsed = this.elapsed.getTimeDurationInSeconds() * 1000L;
    if (timerActive()) {
      chronometer.setVisibility(View.VISIBLE);
      elapsed += DateUtilities.now() - timerStarted;
      chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
      if (elapsed > DateUtilities.ONE_DAY) {
        chronometer.setOnChronometerTickListener(
            cArg -> {
              long t = SystemClock.elapsedRealtime() - cArg.getBase();
              cArg.setText(DateFormat.format("d'd' h:mm", t)); // $NON-NLS-1$
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

  public interface TimerControlSetCallback {

    Task stopTimer();

    Task startTimer();
  }
}
