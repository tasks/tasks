package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.DateUtilities.getRelativeDateTime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.TaskCompleter;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.List;
import org.tasks.R;
import org.tasks.data.TaskContainer;
import org.tasks.dialogs.Linkify;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxProvider;
import org.tasks.ui.ChipProvider;
import org.threeten.bp.format.FormatStyle;

public class ViewHolder extends RecyclerView.ViewHolder {

  private final Activity context;
  private final Preferences preferences;
  private final int textColorSecondary;
  private final TaskCompleter taskCompleter;
  private final ViewHolderCallbacks callback;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final int rowPadding;
  private final Linkify linkify;
  private final CheckBoxProvider checkBoxProvider;
  private final int textColorOverdue;
  private final ChipProvider chipProvider;

  @BindView(R.id.row)
  public ViewGroup row;

  @BindView(R.id.due_date)
  public TextView dueDate;

  public TaskContainer task;

  @BindView(R.id.rowBody)
  ViewGroup rowBody;

  @BindView(R.id.title)
  TextView nameView;

  @BindView(R.id.description)
  TextView description;

  @BindView(R.id.completeBox)
  CheckableImageView completeBox;

  @BindView(R.id.chip_group)
  ChipGroup chipGroup;

  private int indent;
  private boolean selected;
  private boolean moving;
  private int minIndent;
  private int maxIndent;

  ViewHolder(
      Activity context,
      ViewGroup view,
      Preferences preferences,
      int fontSize,
      ChipProvider chipProvider,
      CheckBoxProvider checkBoxProvider,
      int textColorOverdue,
      int textColorSecondary,
      TaskCompleter taskCompleter,
      ViewHolderCallbacks callback,
      DisplayMetrics metrics,
      int background,
      int selectedColor,
      int rowPadding,
      Linkify linkify) {
    super(view);
    this.context = context;
    this.preferences = preferences;
    this.chipProvider = chipProvider;
    this.checkBoxProvider = checkBoxProvider;
    this.textColorOverdue = textColorOverdue;
    this.textColorSecondary = textColorSecondary;
    this.taskCompleter = taskCompleter;
    this.callback = callback;
    this.metrics = metrics;
    this.background = background;
    this.selectedColor = selectedColor;
    this.rowPadding = rowPadding;
    this.linkify = linkify;
    ButterKnife.bind(this, view);

    if (preferences.getBoolean(R.string.p_fullTaskTitle, false)) {
      nameView.setMaxLines(Integer.MAX_VALUE);
      nameView.setSingleLine(false);
      nameView.setEllipsize(null);
    }

    if (preferences.getBoolean(R.string.p_show_full_description, false)) {
      description.setMaxLines(Integer.MAX_VALUE);
      description.setSingleLine(false);
      description.setEllipsize(null);
    }

    setTopPadding(rowPadding, nameView, completeBox, dueDate);
    setBottomPadding(rowPadding, completeBox, dueDate);

    nameView.setTextSize(fontSize);
    description.setTextSize(fontSize);
    int fontSizeDetails = Math.max(10, fontSize - 2);
    dueDate.setTextSize(fontSizeDetails);

    view.setTag(this);
    for (int i = 0; i < view.getChildCount(); i++) {
      view.getChildAt(i).setTag(this);
    }
  }

  private void setTopPadding(int padding, View... views) {
    for (View v : views) {
      v.setPaddingRelative(v.getPaddingStart(), padding, v.getPaddingEnd(), v.getPaddingBottom());
    }
  }

  private void setBottomPadding(int padding, View... views) {
    for (View v : views) {
      v.setPaddingRelative(v.getPaddingStart(), v.getPaddingTop(), v.getPaddingEnd(), padding);
    }
  }

  boolean isMoving() {
    return moving;
  }

  void setMoving(boolean moving) {
    this.moving = moving;
    updateBackground();
  }

  private void updateBackground() {
    if (selected || moving) {
      rowBody.setBackgroundColor(selectedColor);
    } else {
      rowBody.setBackgroundResource(background);
      rowBody.getBackground().jumpToCurrentState();
    }
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
    updateBackground();
  }

  @SuppressLint("NewApi")
  public void setIndent(int indent) {
    this.indent = indent;
    int indentSize = getIndentSize(indent);
    if (atLeastLollipop()) {
      MarginLayoutParams layoutParams = (MarginLayoutParams) row.getLayoutParams();
      layoutParams.setMarginStart(indentSize);
      row.setLayoutParams(layoutParams);
    } else {
      rowBody.setPadding(indentSize, rowBody.getPaddingTop(), 0, rowBody.getPaddingBottom());
    }
  }

  float getShiftSize() {
    return 20 * metrics.density;
  }

  private int getIndentSize(int indent) {
    return Math.round(indent * getShiftSize());
  }

  void bindView(TaskContainer task, Filter filter, boolean hideSubtasks) {
    this.task = task;
    this.indent = task.indent;

    nameView.setText(task.getTitle());
    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
        nameView, task.isHidden() ? R.drawable.ic_outline_visibility_off_24px : 0, 0, 0, 0);
    setupTitleAndCheckbox();
    setupDueDate();
    if (preferences.getBoolean(R.string.p_show_list_indicators, true)) {
      setupChips(filter, hideSubtasks);
    }
    if (preferences.getBoolean(R.string.p_show_description, true)) {
      description.setText(task.getNotes());
      description.setVisibility(task.hasNotes() ? View.VISIBLE : View.GONE);
    }
    if (preferences.getBoolean(R.string.p_linkify_task_list, false)) {
      linkify.linkify(nameView, this::onRowBodyClick);
      linkify.linkify(description, this::onRowBodyClick);
      nameView.setOnLongClickListener(view -> onRowBodyLongClick());
      description.setOnLongClickListener(view -> onRowBodyLongClick());
    }
    if (chipGroup.getVisibility() == View.VISIBLE) {
      setBottomPadding(rowPadding, chipGroup);
      setBottomPadding(0, description, nameView);
    } else if (description.getVisibility() == View.VISIBLE) {
      setBottomPadding(rowPadding, description);
      setBottomPadding(0, nameView);
    } else {
      setBottomPadding(rowPadding, nameView);
    }
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

  private void setupDueDate() {
    if (task.hasDueDate()) {
      if (task.isOverdue()) {
        dueDate.setTextColor(textColorOverdue);
      } else {
        dueDate.setTextColor(textColorSecondary);
      }
      String dateValue =
          getRelativeDateTime(
              context, task.getDueDate(), Locale.getInstance().getLocale(), FormatStyle.MEDIUM);
      dueDate.setText(dateValue);
      dueDate.setVisibility(View.VISIBLE);
    } else {
      dueDate.setVisibility(View.GONE);
    }
  }

  private void setupChips(Filter filter, boolean hideSubtaskChip) {
    List<Chip> chips = chipProvider.getChips(filter, indent > 0, hideSubtaskChip, task);
    if (chips.isEmpty()) {
      chipGroup.setVisibility(View.GONE);
    } else {
      chipGroup.removeAllViews();
      for (Chip chip : chips) {
        chip.setOnClickListener(this::onChipClick);
        chipGroup.addView(chip);
      }
      chipGroup.setVisibility(View.VISIBLE);
    }
  }

  private void onChipClick(View v) {
    Object tag = v.getTag();
    if (tag instanceof Filter) {
      callback.onClick((Filter) tag);
    } else if (tag instanceof TaskContainer) {
      TaskContainer task = (TaskContainer) tag;
      callback.toggleSubtasks(task, !task.isCollapsed());
    }
  }

  @OnClick(R.id.rowBody)
  void onRowBodyClick() {
    callback.onClick(this);
  }

  @OnLongClick(R.id.rowBody)
  boolean onRowBodyLongClick() {
    return callback.onLongPress(this);
  }

  @OnClick(R.id.completeBox)
  void onCompleteBoxClick(View v) {
    if (task == null) {
      return;
    }

    boolean newState = completeBox.isChecked();

    if (newState != task.isCompleted()) {
      taskCompleter.setComplete(task.getTask(), newState);
      callback.onCompletedTask(task, newState);
    }

    // set check box to actual action item state
    setupTitleAndCheckbox();
  }

  @OnClick(R.id.due_date)
  void changeDueDate() {
    callback.onChangeDueDate(task);
  }

  public int getIndent() {
    return indent;
  }

  void setMinIndent(int minIndent) {
    this.minIndent = minIndent;
    if (task.getTargetIndent() < minIndent) {
      task.setTargetIndent(minIndent);
    }
  }

  void setMaxIndent(int maxIndent) {
    this.maxIndent = maxIndent;
    if (task.getTargetIndent() > maxIndent) {
      task.setTargetIndent(maxIndent);
    }
  }

  int getMinIndent() {
    return minIndent;
  }

  int getMaxIndent() {
    return maxIndent;
  }

  interface ViewHolderCallbacks {

    void onCompletedTask(TaskContainer task, boolean newState);

    void onClick(ViewHolder viewHolder);

    void onClick(Filter filter);

    void toggleSubtasks(TaskContainer task, boolean collapsed);

    boolean onLongPress(ViewHolder viewHolder);

    void onChangeDueDate(TaskContainer task);
  }
}
