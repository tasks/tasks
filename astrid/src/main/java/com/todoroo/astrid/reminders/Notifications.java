/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class Notifications extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(Notifications.class);

    /**
     * task id extra
     */
    public static final String ID_KEY = "id"; //$NON-NLS-1$

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     */
    public static final String BROADCAST_IN_APP_NOTIFY = Constants.PACKAGE + ".IN_APP_NOTIFY"; //$NON-NLS-1$
    public static final String EXTRAS_CUSTOM_INTENT = "intent"; //$NON-NLS-1$
    public static final String EXTRAS_NOTIF_ID = "notifId"; //$NON-NLS-1$

    /**
     * notification type extra
     */
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$
    public static final String EXTRAS_TITLE = "title"; //$NON-NLS-1$
    public static final String EXTRAS_TEXT = "text"; //$NON-NLS-1$
    public static final String EXTRAS_RING_TIMES = "ringTimes"; //$NON-NLS-1$

    @Inject TaskDao taskDao;
    @Inject @ForApplication Context context;
    @Inject NotificationManager notificationManager;
    @Inject Preferences preferences;
    @Inject Broadcaster broadcaster;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);

        handle(intent);
    }

    void handle(Intent intent) {
        long id = intent.getLongExtra(ID_KEY, 0);
        int type = intent.getIntExtra(EXTRAS_TYPE, (byte) 0);

        Resources r = context.getResources();
        String reminder;

        if (type == ReminderService.TYPE_ALARM) {
            reminder = getRandomReminder(r.getStringArray(R.array.reminders_alarm));
        } else {
            reminder = ""; //$NON-NLS-1$
        }

        if (!showTaskNotification(id, type, reminder)) {
            notificationManager.cancel((int) id);
        }
    }

    /**
     * @return a random reminder string
     */
    private String getRandomReminder(String[] reminders) {
        int next = ReminderService.random.nextInt(reminders.length);
        return reminders[next];
    }

    /**
     * Show a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled.
     */
    private boolean showTaskNotification(long id, int type, String reminder) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                    Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS, Task.USER_ID);
            if (task == null) {
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        if (!preferences.getBoolean(R.string.p_rmd_enabled, true)) {
            return false;
        }
        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted() || task.isDeleted() || !Task.USER_ID_SELF.equals(task.getUserID())) {
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
        String taskTitle = task.getTitle();
        boolean nonstopMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_NONSTOP);
        boolean ringFiveMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_FIVE);
        int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setReminderLast(DateUtilities.now());
        task.setSocialReminder(Task.REMINDER_SOCIAL_UNSEEN);
        taskDao.saveExisting(task);

        String title = context.getString(R.string.app_name);
        String text = reminder + " " + taskTitle; //$NON-NLS-1$

        Intent notifyIntent = new Intent(context, TaskListActivity.class);
        FilterWithCustomIntent itemFilter = new FilterWithCustomIntent(context.getString(R.string.rmd_NoA_filter),
                context.getString(R.string.rmd_NoA_filter),
                new QueryTemplate().where(TaskCriteria.byId(id)),
                null);
        Bundle customExtras = new Bundle();
        customExtras.putLong(NotificationFragment.TOKEN_ID, id);
        customExtras.putString(EXTRAS_TEXT, text);
        itemFilter.customExtras = customExtras;
        itemFilter.customTaskList = new ComponentName(context, NotificationFragment.class);

        notifyIntent.setAction("NOTIFY" + id); //$NON-NLS-1$
        notifyIntent.putExtra(TaskListFragment.TOKEN_FILTER, itemFilter);
        notifyIntent.putExtra(NotificationFragment.TOKEN_ID, id);
        notifyIntent.putExtra(EXTRAS_TEXT, text);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        notifyIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_NOTIFICATION);

        requestNotification((int) id, notifyIntent, type, title, text, ringTimes);
        return true;
    }

    private void requestNotification(long taskId, Intent intent, int type, String title, String text, int ringTimes) {
        Intent inAppNotify = new Intent(BROADCAST_IN_APP_NOTIFY);
        inAppNotify.putExtra(EXTRAS_NOTIF_ID, (int) taskId);
        inAppNotify.putExtra(NotificationFragment.TOKEN_ID, taskId);
        inAppNotify.putExtra(EXTRAS_CUSTOM_INTENT, intent);
        inAppNotify.putExtra(EXTRAS_TYPE, type);
        inAppNotify.putExtra(EXTRAS_TITLE, title);
        inAppNotify.putExtra(EXTRAS_TEXT, text);
        inAppNotify.putExtra(EXTRAS_RING_TIMES, ringTimes);
        broadcaster.sendOrderedBroadcast(inAppNotify, AstridApiConstants.PERMISSION_READ);
    }
}
