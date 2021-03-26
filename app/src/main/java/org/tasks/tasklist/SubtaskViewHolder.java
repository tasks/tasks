package org.tasks.tasklist;

import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.CheckableImageView;

import org.tasks.data.TaskContainer;
import org.tasks.databinding.SubtaskAdapterRowBodyBinding;
import org.tasks.ui.CheckBoxProvider;
import org.tasks.ui.ChipProvider;

public class SubtaskViewHolder extends RecyclerView.ViewHolder {

  private final Callbacks callbacks;
  private final DisplayMetrics metrics;
  private final ChipProvider chipProvider;
  private final CheckBoxProvider checkBoxProvider;

  private TaskContainer task;

  private final ViewGroup rowBody;
  private final TextView nameView;
  private final CheckableImageView completeBox;
  private final ChipGroup chipGroup;

  SubtaskViewHolder(
          SubtaskAdapterRowBodyBinding binding,
          Callbacks callbacks,
          DisplayMetrics metrics,
          ChipProvider chipProvider,
          CheckBoxProvider checkBoxProvider) {
    super(binding.getRoot());
    this.callbacks = callbacks;
    this.metrics = metrics;
    this.chipProvider = chipProvider;
    this.checkBoxProvider = checkBoxProvider;

    rowBody = binding.rowBody;
    nameView = binding.title;
    completeBox = binding.completeBox;
    chipGroup = binding.chipGroup;

    nameView.setOnClickListener(v -> openSubtask());
    completeBox.setOnClickListener(v -> onCompleteBoxClick());

    ViewGroup view = binding.getRoot();
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

  private void openSubtask() {
    callbacks.openSubtask(task.getTask());
  }

  private void onCompleteBoxClick() {
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

  private void setIndent(int indent) {
    int indentSize = getIndentSize(indent);
    MarginLayoutParams layoutParams = (MarginLayoutParams) rowBody.getLayoutParams();
    layoutParams.setMarginStart(indentSize);
    rowBody.setLayoutParams(layoutParams);
  }

  public interface Callbacks {
    void openSubtask(Task task);

    void toggleSubtask(long taskId, boolean collapsed);

    void complete(Task task, boolean completed);
  }
}
