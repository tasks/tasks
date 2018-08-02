package org.tasks.injection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import javax.annotation.Nonnull;
import timber.log.Timber;

public abstract class InjectingJobIntentService extends JobIntentService {

  public static final int JOB_ID_BACKGROUND_SCHEDULER = 2;
  public static final int JOB_ID_GEOFENCE_TRANSITION = 4;
  public static final int JOB_ID_GEOFENCE_SCHEDULING = 5;
  public static final int JOB_ID_TASK_STATUS_CHANGE = 8;
  public static final int JOB_ID_NOTIFICATION_SCHEDULER = 9;
  public static final int JOB_ID_CALENDAR_NOTIFICATION = 10;
  public static final int JOB_ID_TASKER = 11;

  @Override
  protected final void onHandleWork(@NonNull Intent intent) {
    inject(
        ((InjectingApplication) getApplication()).getComponent().plus(new IntentServiceModule()));

    try {
      doWork(intent);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  protected abstract void doWork(@Nonnull Intent intent);

  protected abstract void inject(IntentServiceComponent component);
}
