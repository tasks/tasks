package org.tasks.jobs

import android.content.Context
import android.location.Location
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.data.mapPosition
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
        private val locationDao: LocationDao
) : BaseWorker(context, workerParams, firebase) {

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
            val distanceBetween = place.distanceTo(result)
            if (distanceBetween > 100) {
                Timber.d("Ignoring $result - ${distanceBetween}m away")
                return Result.failure()
            }
            locationDao.update(
                place.copy(
                    name = result.name,
                    address = result.address,
                    phone = result.phone,
                    url = result.url,
                )
            )
            localBroadcastManager.broadcastRefresh()
            Timber.d("found $result")
            Result.success()
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    companion object {
        const val PLACE_ID = "place_id"

        private fun Place.distanceTo(other: Place) = toLocation().distanceTo(other.toLocation())

        private fun Place.toLocation(): Location {
            return Location(null).apply {
                latitude = this@toLocation.latitude
                longitude = this@toLocation.longitude
            }
        }
    }
}