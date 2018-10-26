package org.tasks.tasklist;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.common.collect.Lists;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.List;
import org.tasks.R;
import org.tasks.ui.CheckBoxes;

class ViewHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final CheckBoxes checkBoxes;
  private final TagFormatter tagFormatter;
  private final int textColorSecondary;
  private final TaskDao taskDao;
  private final ViewHolderCallbacks callback;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final int textColorOverdue;

  @BindView(R.id.row)
  public ViewGroup row;

  @BindView(R.id.due_date)
  public TextView dueDate;

  public Task task;

  @BindView(R.id.rowBody)
  ViewGroup rowBody;

  @BindView(R.id.title)
  TextView nameView;

  @BindView(R.id.completeBox)
  CheckableImageView completeBox;

  @BindView(R.id.tag_block)
  TextView tagBlock;

  private int indent;
  private boolean selected;
  private boolean moving;

  ViewHolder(
      Context context,
      ViewGroup view,
      boolean showFullTaskTitle,
      int fontSize,
      CheckBoxes checkBoxes,
      TagFormatter tagFormatter,
      int textColorOverdue,
      int textColorSecondary,
      TaskDao taskDao,
      ViewHolderCallbacks callback,
      DisplayMetrics metrics,
      int background,
      int selectedColor,
      int rowPadding) {
    super(view);
    this.context = context;
    this.checkBoxes = checkBoxes;
    this.tagFormatter = tagFormatter;
    this.textColorOverdue = textColorOverdue;
    this.textColorSecondary = textColorSecondary;
    this.taskDao = taskDao;
    this.callback = callback;
    this.metrics = metrics;
    this.background = background;
    this.selectedColor = selectedColor;
    ButterKnife.bind(this, view);

    if (showFullTaskTitle) {
      nameView.setMaxLines(Integer.MAX_VALUE);
      nameView.setSingleLine(false);
      nameView.setEllipsize(null);
    }

    if (atLeastKitKat()) {
      rowBody.setPadding(0, rowPadding, 0, rowPadding);
    } else {
      ViewGroup.MarginLayoutParams layoutParams =
          (ViewGroup.MarginLayoutParams) rowBody.getLayoutParams();
      layoutParams.setMargins(
          layoutParams.leftMargin, rowPadding, layoutParams.rightMargin, rowPadding);
    }

    nameView.setTextSize(fontSize);
    int fontSizeDetails = Math.max(10, fontSize - 2);
    dueDate.setTextSize(fontSizeDetails);
    tagBlock.setTextSize(fontSizeDetails);

    view.setTag(this);
    for (int i = 0; i < view.getChildCount(); i++) {
      view.getChildAt(i).setTag(this);
    }
  }

  void setMoving(boolean moving) {
    this.moving = moving;
    updateBackground();
  }

  boolean isMoving() {
    return moving;
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
      ViewGroup.MarginLayoutParams layoutParams =
          (ViewGroup.MarginLayoutParams) row.getLayoutParams();
      layoutParams.setMarginStart(indentSize);
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

  boolean isIndented() {
    return indent > 0;
  }

  void bindView(Task task) {
    // TODO: see if this is a performance issue
    this.task = task;

    setFieldContentsAndVisibility();
    setTaskAppearance();
  }

  /** Helper method to set the contents and visibility of each field */
  private synchronized void setFieldContentsAndVisibility() {
    String nameValue = task.getTitle();

    long hiddenUntil = task.getHideUntil();
    if (hiddenUntil > DateUtilities.now()) {
      nameValue = context.getResources().getString(R.string.TAd_hiddenFormat, nameValue);
    }
    nameView.setText(nameValue);

    setupDueDateAndTags();
  }

  private void setTaskAppearance() {
    if (task.isCompleted()) {
      nameView.setEnabled(false);
      nameView.setPaintFlags(nameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
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
    final TextView dueDateView = dueDate;
    if (!task.isCompleted() && task.hasDueDate()) {
      long dueDate = task.getDueDate();
      if (task.isOverdue()) {
        dueDateView.setTextColor(textColorOverdue);
      } else {
        dueDateView.setTextColor(textColorSecondary);
      }
      String dateValue = DateUtilities.getRelativeDateStringWithTime(context, dueDate);
      dueDateView.setText(dateValue);
      dueDateView.setVisibility(View.VISIBLE);
    } else {
      dueDateView.setVisibility(View.GONE);
    }

    if (task.isCompleted()) {
      tagBlock.setVisibility(View.GONE);
    } else {
      String tags = task.getTagsString();
      List<String> tagUuids = tags != null ? newArrayList(tags.split(",")) : Lists.newArrayList();
      CharSequence tagString =
          tagFormatter.getTagString(task.getCaldav(), task.getGoogleTaskList(), tagUuids);
      if (TextUtils.isEmpty(tagString)) {
        tagBlock.setVisibility(View.GONE);
      } else {
        tagBlock.setText(tagString);
        tagBlock.setVisibility(View.VISIBLE);
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
      taskDao.setComplete(task, newState);
    }

    // set check box to actual action item state
    setTaskAppearance();
  }

  interface ViewHolderCallbacks {

    void onCompletedTask(Task task, boolean newState);

    void onClick(ViewHolder viewHolder);

    boolean onLongPress(ViewHolder viewHolder);
  }
}
