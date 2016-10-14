package org.tasks.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.analytics.Tracker;

import javax.inject.Inject;

import timber.log.Timber;

public class RepeatConfirmationReceiver extends BroadcastReceiver {

    private final Property<?>[] REPEAT_RESCHEDULED_PROPERTIES =
            new Property<?>[]{
                    Task.ID,
                    Task.TITLE,
                    Task.DUE_DATE,
                    Task.HIDE_UNTIL,
                    Task.REPEAT_UNTIL
            };

    private final Activity activity;
    private final Tracker tracker;
    private final TaskDao taskDao;

    @Inject
    public RepeatConfirmationReceiver(Activity activity, Tracker tracker, TaskDao taskDao) {
        this.activity = activity;
        this.tracker = tracker;
        this.taskDao = taskDao;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        TaskListFragment taskListFragment = null;
        if (activity instanceof TaskListActivity) {
            taskListFragment = ((TaskListActivity) activity).getTaskListFragment();
        }
        if (taskListFragment == null) {
            Timber.d("No task list fragment");
            return;
        }
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, 0);

        if (taskId > 0) {
            long oldDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
            long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, 0);
            Task task = taskDao.fetch(taskId, REPEAT_RESCHEDULED_PROPERTIES);

            try {
                showSnackbar(taskListFragment, task, oldDueDate, newDueDate);
            } catch (Exception e) {
                tracker.reportException(e);
            }
        }
    }

    private void showSnackbar(TaskListFragment taskListFragment, final Task task, final long oldDueDate, final long newDueDate) {
        String dueDateString = getRelativeDateAndTimeString(activity, newDueDate);
        String snackbarText = activity.getString(R.string.repeat_snackbar, task.getTitle(), dueDateString);
        taskListFragment.makeSnackbar(snackbarText)
                .setAction(R.string.DLG_undo, v -> {
                    task.setDueDateAdjustingHideUntil(oldDueDate);
                    task.setCompletionDate(0L);
                    taskDao.save(task);
                })
                .show();
    }

    private String getRelativeDateAndTimeString(Context context, long date) {
        String dueString = date > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
        if (Task.hasDueTime(date)) {
            // TODO: localize this
            dueString = String.format("%s at %s", dueString, //$NON-NLS-1$
                    DateUtilities.getTimeString(context, date));
        }
        return dueString;
    }
}
