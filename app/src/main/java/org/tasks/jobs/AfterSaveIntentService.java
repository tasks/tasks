package org.tasks.jobs;

import static com.todoroo.astrid.dao.TaskDao.TRANS_SUPPRESS_REFRESH;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.repeats.RepeatTaskHelper;
import com.todoroo.astrid.timers.TimerPlugin;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.location.GeofenceService;
import org.tasks.notifications.NotificationManager;
import org.tasks.receivers.PushReceiver;
import org.tasks.scheduling.RefreshScheduler;
import timber.log.Timber;

public class AfterSaveIntentService extends InjectingJobIntentService {

  private static final String EXTRA_CURRENT = "extra_current";
  private static final String EXTRA_ORIGINAL = "extra_original";
  @Inject RepeatTaskHelper repeatTaskHelper;
  @Inject @ForApplication Context context;
  @Inject NotificationManager notificationManager;
  @Inject GeofenceService geofenceService;
  @Inject TimerPlugin timerPlugin;
  @Inject ReminderService reminderService;
  @Inject RefreshScheduler refreshScheduler;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject PushReceiver pushReceiver;

  public static void enqueue(Context context, Task current, Task original) {
    Intent intent = new Intent();
    intent.putExtra(EXTRA_CURRENT, current);
    intent.putExtra(EXTRA_ORIGINAL, original);
    AfterSaveIntentService.enqueueWork(
        context, AfterSaveIntentService.class, InjectingJobIntentService.JOB_ID_TASK_STATUS_CHANGE, intent);
  }

  @Override
  protected void doWork(@NonNull Intent intent) {
    Task task = intent.getParcelableExtra(EXTRA_CURRENT);
    if (task == null) {
      Timber.e("Missing saved task");
      return;
    }
    long taskId = task.getId();

    Task original = intent.getParcelableExtra(EXTRA_ORIGINAL);
    if (original == null
        || !task.getDueDate().equals(original.getDueDate())
        || !task.getReminderFlags().equals(original.getReminderFlags())
        || !task.getReminderPeriod().equals(original.getReminderPeriod())
        || !task.getReminderLast().equals(original.getReminderLast())
        || !task.getReminderSnooze().equals(original.getReminderSnooze())) {
      reminderService.scheduleAlarm(task);
    }

    boolean completionDateModified =
        original == null || !task.getCompletionDate().equals(original.getCompletionDate());
    boolean deletionDateModified =
        original != null && !task.getDeletionDate().equals(original.getDeletionDate());

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

    pushReceiver.push(task, original);
    refreshScheduler.scheduleRefresh(task);
    if (!task.checkAndClearTransitory(TRANS_SUPPRESS_REFRESH)) {
      localBroadcastManager.broadcastRefresh();
    }
  }

  private void updateCalendarTitle(Task task) {
    String calendarUri = task.getCalendarURI();
    if (!TextUtils.isEmpty(calendarUri)) {
      try {
        // change title of calendar event
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(
            CalendarContract.Events.TITLE,
            context.getString(R.string.gcal_completed_title, task.getTitle()));
        cr.update(Uri.parse(calendarUri), values, null, null);
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }

  @Override
  protected void inject(IntentServiceComponent component) {
    component.inject(this);
  }
}
