package org.tasks.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import butterknife.BindView;
import butterknife.OnClick;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.themes.ColorProvider;

public class PriorityControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_importance_pref;
  private static final String EXTRA_PRIORITY = "extra_priority";

  @Inject @ForApplication Context context;
  @Inject ColorProvider colorProvider;

  @BindView(R.id.priority_high)
  AppCompatRadioButton priorityHigh;

  @BindView(R.id.priority_medium)
  AppCompatRadioButton priorityMedium;

  @BindView(R.id.priority_low)
  AppCompatRadioButton priorityLow;

  @BindView(R.id.priority_none)
  AppCompatRadioButton priorityNone;

  private @Priority int priority;

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @OnClick({R.id.priority_high, R.id.priority_medium, R.id.priority_low, R.id.priority_none})
  void onPriorityChanged() {
    priority = getPriority();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState == null) {
      priority = task.getPriority();
    } else {
      priority = savedInstanceState.getInt(EXTRA_PRIORITY);
    }
    if (priority == 0) {
      priorityHigh.setChecked(true);
    } else if (priority == 1) {
      priorityMedium.setChecked(true);
    } else if (priority == 2) {
      priorityLow.setChecked(true);
    } else {
      priorityNone.setChecked(true);
    }
    tintRadioButton(priorityHigh, 0);
    tintRadioButton(priorityMedium, 1);
    tintRadioButton(priorityLow, 2);
    tintRadioButton(priorityNone, 3);
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_PRIORITY, priority);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_priority;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_outline_flag_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public void apply(Task task) {
    task.setPriority(priority);
  }

  @Override
  public boolean hasChanges(Task original) {
    return original.getPriority() != priority;
  }

  private void tintRadioButton(AppCompatRadioButton radioButton, int priority) {
    int color = colorProvider.getPriorityColor(priority, true);
    radioButton.setButtonTintList(
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {color, color}));
  }

  private @Priority int getPriority() {
    if (priorityHigh.isChecked()) {
      return Priority.HIGH;
    }
    if (priorityMedium.isChecked()) {
      return Priority.MEDIUM;
    }
    if (priorityLow.isChecked()) {
      return Priority.LOW;
    }
    return Priority.NONE;
  }
}
