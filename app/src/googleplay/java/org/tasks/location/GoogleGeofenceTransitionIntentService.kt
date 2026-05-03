package org.tasks.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GoogleGeofenceTransitionIntentService {

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
            if (geofencingEvent.hasError()) {
                Timber.e("geofence error code %s", geofencingEvent.errorCode)
                return
            }
            val transitionType = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Timber.i("Received geofence transition: %s, %s", transitionType, triggeringGeofences)
            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
            ) {
                val arrival = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
                val requestIds = triggeringGeofences
                    ?.map { it.requestId }
                    ?.toTypedArray()
                    ?: return
                val workRequest = OneTimeWorkRequest.Builder(GoogleGeofenceTransitionWork::class.java)
                    .setInputData(workDataOf(
                        GoogleGeofenceTransitionWork.EXTRA_ARRIVAL to arrival,
                        GoogleGeofenceTransitionWork.EXTRA_REQUEST_IDS to requestIds,
                    ))
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            } else {
                Timber.w("invalid geofence transition type: %s", transitionType)
            }
        }
    }
}
