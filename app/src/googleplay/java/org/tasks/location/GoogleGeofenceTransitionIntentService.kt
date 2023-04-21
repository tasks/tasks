package org.tasks.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.Notifier
import org.tasks.data.LocationDao
import org.tasks.injection.InjectingJobIntentService
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GoogleGeofenceTransitionIntentService : InjectingJobIntentService() {
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var notifier: Notifier

    override suspend fun doWork(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Timber.e("geofence error code %s", geofencingEvent.errorCode)
            return
        }
        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        Timber.i("Received geofence transition: %s, %s", transitionType, triggeringGeofences)
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER || transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            triggeringGeofences?.forEach {
                triggerNotification(it, transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
            }
        } else {
            Timber.w("invalid geofence transition type: %s", transitionType)
        }
    }

    private suspend fun triggerNotification(triggeringGeofence: Geofence, arrival: Boolean) {
        val requestId = triggeringGeofence.requestId
        try {
            val place = locationDao.getPlace(requestId)
            if (place == null) {
                Timber.e("Can't find place for requestId %s", requestId)
                return
            }
            val geofences = if (arrival) {
                locationDao.getArrivalGeofences(place.uid!!, DateUtilities.now())
            } else {
                locationDao.getDepartureGeofences(place.uid!!, DateUtilities.now())
            }
            notifier.triggerNotifications(place.id, geofences, arrival)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering geofence %s: %s", requestId, e.message)
        }
    }

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            enqueueWork(
                    context,
                    GoogleGeofenceTransitionIntentService::class.java,
                    JOB_ID_GEOFENCE_TRANSITION,
                    intent)
        }
    }
}