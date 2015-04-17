package org.tasks.location;

import android.content.Intent;

import com.google.android.gms.location.GeofencingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.injection.InjectingIntentService;

import java.util.List;

import javax.inject.Inject;

public class GeofenceTransitionsIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceTransitionsIntentService.class);

    @Inject GeofenceService geofenceService;
    @Inject Broadcaster broadcaster;

    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            log.error("geofence error code {}", geofencingEvent.getErrorCode());
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();

        List<com.google.android.gms.location.Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        log.info("Received geofence transition: {}, {}", transitionType, triggeringGeofences);
        if (transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER) {
            for (com.google.android.gms.location.Geofence triggerGeofence : triggeringGeofences) {
                triggerNotification(triggerGeofence);
            }
        } else {
            log.warn("invalid geofence transition type: {}", transitionType);
        }
    }

    private void triggerNotification(com.google.android.gms.location.Geofence triggeringGeofence) {
        try {
            Geofence geofence = geofenceService.getGeofenceById(Long.parseLong(triggeringGeofence.getRequestId()));
            broadcaster.requestNotification(geofence.getMetadataId(), geofence.getTaskId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}