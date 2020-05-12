package org.tasks.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.reminders.ReminderService
import org.tasks.Notifier
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.injection.InjectingJobIntentService
import org.tasks.injection.ServiceComponent
import org.tasks.notifications.Notification
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import javax.inject.Inject

class GeofenceTransitionsIntentService : InjectingJobIntentService() {
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var notifier: Notifier

    override fun doWork(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Timber.e("geofence error code %s", geofencingEvent.errorCode)
            return
        }
        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        Timber.i("Received geofence transition: %s, %s", transitionType, triggeringGeofences)
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER || transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            triggeringGeofences.forEach {
                triggerNotification(it, transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
            }
        } else {
            Timber.w("invalid geofence transition type: %s", transitionType)
        }
    }

    private fun triggerNotification(triggeringGeofence: Geofence, arrival: Boolean) {
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
            geofences
                    .map { toNotification(place, it, arrival) }
                    .apply(notifier::triggerNotifications)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering geofence %s: %s", requestId, e.message)
        }
    }

    private fun toNotification(place: Place, geofence: org.tasks.data.Geofence?, arrival: Boolean): Notification {
        val notification = Notification()
        notification.taskId = geofence!!.task
        notification.type = if (arrival) ReminderService.TYPE_GEOFENCE_ENTER else ReminderService.TYPE_GEOFENCE_EXIT
        notification.timestamp = DateTimeUtils.currentTimeMillis()
        notification.location = place.id
        return notification
    }

    override fun inject(component: ServiceComponent) = component.inject(this)

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            enqueueWork(
                    context,
                    GeofenceTransitionsIntentService::class.java,
                    JOB_ID_GEOFENCE_TRANSITION,
                    intent)
        }
    }
}