package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.data.LocationDao
import org.tasks.injection.BaseWorker
import org.tasks.location.Geocoder
import timber.log.Timber

@HiltWorker
class ReverseGeocodeWork @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val localBroadcastManager: LocalBroadcastManager,
        private val geocoder: Geocoder,
        private val locationDao: LocationDao) : BaseWorker(context, workerParams, firebase) {

    companion object {
        const val PLACE_ID = "place_id"
    }

    override suspend fun run(): Result {
        val id = inputData.getLong(PLACE_ID, 0)
        if (id == 0L) {
            Timber.e("Missing id")
            return Result.failure()
        }
        val place = locationDao.getPlace(id)
        if (place == null) {
            Timber.e("Can't find place $id")
            return Result.failure()
        }
        return try {
            val result = geocoder.reverseGeocode(place.mapPosition) ?: return Result.failure()
            result.id = place.id
            result.uid = place.uid
            locationDao.update(result)
            localBroadcastManager.broadcastRefresh()
            Timber.d("found $result")
            Result.success()
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }
}