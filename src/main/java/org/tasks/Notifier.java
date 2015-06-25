package org.tasks;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.reminders.TaskNotificationIntentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;
import org.tasks.notifications.NotificationManager;
import org.tasks.reminders.NotificationActivity;

import javax.inject.Inject;

public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    private Context context;
    private TaskDao taskDao;
    private NotificationManager notificationManager;

    @Inject
    public Notifier(@ForApplication Context context, TaskDao taskDao, NotificationManager notificationManager) {
        this.context = context;
        this.taskDao = taskDao;
        this.notificationManager = notificationManager;
    }

    public void triggerTaskNotification(long id, int type) {
        if (!showTaskNotification(id, type)) {
            notificationManager.cancel(id);
        }
    }

    private boolean showTaskNotification(final long id, final int type) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                    Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS);
            if (task == null) {
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted() || task.isDeleted()) {
            return false;
        }

        // new task edit in progress
        if (TextUtils.isEmpty(task.getTitle())) {
            return false;
        }

        // it's hidden - don't sound, don't delete
        if (task.isHidden() && type == ReminderService.TYPE_RANDOM) {
            return true;
        }

        // task due date was changed, but alarm wasn't rescheduled
        boolean dueInFuture = task.hasDueTime() && task.getDueDate() > DateUtilities.now() ||
                !task.hasDueTime() && task.getDueDate() - DateUtilities.now() > DateUtilities.ONE_DAY;
        if ((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || dueInFuture)) {
            return true;
        }

        // read properties
        final String taskTitle = task.getTitle();
        boolean nonstopMode = task.isNotifyModeNonstop();
        boolean ringFiveMode = task.isNotifyModeFive();
        final int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setReminderLast(DateUtilities.now());
        taskDao.saveExisting(task);

        final String text = context.getString(R.string.app_name);

        final Intent intent = new Intent(context, NotificationActivity.class) {{
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            setAction("NOTIFY" + id); //$NON-NLS-1$
            putExtra(NotificationActivity.EXTRA_TASK_ID, id);
            putExtra(NotificationActivity.EXTRA_TITLE, taskTitle);
        }};

        context.startService(new Intent(context, TaskNotificationIntentService.class) {{
            putExtra(TaskNotificationIntentService.EXTRAS_NOTIF_ID, (int) id);
            putExtra(TaskNotificationIntentService.EXTRA_TASK_ID, id);
            putExtra(TaskNotificationIntentService.EXTRAS_CUSTOM_INTENT, PendingIntent.getActivity(context, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            putExtra(TaskNotificationIntentService.EXTRAS_TYPE, type);
            putExtra(TaskNotificationIntentService.EXTRAS_TITLE, taskTitle);
            putExtra(TaskNotificationIntentService.EXTRAS_TEXT, text);
            putExtra(TaskNotificationIntentService.EXTRAS_RING_TIMES, ringTimes);
        }});

        return true;
    }
}
