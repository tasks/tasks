package org.tasks.tasklist;

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

import com.google.common.collect.Lists;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.core.LinkActionExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;
import com.todoroo.astrid.ui.CheckableImageView;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.ui.CheckBoxes;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

/**
 * View Holder saves a lot of findViewById lookups.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class ViewHolder extends RecyclerView.ViewHolder {

    public interface ViewHolderCallbacks {
        void onCompletedTask(Task task, boolean newState);

        void onClick(ViewHolder viewHolder);
    }

    @BindView(R.id.row) public ViewGroup row;
    @BindView(R.id.rowBody) ViewGroup rowBody;
    @BindView(R.id.title) TextView nameView;
    @BindView(R.id.completeBox) CheckableImageView completeBox;
    @BindView(R.id.due_date) public TextView dueDate;
    @BindView(R.id.tag_block) TextView tagBlock;
    @BindView(R.id.taskActionContainer) View taskActionContainer;
    @BindView(R.id.taskActionIcon) ImageView taskActionIcon;

    public Task task;

    private String tagsString; // From join query, not part of the task model
    private boolean hasFiles; // From join query, not part of the task model
    private boolean hasNotes;
    private final Context context;
    private final int fontSize;
    private final CheckBoxes checkBoxes;
    private final TagFormatter tagFormatter;
    private final int textColorSecondary;
    private final int textColorHint;
    private final TaskDao taskDao;
    private final DialogBuilder dialogBuilder;
    private final ViewHolderCallbacks callback;
    private final DisplayMetrics metrics;
    private final int textColorOverdue;
    private Pair<Float, Float> lastTouchYRawY = new Pair<>(0f, 0f);
    private int indent;

    public ViewHolder(Context context, ViewGroup view, boolean showFullTaskTitle, int fontSize,
                      CheckBoxes checkBoxes, TagFormatter tagFormatter,
                      int textColorOverdue, int textColorSecondary, int textColorHint, TaskDao taskDao,
                      DialogBuilder dialogBuilder, ViewHolderCallbacks callback, int minRowHeight,
                      DisplayMetrics metrics) {
        super(view);
        this.context = context;
        this.fontSize = fontSize;
        this.checkBoxes = checkBoxes;
        this.tagFormatter = tagFormatter;
        this.textColorOverdue = textColorOverdue;
        this.textColorSecondary = textColorSecondary;
        this.textColorHint = textColorHint;
        this.taskDao = taskDao;
        this.dialogBuilder = dialogBuilder;
        this.callback = callback;
        this.metrics = metrics;
        ButterKnife.bind(this, view);

        task = new Task();

        if (showFullTaskTitle) {
            nameView.setMaxLines(Integer.MAX_VALUE);
            nameView.setSingleLine(false);
            nameView.setEllipsize(null);
        }

        view.setTag(this);
        for(int i = 0; i < view.getChildCount(); i++) {
            view.getChildAt(i).setTag(this);
        }

        setMinimumHeight(minRowHeight);

        addListeners();
    }

    @SuppressLint("NewApi")
    public void setIndent(int indent) {
        this.indent = indent;
        int indentSize = getIndentSize(indent);
        if (atLeastLollipop()) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) row.getLayoutParams();
            layoutParams.setMarginStart(indentSize);
        } else {
            rowBody.setPadding(indentSize, row.getPaddingTop(), 0, row.getPaddingBottom());
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

    void bindView(TodorooCursor<Task> cursor) {
        tagsString = cursor.get(TaskAdapter.TAGS);
        hasFiles = cursor.get(TaskAdapter.FILE_ID_PROPERTY) > 0;
        hasNotes = cursor.get(TaskAdapter.HAS_NOTES_PROPERTY) > 0;

        // TODO: see if this is a performance issue
        task = new Task(cursor);

        setFieldContentsAndVisibility();
        setTaskAppearance();
    }

    private void setMinimumHeight(int minRowHeight) {
        if (fontSize < 16) {
            rowBody.setMinimumHeight(0);
            completeBox.setMinimumHeight(0);
        } else {
            rowBody.setMinimumHeight(minRowHeight);
            completeBox.setMinimumHeight(minRowHeight);
        }
    }

    /** Helper method to set the contents and visibility of each field */
    private synchronized void setFieldContentsAndVisibility() {
        String nameValue = task.getTitle();

        long hiddenUntil = task.getHideUntil();
        if(task.getDeletionDate() > 0) {
            nameValue = context.getResources().getString(R.string.TAd_deletedFormat, nameValue);
        }
        if(hiddenUntil > DateUtilities.now()) {
            nameValue = context.getResources().getString(R.string.TAd_hiddenFormat, nameValue);
        }
        nameView.setText(nameValue);

        setupDueDateAndTags();

        // Task action
        ImageView taskAction = taskActionIcon;
        if (taskAction != null) {
            TaskAction action = getTaskAction(task, hasFiles, hasNotes);
            if (action != null) {
                taskActionContainer.setVisibility(View.VISIBLE);
                taskAction.setImageResource(action.icon);
                taskAction.setTag(action);
            } else {
                taskActionContainer.setVisibility(View.GONE);
                taskAction.setTag(null);
            }
        }
    }

    private TaskAction getTaskAction(Task task, boolean hasFiles, boolean hasNotes) {
        if (task.isCompleted()) {
            return null;
        }
        return LinkActionExposer.getActionsForTask(context, task, hasFiles, hasNotes);
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
        name.setTextSize(fontSize);

        setupDueDateAndTags();

        float detailTextSize = Math.max(10, fontSize * 14 / 20);
        if(dueDate != null) {
            dueDate.setTextSize(detailTextSize);
            dueDate.setTypeface(null, 0);
        }

        setupCompleteBox();
    }

    private void setupCompleteBox() {
        // complete box
        final CheckableImageView checkBoxView = completeBox;
        boolean completed = task.isCompleted();
        checkBoxView.setChecked(completed);

        if (completed) {
            checkBoxView.setImageDrawable(checkBoxes.getCompletedCheckbox(task.getImportance()));
        } else if (TextUtils.isEmpty(task.getRecurrence())) {
            checkBoxView.setImageDrawable(checkBoxes.getCheckBox(task.getImportance()));
        } else {
            checkBoxView.setImageDrawable(checkBoxes.getRepeatingCheckBox(task.getImportance()));
        }
        checkBoxView.invalidate();
    }

    private void setupDueDateAndTags() {
        // due date / completion date
        final TextView dueDateView = dueDate;
        if(!task.isCompleted() && task.hasDueDate()) {
            long dueDate = task.getDueDate();
            if(task.isOverdue()) {
                dueDateView.setTextColor(textColorOverdue);
            } else {
                dueDateView.setTextColor(textColorSecondary);
            }
            String dateValue = DateUtilities.getRelativeDateStringWithTime(context, dueDate);
            dueDateView.setText(dateValue);
            dueDateView.setVisibility(View.VISIBLE);
        } else if(task.isCompleted()) {
            String dateValue = DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate());
            dueDateView.setText(context.getResources().getString(R.string.TAd_completed, dateValue));
            dueDateView.setTextColor(textColorHint);
            dueDateView.setVisibility(View.VISIBLE);
        } else {
            dueDateView.setVisibility(View.GONE);
        }

        if (task.isCompleted()) {
            tagBlock.setVisibility(View.GONE);
        } else {
            String tags = tagsString;

            List<String> tagUuids = tags != null ? newArrayList(tags.split(",")) : Lists.newArrayList();
            CharSequence tagString = tagFormatter.getTagString(tagUuids);
            if (TextUtils.isEmpty(tagString)) {
                tagBlock.setVisibility(View.GONE);
            } else {
                tagBlock.setText(tagString);
                tagBlock.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    private void addListeners() {
        // check box listener
        View.OnTouchListener otl = (v, event) -> {
            lastTouchYRawY = new Pair<>(event.getY(), event.getRawY());
            return false;
        };
        completeBox.setOnTouchListener(otl);
        completeBox.setOnClickListener(completeBoxListener);

        rowBody.setOnClickListener(view -> callback.onClick(this));

        if (taskActionContainer != null) {
            taskActionContainer.setOnClickListener(v -> {
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
                        Timber.e(e, e.getMessage());
                    }
                }
            });
        }
    }

    private void showEditNotesDialog(final Task task) {
        Task t = taskDao.fetch(task.getId(), Task.NOTES);
        if (t == null || !t.hasNotes()) {
            return;
        }
        SpannableString description = new SpannableString(t.getNotes());
        Linkify.addLinks(description, Linkify.ALL);
        AlertDialog dialog = dialogBuilder.newDialog()
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

    private final View.OnClickListener completeBoxListener = v -> {
        int[] location = new int[2];
        v.getLocationOnScreen(location);

        if(Math.abs(location[1] + lastTouchYRawY.getLeft() - lastTouchYRawY.getRight()) > 10) {
            completeBox.setChecked(!completeBox.isChecked());
            return;
        }

        completeTask(task, completeBox.isChecked());

        // set check box to actual action item state
        setTaskAppearance();
    };

    /**
     * This method is called when user completes a task via check box or other
     * means
     *
     * @param newState
     *            state that this task should be set to
     */
    private void completeTask(final Task task, final boolean newState) {
        if(task == null) {
            return;
        }

        if (newState != task.isCompleted()) {
            callback.onCompletedTask(task, newState);
            taskDao.setComplete(task, newState);
        }
    }
}
