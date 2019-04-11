package org.tasks.injection;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import timber.log.Timber;

public abstract class InjectingJobIntentService extends JobIntentService {

  public static final int JOB_ID_GEOFENCE_TRANSITION = 1081;
  public static final int JOB_ID_REFRESH_RECEIVER = 1082;
  public static final int JOB_ID_NOTIFICATION_SCHEDULER = 1084;
  public static final int JOB_ID_CALENDAR_NOTIFICATION = 1085;
  public static final int JOB_ID_TASKER = 1086;

  @Override
  protected final void onHandleWork(@NonNull Intent intent) {
    inject(
        ((InjectingApplication) getApplication()).getComponent().plus(new ServiceModule()));

    try {
      doWork(intent);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  protected abstract void doWork(@NonNull Intent intent);

  protected abstract void inject(ServiceComponent component);
}
