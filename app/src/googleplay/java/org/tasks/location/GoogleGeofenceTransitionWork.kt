package org.tasks.location

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.data.dao.LocationDao
import org.tasks.Notifier
import org.tasks.injection.BaseWorker
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber

@HiltWorker
class GoogleGeofenceTransitionWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val locationDao: LocationDao,
    private val notifier: Notifier,
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val arrival = inputData.getBoolean(EXTRA_ARRIVAL, false)
        val requestIds = inputData.getStringArray(EXTRA_REQUEST_IDS) ?: return Result.failure()
        Timber.d("geofence transition arrival=%s requestIds=%s", arrival, requestIds.toList())
        requestIds.forEach { requestId ->
            triggerNotification(requestId, arrival)
        }
        return Result.success()
    }

    private suspend fun triggerNotification(requestId: String, arrival: Boolean) {
        try {
            val place = locationDao.getPlace(requestId) ?: run {
                Timber.e("Can't find place for requestId %s", requestId)
                return
            }
            val geofences = if (arrival) {
                locationDao.getArrivalGeofences(place.uid!!, currentTimeMillis())
            } else {
                locationDao.getDepartureGeofences(place.uid!!, currentTimeMillis())
            }
            notifier.triggerNotifications(place.id, geofences, arrival)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering geofence %s: %s", requestId, e.message)
        }
    }

    companion object {
        const val EXTRA_ARRIVAL = "extra_arrival"
        const val EXTRA_REQUEST_IDS = "extra_request_ids"
    }
}
