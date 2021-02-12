package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.data.MergedGeofence
import org.tasks.data.Place
import javax.inject.Inject

class GoogleGeofencing @Inject constructor(
        @ApplicationContext private val context: Context
): Geofencing {
    private val client = LocationServices.getGeofencingClient(context)

    @SuppressLint("MissingPermission")
    override fun addGeofences(geofence: MergedGeofence) {
        client.addGeofences(
                GeofencingRequest.Builder().addGeofence(toGoogleGeofence(geofence)).build(),
                PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(context, GoogleGeofenceTransitionIntentService.Broadcast::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT))
    }

    override fun removeGeofences(place: Place) {
        client.removeGeofences(listOf(place.id.toString()))
    }

    private fun toGoogleGeofence(geofence: MergedGeofence): Geofence {
        var transitionTypes = 0
        if (geofence.arrival) {
            transitionTypes = transitionTypes or GeofencingRequest.INITIAL_TRIGGER_ENTER
        }
        if (geofence.departure) {
            transitionTypes = transitionTypes or GeofencingRequest.INITIAL_TRIGGER_EXIT
        }
        return Geofence.Builder()
                .setCircularRegion(geofence.latitude, geofence.longitude, geofence.radius.toFloat())
                .setRequestId(geofence.uid)
                .setTransitionTypes(transitionTypes)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
    }
}