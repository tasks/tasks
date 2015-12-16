package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.astrid.alarms.AlarmService;

import org.tasks.injection.InjectingIntentService;
import org.tasks.location.GeofenceService;

import javax.inject.Inject;

import timber.log.Timber;

public class AlarmSchedulingIntentService extends InjectingIntentService {

    @Inject AlarmService alarmService;
    @Inject GeofenceService geofenceService;

    public AlarmSchedulingIntentService() {
        super(AlarmSchedulingIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Timber.d("onHandleIntent(%s)", intent);

        alarmService.scheduleAllAlarms();
        geofenceService.setupGeofences();
    }
}
