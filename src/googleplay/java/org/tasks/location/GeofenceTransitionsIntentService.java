package org.tasks.location;

import android.content.Intent;

import com.google.android.gms.location.GeofencingEvent;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.injection.InjectingIntentService;

import java.util.List;

import javax.inject.Inject;

public class GeofenceTransitionsIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceTransitionsIntentService.class);

    @Inject Broadcaster broadcaster;
    @Inject MetadataDao metadataDao;

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
        String requestId = triggeringGeofence.getRequestId();
        try {
            Metadata fetch = metadataDao.fetch(Long.parseLong(requestId), Metadata.TASK, GeofenceFields.PLACE, GeofenceFields.LATITUDE, GeofenceFields.LONGITUDE, GeofenceFields.RADIUS);
            Geofence geofence = new Geofence(fetch);
            broadcaster.requestNotification(geofence.getMetadataId(), geofence.getTaskId());
        } catch(Exception e) {
            log.error(String.format("Error triggering geofence %s: %s", requestId, e.getMessage()), e);
        }
    }
}