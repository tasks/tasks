package org.tasks.tasklist;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.DateUtilities.getAbbreviatedRelativeDateWithTime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.collect.Lists;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.TaskCompleter;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.List;
import org.tasks.R;
import org.tasks.data.Location;
import org.tasks.data.TaskContainer;
import org.tasks.dialogs.Linkify;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.ChipProvider;

public class ViewHolder extends RecyclerView.ViewHolder {

  private final Activity context;
  private final Preferences preferences;
  private final int textColorSecondary;
  private final TaskCompleter taskCompleter;
  private final ViewHolderCallbacks callback;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final Linkify linkify;
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

  @BindView(R.id.location_chip)
  Chip locationChip;

  @BindView(R.id.chip_group)
  ChipGroup chipGroup;

  @BindView(R.id.hidden_status)
  ImageView hidden;

  @BindView(R.id.subtasks_chip)
  Chip subtasksChip;

  private int indent;
  private boolean selected;
  private boolean moving;
  private boolean isRemoteList;
  private int minIndent;
  private int maxIndent;

  ViewHolder(
      Activity context,
      Locale locale,
      ViewGroup view,
      Preferences preferences,
      int fontSize,
      ChipProvider chipProvider,
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
    this.textColorOverdue = textColorOverdue;
    this.textColorSecondary = textColorSecondary;
    this.taskCompleter = taskCompleter;
    this.callback = callback;
    this.metrics = metrics;
    this.background = background;
    this.selectedColor = selectedColor;
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

    if (atLeastKitKat()) {
      rowBody.setPadding(0, rowPadding, 0, rowPadding);
    } else {
      MarginLayoutParams lp = (MarginLayoutParams) rowBody.getLayoutParams();
      lp.setMargins(lp.leftMargin, rowPadding, lp.rightMargin, rowPadding);
    }

    nameView.setTextSize(fontSize);
    description.setTextSize(fontSize);
    int fontSizeDetails = Math.max(10, fontSize - 2);
    dueDate.setTextSize(fontSizeDetails);

    if (atLeastJellybeanMR1()) {
      chipGroup.setLayoutDirection(
          locale.isRtl() ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);
    } else {
      MarginLayoutParams lp = (MarginLayoutParams) chipGroup.getLayoutParams();
      lp.setMargins(lp.rightMargin, lp.topMargin, lp.leftMargin, lp.bottomMargin);
    }

    view.setTag(this);
    for (int i = 0; i < view.getChildCount(); i++) {
      view.getChildAt(i).setTag(this);
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

  void bindView(TaskContainer task, boolean isRemoteList) {
    this.task = task;
    this.isRemoteList = isRemoteList;
    this.indent = task.indent;

    nameView.setText(task.getTitle());
    hidden.setVisibility(task.isHidden() ? View.VISIBLE : View.GONE);
    setupTitleAndCheckbox();
    setupDueDate();
    if (task.hasChildren()) {
      subtasksChip.setVisibility(View.VISIBLE);
      subtasksChip.setText(
          context
              .getResources()
              .getQuantityString(R.plurals.subtask_count, task.children, task.children));
      subtasksChip.setChipIconResource(
          task.isCollapsed()
              ? R.drawable.ic_keyboard_arrow_up_black_24dp
              : R.drawable.ic_keyboard_arrow_down_black_24dp);
    } else {
      subtasksChip.setVisibility(View.GONE);
    }
    if (preferences.getBoolean(R.string.p_show_list_indicators, true)) {
      setupLocation();
      setupTags();
    }
    if (preferences.getBoolean(R.string.p_show_description, true)) {
      description.setText(task.getNotes());
      description.setVisibility(task.hasNotes() ? View.VISIBLE : View.GONE);
    }
    if (preferences.getBoolean(R.string.p_linkify_task_list, false)) {
      linkify.linkify(nameView, this::onRowBodyClick, this::onRowBodyLongClick);
      linkify.linkify(description, this::onRowBodyClick, this::onRowBodyLongClick);
      nameView.setOnClickListener(view -> onRowBodyClick());
      nameView.setOnLongClickListener(view -> onRowBodyLongClick());
      description.setOnClickListener(view -> onRowBodyClick());
      description.setOnLongClickListener(view -> onRowBodyLongClick());
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
    completeBox.setImageDrawable(CheckBoxes.getCheckBox(context, task.getTask()));
    completeBox.invalidate();
  }

  private void setupDueDate() {
    if (task.hasDueDate()) {
      if (task.isOverdue()) {
        dueDate.setTextColor(textColorOverdue);
      } else {
        dueDate.setTextColor(textColorSecondary);
      }
      String dateValue = getAbbreviatedRelativeDateWithTime(context, task.getDueDate());
      dueDate.setText(dateValue);
      dueDate.setVisibility(View.VISIBLE);
    } else {
      dueDate.setVisibility(View.GONE);
    }
  }

  private void setupLocation() {
    if (task.hasLocation()) {
      locationChip.setText(task.getLocation().getDisplayName());
      locationChip.setTag(task.getLocation());
      locationChip.setOnClickListener(v -> {
        Location location = (Location) v.getTag();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(location.getGeoUri()));
        context.startActivity(intent);
      });
      locationChip.setVisibility(View.VISIBLE);
    } else {
      locationChip.setVisibility(View.GONE);
    }
  }

  private void setupTags() {
    String tags = task.getTagsString();
    List<String> tagUuids = tags != null ? newArrayList(tags.split(",")) : Lists.newArrayList();
    boolean hideListChip = isRemoteList || indent > 0;
    List<Chip> chips =
        chipProvider.getChips(
            context,
            hideListChip ? null : task.getCaldav(),
            hideListChip ? null : task.getGoogleTaskList(),
            tagUuids);
    if (chips.isEmpty()) {
      chipGroup.setVisibility(View.GONE);
    } else {
      chipGroup.removeAllViews();
      for (Chip chip : chips) {
        chip.setOnClickListener(view -> callback.onClick((Filter) view.getTag()));
        chipGroup.addView(chip);
      }
      chipGroup.setVisibility(View.VISIBLE);
    }
  }

  @OnClick(R.id.subtasks_chip)
  void toggleSubtasks() {
    callback.toggleSubtasks(task, !task.isCollapsed());
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
  }
}
