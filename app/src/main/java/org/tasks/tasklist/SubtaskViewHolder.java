package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.annotation.SuppressLint;
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
import com.google.android.material.chip.ChipGroup;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.CheckableImageView;
import org.tasks.R;
import org.tasks.data.TaskContainer;
import org.tasks.ui.CheckBoxProvider;
import org.tasks.ui.ChipProvider;

public class SubtaskViewHolder extends RecyclerView.ViewHolder {

  private final Callbacks callbacks;
  private final DisplayMetrics metrics;
  private final ChipProvider chipProvider;
  private final CheckBoxProvider checkBoxProvider;

  private TaskContainer task;

  @BindView(R.id.rowBody)
  ViewGroup rowBody;

  @BindView(R.id.title)
  TextView nameView;

  @BindView(R.id.completeBox)
  CheckableImageView completeBox;

  @BindView(R.id.chip_group)
  ChipGroup chipGroup;

  private int indent;

  SubtaskViewHolder(
      ViewGroup view,
      Callbacks callbacks,
      DisplayMetrics metrics,
      ChipProvider chipProvider,
      CheckBoxProvider checkBoxProvider) {
    super(view);
    this.callbacks = callbacks;
    this.metrics = metrics;
    this.chipProvider = chipProvider;
    this.checkBoxProvider = checkBoxProvider;
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

  void bindView(TaskContainer task) {
    this.task = task;
    setIndent(task.indent);
    if (task.hasChildren()) {
      chipGroup.removeAllViews();
      Chip child = chipProvider.newSubtaskChip(task, true);
      child.setOnClickListener(v -> callbacks.toggleSubtask(task.getId(), !task.isCollapsed()));
      chipGroup.addView(child);
      chipGroup.setVisibility(View.VISIBLE);
    } else {
      chipGroup.setVisibility(View.GONE);
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
    completeBox.setImageDrawable(checkBoxProvider.getCheckBox(task.getTask()));
    completeBox.invalidate();
  }

  @OnClick(R.id.title)
  void openSubtask(View v) {
    callbacks.openSubtask(task.getTask());
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

  private void setIndent(int indent) {
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
