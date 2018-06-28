package org.tasks.tasklist;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Paint;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.common.collect.Lists;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.core.LinkActionExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.List;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.ui.CheckBoxes;
import timber.log.Timber;

class ViewHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final CheckBoxes checkBoxes;
  private final TagFormatter tagFormatter;
  private final int textColorSecondary;
  private final int textColorHint;
  private final TaskDao taskDao;
  private final DialogBuilder dialogBuilder;
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

  @BindView(R.id.taskActionIcon)
  ImageView taskActionIcon;

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
      int textColorHint,
      TaskDao taskDao,
      DialogBuilder dialogBuilder,
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
    this.textColorHint = textColorHint;
    this.taskDao = taskDao;
    this.dialogBuilder = dialogBuilder;
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

    // Task action
    TaskAction action = getTaskAction(task, task.hasFiles());
    if (action != null) {
      taskActionIcon.setVisibility(View.VISIBLE);
      taskActionIcon.setImageResource(action.icon);
      taskActionIcon.setTag(action);
    } else {
      taskActionIcon.setVisibility(View.GONE);
      taskActionIcon.setTag(null);
    }
  }

  private TaskAction getTaskAction(Task task, boolean hasFiles) {
    if (task.isCompleted()) {
      return null;
    }
    return LinkActionExposer.getActionsForTask(context, task, hasFiles);
  }

  private void setTaskAppearance() {
    boolean completed = task.isCompleted();

    TextView name = nameView;
    if (completed) {
      name.setEnabled(false);
      name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      name.setEnabled(true);
      name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
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
    } else if (task.isCompleted()) {
      String dateValue =
          DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate());
      dueDateView.setText(context.getResources().getString(R.string.TAd_completed, dateValue));
      dueDateView.setTextColor(textColorHint);
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

  @OnClick(R.id.taskActionIcon)
  void onTaskActionClick() {
    TaskAction action = (TaskAction) taskActionIcon.getTag();
    if (action instanceof NotesAction) {
      showEditNotesDialog(task);
    } else if (action instanceof FilesAction) {
      showFilesDialog(task);
    } else if (action != null) {
      try {
        action.intent.send();
      } catch (PendingIntent.CanceledException e) {
        // Oh well
        Timber.e(e);
      }
    }
  }

  private void showEditNotesDialog(final Task task) {
    Task t = taskDao.fetch(task.getId());
    if (t == null || !t.hasNotes()) {
      return;
    }
    SpannableString description = new SpannableString(t.getNotes());
    Linkify.addLinks(description, Linkify.ALL);
    AlertDialog dialog =
        dialogBuilder
            .newDialog()
            .setMessage(description)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    View message = dialog.findViewById(android.R.id.message);
    if (message != null && message instanceof TextView) {
      ((TextView) message).setMovementMethod(LinkMovementMethod.getInstance());
    }
  }

  private void showFilesDialog(Task task) {
    // TODO: reimplement this
    //        FilesControlSet filesControlSet = new FilesControlSet();
    //        filesControlSet.hideAddAttachmentButton();
    //        filesControlSet.readFromTask(task);
    //        filesControlSet.getView().performClick();
  }

  interface ViewHolderCallbacks {

    void onCompletedTask(Task task, boolean newState);

    void onClick(ViewHolder viewHolder);

    boolean onLongPress(ViewHolder viewHolder);
  }
}
