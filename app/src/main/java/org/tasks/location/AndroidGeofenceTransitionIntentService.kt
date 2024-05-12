package org.tasks.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.Notifier
import org.tasks.data.dao.LocationDao
import org.tasks.injection.InjectingJobIntentService
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AndroidGeofenceTransitionIntentService : InjectingJobIntentService() {
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var notifier: Notifier

    override suspend fun doWork(intent: Intent) {
        val arrival = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        Timber.d("geofence[${intent.data}] arrival[$arrival]")
        val place = intent.data?.lastPathSegment?.toLongOrNull()?.let { locationDao.getPlace(it) }
        if (place == null) {
            Timber.e("Failed to find place ${intent.data}")
            return
        }
        val geofences = if (arrival) {
            locationDao.getArrivalGeofences(place.uid!!)
        } else {
            locationDao.getDepartureGeofences(place.uid!!)
        }
        notifier.triggerNotifications(place.id, geofences, arrival)
    }

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            enqueueWork(
                    context,
                    AndroidGeofenceTransitionIntentService::class.java,
                    JOB_ID_GEOFENCE_TRANSITION,
                    intent)
        }
    }
}