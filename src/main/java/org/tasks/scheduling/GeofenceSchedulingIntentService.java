package org.tasks.scheduling;

import android.content.Intent;

import org.tasks.injection.InjectingIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.location.GeofenceService;

import javax.inject.Inject;

import timber.log.Timber;

public class GeofenceSchedulingIntentService extends InjectingIntentService {

    @Inject GeofenceService geofenceService;

    public GeofenceSchedulingIntentService() {
        super(GeofenceSchedulingIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Timber.d("onHandleIntent(%s)", intent);

        geofenceService.cancelGeofences();
        geofenceService.setupGeofences();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
