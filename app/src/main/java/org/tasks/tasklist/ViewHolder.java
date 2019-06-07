package org.tasks.tasklist;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.DateUtilities.getAbbreviatedRelativeDateWithTime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;
import android.text.TextUtils;
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
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.List;
import org.tasks.R;
import org.tasks.data.TaskContainer;
import org.tasks.dialogs.Linkify;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.ChipProvider;

public class ViewHolder extends RecyclerView.ViewHolder {

  private final Activity context;
  private final Preferences preferences;
  private final CheckBoxes checkBoxes;
  private final int textColorSecondary;
  private final int textColorPrimary;
  private final TaskDao taskDao;
  private final ViewHolderCallbacks callback;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final Linkify linkify;
  private final int textColorOverdue;
  private final ChipProvider chipProvider;
  private final int fontSizeDetails;

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

  @BindView(R.id.hidden_status)
  ImageView hidden;

  private int indent;
  private boolean selected;
  private boolean moving;
  private boolean isGoogleTaskList;
  private int minIndent;
  private int maxIndent;

  ViewHolder(
      Activity context,
      Locale locale,
      ViewGroup view,
      Preferences preferences,
      int fontSize,
      CheckBoxes checkBoxes,
      ChipProvider chipProvider,
      int textColorOverdue,
      int textColorSecondary,
      int textColorPrimary,
      TaskDao taskDao,
      ViewHolderCallbacks callback,
      DisplayMetrics metrics,
      int background,
      int selectedColor,
      int rowPadding,
      Linkify linkify) {
    super(view);
    this.context = context;
    this.preferences = preferences;
    this.checkBoxes = checkBoxes;
    this.chipProvider = chipProvider;
    this.textColorOverdue = textColorOverdue;
    this.textColorSecondary = textColorSecondary;
    this.textColorPrimary = textColorPrimary;
    this.taskDao = taskDao;
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
    fontSizeDetails = Math.max(10, fontSize - 2);
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

  void bindView(TaskContainer task, boolean isGoogleTaskList) {
    this.task = task;
    this.isGoogleTaskList = isGoogleTaskList;

    setFieldContentsAndVisibility();
    setTaskAppearance();
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

  /** Helper method to set the contents and visibility of each field */
  private synchronized void setFieldContentsAndVisibility() {
    nameView.setText(task.getTitle());
    hidden.setVisibility(task.isHidden() ? View.VISIBLE : View.GONE);
    setupDueDateAndTags();
  }

  private void setTaskAppearance() {
    if (task.isCompleted()) {
      nameView.setTextColor(textColorSecondary);
      nameView.setPaintFlags(nameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      nameView.setTextColor(task.isHidden() ? textColorSecondary : textColorPrimary);
      nameView.setEnabled(true);
      nameView.setPaintFlags(nameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    setupDueDateAndTags();

    setupCompleteBox();
  }

  private void setupCompleteBox() {
    // complete box
    final CheckableImageView checkBoxView = completeBox;
    boolean completed = task.isCompleted();
    checkBoxView.setChecked(completed);

    if (completed) {
      checkBoxView.setImageDrawable(checkBoxes.getCompletedCheckbox(task.getPriority()));
    } else if (TextUtils.isEmpty(task.getRecurrence())) {
      checkBoxView.setImageDrawable(checkBoxes.getCheckBox(task.getPriority()));
    } else {
      checkBoxView.setImageDrawable(checkBoxes.getRepeatingCheckBox(task.getPriority()));
    }
    checkBoxView.invalidate();
  }

  private void setupDueDateAndTags() {
    // due date / completion date
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

    if (preferences.getBoolean(R.string.p_show_list_indicators, true)) {
      String tags = task.getTagsString();
      List<String> tagUuids = tags != null ? newArrayList(tags.split(",")) : Lists.newArrayList();

      List<Chip> chips =
          chipProvider.getChips(
              context,
              task.getCaldav(),
              isGoogleTaskList ? null : task.getGoogleTaskList(),
              tagUuids);
      if (chips.isEmpty()) {
        chipGroup.setVisibility(View.GONE);
      } else {
        chipGroup.removeAllViews();
        for (Chip chip : chips) {
          chip.setTextSize(fontSizeDetails);
          chip.setOnClickListener(view -> callback.onClick((Filter) view.getTag()));
          chipGroup.addView(chip);
        }
        chipGroup.setVisibility(View.VISIBLE);
      }
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
      callback.onCompletedTask(task, newState);
      taskDao.setComplete(task.getTask(), newState);
    }

    // set check box to actual action item state
    setTaskAppearance();
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

    boolean onLongPress(ViewHolder viewHolder);
  }
}
