package org.tasks.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import com.google.android.gms.location.GeofencingEvent;
import com.todoroo.astrid.reminders.ReminderService;
import java.util.List;
import javax.inject.Inject;
import org.tasks.Notifier;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import timber.log.Timber;

public class GeofenceTransitionsIntentService extends InjectingJobIntentService {

  @Inject LocationDao locationDao;
  @Inject Notifier notifier;

  @Override
  protected void doWork(Intent intent) {
    GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
    if (geofencingEvent.hasError()) {
      Timber.e("geofence error code %s", geofencingEvent.getErrorCode());
      return;
    }

    int transitionType = geofencingEvent.getGeofenceTransition();

    List<com.google.android.gms.location.Geofence> triggeringGeofences =
        geofencingEvent.getTriggeringGeofences();
    Timber.i("Received geofence transition: %s, %s", transitionType, triggeringGeofences);
    if (transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER) {
      for (com.google.android.gms.location.Geofence triggerGeofence : triggeringGeofences) {
        triggerNotification(triggerGeofence);
      }
    } else {
      Timber.w("invalid geofence transition type: %s", transitionType);
    }
  }

  @Override
  protected void inject(IntentServiceComponent component) {
    component.inject(this);
  }

  private void triggerNotification(com.google.android.gms.location.Geofence triggeringGeofence) {
    String requestId = triggeringGeofence.getRequestId();
    try {
      Location location = locationDao.getGeofence(Long.parseLong(requestId));
      notifier.triggerTaskNotification(location.getTask(), ReminderService.TYPE_ALARM);
    } catch (Exception e) {
      Timber.e(e, "Error triggering geofence %s: %s", requestId, e.getMessage());
    }
  }

  public static class Broadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      JobIntentService.enqueueWork(
          context,
          GeofenceTransitionsIntentService.class,
          InjectingJobIntentService.JOB_ID_GEOFENCE_TRANSITION,
          intent);
    }
  }
}
