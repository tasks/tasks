package org.tasks.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import timber.log.Timber

class AndroidGeofenceTransitionIntentService {

    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val arrival = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
            val placeId = intent.data?.lastPathSegment?.toLongOrNull()
            Timber.d("geofence[%s] arrival[%s]", intent.data, arrival)
            if (placeId == null) {
                Timber.e("Failed to parse place from %s", intent.data)
                return
            }
            val workRequest = OneTimeWorkRequest.Builder(AndroidGeofenceTransitionWork::class.java)
                .setInputData(workDataOf(
                    AndroidGeofenceTransitionWork.EXTRA_ARRIVAL to arrival,
                    AndroidGeofenceTransitionWork.EXTRA_PLACE_ID to placeId,
                ))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
