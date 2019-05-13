package org.tasks.jobs;

import static com.todoroo.astrid.dao.TaskDao.TRANS_SUPPRESS_REFRESH;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Data.Builder;
import androidx.work.WorkerParameters;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.repeats.RepeatTaskHelper;
import com.todoroo.astrid.timers.TimerPlugin;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.location.GeofenceApi;
import org.tasks.notifications.NotificationManager;
import org.tasks.scheduling.RefreshScheduler;
import org.tasks.sync.SyncAdapters;
import timber.log.Timber;

public class AfterSaveWork extends InjectingWorker {

  private static final String EXTRA_ID = "extra_id";
  private static final String EXTRA_ORIG_COMPLETED = "extra_was_completed";
  private static final String EXTRA_ORIG_DELETED = "extra_was_deleted";
  private static final String EXTRA_PUSH_GTASKS = "extra_push_gtasks";
  private static final String EXTRA_PUSH_CALDAV = "extra_push_caldav";

  @Inject RepeatTaskHelper repeatTaskHelper;
  @Inject @ForApplication Context context;
  @Inject NotificationManager notificationManager;
  @Inject GeofenceApi geofenceApi;
  @Inject TimerPlugin timerPlugin;
  @Inject ReminderService reminderService;
  @Inject RefreshScheduler refreshScheduler;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject TaskDao taskDao;
  @Inject SyncAdapters syncAdapters;
  @Inject WorkManager workManager;

  public AfterSaveWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  static Data getInputData(Task current, Task original) {
    boolean suppress = current.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC);
    boolean force = current.checkTransitory(SyncFlags.FORCE_SYNC);
    Builder builder =
        new Builder()
            .putLong(EXTRA_ID, current.getId())
            .putBoolean(
                EXTRA_PUSH_GTASKS, !suppress && (force || !current.googleTaskUpToDate(original)))
            .putBoolean(
                EXTRA_PUSH_CALDAV, !suppress && (force || !current.caldavUpToDate(original)));
    if (original != null) {
      builder
          .putLong(EXTRA_ORIG_COMPLETED, original.getCompletionDate())
          .putLong(EXTRA_ORIG_DELETED, original.getDeletionDate());
    }
    return builder.build();
  }

  @Override
  protected Result run() {
    Data data = getInputData();
    long taskId = data.getLong(EXTRA_ID, -1);
    Task task = taskDao.fetch(taskId);
    if (task == null) {
      Timber.e("Missing saved task");
      return Result.failure();
    }

    reminderService.scheduleAlarm(task);

    boolean completionDateModified =
        !task.getCompletionDate().equals(data.getLong(EXTRA_ORIG_COMPLETED, 0));
    boolean deletionDateModified =
        !task.getDeletionDate().equals(data.getLong(EXTRA_ORIG_DELETED, 0));

    boolean justCompleted = completionDateModified && task.isCompleted();
    boolean justDeleted = deletionDateModified && task.isDeleted();

    if (justCompleted || justDeleted) {
      notificationManager.cancel(taskId);
      geofenceApi.cancel(taskId);
    } else if (completionDateModified || deletionDateModified) {
      geofenceApi.register(taskId);
    }

    if (justCompleted) {
      updateCalendarTitle(task);
      repeatTaskHelper.handleRepeat(task);
      if (task.getTimerStart() > 0) {
        timerPlugin.stopTimer(task);
      }
    }

    if ((data.getBoolean(EXTRA_PUSH_GTASKS, false) && syncAdapters.isGoogleTaskSyncEnabled())
        || (data.getBoolean(EXTRA_PUSH_CALDAV, false) && syncAdapters.isCaldavSyncEnabled())) {
      workManager.sync(false);
    }

    refreshScheduler.scheduleRefresh(task);
    if (!task.checkAndClearTransitory(TRANS_SUPPRESS_REFRESH)) {
      localBroadcastManager.broadcastRefresh();
    }

    return Result.success();
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
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
