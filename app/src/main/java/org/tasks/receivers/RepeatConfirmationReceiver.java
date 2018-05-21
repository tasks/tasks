package org.tasks.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.ical.values.RRule;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.text.ParseException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import timber.log.Timber;

public class RepeatConfirmationReceiver extends BroadcastReceiver {

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
    if (activity instanceof MainActivity) {
      taskListFragment = ((MainActivity) activity).getTaskListFragment();
    }
    if (taskListFragment == null) {
      Timber.d("No task list fragment");
      return;
    }
    long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, 0);

    if (taskId > 0) {
      long oldDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
      long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, 0);
      Task task = taskDao.fetch(taskId);

      try {
        showSnackbar(taskListFragment, task, oldDueDate, newDueDate);
      } catch (Exception e) {
        tracker.reportException(e);
      }
    }
  }

  private void showSnackbar(
      TaskListFragment taskListFragment,
      final Task task,
      final long oldDueDate,
      final long newDueDate) {
    String dueDateString = getRelativeDateAndTimeString(activity, newDueDate);
    String snackbarText =
        activity.getString(R.string.repeat_snackbar, task.getTitle(), dueDateString);
    taskListFragment
        .makeSnackbar(snackbarText)
        .setAction(
            R.string.DLG_undo,
            v -> {
              task.setDueDateAdjustingHideUntil(oldDueDate);
              task.setCompletionDate(0L);
              try {
                RRule rrule = new RRule(task.getRecurrenceWithoutFrom());
                int count = rrule.getCount();
                if (count > 0) {
                  rrule.setCount(count + 1);
                }
                task.setRecurrence(rrule, task.repeatAfterCompletion());
              } catch (ParseException e) {
                Timber.e(e);
              }
              taskDao.save(task);
            })
        .show();
  }

  private String getRelativeDateAndTimeString(Context context, long date) {
    String dueString = date > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
    if (Task.hasDueTime(date)) {
      dueString =
          context.getString(
              R.string.repeat_snackbar_time, dueString, DateUtilities.getTimeString(context, date));
    }
    return dueString;
  }
}
