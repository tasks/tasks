package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.data.MergedGeofence
import org.tasks.data.Place
import javax.inject.Inject

@Suppress("unused")
class AndroidGeofencing @Inject constructor(
        @ApplicationContext private val context: Context
): Geofencing {
    private val client = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun addGeofences(geofence: MergedGeofence) {
        client.addProximityAlert(
                geofence.latitude,
                geofence.longitude,
                geofence.radius.toFloat(),
                -1,
                createPendingIntent(geofence.place.id)
        )
    }

    override fun removeGeofences(place: Place) {
        client.removeProximityAlert(createPendingIntent(place.id))
    }

    private fun createPendingIntent(place: Long) =
            PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, AndroidGeofenceTransitionIntentService.Broadcast::class.java)
                            .setData(Uri.parse("tasks://geofence/$place")),
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
}

