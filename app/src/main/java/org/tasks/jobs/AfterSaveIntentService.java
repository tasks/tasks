package org.tasks.jobs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.repeats.RepeatTaskHelper;
import com.todoroo.astrid.timers.TimerPlugin;

import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.location.GeofenceService;
import org.tasks.notifications.NotificationManager;
import org.tasks.receivers.PushReceiver;
import org.tasks.scheduling.RefreshScheduler;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.todoroo.astrid.dao.TaskDao.TRANS_SUPPRESS_REFRESH;

public class AfterSaveIntentService extends InjectingJobIntentService {

    private static final String EXTRA_TASK_ID = "extra_task_id";
    private static final String EXTRA_MODIFIED_VALUES = "extra_modified_values";

    public static void enqueue(Context context, long taskId, Set<String> modifiedValues) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putStringArrayListExtra(EXTRA_MODIFIED_VALUES, newArrayList(modifiedValues));
        AfterSaveIntentService.enqueueWork(context, AfterSaveIntentService.class, JobManager.JOB_ID_TASK_STATUS_CHANGE, intent);
    }

    @Inject RepeatTaskHelper repeatTaskHelper;
    @Inject @ForApplication Context context;
    @Inject TaskDao taskDao;
    @Inject NotificationManager notificationManager;
    @Inject GeofenceService geofenceService;
    @Inject TimerPlugin timerPlugin;
    @Inject ReminderService reminderService;
    @Inject RefreshScheduler refreshScheduler;
    @Inject LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        super.onHandleWork(intent);

        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        ArrayList<String> modifiedValues = intent.getStringArrayListExtra(EXTRA_MODIFIED_VALUES);

        if (taskId == -1 || modifiedValues == null) {
            Timber.e("Invalid extras, taskId=%s modifiedValues=%s", taskId, modifiedValues);
            return;
        }

        Task task = taskDao.fetch(taskId);
        if (task == null) {
            Timber.e("Can't find task with id %s", taskId);
            return;
        }

        if(modifiedValues.contains(Task.DUE_DATE.name) ||
                modifiedValues.contains(Task.REMINDER_FLAGS.name) ||
                modifiedValues.contains(Task.REMINDER_PERIOD.name) ||
                modifiedValues.contains(Task.REMINDER_LAST.name) ||
                modifiedValues.contains(Task.REMINDER_SNOOZE.name)) {
            reminderService.scheduleAlarm(task);
        }

        if(TaskApiDao.insignificantChange(modifiedValues)) {
            return;
        }

        boolean completionDateModified = modifiedValues.contains(Task.COMPLETION_DATE.name);
        boolean deletionDateModified = modifiedValues.contains(Task.DELETION_DATE.name);

        boolean justCompleted = completionDateModified && task.isCompleted();
        boolean justDeleted = deletionDateModified && task.isDeleted();

        if (justCompleted || justDeleted) {
            notificationManager.cancel(taskId);
            geofenceService.cancelGeofences(taskId);
        } else if (completionDateModified || deletionDateModified) {
            geofenceService.setupGeofences(taskId);
        }

        if (justCompleted) {
            updateCalendarTitle(task);
            repeatTaskHelper.handleRepeat(task);
            if (task.getTimerStart() > 0) {
                timerPlugin.stopTimer(task);
            }
        }

        PushReceiver.broadcast(context, task, modifiedValues);
        refreshScheduler.scheduleRefresh(task);
        if (!task.checkAndClearTransitory(TRANS_SUPPRESS_REFRESH)) {
            localBroadcastManager.broadcastRefresh();
        }
    }

    private void updateCalendarTitle(Task task) {
        String calendarUri = task.getCalendarURI();
        if(!TextUtils.isEmpty(calendarUri)) {
            try {
                // change title of calendar event
                ContentResolver cr = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.TITLE, context.getString(R.string.gcal_completed_title,
                        task.getTitle()));
                cr.update(Uri.parse(calendarUri), values, null, null);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
