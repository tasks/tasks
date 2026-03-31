package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.MergedGeofence
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.PermissionChecker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class LocationServiceGooglePlay @Inject constructor(
        @ApplicationContext private val context: Context,
        private val permissionChecker: PermissionChecker,
        override val locationDao: LocationDao,
        override val appPreferences: AppPreferences,
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
        if (!permissionChecker.canAccessBackgroundLocation()) return
        LocationServices
            .getGeofencingClient(context)
            .addGeofences(
                GeofencingRequest.Builder()
                    .setInitialTrigger(0)
                    .addGeofence(toGoogleGeofence(geofence))
                    .build(),
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
            .addOnSuccessListener {
                Timber.d("Geofence registered for %s", geofence.uid)
            }
            .addOnFailureListener {
                Timber.e(it, "Failed to register geofence for %s", geofence.uid)
            }
    }

    override fun removeGeofences(place: Place) {
        if (!permissionChecker.canAccessBackgroundLocation()) return
        LocationServices
                .getGeofencingClient(context)
                .removeGeofences(listOf(place.uid!!))
                .addOnSuccessListener {
                    Timber.d("Geofence removed for %s", place.uid)
                }
                .addOnFailureListener {
                    Timber.e(it, "Failed to remove geofence for %s", place.uid)
                }
    }

    @SuppressLint("MissingPermission")
    override fun startBackgroundLocationUpdates(intervalMinutes: Int) {
        if (!permissionChecker.canAccessBackgroundLocation()) return
        val client = LocationServices.getFusedLocationProviderClient(context)
        val pendingIntent = LocationUpdateReceiver.pendingIntent(context)
        client.removeLocationUpdates(pendingIntent)
        if (intervalMinutes > 0) {
            val intervalMs = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                intervalMs
            ).build()
            client.requestLocationUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Timber.d("Background location updates started (interval=%dm)", intervalMinutes)
                }
                .addOnFailureListener {
                    Timber.e(it, "Failed to start background location updates")
                }
        }
    }

    override fun stopBackgroundLocationUpdates() {
        LocationServices
            .getFusedLocationProviderClient(context)
            .removeLocationUpdates(LocationUpdateReceiver.pendingIntent(context))
    }

    private fun toGoogleGeofence(geofence: MergedGeofence): Geofence {
        var transitionTypes = 0
        if (geofence.arrival) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_ENTER
        }
        if (geofence.departure) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
        }
        return Geofence.Builder()
                .setCircularRegion(geofence.latitude, geofence.longitude, geofence.radius.toFloat())
                .setRequestId(geofence.uid!!)
                .setTransitionTypes(transitionTypes)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
    }
}
