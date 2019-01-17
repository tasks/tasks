package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import androidx.core.app.JobIntentService;
import javax.inject.Inject;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.ServiceComponent;
import org.tasks.location.GeofenceService;
import timber.log.Timber;

public class GeofenceSchedulingIntentService extends InjectingJobIntentService {

  @Inject GeofenceService geofenceService;

  public static void enqueueWork(Context context) {
    JobIntentService.enqueueWork(
        context,
        GeofenceSchedulingIntentService.class,
        InjectingJobIntentService.JOB_ID_GEOFENCE_SCHEDULING,
        new Intent(context, GeofenceSchedulingIntentService.class));
  }

  @Override
  protected void doWork(Intent intent) {
    Timber.d("onHandleWork(%s)", intent);

    geofenceService.cancelGeofences();
    geofenceService.setupGeofences();
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
