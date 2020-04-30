package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.data.LocationDao
import org.tasks.injection.InjectingWorker
import org.tasks.injection.JobComponent
import org.tasks.location.Geocoder
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ReverseGeocodeWork(context: Context, workerParams: WorkerParameters) : InjectingWorker(context, workerParams) {

    companion object {
        const val PLACE_ID = "place_id"
    }

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var geocoder: Geocoder
    @Inject lateinit var locationDao: LocationDao

    public override fun run(): Result {
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
            val result = geocoder.reverseGeocode(place.mapPosition)
            result.id = place.id
            result.uid = place.uid
            locationDao.update(result)
            localBroadcastManager.broadcastRefresh()
            Timber.d("found $result")
            Result.success()
        } catch (e: IOException) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    override fun inject(component: JobComponent) = component.inject(this)
}