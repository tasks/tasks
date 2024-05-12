package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.MergedGeofence
import org.tasks.data.entity.Place
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class LocationServiceGooglePlay @Inject constructor(
        @ApplicationContext private val context: Context
) : LocationService {
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): MapPosition = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            LocationServices
                    .getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener {
                        cont.resumeWith(
                            it?.let { Result.success(MapPosition(it.latitude, it.longitude)) }
                                ?: Result.failure(NullPointerException())
                        )
                    }
                    .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
        }
    }

    @SuppressLint("MissingPermission")
    override fun addGeofences(geofence: MergedGeofence) {
        LocationServices
            .getGeofencingClient(context)
            .addGeofences(
                GeofencingRequest.Builder().addGeofence(toGoogleGeofence(geofence)).build(),
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, GoogleGeofenceTransitionIntentService.Broadcast::class.java),
                    if (atLeastS())
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
    }

    override fun removeGeofences(place: Place) {
        LocationServices
                .getGeofencingClient(context)
                .removeGeofences(listOf(place.id.toString()))
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
                .setRequestId(geofence.uid!!)
                .setTransitionTypes(transitionTypes)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
    }
}