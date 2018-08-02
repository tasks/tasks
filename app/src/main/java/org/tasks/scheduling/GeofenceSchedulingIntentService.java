package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import javax.inject.Inject;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.location.GeofenceService;
import timber.log.Timber;

public class GeofenceSchedulingIntentService extends InjectingJobIntentService {

  @Inject GeofenceService geofenceService;

  public static void enqueueWork(Context context) {
    JobIntentService.enqueueWork(
        context,
        GeofenceSchedulingIntentService.class,
        InjectingJobIntentService.JOB_ID_GEOFENCE_SCHEDULING,
        new Intent());
  }

  @Override
  protected void doWork(Intent intent) {
    Timber.d("onHandleWork(%s)", intent);

    geofenceService.cancelGeofences();
    geofenceService.setupGeofences();
  }

  @Override
  protected void inject(IntentServiceComponent component) {
    component.inject(this);
  }
}
