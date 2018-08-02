package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.WorkManager;
import timber.log.Timber;

public class BackgroundScheduler extends InjectingJobIntentService {

  @Inject @ForApplication Context context;
  @Inject TaskDao taskDao;
  @Inject WorkManager jobManager;
  @Inject RefreshScheduler refreshScheduler;

  public static void enqueueWork(Context context) {
    BackgroundScheduler.enqueueWork(
        context, BackgroundScheduler.class, InjectingJobIntentService.JOB_ID_BACKGROUND_SCHEDULER, new Intent());
  }

  @Override
  protected void doWork(@NonNull Intent intent) {
    Timber.d("onHandleWork(%s)", intent);

    NotificationSchedulerIntentService.enqueueWork(context, false);
    CalendarNotificationIntentService.enqueueWork(context);
    GeofenceSchedulingIntentService.enqueueWork(context);

    for (Task task : taskDao.needsRefresh()) {
      refreshScheduler.scheduleRefresh(task);
    }
  }

  @Override
  protected void inject(IntentServiceComponent component) {
    component.inject(this);
  }
}
