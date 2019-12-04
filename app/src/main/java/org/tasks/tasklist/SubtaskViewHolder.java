package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.chip.Chip;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.CheckableImageView;
import org.tasks.R;
import org.tasks.data.TaskContainer;
import org.tasks.locale.Locale;
import org.tasks.ui.CheckBoxes;

public class SubtaskViewHolder extends RecyclerView.ViewHolder {

  private final Activity context;
  private final Locale locale;
  private final Callbacks callbacks;
  private final DisplayMetrics metrics;

  public TaskContainer task;

  @BindView(R.id.rowBody)
  ViewGroup rowBody;

  @BindView(R.id.title)
  TextView nameView;

  @BindView(R.id.completeBox)
  CheckableImageView completeBox;

  @BindView(R.id.chip_button)
  Chip chip;

  private int indent;

  SubtaskViewHolder(
      Activity context,
      Locale locale,
      ViewGroup view,
      Callbacks callbacks,
      DisplayMetrics metrics) {
    super(view);
    this.context = context;
    this.locale = locale;
    this.callbacks = callbacks;
    this.metrics = metrics;
    ButterKnife.bind(this, view);

    view.setTag(this);
    for (int i = 0; i < view.getChildCount(); i++) {
      view.getChildAt(i).setTag(this);
    }
  }

  private float getShiftSize() {
    return 20 * metrics.density;
  }

  private int getIndentSize(int indent) {
    return Math.round(indent * getShiftSize());
  }

  void bindView(TaskContainer task, boolean multiLevelSubtasks) {
    this.task = task;
    setIndent(multiLevelSubtasks ? task.indent : 0);
    if (task.hasChildren()) {
      chip.setText(locale.formatNumber(task.children));
      chip.setVisibility(View.VISIBLE);
      chip.setChipIconResource(
          task.isCollapsed()
              ? R.drawable.ic_keyboard_arrow_up_black_24dp
              : R.drawable.ic_keyboard_arrow_down_black_24dp);
    } else {
      chip.setVisibility(View.GONE);
    }

    nameView.setText(task.getTitle());
    setupTitleAndCheckbox();
  }

  private void setupTitleAndCheckbox() {
    if (task.isCompleted()) {
      nameView.setEnabled(false);
      nameView.setPaintFlags(nameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      nameView.setEnabled(!task.isHidden());
      nameView.setPaintFlags(nameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    completeBox.setChecked(task.isCompleted());
    completeBox.setImageDrawable(CheckBoxes.getCheckBox(context, task.getTask()));
    completeBox.invalidate();
  }

  @OnClick(R.id.title)
  void openSubtask(View v) {
    callbacks.openSubtask(task.getTask());
  }

  @OnClick(R.id.chip_button)
  void toggleSubtasks(View v) {
    callbacks.toggleSubtask(task.getId(), !task.isCollapsed());
  }

  @OnClick(R.id.completeBox)
  void onCompleteBoxClick(View v) {
    if (task == null) {
      return;
    }

    boolean newState = completeBox.isChecked();

    if (newState != task.isCompleted()) {
      callbacks.complete(task.getTask(), newState);
    }

    // set check box to actual action item state
    setupTitleAndCheckbox();
  }

  public int getIndent() {
    return indent;
  }

  @SuppressLint("NewApi")
  public void setIndent(int indent) {
    this.indent = indent;
    int indentSize = getIndentSize(indent);
    if (atLeastLollipop()) {
      MarginLayoutParams layoutParams = (MarginLayoutParams) rowBody.getLayoutParams();
      layoutParams.setMarginStart(indentSize);
      rowBody.setLayoutParams(layoutParams);
    } else {
      rowBody.setPadding(indentSize, rowBody.getPaddingTop(), 0, rowBody.getPaddingBottom());
    }
  }

  public interface Callbacks {
    void openSubtask(Task task);

    void toggleSubtask(long taskId, boolean collapsed);

    void complete(Task task, boolean completed);
  }
}
