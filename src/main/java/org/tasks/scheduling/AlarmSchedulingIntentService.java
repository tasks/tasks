package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.astrid.alarms.AlarmService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingIntentService;
import org.tasks.location.GeofenceService;

import javax.inject.Inject;

public class AlarmSchedulingIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(AlarmSchedulingIntentService.class);

    @Inject AlarmService alarmService;
    @Inject GeofenceService geofenceService;

    public AlarmSchedulingIntentService() {
        super(AlarmSchedulingIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        log.debug("onHandleIntent({})", intent);

        alarmService.scheduleAllAlarms();
        geofenceService.setupGeofences();
    }
}
